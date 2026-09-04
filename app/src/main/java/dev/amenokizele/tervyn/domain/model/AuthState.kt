package dev.amenokizele.tervyn.domain.model

sealed interface AuthState {
    data object Checking : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: User) : AuthState
}
