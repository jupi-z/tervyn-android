package dev.amenokizele.tervyn.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.amenokizele.tervyn.core.time.SystemTervynClock
import dev.amenokizele.tervyn.core.time.TervynClock
import dev.amenokizele.tervyn.core.time.TervynDateTimeFormatter
import dev.amenokizele.tervyn.data.local.dao.SyncOperationDao
import dev.amenokizele.tervyn.data.local.dao.UserDao
import dev.amenokizele.tervyn.data.local.db.TervynDatabase
import dev.amenokizele.tervyn.data.local.db.MIGRATION_1_2
import dev.amenokizele.tervyn.data.local.repository.LocalIdGenerator
import dev.amenokizele.tervyn.data.local.repository.UuidLocalIdGenerator
import dev.amenokizele.tervyn.data.local.sync.SyncRuntimeStateStore
import dev.amenokizele.tervyn.data.local.sync.SyncBackoffPolicy
import dev.amenokizele.tervyn.data.local.sync.SyncOperationPayloadCodec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

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
        ).addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    @Singleton
    fun provideLocalIdGenerator(): LocalIdGenerator = UuidLocalIdGenerator()

    @Provides
    fun provideSyncOperationDao(database: TervynDatabase): SyncOperationDao {
        return database.syncOperationDao()
    }

    @Provides
    fun provideUserDao(database: TervynDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideLocalMetadataDao(database: TervynDatabase) = database.localMetadataDao()

    @Provides
    @Singleton
    fun provideSyncRuntimeStateStore(): SyncRuntimeStateStore = SyncRuntimeStateStore()

    @Provides
    @Singleton
    fun provideSyncOperationPayloadCodec(): SyncOperationPayloadCodec = SyncOperationPayloadCodec()

    @Provides
    @Singleton
    fun provideSyncBackoffPolicy(): SyncBackoffPolicy = SyncBackoffPolicy()

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + ioDispatcher),
            produceFile = { context.preferencesDataStoreFile("tervyn_preferences") }
        )
    }

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @DefaultDispatcher dispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
