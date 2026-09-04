package dev.amenokizele.tervyn.feature.jobdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.core.time.TervynDateTimeFormatter
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.SyncState
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.ui.components.EmptyState
import dev.amenokizele.tervyn.ui.components.JobStatusBadge
import dev.amenokizele.tervyn.ui.components.PriorityBadge
import dev.amenokizele.tervyn.ui.components.SectionHeader
import dev.amenokizele.tervyn.ui.components.SyncStateIndicator
import dev.amenokizele.tervyn.ui.components.TervynPrimaryButton
import dev.amenokizele.tervyn.ui.components.TervynSecondaryButton
import dev.amenokizele.tervyn.ui.components.TervynTopAppBar
import dev.amenokizele.tervyn.ui.preview.TervynPreviewData
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme
import kotlinx.coroutines.launch

@Composable
fun JobDetailScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onNavigateToExecution: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JobDetailViewModel = hiltViewModel()
) {
    val job by viewModel.job.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val jobStartedMessage = stringResource(R.string.message_job_started)

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    Scaffold(
        topBar = {
            TervynTopAppBar(
                title = stringResource(R.string.title_job_detail),
                subtitle = job?.reference,
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        if (job == null) {
            EmptyState(
                title = stringResource(R.string.title_job_not_found),
                description = stringResource(R.string.job_not_found_description),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            JobDetailContent(
                job = job!!,
                onStartJob = {
                    viewModel.startJob(jobId) {
                        scope.launch {
                            snackbarHostState.showSnackbar(jobStartedMessage)
                        }
                        onNavigateToExecution(jobId)
                    }
                },
                onContinueJob = { onNavigateToExecution(jobId) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun JobDetailContent(
    job: Job,
    onStartJob: () -> Unit,
    onContinueJob: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter = remember { TervynDateTimeFormatter() }
    val syncDescription = when (job.syncState) {
        SyncState.SYNCED -> stringResource(R.string.sync_state_synced_detail)
        SyncState.PENDING -> stringResource(R.string.sync_state_pending_detail)
        SyncState.SYNCING -> stringResource(R.string.sync_state_syncing_detail)
        SyncState.FAILED -> stringResource(R.string.sync_state_failed_detail)
    }
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
                .testTag("job_detail_content")
        ) {
            // Title & Reference
            Text(
                text = job.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = job.reference,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // Badges & Sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    PriorityBadge(priority = job.priority)
                    JobStatusBadge(status = job.status)
                }
                SyncStateIndicator(syncState = job.syncState, showWhenSynced = true)
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(Spacing.md))

            // Information fields
            DetailSection(label = stringResource(R.string.label_client), value = job.clientName)
            DetailSection(label = stringResource(R.string.label_site), value = job.siteName)
            DetailSection(label = stringResource(R.string.label_address), value = job.siteAddress.orEmpty())
            DetailSection(label = stringResource(R.string.label_scheduled), value = dateTimeFormatter.formatDayTime(job.scheduledAt))

            if (job.startedAt != null) {
                DetailSection(label = stringResource(R.string.label_started), value = dateTimeFormatter.formatDayTime(job.startedAt))
            }
            if (job.completedAt != null) {
                DetailSection(label = stringResource(R.string.label_completed), value = dateTimeFormatter.formatDayTime(job.completedAt))
            }

            DetailSection(label = stringResource(R.string.label_description), value = job.description.orEmpty())

            // Checklist summary
            SectionHeader(
                title = stringResource(R.string.label_checklist),
                trailingText = stringResource(R.string.checklist_completed_count, job.completedChecklistCount, job.totalChecklistCount)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))

            // Synchronisation
            SectionHeader(title = stringResource(R.string.title_sync))
            Text(
                text = syncDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Action Button based on status
            when (job.status) {
                JobStatus.ASSIGNED -> {
                    TervynPrimaryButton(
                        text = stringResource(R.string.action_start_job),
                        onClick = onStartJob,
                        icon = Icons.Default.PlayArrow,
                        testTag = "start_job_button"
                    )
                }
                JobStatus.IN_PROGRESS -> {
                    TervynPrimaryButton(
                        text = stringResource(R.string.action_continue_job),
                        onClick = onContinueJob,
                        testTag = "continue_job_button"
                    )
                }
                JobStatus.COMPLETED -> {
                    TervynSecondaryButton(
                        text = stringResource(R.string.action_view_completed_job),
                        onClick = onContinueJob,
                        testTag = "view_completed_job_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Composable
private fun DetailSection(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = Spacing.md)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "Job Detail Screen - Light", showBackground = true)
@Composable
private fun JobDetailScreenPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        JobDetailContent(
            job = TervynPreviewData.jobs.first(),
            onStartJob = {},
            onContinueJob = {}
        )
    }
}

@Preview(name = "Job Detail Screen - Dark", showBackground = true)
@Composable
private fun JobDetailScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        JobDetailContent(
            job = TervynPreviewData.inProgressJob,
            onStartJob = {},
            onContinueJob = {}
        )
    }
}
