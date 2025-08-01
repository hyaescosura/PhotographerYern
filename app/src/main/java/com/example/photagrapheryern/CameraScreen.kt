package com.example.photagrapheryern

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
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

    // --- States for Camera UI and Logic ---
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val labelText = remember { mutableStateOf("Point your camera at something...") }
    var mostRecentPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // --- States for AI Analysis Popup within CameraScreen ---
    var showAiPopupOnCamera by remember { mutableStateOf(false) }
    var analysisResultOnCamera by remember { mutableStateOf<AnalysisResult?>(null) }
    var isLoadingAnalysisOnCamera by remember { mutableStateOf(false) }
    var analysisErrorMessageOnCamera by remember { mutableStateOf<String?>(null) }
    var showAiIconOnCamera by remember { mutableStateOf(false) } // Controls visibility of AI icon

    val Montserrat = FontFamily(Font(R.font.montserrat_semibold, FontWeight.SemiBold))

    // CameraX ImageCapture use case setup
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

    // CameraX lifecycle management (assuming rememberCameraUseCases is a helper composable)
    rememberCameraUseCases(
        context = context,
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        imageCapture = imageCapture,
        lensFacing = lensFacing,
        flashMode = flashMode,
        onLabelDetected = { label -> labelText.value = label }
    )

    // Launcher for picking a single image from the gallery
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encodedUri = Uri.encode(uri.toString())
            // Navigate to PhotoDisplayScreen; 'fromCamera' is false as it's from gallery
            navController.navigate("photo_display_screen/$encodedUri?fromCamera=${false}")
        } else {
            Toast.makeText(context, "Image selection cancelled.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Effects ---
    LaunchedEffect(Unit) {
        // Load the most recent photo when the screen first appears
        mostRecentPhotoUri = getMostRecentPhotoUri(context)
    }

    // --- UI Layout ---
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF030303)) // Dark background for the whole screen
        ) {
            // Background Image
            Image(
                painter = painterResource(id = R.drawable.bg3),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Semi-transparent overlay for better UI readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Camera Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Flash Toggle Button
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

                    // AI Analysis Icon on Camera Screen
                    // Visible only after a photo has been taken
                    if (showAiIconOnCamera) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(52.dp)
                                .offset(x = (-10).dp, y = (-10).dp)
                                .clickable {
                                    // Trigger AI analysis for the most recent photo
                                    mostRecentPhotoUri?.let { uri ->
                                        showAiPopupOnCamera = true
                                        isLoadingAnalysisOnCamera = true
                                        analysisErrorMessageOnCamera = null
                                        analysisResultOnCamera = null

                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                                                    ImageDecoder.decodeBitmap(source) { decoder, info, source ->
                                                        decoder.isMutableRequired = true
                                                    }
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                                                }

                                                if (bitmap != null) {
                                                    val promptText = "Provide tips on angle, lighting, pose based on the scene. Shorten your response for up to 5 lines. Also provide a sample picture with the improvements"
                                                    val result = FirebaseImageAnalyzer.analyzeImage(bitmap, promptText)
                                                    analysisResultOnCamera = AnalysisResult(
                                                        suggestion = result.first,
                                                        enhancedImage = result.second
                                                    )
                                                } else {
                                                    analysisErrorMessageOnCamera = "Error: Could not load image for analysis."
                                                    Log.e("CameraScreen", "Could not convert Uri to Bitmap for analysis: $uri")
                                                }
                                            } catch (e: Exception) {
                                                analysisErrorMessageOnCamera = "Analysis failed: ${e.localizedMessage ?: "Unknown error"}"
                                                Log.e("CameraScreen", "AI analysis failed from CameraScreen: ${e.message}", e)
                                            } finally {
                                                isLoadingAnalysisOnCamera = false
                                            }
                                        }
                                    } ?: run {
                                        Toast.makeText(context, "No photo taken yet for analysis.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ai),
                                    contentDescription = "AI Analysis Icon",
                                    modifier = Modifier.size(20.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                } // End of Camera Preview Box

                Spacer(modifier = Modifier.height(24.dp))

                // --- Control Row: Gallery, Capture, Switch Camera ---
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

                    // Take Photo Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                            .clickable {
                                coroutineScope.launch {
                                    val photoUri = takePhotoWithMediaStore(context, imageCapture)
                                    photoUri?.let { uri ->
                                        mostRecentPhotoUri = uri // Update thumbnail
                                        showAiIconOnCamera = true // Show AI icon after taking a photo

                                        val encodedUri = Uri.encode(uri.toString())
                                        val navigateRoute = "photo_display_screen/$encodedUri?fromCamera=${true}"
                                        navController.navigate(navigateRoute)
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

                    // Camera Switch Button
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        },
                        modifier = Modifier.size(56.dp)
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

        // --- AI Analysis Popup (AlertDialog) for CameraScreen ---
        if (showAiPopupOnCamera) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            val buttonBackgroundColor = if (isPressed) {
                Color.Blue.copy(alpha = 0.5f)
            } else {
                Color.Transparent
            }

            AlertDialog(
                onDismissRequest = { showAiPopupOnCamera = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
                content = { // <--- This content lambda starts here
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .heightIn(max = 350.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) { // <--- Card's content lambda starts here
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(28.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (isLoadingAnalysisOnCamera) {
                                    CircularProgressIndicator(color = Color(0xFFFCD04C))
                                    Text(
                                        text = "Analyzing your photo...",
                                        fontFamily = Montserrat,
                                        fontSize = 16.sp,
                                        color = Color.DarkGray
                                    )
                                } else if (analysisErrorMessageOnCamera != null) {
                                    Text(
                                        text = analysisErrorMessageOnCamera!!,
                                        fontFamily = Montserrat,
                                        fontSize = 16.sp,
                                        color = Color.Red,
                                        textAlign = TextAlign.Center
                                    )
                                } else if (analysisResultOnCamera != null) {
                                    Text(
                                        text = "AI Photography Tips:",
                                        fontFamily = Montserrat,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Color.Black,
                                        style = LocalTextStyle.current.copy(letterSpacing = 1.sp)
                                    )
                                    Text(
                                        text = analysisResultOnCamera!!.suggestion,
                                        fontFamily = Montserrat,
                                        fontSize = 16.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    analysisResultOnCamera!!.enhancedImage?.let { enhancedBitmap ->
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Suggested Improvement:",
                                            fontFamily = Montserrat,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 18.sp,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Image(
                                            bitmap = enhancedBitmap.asImageBitmap(),
                                            contentDescription = "Enhanced Photo Suggestion",
                                            modifier = Modifier
                                                .fillMaxWidth(0.8f)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.FillWidth
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "No analysis available. Take a photo or load one from gallery.",
                                        fontFamily = Montserrat,
                                        fontSize = 16.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showAiPopupOnCamera = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(36.dp)
                                    .offset(x = 18.dp, y = (-18).dp)
                                    .clip(CircleShape)
                                    .background(buttonBackgroundColor)
                                    .padding(8.dp),
                                interactionSource = interactionSource
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    } // <--- This closes the Card's content lambda
                } // <--- This closes the AlertDialog's content lambda
            )
        }
    }
}

// Helper Function: getMostRecentPhotoUri - MUST BE AT THE TOP-LEVEL OF THE FILE
// Not nested inside any other function or class.
private suspend fun getMostRecentPhotoUri(context: Context): Uri? = withContext(Dispatchers.IO) {
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

        context.contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
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

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
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