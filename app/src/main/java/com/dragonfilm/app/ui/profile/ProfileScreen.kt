package com.dragonfilm.app.ui.profile

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dragonfilm.app.data.model.User
import com.dragonfilm.app.data.storage.AuthManager
import com.dragonfilm.app.data.storage.CloudSync
import com.dragonfilm.app.data.storage.LocalStore
import com.dragonfilm.app.ui.components.Badge
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.glassCard
import kotlinx.coroutines.launch

@Composable
fun UserAvatarView(
    avatarUrl: String?,
    username: String,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    textSize: TextUnit = 28.sp
) {
    val bitmap = remember(avatarUrl) {
        if (!avatarUrl.isNullOrEmpty() && avatarUrl.startsWith("data:image")) {
            try {
                val base64Data = avatarUrl.substringAfter("base64,")
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(DFColor.Bg3)
            .border(1.5.dp, DFColor.GlassBorderGradient, CircleShape)
            .shadow(8.dp, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = username,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!avatarUrl.isNullOrEmpty() && (avatarUrl.startsWith("http") || avatarUrl.startsWith("/"))) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = username,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = username.take(1).uppercase().ifEmpty { "?" },
                style = DFTypography.largeTitle.copy(color = DFColor.Gold, fontSize = textSize)
            )
        }
    }
}

@Composable
fun ProfileScreen(
    authManager: AuthManager,
    localStore: LocalStore,
    cloudSync: CloudSync? = null
) {
    val currentUser by authManager.currentUser.collectAsState()
    var showAuthDialog by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    val historyItems by localStore.historyFlow.collectAsState()
    val watchLaterMovies by localStore.watchLaterFlow.collectAsState()
    val likedMovies by localStore.likedFlow.collectAsState()

    val scope = rememberCoroutineScope()

    // Automatically sync and refresh profile on tab load
    LaunchedEffect(currentUser) {
        if (currentUser != null && cloudSync != null) {
            cloudSync.sync()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DFColor.Bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
            }

            if (currentUser != null) {
                // User Account Card
                item {
                    UserAccountCard(user = currentUser!!)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Stats Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatTile(label = "Đã xem", count = "${historyItems.size}", modifier = Modifier.weight(1f))
                        StatTile(label = "Xem sau", count = "${watchLaterMovies.size}", modifier = Modifier.weight(1f))
                        StatTile(label = "Yêu thích", count = "${likedMovies.size}", modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Actions List
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = DFRadius.lg)
                    ) {
                        ActionRowItem(
                            icon = Icons.Default.Sync,
                            title = "Đồng bộ dữ liệu đám mây",
                            subtitle = if (isSyncing) "Đang đồng bộ..." else syncMessage ?: "Cập nhật lịch sử & yêu thích",
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    val ok = cloudSync?.sync() ?: false
                                    syncMessage = if (ok) "Đã đồng bộ thành công!" else "Đồng bộ hoàn tất."
                                    isSyncing = false
                                }
                            }
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)

                        ActionRowItem(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Đăng xuất tài khoản",
                            titleColor = DFColor.Crimson,
                            iconColor = DFColor.Crimson,
                            onClick = {
                                authManager.logout()
                            }
                        )
                    }
                }
            } else {
                // Guest Card
                item {
                    GuestCard(
                        onLoginClick = { showAuthDialog = true }
                    )
                }
            }

            // App Info Section
            item {
                Spacer(modifier = Modifier.height(28.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DragonFilm for Android v1.0.1",
                        style = DFTypography.caption.copy(color = DFColor.TextMuted, fontSize = 11.5.sp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Phát triển bởi DragonFilm Team",
                        style = DFTypography.small.copy(color = DFColor.TextMuted, fontSize = 10.sp)
                    )
                }
            }
        }
    }

    if (showAuthDialog) {
        AuthDialog(
            authManager = authManager,
            onDismiss = {
                showAuthDialog = false
                scope.launch {
                    cloudSync?.sync()
                }
            }
        )
    }
}

@Composable
private fun UserAccountCard(user: User) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = DFRadius.xl)
            .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Dynamic Avatar Circle (Image or Initial)
        UserAvatarView(
            avatarUrl = user.avatarUrl,
            username = user.username,
            size = 76.dp,
            textSize = 28.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = user.username,
            style = DFTypography.title,
            color = DFColor.Text
        )

        if (user.email.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = user.email,
                style = DFTypography.caption.copy(color = DFColor.TextMuted)
            )
        }

        if (user.isAdmin) {
            Spacer(modifier = Modifier.height(6.dp))
            Badge(text = "VIP ADMIN", backgroundColor = DFColor.Gold.copy(alpha = 0.25f), textColor = DFColor.Gold)
        }
    }
}

@Composable
private fun GuestCard(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = DFRadius.xl)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = DFColor.Gold,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Tài khoản khách",
            style = DFTypography.title,
            color = DFColor.Text
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Đăng nhập để đồng bộ lịch sử xem và phim yêu thích trên mọi thiết bị.",
            style = DFTypography.body.copy(fontSize = 12.5.sp),
            color = DFColor.TextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(DFColor.GoldGradient)
                .clickable { onLoginClick() }
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Đăng nhập / Đăng ký",
                style = DFTypography.headline.copy(color = Color(0xFF07080A), fontSize = 13.5.sp)
            )
        }
    }
}

@Composable
private fun StatTile(label: String, count: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .glassCard(cornerRadius = DFRadius.lg)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            style = DFTypography.largeTitle.copy(color = DFColor.Gold, fontSize = 20.sp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = DFTypography.small.copy(color = DFColor.TextMuted, fontSize = 11.sp)
        )
    }
}

@Composable
private fun ActionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color = DFColor.Text,
    iconColor: Color = DFColor.Gold,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DFTypography.headline.copy(color = titleColor, fontSize = 13.5.sp)
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = DFTypography.small.copy(color = DFColor.TextMuted, fontSize = 11.sp)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(16.dp)
        )
    }
}
