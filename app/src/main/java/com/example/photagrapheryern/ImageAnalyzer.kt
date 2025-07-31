// ImageAnalyzer.kt
package com.example.photagrapheryern

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.firebase.ai.*
import com.google.firebase.ai.type.GenerativeBackend


class ImageAnalyzer(
    private val onLabelDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)


    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage: Image = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        if (imageProxy.format != ImageFormat.YUV_420_888) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                val topLabel = labels.firstOrNull()?.text ?: "No label detected"
                onLabelDetected(topLabel)
            }
            .addOnFailureListener { e ->
                Log.e("ImageAnalyzer", "Labeling failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
