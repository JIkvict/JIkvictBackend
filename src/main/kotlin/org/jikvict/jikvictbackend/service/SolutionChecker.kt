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
    ): String? {
        logger.info("Retrieving hidden files for assignment $taskId")
        val hiddenFilesBytes = assignmentService.getHiddenFilesForTask(taskId)

        logger.info("Retrieving exposed files for assignment $taskId")

        return executeCode(solution, hiddenFilesBytes, timeoutSeconds)
    }

    fun executeCode(
        solution: ByteArray,
        hiddenFiles: ByteArray,
        timeoutSeconds: Long,
    ): String? {
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
            logger.warn("Не удалось установить POSIX права доступа: ${e.message}")
        }

        logger.info("Файл сохранен: ${targetFile.toAbsolutePath()}")
        logger.info("Размер файла: ${Files.size(targetFile)} байт")
        logger.info("Скрытый файл сохранен: ${hiddenTargetFile.toAbsolutePath()}")
        logger.info("Размер скрытого файла: ${Files.size(hiddenTargetFile)} байт")

        try {
            val dockerImage = "jikvict-solution-runner"

            GenericContainer(dockerImage).apply {
                // Монтируем директорию как READ_WRITE, чтобы контейнер мог писать результаты
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

                // Добавляем переменную окружения для указания, куда сохранять результаты
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
                logger.info("Контейнер запущен: ${container.containerId}")

                val exitCode = try {
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

                // Ищем результаты в примонтированной директории
                try {
                    logger.info("Поиск результатов в примонтированной директории...")

                    // Сначала покажем все файлы для диагностики
                    logger.info("Все файлы в tempDir:")
                    Files.walk(tempDir)
                        .filter { Files.isRegularFile(it) }
                        .forEach { logger.info("  - ${it.fileName} (${Files.size(it)} bytes)") }

                    // Ищем файл результатов
                    val resultFiles = Files.walk(tempDir)
                        .filter { Files.isRegularFile(it) }
                        .filter { it.fileName.toString() == "jikvict-results.json" }
                        .toList()

                    if (resultFiles.isNotEmpty()) {
                        val resultsFile = resultFiles.first()
                        resultsJson = Files.readString(resultsFile)
                        logger.info("Результаты найдены в примонтированной директории")
                        logger.info("Содержимое: $resultsJson")
                    } else {
                        // Попробуем найти любые JSON файлы
                        val jsonFiles = Files.walk(tempDir)
                            .filter { Files.isRegularFile(it) }
                            .filter { it.fileName.toString().endsWith(".json") }
                            .toList()

                        if (jsonFiles.isNotEmpty()) {
                            logger.info("Найдены JSON файлы: ${jsonFiles.map { it.fileName }}")
                            val resultsFile = jsonFiles.first()
                            resultsJson = Files.readString(resultsFile)
                            logger.info("Использован первый найденный JSON файл")
                        } else {
                            logger.warn("Файлы результатов не найдены")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Ошибка при поиске результатов", e)
                }
            }
        } catch (e: Exception) {
            logger.error("Ошибка выполнения кода", e)
        } finally {
            cleanupDirectory(tempDir)
            cleanupDirectory(gradleCacheDir)
        }

        return resultsJson
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
