package com.esf.calendar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.esf.calendar.ui.screens.calendar.CalendarScreen
import com.esf.calendar.ui.screens.login.LoginScreen

/**
 * Routes de navigation
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
}

/**
 * Graphe de navigation principal
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Calendar.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            // TODO: Implémenter SettingsScreen
            androidx.compose.material3.Text("Paramètres - À implémenter")
        }
    }
}
