package com.dragonfilm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography

@Composable
fun SectionHeader(
    title: String,
    onSeeMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DFSpacing.xxl),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gold Accent Indicator Bar
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(18.dp)
                .shadow(elevation = 6.dp, ambientColor = DFColor.Gold, spotColor = DFColor.Gold)
                .clip(RoundedCornerShape(2.dp))
                .background(DFColor.GoldGradient)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            style = DFTypography.title2,
            color = DFColor.Text,
            modifier = Modifier.weight(1f)
        )

        if (onSeeMore != null) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DFColor.Gold.copy(alpha = 0.12f))
                    .border(width = 0.6.dp, color = DFColor.Gold.copy(alpha = 0.25f), shape = CircleShape)
                    .clickable(onClick = onSeeMore)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Xem thêm",
                    style = DFTypography.caption.copy(color = DFColor.Gold, fontSize = 11.sp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Xem thêm",
                    tint = DFColor.Gold,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
