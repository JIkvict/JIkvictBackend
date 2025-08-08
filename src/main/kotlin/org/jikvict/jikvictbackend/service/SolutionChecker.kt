package org.jikvict.jikvictbackend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import org.apache.logging.log4j.Logger
import org.jikvict.docker.dockerRunner
import org.jikvict.docker.env
import org.jikvict.docker.util.grantAllPermissions
import org.jikvict.jikvictbackend.model.dto.AssignmentDto
import org.jikvict.jikvictbackend.model.mapper.AssignmentMapper
import org.jikvict.problems.exception.contract.ServiceException
import org.jikvict.testing.model.TestSuiteResult
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Service
class SolutionChecker(
    private val logger: Logger,
    private val assignmentService: AssignmentService,
    private val objectMapper: ObjectMapper,
    private val assignmentMapper: AssignmentMapper,
) {
    @Transactional
    suspend fun checkSolution(
        taskId: Int,
        solution: ByteArray,
        assignmentId: Long,
    ): TestSuiteResult {
        logger.info("Retrieving hidden files for assignment $taskId")
        val hiddenFilesBytes = assignmentService.getHiddenFilesForTask(taskId)
        val assignmentDto = assignmentMapper.toDto(assignmentService.getAssignmentById(assignmentId))
        return e(solution, hiddenFilesBytes, assignmentDto)
    }

    @Transactional
    suspend fun e(
        solution: ByteArray,
        hiddenFiles: ByteArray,
        assignmentDto: AssignmentDto,
    ): TestSuiteResult {
        val executionId = UUID.randomUUID().toString()
        val tempDir = Files.createTempDirectory("code-$executionId")
        val targetFile = tempDir.resolve("solution")
        Files.write(targetFile, solution)
        val hiddenTargetFile = tempDir.resolve("hidden-files")
        Files.write(hiddenTargetFile, hiddenFiles)
        val gradleCacheDir = Path.of("/tmp/gradle-cache", executionId)
        Files.createDirectories(gradleCacheDir)

        runCatching {
            tempDir.grantAllPermissions()
            hiddenTargetFile.grantAllPermissions()
            gradleCacheDir.grantAllPermissions()
        }

        var resultsJson: String? = null
        val runner =
            dockerRunner("jikvict-solution-runner") {
                withEnvs(
                    env("GRADLE_USER_HOME", "/gradle-cache"),
                    env("GRADLE_OPTS", "-Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Xmx512m"),
                    env("JAVA_OPTS", "-Xmx256m -Xms128m"),
                    env("RESULTS_OUTPUT_DIR", "/app/input"),
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
                            }
                        }
                    },
                    { it.forEach(::cleanupDirectory) },
                )

                withLogsConsumers({ print(it) })

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

        runner.run()

        val results =
            runCatching {
                objectMapper.readValue(resultsJson, TestSuiteResult::class.java)
            }.onFailure {
                logger.error("Failed to parse results", it)
                throw ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse results")
            }.getOrNull()!!

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
