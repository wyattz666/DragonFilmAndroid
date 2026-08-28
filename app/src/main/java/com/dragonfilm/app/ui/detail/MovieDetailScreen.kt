package com.dragonfilm.app.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.dragonfilm.app.data.model.Episode
import com.dragonfilm.app.data.model.EpisodeServer
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.model.PersonRef
import com.dragonfilm.app.data.repository.MovieDetailResult
import com.dragonfilm.app.data.repository.MovieRepository
import com.dragonfilm.app.data.storage.AuthManager
import com.dragonfilm.app.data.storage.LocalStore
import com.dragonfilm.app.ui.comments.CommentSectionView
import com.dragonfilm.app.ui.components.Badge
import com.dragonfilm.app.ui.components.EmptyStateView
import com.dragonfilm.app.ui.components.SectionHeader
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.glassCard
import com.dragonfilm.app.util.SourceServer
import kotlinx.coroutines.launch

@Composable
fun MovieDetailScreen(
    slug: String,
    repository: MovieRepository,
    localStore: LocalStore,
    authManager: AuthManager? = null,
    onBack: () -> Unit,
    onPlayEpisode: (Movie, SourceServer, Episode, List<Episode>) -> Unit
) {
    var detailResult by remember { mutableStateOf<MovieDetailResult?>(null) }
    var selectedServer by remember { mutableStateOf(SourceServer.KKPHIM) }
    var selectedEpisodeServerIndex by remember { mutableIntStateOf(0) }
    var currentEpisodeServers by remember { mutableStateOf<List<EpisodeServer>>(emptyList()) }
    var fullDescription by remember { mutableStateOf("") }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val likedMovies by localStore.likedFlow.collectAsState()
    val watchLaterMovies by localStore.watchLaterFlow.collectAsState()
    val historyItems by localStore.historyFlow.collectAsState()
    val favoriteActors by localStore.actorsFlow.collectAsState()

    val scope = rememberCoroutineScope()

    LaunchedEffect(slug) {
        isLoading = true
        val res = repository.getMovieDetail(slug)
        if (res != null) {
            detailResult = res
            selectedServer = res.availableServers.firstOrNull() ?: SourceServer.KKPHIM
            currentEpisodeServers = res.episodeServers
            fullDescription = res.description
        }
        isLoading = false
    }

    fun switchServer(server: SourceServer) {
        selectedServer = server
        scope.launch {
            val (eps, desc) = repository.getEpisodesForServer(server, slug)
            currentEpisodeServers = eps
            selectedEpisodeServerIndex = 0
            if (desc.length > fullDescription.length) {
                fullDescription = desc
            }
        }
    }

    val movie = detailResult?.movie

    Box(modifier = Modifier.fillMaxSize().background(DFColor.Bg)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DFColor.Gold)
            }
        } else if (movie == null) {
            EmptyStateView(
                title = "Không tìm thấy phim",
                message = "Phim này hiện không khả dụng hoặc đã bị gỡ."
            )
        } else {
            val isLiked = likedMovies.any { it.slug == movie.slug }
            val isWatchLater = watchLaterMovies.any { it.slug == movie.slug }
            val history = historyItems.firstOrNull { it.slug == movie.slug }

            val episodes = currentEpisodeServers.getOrNull(selectedEpisodeServerIndex)?.items ?: emptyList()
            val resumeEp = if (history != null) {
                episodes.firstOrNull { it.slug == history.episodeSlug || it.name == history.episodeName }
                    ?: episodes.getOrNull(history.episodeIndex0)
                    ?: episodes.firstOrNull()
            } else {
                episodes.firstOrNull()
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Immersive Backdrop with Poster & Badges
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(movie.bestBanner)
                                .crossfade(150)
                                .scale(Scale.FILL)
                                .build(),
                            contentDescription = movie.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DFColor.HeroBackdropGradient)
                        )

                        // Floating Poster and Meta
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Poster
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .aspectRatio(2f / 3f)
                                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(DFRadius.md), spotColor = Color.Black)
                                    .clip(RoundedCornerShape(DFRadius.md))
                                    .background(DFColor.Bg3)
                                    .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(DFRadius.md))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(movie.bestPoster)
                                        .crossfade(150)
                                        .size(240, 360)
                                        .build(),
                                    contentDescription = movie.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                if (movie.yearString.isNotEmpty()) {
                                    Text(
                                        text = movie.yearString,
                                        style = DFTypography.caption.copy(color = DFColor.Gold, fontSize = 11.5.sp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                Text(
                                    text = movie.name,
                                    style = DFTypography.heroTitle.copy(fontSize = 18.sp, lineHeight = 23.sp),
                                    color = DFColor.Text,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (movie.originName.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = movie.originName,
                                        style = DFTypography.small.copy(fontSize = 11.sp),
                                        color = DFColor.TextDim,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    movie.quality?.let { q ->
                                        Badge(text = q, backgroundColor = Color.White.copy(alpha = 0.18f), textColor = Color.White)
                                    }
                                    movie.lang?.let { l ->
                                        Badge(text = l, backgroundColor = DFColor.Steel.copy(alpha = 0.15f), textColor = DFColor.Steel)
                                    }
                                    if (movie.episodeCurrent.isNotEmpty()) {
                                        Badge(text = movie.episodeCurrent, backgroundColor = DFColor.Sage.copy(alpha = 0.15f), textColor = DFColor.Sage)
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Info Bar (Rating, Year, Genre)
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .glassCard(cornerRadius = DFRadius.md)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val score = movie.tmdb?.scoreString?.takeIf { it != "N/A" }
                            ?: movie.imdb?.scoreString?.takeIf { it != "N/A" }
                            ?: "N/A"

                        QuickInfoItem(
                            icon = Icons.Default.Star,
                            label = "Điểm TMDB",
                            value = score,
                            color = DFColor.Gold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier.height(22.dp).width(1.dp).background(Color.White.copy(alpha = 0.1f))
                        )
                        QuickInfoItem(
                            icon = Icons.Default.CalendarToday,
                            label = "Năm",
                            value = movie.yearString.ifEmpty { "2024" },
                            color = DFColor.Steel,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier.height(22.dp).width(1.dp).background(Color.White.copy(alpha = 0.1f))
                        )
                        QuickInfoItem(
                            icon = Icons.Default.Movie,
                            label = "Thể loại",
                            value = movie.category.firstOrNull()?.name ?: "Phim",
                            color = DFColor.Sage,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Action Buttons (Play, Watch Later, Like)
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Play Button
                        val playTitle = if (resumeEp != null && history != null) {
                            "Tiếp tục ${resumeEp.name}"
                        } else if (resumeEp != null) {
                            "Xem ${resumeEp.name}"
                        } else {
                            "Xem Phim"
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(elevation = 8.dp, shape = RoundedCornerShape(DFRadius.md), ambientColor = DFColor.Gold, spotColor = DFColor.Gold)
                                .clip(RoundedCornerShape(DFRadius.md))
                                .background(DFColor.GoldGradient)
                                .clickable {
                                    if (resumeEp != null) {
                                        onPlayEpisode(movie, selectedServer, resumeEp, episodes)
                                    }
                                }
                                .padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF07080A),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = playTitle,
                                style = DFTypography.headline.copy(color = Color(0xFF07080A), fontSize = 13.5.sp)
                            )
                        }

                        // Watch Later Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(DFRadius.md))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(DFRadius.md))
                                .clickable {
                                    localStore.toggleWatchLater(movie)
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isWatchLater) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Xem sau",
                                tint = if (isWatchLater) DFColor.Gold else Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isWatchLater) "Đã lưu" else "Xem sau",
                                style = DFTypography.callout.copy(color = Color.White, fontSize = 12.sp)
                            )
                        }

                        // Like Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(DFRadius.md))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.16f), shape = RoundedCornerShape(DFRadius.md))
                                .clickable {
                                    localStore.toggleLiked(movie)
                                }
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Yêu thích",
                                tint = if (isLiked) Color.Red else Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Description
                if (fullDescription.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .animateContentSize()
                        ) {
                            Text(
                                text = "NỘI DUNG PHIM",
                                style = DFTypography.small.copy(fontSize = 10.sp),
                                color = DFColor.TextMuted
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = fullDescription,
                                style = DFTypography.body.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                                color = DFColor.TextDim,
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isDescriptionExpanded) "Thu gọn ▲" else "Xem thêm ▼",
                                style = DFTypography.caption.copy(color = DFColor.Gold, fontSize = 11.sp),
                                modifier = Modifier.clickable {
                                    isDescriptionExpanded = !isDescriptionExpanded
                                }
                            )
                        }
                    }
                }

                // Multi-Server Switcher
                val availableServers = detailResult?.availableServers ?: emptyList()
                if (availableServers.size > 1) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "CHỌN NGUỒN PHÁT (SERVER):",
                                style = DFTypography.small.copy(fontSize = 10.sp),
                                color = DFColor.TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(availableServers) { srv ->
                                    val isSelected = selectedServer == srv
                                    Row(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) DFColor.Surface else DFColor.CardBg)
                                            .border(
                                                width = 0.8.dp,
                                                color = if (isSelected) DFColor.Gold else Color.White.copy(alpha = 0.1f),
                                                shape = CircleShape
                                            )
                                            .clickable { switchServer(srv) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) DFColor.Gold else Color.White.copy(alpha = 0.3f))
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = srv.displayName,
                                            style = DFTypography.caption.copy(
                                                color = if (isSelected) Color.White else DFColor.TextDim,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Episode Section Header
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionHeader(
                            title = "Danh Sách Tập",
                            modifier = Modifier.weight(1f).padding(horizontal = 0.dp)
                        )
                        if (episodes.isNotEmpty()) {
                            Text(
                                text = "${episodes.size} tập",
                                style = DFTypography.caption.copy(color = DFColor.TextMuted, fontSize = 11.5.sp)
                            )
                        }
                    }
                }

                // Version Selector Tabs (Vietsub, Thuyết minh, Lồng tiếng)
                if (currentEpisodeServers.size > 1) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentEpisodeServers.indices.toList()) { idx ->
                                val srv = currentEpisodeServers[idx]
                                val isSelected = selectedEpisodeServerIndex == idx
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) DFColor.Gold else Color.White.copy(alpha = 0.08f))
                                        .border(
                                            width = 0.6.dp,
                                            color = if (isSelected) DFColor.Gold else Color.White.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedEpisodeServerIndex = idx }
                                        .padding(horizontal = 13.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = srv.serverName,
                                        style = DFTypography.caption.copy(
                                            color = if (isSelected) Color(0xFF07080A) else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Episode Grid (Chunked 4 columns)
                if (episodes.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Chưa có tập phim",
                            message = "Hãy thử chọn nguồn phát (Server) khác ở trên."
                        )
                    }
                } else {
                    val chunkedEpisodes = episodes.chunked(4)
                    items(chunkedEpisodes) { rowEps ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (ep in rowEps) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .glassCard(cornerRadius = DFRadius.md)
                                        .clickable {
                                            onPlayEpisode(movie, selectedServer, ep, episodes)
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ep.name,
                                        style = DFTypography.headline.copy(fontSize = 12.sp),
                                        color = DFColor.Text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (rowEps.size < 4) {
                                for (k in 0 until (4 - rowEps.size)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Cast Section
                val actors = movie.actor
                if (!actors.isNullOrEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        SectionHeader(title = "Diễn Viên", modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(actors) { actor ->
                                val isFav = favoriteActors.any { it.name == actor.name }
                                Column(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clickable {
                                            localStore.toggleFavoriteActor(actor)
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(62.dp)
                                            .clip(CircleShape)
                                            .background(DFColor.Bg3)
                                            .border(width = 1.dp, brush = DFColor.GlassBorderGradient, shape = CircleShape)
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

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(DFColor.Bg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = null,
                                                tint = if (isFav) Color.Red else DFColor.TextMuted,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = actor.name,
                                        style = DFTypography.small.copy(fontSize = 10.sp),
                                        color = DFColor.Text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (actor.character.isNotEmpty()) {
                                        Text(
                                            text = actor.character,
                                            style = DFTypography.small.copy(fontSize = 8.5.sp, color = DFColor.TextMuted),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Comments Section
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    CommentSectionView(
                        movieKey = movie.commentKey,
                        movieName = movie.name,
                        title = "Bình Luận",
                        repository = repository,
                        localStore = localStore,
                        authManager = authManager
                    )
                }
            }
        }

        // Top Back Button with statusBarsPadding
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 12.dp, top = 6.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .border(0.6.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun QuickInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Column {
            Text(text = label, style = DFTypography.small.copy(fontSize = 8.5.sp), color = DFColor.TextMuted)
            Text(text = value, style = DFTypography.headline.copy(fontSize = 11.5.sp), color = DFColor.Text, maxLines = 1)
        }
    }
}
