package dev.amenokizele.tervyn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.domain.model.ChecklistItem
import dev.amenokizele.tervyn.ui.theme.BadgeShape
import dev.amenokizele.tervyn.ui.theme.Spacing

@Composable
fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onToggle)
            .testTag("checklist_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.completed,
                onCheckedChange = { if (enabled) onToggle() },
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.testTag("checkbox_${item.id}")
            )

            Spacer(modifier = Modifier.width(Spacing.xs))

            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                modifier = Modifier.weight(1f)
            )

            if (item.required) {
                Spacer(modifier = Modifier.width(Spacing.xs))
                Box(
                    modifier = Modifier
                        .clip(BadgeShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), BadgeShape)
                        .padding(horizontal = Spacing.xs, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.checklist_required),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    }
}
