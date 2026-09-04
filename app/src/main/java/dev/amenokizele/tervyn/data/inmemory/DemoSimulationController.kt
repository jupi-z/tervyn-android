package dev.amenokizele.tervyn.data.inmemory

import dev.amenokizele.tervyn.app.SimulationController
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoSimulationController @Inject constructor(
    private val store: InMemoryStore
) : SimulationController {
    override val isOffline: StateFlow<Boolean> = store.isOffline

    override fun toggleOffline() {
        store.isOfflineValue = !store.isOfflineValue
    }
}
