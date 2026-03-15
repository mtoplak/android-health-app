package com.example.health_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.health_app.ui.screens.PodrobnostiMeritveScreen
import com.example.health_app.ui.screens.SeznamMeritevScreen
import com.example.health_app.ui.screens.VnosMeritveScreen
import com.example.health_app.viewmodel.MeritevViewModel

object Routes {
    const val VNOS = "vnos"
    const val SEZNAM = "seznam"
    const val PODROBNOSTI = "podrobnosti"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MeritevViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.VNOS
    ) {
        // View 1 – Input form (new or edit)
        composable(
            route = "${Routes.VNOS}?meritevId={meritevId}",
            arguments = listOf(
                navArgument("meritevId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            val meritevId = backStackEntry.arguments?.getInt("meritevId") ?: -1
            VnosMeritveScreen(
                viewModel = viewModel,
                meritevId = if (meritevId == -1) null else meritevId,
                onNavigateToDetail = { id ->
                    navController.navigate("${Routes.PODROBNOSTI}/$id")
                },
                onNavigateToList = {
                    navController.navigate(Routes.SEZNAM)
                }
            )
        }

        // View 3 – Measurement list
        composable(route = Routes.SEZNAM) {
            SeznamMeritevScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { id ->
                    navController.navigate("${Routes.PODROBNOSTI}/$id")
                },
                onNavigateToEdit = { id ->
                    navController.navigate("${Routes.VNOS}?meritevId=$id")
                }
            )
        }

        // View 2 – Measurement details
        composable(
            route = "${Routes.PODROBNOSTI}/{meritevId}",
            arguments = listOf(
                navArgument("meritevId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val meritevId = backStackEntry.arguments?.getInt("meritevId") ?: return@composable
            PodrobnostiMeritveScreen(
                viewModel = viewModel,
                meritevId = meritevId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { id ->
                    navController.navigate("${Routes.VNOS}?meritevId=$id")
                }
            )
        }
    }
}

