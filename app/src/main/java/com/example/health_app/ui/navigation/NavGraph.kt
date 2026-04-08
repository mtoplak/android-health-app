package com.example.health_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.health_app.ui.screens.AuthScreen
import com.example.health_app.ui.screens.PodrobnostiMeritveScreen
import com.example.health_app.ui.screens.SeznamMeritevScreen
import com.example.health_app.ui.screens.StatisticsScreen
import com.example.health_app.ui.screens.VnosMeritveScreen
import com.example.health_app.viewmodel.AuthViewModel
import com.example.health_app.viewmodel.MeritevViewModel

object Routes {
    const val AUTH = "auth"
    const val VNOS = "vnos"
    const val SEZNAM = "seznam"
    const val PODROBNOSTI = "podrobnosti"
    const val STATISTIKA = "statistika"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MeritevViewModel,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val startDestination = if (authState.currentUser != null) Routes.VNOS else Routes.AUTH

    LaunchedEffect(authState.currentUser?.uid) {
        viewModel.setCurrentUser(authState.currentUser?.uid)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Routes.AUTH) {
            AuthScreen(
                authViewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Routes.VNOS) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

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
                onSync = { viewModel.syncFromFirestore() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                loggedInEmail = authState.currentUser?.email,
                onNavigateToDetail = { id ->
                    navController.navigate("${Routes.PODROBNOSTI}/$id")
                },
                onNavigateToEdit = { id ->
                    navController.navigate("${Routes.VNOS}?meritevId=$id")
                },
                onNavigateToStatistics = {
                    navController.navigate(Routes.STATISTIKA)
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

        // Statistics screen
        composable(route = Routes.STATISTIKA) {
            StatisticsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

