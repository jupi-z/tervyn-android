package dev.amenokizele.tervyn.feature.sync

import androidx.lifecycle.ViewModel
import dev.amenokizele.tervyn.demo.DemoRepository
import dev.amenokizele.tervyn.model.SyncState
import kotlinx.coroutines.flow.StateFlow

class SyncViewModel : ViewModel() {

    val isSyncing: StateFlow<Boolean> = DemoRepository.isSyncing
    val isOffline: StateFlow<Boolean> = DemoRepository.isOffline
    val syncError: StateFlow<String?> = DemoRepository.syncError
    val lastSyncTime: StateFlow<String> = DemoRepository.lastSyncTime
    val pendingCount: StateFlow<Int> = DemoRepository.pendingCount

    fun syncNow(onCompleted: () -> Unit = {}) {
        DemoRepository.simulateSync(onCompleted)
    }

    fun toggleOffline() {
        DemoRepository.setOffline(!isOffline.value)
    }
}
