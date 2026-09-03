package dev.amenokizele.tervyn.demo

import dev.amenokizele.tervyn.model.DemoChecklistItem
import dev.amenokizele.tervyn.model.DemoJob
import dev.amenokizele.tervyn.model.DemoNote
import dev.amenokizele.tervyn.model.DemoPhoto
import dev.amenokizele.tervyn.model.JobFilter
import dev.amenokizele.tervyn.model.JobStatus
import dev.amenokizele.tervyn.model.SyncState
import dev.amenokizele.tervyn.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

object DemoRepository {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _jobs = MutableStateFlow<List<DemoJob>>(DemoData.initialJobs)
    val jobs: StateFlow<List<DemoJob>> = _jobs.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Aujourd'hui · 08:00")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    val pendingCount: StateFlow<Int> = _jobs.map { list ->
        list.count { it.syncState == SyncState.PENDING } +
                list.flatMap { it.notes }.count { it.syncState == SyncState.PENDING } +
                list.flatMap { it.photos }.count { it.syncState == SyncState.PENDING }
    }.stateIn(scope, SharingStarted.Eagerly, 2)

    fun getJobById(id: String): DemoJob? {
        return _jobs.value.find { it.id == id }
    }

    fun observeJob(jobId: String): Flow<DemoJob?> {
        return jobs
            .map { list -> list.find { it.id == jobId } }
            .distinctUntilChanged()
    }

    fun filterJobs(filter: JobFilter, query: String): List<DemoJob> {
        return filterJobs(_jobs.value, filter, query)
    }

    fun filterJobs(jobs: List<DemoJob>, filter: JobFilter, query: String): List<DemoJob> {
        val filteredByStatus = when (filter) {
            JobFilter.ALL -> jobs
            JobFilter.ASSIGNED -> jobs.filter { it.status == JobStatus.ASSIGNED }
            JobFilter.IN_PROGRESS -> jobs.filter { it.status == JobStatus.IN_PROGRESS }
            JobFilter.COMPLETED -> jobs.filter { it.status == JobStatus.COMPLETED }
        }

        if (query.isBlank()) {
            return filteredByStatus.sortedBy { it.scheduledAt }
        }

        val cleanedQuery = query.trim().lowercase()
        return filteredByStatus.filter {
            it.reference.lowercase().contains(cleanedQuery) ||
                    it.title.lowercase().contains(cleanedQuery) ||
                    it.clientName.lowercase().contains(cleanedQuery) ||
                    it.siteName.lowercase().contains(cleanedQuery) ||
                    it.siteAddress.lowercase().contains(cleanedQuery)
        }.sortedBy { it.scheduledAt }
    }

    fun filterCounts(jobs: List<DemoJob>, query: String): Map<JobFilter, Int> {
        val baseList = filterJobs(jobs, JobFilter.ALL, query)
        return mapOf(
            JobFilter.ALL to baseList.size,
            JobFilter.ASSIGNED to baseList.count { it.status == JobStatus.ASSIGNED },
            JobFilter.IN_PROGRESS to baseList.count { it.status == JobStatus.IN_PROGRESS },
            JobFilter.COMPLETED to baseList.count { it.status == JobStatus.COMPLETED }
        )
    }

    fun startJob(jobId: String): Boolean {
        var success = false
        _jobs.value = _jobs.value.map { job ->
            if (job.id == jobId && job.status == JobStatus.ASSIGNED) {
                success = true
                job.copy(
                    status = JobStatus.IN_PROGRESS,
                    startedAt = "10:00",
                    syncState = SyncState.PENDING
                )
            } else {
                job
            }
        }
        return success
    }

    fun toggleChecklistItem(jobId: String, itemId: String): Boolean {
        var success = false
        _jobs.value = _jobs.value.map { job ->
            if (job.id == jobId && job.status == JobStatus.IN_PROGRESS && job.checklist.any { it.id == itemId }) {
                val updatedChecklist = job.checklist.map { item ->
                    if (item.id == itemId) {
                        item.copy(completed = !item.completed)
                    } else {
                        item
                    }
                }
                success = true
                job.copy(checklist = updatedChecklist, syncState = SyncState.PENDING)
            } else {
                job
            }
        }
        return success
    }

    fun addNote(jobId: String, content: String): DemoNote? {
        if (content.isBlank()) return null
        val newNote = DemoNote(
            id = "note-${UUID.randomUUID().toString().take(6)}",
            content = content.trim(),
            author = "Vous",
            createdAt = "10:14",
            syncState = SyncState.PENDING
        )
        var success = false
        _jobs.value = _jobs.value.map { job ->
            if (job.id == jobId && job.status == JobStatus.IN_PROGRESS) {
                success = true
                job.copy(
                    notes = listOf(newNote) + job.notes,
                    syncState = SyncState.PENDING
                )
            } else {
                job
            }
        }
        return if (success) newNote else null
    }

    fun addPhoto(jobId: String, title: String, tag: String = "PHOTO"): DemoPhoto? {
        val newPhoto = DemoPhoto(
            id = "photo-${UUID.randomUUID().toString().take(6)}",
            title = title.ifBlank { "Photo terrain" },
            placeholderTag = tag,
            createdAt = "10:42",
            syncState = SyncState.PENDING
        )
        var success = false
        _jobs.value = _jobs.value.map { job ->
            if (job.id == jobId && job.status == JobStatus.IN_PROGRESS) {
                success = true
                job.copy(
                    photos = job.photos + newPhoto,
                    syncState = SyncState.PENDING
                )
            } else {
                job
            }
        }
        return if (success) newPhoto else null
    }

    fun deletePhoto(jobId: String, photoId: String): Boolean {
        var success = false
        _jobs.value = _jobs.value.map { job ->
            if (job.id == jobId && job.status == JobStatus.IN_PROGRESS && job.photos.any { it.id == photoId }) {
                success = true
                job.copy(
                    photos = job.photos.filterNot { it.id == photoId },
                    syncState = SyncState.PENDING
                )
            } else {
                job
            }
        }
        return success
    }

    fun completeJob(jobId: String): Boolean {
        val target = getJobById(jobId) ?: return false
        if (target.status != JobStatus.IN_PROGRESS) return false
        if (!target.allRequiredCompleted) return false

        var success = false
        _jobs.value = _jobs.value.map { job ->
            if (job.id == jobId && job.status == JobStatus.IN_PROGRESS) {
                success = true
                job.copy(
                    status = JobStatus.COMPLETED,
                    completedAt = "11:30",
                    syncState = SyncState.PENDING
                )
            } else {
                job
            }
        }
        return success
    }

    fun simulateSync(onCompleted: () -> Unit = {}) {
        if (_isSyncing.value) return
        scope.launch {
            _isSyncing.value = true
            _syncError.value = null

            // Simulate progress
            delay(1200)

            if (_isOffline.value) {
                _isSyncing.value = false
                _syncError.value = "Connexion réseau indisponible. Impossible de synchroniser."
                return@launch
            }

            // Mark all synced
            _jobs.value = _jobs.value.map { job ->
                job.copy(
                    syncState = SyncState.SYNCED,
                    notes = job.notes.map { it.copy(syncState = SyncState.SYNCED) },
                    photos = job.photos.map { it.copy(syncState = SyncState.SYNCED) }
                )
            }
            _lastSyncTime.value = "Aujourd'hui · 14:32"
            _isSyncing.value = false
            onCompleted()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setOffline(offline: Boolean) {
        _isOffline.value = offline
    }

    fun resetDemoData() {
        _jobs.value = DemoData.initialJobs
        _isOffline.value = false
        _isSyncing.value = false
        _syncError.value = null
        _lastSyncTime.value = "Aujourd'hui · 08:00"
    }
}
