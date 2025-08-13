package com.bibintomj.echoscholar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutRequest(
    @SerialName("user_id") val userId: String,
    val email: String
)

@Serializable
data class CheckoutResponse(
    val url: String
)
