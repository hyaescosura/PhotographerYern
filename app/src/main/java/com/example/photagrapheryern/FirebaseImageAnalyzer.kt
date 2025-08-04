package com.example.photagrapheryern

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.asImageOrNull
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig

object FirebaseImageAnalyzer {
    val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = "gemini-2.0-flash-preview-image-generation",
        // Configure the model to respond with text and images
        generationConfig = generationConfig {
            responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE) }
    )

    suspend fun analyzeImage(bitmap: Bitmap, prompttext: String = "What should I improve in this photo?"): Pair<String, Bitmap> {
        return try {
            val prompt = content {
                image (bitmap)
                text (prompttext)
            }
            var generatedImageAsBitmap: Bitmap? = null
            var text = ""
            val response = model.generateContent(prompt).candidates.first().content
            for (part in response.parts) {
                when (part) {
                    is ImagePart -> {
                        // ImagePart as a bitmap
                        generatedImageAsBitmap = part.asImageOrNull()!!
                    }
                    is TextPart -> {
                        // Text content from the TextPart
                        text = part.text
                    }
                }
            }

            return Pair(text, generatedImageAsBitmap ?: bitmap) // fallback to original if null
            //response.text ?: "No suggestions found."
        } catch (e: Exception) {
            Log.e("FirebaseAI", "Analysis failed", e)
            return Pair("Error: ${e.localizedMessage}", bitmap) // use original bitmap as fallback
        }
    }
}
