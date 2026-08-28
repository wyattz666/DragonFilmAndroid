package com.dragonfilm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.shimmer

@Composable
fun PosterCard(
    imageUrl: String,
    title: String,
    subtitle: String = "",
    badge: String? = null,
    width: Dp? = null,
    onClick: () -> Unit
) {
    val rootModifier = if (width != null) {
        Modifier.width(width).clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    }

    Column(modifier = rootModifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(DFRadius.md))
                .background(DFColor.Bg3)
                .border(width = 0.6.dp, color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(DFRadius.md))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(150)
                    .size(280, 420)
                    .scale(Scale.FILL)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle dark bottom gradient on poster
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.65f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.7f)
                        )
                    )
            )

            // Badge overlay (Episode or Quality)
            if (!badge.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            color = DFColor.Crimson.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(DFRadius.sm)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = DFTypography.small.copy(color = Color.White, fontSize = 9.sp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = title,
            style = DFTypography.headline.copy(fontSize = 12.5.sp, lineHeight = 16.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = DFColor.Text
        )

        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = DFTypography.caption.copy(fontSize = 10.5.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = DFColor.TextMuted
            )
        }
    }
}

@Composable
fun PosterCardSkeleton(width: Dp? = null) {
    val rootModifier = if (width != null) {
        Modifier.width(width)
    } else {
        Modifier.fillMaxWidth()
    }

    Column(modifier = rootModifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(DFRadius.md))
                .shimmer()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(13.dp)
                .clip(RoundedCornerShape(DFRadius.sm))
                .shimmer()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(10.dp)
                .clip(RoundedCornerShape(DFRadius.sm))
                .shimmer()
        )
    }
}
