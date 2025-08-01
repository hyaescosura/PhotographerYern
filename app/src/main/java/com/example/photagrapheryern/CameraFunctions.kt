package com.example.photagrapheryern

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
//import android.graphics.Camera
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.photagrapheryern.ml.ImageAnalyzer // Assuming this is your local ML Kit ImageAnalyzer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.camera.core.Camera



// Data class for analysis result - This can stay in CameraFunctions if it's strictly for camera-related analysis
// Or move it to a shared file if used broadly across the app. For now, let's keep it here.
data class AnalysisResult(
    val suggestion: String,
    val enhancedImage: Bitmap
)

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
            Log.e("CameraFunctions", "Failed to bind camera", e)
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
                Log.d("CameraFunctions", "Saved: ${output.savedUri}")
                continuation.resume(output.savedUri)
            }

            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(context, "Capture failed", Toast.LENGTH_SHORT).show()
                Log.e("CameraFunctions", "Error: ${exc.message}", exc)
                continuation.resumeWithException(exc)
            }
        })
}