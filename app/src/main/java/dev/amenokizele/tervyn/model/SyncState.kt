package dev.amenokizele.tervyn.model

enum class SyncState(val label: String) {
    SYNCED("Synchronisé"),
    PENDING("En attente"),
    SYNCING("En cours de sync"),
    FAILED("Échec")
}
