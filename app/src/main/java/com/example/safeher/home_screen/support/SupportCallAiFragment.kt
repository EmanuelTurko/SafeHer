package com.example.safeher.home_screen.support

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.safeher.databinding.FragmentSupportCallAiBinding
import kotlinx.coroutines.delay
import androidx.navigation.fragment.findNavController


class SupportCallAiFragment :Fragment() {
    private var binding: FragmentSupportCallAiBinding? = null
    private val viewModel: SupportCallAiViewModel by viewModels()
    private lateinit var voiceRecognitionManager: VoiceRecognitionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSupportCallAiBinding.inflate(inflater, container, false)
        return binding?.root


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.initializeTextToSpeech(requireContext())

        voiceRecognitionManager = VoiceRecognitionManager(requireContext()) { userSpeech ->
            Log.d("SupportCallAiFragment", "User said: $userSpeech")
            viewModel.handleUserVoiceInput(userSpeech)
        }
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                1001
            )
        } else {
            voiceRecognitionManager.startListening()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.autoVoiceTrigger.collect { trigger ->
                if(trigger){
                    delay(500)
                    voiceRecognitionManager.startListening()
                    viewModel.resetAutoVoiceTrigger()
                }
            }
        }



        binding?.callDurationTextView?.text = "00:00"
        callTimer()


        binding?.mutedImageBtn?.setOnClickListener {
            val ttsSettingsIntent = Intent("com.android.settings.TTS_SETTINGS")
            startActivity(ttsSettingsIntent)
        }
        binding?.speakerImageBtn?.setOnClickListener {
            viewModel.textToSpeechManager.speak("שלום, איך אני יכולה לעזור?")
        }

        binding?.hangUpImageBtn?.setOnClickListener{
            viewModel.stopSpeaking()
           findNavController().popBackStack()
        }
    }

    private fun callTimer(){
        var secondsPassed = 0

        viewLifecycleOwner.lifecycleScope.launch{
            while(true){
                val minutes = secondsPassed / 60
                val seconds = secondsPassed % 60
                binding?.callDurationTextView?.text = String.format("%02d:%02d", minutes, seconds)
                delay(1000)
                secondsPassed++
            }
        }

    }
    override fun onDestroy() {
        super.onDestroy()
        binding = null
        voiceRecognitionManager.stopListening()
        viewModel.stopSpeaking()
    }
}