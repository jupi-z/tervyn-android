package dev.amenokizele.tervyn.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.domain.usecase.LogoutUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveAuthStateUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveSyncOverviewUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveThemeModeUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val localDataInitializer: LocalDataInitializer,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    private val localDataState =
        MutableStateFlow<LocalDataInitializationState>(LocalDataInitializationState.Initializing)
    private var initializationJob: Job? = null

    val state = combine(
        observeAuthState(),
        observeThemeMode(),
        observeSyncOverview(),
        localDataState
    ) { authState, themeMode, syncOverview, currentLocalDataState ->
        TervynAppState(
            authState = if (currentLocalDataState == LocalDataInitializationState.Ready) {
                authState
            } else {
                AuthState.Checking
            },
            themeMode = themeMode,
            pendingSyncCount = syncOverview.pendingCount,
            localDataState = currentLocalDataState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TervynAppState(
            authState = AuthState.Checking,
            themeMode = ThemeMode.SYSTEM,
            pendingSyncCount = 0,
            localDataState = LocalDataInitializationState.Initializing
        )
    )

    init {
        initializeLocalData()
    }

    fun retryLocalDataInitialization() {
        initializeLocalData()
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        viewModelScope.launch {
            if (logoutUseCase() is AppResult.Success) {
                onLoggedOut()
            }
        }
    }

    private fun initializeLocalData() {
        if (initializationJob?.isActive == true) {
            return
        }
        initializationJob = viewModelScope.launch {
            localDataState.value = LocalDataInitializationState.Initializing
            val result = try {
                localDataInitializer.initialize()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                AppResult.Failure(
                    AppError.Unknown(
                        code = "local_data_initialization_failed",
                        cause = exception
                    )
                )
            }
            localDataState.value = when (result) {
                is AppResult.Success -> LocalDataInitializationState.Ready
                is AppResult.Failure -> LocalDataInitializationState.Error(result.error)
            }
        }
    }
}
