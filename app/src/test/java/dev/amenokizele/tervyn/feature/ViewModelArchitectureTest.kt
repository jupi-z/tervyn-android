package dev.amenokizele.tervyn.feature

import dev.amenokizele.tervyn.FakeTervynClock
import dev.amenokizele.tervyn.MainDispatcherRule
import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.data.inmemory.DemoSimulationController
import dev.amenokizele.tervyn.data.inmemory.InMemoryAuthRepository
import dev.amenokizele.tervyn.data.inmemory.InMemoryJobRepository
import dev.amenokizele.tervyn.data.inmemory.InMemoryStore
import dev.amenokizele.tervyn.data.inmemory.InMemorySyncRepository
import dev.amenokizele.tervyn.data.inmemory.InMemoryUserPreferencesRepository
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.domain.usecase.AddAttachmentUseCase
import dev.amenokizele.tervyn.domain.usecase.AddNoteUseCase
import dev.amenokizele.tervyn.domain.usecase.CompleteJobUseCase
import dev.amenokizele.tervyn.domain.usecase.DeleteAttachmentUseCase
import dev.amenokizele.tervyn.domain.usecase.LoginUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveJobUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveJobsUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveSyncOverviewUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveThemeModeUseCase
import dev.amenokizele.tervyn.domain.usecase.RetryPendingOperationsUseCase
import dev.amenokizele.tervyn.domain.usecase.SetThemeModeUseCase
import dev.amenokizele.tervyn.domain.usecase.ToggleChecklistItemUseCase
import dev.amenokizele.tervyn.feature.auth.LoginUiState
import dev.amenokizele.tervyn.feature.auth.LoginViewModel
import dev.amenokizele.tervyn.feature.execution.ExecutionViewModel
import dev.amenokizele.tervyn.feature.jobs.JobsViewModel
import dev.amenokizele.tervyn.feature.settings.SettingsViewModel
import dev.amenokizele.tervyn.model.JobFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelArchitectureTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun jobsViewModel_appliesSearchFilterAndRepositoryUpdates() = runTest(mainDispatcherRule.dispatcher) {
        val graph = TestGraph(StandardTestDispatcher(testScheduler))
        val viewModel = JobsViewModel(
            observeJobs = ObserveJobsUseCase(graph.jobRepository),
            observeSyncOverview = ObserveSyncOverviewUseCase(graph.syncRepository),
            simulationController = graph.simulationController
        )
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("routeur")
        advanceUntilIdle()
        assertEquals(listOf("job-001"), viewModel.jobs.value.map { it.id })

        viewModel.clearSearch()
        viewModel.setFilter(JobFilter.IN_PROGRESS)
        advanceUntilIdle()
        assertTrue(viewModel.jobs.value.all { it.status == JobStatus.IN_PROGRESS })

        graph.jobRepository.startJob("job-001")
        advanceUntilIdle()
        assertTrue(viewModel.jobs.value.any { it.id == "job-001" })
    }

    @Test
    fun executionViewModel_observesLiveJobUpdates() = runTest(mainDispatcherRule.dispatcher) {
        val graph = TestGraph(StandardTestDispatcher(testScheduler))
        val viewModel = graph.executionViewModel()

        viewModel.loadJob("job-003")
        advanceUntilIdle()
        assertEquals(JobStatus.IN_PROGRESS, viewModel.job.value?.status)

        assertTrue(viewModel.toggleChecklistItem("job-003", "c3-3"))
        assertTrue(viewModel.completeJob("job-003"))
        advanceUntilIdle()

        assertEquals(JobStatus.COMPLETED, viewModel.job.value?.status)
    }

    @Test
    fun loginViewModel_exposesLoadingSuccessAndFailure() = runTest(mainDispatcherRule.dispatcher) {
        val graph = TestGraph(StandardTestDispatcher(testScheduler))
        val viewModel = LoginViewModel(LoginUseCase(graph.authRepository))
        val observedStates = mutableListOf<LoginUiState>()
        val collectStates = backgroundScope.launch {
            viewModel.uiState.collect { observedStates.add(it) }
        }

        viewModel.login()
        advanceUntilIdle()
        assertTrue(observedStates.contains(LoginUiState.Loading))
        assertEquals(LoginUiState.Authenticated, viewModel.uiState.value)

        viewModel.onEmailChanged("bad-email")
        viewModel.login()
        advanceUntilIdle()
        assertEquals(LoginUiState.InvalidEmail, viewModel.uiState.value)
        collectStates.cancel()
    }

    @Test
    fun settingsViewModel_updatesThemeState() = runTest(mainDispatcherRule.dispatcher) {
        val graph = TestGraph(StandardTestDispatcher(testScheduler))
        val viewModel = SettingsViewModel(
            observeThemeMode = ObserveThemeModeUseCase(graph.preferencesRepository),
            observeSyncOverview = ObserveSyncOverviewUseCase(graph.syncRepository),
            setThemeMode = SetThemeModeUseCase(graph.preferencesRepository),
            dateTimeFormatter = dev.amenokizele.tervyn.core.time.TervynDateTimeFormatter()
        )

        viewModel.setTheme(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.currentTheme.value)
    }

    private class TestGraph(dispatcher: kotlinx.coroutines.CoroutineDispatcher) {
        val store = InMemoryStore()
        val clock = FakeTervynClock()
        val jobRepository = InMemoryJobRepository(store, clock)
        val authRepository = InMemoryAuthRepository(store, dispatcher)
        val preferencesRepository = InMemoryUserPreferencesRepository(store)
        val syncRepository = InMemorySyncRepository(store, clock, dispatcher)
        val simulationController: SimulationController = DemoSimulationController(store)

        fun executionViewModel() = ExecutionViewModel(
            observeJob = ObserveJobUseCase(jobRepository),
            toggleChecklistItemUseCase = ToggleChecklistItemUseCase(jobRepository),
            addNoteUseCase = AddNoteUseCase(jobRepository, authRepository),
            addAttachmentUseCase = AddAttachmentUseCase(jobRepository, authRepository),
            deleteAttachmentUseCase = DeleteAttachmentUseCase(jobRepository),
            completeJobUseCase = CompleteJobUseCase(jobRepository)
        )
    }
}
