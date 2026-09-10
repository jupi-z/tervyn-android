package dev.amenokizele.tervyn.data.remote.auth

import java.util.UUID
import okhttp3.Interceptor
import okhttp3.Response

class SafeHeadersInterceptor(
    private val clientVersion: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
            .header("Accept", "application/json")
            .header("X-Client-Platform", "android")
            .header("X-Client-Version", clientVersion)
        if (request.header("X-Request-ID") == null) {
            builder.header("X-Request-ID", UUID.randomUUID().toString())
        }
        return chain.proceed(builder.build())
    }
}
