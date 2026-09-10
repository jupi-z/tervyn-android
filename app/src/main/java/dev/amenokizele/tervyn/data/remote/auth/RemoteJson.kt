package dev.amenokizele.tervyn.data.remote.auth

import kotlinx.serialization.json.Json

object RemoteJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        isLenient = false
        coerceInputValues = false
    }
}
