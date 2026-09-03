package dev.amenokizele.tervyn.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.ui.theme.Spacing

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    onRetry: (() -> Unit)? = null
) {
    val resolvedTitle = title ?: stringResource(R.string.error_state_title)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xl)
            .testTag("error_state_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Spacing.xxl)
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = resolvedTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            TervynSecondaryButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
                testTag = "error_retry_button"
            )
        }
    }
}
