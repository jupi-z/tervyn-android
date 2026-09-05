package dev.amenokizele.tervyn.app

import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.ThemeMode

data class TervynAppState(
    val authState: AuthState = AuthState.Checking,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pendingSyncCount: Int = 0,
    val localDataState: LocalDataInitializationState = LocalDataInitializationState.Initializing
)
