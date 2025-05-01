package com.example.safeher.home_screen.support
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.util.Log
import java.util.Locale


class VoiceRecognitionManager(
    private val context: Context,
    private val callback: (String)-> Unit) {

    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

    init{
        val listener = object: RecognitionListener{

            override fun onResults(results: Bundle?){
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()
                Log.d("SupportCallAiFragment", "Recognized speech: $spokenText")
                if(!spokenText.isNullOrBlank()){
                    callback(spokenText)
                }
            }


            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SupportCallAiFragment", "Ready for speech")
            }
            override fun onBeginningOfSpeech() {
                Log.d("SupportCallAiFragment", "Speech started")
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d("SupportCallAiFragment", "Speech ended")
            }
            override fun onError(error: Int) {
                Log.e("SupportCallAiFragment", "Error occurred: $error")
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}


        }
        speechRecognizer.setRecognitionListener(listener)

    }
    fun startListening() {

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "he-IL") // Strictly Hebrew
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false) // Try offline first

            // These help restrict it to Hebrew only
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "he-IL")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "he-IL")
        }

        if (!isHebrewOfflineModelAvailable()) {
            Log.e("VoiceRecognition", "Hebrew offline speech model not available on this device")
            // Optional: fallback to online mode by disabling EXTRA_PREFER_OFFLINE
            // or show a UI message
        }

        Log.d("VoiceRecognition", "Starting recognition in Hebrew only")
        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }
    fun isHebrewOfflineModelAvailable(): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS)
        var availableLanguages: List<String>? = null

        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                availableLanguages = intent
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)
            }
        }

        context.sendOrderedBroadcast(intent, null, receiver, null, 0, null, null)

        // You might need to delay this check slightly or persist the result
        // Because it's async
        return availableLanguages?.contains("he-IL") ?: false
    }
}