package com.example.safeher.home_screen.videoLibrary

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.safeher.video.VideoManager
import java.io.File

class VideoViewModel(context: Context): ViewModel() {

    val videoManager = VideoManager(context)
    var frameIndex : Int = 0
    @Volatile
    var isConverting:Boolean = false

    private val _videoToGallery = MutableLiveData<Result<Uri>>()
    val videoToGallery: LiveData<Result<Uri>> = _videoToGallery

    fun cleanUpTempFilesBeforeConverting(){
        videoManager.cleanupTempFiles()
        frameIndex = 0
    }
    fun processVideo() {
        Log.d("VideoViewModel", "stopRecording: $isConverting ")
        if (isConverting) {
            Log.d("VideoViewModel", "Video is already being converted")
            return
        }
        isConverting = true
        videoManager.convertToVideo(frameRate = 12, object : VideoManager.ConversionCallback {
            override fun onSuccess(outputFile: File) {
                videoManager.saveToPublicStorage(outputFile) { publicUri ->
                    if (publicUri != null) {
                        Log.d("VideoViewModel", "Video saved to gallery: $publicUri")
                        _videoToGallery.postValue(Result.success(publicUri))
                        Log.d("VideoViewModel", "is converting: $isConverting ")
                        isConverting = false
                        cleanUpTempFilesBeforeConverting()
                    } else {
                        Log.e("VideoViewModel", "Failed to save video to gallery")
                        _videoToGallery.postValue(Result.failure(Exception("Failed to save video to gallery")))
                        Log.d("VideoViewModel", "is converting2: $isConverting ")
                        isConverting = false
                        cleanUpTempFilesBeforeConverting()
                    }
                }

            }

            override fun onFailure(error: String) {
                Log.d("VideoViewModel", "onFailure: $error")
                _videoToGallery.postValue(Result.failure(Exception(error)))
            }
        })
    }

    fun saveImageData(data: ByteArray, index: Int): Boolean {
        return videoManager.saveImageData(data, index)
    }
    fun getFrameCount(): Int{
        return videoManager.getFrameCount()
    }
    fun nextFrame(){
        frameIndex++
    }
}