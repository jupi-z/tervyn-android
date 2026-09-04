package dev.amenokizele.tervyn.data.inmemory

import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryUserPreferencesRepository @Inject constructor(
    private val store: InMemoryStore
) : UserPreferencesRepository {
    override val themeMode: Flow<ThemeMode> = store.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.themeModeValue = mode
    }
}
