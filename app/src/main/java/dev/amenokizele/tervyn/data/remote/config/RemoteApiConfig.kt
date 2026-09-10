package dev.amenokizele.tervyn.data.remote.config

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class RemoteApiConfig(
    val enabled: Boolean,
    val baseUrl: String,
    val allowHttpForTests: Boolean = false
) {
    fun validatedProductionBaseUrl(): HttpUrl {
        require(baseUrl.endsWith("/")) {
            "TERVYN_API_BASE_URL must end with /"
        }
        val parsed = baseUrl.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("TERVYN_API_BASE_URL must be a valid absolute URL")
        require(parsed.host.isNotBlank()) {
            "TERVYN_API_BASE_URL must contain a host"
        }
        require(parsed.isHttps || allowHttpForTests) {
            "TERVYN_API_BASE_URL must use HTTPS when remote API is enabled"
        }
        return parsed
    }

    fun clientBaseUrl(): HttpUrl {
        if (enabled || allowHttpForTests) return validatedProductionBaseUrl()
        return baseUrl.toHttpUrlOrNull()?.takeIf { it.host.isNotBlank() && baseUrl.endsWith("/") }
            ?: DEFAULT_BASE_URL.toHttpUrl()
    }

    companion object {
        private const val DEFAULT_BASE_URL = "https://tervyn.invalid/"

        fun default() = RemoteApiConfig(
            enabled = false,
            baseUrl = DEFAULT_BASE_URL
        )
    }
}
