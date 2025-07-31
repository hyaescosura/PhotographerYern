package com.example.photagrapheryern

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDisplayScreen(
    navController: NavController,
    photoUri: Uri?
) {
    val context = LocalContext.current

    val Montserrat = FontFamily(
        Font(R.font.montserrat_semibold, FontWeight.SemiBold)
    )

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
                    .padding(horizontal = 16.dp), // General horizontal padding
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
                        Image(
                            painter = rememberAsyncImagePainter(model = photoUri),
                            contentDescription = "Captured Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.FillWidth
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // MODIFIED: Row for the buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            // Change arrangement to Center, and use a Spacer for custom gap
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Recapture Button
                            Button(
                                onClick = { navController.popBackStack() },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D472B)),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    text = "Recapture",
                                    color = Color.White,
                                    fontFamily = Montserrat,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }

                            // NEW: Spacer between the buttons to control their proximity
                            Spacer(modifier = Modifier.width(56.dp)) // Adjust this width (e.g., 8.dp, 16.dp, 24.dp)

                            // SHARE BUTTON
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1D472B),
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
        }
    }
}