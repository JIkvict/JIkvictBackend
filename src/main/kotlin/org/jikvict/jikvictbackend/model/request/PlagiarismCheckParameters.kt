package org.jikvict.jikvictbackend.model.request

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlagiarismCheckParameters(
    val minimumTokenMatch: Int? = null,
    val similarityThreshold: Double? = null,
    val maxNumberOfComparisons: Int? = null,
    val similarityMetric: SimilarityMetricOption? = null,
) {
    enum class SimilarityMetricOption { AVG, MAX, LONGEST_MATCH, OVERALL }
}
