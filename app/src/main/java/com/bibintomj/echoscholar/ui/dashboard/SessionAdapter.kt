package com.bibintomj.echoscholar.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bibintomj.echoscholar.data.model.SessionItem
import com.bibintomj.echoscholar.databinding.ItemSessionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SessionAdapter(
    private val onSessionClick: (SessionItem) -> Unit
) : ListAdapter<SessionItem, SessionAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(session: SessionItem) {
            binding.sessionTitle.text = session.title ?: "Untitled Session"
            binding.sessionTime.text = session.getTimeAgo()

            binding.playIcon.setOnClickListener {
                onSessionClick(session)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<SessionItem>() {
        override fun areItemsTheSame(oldItem: SessionItem, newItem: SessionItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: SessionItem, newItem: SessionItem): Boolean =
            oldItem == newItem
    }
}

fun SessionItem.getTimeAgo(): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    return try {
        val createdDate = format.parse(this.createdOn) ?: return "unknown time"
        val now = Date()
        val diff = now.time - createdDate.time

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            days == 1L -> "yesterday"
            else -> "$days days ago"
        }
    } catch (e: Exception) {
        "unknown time"
    }
}
