package org.jikvict.jikvictbackend.service

import com.github.dockerjava.api.command.WaitContainerResultCallback
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Service
class SolutionChecker(
    private val logger: Logger
) {

    fun executeCode(file: MultipartFile, timeoutSeconds: Long) {
        val executionId = UUID.randomUUID().toString()
        val tempDir = Files.createTempDirectory("code-$executionId")

        val targetFile = tempDir.resolve(file.originalFilename ?: "solution.jar")
        file.transferTo(targetFile.toFile())

        return try {
            val dockerImage = "openjdk:26-oracle"

            GenericContainer(dockerImage).apply {
                withFileSystemBind(tempDir.toString(), "/app", BindMode.READ_ONLY)
                withWorkingDirectory("/app")
                withCreateContainerCmdModifier { cmd ->
                    cmd.hostConfig?.apply {
                        withMemory(128 * 1024 * 1024L)
                        withCpuQuota(50000L)
                        withPidsLimit(50L)
                        withNetworkMode("none")
                        withReadonlyRootfs(true)
                        withTmpFs(mapOf("/tmp" to "size=10m"))
                    }
                }
                withCommand("java", "-jar", file.originalFilename ?: "solution.jar")
                withStartupTimeout(30.seconds.toJavaDuration())
            }.use { container ->
                container.start()
                logger.info("Контейнер запущен: ${container.containerId}")

                val exitCode = try {
                    container.dockerClient.waitContainerCmd(container.containerId)
                        .exec(WaitContainerResultCallback())
                        .awaitStatusCode(timeoutSeconds, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    logger.error("Таймаут или ошибка при ожидании завершения контейнера", e)
                    container.stop()
                    -1
                }

                val logs = container.logConsumers

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
        }
    }


    fun cleanupDirectory(directory: java.nio.file.Path) {
        try {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        } catch (e: Exception) {
            logger.error("Ошибка при очистке временной директории", e)
        }
    }
}
