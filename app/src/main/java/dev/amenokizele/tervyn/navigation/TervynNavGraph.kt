package dev.amenokizele.tervyn.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import androidx.navigation.navArgument
import dev.amenokizele.tervyn.R
import dev.amenokizele.tervyn.app.TervynAppState
import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.feature.auth.LoginScreen
import dev.amenokizele.tervyn.feature.bootstrap.BootstrapScreen
import dev.amenokizele.tervyn.feature.execution.AddNoteScreen
import dev.amenokizele.tervyn.feature.execution.AddPhotoScreen
import dev.amenokizele.tervyn.feature.execution.CompleteJobScreen
import dev.amenokizele.tervyn.feature.execution.ExecutionScreen
import dev.amenokizele.tervyn.feature.execution.PhotoViewerScreen
import dev.amenokizele.tervyn.feature.jobdetail.JobDetailScreen
import dev.amenokizele.tervyn.feature.jobs.JobsScreen
import dev.amenokizele.tervyn.feature.settings.LogoutScreen
import dev.amenokizele.tervyn.feature.settings.SettingsScreen
import dev.amenokizele.tervyn.feature.sync.SyncScreen
import dev.amenokizele.tervyn.ui.components.BottomBarDestination
import dev.amenokizele.tervyn.ui.components.TervynBottomNavigation
import kotlinx.coroutines.launch

@Composable
fun TervynNavGraph(
    navController: NavHostController,
    appState: TervynAppState,
    onLogoutConfirmed: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = listOf(
        TervynDestination.Jobs.route,
        TervynDestination.Sync.route,
        TervynDestination.Settings.route
    )

    val showBottomBar = currentRoute in topLevelRoutes

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val noteAddedMessage = stringResource(R.string.message_note_added)
    val photoAddedMessage = stringResource(R.string.message_photo_added)
    val photoDeletedMessage = stringResource(R.string.message_photo_deleted)
    val jobCompletedMessage = stringResource(R.string.message_job_completed)
    val loggedOutMessage = stringResource(R.string.message_logged_out)

    LaunchedEffect(appState.authState) {
        when (appState.authState) {
            AuthState.Checking -> Unit
            is AuthState.Authenticated -> {
                navController.navigate(NavGraph.App.route) {
                    popUpTo(NavGraph.Root.route) {
                        inclusive = false
                        saveState = false
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
            AuthState.Unauthenticated -> {
                navController.navigate(NavGraph.Auth.route) {
                    popUpTo(NavGraph.Root.route) {
                        inclusive = false
                        saveState = false
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                TervynBottomNavigation(
                    currentRoute = currentRoute ?: TervynDestination.Jobs.route,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(NavGraph.App.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    pendingSyncCount = appState.pendingSyncCount
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TervynDestination.Bootstrap.route,
            route = NavGraph.Root.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            composable(TervynDestination.Bootstrap.route) {
                BootstrapScreen()
            }

            navigation(
                startDestination = TervynDestination.Login.route,
                route = NavGraph.Auth.route
            ) {
                composable(TervynDestination.Login.route) {
                    LoginScreen()
                }
            }

            navigation(
                startDestination = TervynDestination.Jobs.route,
                route = NavGraph.App.route
            ) {
                composable(TervynDestination.Jobs.route) {
                    JobsScreen(
                        onJobClick = { jobId ->
                            navController.navigate(TervynDestination.JobDetail.createRoute(jobId))
                        }
                    )
                }

                composable(TervynDestination.Sync.route) {
                    SyncScreen()
                }

                composable(TervynDestination.Settings.route) {
                    SettingsScreen(
                        onNavigateToLogout = {
                            navController.navigate(TervynDestination.Logout.route)
                        }
                    )
                }

                composable(
                    route = TervynDestination.JobDetail.route,
                    arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val jobId = backStackEntry.arguments?.getString("jobId").orEmpty()
                    JobDetailScreen(
                        jobId = jobId,
                        onBackClick = { navController.popBackStack() },
                        onNavigateToExecution = { id ->
                            navController.navigate(TervynDestination.Execution.createRoute(id))
                        }
                    )
                }

                composable(
                    route = TervynDestination.Execution.route,
                    arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val jobId = backStackEntry.arguments?.getString("jobId").orEmpty()
                    ExecutionScreen(
                        jobId = jobId,
                        onBackClick = { navController.popBackStack() },
                        onNavigateToAddNote = { id ->
                            navController.navigate(TervynDestination.AddNote.createRoute(id))
                        },
                        onNavigateToAddPhoto = { id ->
                            navController.navigate(TervynDestination.AddPhoto.createRoute(id))
                        },
                        onNavigateToPhotoViewer = { id, photoId ->
                            navController.navigate(TervynDestination.PhotoViewer.createRoute(id, photoId))
                        },
                        onNavigateToCompleteJob = { id ->
                            navController.navigate(TervynDestination.CompleteJob.createRoute(id))
                        }
                    )
                }

                composable(
                    route = TervynDestination.AddNote.route,
                    arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val jobId = backStackEntry.arguments?.getString("jobId").orEmpty()
                    AddNoteScreen(
                        jobId = jobId,
                        onBackClick = { navController.popBackStack() },
                        onNoteAdded = {
                            navController.popBackStack()
                            scope.launch {
                                snackbarHostState.showSnackbar(noteAddedMessage)
                            }
                        }
                    )
                }

                composable(
                    route = TervynDestination.AddPhoto.route,
                    arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val jobId = backStackEntry.arguments?.getString("jobId").orEmpty()
                    AddPhotoScreen(
                        jobId = jobId,
                        onBackClick = { navController.popBackStack() },
                        onPhotoAdded = {
                            navController.popBackStack()
                            scope.launch {
                                snackbarHostState.showSnackbar(photoAddedMessage)
                            }
                        }
                    )
                }

                composable(
                    route = TervynDestination.PhotoViewer.route,
                    arguments = listOf(
                        navArgument("jobId") { type = NavType.StringType },
                        navArgument("photoId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val jobId = backStackEntry.arguments?.getString("jobId").orEmpty()
                    val photoId = backStackEntry.arguments?.getString("photoId").orEmpty()
                    PhotoViewerScreen(
                        jobId = jobId,
                        photoId = photoId,
                        onBackClick = { navController.popBackStack() },
                        onPhotoDeleted = {
                            navController.popBackStack()
                            scope.launch {
                                snackbarHostState.showSnackbar(photoDeletedMessage)
                            }
                        }
                    )
                }

                composable(
                    route = TervynDestination.CompleteJob.route,
                    arguments = listOf(navArgument("jobId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val jobId = backStackEntry.arguments?.getString("jobId").orEmpty()
                    CompleteJobScreen(
                        jobId = jobId,
                        onBackClick = { navController.popBackStack() },
                        onJobCompleted = {
                            navController.navigate(TervynDestination.Jobs.route) {
                                popUpTo(TervynDestination.Jobs.route) { inclusive = true }
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(jobCompletedMessage)
                            }
                        }
                    )
                }

                composable(TervynDestination.Logout.route) {
                    LogoutScreen(
                        onBackClick = { navController.popBackStack() },
                        onConfirmLogout = {
                            onLogoutConfirmed {
                                scope.launch {
                                    snackbarHostState.showSnackbar(loggedOutMessage)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
