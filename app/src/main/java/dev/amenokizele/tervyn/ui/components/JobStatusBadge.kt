package dev.amenokizele.tervyn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.ui.theme.BadgeShape
import dev.amenokizele.tervyn.ui.theme.Spacing

@Composable
fun JobStatusBadge(
    status: JobStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, borderColor) = when (status) {
        JobStatus.ASSIGNED -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        JobStatus.IN_PROGRESS -> Triple(
            Color(0xFF315F8C).copy(alpha = 0.12f),
            Color(0xFF315F8C),
            Color(0xFF315F8C).copy(alpha = 0.4f)
        )
        JobStatus.COMPLETED -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
    }
    val label = when (status) {
        JobStatus.ASSIGNED -> stringResource(R.string.status_assigned)
        JobStatus.IN_PROGRESS -> stringResource(R.string.status_in_progress)
        JobStatus.COMPLETED -> stringResource(R.string.status_completed)
    }

    Box(
        modifier = modifier
            .clip(BadgeShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, BadgeShape)
            .padding(horizontal = Spacing.xs, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
