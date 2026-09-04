package dev.amenokizele.tervyn.data.inmemory

import dev.amenokizele.tervyn.data.fixtures.TervynDemoFixtures
import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryStore @Inject constructor() {
    private val _jobs = MutableStateFlow(TervynDemoFixtures.initialJobs())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSuccessfulSyncAt = MutableStateFlow(TervynDemoFixtures.initialLastSyncAt)
    val lastSuccessfulSyncAt: StateFlow<java.time.Instant> = _lastSuccessfulSyncAt.asStateFlow()

    var jobsValue: List<Job>
        get() = _jobs.value
        set(value) {
            _jobs.value = value
        }

    var authStateValue: AuthState
        get() = _authState.value
        set(value) {
            _authState.value = value
        }

    var themeModeValue: ThemeMode
        get() = _themeMode.value
        set(value) {
            _themeMode.value = value
        }

    var isOfflineValue: Boolean
        get() = _isOffline.value
        set(value) {
            _isOffline.value = value
        }

    var isSyncingValue: Boolean
        get() = _isSyncing.value
        set(value) {
            _isSyncing.value = value
        }

    var lastSuccessfulSyncAtValue: java.time.Instant
        get() = _lastSuccessfulSyncAt.value
        set(value) {
            _lastSuccessfulSyncAt.value = value
        }

    fun pendingCount(): Int = jobsValue.sumOf { job ->
        val jobCount = if (job.syncState == SyncState.PENDING || job.syncState == SyncState.FAILED) 1 else 0
        val checklistCount = job.checklist.count { it.syncState == SyncState.PENDING || it.syncState == SyncState.FAILED }
        val noteCount = job.notes.count { it.syncState == SyncState.PENDING || it.syncState == SyncState.FAILED }
        val attachmentCount = job.attachments.count { it.syncState == SyncState.PENDING || it.syncState == SyncState.FAILED }
        jobCount + checklistCount + noteCount + attachmentCount
    }
}
