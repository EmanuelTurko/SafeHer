package com.example.safeher.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Date

@Volatile
private var isEncoding = false

class VideoManager(private val context: Context) {

    private lateinit var tempImageDir: File

    init{
        prepareTempDir()
    }
    fun prepareTempDir() {
        // Create a unique folder for each session
        val sessionId = System.currentTimeMillis().toString()
        tempImageDir = File(context.getExternalFilesDir(null), "esp32_images_$sessionId").apply {
            if (!exists()) mkdirs()
        }
        Log.d("VideoProcessorDirectory", "Temp image directory: ${tempImageDir.absolutePath}")
    }

    internal fun cleanupTempFiles() {
        if (!::tempImageDir.isInitialized) {
            Log.w("VideoProcessorDirectory", "Temp directory not initialized. Skipping cleanup.")
            return
        }

        Log.d("VideoProcessorDirectory", "Cleaning up temporary files from: ${tempImageDir.absolutePath}")
        tempImageDir.listFiles()?.forEach {
            try {
                Log.d("VideoProcessorDirectory", "Found file: ${it.name}, lastModified: ${Date(it.lastModified())}")
                if (it.delete()) {
                    Log.d("VideoProcessorDirectory", "Deleted temporary file: ${it.name}")
                } else {
                    Log.w("VideoProcessorDirectory", "Failed to delete temporary file: ${it.name}")
                }
            } catch (e: Exception) {
                Log.e("VideoProcessorDirectory", "Error deleting temporary file ${it.name}", e)
            }
        }

        // Optionally delete the folder itself after cleaning its contents
        try {
            if (tempImageDir.delete()) {
                Log.d("VideoProcessorDirectory", "Deleted temp folder: ${tempImageDir.name}")
            } else {
                Log.w("VideoProcessorDirectory", "Failed to delete temp folder: ${tempImageDir.name}")
            }
        } catch (e: Exception) {
            Log.e("VideoProcessorDirectory", "Error deleting temp folder", e)
        }
    }

    interface ConversionCallback {
        fun onSuccess(outputFile: File)
        fun onFailure(error: String)
    }

    private var beforeAll = true
    fun saveImageData(data: ByteArray, index: Int): Boolean {
        if(tempImageDir.listFiles()?.size != 0 && beforeAll) {
            cleanupTempFiles()
            Log.d("VideoProcessor", "Dir is not empty, cleaning up")
        }
        beforeAll = false
        return try {
            val imageFile = File(tempImageDir, "frame_${index.toString().padStart(4, '0')}.jpg")
            Log.d("VideoProcessor", "Attempting to save frame $index to ${imageFile.absolutePath}")

            FileOutputStream(imageFile).use { it.write(data) }

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.e("VideoProcessor", "Invalid JPEG dimensions for frame $index")
                imageFile.delete()
                false
            } else {
                Log.d("VideoProcessor", "Successfully saved frame $index (${data.size} bytes, ${options.outWidth}x${options.outHeight})")
                true
            }
        } catch (e: Exception) {
            Log.e("VideoProcessor", "Error saving frame $index", e)
            false
        }
    }

    fun convertToVideo(frameRate: Int = 12, callback: ConversionCallback? = null) {
        try {
            Log.d("VideoProcessor", "Starting video conversion with frame rate: $frameRate")

            val frames = tempImageDir.listFiles { file ->
                file.name.matches(Regex("frame_\\d{4}\\.jpg"))
            }?.sortedBy { it.name }?.drop(2)
            Log.d("TestSample", "Found frame files: ${frames?.joinToString { it.name }}")

            if (frames.isNullOrEmpty()) {
                val error = "No frames found in ${tempImageDir.absolutePath}"
                Log.e("VideoProcessor", error)
                callback?.onFailure(error)
                return
            }

            Log.d("VideoProcessor", "Found ${frames.size} frames to process")

            // Decode first frame to get dimensions
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val firstFrame = BitmapFactory.decodeFile(frames[0].path, options)
            val width = firstFrame.width
            val height = firstFrame.height
            firstFrame.recycle()
            Log.d("VideoProcessor", "Video dimensions: ${width}x$height")

            // Get all frames as bitmaps
            val bitmaps = frames.mapNotNull { file ->
                try {
                    BitmapFactory.decodeFile(file.path, options)?.also { bitmap ->
                        Log.d("TestSample", "Loaded bitmap ${file.name} with size: ${bitmap.width}x${bitmap.height}")
                        if (bitmap.config != Bitmap.Config.ARGB_8888) {
                            Log.w("VideoProcessor", "Bitmap format is ${bitmap.config}, converting to ARGB_8888")
                            return@mapNotNull bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VideoProcessor", "Error decoding frame ${file.name}", e)
                    null
                }
            }

            val outputDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "esp32_videos"
            ).apply {
                if (!exists()) {
                    Log.d("VideoProcessor", "Creating output directory: $absolutePath")
                    mkdirs()
                }
            }

            val outputFile = File(outputDir, "SafeHer_${System.currentTimeMillis()}.mp4")
            if (outputFile.exists()) {
                outputFile.delete()
                outputFile.createNewFile()
            }
            Log.d("VideoProcessor", "Output video file: ${outputFile.absolutePath}")

            encodeVideoWithMediaCodec(
                frames = bitmaps,
                outputFile = outputFile,
                width = width,
                height = height,
                frameRate = frameRate,
                callback = callback
            )
        } catch (e: Exception) {
            val error = "Conversion setup failed: ${e.message}"
            Log.e("VideoProcessor", error, e)
            callback?.onFailure(error)
        }
    }

    private fun encodeVideoWithMediaCodec(
        frames: List<Bitmap>,
        outputFile: File,
        width: Int,
        height: Int,
        frameRate: Int,
        callback: ConversionCallback?
    ) {
        if(isEncoding){
            Log.d("VideoProcessor", "Encoding is already in progress")
            return
        }
        isEncoding = true
        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null
        var muxerStarted = false
        var trackIndex = -1
        val timeoutUs = 10_000L
        val bufferInfo = BufferInfo()

        try {
            Log.d("VideoProcessor", "Initializing MediaMuxer on path: ${outputFile.path}")
            muxer = MediaMuxer(outputFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val codecName = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                .findEncoderForFormat(MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, width, height))

            val codecInfo = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                .codecInfos
                .firstOrNull { it.name == codecName && it.isEncoder }

            val supportedFormats = codecInfo
                ?.getCapabilitiesForType(MIMETYPE_VIDEO_AVC)
                ?.colorFormats
                ?.toList()
                ?: emptyList()
            Log.d("VideoProcessor", "Supported color formats: ${supportedFormats.joinToString()}")

            val format = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, width, height).apply {
                val preferredFormat = when {
                    supportedFormats.contains(21) -> 21  // YUV420 SemiPlanar
                    supportedFormats.contains(19) -> 19  // YUV420 Planar
                    else -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                }

                setInteger(MediaFormat.KEY_COLOR_FORMAT, preferredFormat)
                setInteger(MediaFormat.KEY_BIT_RATE, width * height * 3 * frameRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_LIMITED)
                setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT709)
                setInteger(MediaFormat.KEY_COLOR_TRANSFER, 3)
            }

            Log.d("VideoProcessor", "Creating and configuring encoder")
            encoder = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
            Log.d("VideoProcessor", "Encoder started successfully")

            Log.d("VideoProcessor", "Processing ${frames.size} frames")
            frames.forEachIndexed { index, bitmap ->
                try {
                    Log.v("VideoProcessor", "Processing frame $index")
                    val yuvData = convertBitmapToYUV420(bitmap, width, height)
                    Log.v("VideoProcessor", "Converted frame $index to YUV420")

                    val inputBufferIndex = encoder.dequeueInputBuffer(timeoutUs)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(yuvData)
                        encoder.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            yuvData.size,
                            index * 1_000_000L / frameRate,
                            0
                        )
                        Log.v("VideoProcessor", "Queued frame $index for encoding")
                    } else {
                        Log.w("VideoProcessor", "No input buffer available for frame $index")
                    }

                    while (true) {
                        val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                        when {
                            encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                            encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                if (muxerStarted) throw RuntimeException("Format changed after muxer started")
                                trackIndex = muxer.addTrack(encoder.outputFormat)
                                muxer.start()
                                muxerStarted = true
                                Log.d("VideoProcessor", "Muxer started with track index $trackIndex")
                            }
                            encoderStatus >= 0 -> {
                                val outputBuffer = encoder.getOutputBuffer(encoderStatus)
                                if (outputBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                                    Log.v("VideoProcessor", "Wrote frame $index (size: ${bufferInfo.size})")
                                }
                                encoder.releaseOutputBuffer(encoderStatus, false)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("VideoProcessor", "Failed processing frame $index", e)
                    callback?.onFailure("Encoding failed at frame $index: ${e.message}")
                    return
                }
            }

            // Signal end of stream
            Log.d("VideoProcessor", "Signaling end of stream")
            val inputBufferIndex = encoder.dequeueInputBuffer(timeoutUs)
            if (inputBufferIndex >= 0) {
                encoder.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    0,
                    0,
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }

            // Drain remaining output
            while (true) {
                val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                when {
                    encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                    encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxerStarted) throw RuntimeException("Format changed after muxer started")
                        trackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    encoderStatus >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(encoderStatus)
                        if (outputBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            Log.d("VideoProcessor", "End of stream reached")
                            break
                        }
                    }
                }
            }

            Log.d("VideoProcessor", "Video encoding completed successfully")
            callback?.onSuccess(outputFile)

        } catch (e: Exception) {
            Log.e("VideoProcessor", "Encoding failed", e)
            outputFile.delete()
            callback?.onFailure("Encoding failed: ${e.message}")
        } finally {
            try {
                isEncoding = false
                Log.d("VideoProcessor", "Releasing resources, isEncoding: $isEncoding")
                encoder?.stop()
                encoder?.release()
                muxer?.stop()
                muxer?.release()
                Log.d("VideoProcessor", "Released encoder and muxer resources")
            } catch (e: Exception) {
                Log.e("VideoProcessor", "Error releasing resources", e)
            }
        }
    }


    private fun convertBitmapToYUV420(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        val yuv = ByteArray(width * height * 3 / 2)
        var yIndex = 0
        var uvIndex = width * height

        for (j in 0 until height) {
            for (i in 0 until width) {
                val rgb = argb[j * width + i]

                val r = (rgb shr 16) and 0xff
                val g = (rgb shr 8) and 0xff
                val b = rgb and 0xff

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv[yIndex++] = y.coerceIn(0, 255).toByte()
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                    yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }

        return yuv
    }

    fun saveToPublicStorage(sourceFile: File, callback: (Uri?) -> Unit) {
        if (!sourceFile.exists()) {
            Log.e("VideoProcessor", "Source file does not exist: ${sourceFile.absolutePath}")
            callback(null)
            return
        }
        beforeAll = true

        Log.d("VideoProcessor", "Attempting to save video to public storage")

        val resolver = context.contentResolver
        val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val videoName = "SafeHer_${System.currentTimeMillis()}.mp4"
        val relativeLocation = "${Environment.DIRECTORY_MOVIES}/ESP32_Videos"

        Log.d("VideoProcessor", "Saving video to $relativeLocation/$videoName")

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, videoName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            put(MediaStore.Video.Media.RELATIVE_PATH, relativeLocation)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        Log.d("VideoProcessor", "Inserting video into MediaStore")
        val uri = resolver.insert(videoCollection, contentValues) ?: run {
            Log.e("VideoProcessor", "Failed to create MediaStore entry")
            callback(null)
            return
        }

        try {
            Log.d("VideoProcessor", "Writing video file content")
            resolver.openOutputStream(uri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            Log.d("VideoProcessor", "Finalizing MediaStore entry")
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Log.d("VideoProcessor", "Video saved successfully to $uri")
            callback(uri)
        } catch (e: Exception) {
            Log.e("VideoProcessor", "Failed to save video to public storage", e)
            resolver.delete(uri, null, null)
            callback(null)
        }
    }

    /*internal fun cleanupTempFiles() {
        Log.d("VideoProcessor", "Cleaning up temporary files")
        tempImageDir.listFiles()?.forEach {
            try {
                if (it.delete()) {
                    Log.d("VideoProcessor", "Deleted temporary file: ${it.name}")
                } else {
                    Log.w("VideoProcessor", "Failed to delete temporary file: ${it.name}")
                }
            } catch (e: Exception) {
                Log.e("VideoProcessor", "Error deleting temporary file ${it.name}", e)
            }
        }
    }*/

    fun getFrameCount(): Int {
        val count = tempImageDir.listFiles()?.count {
            it.name.startsWith("frame_") && it.name.endsWith(".jpg")
        } ?: 0
        Log.d("VideoProcessor", "Current frame count: $count")
        return count
    }
}