package org.jikvict.jikvictbackend.configuration

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.service.processor.AssignmentTaskProcessor
import org.jikvict.jikvictbackend.service.processor.SubmissionCheckerTaskProcessor
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener

/**
 * Configuration for registering task processors with the TaskRegistry
 */
@Configuration
class TaskProcessorConfiguration(
    private val taskRegistry: TaskRegistry,
    private val assignmentTaskProcessor: AssignmentTaskProcessor,
    private val verificationTaskProcessor: SubmissionCheckerTaskProcessor,
    private val log: Logger,
) {
    /**
     * Register task processors with the TaskRegistry when the application context is refreshed
     */
    @EventListener(ContextRefreshedEvent::class)
    fun registerTaskProcessors() {
        log.info("Registering task processors with the TaskRegistry")

        // Register the assignment task processor
        taskRegistry.registerProcessor(assignmentTaskProcessor)
        log.info("Registered assignment task processor: ${assignmentTaskProcessor.taskType}")

        // Register the verification task processor
        taskRegistry.registerProcessor(verificationTaskProcessor)
        log.info("Registered verification task processor: ${verificationTaskProcessor.taskType}")
    }
}

@Configuration
class RabbitListenerManualAckConfig {
    @Bean(name = ["manualAckContainerFactory"])
    fun manualAckContainerFactory(
        connectionFactory: ConnectionFactory,
        rabbitMessageConverter: MessageConverter,
    ): SimpleRabbitListenerContainerFactory =
        SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(connectionFactory)
            setAcknowledgeMode(AcknowledgeMode.MANUAL)
            setPrefetchCount(1)
            setConcurrentConsumers(3)
            setMaxConcurrentConsumers(10)
            setMessageConverter(rabbitMessageConverter)
        }
}
