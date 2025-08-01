package org.jikvict.jikvictbackend.entity

import org.jikvict.jikvictbackend.model.response.PendingStatus
import java.time.LocalDateTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table

@Entity
@Table(name = "task_statuses")
class TaskStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    var id: Long = 0

    @Column(nullable = false)
    var taskType: String = ""

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PendingStatus = PendingStatus.PENDING

    @Lob
    @Column(nullable = true)
    var message: String? = null

    @Column(nullable = true)
    var resultId: Long? = null

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @Column(nullable = true)
    var completedAt: LocalDateTime? = null

    // Additional parameters stored as JSON
    @Column(columnDefinition = "TEXT")
    var parameters: String? = null
}
