package dev.amenokizele.tervyn.data.remote.error

import kotlinx.serialization.Serializable

@Serializable
data class RemoteErrorEnvelopeDto(
    val error: RemoteErrorDto? = null
)

@Serializable
data class RemoteErrorDto(
    val code: String? = null,
    val message: String? = null,
    val requestId: String? = null,
    val fieldErrors: Map<String, List<String>>? = null
)
