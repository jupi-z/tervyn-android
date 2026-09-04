package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.domain.repository.UserPreferencesRepository

class SetThemeModeUseCase(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
