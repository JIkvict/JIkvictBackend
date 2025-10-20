package org.jikvict.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.model.Frame
import org.apache.logging.log4j.Logger
import org.testcontainers.DockerClientFactory
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class NetworkManager(
    private val logger: Logger,
) {
    private val docker: DockerClient = DockerClientFactory.instance().client()
    private val managedNetworks = ConcurrentHashMap<String, String>()
    private val proxyContainers = ConcurrentHashMap<String, String>()
    private val ips = ConcurrentHashMap<String, String>()
    private val squidImage = "ubuntu/squid:5.2-22.04_beta"

    private val squidConfig = """
    http_port 0.0.0.0:3128


    acl localnet src 172.0.0.0/8
    acl localnet src 10.0.0.0/8
    acl localnet src 192.168.0.0/16


    acl allowed_sites dstdomain .jitpack.io
    acl allowed_sites dstdomain .repo1.maven.org
    acl allowed_sites dstdomain .repo.maven.apache.org
    acl allowed_sites dstdomain .central.maven.org
    acl allowed_sites dstdomain .plugins.gradle.org


    acl SSL_ports port 443
    acl Safe_ports port 80
    acl Safe_ports port 443
    acl CONNECT method CONNECT


    http_access allow localnet CONNECT SSL_ports allowed_sites
    http_access allow localnet Safe_ports allowed_sites
    http_access deny all


    cache deny all


    dns_nameservers 8.8.8.8 8.8.4.4


    access_log none
    cache_store_log none
    cache_log /dev/null


    shutdown_lifetime 3 seconds
    forwarded_for off
    via off
""".trimIndent()


    fun createIsolatedNetwork(taskId: String): String {
        val networkName = "jikvict-task-$taskId"

        return managedNetworks.computeIfAbsent(networkName) {
            logger.info("Creating isolated network: $networkName")

            val networkId = docker.createNetworkCmd()
                .withName(networkName)
                .withDriver("bridge")
                .exec()
                .id

            logger.info("Created network $networkId for $taskId")

            val proxyId = startProxy(networkName, networkId)
            proxyContainers[networkName] = proxyId

            networkId
        }
    }

    private fun startProxy(networkName: String, networkId: String): String {
        val containerName = "${networkName}-proxy"

        val configDir: Path = Files.createTempDirectory("squid-conf-")
        val configFile = configDir.resolve("squid.conf")
        Files.writeString(configFile, squidConfig)

        logger.info("Starting Squid proxy $containerName")


        val container: CreateContainerResponse = docker.createContainerCmd(squidImage)
            .withName(containerName)
            .withCmd("sh", "-c", "echo '$squidConfig' > /etc/squid/squid.conf && squid -N -f /etc/squid/squid.conf")
            .exec()


        val logsCallback = object : ResultCallback<Frame> {
            override fun close() {}
            override fun onStart(stream: Closeable?) {
                logger.debug("Started collecting logs for container ${container.id}")
            }

            override fun onNext(frame: Frame?) {
                frame?.let {
                    val logLine = String(it.payload).trim()
                    logger.info("Squid [${container.id.take(12)}]: $logLine")
                }
            }

            override fun onError(throwable: Throwable?) {
                throwable?.let { logger.error("Squid logs error: ${it.message}") }
            }

            override fun onComplete() {
                logger.debug("Squid logs completed for ${container.id}")
            }
        }

        docker.startContainerCmd(container.id).exec()

        docker.logContainerCmd(container.id)
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true)
            .withTimestamps(true)
            .exec(logsCallback)


        Thread.sleep(2000)


        val containerState = docker.inspectContainerCmd(container.id).exec().state
        containerState.running?.let {
            if (!it) {
                val exitCode = containerState.exitCodeLong
                logger.error("Container ${container.id} failed to start. Exit code: $exitCode")
                throw RuntimeException("Squid container failed to start with exit code: $exitCode")
            }
        }


        try {
            docker.connectToNetworkCmd()
                .withContainerId(container.id)
                .withNetworkId(networkId)
                .exec()
            logger.info("Successfully connected proxy ${container.id} to network $networkId")
        } catch (e: Exception) {
            logger.error("Failed to connect proxy to network $networkId", e)
            throw e
        }

        val containerInfo = docker.inspectContainerCmd(container.id).exec()
        val networks = containerInfo.networkSettings.networks
        logger.info("Proxy container networks: ${networks.keys}")
        networks[networkName]?.let { networkInfo ->
            logger.info("Proxy IP in network $networkName: ${networkInfo.ipAddress}")
            ips[networkName] = networkInfo.ipAddress!!
        }

        logger.info("Squid proxy started: ${container.id}")

        return container.id
    }

    fun cleanupTaskNetwork(taskId: String) {
        val networkName = "jikvict-task-$taskId"

        proxyContainers.remove(networkName)?.let { id ->
            runCatching {
                docker.stopContainerCmd(id).exec()
            }.onFailure {
                logger.warn("Failed to stop proxy for $taskId", it)
            }
            runCatching {
                docker.removeContainerCmd(id).exec()
                logger.info("Removed proxy container for $taskId")
            }.onFailure {
                logger.warn("Failed to cleanup proxy for $taskId", it)
            }
        }

        managedNetworks.remove(networkName)?.let { id ->
            try {
                docker.removeNetworkCmd(id).exec()
                logger.info("Removed network for $taskId")
            } catch (e: Exception) {
                logger.warn("Failed to cleanup network for $taskId", e)
            }
        }
    }

    fun getProxyIpAddress(taskId: String): String {
        val networkName = "jikvict-task-$taskId"
        return ips[networkName]!!
    }

}
