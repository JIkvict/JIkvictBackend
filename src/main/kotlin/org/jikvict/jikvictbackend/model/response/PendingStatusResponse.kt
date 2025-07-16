package org.jikvict.jikvictbackend.model.response

import com.fasterxml.jackson.annotation.JsonUnwrapped

data class PendingStatusResponse<T>(
    @get:JsonUnwrapped
    val payload: ResponsePayload<T>,
    val status: PendingStatus,
)
