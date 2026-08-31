package com.dragonfilm.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.dragonfilm.app.R
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import kotlin.math.absoluteValue
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.dragonfilm.app.data.model.AniListNormalized
import com.dragonfilm.app.data.model.CatalogFilter
import com.dragonfilm.app.data.model.CatalogFilterKind
import com.dragonfilm.app.data.model.HistoryItem
import com.dragonfilm.app.data.model.HomeRow
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.model.NetflixItem
import com.dragonfilm.app.data.model.TMDBWeeklyItem
import com.dragonfilm.app.data.repository.MovieRepository
import com.dragonfilm.app.data.storage.LocalStore
import com.dragonfilm.app.ui.comments.CommentSectionView
import com.dragonfilm.app.ui.components.AniListRankingRow
import com.dragonfilm.app.ui.components.Badge
import com.dragonfilm.app.ui.components.VipBadge
import com.dragonfilm.app.ui.components.NetflixRankingRow
import com.dragonfilm.app.ui.components.PosterCard
import com.dragonfilm.app.ui.components.SectionHeader
import com.dragonfilm.app.ui.components.TMDBRankingRow
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.glassCard
import com.dragonfilm.app.ui.theme.shimmer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    repository: MovieRepository,
    localStore: LocalStore,
    onMovieClick: (String) -> Unit,
    onSearchClick: () -> Unit
) {
    var homeRows by remember { mutableStateOf<List<HomeRow>>(emptyList()) }
    var isLoadingHome by remember { mutableStateOf(true) }

    var netflixItems by remember { mutableStateOf<List<NetflixItem>>(emptyList()) }
    var tmdbKRItems by remember { mutableStateOf<List<TMDBWeeklyItem>>(emptyList()) }
    var tmdbCNItems by remember { mutableStateOf<List<TMDBWeeklyItem>>(emptyList()) }
    var animeWeeklyItems by remember { mutableStateOf<List<AniListNormalized>>(emptyList()) }
    var animeSeasonItems by remember { mutableStateOf<List<AniListNormalized>>(emptyList()) }
    var animeSeasonLabel by remember { mutableStateOf("Trending Mùa Này") }

    var latestMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var latestPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var isLoadingMoreLatest by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(CatalogFilter()) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val historyItems by localStore.historyFlow.collectAsState()
    val scope = rememberCoroutineScope()

    fun loadLatest(page: Int, filter: CatalogFilter, replace: Boolean) {
        scope.launch {
            if (!replace) isLoadingMoreLatest = true
            val res = repository.getSourceList(
                operation = filter.operation,
                slug = filter.slug,
                page = page
            )
            if (replace) {
                latestMovies = res.movies
            } else {
                val existing = latestMovies.map { it.slug }.toSet()
                latestMovies = latestMovies + res.movies.filter { !existing.contains(it.slug) }
            }
            latestPage = page
            totalPages = res.totalPages
            isLoadingMoreLatest = false
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope {
            val homeTask = async {
                try {
                    val res = repository.getHome()
                    homeRows = res.rows
                } catch (_: Exception) {}
                isLoadingHome = false
            }

            val netflixTask = async { netflixItems = repository.getNetflixTop10() }
            val tmdbKRTask = async { tmdbKRItems = repository.getTMDBWeekly("KR") }
            val tmdbCNTask = async { tmdbCNItems = repository.getTMDBWeekly("CN") }
            val animeTask = async { animeWeeklyItems = repository.getAniListWeeklyTrending(10) }
            val seasonTask = async {
                val res = repository.getAniListSeasonRanking(10)
                animeSeasonLabel = "Trending ${res.first}"
                animeSeasonItems = res.second
            }
            val latestTask = async { loadLatest(1, activeFilter, true) }

            homeTask.await()
            netflixTask.await()
            tmdbKRTask.await()
            tmdbCNTask.await()
            animeTask.await()
            seasonTask.await()
            latestTask.await()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DFColor.Bg)) {
        // Sticky Header with safe area insets
        TopStickyHeader(onSearchClick = onSearchClick)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Category Navigation Filter Chips
            item {
                Spacer(modifier = Modifier.height(10.dp))
                CategoryPillsRow(
                    activeFilter = activeFilter,
                    onFilterSelected = { filter ->
                        activeFilter = filter
                        loadLatest(1, filter, true)
                    },
                    onOpenFilterDialog = { showFilterDialog = true }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Continue Watching Row
            if (historyItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader(title = "Tiếp Tục Xem", modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(historyItems.take(8)) { item ->
                            ContinueWatchingCard(
                                item = item,
                                onClick = { onMovieClick(item.slug) }
                            )
                        }
                    }
                }
            }

            // Home Rows from API
            items(homeRows) { row ->
                Spacer(modifier = Modifier.height(22.dp))
                SectionHeader(
                    title = row.title,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onSeeMore = {
                        activeFilter = when {
                            row.key.contains("bo") || row.title.contains("bộ", ignoreCase = true) ->
                                CatalogFilter(CatalogFilterKind.TYPE, "phim-bo", row.title)
                            row.key.contains("le") || row.title.contains("lẻ", ignoreCase = true) ->
                                CatalogFilter(CatalogFilterKind.TYPE, "phim-le", row.title)
                            row.key.contains("hoat-hinh") || row.title.contains("hoạt hình", ignoreCase = true) ->
                                CatalogFilter(CatalogFilterKind.TYPE, "hoat-hinh", row.title)
                            row.key.contains("tv") || row.title.contains("tv shows", ignoreCase = true) ->
                                CatalogFilter(CatalogFilterKind.TYPE, "tv-shows", row.title)
                            else -> CatalogFilter(CatalogFilterKind.LATEST, row.key, row.title)
                        }
                        loadLatest(1, activeFilter, true)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(row.items) { movie ->
                        PosterCard(
                            imageUrl = movie.bestPoster,
                            title = movie.name,
                            subtitle = movie.yearString,
                            badge = movie.type,
                            width = 114.dp,
                            onClick = { onMovieClick(movie.slug) }
                        )
                    }
                }
            }

            // Netflix Top 10 Ranking
            if (netflixItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    RankingSection(
                        title = "Netflix Việt Nam",
                        badgeLabel = "Top 10 VN",
                        badgeColor = Color(0xFFE50914),
                        items = netflixItems
                    ) { item ->
                        NetflixRankingRow(
                            item = item,
                            rank = item.rank,
                            onClick = { onMovieClick(item.title) }
                        )
                    }
                }
            }

            // TMDB Korea Weekly Ranking
            if (tmdbKRItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    RankingSection(
                        title = "Phim Hàn Hot Tuần",
                        badgeLabel = "TMDB KR",
                        badgeColor = DFColor.Steel,
                        items = tmdbKRItems
                    ) { item ->
                        TMDBRankingRow(
                            item = item,
                            rank = item.rank,
                            onClick = { onMovieClick(item.title) }
                        )
                    }
                }
            }

            // TMDB China Weekly Ranking
            if (tmdbCNItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    RankingSection(
                        title = "Top Phim Trung Quốc",
                        badgeLabel = "TMDB CN",
                        badgeColor = DFColor.GoldDim,
                        items = tmdbCNItems
                    ) { item ->
                        TMDBRankingRow(
                            item = item,
                            rank = item.rank,
                            onClick = { onMovieClick(item.title) }
                        )
                    }
                }
            }

            // AniList Trending Anime
            if (animeWeeklyItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    RankingSection(
                        title = "Anime Thịnh Hành",
                        badgeLabel = "AniList",
                        badgeColor = DFColor.Sage,
                        items = animeWeeklyItems
                    ) { item ->
                        AniListRankingRow(
                            item = item,
                            rank = animeWeeklyItems.indexOf(item) + 1,
                            onClick = { onMovieClick(item.title) }
                        )
                    }
                }
            }

            // Latest Catalog Grid Header
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeader(
                        title = activeFilter.displayTitle,
                        modifier = Modifier.weight(1f).padding(horizontal = 0.dp)
                    )

                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DFColor.Gold.copy(alpha = 0.12f))
                            .border(width = 0.6.dp, color = DFColor.Gold.copy(alpha = 0.35f), shape = CircleShape)
                            .clickable { showFilterDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Bộ lọc",
                            tint = DFColor.Gold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Bộ lọc",
                            style = DFTypography.caption.copy(color = DFColor.Gold, fontSize = 11.5.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Grid items chunked in 3 columns perfectly aligned
            val chunkedMovies = latestMovies.chunked(3)
            items(chunkedMovies) { rowMovies ->
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
                                badge = movie.episodeCurrent.ifEmpty { movie.quality },
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

            // Load More Button
            item {
                if (latestPage < totalPages) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingMoreLatest) {
                            CircularProgressIndicator(
                                color = DFColor.Gold,
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(DFColor.CardBg)
                                    .border(width = 0.8.dp, color = DFColor.Border, shape = CircleShape)
                                    .clickable {
                                        loadLatest(latestPage + 1, activeFilter, false)
                                    }
                                    .padding(horizontal = 22.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = "Xem thêm phim...",
                                    style = DFTypography.callout.copy(color = DFColor.Gold, fontSize = 12.5.sp)
                                )
                            }
                        }
                    }
                }
            }

            // General Home Comments
            item {
                Spacer(modifier = Modifier.height(20.dp))
                CommentSectionView(
                    movieKey = "dragonfilm_homepage",
                    movieName = "DragonFilm",
                    title = "Bình luận cộng đồng",
                    repository = repository,
                    localStore = localStore
                )
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            initialFilter = activeFilter,
            onApply = { newFilter ->
                activeFilter = newFilter
                loadLatest(1, newFilter, true)
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun TopStickyHeader(onSearchClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DFColor.Bg.copy(alpha = 0.96f))
            .statusBarsPadding()
            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "DragonFilm Logo",
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "DRAGONFILM",
                style = DFTypography.largeTitle.copy(
                    brush = DFColor.GoldGradient,
                    fontSize = 19.sp,
                    letterSpacing = 1.2.sp
                )
            )
            VipBadge(text = "VIP MEMBER")
        }

        IconButton(
            onClick = onSearchClick,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(width = 0.8.dp, brush = DFColor.GlassBorderGradient, shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Tìm kiếm",
                tint = DFColor.Gold,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

// MARK: - Category Navigation Filter Chips

@Composable
private fun CategoryPillsRow(
    activeFilter: CatalogFilter,
    onFilterSelected: (CatalogFilter) -> Unit,
    onOpenFilterDialog: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "Đề xuất"
        item {
            val isRecommended = activeFilter.isEmpty
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isRecommended) Color.White else Color.White.copy(alpha = 0.12f))
                    .border(
                        width = 0.8.dp,
                        color = if (isRecommended) Color.White else Color.White.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
                    .clickable {
                        onFilterSelected(CatalogFilter())
                    }
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Đề xuất",
                    style = DFTypography.caption.copy(
                        color = if (isRecommended) Color(0xFF07080A) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // "Phim bộ"
        item {
            val isSeries = activeFilter.slug == "phim-bo"
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSeries) Color.White else Color.White.copy(alpha = 0.12f))
                    .border(
                        width = 0.8.dp,
                        color = if (isSeries) Color.White else Color.White.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
                    .clickable {
                        onFilterSelected(CatalogFilter(CatalogFilterKind.TYPE, "phim-bo", "Phim Bộ"))
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Phim bộ",
                    style = DFTypography.caption.copy(
                        color = if (isSeries) Color(0xFF07080A) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // "Phim lẻ"
        item {
            val isSingle = activeFilter.slug == "phim-le"
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSingle) Color.White else Color.White.copy(alpha = 0.12f))
                    .border(
                        width = 0.8.dp,
                        color = if (isSingle) Color.White else Color.White.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
                    .clickable {
                        onFilterSelected(CatalogFilter(CatalogFilterKind.TYPE, "phim-le", "Phim Lẻ"))
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Phim lẻ",
                    style = DFTypography.caption.copy(
                        color = if (isSingle) Color(0xFF07080A) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // "Thể loại ▾"
        item {
            val isGenre = activeFilter.kind == CatalogFilterKind.GENRE
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isGenre) Color.White else Color.White.copy(alpha = 0.12f))
                    .border(
                        width = 0.8.dp,
                        color = if (isGenre) Color.White else Color.White.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
                    .clickable { onOpenFilterDialog() }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isGenre && activeFilter.label.isNotEmpty()) activeFilter.label else "Thể loại",
                    style = DFTypography.caption.copy(
                        color = if (isGenre) Color(0xFF07080A) else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (isGenre) Color(0xFF07080A) else Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    item: HistoryItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(200.dp)
            .glassCard(cornerRadius = DFRadius.md)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(DFRadius.sm))
                .background(DFColor.Bg3)
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

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = DFTypography.headline.copy(fontSize = 12.sp),
                color = DFColor.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.episodeName,
                style = DFTypography.caption.copy(color = DFColor.Gold, fontSize = 10.5.sp),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun <T> RankingSection(
    title: String,
    badgeLabel: String,
    badgeColor: Color,
    items: List<T>,
    itemContent: @Composable (T) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = title, modifier = Modifier.weight(1f).padding(horizontal = 0.dp))
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.18f), shape = CircleShape)
                    .border(0.6.dp, badgeColor.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeLabel,
                    style = DFTypography.small.copy(color = badgeColor, fontSize = 9.5.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val chunks = items.chunked(5)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chunks) { columnItems ->
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .glassCard(cornerRadius = DFRadius.lg)
                        .padding(10.dp)
                ) {
                    columnItems.forEachIndexed { idx, item ->
                        itemContent(item)
                        if (idx < columnItems.size - 1) {
                            HorizontalDivider(
                                color = DFColor.Border.copy(alpha = 0.25f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
