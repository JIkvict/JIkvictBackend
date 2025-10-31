package org.jikvict.jikvictbackend.service.solution

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import org.apache.logging.log4j.Logger
import org.jikvict.docker.NetworkManager
import org.jikvict.docker.dockerRunner
import org.jikvict.docker.env
import org.jikvict.docker.util.grantAllPermissions
import org.jikvict.jikvictbackend.entity.Assignment
import org.jikvict.problems.exception.contract.ServiceException
import org.jikvict.testing.model.TestSuiteResult
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.streams.asSequence
import kotlin.time.Duration.Companion.seconds

@Service
class SolutionChecker(
    private val logger: Logger,
    private val objectMapper: ObjectMapper,
    private val networkManager: NetworkManager,
) {
    @Transactional
    suspend fun checkSolution(
        solution: ByteArray,
        hiddenFiles: ByteArray,
        assignment: Assignment,
        isActive: () -> Boolean
    ): TestSuiteResult = execute(solution, hiddenFiles, assignment, isActive)

    private suspend fun execute(
        solution: ByteArray,
        hiddenFiles: ByteArray,
        assignmentDto: Assignment,
        isActive: () -> Boolean
    ): TestSuiteResult {
        val executionId = UUID.randomUUID().toString()
        val tempDir = Files.createTempDirectory("code-$executionId")
        logger.info("Created temporary directory: ${tempDir.toAbsolutePath()} for task: $executionId")
        val targetFile = tempDir.resolve("solution")
        Files.write(targetFile, solution)
        val hiddenTargetFile = tempDir.resolve("hidden-files")
        Files.write(hiddenTargetFile, hiddenFiles)
        val gradleCacheDir = Path.of("/tmp/gradle-cache", executionId)
        Files.createDirectories(gradleCacheDir)

        val networkId = networkManager.createIsolatedNetwork(executionId)
        val proxyIp = networkManager.getProxyIpAddress(executionId)
        logger.info("Using proxy IP: $proxyIp for task: $executionId")

        runCatching {
            tempDir.grantAllPermissions()
            hiddenTargetFile.grantAllPermissions()
            gradleCacheDir.grantAllPermissions()
        }

        var resultsJson: String? = null
        val runner =
            dockerRunner("jikvict-solution-runner") {
                withNetwork(networkId)

                withEnvs(
                    env("GRADLE_USER_HOME", "/gradle-cache"),
                    env(
                        "GRADLE_OPTS",
                        "-Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Xmx512m -Dhttp.proxyHost=$proxyIp -Dhttp.proxyPort=3128 -Dhttps.proxyHost=$proxyIp -Dhttps.proxyPort=3128",
                    ),
                    env("JAVA_OPTS", "-Xmx256m -Xms128m -Dhttp.proxyHost=$proxyIp -Dhttp.proxyPort=3128 -Dhttps.proxyHost=$proxyIp -Dhttps.proxyPort=3128"),
                    env("RESULTS_OUTPUT_DIR", "/app/input"),
                    env("http_proxy", "http://$proxyIp:3128"),
                    env("https_proxy", "http://$proxyIp:3128"),
                    env("HTTP_PROXY", "http://$proxyIp:3128"),
                    env("HTTPS_PROXY", "http://$proxyIp:3128"),
                )

                withBinds(
                    Bind(tempDir.toString(), Volume("/app/input")),
                    Bind(gradleCacheDir.toString(), Volume("/gradle-cache")),
                )
                withMountedFilesConsumers(
                    { paths ->
                        val resultFiles =
                            paths
                                .flatMap { pathsIt -> Files.walk(pathsIt).toList() }
                                .filter { Files.isRegularFile(it) }
                                .filter { name -> name.fileName.toString() == "jikvict-results.json" }
                                .toList()

                        runCatching {
                            if (resultFiles.isNotEmpty()) {
                                val resultsFile = resultFiles.first()
                                resultsJson = Files.readString(resultsFile)
                            } else {
                                val jsonFiles =
                                    Files
                                        .walk(tempDir)
                                        .asSequence()
                                        .filter { Files.isRegularFile(it) }
                                        .filter { it.fileName.toString().endsWith(".json") }
                                        .toList()

                                if (jsonFiles.isNotEmpty()) {
                                    val resultsFile = jsonFiles.first()
                                    resultsJson = Files.readString(resultsFile)
                                }
                            }
                        }
                    },
                    { it.forEach(::cleanupDirectory) },
                )

                withLogsConsumers({ println(it.utf8String) })

                withCpuQuota(assignmentDto.cpuLimit)
                withMemory(assignmentDto.memoryLimit)
                withPidsLimit(assignmentDto.pidsLimit)
                withTimeout(assignmentDto.timeOutSeconds.seconds)

                runCommand(
                    "/app/input/${targetFile.fileName}",
                    "/app/input/${hiddenTargetFile.fileName}",
                    assignmentDto.timeOutSeconds.toString(),
                    "/app/input/jikvict-results.json",
                )
            }

        try {
            runner.run(isActive)

            val results =
                runCatching {
                    objectMapper.readValue(resultsJson, TestSuiteResult::class.java)
                }.onFailure {
                    throw ServiceException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to parse test results, probably the solution is invalid and no tests were executed",
                    )
                }.getOrNull()!!

            return results
        } finally {
            try {
                networkManager.cleanupTaskNetwork(executionId)
                logger.info("Cleaned up network for task: $executionId")
            } catch (e: Exception) {
                logger.error("Failed to cleanup network for task: $executionId", e)
            }
        }
    }

    private fun cleanupDirectory(directory: Path) {
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
