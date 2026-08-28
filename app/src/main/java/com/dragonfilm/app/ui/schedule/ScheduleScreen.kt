package com.dragonfilm.app.ui.schedule

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dragonfilm.app.data.model.Movie
import com.dragonfilm.app.data.model.ScheduleDayItem
import com.dragonfilm.app.data.repository.MovieRepository
import com.dragonfilm.app.ui.components.EmptyStateView
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.glassCard
import com.dragonfilm.app.ui.theme.shimmer
import java.util.Calendar
import java.util.Date

@Composable
fun ScheduleScreen(
    repository: MovieRepository,
    onMovieClick: (String) -> Unit
) {
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDay by remember { mutableStateOf(Date()) }

    val days = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        val list = mutableListOf<ScheduleDayItem>()
        for (i in 0 until 14) {
            list.add(ScheduleDayItem(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val res = repository.getSourceList(operation = "latest", page = 1)
        movies = res.movies
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DFColor.Bg)
    ) {
        // Day Picker Bar with statusBarsPadding
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.statusBarsPadding()
        ) {
            items(days) { day ->
                val isSelected = isSameDay(day.date, selectedDay)
                Column(
                    modifier = Modifier
                        .size(width = 46.dp, height = 50.dp)
                        .clip(RoundedCornerShape(DFRadius.md))
                        .background(if (isSelected) DFColor.Gold else DFColor.Bg3)
                        .border(
                            width = 0.8.dp,
                            color = if (day.isToday && !isSelected) DFColor.BorderStrong else Color.Transparent,
                            shape = RoundedCornerShape(DFRadius.md)
                        )
                        .clickable { selectedDay = day.date }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = day.weekdayShort,
                        style = DFTypography.small.copy(
                            color = if (isSelected) DFColor.Bg else DFColor.TextMuted,
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = day.dayNumber,
                        style = DFTypography.callout.copy(
                            color = if (isSelected) DFColor.Bg else DFColor.Text,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }

        // Schedule List
        if (isLoading) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(6) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(DFRadius.lg))
                            .shimmer()
                    )
                }
            }
        } else if (movies.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.CalendarMonth,
                title = "Chưa có lịch chiếu",
                message = "Không tìm thấy phim nào cho ngày này."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(movies) { movie ->
                    ScheduleRowItem(
                        movie = movie,
                        onClick = { onMovieClick(movie.slug) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRowItem(
    movie: Movie,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = DFRadius.md)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(84.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(DFRadius.sm))
                .background(DFColor.Bg3)
                .border(0.5.dp, DFColor.Border.copy(alpha = 0.35f), RoundedCornerShape(DFRadius.sm))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(movie.bestThumb)
                    .crossfade(150)
                    .size(200, 130)
                    .build(),
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movie.name,
                style = DFTypography.headline.copy(fontSize = 13.sp),
                color = DFColor.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (movie.episodeCurrent.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(DFColor.Gold.copy(alpha = 0.15f), RoundedCornerShape(DFRadius.sm))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = movie.episodeCurrent,
                            style = DFTypography.small.copy(color = DFColor.Gold, fontSize = 9.sp)
                        )
                    }
                }
                if (movie.yearString.isNotEmpty()) {
                    Text(
                        text = movie.yearString,
                        style = DFTypography.small.copy(color = DFColor.TextMuted, fontSize = 9.5.sp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = DFColor.TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun isSameDay(d1: Date, d2: Date): Boolean {
    val c1 = Calendar.getInstance().apply { time = d1 }
    val c2 = Calendar.getInstance().apply { time = d2 }
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
