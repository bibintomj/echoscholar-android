package com.bibintomj.echoscholar.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bibintomj.echoscholar.R
import com.bibintomj.echoscholar.data.model.ChatMessage

class MessageAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
                oldItem === newItem

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
                oldItem.text == newItem.text && oldItem.isUser == newItem.isUser
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_USER -> UserVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_user_message, parent, false)
            )
            else -> BotVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_assistant_message, parent, false)
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is UserVH -> holder.bind(msg.text)
            is BotVH -> holder.bind(msg.text)
        }
    }

    class UserVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.messageText)
        fun bind(value: String) { text.text = value }
    }

    class BotVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.messageText)
        fun bind(value: String) { text.text = value }
    }
}
