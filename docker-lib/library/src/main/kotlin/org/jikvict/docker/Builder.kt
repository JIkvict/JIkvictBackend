package org.jikvict.docker

import com.github.dockerjava.api.model.Bind
import org.jikvict.docker.consumer.ExitCodeConsumer
import org.jikvict.docker.consumer.LogsConsumer
import org.jikvict.docker.consumer.MountedFilesConsumer
import kotlin.time.Duration

fun dockerRunner(dockerImage: String, init: DockerRunnerBuilder.() -> Unit): DockerRunner {
    val builder = DockerRunnerBuilder(dockerImage)
    builder.init()
    return builder.build()
}

fun env(key: String, value: String) = DockerEnv(key, value)

@Suppress("unused")
data class DockerRunnerBuilder(
    private val dockerImage: String,
    private val runCommand: MutableList<String> = mutableListOf(),
    private val logsConsumers: MutableList<LogsConsumer> = mutableListOf(),
    private val exitCodeConsumers: MutableList<ExitCodeConsumer> = mutableListOf(),
    private val mountedFilesConsumers: MutableList<MountedFilesConsumer> = mutableListOf(),
    private var memory: Long? = null,
    private var cpuQuota: Long? = null,
    private var pidsLimit: Long? = null,
    private var timeout: Duration? = null,
    private val binds: MutableList<Bind> = mutableListOf(),
    private val envs: MutableList<DockerEnv> = mutableListOf(),
    private var networkName: String? = null,
) {
    fun runCommand(vararg parts: String) = apply { runCommand.addAll(parts) }
    fun runCommand(parts: List<String>) = apply { runCommand.addAll(parts) }

    fun withMemory(memory: Long) = apply { this.memory = memory }
    fun withCpuQuota(cpuQuota: Long) = apply { this.cpuQuota = cpuQuota }
    fun withPidsLimit(pidsLimit: Long) = apply { this.pidsLimit = pidsLimit }
    fun withResources(memory: Long, cpuQuota: Long, pidsLimit: Long) = apply {
        this.memory = memory; this.cpuQuota = cpuQuota; this.pidsLimit = pidsLimit
    }

    fun withTimeout(timeout: Duration) = apply { this.timeout = timeout }

    fun withBinds(vararg binds: Bind) = apply { this.binds.addAll(binds) }
    fun withBinds(binds: List<Bind>) = apply { this.binds.addAll(binds) }

    fun withEnv(key: String, value: String) = apply { this.envs.add(DockerEnv(key, value)) }
    fun withEnvs(vararg envs: DockerEnv) = apply { this.envs.addAll(envs) }
    fun withEnvs(envs: List<DockerEnv>) = apply { this.envs.addAll(envs) }

    fun withNetwork(networkName: String) = apply { this.networkName = networkName }

    fun withLogsConsumers(vararg logsConsumers: LogsConsumer) = apply { this.logsConsumers.addAll(logsConsumers) }
    fun withExitCodeConsumers(vararg exitCodeConsumer: ExitCodeConsumer) = apply { this.exitCodeConsumers.addAll(exitCodeConsumer) }
    fun withMountedFilesConsumers(vararg mountedFilesConsumer: MountedFilesConsumer) = apply { this.mountedFilesConsumers.addAll(mountedFilesConsumer) }

    fun build(): DockerRunner {
        require(runCommand.isNotEmpty()) { "Run command must be set" }

        return DockerRunner(
            dockerImage = dockerImage,
            runCommand = runCommand.toList(),
            logsConsumers = logsConsumers.toList(),
            exitCodeConsumers = exitCodeConsumers.toList(),
            mountedFilesConsumers = mountedFilesConsumers.toList(),
            memory = requireNotNull(memory) { "Memory must be set" },
            cpuQuota = requireNotNull(cpuQuota) { "CPU quota must be set" },
            pidsLimit = requireNotNull(pidsLimit) { "PIDs limit must be set" },
            timeout = requireNotNull(timeout) { "Timeout must be set" },
            binds = binds.toList(),
            envs = envs.toList(),
            networkName = networkName,
        )
    }
}
