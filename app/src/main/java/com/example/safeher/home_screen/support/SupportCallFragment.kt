package com.example.safeher.home_screen.support

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.example.safeher.databinding.FragmentSupportCallBinding

class SupportCallFragment : Fragment() {
    private var binding: FragmentSupportCallBinding? = null
    private val viewModel : SupportCallViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
        ) :View?{
        binding = FragmentSupportCallBinding.inflate(inflater, container, false)


        return binding?.root
    }
    override fun onViewCreated(view:View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AlertDialog.Builder(requireContext())
            .setTitle("Support Call")
            .setMessage("Do you want a support call?")
            .setPositiveButton("Human support") { dialog, _ ->
               //sister's logic
                viewModel.selectCallMode(SupportCallViewModel.CallMode.SISTER)

            }
            .setNegativeButton("Virtual intelligence support") { dialog, _ ->
               //AI logic
                viewModel.selectCallMode(SupportCallViewModel.CallMode.AI)
                findNavController().navigate(R.id.action_supportCallFragment_to_supportCallAIFragment)
            }
            .setCancelable(true)
            .show()

    }
    private fun updateCallLayout(callMode: SupportCallViewModel.CallMode){
        when(callMode){
            SupportCallViewModel.CallMode.AI -> {

            }
            SupportCallViewModel.CallMode.SISTER -> {

            } else -> {
                // NONE mode
            }
        }
    }
    override fun onDestroy(){
        super.onDestroy()
        binding = null
    }
}