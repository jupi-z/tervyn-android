package dev.amenokizele.tervyn.feature.execution

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
class ExecutionViewModel : ViewModel() {

    private val jobId = MutableStateFlow<String?>(null)
    val job: StateFlow<DemoJob?> = jobId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(null) else DemoRepository.observeJob(id)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun loadJob(jobId: String) {
        this.jobId.value = jobId
    }

    fun toggleChecklistItem(jobId: String, itemId: String): Boolean {
        return DemoRepository.toggleChecklistItem(jobId, itemId)
    }

    fun addNote(jobId: String, content: String): Boolean {
        return DemoRepository.addNote(jobId, content) != null
    }

    fun addPhoto(jobId: String, title: String, tag: String): Boolean {
        return DemoRepository.addPhoto(jobId, title, tag) != null
    }

    fun deletePhoto(jobId: String, photoId: String): Boolean {
        return DemoRepository.deletePhoto(jobId, photoId)
    }

    fun completeJob(jobId: String): Boolean {
        return DemoRepository.completeJob(jobId)
    }
}
