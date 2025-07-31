package com.example.photagrapheryern

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember {
        Animatable(0f)
    }
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500,
                easing = {
                    OvershootInterpolator(2f).getInterpolation(it)
                }
            )
        )
        delay(3000L)
        navController.navigate("camera_screen") {
            // This prevents going back to the splash screen from the camera screen
            popUpTo("splash_screen") { inclusive = true }
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Logo Image
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.TopCenter)
                .offset(y = 250.dp)
                .scale(scale.value)
        )
    }
}

//@Composable
//fun CameraScreen(navController: NavController) {
//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Text(text = "MAIN SCREEN", color = Color.White)
//    }
//}