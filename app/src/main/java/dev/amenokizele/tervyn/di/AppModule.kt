package dev.amenokizele.tervyn.di

import dev.amenokizele.tervyn.core.time.SystemTervynClock
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.core.time.TervynDateTimeFormatter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideClock(): TervynClock = SystemTervynClock()

    @Provides
    fun provideDateTimeFormatter(): TervynDateTimeFormatter = TervynDateTimeFormatter()

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
