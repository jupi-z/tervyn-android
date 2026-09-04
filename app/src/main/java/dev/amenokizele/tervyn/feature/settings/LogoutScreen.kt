package dev.amenokizele.tervyn.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.ui.components.TervynSecondaryButton
import dev.amenokizele.tervyn.ui.components.TervynTopAppBar
import dev.amenokizele.tervyn.ui.theme.ButtonShape
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme

@Composable
fun LogoutScreen(
    onBackClick: () -> Unit,
    onConfirmLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TervynTopAppBar(
                title = stringResource(R.string.title_logout),
                onBackClick = onBackClick
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(Spacing.screenHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    text = stringResource(R.string.logout_confirmation_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = stringResource(R.string.logout_confirmation_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                Button(
                    onClick = onConfirmLogout,
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_logout_button")
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm_logout),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                TervynSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onBackClick,
                    testTag = "cancel_logout_button"
                )
            }
        }
    }
}

@Preview(name = "Logout Screen - Light", showBackground = true)
@Composable
private fun LogoutScreenPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        LogoutScreen(
            onBackClick = {},
            onConfirmLogout = {}
        )
    }
}

@Preview(name = "Logout Screen - Dark", showBackground = true)
@Composable
private fun LogoutScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        LogoutScreen(
            onBackClick = {},
            onConfirmLogout = {}
        )
    }
}
