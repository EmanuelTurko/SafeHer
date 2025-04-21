package com.example.safeher.home_screen.support

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import android.util.Log
import com.example.safeher.util.SdkVersion

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
            val languageResult = textToSpeech.setLanguage(Locale("he", "IL"))
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(
                    "SupportCallAiFragment",
                    "Hebrew language is not supported or data is missing"
                )
            } else {
                val preferredVoiceName = "he-il-x-hee-local"
                val selectedVoice = textToSpeech.voices.firstOrNull { it.name == preferredVoiceName }
                if(selectedVoice!= null){
                    textToSpeech.voice = selectedVoice
                    Log.d("SupportCallAiFragment", "✅ Selected preferred voice: ${selectedVoice.name}")
                } else{
                    Log.w("SupportCallAiFragment", "⚠️ Preferred voice not found, using default")
                }
                ttsReady = true
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
    /*override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val hebrewVoices = textToSpeech.voices
                .filter { it.locale.language == "he" || it.locale.language == "iw" }

            Log.d("TTSVoice", "Found ${hebrewVoices.size} Hebrew voices")
            hebrewVoices.forEachIndexed { index, voice ->
                Log.d("TTSVoice", "Voice #${index + 1}: ${voice.name} | Locale: ${voice.locale}")
            }

            if (hebrewVoices.isNotEmpty()) {
                ttsReady = true
                testHebrewVoicesSequentially(hebrewVoices)
            } else {
                Log.d("TTSVoice", "No Hebrew voices found.")
            }
        } else {
            Log.d("TTSVoice", "TTS initialization failed")
        }
    }
   private fun testHebrewVoicesSequentially(voices: List<Voice>) {
        val testText = "שלום, זאת בדיקת קול מספר"
        var currentIndex = 0

        fun speakNext() {
            if (currentIndex < voices.size) {
                val voice = voices[currentIndex]
                textToSpeech.voice = voice
                val utteranceId = "voice-test-${currentIndex}"

                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        currentIndex++
                        speakNext()
                    }
                    override fun onError(utteranceId: String?) {
                        currentIndex++
                        speakNext()
                    }
                })

                Log.d("TTSVoice", "Speaking with voice #${currentIndex + 1}: ${voice.name}")
                textToSpeech.speak("$testText ${currentIndex + 1}", TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
        }

        speakNext()
    }*/

