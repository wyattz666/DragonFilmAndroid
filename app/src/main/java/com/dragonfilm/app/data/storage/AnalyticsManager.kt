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

    fun trackAppOpen() {
        trackEvent(
            pageUrl = "dragonfilm://android/app-launch",
            pageTitle = "Khởi động ứng dụng DragonFilm Android"
        )
    }

    fun trackScreen(screenName: String, title: String? = null) {
        trackEvent(
            pageUrl = "dragonfilm://android/screen/$screenName",
            pageTitle = title ?: "DragonFilm - $screenName"
        )
    }

    fun trackMovieView(slug: String, movieName: String) {
        trackEvent(
            pageUrl = "dragonfilm://android/movie/$slug",
            pageTitle = "Xem chi tiết: $movieName"
        )
    }

    fun trackWatchEpisode(slug: String, movieName: String, epName: String, server: String) {
        trackEvent(
            pageUrl = "dragonfilm://android/player/$slug",
            pageTitle = "Đang xem: $movieName ($epName - $server)"
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
                // Silently ignore telemetry failure to avoid interrupting UX
                Log.d("AnalyticsManager", "Telemetry ping failed: ${e.message}")
            }
        }
    }
}
