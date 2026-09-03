package dev.amenokizele.tervyn.model

enum class JobFilter(val label: String) {
    ALL("Toutes"),
    ASSIGNED("À faire"),
    IN_PROGRESS("En cours"),
    COMPLETED("Terminées")
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Empty(val title: String, val description: String) : UiState<Nothing>
    data class Error(val message: String, val canRetry: Boolean = true) : UiState<Nothing>
}
