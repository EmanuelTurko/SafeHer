package com.example.safeher.settings.lobby

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.safeher.databinding.FragmentQuestionsAndAnswersBinding
import com.example.safeher.R


class QuestionsAndAnswersFragment : Fragment() {

    private var _binding: FragmentQuestionsAndAnswersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionsAndAnswersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back navigation
        binding.backButtonCard.setOnClickListener {
            findNavController().navigate(R.id.action_questionsAndAnswersFragment_to_settingsLobbyFragment)
        }
        binding.homeButton.setOnClickListener{
            findNavController().navigate(R.id.action_questionsAndAnswersFragment_to_SOSHomeScreenFragment)

        }

        // Toggle answers visibility for all questions
        binding.cardQ1.setOnClickListener { toggleVisibility(binding.answer1) }
        binding.cardQ2.setOnClickListener { toggleVisibility(binding.answer2) }
        binding.cardQ3.setOnClickListener { toggleVisibility(binding.answer3) }
        binding.cardQ4.setOnClickListener { toggleVisibility(binding.answer4) }
        binding.cardQ5.setOnClickListener { toggleVisibility(binding.answer5) }
        binding.cardQ6.setOnClickListener { toggleVisibility(binding.answer6) }
        binding.cardQ7.setOnClickListener { toggleVisibility(binding.answer7) }
        binding.cardQ8.setOnClickListener { toggleVisibility(binding.answer8) }
    }

    private fun toggleVisibility(view: View) {
        view.visibility = if (view.visibility == View.GONE) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
