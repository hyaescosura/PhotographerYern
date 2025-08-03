package com.example.photagrapheryern

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDisplayScreen(
    navController: NavController,
    photoUri: Uri?,
    fromCamera: Boolean = false
) {
    val context = LocalContext.current

    val Montserrat = FontFamily(
        Font(R.font.montserrat_semibold, FontWeight.SemiBold)
    )

    var showPopup by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var isLoadingAnalysis by remember { mutableStateOf(false) }
    var analysisErrorMessage by remember { mutableStateOf<String?>(null) }

    Log.d("PhotoAppDebug", "PhotoDisplayScreen: Received fromCamera: $fromCamera (at start of composable)")
    Log.d("PhotoAppDebug", "PhotoDisplayScreen: Received photoUri: $photoUri")

    LaunchedEffect(key1 = fromCamera, key2 = photoUri) {
        Log.d("PhotoAppDebug", "PhotoDisplayScreen: LaunchedEffect triggered with fromCamera: $fromCamera, photoUri: $photoUri")
        if (fromCamera && photoUri != null) {
            showPopup = true
            isLoadingAnalysis = true
            analysisErrorMessage = null
            analysisResult = null
            Log.d("PhotoAppDebug", "PhotoDisplayScreen: LaunchedEffect: Setting showPopup to true, starting analysis")

            try {
                val bitmap: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, photoUri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, source ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, photoUri)
                }

                if (bitmap != null) {
                    val promptText = "You're a photography expert. Give 2-3 simple tips to improve this photo (without props) be enthusiastic but keep it simple, \n " +
                            " and 1 thing I did well. Keep the suggestions short and practical. Provide a visual inspiration based on your suggestions \n" +
                            " from pinterest or instagram. Don't make changes on the face captured. Avoid major distortions in the suggested improvements. Remove bold from your responses"
                    val result = FirebaseImageAnalyzer.analyzeImage(bitmap, promptText)
                    analysisResult = AnalysisResult(
                        suggestion = result.first,
                        enhancedImage = result.second
                    )
                } else {
                    analysisErrorMessage = "Error: Could not load image for analysis."
                    Log.e("PhotoDisplayScreen", "Could not convert Uri to Bitmap for analysis: $photoUri")
                }
            } catch (e: Exception) {
                analysisErrorMessage = "Analysis failed: ${e.localizedMessage ?: "Unknown error"}"
                Log.e("PhotoDisplayScreen", "AI analysis failed: ${e.message}", e)
            } finally {
                isLoadingAnalysis = false
            }
        }
    }

    Scaffold(
        // The topBar is still removed
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF030303))
        ) {
            Image(
                painter = painterResource(id = R.drawable.bg2),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.6f
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                Image(
                    painter = painterResource(id = R.drawable.logo2),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(150.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(0.dp))

                if (photoUri != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = photoUri),
                                contentDescription = "Captured Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.FillWidth
                            )

                            // SPARKLE IMAGE - Top Right of the PHOTO CARD
                            Image(
                                painter = painterResource(id = R.drawable.sparkle),
                                contentDescription = "Sparkle decoration",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(72.dp)
                                    .offset(x = 20.dp, y = (-20).dp),
                                contentScale = ContentScale.Fit
                            )

                            // SPARKLE IMAGE - Bottom Left of the PHOTO CARD
                            Image(
                                painter = painterResource(id = R.drawable.sparkle),
                                contentDescription = "Sparkle decoration",
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .size(72.dp)
                                    .offset(x = (-20).dp, y = 20.dp),
                                contentScale = ContentScale.Fit
                            )

                            // --- AI ICON WITH CIRCLE (NOW CLICKABLE) ---
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(52.dp)
                                    .offset(x = (-10).dp, y = (-10).dp)
                                    .clickable { // <--- ADDED CLICKABLE MODIFIER HERE
                                        showPopup = true
                                        // If you want to re-run analysis every time the AI icon is clicked, uncomment below:
                                        // isLoadingAnalysis = true
                                        // analysisErrorMessage = null
                                        // analysisResult = null
                                        // (You might need to re-call FirebaseImageAnalyzer based on your logic)
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
                            // --- END AI ICON WITH CIRCLE ---
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { navController.popBackStack() },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Recapture",
                                        color = Color.White,
                                        fontFamily = Montserrat,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFCD04C),
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = "Recapture",
                                                tint = Color.Black,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(52.dp))

                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(width = 48.dp, height = 48.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        photoUri?.let { uri ->
                                            val shareIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                type = "image/*"
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share photo via"))
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No photo to display.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
            }

            if (showPopup) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                // Changed the pressed color to Color.DarkGray.copy(alpha = 0.5f)
                val buttonBackgroundColor = if (isPressed) {
                    Color.DarkGray.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                }

                val scrollState = rememberScrollState()

                AlertDialog(
                    onDismissRequest = { showPopup = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    content = {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .heightIn(max = 350.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(28.dp)
                            ) {
                                // Pass the states needed for FadingScrollableContent to decide its internal display
                                FadingScrollableContent(
                                    modifier = Modifier.fillMaxWidth(),
                                    scrollState = scrollState,
                                    isLoadingAnalysis = isLoadingAnalysis,
                                    analysisErrorMessage = analysisErrorMessage, // Pass error message
                                    analysisResult = analysisResult, // Pass analysis result
                                    photoUri = photoUri // Pass photoUri to determine "Tap camera" message
                                )
                                // Note: The 'content' lambda is now removed here, as FadingScrollableContent
                                // will manage all the inner content based on the states passed to it.

                                // Close IconButton remains in the Box
                                IconButton(
                                    onClick = {
                                        Log.d("PhotoAppDebug", "Close button clicked! Attempting to close popup.")
                                        showPopup = false
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(36.dp)
                                        .offset(x = 18.dp, y = (-18).dp)
                                        .clip(CircleShape)
                                        .background(buttonBackgroundColor) // This will now turn dark gray when pressed
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
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FadingScrollableContent(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    isLoadingAnalysis: Boolean,
    analysisErrorMessage: String?,
    analysisResult: AnalysisResult?,
    photoUri: Uri? // Added to determine if "Tap camera" message should show
) {
    val Montserrat = FontFamily( // Re-define if needed, or pass from caller if fonts are global
        Font(R.font.montserrat_semibold, FontWeight.SemiBold)
    )

    val showTopFade by remember { derivedStateOf { scrollState.value > 0 } }
    val showBottomFade by remember { derivedStateOf { scrollState.value < scrollState.maxValue } }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoadingAnalysis -> {
                // Display loading indicator centered in the Box
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFCD04C))
                    Text(
                        text = "Analyzing your photo...",
                        fontFamily = Montserrat,
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }
            }
            analysisErrorMessage != null -> {
                // Display error message centered
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = analysisErrorMessage,
                        fontFamily = Montserrat,
                        fontSize = 16.sp,
                        color = Color.Red,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            analysisResult != null -> {
                // Display scrollable analysis result
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    // REMOVED: verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "AI Photography Tips:",
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black,
                        style = LocalTextStyle.current.copy(letterSpacing = 1.sp)
                    )
                    // Added a small spacer after "AI Photography Tips:"
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = analysisResult.suggestion,
                        fontFamily = Montserrat,
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    analysisResult.enhancedImage?.let { enhancedBitmap ->
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
                    // Add a spacer at the bottom of the scrollable content to ensure
                    // there's some padding below the last element before the fade.
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Top fade overlay (only applies to scrollable content)
                if (showTopFade) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White, Color.White.copy(alpha = 0f))
                                )
                            )
                    )
                }

                // Bottom fade overlay (only applies to scrollable content)
                if (showBottomFade) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0f), Color.White)
                                )
                            )
                    )
                }
            }
            // This is the "Tap the camera" case
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Tap the camera to take a photo and get AI tips!",
                        fontFamily = Montserrat,
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}