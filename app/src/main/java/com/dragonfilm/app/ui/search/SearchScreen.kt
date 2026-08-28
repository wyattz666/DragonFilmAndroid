package com.dragonfilm.app.ui.search

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
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
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.repository.MovieRepository
import com.dragonfilm.app.data.storage.LocalStore
import com.dragonfilm.app.ui.components.EmptyStateView
import com.dragonfilm.app.ui.components.PosterCard
import com.dragonfilm.app.ui.components.PosterCardSkeleton
import com.dragonfilm.app.ui.components.SectionHeader
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.glassCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    repository: MovieRepository,
    localStore: LocalStore,
    onMovieClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var recentSearches by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val discoverTags = listOf("Anime Mùa Này", "Phim Chiếu Rạp", "Phim Hàn Quốc", "Phim Trung Quốc", "Top Netflix", "Hành Động")

    fun loadRecent() {
        recentSearches = localStore.getRecentSearches()
    }

    LaunchedEffect(Unit) {
        loadRecent()
    }

    fun performSearch(text: String) {
        val q = text.trim()
        if (q.isEmpty()) {
            results = emptyList()
            isLoading = false
            return
        }
        searchJob?.cancel()
        searchJob = scope.launch {
            isLoading = true
            delay(350)
            localStore.addSearch(q)
            loadRecent()
            val list = repository.searchMovies(q)
            results = list
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DFColor.Bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Glass Search Bar with statusBarsPadding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        performSearch(it)
                    },
                    placeholder = {
                        Text(
                            text = "Tìm tên phim, anime, diễn viên...",
                            style = DFTypography.body.copy(color = DFColor.TextMuted, fontSize = 13.sp)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = DFColor.Gold,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                results = emptyList()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Xóa",
                                    tint = DFColor.TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = DFRadius.lg),
                    shape = RoundedCornerShape(DFRadius.lg),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = DFColor.Text,
                        unfocusedTextColor = DFColor.Text
                    ),
                    singleLine = true
                )
            }

            // Body content
            if (query.isEmpty() && results.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // Quick Discover Tags
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        SectionHeader(title = "Khám Phá Nhanh", modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(discoverTags) { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(DFColor.CardBg)
                                        .border(0.6.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                        .clickable {
                                            query = tag
                                            performSearch(tag)
                                        }
                                        .padding(horizontal = 13.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = tag,
                                        style = DFTypography.caption.copy(color = DFColor.Text, fontSize = 11.5.sp)
                                    )
                                }
                            }
                        }
                    }

                    // Recent Searches
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            SectionHeader(title = "Tìm Gần Đây", modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        items(recentSearches) { recent ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .glassCard(cornerRadius = DFRadius.md)
                                    .clickable {
                                        query = recent
                                        performSearch(recent)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = DFColor.Gold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = recent,
                                    style = DFTypography.body.copy(color = DFColor.Text, fontSize = 13.sp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else if (isLoading) {
                // Skeletons
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val skeletonRows = (0..6).chunked(3)
                    items(skeletonRows) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in row) {
                                Box(modifier = Modifier.weight(1f)) {
                                    PosterCardSkeleton(width = null)
                                }
                            }
                        }
                    }
                }
            } else if (results.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Search,
                    title = "Không tìm thấy phim",
                    message = "Thử tìm kiếm với từ khóa khác hoặc tên tiếng Anh."
                )
            } else {
                // Results Grid Chunked in 3
                val chunkedResults = results.chunked(3)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(chunkedResults) { rowMovies ->
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
                }
            }
        }
    }
}
