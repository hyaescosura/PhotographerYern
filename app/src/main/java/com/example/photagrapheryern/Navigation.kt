package com.example.photagrapheryern

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "splash_screen", // Start with the splash screen
        modifier = modifier
    ) {
        composable("splash_screen") {
            SplashScreen(navController = navController)
        }
        composable("camera_screen") {
            CameraScreen(navController = navController)
        }
        // Define the route for the photo display screen
        composable(
            route = "photo_display_screen/{photoUri}?fromCamera={fromCamera}",
            arguments = listOf(
                navArgument("photoUri") { type = NavType.StringType },
                navArgument("fromCamera") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val photoUriString = backStackEntry.arguments?.getString("photoUri")
            val photoUri = photoUriString?.let { Uri.decode(it).toUri() }
            val fromCamera = backStackEntry.arguments?.getBoolean("fromCamera") ?: false
            // ADD THIS LINE:
            Log.d("PhotoAppDebug", "NavController: Extracted fromCamera: $fromCamera for URI: $photoUri")

            PhotoDisplayScreen(navController = navController, photoUri = photoUri, fromCamera = fromCamera)
        }
    }
}