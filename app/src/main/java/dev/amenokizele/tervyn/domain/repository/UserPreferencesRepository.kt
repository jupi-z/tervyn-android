package dev.amenokizele.tervyn.domain.repository

import dev.amenokizele.tervyn.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
