package dev.amenokizele.tervyn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.model.JobFilter
import dev.amenokizele.tervyn.ui.theme.Spacing

@Composable
fun TervynFilterTabs(
    selectedFilter: JobFilter,
    onFilterSelected: (JobFilter) -> Unit,
    modifier: Modifier = Modifier,
    counts: Map<JobFilter, Int>? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        JobFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            val count = counts?.get(filter)
            val filterLabel = when (filter) {
                JobFilter.ALL -> stringResource(R.string.filter_all)
                JobFilter.ASSIGNED -> stringResource(R.string.filter_assigned)
                JobFilter.IN_PROGRESS -> stringResource(R.string.filter_in_progress)
                JobFilter.COMPLETED -> stringResource(R.string.filter_completed)
            }

            val backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }

            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            val borderColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable(
                        role = Role.Tab,
                        onClick = { onFilterSelected(filter) }
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("filter_tab_${filter.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                val labelText = if (count != null) {
                    stringResource(R.string.filter_with_count, filterLabel, count)
                } else {
                    filterLabel
                }

                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = contentColor
                )
            }
        }
    }
}
