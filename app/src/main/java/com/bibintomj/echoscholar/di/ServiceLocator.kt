package com.bibintomj.echoscholar.di

import com.bibintomj.echoscholar.BuildConfig
import com.bibintomj.echoscholar.data.repository.SessionRepository
import com.bibintomj.echoscholar.data.repository.SessionRepositoryImpl
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

object ServiceLocator {
    private val okHttp by lazy { OkHttpClient.Builder().build() }
    private val json by lazy { Json { ignoreUnknownKeys = true } }

    val sessionRepository: SessionRepository by lazy {
        SessionRepositoryImpl(okHttp, json)
    }

    // If you don't have BuildConfig.API_BASE_URL, hardcode the full URL here
    val saveSessionUrl: String by lazy {
        // e.g. BuildConfig.API_BASE_URL + "/api/session/save"
        "${com.bibintomj.echoscholar.BuildConfig.API_BASE_URL}/api/savesession"
    }
    // ServiceLocator.kt
    val listSessionsUrls: List<String> by lazy {
        listOf(
            "${BuildConfig.API_BASE_URL}/api/sessions",
            "${BuildConfig.API_BASE_URL}/api/session"
        )
    }

}
