package com.dragonfilm.app.util

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object StreamResolver {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun resolve(rawUrl: String): String = withContext(Dispatchers.IO) {
        if (rawUrl.isEmpty()) return@withContext rawUrl

        // Direct m3u8
        if (rawUrl.contains(".m3u8")) {
            return@withContext rawUrl
        }

        // VSMov video to stream conversion
        if (rawUrl.contains("streamvsmov.com/video/")) {
            return@withContext rawUrl.replace("/video/", "/stream/") + "/master.m3u8"
        }

        // NguonC / streamc embed resolution
        if (rawUrl.contains("embed") || rawUrl.contains("streamc") || rawUrl.contains("nguonc")) {
            val direct = resolveNguonCEmbed(rawUrl)
            if (!direct.isNullOrEmpty()) {
                return@withContext direct
            }
        }

        return@withContext rawUrl
    }

    private fun resolveNguonCEmbed(embedUrl: String): String? {
        return try {
            val request = Request.Builder()
                .url(embedUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
                .addHeader("Referer", "https://phim.nguonc.com/")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val html = response.body?.string() ?: return null
            val pattern = Pattern.compile("data-obf=[\"']([^\"']+)[\"']")
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val b64 = matcher.group(1) ?: return null
                val decoded = String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
                val json = JSONObject(decoded)
                val sUb = json.optString("sUb")
                if (sUb.isNotEmpty()) {
                    val uri = java.net.URI(embedUrl)
                    return "${uri.scheme}://${uri.host}/$sUb"
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
