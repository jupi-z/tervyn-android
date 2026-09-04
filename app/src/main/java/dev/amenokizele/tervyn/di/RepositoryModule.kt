package dev.amenokizele.tervyn.di

import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.app.LocalDataInitializer
import dev.amenokizele.tervyn.data.local.seed.RoomDatabaseSeeder
import dev.amenokizele.tervyn.data.local.repository.RoomJobRepository
import dev.amenokizele.tervyn.data.local.repository.RoomSyncRepository
import dev.amenokizele.tervyn.data.inmemory.DemoSimulationController
import dev.amenokizele.tervyn.data.inmemory.InMemoryAuthRepository
import dev.amenokizele.tervyn.data.inmemory.InMemoryUserPreferencesRepository
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
    abstract fun bindAuthRepository(repository: InMemoryAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(repository: RoomSyncRepository): SyncRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(repository: InMemoryUserPreferencesRepository): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindSimulationController(controller: DemoSimulationController): SimulationController

    @Binds
    @Singleton
    abstract fun bindLocalDataInitializer(initializer: RoomDatabaseSeeder): LocalDataInitializer
}
