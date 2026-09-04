package dev.amenokizele.tervyn.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.ui.components.InlineMessage
import dev.amenokizele.tervyn.ui.components.InlineMessageType
import dev.amenokizele.tervyn.ui.components.TervynOutlinedTextField
import dev.amenokizele.tervyn.ui.components.TervynPrimaryButton
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val isPasswordVisible by viewModel.isPasswordVisible.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginContent(
        email = email,
        password = password,
        isPasswordVisible = isPasswordVisible,
        uiState = uiState,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onLoginClick = viewModel::login,
        modifier = modifier
    )
}

@Composable
fun LoginContent(
    email: String,
    password: String,
    isPasswordVisible: Boolean,
    uiState: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xl),
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = stringResource(R.string.title_login_brand),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            val errorMessage = when (uiState) {
                LoginUiState.InvalidCredentials -> stringResource(R.string.login_error_invalid_credentials)
                LoginUiState.InvalidEmail -> stringResource(R.string.login_error_invalid_email)
                LoginUiState.EmptyPassword -> stringResource(R.string.login_error_empty_password)
                LoginUiState.NetworkUnavailable -> stringResource(R.string.login_error_offline)
                LoginUiState.ServerError -> stringResource(R.string.login_error_server)
                else -> null
            }
            val messageType = if (uiState == LoginUiState.NetworkUnavailable) {
                InlineMessageType.WARNING
            } else {
                InlineMessageType.ERROR
            }
            if (errorMessage != null) {
                InlineMessage(
                    text = errorMessage,
                    type = messageType,
                    modifier = Modifier.padding(bottom = Spacing.md)
                )
            }

            // Email field
            TervynOutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.login_email_label),
                placeholder = stringResource(R.string.login_demo_email),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                testTag = "login_email_input"
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Password field
            TervynOutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.login_password_label),
                placeholder = stringResource(R.string.login_password_placeholder),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        modifier = Modifier.testTag("login_password_toggle")
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) {
                                stringResource(R.string.cd_password_hide)
                            } else {
                                stringResource(R.string.cd_password_show)
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
                testTag = "login_password_input"
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Submit Button
            TervynPrimaryButton(
                text = stringResource(R.string.action_login),
                onClick = onLoginClick,
                isLoading = uiState is LoginUiState.Loading,
                testTag = "login_submit_button"
            )
        }
    }
}

@Preview(name = "Login Screen - Light", showBackground = true)
@Composable
private fun LoginScreenPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        LoginContent(
            email = "amina@tervyn.demo",
            password = "password",
            isPasswordVisible = false,
            uiState = LoginUiState.Idle,
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {}
        )
    }
}

@Preview(name = "Login Screen - Dark", showBackground = true)
@Composable
private fun LoginScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        LoginContent(
            email = "amina@tervyn.demo",
            password = "password",
            isPasswordVisible = false,
            uiState = LoginUiState.InvalidCredentials,
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onLoginClick = {}
        )
    }
}
