package dev.amenokizele.tervyn.feature.bootstrap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.app.LocalDataInitializationState
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme

@Composable
fun BootstrapScreen(
    initializationState: LocalDataInitializationState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.xl)
            .testTag("bootstrap_screen"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(400))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.title_login_brand),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.bootstrap_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                when (initializationState) {
                    LocalDataInitializationState.Initializing -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(
                            text = stringResource(R.string.bootstrap_initializing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    LocalDataInitializationState.Ready -> Unit

                    is LocalDataInitializationState.Error -> {
                        Text(
                            text = stringResource(R.string.bootstrap_error_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = stringResource(R.string.bootstrap_error_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Button(onClick = onRetry) {
                            Text(text = stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Bootstrap Initializing - Light", showBackground = true)
@Composable
private fun BootstrapInitializingPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        BootstrapScreen(
            initializationState = LocalDataInitializationState.Initializing,
            onRetry = {}
        )
    }
}

@Preview(name = "Bootstrap Initializing - Dark", showBackground = true)
@Composable
private fun BootstrapInitializingPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        BootstrapScreen(
            initializationState = LocalDataInitializationState.Initializing,
            onRetry = {}
        )
    }
}

@Preview(name = "Bootstrap Error - Light", showBackground = true)
@Composable
private fun BootstrapErrorPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        BootstrapScreen(
            initializationState = LocalDataInitializationState.Error(
                dev.amenokizele.tervyn.core.result.AppError.Storage("preview")
            ),
            onRetry = {}
        )
    }
}

@Preview(name = "Bootstrap Error - Dark", showBackground = true)
@Composable
private fun BootstrapErrorPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        BootstrapScreen(
            initializationState = LocalDataInitializationState.Error(
                dev.amenokizele.tervyn.core.result.AppError.Storage("preview")
            ),
            onRetry = {}
        )
    }
}
