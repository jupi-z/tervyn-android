package dev.amenokizele.tervyn.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.amenokizele.tervyn.core.time.TervynDateTimeFormatter
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.domain.usecase.ObserveSyncOverviewUseCase
import dev.amenokizele.tervyn.domain.usecase.ObserveThemeModeUseCase
import dev.amenokizele.tervyn.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeThemeMode: ObserveThemeModeUseCase,
    observeSyncOverview: ObserveSyncOverviewUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val dateTimeFormatter: TervynDateTimeFormatter
) : ViewModel() {

    val currentTheme: StateFlow<ThemeMode> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val lastSyncTime: StateFlow<String> = observeSyncOverview()
        .map { dateTimeFormatter.formatDayTime(it.lastSuccessfulSyncAt) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch {
            setThemeMode(theme)
        }
    }
}
