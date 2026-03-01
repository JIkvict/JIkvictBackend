package org.jikvict.jikvictbackend.entity

import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.jikvict.testing.model.TestSuiteResult
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

@Entity(name = "assignment_result")
class AssignmentResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    var user: User = User()

    @ManyToOne(fetch = FetchType.LAZY)
    var assignment: Assignment = Assignment()

    var timeStamp: LocalDateTime = LocalDateTime.now()

    var points: Int = 0

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_logs", columnDefinition = "jsonb")
    var testSuiteResult: TestSuiteResult? = null

    @Column(name = "logs", columnDefinition = "TEXT")
    var logs: String? = null

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "zip_file")
    var zipFile: ByteArray? = null
}
