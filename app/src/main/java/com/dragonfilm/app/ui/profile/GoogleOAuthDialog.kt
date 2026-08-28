package com.dragonfilm.app.ui.profile

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dragonfilm.app.data.api.ApiClient
import com.dragonfilm.app.data.storage.AuthManager
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFTypography
import kotlinx.coroutines.launch
import java.net.URLDecoder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleOAuthDialog(
    authManager: AuthManager,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var tokenHandled by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val startUrl = "https://dragonfilm.pages.dev/api/auth/oauth/google/start?provider=google&returnTo=%2Findex.html"

    fun handleAuthToken(rawToken: String) {
        if (tokenHandled) return
        tokenHandled = true
        val token = try {
            URLDecoder.decode(rawToken, "UTF-8")
        } catch (_: Exception) {
            rawToken
        }

        scope.launch {
            try {
                val resp = ApiClient.service.getProfile("Bearer $token")
                if (resp.ok && resp.user != null) {
                    authManager.setSession(token, resp.user)
                    onSuccess()
                    onDismiss()
                } else {
                    onError(resp.error ?: "Không lấy được thông tin tài khoản Google.")
                    onDismiss()
                }
            } catch (e: Exception) {
                onError("Lỗi kết nối máy chủ khi đăng nhập Google.")
                onDismiss()
            }
        }
    }

    fun checkUrl(url: String?): Boolean {
        if (url.isNullOrEmpty() || tokenHandled) return false

        try {
            val uri = Uri.parse(url)
            val fragment = uri.fragment ?: ""

            if (fragment.isNotEmpty()) {
                val params = fragment.split("&").associate { param ->
                    val parts = param.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
                }

                val err = params["oauth_error"] ?: params["error_description"] ?: params["error"]
                if (!err.isNullOrEmpty()) {
                    tokenHandled = true
                    onError(try { URLDecoder.decode(err, "UTF-8") } catch (_: Exception) { err })
                    onDismiss()
                    return true
                }

                val token = params["oauth_token"] ?: params["token"]
                if (!token.isNullOrEmpty()) {
                    handleAuthToken(token)
                    return true
                }
            }

            // Check query params as fallback
            val queryToken = uri.getQueryParameter("oauth_token") ?: uri.getQueryParameter("token")
            if (!queryToken.isNullOrEmpty()) {
                handleAuthToken(queryToken)
                return true
            }

            val queryError = uri.getQueryParameter("oauth_error") ?: uri.getQueryParameter("error")
            if (!queryError.isNullOrEmpty()) {
                tokenHandled = true
                onError(queryError)
                onDismiss()
                return true
            }
        } catch (_: Exception) {}

        return false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            color = DFColor.Bg,
            shape = RoundedCornerShape(DFRadius.xl)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DFColor.Bg2)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đăng Nhập với Google",
                        style = DFTypography.headline,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = DFColor.TextMuted
                        )
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = DFColor.Gold,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }

                // WebView Container
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                                }

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress / 100f
                                        isLoading = newProgress < 100
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                        if (checkUrl(url)) {
                                            view?.stopLoading()
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val reqUrl = request?.url?.toString()
                                        if (checkUrl(reqUrl)) {
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        if (checkUrl(url)) return

                                        // Evaluate JS to check localStorage token if on dragonfilm domain
                                        if (url?.contains("dragonfilm") == true) {
                                            evaluateJavascript(
                                                "(function() { try { return localStorage.getItem('dragonfilm_auth_token') || ''; } catch(e) { return ''; } })()"
                                            ) { storedToken ->
                                                val cleaned = storedToken?.replace("\"", "")?.trim()
                                                if (!cleaned.isNullOrEmpty() && cleaned != "null") {
                                                    handleAuthToken(cleaned)
                                                }
                                            }
                                        }
                                    }
                                }

                                loadUrl(startUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
