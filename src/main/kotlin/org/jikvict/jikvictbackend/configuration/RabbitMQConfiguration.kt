package org.jikvict.jikvictbackend.configuration

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Declarable
import org.springframework.amqp.core.Declarables
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.MessageDeliveryMode
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener

@Configuration
class RabbitMQConfiguration(
    private val taskRegistry: TaskRegistry,
    private val log: Logger,
) {
    @EventListener(ContextRefreshedEvent::class)
    fun logTaskRegistry() {
        val processors = taskRegistry.getAllProcessors()
        log.info("TaskRegistry contains ${processors.size} processors:")
        processors.forEach { processor ->
            log.info("Processor: ${processor.taskType}, Queue: ${processor.queueName}, Exchange: ${processor.exchangeName}")
        }
    }

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        val admin = RabbitAdmin(connectionFactory)
        admin.isAutoStartup = true
        return admin
    }

    @Bean("rabbitDeclarables")
    fun rabbitDeclarables(): Declarables {
        val processors = taskRegistry.getAllProcessors()
        log.info("Creating ${processors.size} queues, exchanges and bindings")

        val all = mutableListOf<Declarable>()
        processors.forEach { processor ->
            log.info("Registering queue: ${processor.queueName}")
            val queue = Queue(processor.queueName, true)
            log.info("Registering exchange for task type: ${processor.taskType}")
            val exchange = DirectExchange(processor.exchangeName)
            log.info("Registering binding for task type: ${processor.taskType}")
            val binding = BindingBuilder.bind(queue).to(exchange).with(processor.routingKey)
            all.add(queue)
            all.add(exchange)
            all.add(binding)
        }
        return Declarables(*all.toTypedArray())
    }

    @Bean
    fun messageConverter(): MessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        messageConverter: MessageConverter,
    ): RabbitTemplate =
        RabbitTemplate(connectionFactory).apply {
            setMessageConverter(messageConverter)
            setBeforePublishPostProcessors(
                { message ->
                    message.messageProperties.deliveryMode = MessageDeliveryMode.PERSISTENT
                    message
                },
            )
        }
}

@ConfigurationProperties("rabbitmq")
data class RabbitMQProperties(
    val defaultDockerWorkers: Int = 3,
    val maxDockerWorkers: Int = 10,
)
