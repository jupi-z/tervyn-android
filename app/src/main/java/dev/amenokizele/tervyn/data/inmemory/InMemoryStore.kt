package dev.amenokizele.tervyn.data.inmemory

import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryStore @Inject constructor() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    var authStateValue: AuthState
        get() = _authState.value
        set(value) {
            _authState.value = value
        }

    var themeModeValue: ThemeMode
        get() = _themeMode.value
        set(value) {
            _themeMode.value = value
        }

    var isOfflineValue: Boolean
        get() = _isOffline.value
        set(value) {
            _isOffline.value = value
        }
}
