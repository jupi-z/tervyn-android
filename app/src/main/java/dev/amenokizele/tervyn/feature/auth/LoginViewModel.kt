package dev.amenokizele.tervyn.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object InvalidCredentials : LoginUiState
    data object InvalidEmail : LoginUiState
    data object EmptyPassword : LoginUiState
    data object NetworkUnavailable : LoginUiState
    data object ServerError : LoginUiState
    data object Authenticated : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _email = MutableStateFlow("amina@tervyn.demo")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("tervyn2026")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isPasswordVisible = MutableStateFlow(false)
    val isPasswordVisible: StateFlow<Boolean> = _isPasswordVisible.asStateFlow()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(value: String) {
        _email.value = value
        if (_uiState.value !is LoginUiState.Idle) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun onPasswordChanged(value: String) {
        _password.value = value
        if (_uiState.value !is LoginUiState.Idle) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun togglePasswordVisibility() {
        _isPasswordVisible.value = !_isPasswordVisible.value
    }

    fun login() {
        val trimmedEmail = _email.value.trim()
        val currentPassword = _password.value

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            yield()
            _uiState.value = when (val result = loginUseCase(trimmedEmail, currentPassword)) {
                is AppResult.Success -> LoginUiState.Authenticated
                is AppResult.Failure -> result.error.toLoginUiState()
            }
        }
    }

    private fun AppError.toLoginUiState(): LoginUiState = when (this) {
        is AppError.Validation -> when (code) {
            "invalid_email" -> LoginUiState.InvalidEmail
            "empty_password" -> LoginUiState.EmptyPassword
            else -> LoginUiState.InvalidCredentials
        }
        is AppError.InvalidState -> if (code == "offline_simulation") {
            LoginUiState.NetworkUnavailable
        } else {
            LoginUiState.ServerError
        }
        is AppError.Authentication -> LoginUiState.InvalidCredentials
        else -> LoginUiState.ServerError
    }
}
