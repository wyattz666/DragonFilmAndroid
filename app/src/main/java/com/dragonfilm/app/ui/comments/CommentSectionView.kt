package com.dragonfilm.app.ui.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragonfilm.app.data.model.Comment
import com.dragonfilm.app.data.repository.MovieRepository
import com.dragonfilm.app.data.storage.AuthManager
import com.dragonfilm.app.data.storage.LocalStore
import com.dragonfilm.app.ui.components.Badge
import com.dragonfilm.app.ui.components.SectionHeader
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.glassCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun CommentSectionView(
    movieKey: String,
    movieName: String,
    title: String = "Bình luận",
    repository: MovieRepository,
    localStore: LocalStore,
    authManager: AuthManager? = null
) {
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val user = authManager?.currentUser?.value
    val token = authManager?.token?.value

    LaunchedEffect(movieKey) {
        while (isActive) {
            try {
                comments = repository.getComments(movieKey)
            } catch (_: Exception) {}
            delay(15000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DFSpacing.md)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DFSpacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionHeader(title = title, modifier = Modifier.weight(1f).padding(horizontal = 0.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(DFColor.LiveGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LIVE",
                    style = DFTypography.small.copy(color = DFColor.LiveGreen, fontSize = 10.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(DFSpacing.lg))

        // Comment Input Composer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DFSpacing.xxl)
        ) {
            if (token != null) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= 500) draft = it },
                    placeholder = {
                        Text(
                            text = "Chia sẻ cảm nhận về phim...",
                            style = DFTypography.body.copy(color = DFColor.TextMuted)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DFColor.Bg3, shape = RoundedCornerShape(DFRadius.lg)),
                    shape = RoundedCornerShape(DFRadius.lg),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DFColor.Gold,
                        unfocusedBorderColor = DFColor.Border,
                        focusedTextColor = DFColor.Text,
                        unfocusedTextColor = DFColor.Text
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${draft.length}/500",
                        style = DFTypography.small.copy(
                            color = if (draft.length >= 500) Color.Red else DFColor.TextMuted
                        )
                    )

                    val canSend = draft.trim().isNotEmpty() && !isSending
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (canSend) DFColor.Gold else DFColor.GoldDim.copy(alpha = 0.3f))
                            .clickable(enabled = canSend) {
                                scope.launch {
                                    isSending = true
                                    val newComment = repository.postComment(
                                        token = token,
                                        movieKey = movieKey,
                                        text = draft.trim(),
                                        movieName = movieName
                                    )
                                    if (newComment != null) {
                                        draft = ""
                                        comments = listOf(newComment) + comments
                                    }
                                    isSending = false
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                color = DFColor.Bg,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Gửi",
                                tint = Color(0xFF07080A),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Gửi",
                                style = DFTypography.caption.copy(color = Color(0xFF07080A), fontSize = 12.sp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Đăng nhập để gửi bình luận.",
                    style = DFTypography.caption.copy(color = DFColor.TextMuted)
                )
            }
        }

        Spacer(modifier = Modifier.height(DFSpacing.lg))

        // Comment List
        if (comments.isEmpty()) {
            Text(
                text = "Chưa có bình luận nào. Hãy là người đầu tiên bình luận!",
                style = DFTypography.body.copy(fontSize = 13.sp),
                color = DFColor.TextMuted,
                modifier = Modifier.padding(horizontal = DFSpacing.xxl, vertical = DFSpacing.lg)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DFSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(DFSpacing.md)
            ) {
                comments.forEach { comment ->
                    CommentRowView(
                        comment = comment,
                        isAdmin = user?.isAdmin == true,
                        onDelete = {
                            if (token != null) {
                                scope.launch {
                                    val deleted = repository.deleteComment(token, comment.id)
                                    if (deleted) {
                                        comments = comments.filter { it.id != comment.id }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRowView(
    comment: Comment,
    isAdmin: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = DFRadius.md)
            .padding(DFSpacing.lg),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar Initial
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(DFColor.Bg3),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.user.username.take(1).uppercase(),
                style = DFTypography.headline.copy(color = DFColor.Gold, fontSize = 13.sp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.user.username,
                        style = DFTypography.caption.copy(color = DFColor.Text, fontSize = 12.sp)
                    )
                    if (comment.user.isAdmin) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Badge(text = "Admin", backgroundColor = DFColor.Gold.copy(alpha = 0.2f))
                    }
                }

                if (isAdmin) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = DFColor.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.body,
                style = DFTypography.body.copy(color = DFColor.TextDim, fontSize = 13.sp)
            )
        }
    }
}
