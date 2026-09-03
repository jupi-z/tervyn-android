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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.amenokizele.tervyn.model.JobStatus
import dev.amenokizele.tervyn.model.ThemeMode
import dev.amenokizele.tervyn.ui.components.InlineMessage
import dev.amenokizele.tervyn.ui.components.InlineMessageType
import dev.amenokizele.tervyn.ui.components.TervynOutlinedTextField
import dev.amenokizele.tervyn.ui.components.TervynPrimaryButton
import dev.amenokizele.tervyn.ui.components.TervynSecondaryButton
import dev.amenokizele.tervyn.ui.components.TervynTopAppBar
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme

@Composable
fun AddNoteScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onNoteAdded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExecutionViewModel = viewModel()
) {
    val job by viewModel.job.collectAsState()
    var noteContent by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val canEdit = job?.status == JobStatus.IN_PROGRESS

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    Scaffold(
        topBar = {
            TervynTopAppBar(
                title = "Nouvelle note",
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
                    text = "Observations terrain",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = "Ajoutez une note d'intervention. Elle sera horodatée et marquée en attente de synchronisation simulée.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                if (!canEdit) {
                    InlineMessage(
                        text = "Les notes peuvent être ajoutées uniquement pendant une intervention en cours.",
                        type = InlineMessageType.WARNING,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                }

                TervynOutlinedTextField(
                    value = noteContent,
                    onValueChange = {
                        noteContent = it
                        if (isError && it.isNotBlank()) isError = false
                    },
                    label = "Détails de la note",
                    placeholder = "Saisissez les détails de l'opération, matériel utilisé ou anomalies constatées...",
                    singleLine = false,
                    minLines = 5,
                    maxLines = 10,
                    isError = isError,
                    errorMessage = "Veuillez saisir du texte pour ajouter cette note.",
                    enabled = canEdit,
                    testTag = "note_content_input"
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                TervynPrimaryButton(
                    text = "Ajouter la note",
                    onClick = {
                        if (noteContent.isBlank()) {
                            isError = true
                        } else if (viewModel.addNote(jobId, noteContent)) {
                            onNoteAdded()
                        }
                    },
                    enabled = canEdit,
                    testTag = "submit_note_button"
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                TervynSecondaryButton(
                    text = "Annuler",
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
        AddNoteScreen(
            jobId = "job-001",
            onBackClick = {},
            onNoteAdded = {}
        )
    }
}

@Preview(name = "Add Note Screen - Dark", showBackground = true)
@Composable
private fun AddNoteScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        AddNoteScreen(
            jobId = "job-001",
            onBackClick = {},
            onNoteAdded = {}
        )
    }
}
