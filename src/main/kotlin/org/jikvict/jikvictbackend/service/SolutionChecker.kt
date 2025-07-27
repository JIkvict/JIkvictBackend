package org.jikvict.jikvictbackend.service

import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import org.apache.logging.log4j.Logger
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
) {
    fun checkSolution(
        taskId: Int,
        solution: ByteArray,
        timeoutSeconds: Long,
    ) {
        logger.info("Retrieving hidden files for assignment $taskId")
        val hiddenFilesBytes = assignmentService.getHiddenFilesForTask(taskId)

        logger.info("Retrieving exposed files for assignment $taskId")

        executeCode(solution, hiddenFilesBytes, timeoutSeconds)
    }

    fun executeCode(
        solution: ByteArray,
        hiddenFiles: ByteArray,
        timeoutSeconds: Long,
    ) {
        val executionId = UUID.randomUUID().toString()
        val tempDir = Files.createTempDirectory("code-$executionId")

        val targetFile = tempDir.resolve("solution")
        Files.write(targetFile, solution)

        val hiddenTargetFile = tempDir.resolve("hidden-files")
        Files.write(hiddenTargetFile, hiddenFiles)

        val gradleCacheDir = Path.of("/tmp/gradle-cache", executionId)
        Files.createDirectories(gradleCacheDir)
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

            // Set permissions for main file
            Files.setPosixFilePermissions(
                targetFile,
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.OTHERS_READ,
                ),
            )

            // Set permissions for hidden files
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
            logger.warn("Не удалось установить POSIX права доступа: ${e.message}")
        }

        logger.info("Файл сохранен: ${targetFile.toAbsolutePath()}")
        logger.info("Размер файла: ${Files.size(targetFile)} байт")

        logger.info("Скрытый файл сохранен: ${hiddenTargetFile.toAbsolutePath()}")
        logger.info("Размер скрытого файла: ${Files.size(hiddenTargetFile)} байт")

        return try {
            val dockerImage = "jikvict-solution-runner"

            GenericContainer(dockerImage)
                .apply {
                    withFileSystemBind(tempDir.toString(), "/app/input", BindMode.READ_WRITE)
                    withWorkingDirectory("/app")

                    withCreateContainerCmdModifier { cmd ->
                        cmd.hostConfig?.apply {
                            withMemory(1024 * 1024 * 1024L) // Memory limit (1GB)
                            withCpuQuota(1000000L) // CPU quota
                            withPidsLimit(500L) // Process limit
                            withNetworkMode("bridge") // Bridge network mode
                            withReadonlyRootfs(false) // Allow write operations in the container filesystem
                            withTmpFs(
                                mapOf(
                                    "/tmp" to "size=1g",
                                    "/root" to "size=1g",
                                    "/var/tmp" to "size=1g",
                                ),
                            )
                            // Remove specific "repo.maven.apache.org" host
                            // Use default DNS as configured in container environment
                            withBinds(
                                Bind(tempDir.toString(), Volume("/app/input")),
                                Bind(gradleCacheDir.toString(), Volume("/gradle-cache")),
                            )
                        }
                    }

                    // Set Gradle user home to use mounted cache
                    withEnv("GRADLE_USER_HOME", "/gradle-cache")
                    withEnv("GRADLE_OPTS", "-Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Xmx512m")
                    withEnv("JAVA_OPTS", "-Xmx256m -Xms128m")

                    // Remove restrictive Maven repository configuration
                    // Maven will now use default repositories configured in the build system
                    // withEnv("MAVEN_OPTS", "-Dmaven.repo.remote=https://repo.maven.apache.org/maven2")

                    withCommand(
                        "/app/input/${targetFile.fileName}",
                        "/app/input/${hiddenTargetFile.fileName}",
                        timeoutSeconds.toString(),
                    )
                    withStartupTimeout(30.seconds.toJavaDuration())
                }.use { container ->
                    container.start()
                    logger.info("Контейнер запущен: ${container.containerId}")

                    // Проверим, что файл действительно доступен в контейнере
                    try {
                        val lsResult = container.execInContainer("ls", "-la", "/app/input/")
                        logger.info("Содержимое /app/input/: ${lsResult.stdout}")

                        // Also check the Gradle cache directory
                        val gradleCacheResult = container.execInContainer("ls", "-la", "/gradle-cache/")
                        logger.info("Содержимое /gradle-cache/: ${gradleCacheResult.stdout}")
                    } catch (e: Exception) {
                        logger.warn("Не удалось проверить содержимое директории: ${e.message}")
                    }

                    val exitCode =
                        try {
                            container.dockerClient
                                .waitContainerCmd(container.containerId)
                                .exec(WaitContainerResultCallback())
                                .awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS)
                        } catch (e: Exception) {
                            logger.error("Таймаут или ошибка при ожидании завершения контейнера", e)
                            container.stop()
                            -1
                        }

                    val logs = container.logs

                    when (exitCode) {
                        0 -> {
                            logger.info("Код выполнен успешно. Код выхода: $exitCode")
                            logger.info("Логи выполнения:\n$logs")
                        }

                        -1 -> {
                            logger.error("Время выполнения кода истекло. Контейнер был остановлен принудительно")
                            logger.error("Логи выполнения:\n$logs")
                        }

                        else -> {
                            logger.error("Код завершился с ошибкой. Код выхода: $exitCode")
                            logger.error("Логи выполнения:\n$logs")
                        }
                    }
                }
        } catch (e: Exception) {
            logger.error("Ошибка выполнения кода", e)
        } finally {
            cleanupDirectory(tempDir)
            cleanupDirectory(gradleCacheDir)
        }
    }

    fun cleanupDirectory(directory: Path) {
        try {
            Files
                .walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        } catch (e: Exception) {
            logger.error("Ошибка при очистке временной директории", e)
        }
    }
}
