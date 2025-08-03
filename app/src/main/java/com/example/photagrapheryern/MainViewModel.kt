import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.photagrapheryern.AIState
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.asImageOrNull
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val aiModel: GenerativeModel
): ViewModel() {
    private var _aiStatus = MutableStateFlow(AIState.Success)
    val aiStatus = _aiStatus

    private var _aiSuggestion = MutableStateFlow<List<String>>(emptyList())
    val aiSuggestion = _aiSuggestion

    private var _aiBitmap: MutableStateFlow<Bitmap?> = MutableStateFlow<Bitmap?>(null)
    val aiBitmap = _aiBitmap

    fun onPhotoTaken(path: String?) {
        if(path != null) {
            val imgFile = File(path)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                analyzeImage(bitmap)
            } else {
                println("Image does not exists")
            }
        }
    }

    @OptIn(PublicPreviewAPI::class)
    fun analyzeImage(bitmap: Bitmap) {
        _aiStatus.value = AIState.Loading
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Provide a prompt that includes the image specified above and text
                val prompt = content {
                    image(bitmap)
                    //text("Suggest camera settings on Android mobile device to improve the image quality.")
                    text("""
                            Provide tips on angle, lighting to improve the image.
                            Create improved images with the applied tips.
                            """.trimIndent()
                    )
                }

                // To generate text output, call generateContent with the prompt
                //val response = aiModel.generateContent(prompt)
                val responseContent = aiModel.generateContent(prompt).candidates.first().content

                // The response will contain image and text parts interleaved
                for (part in responseContent.parts) {
                    when (part) {
                        is ImagePart -> {
                            // ImagePart as a bitmap
                            _aiBitmap.value = part.asImageOrNull()
                        }
                        is TextPart -> {
                            // Text content from the TextPart
                            val text = part.text
                            println("AI suggestion: $text")
                            aiSuggestion.update { currentList ->
                                currentList + part.text
                            }
                        }
                    }
                }
//                aiSuggestion.update { currentList ->
//                    currentList + tmpSuggestion
//                }
                _aiStatus.value = AIState.Success
            } catch (e: Exception) {
                _aiStatus.value = AIState.Failed
                Log.e("FirebaseAI", "Error calling Firebase AI: ${e.message}", e)
            }
        }
    }

    companion object {
        // Used to inject this ViewModel's dependencies
        // See also: https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories
        val jsonSchema = Schema.obj(
            mapOf("cameraSettings" to Schema.array(
                Schema.obj(
                    mapOf(
                        "settingsName" to Schema.string(),
                        "suggestedSettingsValue" to Schema.string(),
                    ),
                )
            ))
        )

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val firebaseAI = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(
                        "gemini-2.0-flash-preview-image-generation",
                        generationConfig = generationConfig {
                            //responseMimeType = "application/json"
                            //responseSchema = jsonSchema
                            responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
                        }
                    )

                return MainViewModel(firebaseAI) as T
            }
        }
    }

}