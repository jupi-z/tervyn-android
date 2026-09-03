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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.amenokizele.tervyn.model.JobPriority
import dev.amenokizele.tervyn.ui.theme.BadgeShape
import dev.amenokizele.tervyn.ui.theme.Spacing

@Composable
fun PriorityBadge(
    priority: JobPriority,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, borderColor) = when (priority) {
        JobPriority.LOW -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
        JobPriority.NORMAL -> Triple(
            Color(0xFF315F8C).copy(alpha = 0.12f),
            Color(0xFF315F8C),
            Color(0xFF315F8C).copy(alpha = 0.3f)
        )
        JobPriority.HIGH -> Triple(
            Color(0xFF9A6700).copy(alpha = 0.12f),
            Color(0xFF9A6700),
            Color(0xFF9A6700).copy(alpha = 0.3f)
        )
        JobPriority.URGENT -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
        )
    }

    Box(
        modifier = modifier
            .clip(BadgeShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, BadgeShape)
            .padding(horizontal = Spacing.xs, vertical = 2.dp)
    ) {
        Text(
            text = priority.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
