package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.domain.repository.UserPreferencesRepository

class ObserveThemeModeUseCase(
    private val repository: UserPreferencesRepository
) {
    operator fun invoke() = repository.themeMode
}
