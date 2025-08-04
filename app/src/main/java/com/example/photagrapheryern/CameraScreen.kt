package com.example.photagrapheryern

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
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
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.photagrapheryern.ml.ImageAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.absoluteValue

// Data class for analysis result
data class AnalysisResult(
    val suggestion: String,
    val enhancedImage: Bitmap
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val labelText = remember { mutableStateOf("Point your camera at something...") }
    var mostRecentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var tapAnimationOffset by remember { mutableStateOf(Offset.Zero) }
    var tapAnimationVisible by remember { mutableStateOf(false) }
    val tapAnimationAlpha = remember { androidx.compose.animation.core.Animatable(0f) }

    // Declare zoomRatio here
    var zoomRatio by remember { mutableStateOf(1f) }

    // State for brightness (exposure compensation)
    var brightnessLevel by remember { mutableStateOf(0f) } // Default to 0 (no compensation)
    var isBrightnessSliderVisible by remember { mutableStateOf(false) } // New state to control slider visibility

    // store the bound Camera
    val cameraRef = remember { mutableStateOf<Camera?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(flashMode)
            .build()
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Apply brightness to camera control whenever brightnessLevel changes
    LaunchedEffect(brightnessLevel) {
        cameraRef.value?.cameraControl?.setExposureCompensationIndex(brightnessLevel.toInt())
    }

    rememberCameraUseCases(
        context = context,
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        imageCapture = imageCapture,
        lensFacing = lensFacing,
        flashMode = flashMode,
        onLabelDetected = { labelText.value = it },
        onCameraReady = { camera ->
            cameraRef.value = camera
            // Get the initial exposure range from the camera when it's ready
            val exposureState = camera.cameraInfo.exposureState
            brightnessLevel = exposureState.exposureCompensationIndex.toFloat() // Initialize with current
        }
    )

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = Uri.encode(uri.toString())
            navController.navigate("photo_display_screen/$encoded?fromCamera=false")
        } else {
            Toast.makeText(context, "Image selection cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            mostRecentPhotoUri = getMostRecentPhotoUri(context)
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Background
            Image(
                painter = painterResource(id = R.drawable.bg3),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))

                // Preview with pinch-to-zoom and tap-to-focus
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(9f/16f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                cameraRef.value?.let { cam ->
                                    val zoomState = cam.cameraInfo.zoomState.value
                                    val newZoom = (zoomState.zoomRatio * zoom)
                                        .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                                    cam.cameraControl.setZoomRatio(newZoom)
                                    zoomRatio = newZoom // Update zoomRatio when pinch-to-zoom occurs
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { pos ->
                                val point = previewView.meteringPointFactory.createPoint(pos.x, pos.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                cameraRef.value?.cameraControl?.startFocusAndMetering(action)

                                // Trigger animation
                                tapAnimationOffset = pos
                                tapAnimationVisible = true
                                coroutineScope.launch {
                                    tapAnimationAlpha.animateTo(
                                        targetValue = 0.8f,
                                        animationSpec = tween(durationMillis = 200)
                                    )
                                    delay(300)
                                    tapAnimationAlpha.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 200)
                                    ) {
                                        if (value == 0f) {
                                            tapAnimationVisible = false
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                            .pointerInput(Unit) { // Re-added for tap-to-focus only, will be covered by outer Box's pointerInput
                                detectTapGestures { pos ->
                                    val camera = cameraRef.value ?: return@detectTapGestures
                                    val point = previewView.meteringPointFactory.createPoint(pos.x, pos.y)
                                    val action = FocusMeteringAction.Builder(point).build()

                                    if (camera.cameraInfo.isFocusMeteringSupported(action)) {
                                        camera.cameraControl.startFocusAndMetering(action)
                                        Log.d("TapToFocus", "Tap-to-focus action started.")

                                        // Trigger animation
                                        tapAnimationOffset = pos
                                        tapAnimationVisible = true
                                        coroutineScope.launch {
                                            tapAnimationAlpha.animateTo(
                                                targetValue = 0.8f,
                                                animationSpec = tween(durationMillis = 200)
                                            )
                                            delay(300)
                                            tapAnimationAlpha.animateTo(
                                                targetValue = 0f,
                                                animationSpec = tween(durationMillis = 200)
                                            ) {
                                                if (value == 0f) {
                                                    tapAnimationVisible = false
                                                }
                                            }
                                        }
                                    } else {
                                        Log.d("TapToFocus", "Tap-to-focus is not supported.")
                                    }
                                }
                            }
                    )
                    if (tapAnimationVisible) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = tapAnimationAlpha.value),
                                radius = 60f,
                                center = tapAnimationOffset
                            )
                        }
                    }
                    //Call GridOverlay func
                    GridOverlay()

                    // Flash button (remains the same)
                    IconButton(
                        onClick = {
                            flashMode = if (flashMode == ImageCapture.FLASH_MODE_ON)
                                ImageCapture.FLASH_MODE_OFF
                            else
                                ImageCapture.FLASH_MODE_ON
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
                            tint = Color.White
                        )
                    }

                    // --- Brightness Control ---

                    // 1. Collapsed Brightness Icon (always present and clickable)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart) // Aligns this box itself within the parent preview Box
                            .padding(12.dp) // Offset from the corner of the preview Box
                            .clickable { isBrightnessSliderVisible = !isBrightnessSliderVisible } // Toggle visibility on click
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape) // Background for the icon itself
                            .size(52.dp), // Fixed size for the clickable area, same as flash
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessMedium,
                            contentDescription = "Brightness",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp) // Icon size matched to flash
                        )
                    }

                    // 2. Expanded Brightness Slider (conditionally rendered and positioned)
                    if (isBrightnessSliderVisible) {
                        Column(
                            // Position this Column relative to the TopStart of the camera preview Box.
                            // The offset moves it rightward from the initial icon.
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 12.dp + 56.dp + 8.dp, y = 12.dp) // (initial padding) + (icon size) + (desired gap)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessMedium,
                                contentDescription = "Brightness",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp) // Icon size when expanded
                            )
                            Spacer(Modifier.height(12.dp))
                            Slider(
                                value = brightnessLevel,
                                onValueChange = { newValue ->
                                    cameraRef.value?.let { cam ->
                                        val exposureState = cam.cameraInfo.exposureState
                                        val minExposure = exposureState.exposureCompensationRange.lower.toFloat()
                                        val maxExposure = exposureState.exposureCompensationRange.upper.toFloat()
                                        val coercedValue = newValue.coerceIn(minExposure, maxExposure)
                                        brightnessLevel = coercedValue
                                    }
                                },
                                valueRange = cameraRef.value?.cameraInfo?.exposureState?.exposureCompensationRange?.let {
                                    it.lower.toFloat()..(it.upper.toFloat())
                                } ?: (-12f..12f),
                                steps = cameraRef.value?.cameraInfo?.exposureState?.exposureCompensationStep?.let { step ->
                                    ((cameraRef.value!!.cameraInfo.exposureState.exposureCompensationRange.upper - cameraRef.value!!.cameraInfo.exposureState.exposureCompensationRange.lower) / step.toFloat()).toInt() -1
                                } ?: 0,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFFCD04C),
                                    activeTrackColor = Color(0xFFFCD04C),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.width(205.dp)
                            )
                        }
                    }

                    // Zoom controls (moved here to overlap)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 32.dp, vertical = 12.dp)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(50.dp)),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val fixedZoomLevels = listOf(0.5f, 1f, 2f)
                        fixedZoomLevels.forEach { level ->
                            // Check if the current zoom level is close enough to the button level
                            val isCurrentLevel = (zoomRatio - level).absoluteValue < 0.1f
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrentLevel) 44.dp else 34.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .clickable {
                                        cameraRef.value?.cameraControl?.setZoomRatio(level)
                                        zoomRatio = level
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isCurrentLevel) "${level}x" else if (level == 0.5f) ".5" else level.toString(),
                                    color = Color(0xFFFCD04C),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // AI Analysis Result
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
                        Image(
                            bitmap = result.enhancedImage.asImageBitmap(),
                            contentDescription = "AI-enhanced image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery picker
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable {
                                pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        mostRecentPhotoUri?.let {
                            Image(
                                painter = rememberAsyncImagePainter(it),
                                contentDescription = "Most Recent Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } ?: Icon(
                            Icons.Default.Image,
                            contentDescription = "Gallery",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Capture button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            .clickable {
                                coroutineScope.launch {
                                    val uri = takePhotoWithMediaStore(context, imageCapture)
                                    uri?.let {
                                        mostRecentPhotoUri = it
                                        val encoded = Uri.encode(it.toString())
                                        navController.navigate("photo_display_screen/$encoded?fromCamera=true")
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    // Switch camera
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT
                            else
                                CameraSelector.LENS_FACING_BACK
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape) // Added background here
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cached,
                            contentDescription = "Switch Camera",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun rememberCameraUseCases(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    lensFacing: Int,
    flashMode: Int,
    onLabelDetected: (String) -> Unit,
    onCameraReady: (Camera) -> Unit
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(lensFacing, flashMode) {
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analyzerUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(ContextCompat.getMainExecutor(context),
                    ImageAnalyzer { label -> onLabelDetected(label) })
            }

        imageCapture.flashMode = flashMode

        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                preview,
                analyzerUseCase,
                imageCapture
            )
            onCameraReady(camera)
        } catch (e: Exception) {
            Log.e("CameraScreen", "Failed to bind camera", e)
        }
    }
}

/**
 * Handles taking a photo and saving it to MediaStore.
 * This function is now a suspend function that returns the Uri of the saved image.
 *
 * @param context The current Android context.
 * @param imageCapture The ImageCapture use case.
 * @return The Uri of the saved image if successful, null otherwise.
 */
@SuppressLint("SimpleDateFormat")
suspend fun takePhotoWithMediaStore(
    context: Context,
    imageCapture: ImageCapture
): Uri? = suspendCancellableCoroutine { continuation ->
    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/PhotographerYern")
        }
    }

    val options = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(options, ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Toast.makeText(context, "Photo saved to gallery", Toast.LENGTH_SHORT).show()
                Log.d("CameraScreen", "Saved: ${output.savedUri}")
                continuation.resume(output.savedUri)
            }

            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                Log.e("CameraScreen", "Error: ${exc.message}", exc)
                continuation.resumeWithException(exc)
            }
        })
}

// Async load the most recent photo
private suspend fun getMostRecentPhotoUri(context: Context): Uri? = withContext(Dispatchers.IO) {
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    else
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN)

    val contentUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // On Android 11+ we can use queryArgs to limit directly
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
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                Uri.withAppendedPath(collection, id.toString())
            } else null
        }
    } else {
        // On older Android, sort DESC and pick the first row—no LIMIT token
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                Uri.withAppendedPath(collection, id.toString())
            } else null
        }
    }

    contentUri
}

@Composable
//GridOverlay
fun GridOverlay(
    color: Color = Color.White.copy(alpha = 0.3f),
    strokeWidth: Dp = 1.dp
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val thirdWidth = size.width / 3
        val thirdHeight = size.height / 3
        val strokePx = strokeWidth.toPx()

        // Vertical lines
        drawLine(color, Offset(thirdWidth, 0f), Offset(thirdWidth, size.height), strokePx)
        drawLine(color, Offset(2 * thirdWidth, 0f), Offset(2 * thirdWidth, size.height), strokePx)

        // Horizontal lines
        drawLine(color, Offset(0f, thirdHeight), Offset(size.width, thirdHeight), strokePx)
        drawLine(color, Offset(0f, 2 * thirdHeight), Offset(size.width, 2 * thirdHeight), strokePx)
    }
}