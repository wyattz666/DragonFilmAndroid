package com.dragonfilm.app.ui.player

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedPlayerView(
    url: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(0xFF000000.toInt())
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    loadsImagesAutomatically = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                }
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()

                val extraHeaders = mapOf(
                    "Referer" to "https://phim.nguonc.com/",
                    "Origin" to "https://dragonfilm.pages.dev"
                )
                loadUrl(url, extraHeaders)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                val extraHeaders = mapOf(
                    "Referer" to "https://phim.nguonc.com/",
                    "Origin" to "https://dragonfilm.pages.dev"
                )
                webView.loadUrl(url, extraHeaders)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
