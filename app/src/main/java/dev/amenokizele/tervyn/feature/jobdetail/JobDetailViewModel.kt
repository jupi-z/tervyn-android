package dev.amenokizele.tervyn.feature.jobdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.usecase.ObserveJobUseCase
import dev.amenokizele.tervyn.domain.usecase.StartJobUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val observeJob: ObserveJobUseCase,
    private val startJob: StartJobUseCase
) : ViewModel() {

    private val jobId = MutableStateFlow<String?>(null)
    val job: StateFlow<Job?> = jobId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(null) else observeJob(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun loadJob(jobId: String) {
        this.jobId.value = jobId
    }

    fun startJob(jobId: String, onStarted: () -> Unit) {
        viewModelScope.launch {
            if (startJob(jobId) is AppResult.Success) {
                onStarted()
            }
        }
    }
}
