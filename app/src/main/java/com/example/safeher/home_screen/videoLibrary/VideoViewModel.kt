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
    private val videoManager = VideoManager(context)
    private var frameIndex: Int = 0
    @Volatile
    private var isConverting: Boolean = false

    private val _videoToGallery = MutableLiveData<Result<Uri>>()
    val videoToGallery: LiveData<Result<Uri>> = _videoToGallery

    private val _conversionProgress = MutableLiveData<Int>()
    val conversionProgress: LiveData<Int> = _conversionProgress

    private val _conversionError = MutableLiveData<String?>()
    val conversionError: LiveData<String?> = _conversionError

    fun cleanUpTempFiles() {
        videoManager.cleanupTempFiles()
        frameIndex = 0
        _conversionError.postValue(null)
    }

    fun processVideo() {
        if (isConverting) {
            Log.d("VideoViewModel", "Video is already being converted")
            return
        }

        if (getFrameCount() == 0) {
            _conversionError.postValue("No frames available to convert")
            return
        }

        isConverting = true
        _conversionProgress.postValue(0)
        _conversionError.postValue(null)

        videoManager.convertToVideo(frameRate = 12, object : VideoManager.ConversionCallback {
            override fun onSuccess(outputFile: File) {
                Log.d("VideoViewModel", "Video conversion successful: ${outputFile.path}")
                _conversionProgress.postValue(100)

                videoManager.saveToPublicStorage(outputFile) { publicUri ->
                    isConverting = false
                    if (publicUri != null) {
                        Log.d("VideoViewModel", "Video saved to gallery: $publicUri")
                        _videoToGallery.postValue(Result.success(publicUri))
                    } else {
                        Log.e("VideoViewModel", "Failed to save video to gallery")
                        _conversionError.postValue("Failed to save video to gallery")
                        _videoToGallery.postValue(Result.failure(Exception("Failed to save video to gallery")))
                    }
                }
            }

            override fun onFailure(error: String) {
                Log.e("VideoViewModel", "Video conversion failed: $error")
                isConverting = false
                _conversionError.postValue(error)
                _videoToGallery.postValue(Result.failure(Exception(error)))
            }
        })
    }

    fun saveImageData(data: ByteArray): Boolean {
        val success = videoManager.saveImageData(data, frameIndex)
        if (success) {
            frameIndex++
        }
        return success
    }

    fun getFrameCount(): Int {
        return videoManager.getFrameCount()
    }

    fun getCurrentFrameIndex(): Int {
        return frameIndex
    }

    fun resetConversionState() {
        isConverting = false
        _conversionError.postValue(null)
        _conversionProgress.postValue(0)
    }
}