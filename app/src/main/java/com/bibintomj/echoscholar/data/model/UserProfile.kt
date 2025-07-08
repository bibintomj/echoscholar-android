package com.bibintomj.echoscholar.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val name: String,
    val avatarUrl: String? = null
)
