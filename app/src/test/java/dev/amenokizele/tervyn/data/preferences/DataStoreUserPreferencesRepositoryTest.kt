package dev.amenokizele.tervyn.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dev.amenokizele.tervyn.domain.model.ThemeMode
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreUserPreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun missingThemeDefaultsToSystem() = runTest {
        val repository = DataStoreUserPreferencesRepository(dataStore())

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    @Test
    fun setDarkThemeEmitsDark() = runTest {
        val repository = DataStoreUserPreferencesRepository(dataStore())

        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun themePersistsAcrossRepositoryRecreation() = runTest {
        val store = dataStore()
        val firstRepository = DataStoreUserPreferencesRepository(store)
        firstRepository.setThemeMode(ThemeMode.DARK)

        val secondRepository = DataStoreUserPreferencesRepository(store)

        assertEquals(ThemeMode.DARK, secondRepository.themeMode.first())
    }

    @Test
    fun invalidRawThemeFallsBackToSystem() = runTest {
        val store = dataStore()
        store.edit { preferences ->
            preferences[DataStoreUserPreferencesRepository.THEME_MODE_KEY] = "SEPIA"
        }
        val repository = DataStoreUserPreferencesRepository(store)

        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
    }

    private fun TestScope.dataStore(): DataStore<Preferences> {
        val file = File(temporaryFolder.newFolder(), "tervyn.preferences_pb")
        return PreferenceDataStoreFactory.create(
            scope = this.backgroundScope,
            produceFile = { file }
        )
    }
}
