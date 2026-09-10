package dev.amenokizele.tervyn.data.remote.auth

import okhttp3.Interceptor
import okhttp3.Response

class BearerTokenInterceptor(
    private val sessionCoordinator: SessionCoordinator
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header("Authorization") != null) {
            return chain.proceed(request)
        }
        val session = sessionCoordinator.snapshot() ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer ${session.accessToken}")
                .build()
        )
    }
}
