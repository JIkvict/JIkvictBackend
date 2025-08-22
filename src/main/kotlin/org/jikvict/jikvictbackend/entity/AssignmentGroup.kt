package org.jikvict.jikvictbackend.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "assignment_groups")
class AssignmentGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    var id: Long = 0

    @Column(nullable = false)
    var name: String = ""

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_assignment_groups",
        joinColumns = [JoinColumn(name = "assignment_group_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")],
    )
    var users: MutableSet<User> = mutableSetOf()

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "assignment_group_assignments",
        joinColumns = [JoinColumn(name = "group_id")],
        inverseJoinColumns = [JoinColumn(name = "assignment_id")],
    )
    var assignments: MutableSet<Assignment> = mutableSetOf()
}
