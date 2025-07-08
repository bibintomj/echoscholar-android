package com.bibintomj.echoscholar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SessionItem(
    val id: String,
    val title: String?,
    val createdOn: String, // you’ll convert this to "just now", "yesterday", etc. in the adapter
    val userId: String?,
    val targetLanguage: String?,
    val audioFilePath: String?
)
