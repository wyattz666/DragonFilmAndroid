package com.dragonfilm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFTypography

@Composable
fun Badge(
    text: String,
    backgroundColor: Color = DFColor.Gold.copy(alpha = 0.18f),
    textColor: Color = DFColor.Gold,
    borderColor: Color = DFColor.Gold.copy(alpha = 0.4f),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = RoundedCornerShape(DFRadius.sm))
            .border(width = 0.6.dp, color = borderColor, shape = RoundedCornerShape(DFRadius.sm))
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = text,
            style = DFTypography.small.copy(color = textColor, fontSize = 9.5.sp)
        )
    }
}

@Composable
fun PillBadge(
    text: String,
    backgroundColor: Color = Color.White.copy(alpha = 0.15f),
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color = backgroundColor, shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.5.dp)
    ) {
        Text(
            text = text,
            style = DFTypography.small.copy(color = textColor, fontSize = 10.sp)
        )
    }
}

@Composable
fun VipBadge(
    text: String = "VIP MEMBER",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(brush = DFColor.GoldGradient, shape = RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = DFTypography.small.copy(
                color = Color(0xFF07080A),
                fontSize = 9.5.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}
