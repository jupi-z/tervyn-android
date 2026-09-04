package dev.amenokizele.tervyn.di

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.amenokizele.tervyn.data.local.dao.SyncOperationDao
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.repository.LocalIdGenerator
import dev.amenokizele.tervyn.data.local.repository.UuidLocalIdGenerator
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
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TervynDatabase {
        return Room.databaseBuilder(
            context,
            TervynDatabase::class.java,
            TervynDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideLocalIdGenerator(): LocalIdGenerator = UuidLocalIdGenerator()

    @Provides
    fun provideSyncOperationDao(database: TervynDatabase): SyncOperationDao {
        return database.syncOperationDao()
    }

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
