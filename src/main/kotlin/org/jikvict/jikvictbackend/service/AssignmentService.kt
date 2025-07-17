package org.jikvict.jikvictbackend.service

import org.apache.logging.log4j.Logger
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.TreeWalk
import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.jikvictbackend.model.properties.AssignmentProperties
import org.jikvict.jikvictbackend.model.response.PendingStatus
import org.jikvict.jikvictbackend.model.response.PendingStatusResponse
import org.jikvict.jikvictbackend.model.response.ResponsePayload
import org.jikvict.jikvictbackend.repository.AssignmentRepository
import org.jikvict.problems.exception.contract.ServiceException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class AssignmentService(
    private val properties: AssignmentProperties,
    private val log: Logger,
    private val assignmentRepository: AssignmentRepository,
    private val taskQueueService: TaskQueueService,
) {
    /**
     * Creates an assignment asynchronously by enqueueing a task
     * @param number The assignment number
     * @return A response with the task ID and pending status
     */
    fun createAssignmentByNumber(number: Int): PendingStatusResponse<Long> {
        log.info("Creating assignment by number: $number")
        val taskId = taskQueueService.enqueueAssignmentCreationTask(number)
        return PendingStatusResponse(
            payload = ResponsePayload(taskId),
            status = PendingStatus.PENDING,
        )
    }

    /**
     * Gets an assignment by ID
     * @param id The assignment ID
     * @return The assignment
     */
    fun getAssignmentById(id: Long): Assignment =
        assignmentRepository
            .findById(id)
            .orElseThrow { ServiceException(HttpStatus.NOT_FOUND, "Assignment with ID $id not found") }

    fun cloneZipBytes(include: List<Regex>): ByteArray {
        val outputStream = ByteArrayOutputStream()
        streamZipToOutput(
            outputStream,
            pathFilters = include,
            excludePatterns =
                listOf(
                    ".*/hidden(/.*)?".toRegex(),
                    "\\.git/.*".toRegex(),
                    "\\.idea/.*".toRegex(),
                ),
        )
        return outputStream.toByteArray()
    }

    fun getAssignmentDescription(assignmentNumber: Int): String = getFileContentFromAssignmentRepo(Path.of("DESCRIPTION.md"), assignmentNumber).toString(Charsets.UTF_8)

    fun getFileFromAssignmentRepo(
        file: Path,
        assignmentNumber: Int,
    ): ByteArray {
        val fileName = file.fileName.toString()
        return cloneZipBytes(listOf("^task$assignmentNumber/.*${Regex.escape(fileName)}$".toRegex()))
    }

    fun getFileContentFromAssignmentRepo(
        file: Path,
        assignmentNumber: Int,
    ): ByteArray {
        val fileName = file.fileName.toString()
        val pattern = "^task$assignmentNumber/.*${Regex.escape(fileName)}$".toRegex()

        val credentialsProvider = UsernamePasswordCredentialsProvider(properties.githubUsername, properties.githubToken)

        val repoDesc = DfsRepositoryDescription("streaming-repo")
        val repo = InMemoryRepository(repoDesc)
        repo.create()

        Git(repo).use { git ->
            git
                .remoteAdd()
                .setName("origin")
                .setUri(URIish(properties.repositoryUrl))
                .call()

            git
                .fetch()
                .setRemote("origin")
                .setRefSpecs(RefSpec("+refs/heads/main:refs/remotes/origin/main"))
                .setCredentialsProvider(credentialsProvider)
                .setDepth(1)
                .call()

            val ref = repo.exactRef("refs/remotes/origin/main") ?: error("Branch origin/main not found")
            val revWalk = RevWalk(repo)
            val commit = revWalk.parseCommit(ref.objectId)
            val tree = commit.tree

            val treeWalk = TreeWalk(repo)
            treeWalk.addTree(tree)
            treeWalk.isRecursive = true

            while (treeWalk.next()) {
                val path = treeWalk.pathString

                if (path.matches(pattern)) {
                    val objectId = treeWalk.getObjectId(0)
                    val loader = repo.open(objectId)

                    return loader.openStream().use { inputStream ->
                        inputStream.readBytes()
                    }
                }
            }

            throw ServiceException(
                HttpStatus.NOT_FOUND,
                "File $fileName not found in assignment $assignmentNumber",
            )
        }
    }

    fun streamZipToOutput(
        outputStream: OutputStream,
        pathFilters: List<Regex> = emptyList(),
        excludePatterns: List<Regex> = emptyList(),
    ) {
        val credentialsProvider = UsernamePasswordCredentialsProvider(properties.githubUsername, properties.githubToken)

        val repoDesc = DfsRepositoryDescription("streaming-repo")
        val repo = InMemoryRepository(repoDesc)
        repo.create()

        Git(repo).use { git ->
            git
                .remoteAdd()
                .setName("origin")
                .setUri(URIish(properties.repositoryUrl))
                .call()

            git
                .fetch()
                .setRemote("origin")
                .setRefSpecs(RefSpec("+refs/heads/main:refs/remotes/origin/main"))
                .setCredentialsProvider(credentialsProvider)
                .setDepth(1)
                .call()

            val ref = repo.exactRef("refs/remotes/origin/main") ?: error("Branch origin/main not found")
            val revWalk = RevWalk(repo)
            val commit = revWalk.parseCommit(ref.objectId)
            val tree = commit.tree
            var filesProcessed = 0
            ZipOutputStream(outputStream).use { zipOut ->
                val treeWalk = TreeWalk(repo)
                treeWalk.addTree(tree)
                treeWalk.isRecursive = true

                while (treeWalk.next()) {
                    val path = treeWalk.pathString

                    val shouldExclude =
                        excludePatterns.any { pattern ->
                            path.matches(pattern)
                        }

                    if (shouldExclude) {
                        log.info("Excluding file: $path")
                        continue
                    }

                    val shouldInclude =
                        pathFilters.isEmpty() ||
                            pathFilters.any { filter ->
                                path.matches(filter)
                            }

                    if (!shouldInclude) {
                        log.info("Excluding file: $path")
                        continue
                    }

                    val objectId = treeWalk.getObjectId(0)
                    val loader = repo.open(objectId)

                    zipOut.putNextEntry(ZipEntry(path))
                    loader.openStream().use { inputStream ->
                        inputStream.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                    filesProcessed++
                    log.info("Processed file: $path (${loader.size} bytes)")
                }
            }
            if (filesProcessed == 0) {
                throw ServiceException(
                    HttpStatus.NOT_FOUND,
                    "No files found matching the specified filters",
                )
            }
        }
    }
}
