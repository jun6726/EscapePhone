package com.example.escapephone.core.services

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PlaytestAnalyticsUploader {
    val isConfigured: Boolean
    suspend fun upload(jsonPayload: String): Boolean
}

object NoOpPlaytestAnalyticsUploader : PlaytestAnalyticsUploader {
    override val isConfigured = false
    override suspend fun upload(jsonPayload: String) = false
}

class HttpPlaytestAnalyticsUploader(private val endpointUrl: String) : PlaytestAnalyticsUploader {
    override val isConfigured: Boolean get() = endpointUrl.startsWith("https://")

    override suspend fun upload(jsonPayload: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        runCatching {
            (URL(endpointUrl).openConnection() as HttpURLConnection).run {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                outputStream.use { it.write(jsonPayload.toByteArray(Charsets.UTF_8)) }
                val accepted = responseCode in 200..299
                disconnect()
                accepted
            }
        }.getOrDefault(false)
    }
}
