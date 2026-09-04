package dev.amenokizele.tervyn.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.domain.usecase.LogoutUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveAuthStateUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveSyncOverviewUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveThemeModeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TervynAppViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    observeSyncOverview: ObserveSyncOverviewUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    val state = combine(
        observeAuthState(),
        observeThemeMode(),
        observeSyncOverview()
    ) { authState, themeMode, syncOverview ->
        TervynAppState(
            authState = authState,
            themeMode = themeMode,
            pendingSyncCount = syncOverview.pendingCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TervynAppState(
            authState = AuthState.Checking,
            themeMode = ThemeMode.SYSTEM,
            pendingSyncCount = 0
        )
    )

    fun logout(onLoggedOut: () -> Unit = {}) {
        viewModelScope.launch {
            if (logoutUseCase() is AppResult.Success) {
                onLoggedOut()
            }
        }
    }
}
