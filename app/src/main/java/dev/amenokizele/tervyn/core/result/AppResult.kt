package dev.amenokizele.tervyn.core.result

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

fun AppResult<Unit>.isSuccess(): Boolean = this is AppResult.Success
