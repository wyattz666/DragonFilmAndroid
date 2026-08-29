package com.dragonfilm.app.data.storage

import android.content.Context
import android.util.Log
import com.dragonfilm.app.data.api.ApiClient
import com.dragonfilm.app.util.DeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AnalyticsManager(
    private val context: Context,
    private val authManager: AuthManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = context.getSharedPreferences("df_analytics_prefs", Context.MODE_PRIVATE)

    fun trackAppOpen() {
        val now = System.currentTimeMillis()
        val lastPing = prefs.getLong("last_telemetry_ping", 0L)
        // Rate limit: Only send 1 telemetry ping per hour per device to conserve D1 write quota
        if (now - lastPing < 60 * 60 * 1000L) {
            return
        }

        prefs.edit().putLong("last_telemetry_ping", now).apply()

        trackEvent(
            pageUrl = "dragonfilm://android/app-launch",
            pageTitle = "Khởi động ứng dụng DragonFilm Android"
        )
    }

    private fun trackEvent(pageUrl: String, pageTitle: String) {
        scope.launch {
            try {
                val token = authManager.token.value
                val authHeader = if (!token.isNullOrEmpty()) "Bearer $token" else null

                val payload = mapOf(
                    "page_url" to pageUrl,
                    "page_title" to pageTitle,
                    "screen_res" to DeviceInfo.getScreenSummary(context),
                    "language" to Locale.getDefault().toLanguageTag(),
                    "device_model" to DeviceInfo.deviceName,
                    "device_brand" to DeviceInfo.brand,
                    "os_version" to "Android ${DeviceInfo.osVersion} (API ${DeviceInfo.sdkInt})",
                    "app_version" to DeviceInfo.appVersion,
                    "platform" to "android"
                )

                ApiClient.service.trackVisitor(
                    token = authHeader,
                    body = payload
                )
            } catch (e: Exception) {
                Log.d("AnalyticsManager", "Telemetry ping ignored: ${e.message}")
            }
        }
    }
}
