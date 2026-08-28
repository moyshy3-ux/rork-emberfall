package com.rork.emberfall.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rork.emberfall.ui.game.GameScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "hollow"
    ) {
        composable("hollow") {
            GameScreen()
        }
    }
}
