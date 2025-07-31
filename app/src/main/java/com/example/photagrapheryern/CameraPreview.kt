package com.example.photagrapheryern

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.photagrapheryern.ml.ImageAnalyzer
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CameraPreview(lifecycleOwner: LifecycleOwner) {
    val context = LocalContext.current
    val labelText = remember { mutableStateOf("Point your camera at something...") }

    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(flashMode)
            .build()
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    // Rebind the camera whenever lensFacing or flashMode changes
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
                    ImageAnalyzer { label -> labelText.value = label }
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
            Log.e("CameraPreview", "Use case binding failed", exc)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = labelText.value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = {
                flashMode = if (flashMode == ImageCapture.FLASH_MODE_ON)
                    ImageCapture.FLASH_MODE_OFF else ImageCapture.FLASH_MODE_ON
            }) {
                Icon(
                    imageVector = if (flashMode == ImageCapture.FLASH_MODE_ON)
                        Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle Flash"
                )
            }

            IconButton(onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                    CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            }) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera"
                )
            }
        }

        Button(
            onClick = {
                takePhotoWithMediaStore(context, imageCapture)
            },
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Take Photo")
        }
    }
}

@SuppressLint("SimpleDateFormat")
private fun takePhotoWithMediaStore(context: Context, imageCapture: ImageCapture) {
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
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Toast.makeText(context, "Photo saved to gallery", Toast.LENGTH_SHORT).show()
                Log.d("CameraPreview", "Photo saved to MediaStore")
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Photo capture failed", Toast.LENGTH_SHORT).show()
                Log.e("CameraPreview", "MediaStore save failed", exception)
            }
        }
    )
}
