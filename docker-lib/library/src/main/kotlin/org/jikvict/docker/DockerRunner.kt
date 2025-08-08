package org.jikvict.docker

import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Bind
import org.jikvict.docker.consumer.ExitCodeConsumer
import org.jikvict.docker.consumer.LogsConsumer
import org.jikvict.docker.consumer.MountedFilesConsumer
import org.testcontainers.containers.GenericContainer
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class DockerRunner(
    private val dockerImage: String,
    private val runCommand: List<String>,
    private val logsConsumers: List<LogsConsumer> = emptyList(),
    private val exitCodeConsumers: List<ExitCodeConsumer> = emptyList(),
    private val mountedFilesConsumers: List<MountedFilesConsumer> = emptyList(),
    private val memory: Long,
    private val cpuQuota: Long,
    private val pidsLimit: Long,
    private val timeout: Duration,
    private val binds: List<Bind> = emptyList(),
    private val envs: List<DockerEnv> = emptyList(),
) {
    suspend fun run() {
        var container: GenericContainer<*>? = null
        try {
            container = withContext(Dispatchers.IO) {
                GenericContainer(dockerImage).apply {
                    withWorkingDirectory("/app")
                    withCreateContainerCmdModifier { cmd ->
                        cmd.hostConfig?.apply {
                            withMemory(this@DockerRunner.memory)
                            withCpuQuota(this@DockerRunner.cpuQuota)
                            withPidsLimit(this@DockerRunner.pidsLimit)
                            withNetworkMode("bridge")
                            withReadonlyRootfs(false)
                            withTmpFs(
                                mapOf(
                                    "/tmp" to "size=1g",
                                    "/root" to "size=1g",
                                    "/var/tmp" to "size=1g",
                                ),
                            )
                            withBinds(*this@DockerRunner.binds.toTypedArray())
                        }
                    }
                    envs.forEach { (k, v) -> withEnv(k, v) }
                    withCommand(*runCommand.toTypedArray())
                    withStartupTimeout(30.seconds.toJavaDuration())
                }.also {
                    it.start()
                    it.logConsumers.addAll(logsConsumers)
                }
            }

            val exitCode = withTimeout(timeout) {
                withContext(Dispatchers.IO) {
                    container.dockerClient
                        .waitContainerCmd(container.containerId)
                        .exec(WaitContainerResultCallback())
                        .awaitStatusCode(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                }
            }

            exitCodeConsumers.forEach { it.accept(exitCode) }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            throw t
        } finally {
            withContext(Dispatchers.IO) {
                runCatching { container?.stop() }
                val resultFiles = binds.mapNotNull { it.path }.map { Path.of(it) }
                mountedFilesConsumers.forEach { it.accept(resultFiles) }
            }
        }
    }
}

data class DockerEnv(val key: String, val value: String)
