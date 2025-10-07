package org.jikvict.jikvictbackend.configuration

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for registering task processors with the TaskRegistry
 */
@Configuration
class TaskProcessorConfiguration(
    private val taskRegistry: TaskRegistry,
    private val log: Logger,
) {

}

@Configuration
class RabbitListenerManualAckConfig(
    private val rabbitMQProperties: RabbitMQProperties,
) {
    @Bean(name = ["manualAckContainerFactory"])
    fun manualAckContainerFactory(
        connectionFactory: ConnectionFactory,
        rabbitMessageConverter: MessageConverter,
    ): SimpleRabbitListenerContainerFactory =
        SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(connectionFactory)
            setAcknowledgeMode(AcknowledgeMode.MANUAL)
            setPrefetchCount(1)
            setConcurrentConsumers(rabbitMQProperties.defaultDockerWorkers)
            setMaxConcurrentConsumers(rabbitMQProperties.maxDockerWorkers)
            setMessageConverter(rabbitMessageConverter)
        }
}
