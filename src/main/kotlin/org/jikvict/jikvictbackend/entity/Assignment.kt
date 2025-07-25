package org.jikvict.jikvictbackend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
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

    @Column(name = "task_number", nullable = false)
    var taskNumber: Int = 0
}
