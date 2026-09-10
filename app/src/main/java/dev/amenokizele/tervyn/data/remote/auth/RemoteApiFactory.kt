package dev.amenokizele.tervyn.data.remote.auth

import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object RemoteApiFactory {
    fun createPublicRetrofit(config: RemoteApiConfig, json: Json): Retrofit {
        return createRetrofit(config, json, OkHttpClient())
    }

    fun createRetrofit(
        config: RemoteApiConfig,
        json: Json,
        client: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(config.clientBaseUrl())
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
