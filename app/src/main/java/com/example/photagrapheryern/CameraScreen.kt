package com.example.photagrapheryern

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // State for UI elements and camera settings
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val labelText = remember { mutableStateOf("Point your camera at something...") }
    var mostRecentPhotoUri by remember { mutableStateOf<Uri?>(null) } // State for the recent photo thumbnail
    // State to hold the result of the Firebase AI analysis
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }

    var cameraRef = remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // CameraX ImageCapture use case
    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(flashMode)
            .build()
    }

    // AndroidView for CameraX Preview
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Call the camera logic from CameraFunctions.kt (it's a @Composable helper)
    rememberCameraUseCases( // Assuming this is a composable function that sets up CameraX lifecycle and analysis
        context = context,
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        imageCapture = imageCapture,
        lensFacing = lensFacing,
        flashMode = flashMode,
        onLabelDetected = { label -> labelText.value = label },
        onCameraReady = { camera -> cameraRef.value = camera }
    )

    // Launcher for picking a single image from the gallery
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encodedUri = Uri.encode(uri.toString())
            // --- CHANGE HERE: fromCamera is FALSE when picking from gallery ---
            navController.navigate("photo_display_screen/$encodedUri?fromCamera=${false}")
        } else {
            // User cancelled the picker
            Toast.makeText(context, "Image selection cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    // Effect to load the most recent photo when the screen becomes active
    LaunchedEffect(Unit) { // Runs once when the Composable enters the composition
        coroutineScope.launch {
            mostRecentPhotoUri = getMostRecentPhotoUri(context)
        }
    }

    Scaffold(
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 1. Background Image
                Image(
                    painter = painterResource(id = R.drawable.bg3), // <--- REPLACE 'your_background_image'
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 2. Optional: Semi-transparent overlay to make UI elements more readable
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)) // Adjust alpha for desired darkness
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Spacer at the top (optional, can adjust for overall vertical position)
                    Spacer(modifier = Modifier.height(24.dp))

                    // Camera Preview Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f) // Keep 95% width, or adjust if desired
                            .aspectRatio(9f / 16f) // <--- THIS IS NOW SET TO MAKE IT LONGER
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Flash Toggle Button - TOP RIGHT (aligned to this Camera Preview Box)
                        IconButton(
                            onClick = {
                                flashMode = if (flashMode == ImageCapture.FLASH_MODE_ON)
                                    ImageCapture.FLASH_MODE_OFF else ImageCapture.FLASH_MODE_ON
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (flashMode == ImageCapture.FLASH_MODE_ON)
                                    Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Flash",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // AI Label Card (uncommented and placed here)
                        // This will display the label from your ML Kit ImageAnalyzer
//                        Box(
//                            modifier = Modifier
//                                .align(Alignment.BottomCenter) // Aligns this whole container to the bottom-center of the camera preview
//                                .fillMaxWidth(0.9f)
//                                .padding(bottom = 16.dp)
//                        ) {
//                            // You had GlassmorphismCardWithText, assuming it's a custom composable.
//                            // If not, you can use a regular Box with background and text.
//                            // I'm using the simpler Box as per your commented out code.
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .heightIn(min = 70.dp, max = 180.dp)
//                                    .clip(MaterialTheme.shapes.medium)
//                                    .background(Color.Black.copy(alpha = 0.5f))
//                                    .border(1.dp, Color.White.copy(alpha = 0.3f), MaterialTheme.shapes.medium),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Text(
//                                    text = labelText.value,
//                                    color = Color.White,
//                                    style = MaterialTheme.typography.headlineSmall,
//                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//                                )
//                            }
//                            // The sparkle image you had is commented out, leaving it that way for now.
//                            /*
//                            Image(
//                                painter = painterResource(id = R.drawable.sparkle),
//                                contentDescription = "Card Overlap Image",
//                                modifier = Modifier
//                                    .align(Alignment.TopEnd)
//                                    .size(64.dp)
//                                    .offset(
//                                        x = 24.dp,
//                                        y = -24.dp
//                                    )
//                            )
//                            */
//                        }
                    } // End of Camera Preview Box

                    // Display AI Analysis Results below the camera preview
                    analysisResult?.let { result ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                result.suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

//                            Image(
//                                bitmap = analysisResult.enhancedImage!!.asImageBitmap(),
//                                contentDescription = "AI-enhanced image",
                            Image(
                                bitmap = result.enhancedImage.asImageBitmap(),
                                contentDescription = "AI-enhanced image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp) // Or adjust height as needed
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit // Use Fit to show the whole image
                            )
                        }
                    }


                    // Spacer between camera preview/analysis and controls row
                    Spacer(modifier = Modifier.height(24.dp))

                    // Controls below the camera preview (Gallery, Capture, Switch Camera)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery Button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    } else {
                                        Toast.makeText(context, "Gallery picker requires Android 10+ or explicit ACTION_PICK handling.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (mostRecentPhotoUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = mostRecentPhotoUri),
                                    contentDescription = "Most Recent Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Gallery",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Take Photo Button - Circular Outline with Inner Circle
                        // In your CameraScreen.kt file

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                .clickable {
                                    coroutineScope.launch {
                                        // Capture the photo to gallery
                                        val photoUri = takePhotoWithMediaStore(context, imageCapture)
                                        photoUri?.let { uri ->
                                            mostRecentPhotoUri = uri // Update thumbnail for CameraScreen thumbnail if you have one

                                            // Navigate to PhotoDisplayScreen
                                            val encodedUri = Uri.encode(uri.toString())
                                            val navigateRoute = "photo_display_screen/$encodedUri?fromCamera=${true}"
                                            Log.d("PhotoAppDebug", "CameraScreen: Navigating to: $navigateRoute")
                                            navController.navigate(navigateRoute) // Make sure this is uncommented
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp) // Smaller size for the inner circle
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }

                        // Camera Switch Button
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                    CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                            },
                            modifier = Modifier
                                .size(56.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.camera_switch),
                                contentDescription = "Switch Camera",
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

// Kept this function here as it's directly used by CameraScreen's UI for recent photo thumbnail
private suspend fun getMostRecentPhotoUri(context: Context): Uri? = withContext(Dispatchers.IO) {
    val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

    val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN)

    val contentUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val queryArgs = Bundle().apply {
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Images.Media.DATE_TAKEN)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
        }

        context.contentResolver.query(
            collection,
            projection,
            queryArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumn)
                Uri.withAppendedPath(collection, id.toString())
            } else {
                null
            }
        }
    } else {
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC LIMIT 1"

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumn)
                Uri.withAppendedPath(collection, id.toString())
            } else {
                null
            }
        }
    }
    contentUri
}