package dev.amenokizele.tervyn.feature.jobdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.amenokizele.tervyn.demo.DemoRepository
import dev.amenokizele.tervyn.model.DemoJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class JobDetailViewModel : ViewModel() {

    private val jobId = MutableStateFlow<String?>(null)
    val job: StateFlow<DemoJob?> = jobId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(null) else DemoRepository.observeJob(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun loadJob(jobId: String) {
        this.jobId.value = jobId
    }

    fun startJob(jobId: String, onStarted: () -> Unit) {
        val success = DemoRepository.startJob(jobId)
        if (success) {
            onStarted()
        }
    }
}
