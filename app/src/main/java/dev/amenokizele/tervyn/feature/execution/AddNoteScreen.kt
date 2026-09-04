package dev.amenokizele.tervyn.feature.execution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.domain.model.Job
import dev.amenokizele.tervyn.domain.model.JobStatus
import dev.amenokizele.tervyn.domain.model.ThemeMode
import dev.amenokizele.tervyn.ui.preview.TervynPreviewData
import dev.amenokizele.tervyn.ui.components.InlineMessage
import dev.amenokizele.tervyn.ui.components.InlineMessageType
import dev.amenokizele.tervyn.ui.components.TervynOutlinedTextField
import dev.amenokizele.tervyn.ui.components.TervynPrimaryButton
import dev.amenokizele.tervyn.ui.components.TervynSecondaryButton
import dev.amenokizele.tervyn.ui.components.TervynTopAppBar
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme
import kotlinx.coroutines.launch

@Composable
fun AddNoteScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onNoteAdded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExecutionViewModel = hiltViewModel()
) {
    val job by viewModel.job.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var noteContent by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val canEdit = job?.status == JobStatus.IN_PROGRESS

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    AddNoteContent(
        job = job,
        noteContent = noteContent,
        isError = isError,
        onNoteContentChange = {
            noteContent = it
            if (isError && it.isNotBlank()) isError = false
        },
        onSubmit = {
            if (noteContent.isBlank()) {
                isError = true
            } else {
                scope.launch {
                    if (viewModel.addNote(jobId, noteContent)) {
                        onNoteAdded()
                    }
                }
            }
        },
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@Composable
private fun AddNoteContent(
    job: Job?,
    noteContent: String,
    isError: Boolean,
    onNoteContentChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canEdit = job?.status == JobStatus.IN_PROGRESS

    Scaffold(
        topBar = {
            TervynTopAppBar(
                title = stringResource(R.string.title_add_note),
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
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg)
                    .testTag("add_note_screen_content")
            ) {
                Text(
                    text = stringResource(R.string.add_note_heading),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = stringResource(R.string.add_note_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                if (!canEdit) {
                    InlineMessage(
                        text = stringResource(R.string.add_note_readonly),
                        type = InlineMessageType.WARNING,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                }

                TervynOutlinedTextField(
                    value = noteContent,
                    onValueChange = onNoteContentChange,
                    label = stringResource(R.string.add_note_label),
                    placeholder = stringResource(R.string.add_note_placeholder),
                    singleLine = false,
                    minLines = 5,
                    maxLines = 10,
                    isError = isError,
                    errorMessage = stringResource(R.string.add_note_error_empty),
                    enabled = canEdit,
                    testTag = "note_content_input"
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                TervynPrimaryButton(
                    text = stringResource(R.string.action_add_note),
                    onClick = onSubmit,
                    enabled = canEdit,
                    testTag = "submit_note_button"
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                TervynSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onBackClick,
                    testTag = "cancel_note_button"
                )
            }
        }
    }
}

@Preview(name = "Add Note Screen - Light", showBackground = true)
@Composable
private fun AddNoteScreenPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        AddNoteContent(
            job = TervynPreviewData.inProgressJob,
            noteContent = "",
            isError = false,
            onNoteContentChange = {},
            onSubmit = {},
            onBackClick = {}
        )
    }
}

@Preview(name = "Add Note Screen - Dark", showBackground = true)
@Composable
private fun AddNoteScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        AddNoteContent(
            job = TervynPreviewData.inProgressJob,
            noteContent = "Contrôle visuel terminé, RAS.",
            isError = false,
            onNoteContentChange = {},
            onSubmit = {},
            onBackClick = {}
        )
    }
}
