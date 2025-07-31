package com.example.photagrapheryern

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.*
import kotlinx.coroutines.tasks.await
import com.google.firebase.Firebase
import com.google.firebase.ai.type.content

object FirebaseImageAnalyzer {
    val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-2.5-flash")

    suspend fun analyzeImage(bitmap: Bitmap, prompt: String = "What should I improve in this photo?"): String {
        return try {
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            response.text ?: "No suggestions found."
        } catch (e: Exception) {
            Log.e("FirebaseAI", "Analysis failed", e)
            "Error: ${e.localizedMessage}"
        }
    }
}
