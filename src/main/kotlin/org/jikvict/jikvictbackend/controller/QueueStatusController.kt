package org.jikvict.jikvictbackend.controller

import org.apache.logging.log4j.Logger
import org.jikvict.jikvictbackend.model.response.QueueStatusDto
import org.jikvict.jikvictbackend.service.UserDetailsServiceImpl
import org.jikvict.jikvictbackend.service.queue.QueueStatusService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@EnableScheduling
class QueueStatusController(
    private val queueStatusService: QueueStatusService,
    private val userDetailsService: UserDetailsServiceImpl,
    private val messagingTemplate: SimpMessagingTemplate,
    private val log: Logger,
) {

    @MessageMapping("/queue-status")
    @SendTo("/topic/queue-status")
    fun getQueueStatusWs(): QueueStatusDto {
        return buildQueueStatus()
    }

    @Scheduled(fixedRate = 2000)
    fun broadcastQueueStatus() {
        val queueStatusMap = queueStatusService.getQueueStatusForAll()
        queueStatusMap.forEach { (userId, status) ->
            messagingTemplate.convertAndSend("/topic/queue-status/$userId", status)
        }
    }

    @GetMapping("/api/queue-status")
    fun getQueueStatusHttp(): QueueStatusDto {
        return buildQueueStatus()
    }


    private fun buildQueueStatus(): QueueStatusDto {
        val principal = try {
            userDetailsService.getCurrentUser()
        } catch (e: Exception) {
            log.error("Failed to get current user", e)
            null
        }
        if (principal == null) {
            return QueueStatusDto(
                totalInQueue = 0,
                userTaskPosition = null,
                userTaskId = null,
                estimatedTimeRemainingSeconds = null,
            )
        }
        return queueStatusService.getQueueStatus(principal)
    }
}
