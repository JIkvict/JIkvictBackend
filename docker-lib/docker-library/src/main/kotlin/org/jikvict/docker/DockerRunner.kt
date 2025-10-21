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
    private val networkName: String? = null,
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
                            withNanoCPUs(this@DockerRunner.cpuQuota)
                            withPidsLimit(this@DockerRunner.pidsLimit)

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
                    logsConsumers.forEach { withLogConsumer(it) }
                }.also { container ->
                    container.start()

                    if (!this@DockerRunner.networkName.isNullOrBlank()) {
                        try {
                            println("Connecting container ${container.containerId} to network ${this@DockerRunner.networkName}")
                            container.dockerClient.connectToNetworkCmd()
                                .withContainerId(container.containerId)
                                .withNetworkId(this@DockerRunner.networkName)
                                .exec()


                            Thread.sleep(2000)
                            val containerInfo = container.dockerClient.inspectContainerCmd(container.containerId).exec()
                            val networks = containerInfo.networkSettings.networks
                            println("Solution container networks: ${networks.keys}")


                            val networkName = networks.keys.find { it.startsWith("jikvict-task-") }
                            if (networkName != null) {
                                val proxyName = "$networkName-proxy"
                                println("Attempting to resolve proxy: $proxyName")


                                try {
                                    val execResult = container.dockerClient.execCreateCmd(container.containerId)
                                        .withCmd("nslookup", proxyName)
                                        .withAttachStdout(true)
                                        .withAttachStderr(true)
                                        .exec()

                                    println("DNS lookup result for $proxyName: $execResult")
                                } catch (e: Exception) {
                                    println("DNS lookup failed: ${e.message}")
                                }
                            }

                            println("Successfully connected to network ${this@DockerRunner.networkName}")
                        } catch (e: Exception) {
                            println("Failed to connect to network ${this@DockerRunner.networkName}: ${e.message}")
                            throw e
                        }
                    }
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
