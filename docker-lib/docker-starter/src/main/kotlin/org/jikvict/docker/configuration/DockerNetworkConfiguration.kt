package org.jikvict.docker.configuration

import org.apache.logging.log4j.LogManager
import org.jikvict.docker.NetworkManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DockerNetworkConfiguration {

    @Bean
    fun networkManager(): NetworkManager {
        val logger = LogManager.getLogger(NetworkManager::class.java)
        return NetworkManager(logger)
    }
}
