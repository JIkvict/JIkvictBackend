package org.jikvict.jikvictbackend.service.queue

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.repository.TaskStatusRepository
import org.jikvict.jikvictbackend.service.registry.TaskRegistry
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class GeneralTaskQueueService(
    rabbitTemplate: RabbitTemplate,
    taskStatusRepository: TaskStatusRepository,
    taskRegistry: TaskRegistry,
    log: Logger,
) : TaskQueueService(rabbitTemplate, taskStatusRepository, taskRegistry, log)
