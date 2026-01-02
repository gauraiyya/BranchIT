package com.binarybhaskar.branchitandroid.navigation

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Destinations {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SCREEN_CONTENT = "home"
    const val SETTINGS = "settings"
    const val CHAT = "chat"
}

@Composable
fun BranchITNavHost(isLoggedIn: Boolean, prefs: SharedPreferences) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destinations.SPLASH) {
        composable(Destinations.SPLASH) {
            com.binarybhaskar.branchitandroid.screen.SplashScreen(onFinished = {
                if (isLoggedIn) {
                    navController.navigate(Destinations.SCREEN_CONTENT) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(Destinations.LOGIN) {
                        popUpTo(Destinations.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            })
        }
        composable(Destinations.LOGIN) {
            com.binarybhaskar.branchitandroid.screen.LoginScreen(prefs = prefs, onLoginSuccess = {
                navController.navigate(Destinations.SCREEN_CONTENT) {
                    popUpTo(Destinations.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        composable(Destinations.SCREEN_CONTENT) {
            com.binarybhaskar.branchitandroid.screen.ScreenContent(navController)
        }
        composable(Destinations.SETTINGS) {
            com.binarybhaskar.branchitandroid.screen.SettingsScreen(navController, prefs)
        }
        composable(
            route = "${Destinations.CHAT}/{type}/{id}",
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType })) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type").orEmpty()
            val id = backStackEntry.arguments?.getString("id").orEmpty()
//            val target =
//                if (type == "group") com.binarybhaskar.branchitandroid.data.ChatTarget.GroupRoom(id)
//                else com.binarybhaskar.branchitandroid.data.ChatTarget.PostThread(id)
//            com.binarybhaskar.branchitandroid.screen.IndividualChatScreen(navController, target)
        }
    }
}
