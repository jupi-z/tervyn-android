package dev.amenokizele.tervyn.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.amenokizele.tervyn.core.time.TervynDateTimeFormatter
import dev.amenokizele.tervyn.domain.model.Note
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.ui.theme.Spacing

@Composable
fun NoteListItem(
    note: Note,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter = remember { TervynDateTimeFormatter() }
    val authorLabel = when (note.authorUserId) {
        "user-amina" -> "Vous"
        "support-n1" -> "Support N1"
        else -> note.authorUserId
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm)
            .testTag("note_item_${note.id}")
    ) {
        Text(
            text = note.content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$authorLabel · ${dateTimeFormatter.formatTime(note.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (note.syncState != SyncState.SYNCED) {
                SyncStateIndicator(syncState = note.syncState)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    }
}
