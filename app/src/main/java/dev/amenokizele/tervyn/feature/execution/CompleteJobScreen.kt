package dev.amenokizele.tervyn.feature.execution

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.ui.components.InlineMessage
import dev.amenokizele.tervyn.ui.components.InlineMessageType
import dev.amenokizele.tervyn.ui.components.TervynPrimaryButton
import dev.amenokizele.tervyn.ui.components.TervynSecondaryButton
import dev.amenokizele.tervyn.ui.components.TervynTopAppBar
import dev.amenokizele.tervyn.ui.preview.TervynPreviewData
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme
import kotlinx.coroutines.launch

@Composable
fun CompleteJobScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onJobCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExecutionViewModel = hiltViewModel()
) {
    val job by viewModel.job.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val canComplete = job?.status == JobStatus.IN_PROGRESS && job?.allRequiredCompleted == true

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    CompleteJobContent(
        job = job,
        onBackClick = onBackClick,
        onCompleteClick = {
            scope.launch {
                val success = viewModel.completeJob(jobId)
                if (success) {
                    onJobCompleted()
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun CompleteJobContent(
    job: Job?,
    onBackClick: () -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canComplete = job?.status == JobStatus.IN_PROGRESS && job.allRequiredCompleted

    Scaffold(
        topBar = {
            TervynTopAppBar(
                title = stringResource(R.string.title_complete_job),
                subtitle = job?.reference,
                onBackClick = onBackClick
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(Spacing.screenHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    text = stringResource(R.string.complete_confirm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = stringResource(R.string.complete_confirm_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Summary stats
                job?.let { currentJob ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(Spacing.md)
                    ) {
                        Text(
                            text = stringResource(R.string.complete_summary_title),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        SummaryRow(
                            label = stringResource(R.string.complete_summary_checklist),
                            value = "${currentJob.completedChecklistCount} / ${currentJob.totalChecklistCount}"
                        )
                        SummaryRow(
                            label = stringResource(R.string.complete_summary_notes),
                            value = "${currentJob.notes.size}"
                        )
                        SummaryRow(
                            label = stringResource(R.string.complete_summary_photos),
                            value = "${currentJob.attachments.size}"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                if (!canComplete) {
                    InlineMessage(
                        text = stringResource(R.string.complete_unavailable),
                        type = InlineMessageType.WARNING,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                }

                TervynPrimaryButton(
                    text = stringResource(R.string.action_complete_job),
                    onClick = onCompleteClick,
                    enabled = canComplete,
                    icon = Icons.Default.CheckCircle,
                    testTag = "confirm_complete_job_button"
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                TervynSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onBackClick,
                    testTag = "cancel_complete_job_button"
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview(name = "Complete Job Screen - Light", showBackground = true)
@Composable
private fun CompleteJobScreenPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        CompleteJobContent(
            job = TervynPreviewData.inProgressJob,
            onBackClick = {},
            onCompleteClick = {}
        )
    }
}

@Preview(name = "Complete Job Screen - Dark", showBackground = true)
@Composable
private fun CompleteJobScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        CompleteJobContent(
            job = TervynPreviewData.inProgressJob,
            onBackClick = {},
            onCompleteClick = {}
        )
    }
}
