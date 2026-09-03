package dev.amenokizele.tervyn.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.amenokizele.tervyn.demo.DemoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class InvalidCredentials(val message: String = "Email ou mot de passe incorrect.") : LoginUiState
    data class NetworkUnavailable(val message: String = "Connexion indisponible. Vérifiez votre réseau et réessayez.") : LoginUiState
    data class ServerError(val message: String = "Le service est momentanément indisponible.") : LoginUiState
    data object Authenticated : LoginUiState
}

class LoginViewModel : ViewModel() {

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

    fun login(onSuccess: () -> Unit) {
        val trimmedEmail = _email.value.trim()
        val currentPassword = _password.value

        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            _uiState.value = LoginUiState.InvalidCredentials("Veuillez saisir une adresse email valide.")
            return
        }

        if (currentPassword.isEmpty()) {
            _uiState.value = LoginUiState.InvalidCredentials("Veuillez saisir votre mot de passe.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            delay(600)

            if (DemoRepository.isOffline.value) {
                // If in simulated offline mode
                _uiState.value = LoginUiState.NetworkUnavailable()
                return@launch
            }

            // Demo rule: any non-empty input succeeds
            _uiState.value = LoginUiState.Authenticated
            onSuccess()
        }
    }
}
