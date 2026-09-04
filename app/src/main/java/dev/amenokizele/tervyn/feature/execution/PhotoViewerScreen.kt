package dev.amenokizele.tervyn.feature.execution

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import dev.amenokizele.tervyn.ui.components.EmptyState
import dev.amenokizele.tervyn.ui.components.SyncStateIndicator
import dev.amenokizele.tervyn.ui.components.TervynPrimaryButton
import dev.amenokizele.tervyn.ui.preview.TervynPreviewData
import dev.amenokizele.tervyn.ui.theme.ButtonShape
import dev.amenokizele.tervyn.ui.theme.Spacing
import dev.amenokizele.tervyn.ui.theme.TervynTheme
import kotlinx.coroutines.launch

@Composable
fun PhotoViewerScreen(
    jobId: String,
    photoId: String,
    onBackClick: () -> Unit,
    onPhotoDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExecutionViewModel = hiltViewModel()
) {
    val job by viewModel.job.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    PhotoViewerContent(
        job = job,
        photoId = photoId,
        onBackClick = onBackClick,
        onDeleteConfirmed = {
            scope.launch {
                if (viewModel.deletePhoto(jobId, photoId)) {
                    onPhotoDeleted()
                }
            }
        },
        modifier = modifier
    )
}

@Composable
private fun PhotoViewerContent(
    job: Job?,
    photoId: String,
    onBackClick: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateTimeFormatter = remember { TervynDateTimeFormatter() }
    var isConfirmingDelete by remember { mutableStateOf(false) }
    val photo = job?.attachments?.find { it.id == photoId }
    val canDelete = job?.status == JobStatus.IN_PROGRESS
    val backLabel = stringResource(R.string.action_back)
    val deletePhotoLabel = stringResource(R.string.action_delete_photo)
    val confirmDeleteLabel = stringResource(R.string.action_confirm_delete)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0E1210))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("photo_viewer_screen")
    ) {
        if (photo == null) {
            EmptyState(
                title = stringResource(R.string.title_photo_not_found),
                description = stringResource(R.string.photo_not_found_description),
                action = {
                    TervynPrimaryButton(text = backLabel, onClick = onBackClick)
                },
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("photo_viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backLabel,
                            tint = Color.White
                        )
                    }

                    Text(
                        text = photo.fileName ?: stringResource(R.string.add_photo_default_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (canDelete) {
                        IconButton(
                            onClick = { isConfirmingDelete = true },
                            modifier = Modifier.testTag("photo_viewer_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = deletePhotoLabel,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                }

                // Main Photo Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(Spacing.screenHorizontal),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF151A17))
                            .border(1.dp, Color(0xFF39453F), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color(0xFF5FC6AA),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = stringResource(
                                    R.string.photo_placeholder,
                                    photo.localUri?.substringAfterLast('/') ?: "PHOTO"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE8F0EC),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(Spacing.xxs))
                            Text(
                                text = photo.fileName ?: stringResource(R.string.add_photo_default_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFAAB7B0)
                            )
                        }
                    }
                }

                // Bottom Metadata and Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF151A17))
                        .padding(Spacing.screenHorizontal)
                ) {
                    if (isConfirmingDelete && canDelete) {
                        Text(
                            text = stringResource(R.string.photo_delete_confirm_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = stringResource(R.string.photo_delete_confirm_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAB7B0)
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            OutlinedButton(
                                onClick = { isConfirmingDelete = false },
                                shape = ButtonShape,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.action_cancel), color = Color.White)
                            }
                            OutlinedButton(
                                onClick = {
                                    onDeleteConfirmed()
                                    isConfirmingDelete = false
                                },
                                shape = ButtonShape,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.weight(1f).testTag("confirm_delete_photo_button")
                            ) {
                                Text(confirmDeleteLabel)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.photo_capture_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFAAB7B0)
                                )
                                Text(
                                    text = dateTimeFormatter.formatDayTime(photo.createdAt),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            SyncStateIndicator(syncState = photo.syncState, showWhenSynced = true)
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
            }
        }
    }
}

@Preview(name = "Photo Viewer Screen - Dark", showBackground = true)
@Composable
private fun PhotoViewerScreenPreview() {
    TervynTheme(themeMode = ThemeMode.DARK) {
        PhotoViewerContent(
            job = TervynPreviewData.inProgressJob,
            photoId = "preview-photo-1",
            onBackClick = {},
            onDeleteConfirmed = {}
        )
    }
}
