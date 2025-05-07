package com.example.safeher.auth.safeCircle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.example.safeher.databinding.FragmentSafeCircleIntroBinding

class SafeCircleIntroFragment : Fragment() {

    private var _binding: FragmentSafeCircleIntroBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSafeCircleIntroBinding.inflate(inflater, container, false)

        binding.addButton.setOnClickListener {
            findNavController().navigate(R.id.action_safeCircleIntroFragment_to_pairFragment)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
