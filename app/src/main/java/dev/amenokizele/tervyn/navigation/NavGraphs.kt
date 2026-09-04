package dev.amenokizele.tervyn.navigation

sealed class NavGraph(val route: String) {
    data object Root : NavGraph("root_graph")
    data object Auth : NavGraph("auth_graph")
    data object App : NavGraph("app_graph")
}
