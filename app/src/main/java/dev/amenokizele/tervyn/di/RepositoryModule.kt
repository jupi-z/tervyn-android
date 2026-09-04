package dev.amenokizele.tervyn.di

import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.data.inmemory.DemoSimulationController
import dev.amenokizele.tervyn.data.inmemory.InMemoryAuthRepository
import dev.amenokizele.tervyn.data.inmemory.InMemoryJobRepository
import dev.amenokizele.tervyn.data.inmemory.InMemorySyncRepository
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
    abstract fun bindJobRepository(repository: InMemoryJobRepository): JobRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(repository: InMemoryAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(repository: InMemorySyncRepository): SyncRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(repository: InMemoryUserPreferencesRepository): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindSimulationController(controller: DemoSimulationController): SimulationController
}
