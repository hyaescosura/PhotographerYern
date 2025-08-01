package com.example.photagrapheryern

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.util.Log // Import Log for debugging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDisplayScreen(
    navController: NavController,
    photoUri: Uri?,
    fromCamera: Boolean = false // Default to false if not provided
) {
    val context = LocalContext.current

    val Montserrat = FontFamily(
        Font(R.font.montserrat_semibold, FontWeight.SemiBold)
    )

    var showPopup by remember { mutableStateOf(false) }

    Log.d("PhotoAppDebug", "PhotoDisplayScreen: Received fromCamera: $fromCamera (at start of composable)")
    Log.d("PhotoAppDebug", "PhotoDisplayScreen: Received photoUri: $photoUri")


    LaunchedEffect(key1 = fromCamera) {
        Log.d("PhotoAppDebug", "PhotoDisplayScreen: LaunchedEffect triggered with fromCamera: $fromCamera")
        if (fromCamera) {
            showPopup = true
            Log.d("PhotoAppDebug", "PhotoDisplayScreen: LaunchedEffect: Setting showPopup to true")
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
                        // THIS IS THE BOX THAT CONTAINS THE PHOTO AND NOW THE SPARKLE IMAGES
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
                                painter = painterResource(id = R.drawable.sparkle), // Using R.drawable.sparkle for top right
                                contentDescription = "Sparkle decoration",
                                modifier = Modifier
                                    .align(Alignment.TopEnd) // Position relative to the photo card
                                    .size(72.dp) // Set size to 72.dp
                                    .offset(x = 20.dp, y = (-20).dp), // Adjust offset to position outside but near the corner
                                contentScale = ContentScale.Fit
                            )

                            // SPARKLE IMAGE - Bottom Left of the PHOTO CARD
                            Image(
                                painter = painterResource(id = R.drawable.sparkle), // Using R.drawable.sparkle for bottom left
                                contentDescription = "Sparkle decoration",
                                modifier = Modifier
                                    .align(Alignment.BottomStart) // Position relative to the photo card
                                    .size(72.dp) // Set size to 72.dp
                                    .offset(x = (-20).dp, y = 20.dp), // Adjust offset to position outside but near the corner
                                contentScale = ContentScale.Fit
                            )
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

            // AlertDialog for the Pop-up Card (with close icon)
            if (showPopup) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                // Define the background color based on pressed state - TEMPORARILY BRIGHT FOR TESTING
                val buttonBackgroundColor = if (isPressed) {
                    Color.Blue.copy(alpha = 0.5f) // Change this line
                } else {
                    Color.Transparent
                }

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
                                    .padding(28.dp) // Main padding for the Card's content
                            ) {
                                // IMPORTANT: Column comes first, so the IconButton can be drawn on top
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(top = 16.dp), // Add top padding to avoid icon overlapping text
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "Photo Taken!",
                                        fontFamily = Montserrat,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = Color.Black,
                                        style = LocalTextStyle.current.copy(letterSpacing = 2.sp)
                                    )
                                    Text(
                                        text = "Congratulations! Your photograph has been successfully captured and processed. This image is now ready for you to explore its full potential. You can choose to apply various editing tools, enhance its colors, or add unique filters to make it truly shine. Alternatively, it's perfectly poised for sharing with your friends and family on social media or through direct messaging. We hope you cherish this moment captured through your lens.",
                                        fontFamily = Montserrat,
                                        fontSize = 16.sp,
                                        color = Color.DarkGray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Text(
                                        text = "Remember, every photo tells a story, and yours is just beginning. Take your time to perfect it, or share its raw beauty with the world. Our app provides all the tools you need to bring your creative vision to life. Enjoy the process of transforming your images into masterpieces, or simply sharing them as beautiful memories. Thank you for using our photography app!",
                                        fontFamily = Montserrat,
                                        fontSize = 16.sp,
                                        color = Color.DarkGray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }

                                // Close Icon at top right - MOVED CLOSER TO CORNER
                                IconButton(
                                    onClick = {
                                        Log.d("PhotoAppDebug", "Close button clicked! Attempting to close popup.")
                                        showPopup = false
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(36.dp) // Generous touch target size
                                        .offset(x = 18.dp, y = (-18).dp) // Adjusted offset for closer corner placement
                                        .clip(CircleShape) // Ensures circular touch area
                                        .background(buttonBackgroundColor) // Apply the dynamic background color
                                        .padding(8.dp), // Adds a buffer around the icon within the touch target
                                    interactionSource = interactionSource // Attach the interaction source
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close",
                                        tint = Color.DarkGray, // Keep icon tint constant
                                        modifier = Modifier.size(32.dp) // Icon size remains the same
                                    )
                                }
                            }
                        }
                    }
                )
            }
            // --- END: AlertDialog for Pop-up Card ---
        }
    }
}