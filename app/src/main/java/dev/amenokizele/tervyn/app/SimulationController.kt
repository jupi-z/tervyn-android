package dev.amenokizele.tervyn.app

import kotlinx.coroutines.flow.StateFlow

interface SimulationController {
    val isOffline: StateFlow<Boolean>

    fun toggleOffline()
}
