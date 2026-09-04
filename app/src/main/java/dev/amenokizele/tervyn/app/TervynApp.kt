package dev.amenokizele.tervyn.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import dev.amenokizele.tervyn.navigation.TervynNavGraph

@Composable
fun TervynApp(
    state: TervynAppState,
    onLogoutConfirmed: (() -> Unit) -> Unit
) {
    val navController = rememberNavController()
    TervynNavGraph(
        navController = navController,
        appState = state,
        onLogoutConfirmed = onLogoutConfirmed
    )
}
