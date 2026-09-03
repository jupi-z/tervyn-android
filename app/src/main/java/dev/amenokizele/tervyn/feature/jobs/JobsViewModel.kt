package dev.amenokizele.tervyn.feature.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.amenokizele.tervyn.demo.DemoRepository
import dev.amenokizele.tervyn.model.DemoJob
import dev.amenokizele.tervyn.model.JobFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class JobsViewModel : ViewModel() {

    private val _selectedFilter = MutableStateFlow(JobFilter.ALL)
    val selectedFilter: StateFlow<JobFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val isOffline: StateFlow<Boolean> = DemoRepository.isOffline

    val pendingCount: StateFlow<Int> = DemoRepository.pendingCount

    val jobs: StateFlow<List<DemoJob>> = combine(
        DemoRepository.jobs,
        _selectedFilter,
        _searchQuery
    ) { allJobs, filter, query ->
        DemoRepository.filterJobs(allJobs, filter, query)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filterCounts: StateFlow<Map<JobFilter, Int>> = DemoRepository.jobs.combine(_searchQuery) { allJobs, query ->
        DemoRepository.filterCounts(allJobs, query)
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
}
