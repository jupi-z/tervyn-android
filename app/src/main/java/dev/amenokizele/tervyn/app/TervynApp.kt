package dev.amenokizele.tervyn.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import dev.amenokizele.tervyn.navigation.TervynNavGraph

@Composable
fun TervynApp(
    state: TervynAppState,
    onRetryLocalData: () -> Unit,
    onLogoutConfirmed: (() -> Unit) -> Unit
) {
    val navController = rememberNavController()
    TervynNavGraph(
        navController = navController,
        appState = state,
        onRetryLocalData = onRetryLocalData,
        onLogoutConfirmed = onLogoutConfirmed
    )
}
