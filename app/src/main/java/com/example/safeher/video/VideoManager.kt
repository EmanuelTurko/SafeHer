package com.example.safeher.video

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.*
import android.media.MediaCodec.BufferInfo
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import androidx.core.net.toUri

class VideoManager(private val context: Context) {
    private val tempImageDir by lazy {
        File(context.getExternalFilesDir(null), "esp32_images").apply {
            if (!exists()) mkdirs()
        }
    }

    interface ConversionCallback {
        fun onSuccess(outputFile: File)
        fun onFailure(error: String)
    }

    fun saveImageData(data: ByteArray, index: Int): Boolean {
        return try {
            val imageFile = File(tempImageDir, "frame_${index.toString().padStart(4, '0')}.jpg")
            FileOutputStream(imageFile).use { it.write(data) }

            // Validate image dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.e("VideoProcessor", "Invalid JPEG dimensions for frame $index")
                imageFile.delete()
                false
            } else {
                Log.d("VideoProcessor", "Saved frame $index (${data.size} bytes, ${options.outWidth}x${options.outHeight})")
                true
            }
        } catch (e: Exception) {
            Log.e("VideoProcessor", "Error saving frame $index", e)
            false
        }
    }

    fun convertToVideo(frameRate: Int = 12, callback: ConversionCallback? = null) {
        try {
            val frames = tempImageDir.listFiles { file ->
                file.name.matches(Regex("frame_\\d{4}\\.jpg"))
            }?.sortedBy { it.name }

            if (frames.isNullOrEmpty()) {
                callback?.onFailure("No frames found in ${tempImageDir.absolutePath}")
                return
            }

            // Validate frames
            frames.forEachIndexed { index, file ->
                if (!file.exists() || file.length() == 0L) {
                    callback?.onFailure("Missing or empty frame: ${file.name}")
                    return
                }
            }

            // Setup output directory
            val outputDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "esp32_videos"
            ).apply {
                if (!exists() && !mkdirs()) {
                    callback?.onFailure("Failed to create output directory")
                    return
                }
            }

            val outputFile = File(outputDir, "output_${System.currentTimeMillis()}.mp4").apply {
                if (exists() && !delete()) {
                    callback?.onFailure("Couldn't clear existing output file")
                    return
                }
            }

            // Get dimensions from first frame
            val firstFrame = BitmapFactory.decodeFile(frames[0].path)
            val width = firstFrame.width
            val height = firstFrame.height
            firstFrame.recycle()

            // Start MediaCodec encoding
            encodeWithMediaCodec(
                frames = frames.map { BitmapFactory.decodeFile(it.path) },
                outputFile = outputFile,
                width = width,
                height = height,
                frameRate = frameRate,
                callback = callback
            )
        } catch (e: Exception) {
            Log.e("VideoProcessor", "Conversion setup failed", e)
            callback?.onFailure("Setup error: ${e.message}")
        }
    }

    private fun encodeWithMediaCodec(
        frames: List<Bitmap>,
        outputFile: File,
        width: Int,
        height: Int,
        frameRate: Int,
        callback: ConversionCallback?
    ) {
        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null
        var trackIndex = -1
        var muxerStarted = false

        try {
            // Configure MediaCodec encoder
            muxer = MediaMuxer(outputFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val format = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 2_000_000) // 2 Mbps
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) // Keyframe every 1 second
            }

            encoder = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val bufferInfo = BufferInfo()
            val timeoutUs = 10_000L // 10ms timeout

            // Process each frame
            frames.forEachIndexed { index, bitmap ->
                // Convert bitmap to YUV420 format
                val yuvData = convertBitmapToYUV420(bitmap, width, height)
                bitmap.recycle()

                // Get input buffer and feed frame data
                val inputBufferIndex = encoder.dequeueInputBuffer(timeoutUs)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                    inputBuffer?.put(yuvData)

                    val presentationTimeUs = index * 1_000_000L / frameRate
                    encoder.queueInputBuffer(
                        inputBufferIndex,
                        0,
                        yuvData.size,
                        presentationTimeUs,
                        0
                    )
                }

                // Process encoder output
                while (true) {
                    val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    when {
                        encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                        encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // Format changed - prepare muxer
                            if (muxerStarted) {
                                throw RuntimeException("Format changed twice")
                            }
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        encoderStatus >= 0 -> {
                            val outputBuffer = encoder.getOutputBuffer(encoderStatus)
                                ?: throw RuntimeException("No output buffer")

                            if (bufferInfo.size > 0 && muxerStarted) {
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                            }
                            encoder.releaseOutputBuffer(encoderStatus, false)
                        }
                    }
                }
            }

            // Signal end of input stream
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

            // Process remaining output
            while (true) {
                val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
                when {
                    encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                    encoderStatus >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(encoderStatus)
                        if (bufferInfo.size > 0 && outputBuffer != null && muxerStarted) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break
                        }
                    }
                }
            }

            callback?.onSuccess(outputFile)
        } catch (e: Exception) {
            Log.e("VideoProcessor", "MediaCodec encoding failed", e)
            outputFile.delete()
            callback?.onFailure("Encoding failed: ${e.message}")
        } finally {
            frames.forEach { it.recycle() }
            try {
                encoder?.stop()
                encoder?.release()
            } catch (e: Exception) {
                Log.e("VideoProcessor", "Error stopping encoder", e)
            }
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (e: Exception) {
                Log.e("VideoProcessor", "Error stopping muxer", e)
            }
        }
    }

    private fun convertBitmapToYUV420(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        val yuv = ByteArray(width * height * 3 / 2) // YUV420 (1.5 bytes per pixel)
        val frameSize = width * height

        var yIndex = 0
        var uIndex = frameSize
        var vIndex = frameSize + frameSize / 4

        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = argb[j * width + i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Y component (luminance)
                yuv[yIndex++] = ((66 * r + 129 * g + 25 * b + 128) shr 8 + 16).toByte()

                // U and V components (chrominance, subsampled)
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uIndex++] = ((-38 * r - 74 * g + 112 * b + 128) shr 8 + 128).toByte()
                    yuv[vIndex++] = ((112 * r - 94 * g - 18 * b + 128) shr 8 + 128).toByte()
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

        val uri = resolver.insert(videoCollection, contentValues) ?: run {
            Log.e("VideoProcessor", "Failed to create MediaStore entry")
            callback(null)
            return
        }

        try {
            resolver.openOutputStream(uri)?.use { outStream ->
                sourceFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            // Finalize the media entry
            contentValues.clear()
            contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            // Trigger media scan
            MediaScannerConnection.scanFile(
                context,
                arrayOf(uri.toString().toUri().path),
                arrayOf("video/mp4"),
                null
            )

            callback(uri)
        } catch (e: Exception) {
            Log.e("VideoProcessor", "Error writing video file", e)
            resolver.delete(uri, null, null)
            callback(null)
        }
    }

    internal fun cleanupTempFiles() {
        tempImageDir.listFiles()?.forEach {
            try {
                if (!it.delete()) {
                    Log.w("VideoProcessor", "Failed to delete ${it.name}")
                }
            } catch (e: Exception) {
                Log.e("VideoProcessor", "Error deleting ${it.name}", e)
            }
        }
    }

    fun getFrameCount(): Int {
        return tempImageDir.listFiles()?.count {
            it.name.startsWith("frame_") && it.name.endsWith(".jpg")
        } ?: 0
    }
}