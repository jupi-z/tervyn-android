package dev.amenokizele.tervyn.data.inmemory

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryStore @Inject constructor() {
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    var isOfflineValue: Boolean
        get() = _isOffline.value
        set(value) {
            _isOffline.value = value
        }
}
