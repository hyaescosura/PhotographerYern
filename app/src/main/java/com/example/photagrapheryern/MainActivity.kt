package com.example.photagrapheryern

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.photagrapheryern.ui.theme.PhotagrapherYernTheme

class MainActivity : ComponentActivity() {

    // Launcher for multiple permissions
    private lateinit var requestPermissionsLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Define the permissions we need
        val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // For older Android versions
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { // For Android 12 and below
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else { // For Android 13 and above
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val storageGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
            } else {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
            }

            if (cameraGranted && storageGranted) {
                setContentForApp()
            } else {
                val missingPermissions = permissions.filter { !it.value }.keys.joinToString(", ")
                Toast.makeText(this, "Required permissions denied: $missingPermissions. Please enable them in settings to use the app.", Toast.LENGTH_LONG).show()
                setContent {
                    PhotagrapherYernTheme {
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Permissions denied. Please enable them in settings to use the app.",
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Check if all necessary permissions are already granted
        val allPermissionsGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            setContentForApp()
        } else {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun setContentForApp() {
        setContent {
            PhotagrapherYernTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF030303)
                ) {
                    AppNavigation()
                }
            }
        }
    }
}