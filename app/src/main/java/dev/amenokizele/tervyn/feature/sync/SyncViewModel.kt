package dev.amenokizele.tervyn.feature.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.time.TervynDateTimeFormatter
import dev.amenokizele.tervyn.app.SimulationController
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.usecase.ObserveSyncOverviewUseCase
import dev.amenokizele.tervyn.domain.usecase.RetryPendingOperationsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    observeSyncOverview: ObserveSyncOverviewUseCase,
    private val retryPendingOperations: RetryPendingOperationsUseCase,
    private val simulationController: SimulationController,
    private val dateTimeFormatter: TervynDateTimeFormatter
) : ViewModel() {

    private val overview = observeSyncOverview()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = dev.amenokizele.tervyn.domain.model.SyncOverview(
                state = SyncState.SYNCED,
                pendingCount = 0,
                failedCount = 0,
                lastSuccessfulSyncAt = null,
                isOnline = true,
                completedOperations = 1,
                totalOperations = 1
            )
        )

    val isSyncing: StateFlow<Boolean> = overview
        .map { it.state == SyncState.SYNCING }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isOffline: StateFlow<Boolean> = overview
        .map { !it.isOnline }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    val lastSyncTime: StateFlow<String> = overview
        .map { dateTimeFormatter.formatDayTime(it.lastSuccessfulSyncAt) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val pendingCount: StateFlow<Int> = overview
        .map { it.pendingCount }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun syncNow(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            _syncError.value = null
            when (val result = retryPendingOperations()) {
                is AppResult.Success -> onCompleted()
                is AppResult.Failure -> _syncError.value = result.error.toSyncMessage()
            }
        }
    }

    fun toggleOffline() {
        simulationController.toggleOffline()
        _syncError.value = null
    }

    private fun AppError.toSyncMessage(): String = when (this) {
        is AppError.InvalidState -> if (code == "offline_simulation") {
            "Simulation hors connexion active. Repassez la simulation en ligne pour vérifier la file locale."
        } else {
            "Traitement local déjà en cours."
        }
        is AppError.Network -> if (code == "remote_sync_not_configured") {
            "Aucun serveur distant n'est configuré en Phase 2."
        } else {
            "Synchronisation distante indisponible."
        }
        else -> "La vérification de la file locale a échoué."
    }
}
