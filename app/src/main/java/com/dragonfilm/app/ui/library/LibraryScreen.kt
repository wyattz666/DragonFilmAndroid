package com.dragonfilm.app.ui.library

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dragonfilm.app.data.model.HistoryItem
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.model.SavedActor
import com.dragonfilm.app.data.storage.LocalStore
import com.dragonfilm.app.ui.components.EmptyStateView
import com.dragonfilm.app.ui.components.PosterCard
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.glassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LibraryTab(val title: String) {
    HISTORY("Lịch sử xem"),
    WATCH_LATER("Phim xem sau"),
    LIKED("Phim yêu thích"),
    ACTORS("Diễn viên")
}

@Composable
fun LibraryScreen(
    localStore: LocalStore,
    onMovieClick: (String) -> Unit
) {
    var currentTab by remember { mutableStateOf(LibraryTab.HISTORY) }
    var showClearDialog by remember { mutableStateOf(false) }

    val historyItems by localStore.historyFlow.collectAsState()
    val watchLaterMovies by localStore.watchLaterFlow.collectAsState()
    val likedMovies by localStore.likedFlow.collectAsState()
    val favoriteActors by localStore.actorsFlow.collectAsState()

    val hasContent = when (currentTab) {
        LibraryTab.HISTORY -> historyItems.isNotEmpty()
        LibraryTab.WATCH_LATER -> watchLaterMovies.isNotEmpty()
        LibraryTab.LIKED -> likedMovies.isNotEmpty()
        LibraryTab.ACTORS -> favoriteActors.isNotEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DFColor.Bg)
    ) {
        // Tab Selector and Clear button with statusBarsPadding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(LibraryTab.entries) { tab ->
                    val isSelected = currentTab == tab
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) DFColor.Gold else Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 0.6.dp,
                                color = if (isSelected) DFColor.Gold else Color.White.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                            .clickable { currentTab = tab }
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = tab.title,
                            style = DFTypography.caption.copy(
                                color = if (isSelected) Color(0xFF07080A) else DFColor.TextDim,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            }

            if (hasContent) {
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Xóa tất cả",
                        tint = DFColor.GoldDim,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Tab Content
        when (currentTab) {
            LibraryTab.HISTORY -> {
                if (historyItems.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.History,
                        title = "Chưa có lịch sử xem",
                        message = "Những phim bạn xem sẽ xuất hiện ở đây."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(historyItems) { item ->
                            HistoryRowItem(
                                item = item,
                                onClick = { onMovieClick(item.slug) }
                            )
                        }
                    }
                }
            }

            LibraryTab.WATCH_LATER -> {
                if (watchLaterMovies.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Bookmark,
                        title = "Chưa có phim xem sau",
                        message = "Lưu phim từ trang chi tiết để xem lại sau."
                    )
                } else {
                    val chunked = watchLaterMovies.chunked(3)
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 90.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chunked) { rowMovies ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (movie in rowMovies) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        PosterCard(
                                            imageUrl = movie.bestPoster,
                                            title = movie.name,
                                            subtitle = movie.yearString,
                                            width = null,
                                            onClick = { onMovieClick(movie.slug) }
                                        )
                                    }
                                }
                                if (rowMovies.size < 3) {
                                    for (k in 0 until (3 - rowMovies.size)) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LibraryTab.LIKED -> {
                if (likedMovies.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Favorite,
                        title = "Chưa có phim yêu thích",
                        message = "Bấm biểu tượng yêu thích ở trang phim để thêm vào đây."
                    )
                } else {
                    val chunked = likedMovies.chunked(3)
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 90.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chunked) { rowMovies ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (movie in rowMovies) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        PosterCard(
                                            imageUrl = movie.bestPoster,
                                            title = movie.name,
                                            subtitle = movie.yearString,
                                            width = null,
                                            onClick = { onMovieClick(movie.slug) }
                                        )
                                    }
                                }
                                if (rowMovies.size < 3) {
                                    for (k in 0 until (3 - rowMovies.size)) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LibraryTab.ACTORS -> {
                if (favoriteActors.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Person,
                        title = "Chưa có diễn viên yêu thích",
                        message = "Bấm vào diễn viên ở trang phim để lưu."
                    )
                } else {
                    val chunked = favoriteActors.chunked(3)
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chunked) { rowActors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (actor in rowActors) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(CircleShape)
                                                .background(DFColor.Bg3)
                                                .border(1.dp, DFColor.GlassBorderGradient, CircleShape)
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(actor.profileUrl)
                                                    .crossfade(150)
                                                    .size(150, 150)
                                                    .build(),
                                                contentDescription = actor.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = actor.name,
                                            style = DFTypography.caption.copy(color = DFColor.Text, fontSize = 11.sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (rowActors.size < 3) {
                                    for (k in 0 until (3 - rowActors.size)) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = "Xác nhận xóa?", color = DFColor.Text) },
            text = { Text(text = "Bạn có chắc muốn xóa toàn bộ mục ${currentTab.title.lowercase()} không?", color = DFColor.TextDim) },
            confirmButton = {
                Button(
                    onClick = {
                        when (currentTab) {
                            LibraryTab.HISTORY -> localStore.clearHistory()
                            LibraryTab.WATCH_LATER -> localStore.clearWatchLater()
                            LibraryTab.LIKED -> localStore.clearLiked()
                            LibraryTab.ACTORS -> localStore.clearActors()
                        }
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DFColor.Crimson)
                ) {
                    Text(text = "Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "Hủy", color = DFColor.TextDim)
                }
            },
            containerColor = DFColor.CardBgSolid
        )
    }
}

@Composable
private fun HistoryRowItem(
    item: HistoryItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = DFRadius.md)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(DFRadius.sm))
                .background(DFColor.Bg3)
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(DFRadius.sm))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.posterUrl)
                    .crossfade(150)
                    .size(120, 180)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = DFTypography.headline.copy(fontSize = 13.sp),
                color = DFColor.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.episodeName,
                style = DFTypography.caption.copy(color = DFColor.Gold, fontSize = 11.sp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(Date((item.watchedAt * 1000).toLong()))

            Text(
                text = "Đã xem lúc $dateStr",
                style = DFTypography.small.copy(color = DFColor.TextMuted, fontSize = 9.5.sp)
            )
        }

        Icon(
            imageVector = Icons.Default.PlayCircleFilled,
            contentDescription = null,
            tint = DFColor.Gold,
            modifier = Modifier.size(24.dp)
        )
    }
}
