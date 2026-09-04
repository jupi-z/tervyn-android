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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.ui.components.ChecklistItemRow
import dev.amenokizele.tervyn.ui.components.EmptyState
import dev.amenokizele.tervyn.ui.components.InlineMessage
import dev.amenokizele.tervyn.ui.components.InlineMessageType
import dev.amenokizele.tervyn.ui.components.NoteListItem
import dev.amenokizele.tervyn.ui.components.PhotoGridItem
import dev.amenokizele.tervyn.ui.components.SectionHeader
import dev.amenokizele.tervyn.ui.components.TervynPrimaryButton
import dev.amenokizele.tervyn.ui.components.TervynTopAppBar
import dev.amenokizele.tervyn.ui.preview.TervynPreviewData
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme
import kotlinx.coroutines.launch

@Composable
fun ExecutionScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onNavigateToAddNote: (String) -> Unit,
    onNavigateToAddPhoto: (String) -> Unit,
    onNavigateToPhotoViewer: (String, String) -> Unit,
    onNavigateToCompleteJob: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExecutionViewModel = hiltViewModel()
) {
    val job by viewModel.job.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    Scaffold(
        topBar = {
            TervynTopAppBar(
                title = stringResource(R.string.title_execution),
                subtitle = job?.reference,
                onBackClick = onBackClick
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (job == null) {
            EmptyState(
                title = stringResource(R.string.title_job_not_found),
                description = stringResource(R.string.execution_not_found_description),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            ExecutionContent(
                job = job!!,
                onToggleChecklist = { itemId ->
                    scope.launch {
                        viewModel.toggleChecklistItem(jobId, itemId)
                    }
                },
                onAddNoteClick = { onNavigateToAddNote(jobId) },
                onAddPhotoClick = { onNavigateToAddPhoto(jobId) },
                onPhotoClick = { photoId -> onNavigateToPhotoViewer(jobId, photoId) },
                onCompleteClick = { onNavigateToCompleteJob(jobId) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun ExecutionContent(
    job: Job,
    onToggleChecklist: (String) -> Unit,
    onAddNoteClick: () -> Unit,
    onAddPhotoClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onCompleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter = remember { TervynDateTimeFormatter() }
    val isEditable = job.status == JobStatus.IN_PROGRESS
    val canComplete = isEditable && job.allRequiredCompleted

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
                .testTag("execution_screen_content")
        ) {
            // Header summary
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${job.clientName} · ${job.siteName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.lg))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(Spacing.md))

            // SECTION 1 : CHECKLIST
            SectionHeader(
                title = stringResource(R.string.label_checklist),
                trailingText = "${job.completedChecklistCount}/${job.totalChecklistCount}"
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                job.checklist.forEach { item ->
                    ChecklistItemRow(
                        item = item,
                        onToggle = { onToggleChecklist(item.id) },
                        enabled = isEditable
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // SECTION 2 : NOTES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = stringResource(R.string.label_notes),
                    trailingText = "${job.notes.size}",
                    modifier = Modifier.weight(1f)
                )

                if (isEditable) {
                    TextButton(
                        onClick = onAddNoteClick,
                        modifier = Modifier.testTag("add_note_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(stringResource(R.string.action_add_note_short))
                    }
                }
            }

            if (job.notes.isEmpty()) {
                Text(
                    text = stringResource(R.string.notes_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.sm)
                )
            } else {
                job.notes.forEach { note ->
                    NoteListItem(note = note)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // SECTION 3 : PHOTOS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = stringResource(R.string.label_photos),
                    trailingText = "${job.attachments.size}",
                    modifier = Modifier.weight(1f)
                )

                if (isEditable) {
                    TextButton(
                        onClick = onAddPhotoClick,
                        modifier = Modifier.testTag("add_photo_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(stringResource(R.string.action_add_photo_short))
                    }
                }
            }

            if (job.attachments.isEmpty()) {
                Text(
                    text = stringResource(R.string.photos_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.sm)
                )
            } else {
                // 2 columns photo grid
                val chunkedPhotos = job.attachments.chunked(2)
                chunkedPhotos.forEach { rowPhotos ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        rowPhotos.forEach { photo ->
                            Box(modifier = Modifier.weight(1f)) {
                                PhotoGridItem(
                                    photo = photo,
                                    onClick = { onPhotoClick(photo.id) }
                                )
                            }
                        }
                        if (rowPhotos.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // SECTION 4 : TERMINER L'INTERVENTION
            if (isEditable) {
                if (!canComplete) {
                    InlineMessage(
                        text = stringResource(R.string.execution_required_warning),
                        type = InlineMessageType.WARNING,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                }

                TervynPrimaryButton(
                    text = stringResource(R.string.action_complete_job),
                    onClick = onCompleteClick,
                    enabled = canComplete,
                    icon = Icons.Default.CheckCircle,
                    testTag = "complete_job_action_button"
                )
            } else if (job.status == JobStatus.COMPLETED) {
                InlineMessage(
                    text = stringResource(
                        R.string.execution_completed_readonly,
                        dateTimeFormatter.formatDayTime(job.completedAt).ifBlank {
                            stringResource(R.string.execution_completed_fallback_time)
                        }
                    ),
                    type = InlineMessageType.SUCCESS
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}

@Preview(name = "Execution Screen - Light", showBackground = true)
@Composable
private fun ExecutionScreenPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        ExecutionContent(
            job = TervynPreviewData.inProgressJob,
            onToggleChecklist = {},
            onAddNoteClick = {},
            onAddPhotoClick = {},
            onPhotoClick = {},
            onCompleteClick = {}
        )
    }
}

@Preview(name = "Execution Screen - Dark", showBackground = true)
@Composable
private fun ExecutionScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        ExecutionContent(
            job = TervynPreviewData.inProgressJob,
            onToggleChecklist = {},
            onAddNoteClick = {},
            onAddPhotoClick = {},
            onPhotoClick = {},
            onCompleteClick = {}
        )
    }
}
