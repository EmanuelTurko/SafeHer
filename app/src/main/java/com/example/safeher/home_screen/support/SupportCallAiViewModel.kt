package com.example.safeher.home_screen.support

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safeher.api.RetroFitAiClient
import com.example.safeher.model.api.AiChatRequest
import com.example.safeher.model.api.AiChatResponse
import com.example.safeher.model.api.Content
import com.example.safeher.model.api.Part
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class SupportCallAiViewModel: ViewModel() {

    lateinit var textToSpeechManager : TextToSpeechManager
    fun initializeTextToSpeech(context: Context){
        Log.d("SupportCallAiViewModel", "Initializing TextToSpeechManager")
        textToSpeechManager = TextToSpeechManager(context)
    }

    private val _aiResponse = MutableStateFlow<String>("")
    val aiResponse: StateFlow<String> = _aiResponse

    private val _autoVoiceTrigger = MutableStateFlow<Boolean>(false)
    val autoVoiceTrigger: StateFlow<Boolean> = _autoVoiceTrigger

    fun askGemini(message:String){
        Log.d("SupportCallAiFragment", "Sending message to Gemini: $message")
        viewModelScope.launch {
            try{
                val prompt = """
    Respond directly to the following message as if you're part of a conversation, using 1-2 sentences:
    "$message"
    """.trimIndent()

                val request = AiChatRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = prompt)
                            )
                        )
                    )
                )
                val response : Response<AiChatResponse> = RetroFitAiClient.api.generateContent(request)
                if(response.isSuccessful){
                    val aiChatResponse = response.body()
                    val content = aiChatResponse?.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text
                        ?: "No response"

                    Log.d("SupportCallAiFragment", "Received Gemini's response: $content")
                    _aiResponse.value = content

                    textToSpeechManager.speak(content) {
                        Log.d("SupportCallAiFragment", "Finished speaking, triggering next voice input")
                        _autoVoiceTrigger.value = true
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = "Error: ${response.code()} - ${response.message()} | $errorBody"
                    Log.e("SupportCallAiFragment", errorMessage)
                    _aiResponse.value = errorMessage
                }
            } catch(e: Exception){
                val exceptionMessage = "Exception: ${e.message}"
                Log.e("SupportCallAiFragment", exceptionMessage)
                _aiResponse.value = "Error: ${e.message}"
            }

        }
    }
    fun handleUserVoiceInput(userVoiceInput: String){
        Log.d("SupportCallAiFragment", "handleUserVoiceInput called with: $userVoiceInput")
        askGemini(userVoiceInput)
    }
    fun stopSpeaking(){
        Log.d("SupportCallAiFragment", "Stopping TextToSpeech")
        textToSpeechManager.stop()
    }
    fun resetAutoVoiceTrigger(){
        Log.d("SupportCallAiFragment", "Resetting auto voice trigger")
        _autoVoiceTrigger.value = false
    }
}