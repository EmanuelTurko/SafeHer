package com.example.safeher.home_screen.support

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.media.AudioManager

class VoiceRecognitionManager(
    private val context: Context,
    private val callback: (String) -> Unit
) {
    private val TAG = "SupportCallAiFragment"
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isRecognizing = false  // Track recognition status to prevent continuous restart

    init {
        val listener = object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()
                Log.d(TAG, "✅ Recognized speech: $spokenText")
                if (!spokenText.isNullOrBlank()) {
                    callback(spokenText)
                }
            }

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "🎤 Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "🎤 Speech started")
                adjustVolume(-10)  // Lower volume when speech starts
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "🛑 Speech ended")
                adjustVolume(10)  // Raise volume when speech ends
                isRecognizing = false
                speechRecognizer.stopListening()
            }

            override fun onError(error: Int) {
                Log.e(TAG, "❌ Error occurred: $error")
                if (!isRecognizing) {
                    isRecognizing = true
                    when (error) {
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            Log.e(TAG, "❌ Recognizer busy, retrying after delay")
                            Thread.sleep(1000)  // Retry after 1 second
                            startListening()
                        }
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                            Log.e(TAG, "❌ Network timeout, retrying after delay")
                            Thread.sleep(1000)  // Retry after 1 second
                            startListening()
                        }
                        SpeechRecognizer.ERROR_SERVER -> {
                            Log.e(TAG, "❌ Server error, retrying after delay")
                            Thread.sleep(1000)  // Retry after 1 second
                            startListening()
                        }
                        SpeechRecognizer.ERROR_NO_MATCH -> {
                            Log.e(TAG, "❌ No match found, retrying after delay")
                            Thread.sleep(1000)  // Retry after 1 second
                            startListening()
                        }
                        SpeechRecognizer.ERROR_CLIENT -> {
                            // Client-side error, log and retry
                            Log.e(TAG, "❌ Client error, retrying...")
                            startListening()
                        }
                        else -> {
                            // If it's a different error, just stop listening
                            Log.e(TAG, "❌ Unknown error, stopping recognition.")
                            stopListening()
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)
    }

    private fun adjustVolume(step: Int) {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (currentVolume + step).coerceIn(0, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    }

    fun startListening() {
        if (isRecognizing) return  // Prevent starting multiple recognitions at once
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        Log.d(TAG, "🎙️ Starting recognition")
        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        isRecognizing = false
        speechRecognizer.stopListening()
    }

    fun destroy() {
        speechRecognizer.destroy()
    }
}
