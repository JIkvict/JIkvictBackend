package org.jikvict.jikvictbackend.configuration

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfiguration(
    private val taskRegistry: TaskRegistry,
    private val log: Logger,
) {
    companion object {
        // Legacy constants for backward compatibility
        const val ASSIGNMENT_QUEUE = "assignment.queue"
        const val ASSIGNMENT_EXCHANGE = "assignment.exchange"
        const val ASSIGNMENT_ROUTING_KEY = "assignment.routingkey"

        // Verification constants
        const val VERIFICATION_QUEUE = "verification.queue"
        const val VERIFICATION_EXCHANGE = "verification.exchange"
        const val VERIFICATION_ROUTING_KEY = "verification.routingkey"
    }

    /**
     * Register queues, exchanges, and bindings for all registered task processors
     */
    @Bean
    fun registerQueues(): List<Queue> =
        taskRegistry.getAllProcessors().map { processor ->
            log.info("Registering queue for task type: ${processor.taskType}")
            Queue(processor.queueName, true)
        }

    /**
     * Register exchanges for all registered task processors
     */
    @Bean
    fun registerExchanges(): List<DirectExchange> =
        taskRegistry.getAllProcessors().map { processor ->
            log.info("Registering exchange for task type: ${processor.taskType}")
            DirectExchange(processor.exchangeName)
        }

    /**
     * Register bindings for all registered task processors
     */
    @Bean
    fun registerBindings(): List<Binding> =
        taskRegistry.getAllProcessors().map { processor ->
            log.info("Registering binding for task type: ${processor.taskType}")
            BindingBuilder
                .bind(Queue(processor.queueName, true))
                .to(DirectExchange(processor.exchangeName))
                .with(processor.routingKey)
        }

    /**
     * For backward compatibility
     */
    @Bean
    fun queue(): Queue = Queue(ASSIGNMENT_QUEUE, true)

    /**
     * For backward compatibility
     */
    @Bean
    fun exchange(): DirectExchange = DirectExchange(ASSIGNMENT_EXCHANGE)

    /**
     * For backward compatibility
     */
    @Bean
    fun binding(
        queue: Queue,
        exchange: DirectExchange,
    ): Binding = BindingBuilder.bind(queue).to(exchange).with(ASSIGNMENT_ROUTING_KEY)

    /**
     * Explicitly declare verification queue to ensure it exists before listeners connect
     */
    @Bean
    fun verificationQueue(): Queue = Queue(VERIFICATION_QUEUE, true)

    /**
     * Explicitly declare verification exchange
     */
    @Bean
    fun verificationExchange(): DirectExchange = DirectExchange(VERIFICATION_EXCHANGE)

    /**
     * Explicitly declare verification binding
     */
    @Bean
    fun verificationBinding(
        verificationQueue: Queue,
        verificationExchange: DirectExchange,
    ): Binding = BindingBuilder.bind(verificationQueue).to(verificationExchange).with(VERIFICATION_ROUTING_KEY)

    @Bean
    fun jsonMessageConverter(): MessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = jsonMessageConverter()
        return rabbitTemplate
    }
}
