//package com.example.safeher.video
//
//import android.content.ContentValues
//import android.content.Context
//import android.graphics.BitmapFactory
//import android.media.MediaScannerConnection
//import android.net.Uri
//import android.os.Environment
//import android.provider.MediaStore
//import android.util.Log
//import java.io.File
//import java.io.FileOutputStream
//import androidx.core.net.toUri
//
//class VideoManager(private val context: Context) {
//    private val tempImageDir by lazy {
//        File(context.getExternalFilesDir(null), "esp32_images").apply {
//            if (!exists()) mkdirs()
//        }
//    }
//
//    interface ConversionCallback {
//        fun onSuccess(uri: File)
//        fun onFailure(error: String)
//    }
//
//    fun saveImageData(data: ByteArray, index: Int): Boolean {
//        return try {
//            val imageFile = File(tempImageDir, "frame_${index.toString().padStart(4, '0')}.jpg")
//            FileOutputStream(imageFile).use { it.write(data) }
//
//            // Enhanced validation
//            val options = BitmapFactory.Options().apply {
//                inJustDecodeBounds = true
//            }
//            BitmapFactory.decodeByteArray(data, 0, data.size, options)
//
//            if (options.outWidth <= 0 || options.outHeight <= 0) {
//                Log.e("VideoProcessor", "Invalid JPEG dimensions for frame $index")
//                imageFile.delete()
//                false
//            } else {
//                Log.d("VideoProcessor", "Saved frame $index (${data.size} bytes, ${options.outWidth}x${options.outHeight})")
//                true
//            }
//        } catch (e: Exception) {
//            Log.e("VideoProcessor", "Error saving frame $index", e)
//            false
//        }
//    }
//
//    fun convertToVideo(frameRate: Int = 12, callback: ConversionCallback? = null) {
//        try {
//            val frames = tempImageDir.listFiles { file ->
//                file.name.matches(Regex("frame_\\d{4}\\.jpg"))
//            }?.sortedBy { it.name }
//
//            if (frames.isNullOrEmpty()) {
//                callback?.onFailure("No frames found in ${tempImageDir.absolutePath}")
//                return
//            }
//
//            // Validate all frames before conversion
//            frames.forEachIndexed { index, file ->
//                if (!file.exists() || file.length() == 0L) {
//                    callback?.onFailure("Missing or empty frame: ${file.name}")
//                    return
//                }
//            }
//
//            val outputDir = File(
//                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
//                "esp32_videos"
//            ).apply {
//                if (!exists() && !mkdirs()) {
//                    callback?.onFailure("Failed to create output directory")
//                    return
//                }
//            }
//
//            val outputFile = File(outputDir, "output_${System.currentTimeMillis()}.mp4").apply {
//                if (exists() && !delete()) {
//                    callback?.onFailure("Couldn't clear existing output file")
//                    return
//                }
//            }
//
//            checkAvailableCodecs { hasLibx264 ->
//                val ffmpegCommand = if (hasLibx264) {
//                    """
//                    -y -framerate $frameRate
//                    -i ${tempImageDir.absolutePath}/frame_%04d.jpg
//                    -c:v libx264 -preset ultrafast -pix_fmt yuv420p
//                    -vf "scale=800:-2,format=yuv420p"
//                    -movflags +faststart
//                    ${outputFile.absolutePath}
//                    """.trimIndent().replace("\n", " ")
//                } else {
//                    """
//                    -y -framerate $frameRate
//                    -i ${tempImageDir.absolutePath}/frame_%04d.jpg
//                    -c:v mpeg4 -q:v 2 -pix_fmt yuv420p
//                    -vf "scale=800:-2,format=yuv420p"
//                    -movflags +faststart
//                    ${outputFile.absolutePath}
//                    """.trimIndent().replace("\n", " ")
//                }
//
//                Log.d("VideoProcessor", "Executing FFmpeg: $ffmpegCommand")
//                executeConversion(ffmpegCommand, outputFile, callback)
//            }
//        } catch (e: Exception) {
//            Log.e("VideoProcessor", "Conversion setup failed", e)
//            callback?.onFailure("Setup error: ${e.message}")
//        }
//    }
//    private fun executeConversion(
//        command: String,
//        outputFile: File,
//        callback: ConversionCallback?
//    ) {
//        FFmpegKit.executeAsync(command) { session ->
//            val returnCode = session.returnCode
//
//            if (returnCode.isValueSuccess) {
//                Log.d("VideoProcessor", "FFmpeg conversion succeeded")
//                callback?.onSuccess(outputFile) // pass the actual file
//            } else {
//                Log.e("VideoProcessor", "FFmpeg conversion failed")
//                callback?.onFailure("Conversion failed")
//            }
//        }
//    }
//
//
//    private fun checkAvailableCodecs(callback: (hasLibx264: Boolean) -> Unit) {
//        FFmpegKit.executeAsync("-codecs") { session ->
//            val logs = session.logs?.joinToString("\n") { it.message } ?: "No codec info available"
//
//            val hasLibx264 = logs.contains("libx264") &&
//                    logs.contains("H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10")
//            callback(hasLibx264)
//        }
//    }
//
//   /* private fun executeConversion(command: String, outputFile: File, callback: ConversionCallback?) {
//        FFmpegKit.executeAsync(command) { session ->
//            try {
//                when {
//                    ReturnCode.isSuccess(session.returnCode) && outputFile.exists() -> {
//                        Log.d("VideoProcessor", "Video created: ${outputFile.length()} bytes")
//                        val uri = FileProvider.getUriForFile(
//                            context,
//                            "${context.packageName}.provider",
//                            outputFile
//                        )
//                        callback?.onSuccess(uri)
//                    }
//                    else -> {
//                        val errorLogs = session.logs?.joinToString("\n") { it.message }
//                            ?: "No FFmpeg logs available"
//                        Log.e("VideoProcessor", "Conversion failed. Logs:\n$errorLogs")
//                        outputFile.delete()
//                        callback?.onFailure("""
//                            FFmpeg Error:
//                            Return Code: ${session.returnCode}
//                            $errorLogs
//                        """.trimIndent())
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("VideoProcessor", "Post-processing error", e)
//                callback?.onFailure("Post-processing error: ${e.message}")
//            } finally {
//                cleanupTempFiles()
//            }
//        }
//    }*/
//
//    fun saveToPublicStorage(sourceFile: File, callback: (Uri?) -> Unit) {
//        if (!sourceFile.exists()) {
//            Log.e("VideoProcessor", "Source file does not exist: ${sourceFile.absolutePath}")
//            callback(null)
//            return
//        }
//        val resolver = context.contentResolver
//        val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//        val videoName = "SafeHer_${System.currentTimeMillis()}.mp4"
//        val relativeLocation = "${Environment.DIRECTORY_MOVIES}/ESP32_Videos"
//
//        Log.d("VideoProcessor", "Saving video to $relativeLocation/$videoName")
//
//        // ContentValues for the video
//        val contentValues = ContentValues().apply {
//            put(MediaStore.Video.Media.DISPLAY_NAME, videoName)
//            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
//            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
//            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
//            put(MediaStore.Video.Media.RELATIVE_PATH, relativeLocation)
//            put(MediaStore.Video.Media.IS_PENDING, 1)
//        }
//
//        Log.d("VideoProcessor", "Inserting video into MediaStore")
//
//        val uri = resolver.insert(videoCollection, contentValues)
//
//        if (uri != null) {
//            try {
//                resolver.openOutputStream(uri).use { outStream ->
//                    sourceFile.inputStream().use { inStream ->
//                        inStream.copyTo(outStream!!)
//                    }
//                }
//                Log.d("VideoProcessor", "Video written to MediaStore: $uri")
//
//                // Finalize the media so it's visible
//                contentValues.clear()
//                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
//                resolver.update(uri, contentValues, null, null)
//
//                // Force MediaStore to scan the new file
//                MediaScannerConnection.scanFile(
//                    context,
//                    arrayOf(uri.toString().toUri().path),
//                    arrayOf("video/mp4"),
//                    null
//                )
//                Log.d("VideoProcessor", "MediaStore scan completed for $uri")
//
//                callback(uri)
//
//            } catch (e: Exception) {
//                Log.e("VideoProcessor", "Error writing video file", e)
//                resolver.delete(uri, null, null)
//                callback(null)
//            }
//        } else {
//            Log.e("VideoProcessor", "Failed to create MediaStore entry")
//            callback(null)
//        }
//    }
//
//
//
//    internal fun cleanupTempFiles() {
//        tempImageDir.listFiles()?.forEach {
//            try {
//                if (!it.delete()) {
//                    Log.w("VideoProcessor", "Failed to delete ${it.name}")
//                }
//            } catch (e: Exception) {
//                Log.e("VideoProcessor", "Error deleting ${it.name}", e)
//            }
//        }
//    }
//
//    fun getFrameCount(): Int {
//        return tempImageDir.listFiles()?.count {
//            it.name.startsWith("frame_") && it.name.endsWith(".jpg")
//        } ?: 0
//    }
//}