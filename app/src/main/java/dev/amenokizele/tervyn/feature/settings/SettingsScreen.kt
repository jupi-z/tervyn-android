package dev.amenokizele.tervyn.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.amenokizele.tervyn.model.ThemeMode
import dev.amenokizele.tervyn.ui.components.SectionHeader
import dev.amenokizele.tervyn.ui.components.TervynSecondaryButton
import dev.amenokizele.tervyn.ui.components.TervynTopAppBar
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme

@Composable
fun SettingsScreen(
    onNavigateToLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val currentTheme by viewModel.currentTheme.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TervynTopAppBar(title = "Paramètres")
        },
        modifier = modifier
    ) { innerPadding ->
        SettingsContent(
            currentTheme = currentTheme,
            lastSyncTime = lastSyncTime,
            onThemeSelected = viewModel::setTheme,
            onLogoutClick = onNavigateToLogout,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun SettingsContent(
    currentTheme: ThemeMode,
    lastSyncTime: String,
    onThemeSelected: (ThemeMode) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg)
                .testTag("settings_screen_content")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.iconMedium)
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.md))

                Column {
                    Text(
                        text = "Amina Kizele",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "amina@tervyn.demo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Technicienne terrain N2",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            SectionDivider()

            SectionHeader(title = "Apparence")
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = currentTheme == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onThemeSelected(mode) }
                            .padding(vertical = Spacing.sm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                ThemeMode.SYSTEM -> "Système"
                                ThemeMode.LIGHT -> "Clair"
                                ThemeMode.DARK -> "Sombre"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionDivider()

            SectionHeader(title = "Synchronisation")
            Spacer(modifier = Modifier.height(Spacing.xs))
            SettingsInfoRow(
                icon = Icons.Default.Sync,
                label = "Dernière synchronisation",
                value = lastSyncTime
            )

            SectionDivider()

            SectionHeader(title = "À propos")
            Spacer(modifier = Modifier.height(Spacing.xs))
            SettingsInfoRow(label = "Application", value = "Tervyn")
            SettingsInfoRow(label = "Version", value = "1.0")
            SettingsInfoRow(label = "Licence", value = "MIT")

            SectionDivider(extraTop = Spacing.xl)

            TervynSecondaryButton(
                text = "Se déconnecter",
                onClick = onLogoutClick,
                icon = Icons.Default.ExitToApp,
                testTag = "settings_logout_button"
            )

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun SettingsInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Spacing.iconSmall)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionDivider(extraTop: Dp = Spacing.lg) {
    Spacer(modifier = Modifier.height(extraTop))
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    )
    Spacer(modifier = Modifier.height(Spacing.lg))
}

@Preview(name = "Settings Screen - Light", showBackground = true)
@Composable
private fun SettingsScreenPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        SettingsContent(
            currentTheme = ThemeMode.LIGHT,
            lastSyncTime = "Aujourd'hui · 14:32",
            onThemeSelected = {},
            onLogoutClick = {}
        )
    }
}

@Preview(name = "Settings Screen - Dark", showBackground = true)
@Composable
private fun SettingsScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        SettingsContent(
            currentTheme = ThemeMode.DARK,
            lastSyncTime = "Aujourd'hui · 08:00",
            onThemeSelected = {},
            onLogoutClick = {}
        )
    }
}
