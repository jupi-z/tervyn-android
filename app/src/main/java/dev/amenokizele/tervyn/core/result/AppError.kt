package dev.amenokizele.tervyn.core.result

sealed interface AppError {
    data class NotFound(val entity: String, val id: String) : AppError
    data class Validation(val code: String) : AppError
    data class InvalidState(val code: String) : AppError
    data class Authentication(val code: String) : AppError
    data class Network(val code: String) : AppError
    data class Conflict(val code: String) : AppError
    data class Storage(val code: String) : AppError
    data class Unknown(val code: String? = null, val cause: Throwable? = null) : AppError
}
