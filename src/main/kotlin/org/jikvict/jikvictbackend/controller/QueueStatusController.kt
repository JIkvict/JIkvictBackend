package org.jikvict.jikvictbackend.controller

import org.jikvict.jikvictbackend.model.response.QueueStatusDto
import org.jikvict.jikvictbackend.service.UserDetailsServiceImpl
import org.jikvict.jikvictbackend.service.queue.QueueStatusService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Controller
import java.security.Principal

@Controller
@EnableScheduling
class QueueStatusController(
    private val queueStatusService: QueueStatusService,
    private val userDetailsService: UserDetailsServiceImpl,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    @MessageMapping("/queue-status")
    @SendTo("/topic/queue-status")
    fun getQueueStatus(principal: Principal?): QueueStatusDto {
        if (principal == null) {
            return QueueStatusDto(
                totalInQueue = 0,
                userTaskPosition = null,
                userTaskId = null,
                estimatedTimeRemainingSeconds = null,
            )
        }

        val user = userDetailsService.getCurrentUser()
        return queueStatusService.getQueueStatus(user)
    }

    @Scheduled(fixedRate = 2000)
    fun broadcastQueueStatus() {
        val queueStatusMap = queueStatusService.getQueueStatusForAll()

        queueStatusMap.forEach { (userId, status) ->
            messagingTemplate.convertAndSend(
                "/topic/queue-status/$userId",
                status,
            )
        }
    }
}
