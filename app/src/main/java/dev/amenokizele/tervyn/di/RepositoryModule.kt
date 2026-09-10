package dev.amenokizele.tervyn.di

import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.app.LocalDataInitializer
import dev.amenokizele.tervyn.data.auth.demo.DemoAuthGateway
import dev.amenokizele.tervyn.data.auth.demo.LocalDemoAuthGateway
import dev.amenokizele.tervyn.data.auth.local.LocalUserDataSource
import dev.amenokizele.tervyn.data.auth.local.RoomLocalUserDataSource
import dev.amenokizele.tervyn.data.auth.repository.PersistentAuthRepository
import dev.amenokizele.tervyn.data.auth.session.AndroidKeystoreSessionStore
import dev.amenokizele.tervyn.data.auth.session.SecureSessionStore
import dev.amenokizele.tervyn.data.inmemory.DemoSimulationController
import dev.amenokizele.tervyn.data.local.repository.RoomJobRepository
import dev.amenokizele.tervyn.data.local.repository.RoomSyncRepository
import dev.amenokizele.tervyn.data.local.seed.RoomDatabaseSeeder
import dev.amenokizele.tervyn.data.preferences.DataStoreUserPreferencesRepository
import dev.amenokizele.tervyn.data.remote.auth.AuthGateway
import dev.amenokizele.tervyn.data.remote.auth.ConfiguredAuthGateway
import dev.amenokizele.tervyn.data.remote.auth.SecureSessionCoordinator
import dev.amenokizele.tervyn.data.remote.auth.SessionCoordinator
import dev.amenokizele.tervyn.data.remote.job.RemoteJobDataSource
import dev.amenokizele.tervyn.data.remote.job.RetrofitRemoteJobDataSource
import dev.amenokizele.tervyn.domain.repository.AuthRepository
import dev.amenokizele.tervyn.domain.repository.JobRepository
import dev.amenokizele.tervyn.domain.repository.SyncRepository
import dev.amenokizele.tervyn.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindJobRepository(repository: RoomJobRepository): JobRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(repository: PersistentAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(repository: RoomSyncRepository): SyncRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(repository: DataStoreUserPreferencesRepository): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindSecureSessionStore(store: AndroidKeystoreSessionStore): SecureSessionStore

    @Binds
    @Singleton
    abstract fun bindSessionCoordinator(coordinator: SecureSessionCoordinator): SessionCoordinator

    @Binds
    @Singleton
    abstract fun bindAuthGateway(gateway: ConfiguredAuthGateway): AuthGateway

    @Binds
    @Singleton
    abstract fun bindRemoteJobDataSource(dataSource: RetrofitRemoteJobDataSource): RemoteJobDataSource

    @Binds
    @Singleton
    abstract fun bindDemoAuthGateway(gateway: LocalDemoAuthGateway): DemoAuthGateway

    @Binds
    @Singleton
    abstract fun bindLocalUserDataSource(dataSource: RoomLocalUserDataSource): LocalUserDataSource

    @Binds
    @Singleton
    abstract fun bindSimulationController(controller: DemoSimulationController): SimulationController

    @Binds
    @Singleton
    abstract fun bindLocalDataInitializer(initializer: RoomDatabaseSeeder): LocalDataInitializer
}
