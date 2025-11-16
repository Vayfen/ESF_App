package com.esf.calendar.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.esf.calendar.ui.navigation.NavGraph
import com.esf.calendar.ui.theme.ESFCalendarTheme

/**
 * Activité principale de l'application ESF Calendar
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.d("MainActivity", "=== onCreate appelé ===")

        // Installer le splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)

        android.util.Log.d("MainActivity", "Initialisation UI...")

        setContent {
            android.util.Log.d("MainActivity", "setContent composable")
            ESFCalendarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    android.util.Log.d("MainActivity", "NavController créé, lancement NavGraph")
                    NavGraph(navController = navController)
                }
            }
        }

        android.util.Log.d("MainActivity", "=== onCreate terminé ===")
    }
}
