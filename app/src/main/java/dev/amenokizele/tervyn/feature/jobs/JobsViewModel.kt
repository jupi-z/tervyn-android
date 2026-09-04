package dev.amenokizele.tervyn.feature.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.usecase.ObserveJobsUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveSyncOverviewUseCase
import dev.amenokizele.tervyn.model.JobFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class JobsViewModel @Inject constructor(
    observeJobs: ObserveJobsUseCase,
    observeSyncOverview: ObserveSyncOverviewUseCase,
    simulationController: SimulationController
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(JobFilter.ALL)
    val selectedFilter: StateFlow<JobFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isOffline: StateFlow<Boolean> = simulationController.isOffline

    val pendingCount: StateFlow<Int> = observeSyncOverview()
        .map { it.pendingCount }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val jobs: StateFlow<List<Job>> = combine(
        observeJobs(),
        _selectedFilter,
        _searchQuery
    ) { allJobs, filter, query ->
        filterJobs(allJobs, filter, query)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filterCounts: StateFlow<Map<JobFilter, Int>> = observeJobs().combine(_searchQuery) { allJobs, query ->
        filterCounts(allJobs, query)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    fun setFilter(filter: JobFilter) {
        _selectedFilter.value = filter
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    private fun filterJobs(jobs: List<Job>, filter: JobFilter, query: String): List<Job> {
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
                    it.siteAddress.orEmpty().lowercase().contains(cleanedQuery)
        }.sortedBy { it.scheduledAt }
    }

    private fun filterCounts(jobs: List<Job>, query: String): Map<JobFilter, Int> {
        val baseList = filterJobs(jobs, JobFilter.ALL, query)
        return mapOf(
            JobFilter.ALL to baseList.size,
            JobFilter.ASSIGNED to baseList.count { it.status == JobStatus.ASSIGNED },
            JobFilter.IN_PROGRESS to baseList.count { it.status == JobStatus.IN_PROGRESS },
            JobFilter.COMPLETED to baseList.count { it.status == JobStatus.COMPLETED }
        )
    }
}
