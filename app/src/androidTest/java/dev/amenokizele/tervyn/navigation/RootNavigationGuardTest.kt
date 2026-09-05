package dev.amenokizele.tervyn.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.navArgument
import androidx.navigation.navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import dev.amenokizele.tervyn.app.LocalDataInitializationState
import dev.amenokizele.tervyn.app.TervynAppState
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.data.fixtures.TervynDemoFixtures
import dev.amenokizele.tervyn.domain.model.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RootNavigationGuardTest {
    @Test
    fun restoredAppDestinationReturnsToBootstrapWhileLocalDataIsInitializing() = runOnMainThread {
        val navController = testNavController()
        navController.navigate(NavGraph.App.route)
        navController.navigate(TervynDestination.Execution.createRoute("job-003"))

        reconcileRootNavigation(
            navController = navController,
            appState = TervynAppState(
                localDataState = LocalDataInitializationState.Initializing
            )
        )

        assertEquals(TervynDestination.Bootstrap.route, navController.currentRoute())
    }

    @Test
    fun restoredAppDestinationReturnsToBootstrapOnLocalDataErrorAndClearsAppBackStack() = runOnMainThread {
        val navController = testNavController()
        navController.navigate(NavGraph.App.route)
        navController.navigate(TervynDestination.Execution.createRoute("job-003"))

        reconcileRootNavigation(
            navController = navController,
            appState = TervynAppState(
                localDataState = LocalDataInitializationState.Error(AppError.Storage("seed_failed"))
            )
        )

        assertEquals(TervynDestination.Bootstrap.route, navController.currentRoute())
        navController.popBackStack()
        assertFalse(navController.currentRoute() in appOrAuthRoutes)
    }

    @Test
    fun restoredAuthDestinationReturnsToBootstrapOnLocalDataError() = runOnMainThread {
        val navController = testNavController()
        navController.navigate(NavGraph.Auth.route)

        reconcileRootNavigation(
            navController = navController,
            appState = TervynAppState(
                localDataState = LocalDataInitializationState.Error(AppError.Storage("seed_failed"))
            )
        )

        assertEquals(TervynDestination.Bootstrap.route, navController.currentRoute())
    }

    @Test
    fun bootstrapDoesNotDuplicateWhenLocalDataMovesFromInitializingToError() = runOnMainThread {
        val navController = testNavController()

        reconcileRootNavigation(
            navController = navController,
            appState = TervynAppState(
                localDataState = LocalDataInitializationState.Initializing
            )
        )
        reconcileRootNavigation(
            navController = navController,
            appState = TervynAppState(
                localDataState = LocalDataInitializationState.Error(AppError.Storage("seed_failed"))
            )
        )

        assertEquals(TervynDestination.Bootstrap.route, navController.currentRoute())
        navController.popBackStack()
        assertEquals(null, navController.currentRoute())
    }

    @Test
    fun readyUnauthenticatedNavigatesToLogin() = runOnMainThread {
        val navController = testNavController()

        reconcileRootNavigation(
            navController = navController,
            appState = TervynAppState(
                authState = AuthState.Unauthenticated,
                localDataState = LocalDataInitializationState.Ready
            )
        )

        assertEquals(TervynDestination.Login.route, navController.currentRoute())
    }

    @Test
    fun readyAuthenticatedNavigatesToApp() = runOnMainThread {
        val navController = testNavController()

        reconcileRootNavigation(
            navController = navController,
            appState = TervynAppState(
                authState = AuthState.Authenticated(TervynDemoFixtures.currentUser),
                localDataState = LocalDataInitializationState.Ready
            )
        )

        assertEquals(TervynDestination.Jobs.route, navController.currentRoute())
    }

    private fun runOnMainThread(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    private fun testNavController(): TestNavHostController {
        return TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
            graph = createGraph(
                startDestination = TervynDestination.Bootstrap.route,
                route = NavGraph.Root.route
            ) {
                addRootDestinations()
            }
        }
    }

    private fun NavGraphBuilder.addRootDestinations() {
        composable(TervynDestination.Bootstrap.route) {}
        navigation(
            startDestination = TervynDestination.Login.route,
            route = NavGraph.Auth.route
        ) {
            composable(TervynDestination.Login.route) {}
        }
        navigation(
            startDestination = TervynDestination.Jobs.route,
            route = NavGraph.App.route
        ) {
            composable(TervynDestination.Jobs.route) {}
            composable(TervynDestination.Sync.route) {}
            composable(TervynDestination.Settings.route) {}
            composable(
                route = TervynDestination.Execution.route,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) {}
        }
    }

    private fun NavController.currentRoute(): String? = currentDestination?.route

    private val appOrAuthRoutes = setOf(
        NavGraph.Auth.route,
        NavGraph.App.route,
        TervynDestination.Login.route,
        TervynDestination.Jobs.route,
        TervynDestination.Sync.route,
        TervynDestination.Settings.route,
        TervynDestination.Execution.route
    )
}
