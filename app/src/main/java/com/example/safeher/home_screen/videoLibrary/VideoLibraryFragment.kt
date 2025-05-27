package com.example.safeher.home_screen.videoLibrary

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safeher.R
import com.example.safeher.adapters.VideoAdapter
import java.io.File
import android.util.Log

class VideoLibraryFragment :Fragment() {

    var isFromSettings = false
    lateinit var mBackBtn: CardView
    lateinit var mHome: CardView

    lateinit var recyclerView: RecyclerView
    lateinit var videoAdapter: VideoAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_video_library, container, false)
        initView(view)
        initListener()
        try {
            // Retrieve the Goal object passed from the Activity
            if (arguments != null) {
                val args = VideoLibraryFragmentArgs.fromBundle(requireArguments())
                isFromSettings = args.fromSettings

            }
        }catch (e:Exception) {
            isFromSettings = false
        }

        setupRecyclerView()
        return view
    }

    private fun initView(view: View) {
        mBackBtn = view.findViewById(R.id.backButtonCard)
        mHome = view.findViewById(R.id.homeButton)
        recyclerView = view.findViewById(R.id.videoRecyclerView)

    }

    private fun initListener() {
        mBackBtn.setOnClickListener {
            if (isFromSettings) {
                findNavController().navigate(R.id.action_videoLibraryFragment2_to_settingsLobbyFragment)
            } else {
                findNavController().navigate(R.id.action_videoLibraryFragment_to_SOSHomeScreenFragment)
            }
        }

        mHome.setOnClickListener {
            if (isFromSettings) {
                findNavController().navigate(R.id.action_videoLibraryFragment2_to_SOSHomeScreenFragment)
            } else {
                findNavController().navigate(R.id.action_videoLibraryFragment_to_SOSHomeScreenFragment)
            }
        }

    }
        private fun setupRecyclerView(){
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            videoAdapter = VideoAdapter(requireContext(),getVideoFiles())
            recyclerView.adapter = videoAdapter

        }
    private fun getVideoFiles(): List<File> {
        val videoDir = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES), "ESP32_Videos")
        return videoDir.listFiles { file -> file.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
        Log.d("TestSample", "Video files: ${videoDir.listFiles()?.joinToString(", ") { it.name }}")
    }

    override fun onResume() {
        super.onResume()
        getVideoFiles()
    }

}