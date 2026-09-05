package dev.amenokizele.tervyn.app

import dev.amenokizele.tervyn.core.result.AppError

sealed interface LocalDataInitializationState {
    data object Initializing : LocalDataInitializationState
    data object Ready : LocalDataInitializationState
    data class Error(val error: AppError) : LocalDataInitializationState
}
