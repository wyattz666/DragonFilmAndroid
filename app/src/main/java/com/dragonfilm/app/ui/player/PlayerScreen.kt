package com.dragonfilm.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dragonfilm.app.data.model.Episode
import com.dragonfilm.app.data.model.HistoryItem
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.storage.CloudSync
import com.dragonfilm.app.data.storage.LocalStore
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.util.SourceServer
import com.dragonfilm.app.util.StreamResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    movie: Movie,
    server: SourceServer,
    initialEpisode: Episode,
    allEpisodes: List<Episode>,
    localStore: LocalStore,
    cloudSync: CloudSync? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var currentEpisode by remember { mutableStateOf(initialEpisode) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isSpeedHoldActive by remember { mutableStateOf(false) }
    var isZoomFill by remember { mutableStateOf(false) }
    var seekFeedback by remember { mutableStateOf<Boolean?>(null) } // true: +10s, false: -10s
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showEpisodeSheet by remember { mutableStateOf(false) }
    var useEmbedFallback by remember { mutableStateOf(false) }
    var embedUrl by remember { mutableStateOf<String?>(null) }

    // Lock landscape orientation
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val isEmbedMode = useEmbedFallback || server == SourceServer.NGUONC || server == SourceServer.VSMOV
            || (currentEpisode.linkM3U8.isNullOrEmpty() && !currentEpisode.linkEmbed.isNullOrEmpty())

    // ExoPlayer initialization
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_READY) {
                        durationMs = duration.coerceAtLeast(0L)
                    }
                }
            })
        }
    }

    fun saveWatchProgress(force: Boolean = false) {
        val currentSec = if (isEmbedMode) 0.0 else (currentPositionMs / 1000.0)
        val epIdx = allEpisodes.indexOfFirst { it.id == currentEpisode.id }.coerceAtLeast(0)
        val item = HistoryItem(
            slug = movie.slug,
            name = movie.name,
            posterUrl = movie.bestPoster,
            year = movie.yearString,
            server = server.rawValue,
            sourceName = "",
            episodeName = currentEpisode.name,
            episodeSlug = currentEpisode.slug,
            episodeServerName = server.displayName,
            episodeServerIdx = 0,
            episodeIndex0 = epIdx,
            episodeNumber = epIdx + 1,
            watchedSeconds = currentSec
        )
        localStore.addToHistory(item)
        if (currentSec > 5) {
            localStore.setResumeTime(movie.slug, currentSec)
        }
        scope.launch {
            cloudSync?.sync()
        }
    }

    fun loadEpisodeStream(ep: Episode) {
        currentEpisode = ep
        saveWatchProgress(force = true)

        if (isEmbedMode) {
            embedUrl = ep.linkEmbed ?: ep.linkM3U8
            return
        }

        val rawUrl = ep.linkM3U8 ?: ep.linkEmbed
        if (rawUrl.isNullOrEmpty()) {
            useEmbedFallback = true
            embedUrl = ep.linkEmbed
            return
        }

        scope.launch {
            isBuffering = true
            val resolved = StreamResolver.resolve(rawUrl)
            if (resolved.contains(".m3u8")) {
                val resumeSec = localStore.getResumeTime(movie.slug)
                val mediaItem = MediaItem.fromUri(Uri.parse(resolved))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                if (resumeSec > 5) {
                    exoPlayer.seekTo((resumeSec * 1000).toLong())
                }
                exoPlayer.play()
            } else {
                useEmbedFallback = true
                embedUrl = resolved
            }
        }
    }

    LaunchedEffect(currentEpisode) {
        loadEpisodeStream(currentEpisode)
    }

    // Position tracker & auto-save loop
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(1000)
        }
    }

    // Auto-save watch history every 5s
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(5000)
            saveWatchProgress()
        }
    }

    // Auto-hide controls
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(4000)
            isControlsVisible = false
        }
    }

    BackHandler {
        saveWatchProgress(force = true)
        exoPlayer.release()
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            saveWatchProgress(force = true)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isEmbedMode && !embedUrl.isNullOrEmpty()) {
            EmbedPlayerView(
                url = embedUrl!!,
                modifier = Modifier.fillMaxSize()
            )

            // Floating Exit for Embed Player
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable {
                            saveWatchProgress(force = true)
                            exoPlayer.release()
                            onBack()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Thoát",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Thoát",
                        style = DFTypography.caption.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(0.8.dp, DFColor.Gold.copy(alpha = 0.5f), CircleShape)
                        .clickable { showEpisodeSheet = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentEpisode.name} · Chọn tập ▼",
                        style = DFTypography.caption.copy(color = DFColor.Gold, fontWeight = FontWeight.Bold)
                    )
                }
            }
        } else {
            // Native ExoPlayer Video Player
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false
                        player = exoPlayer
                        resizeMode = if (isZoomFill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { pv ->
                    pv.resizeMode = if (isZoomFill) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                isControlsVisible = !isControlsVisible
                            },
                            onDoubleTap = { offset ->
                                val width = size.width
                                if (offset.x < width * 0.38f) {
                                    // Seek -10s
                                    val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                    exoPlayer.seekTo(newPos)
                                    seekFeedback = false
                                    scope.launch {
                                        delay(700)
                                        seekFeedback = null
                                    }
                                } else if (offset.x > width * 0.62f) {
                                    // Seek +10s
                                    val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                    exoPlayer.seekTo(newPos)
                                    seekFeedback = true
                                    scope.launch {
                                        delay(700)
                                        seekFeedback = null
                                    }
                                } else {
                                    isControlsVisible = !isControlsVisible
                                }
                            },
                            onLongPress = {
                                if (exoPlayer.isPlaying) {
                                    isSpeedHoldActive = true
                                    exoPlayer.playbackParameters = PlaybackParameters(2.0f)
                                }
                            }
                        )
                    }
            )

            // Buffering Spinner
            if (isBuffering) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = DFColor.Gold,
                        modifier = Modifier.size(54.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            // Seek Feedback Bubble Indicator (±10s)
            seekFeedback?.let { isForward ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 80.dp)
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.75f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (isForward) "+10s" else "-10s",
                            style = DFTypography.caption.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // 2x Speed Hold Indicator Pill
            if (isSpeedHoldActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.85f))
                            .border(1.dp, DFColor.Gold, CircleShape)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = DFColor.Gold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tốc độ 2×",
                            style = DFTypography.caption.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Overlay Controls (Top & Bottom bars, Center Play/Pause)
            AnimatedVisibility(
                visible = isControlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    saveWatchProgress(force = true)
                                    exoPlayer.release()
                                    onBack()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Quay lại",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = movie.name,
                                    style = DFTypography.headline.copy(fontSize = 15.sp),
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${currentEpisode.name} · ${server.displayName}",
                                    style = DFTypography.small.copy(color = DFColor.Gold)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Zoom / Fit Toggle
                            IconButton(
                                onClick = { isZoomFill = !isZoomFill }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Tỉ lệ màn hình",
                                    tint = if (isZoomFill) DFColor.Gold else Color.White
                                )
                            }

                            // Speed Menu
                            Box {
                                IconButton(onClick = { showSpeedMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Tốc độ",
                                        tint = Color.White
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { rate ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${rate}×",
                                                    color = if (playbackSpeed == rate) DFColor.Gold else Color.Unspecified
                                                )
                                            },
                                            onClick = {
                                                playbackSpeed = rate
                                                exoPlayer.playbackParameters = PlaybackParameters(rate)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Center Play / Pause & Seek Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(40.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(newPos)
                            },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Lùi 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DFColor.Gold)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                                tint = Color(0xFF07080A),
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(newPos)
                            },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Tiến 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Bottom Bar (Timeline & Actions)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        // Slider Timeline Bar
                        Slider(
                            value = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                            onValueChange = { percent ->
                                val target = (percent * durationMs).toLong()
                                exoPlayer.seekTo(target)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = DFColor.Gold,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPositionMs),
                                style = DFTypography.small.copy(color = Color.White)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Next episode
                                val currIdx = allEpisodes.indexOfFirst { it.id == currentEpisode.id }
                                if (currIdx >= 0 && currIdx + 1 < allEpisodes.size) {
                                    val nextEp = allEpisodes[currIdx + 1]
                                    Row(
                                        modifier = Modifier
                                            .clickable { loadEpisodeStream(nextEp) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Tập tiếp",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Tập tiếp",
                                            style = DFTypography.caption.copy(color = Color.White)
                                        )
                                    }
                                }

                                // Choose episode
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .clickable { showEpisodeSheet = true }
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chọn tập ▼",
                                        style = DFTypography.caption.copy(color = Color.White)
                                    )
                                }
                            }

                            Text(
                                text = formatTime(durationMs),
                                style = DFTypography.small.copy(color = DFColor.TextMuted)
                            )
                        }
                    }
                }
            }
        }
    }

    // Episode Selector Modal Sheet
    if (showEpisodeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEpisodeSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = DFColor.CardBgSolid
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DFSpacing.xl)
            ) {
                Text(
                    text = "Danh Sách Tập Phim",
                    style = DFTypography.title,
                    color = DFColor.Text
                )
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(allEpisodes) { ep ->
                        val isSelected = ep.id == currentEpisode.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(DFRadius.md))
                                .background(if (isSelected) DFColor.Gold.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    showEpisodeSheet = false
                                    loadEpisodeStream(ep)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ep.name,
                                style = DFTypography.headline.copy(
                                    color = if (isSelected) DFColor.Gold else DFColor.Text,
                                    fontSize = 14.sp
                                )
                            )
                            if (isSelected) {
                                Text(
                                    text = "Đang phát",
                                    style = DFTypography.small.copy(color = DFColor.Gold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
