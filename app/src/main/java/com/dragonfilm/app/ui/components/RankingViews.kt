package com.dragonfilm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.dragonfilm.app.data.model.AniListNormalized
import com.dragonfilm.app.data.model.NetflixItem
import com.dragonfilm.app.data.model.TMDBWeeklyItem
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.glassCard

@Composable
fun NetflixRankingRow(
    item: NetflixItem,
    rank: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankNumberBadge(rank = rank)

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .width(44.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(DFRadius.sm))
                .background(DFColor.Bg3)
                .border(width = 0.5.dp, color = DFColor.Border, shape = RoundedCornerShape(DFRadius.sm))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.posterUrl ?: item.poster ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp, 64.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = DFTypography.headline.copy(fontSize = 13.5.sp),
                color = DFColor.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.tmdb?.voteAverage != null && item.tmdb.voteAverage > 0) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = DFColor.Gold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = String.format("%.1f", item.tmdb.voteAverage),
                        style = DFTypography.small.copy(color = DFColor.Gold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = item.type.ifEmpty { "Trending" },
                    style = DFTypography.caption.copy(color = DFColor.TextMuted, fontSize = 10.5.sp)
                )
            }
        }
    }
}

@Composable
fun TMDBRankingRow(
    item: TMDBWeeklyItem,
    rank: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankNumberBadge(rank = rank)

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .width(44.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(DFRadius.sm))
                .background(DFColor.Bg3)
                .border(width = 0.5.dp, color = DFColor.Border, shape = RoundedCornerShape(DFRadius.sm))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.posterUrl ?: "")
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp, 64.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = DFTypography.headline.copy(fontSize = 13.5.sp),
                color = DFColor.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.originalTitle.isNullOrEmpty()) {
                Text(
                    text = item.originalTitle,
                    style = DFTypography.caption.copy(color = DFColor.TextMuted, fontSize = 10.5.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.voteAverage != null && item.voteAverage > 0) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = DFColor.Gold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = String.format("%.1f", item.voteAverage),
                        style = DFTypography.small.copy(color = DFColor.Gold)
                    )
                }
            }
        }
    }
}

@Composable
fun AniListRankingRow(
    item: AniListNormalized,
    rank: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RankNumberBadge(rank = rank)

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .width(44.dp)
                .height(64.dp)
                .clip(RoundedCornerShape(DFRadius.sm))
                .background(DFColor.Bg3)
                .border(width = 0.5.dp, color = DFColor.Border, shape = RoundedCornerShape(DFRadius.sm))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp, 64.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = DFTypography.headline.copy(fontSize = 13.5.sp),
                color = DFColor.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.altTitle.isNotEmpty()) {
                Text(
                    text = item.altTitle,
                    style = DFTypography.caption.copy(color = DFColor.TextMuted, fontSize = 10.5.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.score > 0) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = DFColor.Gold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${item.score}%",
                        style = DFTypography.small.copy(color = DFColor.Gold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (item.year > 0) {
                    Text(
                        text = "${item.year}",
                        style = DFTypography.caption.copy(color = DFColor.TextMuted, fontSize = 10.5.sp)
                    )
                }
            }
        }
    }
}

@Composable
fun RankNumberBadge(rank: Int) {
    val (bgColor, textColor) = when (rank) {
        1 -> DFColor.Gold to Color(0xFF07080A)
        2 -> Color(0xFFC0C0C0) to Color(0xFF07080A)
        3 -> Color(0xFFCD7F32) to Color(0xFF07080A)
        else -> DFColor.Bg3 to DFColor.TextMuted
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color = bgColor, shape = CircleShape)
            .shadow(
                elevation = if (rank <= 3) 4.dp else 0.dp,
                shape = CircleShape,
                ambientColor = bgColor,
                spotColor = bgColor
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$rank",
            style = DFTypography.small.copy(
                color = textColor,
                fontWeight = FontWeight.Black,
                fontSize = 11.sp
            )
        )
    }
}
