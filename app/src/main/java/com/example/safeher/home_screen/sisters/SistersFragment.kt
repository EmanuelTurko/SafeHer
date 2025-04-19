package com.example.safeher.home_screen.sisters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.safeher.R

class SistersFragment :Fragment() {
    lateinit var mBackBtn: CardView
    lateinit var mHome: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sisters, container, false)
        initView(view)
        initListener()
        return view
    }

    private fun initView(view: View) {
        mBackBtn = view.findViewById(R.id.backButtonCard)
        mHome = view.findViewById(R.id.homeButtonCard)
    }

    private fun initListener() {
        mBackBtn.setOnClickListener {
            findNavController().navigate(R.id.action_sistersFragment_to_SOSHomeScreenFragment)
        }

        mHome.setOnClickListener {
            findNavController().navigate(R.id.action_sistersFragment_to_SOSHomeScreenFragment)
        }
    }
}