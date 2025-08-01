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
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.properties.AssignmentProperties
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
) {
    fun createAssignment(assignmentDto: AssignmentDto): Assignment {
        val description =
            runCatching {
                getAssignmentDescription(assignmentDto.taskId)
            }.onFailure {
                log.error("Could not create assignment: ${it.message}")
                throw ServiceException(
                    HttpStatus.NOT_FOUND,
                    "Could not create assignment: probably there is no task${assignmentDto.taskId} in the repository",
                )
            }

        val assignment =
            Assignment().apply {
                title = assignmentDto.title
                this.description = description.getOrNull()!!
                this.taskId = assignmentDto.taskId
                maxPoints = assignmentDto.maxPoints
                startDate = assignmentDto.startDate
                endDate = assignmentDto.endDate
            }

        val savedAssignment = assignmentRepository.save(assignment)
        return savedAssignment
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

    /**
     * Retrieves hidden files for a specific assignment number
     * @param assignmentNumber The assignment number
     * @return A byte array containing the hidden files in a ZIP archive
     */
    fun getHiddenFilesForTask(assignmentNumber: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        streamZipToOutput(
            outputStream,
            pathFilters = listOf("^task$assignmentNumber/.*/hidden/.*".toRegex()),
            excludePatterns =
                listOf(
                    "\\.git/.*".toRegex(),
                    "\\.idea/.*".toRegex(),
                ),
        )
        return outputStream.toByteArray()
    }

    fun getAssignmentDescription(assignmentNumber: Int): String = getFileContentFromAssignmentRepo(Path.of("DESCRIPTION.md"), assignmentNumber).toString(Charsets.UTF_8)

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
