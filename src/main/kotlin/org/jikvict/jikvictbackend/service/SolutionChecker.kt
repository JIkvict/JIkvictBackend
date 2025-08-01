package org.jikvict.jikvictbackend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import org.apache.logging.log4j.Logger
import org.jikvict.problems.exception.contract.ServiceException
import org.jikvict.testing.model.TestSuiteResult
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service
class SolutionChecker(
    private val logger: Logger,
    private val assignmentService: AssignmentService,
    private val objectMapper: ObjectMapper,
) {
    fun checkSolution(
        taskId: Int,
        solution: ByteArray,
        timeoutSeconds: Long,
    ): TestSuiteResult {
        logger.info("Retrieving hidden files for assignment $taskId")
        val hiddenFilesBytes = assignmentService.getHiddenFilesForTask(taskId)
        return executeCode(solution, hiddenFilesBytes, timeoutSeconds)
    }

    fun executeCode(
        solution: ByteArray,
        hiddenFiles: ByteArray,
        timeoutSeconds: Long,
    ): TestSuiteResult {
        val executionId = UUID.randomUUID().toString()
        val tempDir = Files.createTempDirectory("code-$executionId")
        val targetFile = tempDir.resolve("solution")
        Files.write(targetFile, solution)
        val hiddenTargetFile = tempDir.resolve("hidden-files")
        Files.write(hiddenTargetFile, hiddenFiles)
        val gradleCacheDir = Path.of("/tmp/gradle-cache", executionId)
        Files.createDirectories(gradleCacheDir)
        var resultsJson: String? = null

        try {
            Files.setPosixFilePermissions(
                tempDir,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE,
                ),
            )

            Files.setPosixFilePermissions(
                targetFile,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.OTHERS_READ,
                ),
            )

            Files.setPosixFilePermissions(
                hiddenTargetFile,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.OTHERS_READ,
                ),
            )
        } catch (e: Exception) {
            logger.warn("Failed to set POSIX file permissions: ${e.message}")
        }

        try {
            val dockerImage = "jikvict-solution-runner"

            GenericContainer(dockerImage)
                .apply {
                    withFileSystemBind(tempDir.toString(), "/app/input", BindMode.READ_WRITE)
                    withWorkingDirectory("/app")

                    withCreateContainerCmdModifier { cmd ->
                        cmd.hostConfig?.apply {
                            withMemory(1024 * 1024 * 1024L)
                            withCpuQuota(1000000L)
                            withPidsLimit(500L)
                            withNetworkMode("bridge")
                            withReadonlyRootfs(false)
                            withTmpFs(
                                mapOf(
                                    "/tmp" to "size=1g",
                                    "/root" to "size=1g",
                                    "/var/tmp" to "size=1g",
                                ),
                            )
                            withBinds(
                                Bind(tempDir.toString(), Volume("/app/input")),
                                Bind(gradleCacheDir.toString(), Volume("/gradle-cache")),
                            )
                        }
                    }

                    withEnv("GRADLE_USER_HOME", "/gradle-cache")
                    withEnv("GRADLE_OPTS", "-Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Xmx512m")
                    withEnv("JAVA_OPTS", "-Xmx256m -Xms128m")
                    withEnv("RESULTS_OUTPUT_DIR", "/app/input")

                    withCommand(
                        "/app/input/${targetFile.fileName}",
                        "/app/input/${hiddenTargetFile.fileName}",
                        timeoutSeconds.toString(),
                        "/app/input/jikvict-results.json",
                    )
                    withStartupTimeout(30.seconds.toJavaDuration())
                }.use { container ->
                    container.start()
                    logger.info("Container started: ${container.containerId}")

                    val exitCode =
                        try {
                            container.dockerClient
                                .waitContainerCmd(container.containerId)
                                .exec(WaitContainerResultCallback())
                                .awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS)
                        } catch (e: Exception) {
                            logger.error("Timeout or error waiting for container completion", e)
                            container.stop()
                            -1
                        }

                    val logs = container.logs
                    logger.info("Logs: $logs")

                    when (exitCode) {
                        0 -> {
                            logger.info("Code executed successfully. Exit code: $exitCode")
                        }

                        -1 -> {
                            logger.error("Code execution timed out. Container was forcibly stopped")
                        }

                        else -> {
                            logger.error("Code execution failed. Exit code: $exitCode")
                        }
                    }

                    try {
                        val resultFiles =
                            Files
                                .walk(tempDir)
                                .filter { Files.isRegularFile(it) }
                                .filter { it.fileName.toString() == "jikvict-results.json" }
                                .toList()

                        if (resultFiles.isNotEmpty()) {
                            val resultsFile = resultFiles.first()
                            resultsJson = Files.readString(resultsFile)
                        } else {
                            val jsonFiles =
                                Files
                                    .walk(tempDir)
                                    .filter { Files.isRegularFile(it) }
                                    .filter { it.fileName.toString().endsWith(".json") }
                                    .toList()

                            if (jsonFiles.isNotEmpty()) {
                                val resultsFile = jsonFiles.first()
                                resultsJson = Files.readString(resultsFile)
                            } else {
                                logger.warn("No result files found")
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Error searching for results", e)
                    }
                }
        } catch (e: Exception) {
            logger.error("Error executing code", e)
        } finally {
            cleanupDirectory(tempDir)
            cleanupDirectory(gradleCacheDir)
        }

        val results =
            runCatching {
                objectMapper.readValue(resultsJson, TestSuiteResult::class.java)
            }.onFailure {
                logger.error("Failed to parse results", it)
                throw ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse results")
            }.getOrNull()!!

        logger.info("Results: $results")

        return results
    }

    fun cleanupDirectory(directory: Path) {
        try {
            Files
                .walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        } catch (e: Exception) {
            logger.error("Error cleaning up temporary directory", e)
        }
    }
}
