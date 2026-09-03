package dev.amenokizele.tervyn.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.amenokizele.tervyn.model.SyncState
import dev.amenokizele.tervyn.ui.theme.Spacing

@Composable
fun SyncStateIndicator(
    syncState: SyncState,
    modifier: Modifier = Modifier,
    showWhenSynced: Boolean = false
) {
    if (syncState == SyncState.SYNCED && !showWhenSynced) return

    val (icon, color, label) = when (syncState) {
        SyncState.SYNCED -> Triple(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary, "Synchronisé")
        SyncState.PENDING -> Triple(Icons.Default.Schedule, MaterialTheme.colorScheme.tertiary, "En attente")
        SyncState.SYNCING -> Triple(Icons.Default.Sync, MaterialTheme.colorScheme.tertiary, "En cours...")
        SyncState.FAILED -> Triple(Icons.Default.ErrorOutline, MaterialTheme.colorScheme.error, "Échec de sync")
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.xxs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
