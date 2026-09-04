package dev.amenokizele.tervyn.feature.execution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.AddAttachmentRequest
import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.usecase.AddAttachmentUseCase
import dev.amenokizele.tervyn.domain.usecase.AddNoteUseCase
import dev.amenokizele.tervyn.domain.usecase.CompleteJobUseCase
import dev.amenokizele.tervyn.domain.usecase.DeleteAttachmentUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveJobUseCase
import dev.amenokizele.tervyn.domain.usecase.ToggleChecklistItemUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExecutionViewModel @Inject constructor(
    private val observeJob: ObserveJobUseCase,
    private val toggleChecklistItemUseCase: ToggleChecklistItemUseCase,
    private val addNoteUseCase: AddNoteUseCase,
    private val addAttachmentUseCase: AddAttachmentUseCase,
    private val deleteAttachmentUseCase: DeleteAttachmentUseCase,
    private val completeJobUseCase: CompleteJobUseCase
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

    suspend fun toggleChecklistItem(jobId: String, itemId: String): Boolean {
        return toggleChecklistItemUseCase(jobId, itemId) is AppResult.Success
    }

    suspend fun addNote(jobId: String, content: String): Boolean {
        return addNoteUseCase(jobId, content) is AppResult.Success
    }

    suspend fun addPhoto(jobId: String, title: String, tag: String): Boolean {
        val request = AddAttachmentRequest(
            type = AttachmentType.PHOTO,
            localUri = "tervyn://demo/photo/$tag",
            mimeType = "image/jpeg",
            fileName = title.ifBlank { "Photo terrain" },
            sizeBytes = 0,
            checksumSha256 = null
        )
        return addAttachmentUseCase(jobId, request) is AppResult.Success
    }

    suspend fun deletePhoto(jobId: String, photoId: String): Boolean {
        return deleteAttachmentUseCase(jobId, photoId) is AppResult.Success
    }

    suspend fun completeJob(jobId: String): Boolean {
        return completeJobUseCase(jobId) is AppResult.Success
    }
}
