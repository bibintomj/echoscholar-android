// SessionResponse.kt
package com.bibintomj.echoscholar.data.model

import kotlinx.serialization.Serializable

object ContentBlockListSerializer : FlexibleListSerializer<ContentBlock>(ContentBlock.serializer())

@Serializable
data class SessionResponse(val sessions: List<SessionAPIModel>)

@Serializable
data class SessionAPIModel(
    val id: String,
    val created_on: String = "",
    val user_id: String?,
    val target_language: String?,
    val audio_file_path: String?,
    val audio_signed_url: String? = null,
//    val translations: List<ContentBlock>? = null,
//    val transcriptions: List<ContentBlock>? = null,
//    val summaries: List<ContentBlock>? = null
    @Serializable(with = ContentBlockListSerializer::class)
    val translations: List<ContentBlock>? = null,

    @Serializable(with = ContentBlockListSerializer::class)
    val transcriptions: List<ContentBlock>? = null,

    @Serializable(with = ContentBlockListSerializer::class)
    val summaries: List<ContentBlock>? = null

)


@Serializable
data class ContentBlock(
    val id: String,
    val content: String,
    val created_on: String
)

