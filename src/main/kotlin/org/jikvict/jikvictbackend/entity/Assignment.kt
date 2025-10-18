package org.jikvict.jikvictbackend.entity

import java.time.LocalDateTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "assignments")
class Assignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    var id: Long = 0

    @Column(nullable = false)
    var title: String = ""

    @Column(nullable = true, columnDefinition = "TEXT")
    var description: String? = null

    var taskId: Int = 0

    @Column(name = "max_points", nullable = false)
    var maxPoints: Int = 0

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDateTime = LocalDateTime.now()

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDateTime = LocalDateTime.now()

    var timeOutSeconds: Long = 0

    @ManyToMany(mappedBy = "assignments", fetch = FetchType.EAGER)
    var assignmentGroups: MutableSet<AssignmentGroup> = mutableSetOf()

    @Column(name = "memory_limit", nullable = true)
    var memoryLimit: Long = 0

    @Column(name = "cpu_limit", nullable = true)
    var cpuLimit: Long = 0

    @Column(name = "pids_limit", nullable = true)
    var pidsLimit: Long = 0

    @Column(name = "maximum_attempts", nullable = false, columnDefinition = "integer default 2")
    var maximumAttempts: Int = 2


    @Column(name = "status_override", nullable = false)
    @Enumerated(EnumType.STRING)
    var statusOverride: AssignmentStatusOverride = AssignmentStatusOverride.AUTO
}

val Assignment.isClosed: Boolean
    get() = when (statusOverride) {
        AssignmentStatusOverride.CLOSED -> true
        AssignmentStatusOverride.OPEN -> false
        AssignmentStatusOverride.AUTO -> {
            LocalDateTime.now().isAfter(endDate) || LocalDateTime.now().isBefore(startDate)
        }
    }

enum class AssignmentStatusOverride {
    OPEN,
    CLOSED,
    AUTO
}
