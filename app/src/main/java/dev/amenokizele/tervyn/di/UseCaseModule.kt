package dev.amenokizele.tervyn.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.amenokizele.tervyn.domain.repository.AuthRepository
import dev.amenokizele.tervyn.domain.repository.JobRepository
import dev.amenokizele.tervyn.domain.repository.SyncRepository
import dev.amenokizele.tervyn.domain.repository.UserPreferencesRepository
import dev.amenokizele.tervyn.domain.usecase.AddAttachmentUseCase
import dev.amenokizele.tervyn.domain.usecase.AddNoteUseCase
import dev.amenokizele.tervyn.domain.usecase.CompleteJobUseCase
import dev.amenokizele.tervyn.domain.usecase.DeleteAttachmentUseCase
import dev.amenokizele.tervyn.domain.usecase.LoginUseCase
import dev.amenokizele.tervyn.domain.usecase.LogoutUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveAuthStateUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveJobUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveJobsUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveSyncOverviewUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveThemeModeUseCase
import dev.amenokizele.tervyn.domain.usecase.RetryPendingOperationsUseCase
import dev.amenokizele.tervyn.domain.usecase.SetThemeModeUseCase
import dev.amenokizele.tervyn.domain.usecase.StartJobUseCase
import dev.amenokizele.tervyn.domain.usecase.ToggleChecklistItemUseCase

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides fun provideObserveJobsUseCase(repository: JobRepository) = ObserveJobsUseCase(repository)
    @Provides fun provideObserveJobUseCase(repository: JobRepository) = ObserveJobUseCase(repository)
    @Provides fun provideStartJobUseCase(repository: JobRepository) = StartJobUseCase(repository)
    @Provides fun provideToggleChecklistItemUseCase(repository: JobRepository) = ToggleChecklistItemUseCase(repository)
    @Provides fun provideAddNoteUseCase(repository: JobRepository) = AddNoteUseCase(repository)
    @Provides fun provideAddAttachmentUseCase(repository: JobRepository) = AddAttachmentUseCase(repository)
    @Provides fun provideDeleteAttachmentUseCase(repository: JobRepository) = DeleteAttachmentUseCase(repository)
    @Provides fun provideCompleteJobUseCase(repository: JobRepository) = CompleteJobUseCase(repository)
    @Provides fun provideLoginUseCase(repository: AuthRepository) = LoginUseCase(repository)
    @Provides fun provideLogoutUseCase(repository: AuthRepository) = LogoutUseCase(repository)
    @Provides fun provideObserveAuthStateUseCase(repository: AuthRepository) = ObserveAuthStateUseCase(repository)
    @Provides fun provideObserveThemeModeUseCase(repository: UserPreferencesRepository) = ObserveThemeModeUseCase(repository)
    @Provides fun provideSetThemeModeUseCase(repository: UserPreferencesRepository) = SetThemeModeUseCase(repository)
    @Provides fun provideObserveSyncOverviewUseCase(repository: SyncRepository) = ObserveSyncOverviewUseCase(repository)
    @Provides fun provideRetryPendingOperationsUseCase(repository: SyncRepository) = RetryPendingOperationsUseCase(repository)
}
