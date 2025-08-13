package com.bibintomj.echoscholar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    @SerialName("question") val question: String,
    @SerialName("userId")   val userId: String
)

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)
