package com.bibintomj.echoscholar.network

interface NetworkRequest {
    val baseURL: String
    val path: String
    val method: String
    val headers: Map<String, String>?
    val query: Map<String, String>?
    val bodyJson: String?
}
