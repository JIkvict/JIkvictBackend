package org.jikvict.jikvictbackend.entity

import java.time.LocalDateTime
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
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

    @Lob
    @Column(nullable = true)
    var description: String? = null

    var taskId: Int = 0

    @Column(name = "max_points", nullable = false)
    var maxPoints: Int = 0

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDateTime = LocalDateTime.now()

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDateTime = LocalDateTime.now()

    @ManyToMany(mappedBy = "assignments")
    var assignmentGroups: MutableSet<AssignmentGroup> = mutableSetOf()
}
