package org.jikvict.jikvictbackend.entity

import java.time.LocalDateTime
import jakarta.persistence.Basic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "plagiarism_check_results")
class PlagiarismCheckResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    var assignment: Assignment = Assignment()

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "result_zip")
    var resultZip: ByteArray? = null

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
}
