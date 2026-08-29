package com.dragonfilm.app.util

import android.content.Context
import android.os.Build

object DeviceInfo {

    val manufacturer: String = Build.MANUFACTURER ?: "Unknown"
    val model: String = Build.MODEL ?: "Unknown"
    val brand: String = Build.BRAND ?: "Unknown"
    val osVersion: String = Build.VERSION.RELEASE ?: "Unknown"
    val sdkInt: Int = Build.VERSION.SDK_INT
    const val appVersion: String = "1.0.2"

    val deviceName: String
        get() = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model"
        }

    val userAgent: String
        get() = "DragonFilm-Android/$appVersion (Android $osVersion; API $sdkInt; $deviceName; ${Build.DEVICE})"

    fun getScreenSummary(context: Context): String {
        return try {
            val dm = context.resources.displayMetrics
            "${dm.widthPixels}x${dm.heightPixels} (${dm.densityDpi}dpi)"
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
