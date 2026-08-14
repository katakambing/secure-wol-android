package com.securewol.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.securewol.app.core.security.SessionManager
import com.securewol.app.data.model.AuthState
import com.securewol.app.data.repository.PcRepository
import com.securewol.app.data.repository.SecurityRepository
import com.securewol.app.ui.auth.AuthScreen
import com.securewol.app.ui.auth.AuthViewModel
import com.securewol.app.ui.dashboard.DashboardScreen
import com.securewol.app.ui.dashboard.DashboardViewModel
import com.securewol.app.ui.pcedit.PcEditScreen
import com.securewol.app.ui.pcedit.PcEditViewModel
import com.securewol.app.ui.settings.SecuritySettingsScreen
import com.securewol.app.ui.settings.SecuritySettingsViewModel
import com.securewol.app.ui.setup.SetupScreen
import com.securewol.app.ui.setup.SetupViewModel

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Auth : Screen("auth")
    object Dashboard : Screen("dashboard")
    object PcAdd : Screen("pc_add")
    object PcEdit : Screen("pc_edit/{pcId}") {
        fun createRoute(pcId: String) = "pc_edit/$pcId"
    }
    object SecuritySettings : Screen("security_settings")
}

@Composable
fun AppNavigation(
    securityRepository: SecurityRepository,
    pcRepository: PcRepository,
    navController: NavHostController = rememberNavController()
) {
    val isAuthenticated by SessionManager.isAuthenticated.collectAsState()

    // Determine initial destination
    val startDestination = when (securityRepository.getEffectiveAuthState()) {
        is AuthState.SetupRequired -> Screen.Setup.route
        is AuthState.Authenticated -> Screen.Dashboard.route
        else -> Screen.Auth.route
    }

    // Global session watcher: if session is invalidated (auto-lock or manual), kick to Auth
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated && securityRepository.isOwnerEnrolled()) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Setup.route) {
            val viewModel = SetupViewModel(securityRepository)
            SetupScreen(
                viewModel = viewModel,
                onSetupComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            val viewModel = AuthViewModel(securityRepository)
            AuthScreen(
                viewModel = viewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val viewModel = DashboardViewModel(pcRepository, securityRepository)
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAddPc = { navController.navigate(Screen.PcAdd.route) },
                onNavigateToEditPc = { pcId -> navController.navigate(Screen.PcEdit.createRoute(pcId)) },
                onNavigateToSettings = { navController.navigate(Screen.SecuritySettings.route) },
                onLockApp = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PcAdd.route) {
            val viewModel = PcEditViewModel(pcRepository, null)
            PcEditScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PcEdit.route,
            arguments = listOf(navArgument("pcId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pcId = backStackEntry.arguments?.getString("pcId")
            val viewModel = PcEditViewModel(pcRepository, pcId)
            PcEditScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SecuritySettings.route) {
            val viewModel = SecuritySettingsViewModel(securityRepository)
            SecuritySettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSessionExpired = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDeviceWiped = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
