package dev.amenokizele.tervyn.feature.sync

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.model.ThemeMode
import dev.amenokizele.tervyn.ui.components.InlineMessage
import dev.amenokizele.tervyn.ui.components.InlineMessageType
import dev.amenokizele.tervyn.ui.components.SectionHeader
import dev.amenokizele.tervyn.ui.components.TervynPrimaryButton
import dev.amenokizele.tervyn.ui.components.TervynTopAppBar
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme
import kotlinx.coroutines.launch

@Composable
fun SyncScreen(
    modifier: Modifier = Modifier,
    viewModel: SyncViewModel = viewModel()
) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val syncCompletedMessage = stringResource(R.string.message_sync_completed)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TervynTopAppBar(title = stringResource(R.string.title_sync))
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        SyncContent(
            isSyncing = isSyncing,
            isOffline = isOffline,
            syncError = syncError,
            lastSyncTime = lastSyncTime,
            pendingCount = pendingCount,
            onSyncClick = {
                viewModel.syncNow {
                    scope.launch {
                        snackbarHostState.showSnackbar(syncCompletedMessage)
                    }
                }
            },
            onToggleOffline = viewModel::toggleOffline,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun SyncContent(
    isSyncing: Boolean,
    isOffline: Boolean,
    syncError: String?,
    lastSyncTime: String,
    pendingCount: Int,
    onSyncClick: () -> Unit,
    onToggleOffline: () -> Unit,
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
                .testTag("sync_screen_content")
        ) {
            // Network mode status row - Flat Material Design
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = if (isOffline) {
                                stringResource(R.string.sync_offline_title)
                            } else {
                                stringResource(R.string.sync_demo_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isOffline) {
                                stringResource(R.string.sync_offline_no_system_network)
                            } else {
                                stringResource(R.string.sync_demo_network_online)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = !isOffline,
                    onCheckedChange = { onToggleOffline() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("network_toggle_switch")
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Main Sync State Section
            if (isSyncing) {
                SyncingBlock()
            } else if (syncError != null) {
                FailedBlock(error = syncError, onRetry = onSyncClick)
            } else if (pendingCount > 0) {
                PendingBlock(pendingCount = pendingCount, onSyncClick = onSyncClick)
            } else {
                SyncedBlock(lastSyncTime = lastSyncTime)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            // Additional details - Flat Sections
            SectionHeader(title = stringResource(R.string.sync_history_local))
            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sync_last_sync),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = lastSyncTime,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sync_mode_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.sync_mode_ui_simulation),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sync_mechanism_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.sync_manual_simulated),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun SyncedBlock(lastSyncTime: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.sync_up_to_date),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
            text = stringResource(R.string.sync_all_demo_marked_synced),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PendingBlock(pendingCount: Int, onSyncClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(Spacing.xl)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = stringResource(R.string.sync_pending_count, pendingCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs)
        ) {
            Text(
                text = stringResource(R.string.sync_pending_status_changes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = stringResource(R.string.sync_pending_checklist_updates),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = stringResource(R.string.sync_pending_notes_photos),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        TervynPrimaryButton(
            text = stringResource(R.string.sync_now),
            onClick = onSyncClick,
            icon = Icons.Default.Sync,
            testTag = "sync_now_button"
        )
    }
}

@Composable
private fun SyncingBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.sync_in_progress_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
            text = stringResource(R.string.sync_in_progress_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.sync_upload_in_progress),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FailedBlock(error: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        InlineMessage(
            text = stringResource(R.string.sync_failed_with_error, error),
            type = InlineMessageType.ERROR,
            modifier = Modifier.padding(bottom = Spacing.md)
        )

        TervynPrimaryButton(
            text = stringResource(R.string.action_retry),
            onClick = onRetry,
            icon = Icons.Default.Refresh,
            testTag = "retry_sync_button"
        )
    }
}

@Preview(name = "Sync Screen - Light", showBackground = true)
@Composable
private fun SyncScreenPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        SyncContent(
            isSyncing = false,
            isOffline = false,
            syncError = null,
            lastSyncTime = "Aujourd'hui · 14:32",
            pendingCount = 3,
            onSyncClick = {},
            onToggleOffline = {}
        )
    }
}

@Preview(name = "Sync Screen - Dark", showBackground = true)
@Composable
private fun SyncScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        SyncContent(
            isSyncing = false,
            isOffline = true,
            syncError = "Connexion réseau indisponible.",
            lastSyncTime = "Aujourd'hui · 08:00",
            pendingCount = 2,
            onSyncClick = {},
            onToggleOffline = {}
        )
    }
}
