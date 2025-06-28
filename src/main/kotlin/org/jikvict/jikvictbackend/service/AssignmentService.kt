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
import org.jikvict.jikvictbackend.model.properties.AssignmentProperties
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


@Service
class AssignmentService(
    private val properties: AssignmentProperties,
    private val log: Logger,
) {
    @Volatile
    private var cachedCommitSha: String? = null

    @Volatile
    private var cachedZipBytes: ByteArray? = null

    fun cloneZipBytes(): ByteArray {
        val credentialsProvider = UsernamePasswordCredentialsProvider(properties.githubUsername, properties.githubToken)

        val latestCommitSha = fetchLatestCommitSha(credentialsProvider)
        log.info("Latest remote commit SHA: $latestCommitSha")

        if (cachedCommitSha == latestCommitSha && cachedZipBytes != null) {
            log.info("Returning cached zip for commit $latestCommitSha")
            return cachedZipBytes!!
        }

        log.info("Cache miss or first fetch. Cloning repository and creating archive")

        val (newSha, newZip) = cloneAndZip(credentialsProvider)

        cachedCommitSha = newSha
        cachedZipBytes = newZip

        return newZip
    }

    private fun fetchLatestCommitSha(credentialsProvider: UsernamePasswordCredentialsProvider): String {
        val repoDesc = DfsRepositoryDescription("tmp-check-repo")
        val repo = InMemoryRepository(repoDesc)
        repo.create()

        Git(repo).use { git ->
            git.remoteAdd()
                .setName("origin")
                .setUri(URIish(properties.repositoryUrl))
                .call()

            git.fetch()
                .setRemote("origin")
                .setRefSpecs(RefSpec("+refs/heads/*:refs/remotes/origin/*"))
                .setCredentialsProvider(credentialsProvider)
                .call()

            val ref = repo.exactRef("refs/remotes/origin/main") ?: error("Branch origin/main not found")

            return ref.objectId.name
        }
    }

    private fun cloneAndZip(credentialsProvider: UsernamePasswordCredentialsProvider): Pair<String, ByteArray> {
        val repoDesc = DfsRepositoryDescription("in-memory-repo")
        val inMemoryRepo = InMemoryRepository(repoDesc)
        inMemoryRepo.create()

        Git(inMemoryRepo).use { git ->
            git.remoteAdd()
                .setName("origin")
                .setUri(URIish(properties.repositoryUrl))
                .call()

            git.fetch()
                .setRemote("origin")
                .setRefSpecs(RefSpec("+refs/heads/*:refs/remotes/origin/*"))
                .setCredentialsProvider(credentialsProvider)
                .call()

            val ref = inMemoryRepo.exactRef("refs/remotes/origin/main") ?: error("Branch origin/main not found")

            val revWalk = RevWalk(inMemoryRepo)
            val commit = revWalk.parseCommit(ref.objectId)
            val tree = commit.tree

            val zipOutBytes = ByteArrayOutputStream()
            val zipOut = ZipOutputStream(zipOutBytes)

            val treeWalk = TreeWalk(inMemoryRepo)
            treeWalk.addTree(tree)
            treeWalk.isRecursive = true

            while (treeWalk.next()) {
                val path = treeWalk.pathString
                val objectId = treeWalk.getObjectId(0)
                val loader = inMemoryRepo.open(objectId)
                val content = loader.bytes

                zipOut.putNextEntry(ZipEntry(path))
                zipOut.write(content)
                zipOut.closeEntry()
            }

            zipOut.close()
            return Pair(commit.name, zipOutBytes.toByteArray())
        }
    }
}
