package dev.amenokizele.tervyn.model

enum class JobStatus(val label: String) {
    ASSIGNED("À faire"),
    IN_PROGRESS("En cours"),
    COMPLETED("Terminée")
}
