package org.jikvict.jikvictbackend.service.plagiarism

import de.jplag.JPlag
import de.jplag.java.JavaLanguage
import de.jplag.options.JPlagOptions
import de.jplag.reporting.reportobject.ReportObjectFactory
import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.service.assignment.AssignmentService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

@Service
class PlagiarismCheckRunner(
    private val log: Logger,
    private val dataService: PlagiarismCheckDataService,
    private val assignmentService: AssignmentService,
) {
    @Async
    fun run(assignmentId: Long, taskId: Long) {
        try {
            val submissions = dataService.loadSubmissions(assignmentId)
            if (submissions.isEmpty()) {
                dataService.failTask(taskId, "No submissions with zip files found for assignment $assignmentId")
                return
            }
            val baseCodeZip = runCatching { assignmentService.getZip(assignmentId) }.getOrNull()

            val tempDir = Files.createTempDirectory("jplag-$taskId")
            try {
                val submissionsDir = tempDir.resolve("submissions")
                Files.createDirectories(submissionsDir)

                submissions.forEach { (username, zipBytes) ->
                    val studentDir = submissionsDir.resolve(sanitizeFilename(username))
                    Files.createDirectories(studentDir)
                    extractZipStrippingPrefix(zipBytes, studentDir, prefixDepth = 2)
                }

                val baseCodeDir = tempDir.resolve("basecode")
                if (baseCodeZip != null && baseCodeZip.isNotEmpty()) {
                    Files.createDirectories(baseCodeDir)
                    extractZipStrippingPrefix(baseCodeZip, baseCodeDir, prefixDepth = 1)
                }

                val outputZipFile = tempDir.resolve("report.zip").toFile()

                val language = JavaLanguage()
                val options = JPlagOptions(language, setOf(submissionsDir.toFile()), emptySet())
                    .let { opts ->
                        if (baseCodeZip != null && baseCodeZip.isNotEmpty()) {
                            opts.withBaseCodeSubmissionDirectory(baseCodeDir.toFile())
                        } else {
                            opts
                        }
                    }

                log.info("Running JPlag for assignment=$assignmentId taskId=$taskId submissions=${submissions.size}")
                val result = JPlag.run(options)
                val reportFactory = ReportObjectFactory(outputZipFile)
                reportFactory.createAndSaveReport(result)

                val reportBytes = outputZipFile.readBytes()
                dataService.saveResult(assignmentId, taskId, reportBytes)
                log.info("Plagiarism check taskId=$taskId completed")
            } finally {
                tempDir.toFile().deleteRecursively()
            }
        } catch (e: Exception) {
            log.error("Plagiarism check failed for taskId=$taskId: ${e.message}", e)
            dataService.failTask(taskId, e.message ?: "Unknown error")
        }
    }

    private fun extractZipStrippingPrefix(zipBytes: ByteArray, targetDir: Path, prefixDepth: Int) {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val parts = entry.name.split("/").filter { it.isNotEmpty() }
                    if (parts.size > prefixDepth) {
                        val relativePath = parts.drop(prefixDepth).joinToString("/")
                        val targetFile = targetDir.resolve(relativePath)
                        Files.createDirectories(targetFile.parent)
                        Files.write(targetFile, zis.readBytes())
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun sanitizeFilename(name: String): String = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}
