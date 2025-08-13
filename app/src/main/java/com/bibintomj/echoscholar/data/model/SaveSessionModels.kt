package com.bibintomj.echoscholar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


data class SaveSessionRequest(
    val transcription: String,
    val translation: String,
    val targetLanguage: String,
    val audioBytes: ByteArray,
    val audioFileName: String = "audio.wav",
    val mimeType: String = "audio/wav"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SaveSessionRequest

        if (transcription != other.transcription) return false
        if (translation != other.translation) return false
        if (targetLanguage != other.targetLanguage) return false
        if (!audioBytes.contentEquals(other.audioBytes)) return false
        if (audioFileName != other.audioFileName) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = transcription.hashCode()
        result = 31 * result + translation.hashCode()
        result = 31 * result + targetLanguage.hashCode()
        result = 31 * result + audioBytes.contentHashCode()
        result = 31 * result + audioFileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

@Serializable
data class SaveSessionResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("audio_file_url") val audioFileUrl: String? = null
)
