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
import dev.amenokizele.tervyn.demo.DemoData
import dev.amenokizele.tervyn.model.DemoJob
import dev.amenokizele.tervyn.model.JobStatus
import dev.amenokizele.tervyn.model.ThemeMode
import dev.amenokizele.tervyn.ui.components.EmptyState
import dev.amenokizele.tervyn.ui.components.JobStatusBadge
import dev.amenokizele.tervyn.ui.components.PriorityBadge
import dev.amenokizele.tervyn.ui.components.SectionHeader
import dev.amenokizele.tervyn.ui.components.SyncStateIndicator
import dev.amenokizele.tervyn.ui.components.TervynPrimaryButton
import dev.amenokizele.tervyn.ui.components.TervynSecondaryButton
import dev.amenokizele.tervyn.ui.components.TervynTopAppBar
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme
import kotlinx.coroutines.launch

@Composable
fun JobDetailScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onNavigateToExecution: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JobDetailViewModel = viewModel()
) {
    val job by viewModel.job.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val jobStartedMessage = stringResource(R.string.message_job_started)

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    Scaffold(
        topBar = {
            TervynTopAppBar(
                title = "Intervention",
                subtitle = job?.reference,
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        if (job == null) {
            EmptyState(
                title = "Intervention introuvable",
                description = "Cette intervention n'existe pas ou a été supprimée.",
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
    job: DemoJob,
    onStartJob: () -> Unit,
    onContinueJob: () -> Unit,
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
            DetailSection(label = "Client", value = job.clientName)
            DetailSection(label = "Site", value = job.siteName)
            DetailSection(label = "Adresse", value = job.siteAddress)
            DetailSection(label = "Prévue", value = "02 septembre · ${job.scheduledAt}")

            if (job.startedAt != null) {
                DetailSection(label = "Démarrée", value = "02 septembre · ${job.startedAt}")
            }
            if (job.completedAt != null) {
                DetailSection(label = "Terminée", value = "02 septembre · ${job.completedAt}")
            }

            DetailSection(label = "Description", value = job.description)

            // Checklist summary
            SectionHeader(
                title = "Checklist",
                trailingText = "${job.completedChecklistCount} / ${job.totalChecklistCount} terminées"
            )
            Spacer(modifier = Modifier.height(Spacing.xs))

            // Synchronisation
            SectionHeader(title = "Synchronisation")
            Text(
                text = when (job.syncState) {
                    dev.amenokizele.tervyn.model.SyncState.SYNCED -> "Toutes les modifications sont synchronisées."
                    dev.amenokizele.tervyn.model.SyncState.PENDING -> "Modifications locales en attente de synchronisation."
                    dev.amenokizele.tervyn.model.SyncState.SYNCING -> "Synchronisation simulée en cours..."
                    dev.amenokizele.tervyn.model.SyncState.FAILED -> "Échec de synchronisation. Réessai requis."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Action Button based on status
            when (job.status) {
                JobStatus.ASSIGNED -> {
                    TervynPrimaryButton(
                        text = "Démarrer l'intervention",
                        onClick = onStartJob,
                        icon = Icons.Default.PlayArrow,
                        testTag = "start_job_button"
                    )
                }
                JobStatus.IN_PROGRESS -> {
                    TervynPrimaryButton(
                        text = "Continuer l'intervention",
                        onClick = onContinueJob,
                        testTag = "continue_job_button"
                    )
                }
                JobStatus.COMPLETED -> {
                    TervynSecondaryButton(
                        text = "Consulter l'exécution (Terminée)",
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
            job = DemoData.initialJobs.first(),
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
            job = DemoData.initialJobs[2],
            onStartJob = {},
            onContinueJob = {}
        )
    }
}
