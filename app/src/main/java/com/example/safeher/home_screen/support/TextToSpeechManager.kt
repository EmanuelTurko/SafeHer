package com.example.safeher.home_screen.support

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import android.util.Log

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val textToSpeech: TextToSpeech = TextToSpeech(context, this)
    private var ttsReady = false

    init {
        audioManager.mode = AudioManager.MODE_IN_CALL
        textToSpeech.setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val languageResult = textToSpeech.setLanguage(Locale("en", "US"))  // Set language to English (US)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(
                    "SupportCallAiFragment",
                    "English language is not supported or data is missing"
                )
            } else {
                // Set preferred English voice if available
                val preferredVoiceName = "en-us-x-std"
                val selectedVoice = textToSpeech.voices.firstOrNull { it.name == preferredVoiceName }
                if (selectedVoice != null) {
                    textToSpeech.voice = selectedVoice
                    Log.d("SupportCallAiFragment", "✅ Selected preferred voice: ${selectedVoice.name}")
                } else {
                    Log.w("SupportCallAiFragment", "⚠️ Preferred voice not found, using default")
                }
                ttsReady = true
                val voice = textToSpeech.voice
                Log.d("SupportCallAiFragment", "Voice name: ${voice.name}, isNetworkConnectionRequired: ${voice.isNetworkConnectionRequired}, isNotInstalled: ${voice.features?.contains("notInstalled")}")
                Log.d("SupportCallAiFragment", "TextToSpeech initialization succeeded")
            }
        } else {
            Log.d("SupportCallAiFragment", "TextToSpeech initialization failed")
        }
    }

    fun setOnSpeechDoneListener(onDone: () -> Unit) {
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onDone()
            }

            override fun onError(utteranceId: String?) {
                Log.e("SupportCallAiFragment", "Error during speech with utteranceId: $utteranceId")
            }
        })
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (ttsReady && text.isNotEmpty()) {
            val utteranceId = "utterance-${System.currentTimeMillis()}"
            if (onDone != null) {
                setOnSpeechDoneListener(onDone)
            }
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            Log.e("SupportCallAiFragment", "TTS is not ready or text is empty")
        }
    }

    fun stop() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        audioManager.mode = AudioManager.MODE_NORMAL
    }
}
