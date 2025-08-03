package com.example.photagrapheryern

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.material.icons.filled.Cached
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput


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

    // ScaleGestureDetector for pinch-to-zoom
    val scaleGestureDetector = remember {
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                cameraRef.value?.let { cam ->
                    val zoomState = cam.cameraInfo.zoomState.value
                    val current = zoomState.zoomRatio
                    val delta = detector.scaleFactor
                    val newZoom = (current * delta).coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                    cam.cameraControl.setZoomRatio(newZoom)
                }
                return true
            }
        })
    }

    rememberCameraUseCases(
        context = context,
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        imageCapture = imageCapture,
        lensFacing = lensFacing,
        flashMode = flashMode,
        onLabelDetected = { labelText.value = it },
        onCameraReady = { cameraRef.value = it }
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
                                    val state = cam.cameraInfo.zoomState.value
                                    val newZoom = (state.zoomRatio * zoom)
                                        .coerceIn(state.minZoomRatio, state.maxZoomRatio)
                                    cam.cameraControl.setZoomRatio(newZoom)
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { pos ->
                                val point = previewView.meteringPointFactory.createPoint(pos.x, pos.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                cameraRef.value?.cameraControl?.startFocusAndMetering(action)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                    //Grid overlay
                    androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                        val w = size.width
                        val h = size.height
                        val thirdW = w / 3f
                        val thirdH = h / 3f
                        val strokeWidth = 1.dp.toPx()
                        val lineColor = Color.White.copy(alpha = 0.6f)

                        // Vertical lines
                        drawLine(lineColor, Offset(thirdW, 0f), Offset(thirdW, h), strokeWidth = strokeWidth)
                        drawLine(lineColor, Offset(2 * thirdW, 0f), Offset(2 * thirdW, h), strokeWidth = strokeWidth)

                        // Horizontal lines
                        drawLine(lineColor, Offset(0f, thirdH), Offset(w, thirdH), strokeWidth = strokeWidth)
                        drawLine(lineColor, Offset(0f, 2 * thirdH), Offset(w, 2 * thirdH), strokeWidth = strokeWidth)
                    }
                    // Flash button
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
                }

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

                Spacer(Modifier.height(24.dp))

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
                        modifier = Modifier.size(56.dp)
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

