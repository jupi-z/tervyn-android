package dev.amenokizele.tervyn.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.amenokizele.tervyn.BuildConfig
import dev.amenokizele.tervyn.data.remote.auth.AuthenticatedAuthApi
import dev.amenokizele.tervyn.data.remote.auth.BearerTokenInterceptor
import dev.amenokizele.tervyn.data.remote.auth.PublicAuthApi
import dev.amenokizele.tervyn.data.remote.auth.RevocationAuthApi
import dev.amenokizele.tervyn.data.remote.auth.RemoteTokenRefresher
import dev.amenokizele.tervyn.data.remote.auth.SafeHeadersInterceptor
import dev.amenokizele.tervyn.data.remote.auth.SessionCoordinator
import dev.amenokizele.tervyn.data.remote.auth.SessionRefreshAuthenticator
import dev.amenokizele.tervyn.data.remote.config.RemoteApiConfig
import dev.amenokizele.tervyn.data.remote.error.RemoteErrorMapper
import dev.amenokizele.tervyn.data.remote.job.JobsApi
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideRemoteJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
        isLenient = false
        coerceInputValues = false
    }

    @Provides
    @Singleton
    fun provideRemoteApiConfig(): RemoteApiConfig {
        return RemoteApiConfig(
            enabled = BuildConfig.TERVYN_REMOTE_API_ENABLED,
            baseUrl = BuildConfig.TERVYN_API_BASE_URL
        ).also { if (it.enabled) it.validatedProductionBaseUrl() }
    }

    @Provides
    @Singleton
    fun provideRemoteErrorMapper(json: Json): RemoteErrorMapper = RemoteErrorMapper(json)

    @Provides
    @Singleton
    fun provideSafeHeadersInterceptor(): SafeHeadersInterceptor {
        return SafeHeadersInterceptor(BuildConfig.VERSION_NAME)
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            redactHeader("Cookie")
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    @PublicClient
    fun providePublicOkHttpClient(
        safeHeadersInterceptor: SafeHeadersInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return baseClientBuilder()
            .addInterceptor(safeHeadersInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedOkHttpClient(
        safeHeadersInterceptor: SafeHeadersInterceptor,
        bearerTokenInterceptor: BearerTokenInterceptor,
        authenticator: SessionRefreshAuthenticator,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return baseClientBuilder()
            .addInterceptor(safeHeadersInterceptor)
            .addInterceptor(bearerTokenInterceptor)
            .authenticator(authenticator)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideBearerTokenInterceptor(sessionCoordinator: SessionCoordinator): BearerTokenInterceptor {
        return BearerTokenInterceptor(sessionCoordinator)
    }

    @Provides
    @Singleton
    fun provideSessionRefreshAuthenticator(
        sessionCoordinator: SessionCoordinator,
        tokenRefresher: RemoteTokenRefresher
    ): SessionRefreshAuthenticator {
        return SessionRefreshAuthenticator(sessionCoordinator, tokenRefresher)
    }

    @Provides
    @Singleton
    @PublicRetrofit
    fun providePublicRetrofit(
        config: RemoteApiConfig,
        json: Json,
        @PublicClient client: OkHttpClient
    ): Retrofit = retrofit(config, json, client)

    @Provides
    @Singleton
    @AuthenticatedRetrofit
    fun provideAuthenticatedRetrofit(
        config: RemoteApiConfig,
        json: Json,
        @AuthenticatedClient client: OkHttpClient
    ): Retrofit = retrofit(config, json, client)

    @Provides
    @Singleton
    fun providePublicAuthApi(@PublicRetrofit retrofit: Retrofit): PublicAuthApi {
        return retrofit.create(PublicAuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthenticatedAuthApi(@AuthenticatedRetrofit retrofit: Retrofit): AuthenticatedAuthApi {
        return retrofit.create(AuthenticatedAuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRevocationAuthApi(@PublicRetrofit retrofit: Retrofit): RevocationAuthApi {
        return retrofit.create(RevocationAuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideJobsApi(@AuthenticatedRetrofit retrofit: Retrofit): JobsApi {
        return retrofit.create(JobsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRemoteTokenRefresher(
        authApi: PublicAuthApi,
        errorMapper: RemoteErrorMapper
    ): RemoteTokenRefresher = RemoteTokenRefresher(authApi, errorMapper)

    private fun retrofit(config: RemoteApiConfig, json: Json, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(config.clientBaseUrl())
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private fun baseClientBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
    }
}
