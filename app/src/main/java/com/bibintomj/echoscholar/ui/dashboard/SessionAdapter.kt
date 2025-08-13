package com.bibintomj.echoscholar.ui.dashboard

import android.view.LayoutInflater
import android.view.View
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
    private val onSessionClick: (String) -> Unit,
    private val onSelectionChanged: (count: Int) -> Unit
) : ListAdapter<SessionItem, SessionAdapter.ViewHolder>(DiffCallback()) {

    private val selectedIds = mutableSetOf<String>()
    var selectionMode: Boolean = false
        private set

    fun startSelection(id: String) {
        if (!selectionMode) selectionMode = true
        toggleSelection(id)
    }

    fun toggleSelection(id: String) {
        if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
        onSelectionChanged(selectedIds.size)
        currentList.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { notifyItemChanged(it) }
        if (selectedIds.isEmpty()) exitSelection()
    }

    fun exitSelection() {
        selectionMode = false
        selectedIds.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun getSelectedIds(): List<String> = selectedIds.toList()

    inner class ViewHolder(private val binding: ItemSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(session: SessionItem) {
            val isSelected = selectedIds.contains(session.id)
            binding.sessionTitle.text = session.title ?: "Untitled Session"
            binding.sessionTime.text = session.getTimeAgo()

            binding.check.visibility = if (selectionMode) View.VISIBLE else View.GONE
            binding.arrowIcon.visibility = if (selectionMode) View.INVISIBLE else View.VISIBLE
            binding.check.isChecked = isSelected
            binding.root.isActivated = isSelected

            binding.root.setOnLongClickListener {
                startSelection(session.id); true
            }
            binding.root.setOnClickListener {
                if (selectionMode) toggleSelection(session.id) else onSessionClick(session.id)
            }
            binding.playIcon.setOnClickListener {
                if (selectionMode) toggleSelection(session.id) else onSessionClick(session.id)
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

// Helper extension to format session time
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
