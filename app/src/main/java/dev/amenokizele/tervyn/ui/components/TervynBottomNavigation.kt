package dev.amenokizele.tervyn.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.amenokizele.tervyn.R

enum class BottomBarDestination(val route: String) {
    JOBS("jobs"),
    SYNC("sync"),
    SETTINGS("settings")
}

@Composable
fun TervynBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    pendingSyncCount: Int = 0
) {
    val jobsLabel = stringResource(R.string.nav_jobs)
    val syncLabel = stringResource(R.string.nav_sync)
    val settingsLabel = stringResource(R.string.nav_settings)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                NavigationBarItem(
                    selected = currentRoute == BottomBarDestination.JOBS.route,
                    onClick = { onNavigate(BottomBarDestination.JOBS.route) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = jobsLabel
                        )
                    },
                    label = { Text(jobsLabel) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_item_jobs")
                )

            NavigationBarItem(
                selected = currentRoute == BottomBarDestination.SYNC.route,
                onClick = { onNavigate(BottomBarDestination.SYNC.route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (pendingSyncCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(text = "$pendingSyncCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = syncLabel
                        )
                    }
                },
                label = { Text(syncLabel) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_sync")
            )

            NavigationBarItem(
                selected = currentRoute == BottomBarDestination.SETTINGS.route,
                onClick = { onNavigate(BottomBarDestination.SETTINGS.route) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = settingsLabel
                    )
                },
                label = { Text(settingsLabel) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_settings")
            )
        }
    }
}
}
