package com.example.photagrapheryern

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.photagrapheryern.ui.theme.PhotagrapherYernTheme

class MainActivity : ComponentActivity() {

    private lateinit var requestPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register permission launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                // Optional: Show a toast or fallback UI here
            }
        }

        // Launch permission request
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        setContent {
            PhotagrapherYernTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    CameraPreview(lifecycleOwner)
                }
            }
        }
    }
}
