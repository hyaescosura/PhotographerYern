package com.example.photagrapheryern

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
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
import com.example.photagrapheryern.ml.ImageAnalyzer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ... (rememberCameraUseCases function remains the same as before) ...
@Composable
fun rememberCameraUseCases(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    lensFacing: Int,
    flashMode: Int,
    onLabelDetected: (String) -> Unit
) {
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(lensFacing, flashMode) {
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val analyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                    ImageAnalyzer { label -> onLabelDetected(label) }
                )
            }

        imageCapture.flashMode = flashMode

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                preview,
                imageCapture,
                analyzer
            )
        } catch (exc: Exception) {
            Log.e("CameraFunctions", "Use case binding failed", exc)
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
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/PhotographerYern")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context), // Use the main executor for the callback
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri
                if (savedUri != null) {
                    Log.d("CameraFunctions", "Photo saved to MediaStore: $savedUri")
                    // Show a toast on the UI thread as this is a background operation result
                    Toast.makeText(context, "Photo saved to gallery!", Toast.LENGTH_SHORT).show()
                    continuation.resume(savedUri) // Resume with the Uri on success
                } else {
                    Log.e("CameraFunctions", "Photo saved, but URI is null.")
                    Toast.makeText(context, "Photo saved, but URI is null.", Toast.LENGTH_SHORT).show()
                    continuation.resume(null) // Resume with null if URI is unexpectedly null
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraFunctions", "Photo capture failed: ${exception.message}", exception)
                // Show a toast on the UI thread
                Toast.makeText(context, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                continuation.resumeWithException(exception) // Resume with exception on failure
            }
        }
    )

    // Handle cancellation of the coroutine
    continuation.invokeOnCancellation {
        // You can add cleanup logic here if necessary, though CameraX usually handles it.
        Log.d("CameraFunctions", "Photo capture coroutine cancelled.")
    }
}