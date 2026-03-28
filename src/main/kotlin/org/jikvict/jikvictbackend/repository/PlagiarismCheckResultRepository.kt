package org.jikvict.jikvictbackend.repository

import org.jikvict.jikvictbackend.entity.PlagiarismCheckResult
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlagiarismCheckResultRepository : JpaRepository<PlagiarismCheckResult, Long>
