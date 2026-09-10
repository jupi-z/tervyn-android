package dev.amenokizele.tervyn.data.remote.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteApiConfigTest {
    @Test
    fun defaultConfigKeepsRemoteApiDisabledWithInvalidPlaceholderUrl() {
        val config = RemoteApiConfig.default()

        assertFalse(config.enabled)
        assertEquals("https://tervyn.invalid/", config.baseUrl)
    }

    @Test
    fun enabledProductionConfigRequiresHttpsAndTrailingSlash() {
        val config = RemoteApiConfig(enabled = true, baseUrl = "https://api.tervyn.example/v1/")

        assertEquals("https://api.tervyn.example/v1/", config.validatedProductionBaseUrl().toString())
    }

    @Test
    fun enabledProductionConfigRejectsHttpMissingHostAndMissingTrailingSlash() {
        assertInvalid("http://api.tervyn.example/")
        assertInvalid("https:///")
        assertInvalid("https://api.tervyn.example")
    }

    @Test
    fun disabledConfigDoesNotValidateThePlaceholderAsAProductionBackend() {
        val config = RemoteApiConfig(enabled = false, baseUrl = "https://tervyn.invalid/")

        assertEquals("https://tervyn.invalid/", config.validatedProductionBaseUrl().toString())
    }

    @Test
    fun disabledConfigCanBuildClientWithMalformedCustomBaseUrl() {
        val config = RemoteApiConfig(enabled = false, baseUrl = "not a url")

        assertEquals("https://tervyn.invalid/", config.clientBaseUrl().toString())
    }

    private fun assertInvalid(baseUrl: String) {
        try {
            RemoteApiConfig(enabled = true, baseUrl = baseUrl).validatedProductionBaseUrl()
            throw AssertionError("Expected invalid config for $baseUrl")
        } catch (exception: IllegalArgumentException) {
            assertTrue(exception.message.orEmpty().contains("TERVYN_API_BASE_URL"))
        }
    }
}
