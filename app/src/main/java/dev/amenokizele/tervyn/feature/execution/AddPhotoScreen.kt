package dev.amenokizele.tervyn.feature.execution

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.amenokizele.tervyn.R
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
fun AddPhotoScreen(
    jobId: String,
    onBackClick: () -> Unit,
    onPhotoAdded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExecutionViewModel = viewModel()
) {
    val job by viewModel.job.collectAsState()
    val initialPhotoTitle = stringResource(R.string.add_photo_default_title)
    var photoTitle by remember { mutableStateOf(initialPhotoTitle) }
    var selectedTag by remember { mutableStateOf("CABLING") }
    var selectedSource by remember { mutableStateOf("CAMERA") }
    val canEdit = job?.status == JobStatus.IN_PROGRESS

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    Scaffold(
        topBar = {
            TervynTopAppBar(
                title = stringResource(R.string.title_add_photo),
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
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.lg)
                    .testTag("add_photo_screen_content")
            ) {
                Text(
                    text = stringResource(R.string.add_photo_source_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.xs))

                if (!canEdit) {
                    InlineMessage(
                        text = stringResource(R.string.add_photo_readonly),
                        type = InlineMessageType.WARNING,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                }

                val defaultFieldPhotoTitle = stringResource(R.string.add_photo_default_field)
                val defaultGalleryPhotoTitle = stringResource(R.string.add_photo_default_gallery)
                val defaultPhotoTitle = stringResource(R.string.add_photo_default_title)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    SourceTile(
                        icon = Icons.Default.CameraAlt,
                        label = stringResource(R.string.add_photo_take),
                        isSelected = selectedSource == "CAMERA",
                        enabled = canEdit,
                        onClick = {
                            selectedSource = "CAMERA"
                            if (photoTitle.isBlank()) photoTitle = defaultFieldPhotoTitle
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SourceTile(
                        icon = Icons.Default.PhotoLibrary,
                        label = stringResource(R.string.add_photo_gallery),
                        isSelected = selectedSource == "GALLERY",
                        enabled = canEdit,
                        onClick = {
                            selectedSource = "GALLERY"
                            if (photoTitle.isBlank()) photoTitle = defaultGalleryPhotoTitle
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    text = stringResource(R.string.add_photo_category_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.xs))

                val categories = listOf(
                    "CABLING" to stringResource(R.string.add_photo_category_cabling),
                    "DEVICE" to stringResource(R.string.add_photo_category_device),
                    "ENVIRONMENT" to stringResource(R.string.add_photo_category_environment),
                    "REPORT" to stringResource(R.string.add_photo_category_report)
                )

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    categories.forEach { (tag, label) ->
                        val isSelected = selectedTag == tag
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = canEdit) {
                                    selectedTag = tag
                                    photoTitle = label
                                }
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                TervynOutlinedTextField(
                    value = photoTitle,
                    onValueChange = { photoTitle = it },
                    label = stringResource(R.string.add_photo_caption_label),
                    placeholder = stringResource(R.string.add_photo_caption_placeholder),
                    singleLine = true,
                    enabled = canEdit,
                    testTag = "photo_title_input"
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                TervynPrimaryButton(
                    text = stringResource(R.string.action_add_photo),
                    onClick = {
                        if (viewModel.addPhoto(jobId, photoTitle.ifBlank { defaultPhotoTitle }, selectedTag)) {
                            onPhotoAdded()
                        }
                    },
                    enabled = canEdit,
                    testTag = "save_photo_button"
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                TervynSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onBackClick,
                    testTag = "cancel_photo_button"
                )
            }
        }
    }
}

@Composable
private fun SourceTile(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(Spacing.md),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Spacing.xl)
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Add Photo Screen - Light", showBackground = true)
@Composable
private fun AddPhotoScreenPreviewLight() {
    TervynTheme(themeMode = ThemeMode.LIGHT) {
        AddPhotoScreen(
            jobId = "job-001",
            onBackClick = {},
            onPhotoAdded = {}
        )
    }
}

@Preview(name = "Add Photo Screen - Dark", showBackground = true)
@Composable
private fun AddPhotoScreenPreviewDark() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        AddPhotoScreen(
            jobId = "job-001",
            onBackClick = {},
            onPhotoAdded = {}
        )
    }
}
