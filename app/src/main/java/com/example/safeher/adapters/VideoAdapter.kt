package com.example.safeher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import com.example.safeher.R
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import android.util.Log
import android.widget.ImageView
import android.widget.Toast

class VideoAdapter(private val context: Context, private var videoFiles: List<File>) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }



    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoFiles[position]

        val displayName = video.name
            .removeSuffix(".mp4")
            .replaceAfter(" ", "") +                 // Keeps only the time part as-is
                video.name
                    .removeSuffix(".mp4")
                    .substringAfter(" ")
                    .replace(":", "/")

        holder.dateText.text = displayName

        val fileSizeInBytes = video.length()
        val readableSize = getReadableFileSize(fileSizeInBytes)
        holder.addressText.text = readableSize

        holder.playButton.setOnClickListener {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                video
            )
            Log.d("VideoProcessor", "Video URI: $uri")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "video/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(intent)
        }
        holder.deleteButton.setOnClickListener {
            val file = videoFiles[position]
            if(file.exists()){
                if(file.delete()){
                    Toast.makeText(context, "File deleted successfully", Toast.LENGTH_SHORT).show()
                    videoFiles = videoFiles.toMutableList().also { it.removeAt(position) }
                    notifyItemRemoved(position)
                } else {
                    Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun getItemCount(): Int = videoFiles.size

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val addressText: TextView = itemView.findViewById(R.id.addressText)
        val playButton: ImageView = itemView.findViewById(R.id.playButtonCard)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButtonCard)


        init {
            playButton.setOnClickListener {
                //TODO : Implement video playback functionality
            }
        }
    }
    fun getReadableFileSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size >= gb -> String.format("%.2f GB", size.toFloat() / gb)
            size >= mb -> String.format("%.2f MB", size.toFloat() / mb)
            size >= kb -> String.format("%.2f KB", size.toFloat() / kb)
            else -> "$size B"
        }
    }
}