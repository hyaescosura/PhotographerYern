package com.example.photagrapheryern

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border // This import will still be there but not used for the border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth // Import fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign // Import TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily // Import FontFamily

// Define your Montserrat Semibold font family here
// Make sure you have montserrat_semibold.ttf in your res/font directory
val Montserrat = FontFamily(
    Font(R.font.montserrat_semibold, FontWeight.SemiBold)
)

@OptIn(androidx.compose.ui.graphics.ExperimentalGraphicsApi::class)
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun GlassmorphismCardWithText(
    text: String,
    modifier: Modifier = Modifier,
    blurRadius: Float = 25f
) {
    Box(modifier = modifier) {
        // 1. Blurred Background Layer
        Box(
            modifier = Modifier
                .matchParentSize() // Matches the size provided by the outer Box's modifier
                .clip(RoundedCornerShape(16.dp))
                .graphicsLayer {
                    renderEffect = RenderEffect.createBlurEffect(
                        blurRadius,
                        blurRadius,
                        Shader.TileMode.DECAL
                    ).asComposeRenderEffect()
                }
                .background(Color.White.copy(alpha = 0.05f))
        )

        // 2. The Actual Glass Card (on top of the blur)
        Card(
            modifier = Modifier
                .matchParentSize() // Matches the size provided by the outer Box's modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            // .border( // <--- REMOVED THIS SECTION
            //     BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            //     RoundedCornerShape(16.dp)
            // ),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            // Inner Box for text and its padding
            Box(
                modifier = Modifier
                    .wrapContentSize(align = Alignment.Center) // Ensures this Box wraps its content and aligns it
                    .padding(horizontal = 16.dp, vertical = 12.dp), // Padding around the text
                contentAlignment = Alignment.Center // Centers the *child* (Text) within *this* Box
            ) {
                Text(
                    text = text,
                    color = Color.Black,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = Montserrat, // Apply the Montserrat font family
                        fontWeight = FontWeight.SemiBold, // Ensure Semibold weight
                        fontSize = 20.sp // You might want to adjust font size for this font
                    ),
                    modifier = Modifier.fillMaxWidth(), // Ensures Text fills its parent's width
                    textAlign = TextAlign.Center // Centers the text itself within its bounds
                )
            }
        }
    }
}

// Preview code (remains the same)
@Preview(showBackground = true)
@Composable
fun GlassmorphismCardPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFF2196F3)) // A colored background to see the blur effect
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Example with different lengths of text to test flexibility
            GlassmorphismCardWithText(
                text = "Short Label",
                modifier = Modifier.padding(bottom = 16.dp)
            )
            GlassmorphismCardWithText(
                text = "This is a slightly longer AI label example.",
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}