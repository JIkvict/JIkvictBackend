package org.jikvict.jikvictbackend.entity

import java.time.LocalDateTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "assignments")
class Assignment(
    @Column(nullable = false)
    var title: String,
    @Lob
    @Column(nullable = true)
    var description: String?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    var task: Task,
    @Column(name = "max_points", nullable = false)
    var maxPoints: Int,
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDateTime,
    @Column(name = "end_date", nullable = false)
    var endDate: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    var id: Long = 0

    @ManyToMany(mappedBy = "assignments")
    var assignmentGroups: MutableSet<AssignmentGroup> = mutableSetOf()

    // No-args constructor for JPA
    constructor() : this(
        title = "",
        description = null,
        task = Task(),
        maxPoints = 0,
        startDate = LocalDateTime.now(),
        endDate = LocalDateTime.now(),
    )
}
