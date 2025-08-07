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
    @Bean
    fun registerQueues(): List<Queue> =
        taskRegistry.getAllProcessors().map { processor ->
            log.info("Registering queue for task type: ${processor.taskType}")
            Queue(processor.queueName, true)
        }

    @Bean
    fun registerExchanges(): List<DirectExchange> =
        taskRegistry.getAllProcessors().map { processor ->
            log.info("Registering exchange for task type: ${processor.taskType}")
            DirectExchange(processor.exchangeName)
        }

    @Bean
    fun registerBindings(): List<Binding> =
        taskRegistry.getAllProcessors().map { processor ->
            log.info("Registering binding for task type: ${processor.taskType}")
            BindingBuilder
                .bind(Queue(processor.queueName, true))
                .to(DirectExchange(processor.exchangeName))
                .with(processor.routingKey)
        }

    @Bean
    fun jsonMessageConverter(): MessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = jsonMessageConverter()
        return rabbitTemplate
    }
}
