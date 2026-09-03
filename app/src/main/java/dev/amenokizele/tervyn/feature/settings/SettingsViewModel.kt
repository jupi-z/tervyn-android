package dev.amenokizele.tervyn.feature.settings

import androidx.lifecycle.ViewModel
import dev.amenokizele.tervyn.demo.DemoRepository
import dev.amenokizele.tervyn.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {

    val currentTheme: StateFlow<ThemeMode> = DemoRepository.themeMode
    val lastSyncTime: StateFlow<String> = DemoRepository.lastSyncTime

    fun setTheme(theme: ThemeMode) {
        DemoRepository.setThemeMode(theme)
    }
}
