package dev.amenokizele.tervyn.feature.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.amenokizele.tervyn.demo.DemoData
import dev.amenokizele.tervyn.model.DemoJob
import dev.amenokizele.tervyn.model.JobFilter
import dev.amenokizele.tervyn.model.ThemeMode
import dev.amenokizele.tervyn.ui.components.EmptyState
import dev.amenokizele.tervyn.ui.components.JobListItem
import dev.amenokizele.tervyn.ui.components.OfflineBanner
import dev.amenokizele.tervyn.ui.components.TervynFilterTabs
import dev.amenokizele.tervyn.ui.components.TervynSearchBar
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme

@Composable
fun JobsScreen(
    onJobClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JobsViewModel = viewModel()
) {
    val jobs by viewModel.jobs.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val filterCounts by viewModel.filterCounts.collectAsState()

    JobsContent(
        jobs = jobs,
        selectedFilter = selectedFilter,
        searchQuery = searchQuery,
        isOffline = isOffline,
        filterCounts = filterCounts,
        onFilterSelected = viewModel::setFilter,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onClearSearch = viewModel::clearSearch,
        onJobClick = onJobClick,
        modifier = modifier
    )
}

@Composable
fun JobsContent(
    jobs: List<DemoJob>,
    selectedFilter: JobFilter,
    searchQuery: String,
    isOffline: Boolean,
    filterCounts: Map<JobFilter, Int>,
    onFilterSelected: (JobFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onJobClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header Surface starting at the screen boundary and extending behind the status bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bonjour, Amina",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Interventions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Aujourd'hui",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "02 septembre",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Offline Banner
                if (isOffline) {
                    Box(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xxs)) {
                        OfflineBanner()
                    }
                }

                // Search Bar
                Box(
                    modifier = Modifier.padding(
                        horizontal = Spacing.screenHorizontal,
                        vertical = Spacing.xs
                    )
                ) {
                    TervynSearchBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        placeholder = "Rechercher une intervention...",
                        onClear = onClearSearch
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                // Filter Tabs
                TervynFilterTabs(
                    selectedFilter = selectedFilter,
                    onFilterSelected = onFilterSelected,
                    counts = filterCounts
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            }
        }

        // Jobs List or Empty State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (jobs.isEmpty()) {
                EmptyState(
                    title = "Aucune intervention",
                    description = if (searchQuery.isNotBlank()) {
                        "Aucun résultat pour « $searchQuery »."
                    } else {
                        "Vous n'avez aucune intervention correspondant à ce filtre."
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .testTag("jobs_list"),
                    contentPadding = PaddingValues(
                        horizontal = Spacing.screenHorizontal,
                        vertical = Spacing.md
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(
                        items = jobs,
                        key = { it.id }
                    ) { job ->
                        JobListItem(
                            job = job,
                            onClick = { onJobClick(job.id) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Jobs Screen - Light", showBackground = true)
@Composable
private fun JobsScreenPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        JobsContent(
            jobs = DemoData.initialJobs.take(4),
            selectedFilter = JobFilter.ALL,
            searchQuery = "",
            isOffline = true,
            filterCounts = mapOf(JobFilter.ALL to 10, JobFilter.ASSIGNED to 7, JobFilter.IN_PROGRESS to 1, JobFilter.COMPLETED to 2),
            onFilterSelected = {},
            onSearchQueryChange = {},
            onClearSearch = {},
            onJobClick = {}
        )
    }
}

@Preview(name = "Jobs Screen - Dark", showBackground = true)
@Composable
private fun JobsScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        JobsContent(
            jobs = DemoData.initialJobs.take(4),
            selectedFilter = JobFilter.ASSIGNED,
            searchQuery = "",
            isOffline = false,
            filterCounts = mapOf(JobFilter.ALL to 10, JobFilter.ASSIGNED to 7, JobFilter.IN_PROGRESS to 1, JobFilter.COMPLETED to 2),
            onFilterSelected = {},
            onSearchQueryChange = {},
            onClearSearch = {},
            onJobClick = {}
        )
    }
}
