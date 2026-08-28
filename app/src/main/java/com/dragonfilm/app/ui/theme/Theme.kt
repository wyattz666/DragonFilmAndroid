package com.dragonfilm.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object DFColor {
    // Deep Cinema Obsidian Backgrounds
    val Bg          = Color(0xFF07080A)
    val Bg2         = Color(0xFF0E1015)
    val Bg3         = Color(0xFF151922)
    val Bg4         = Color(0xFF1F2430)
    val CardBg      = Color(0xDD11141A)
    val CardBgSolid = Color(0xFF11141A)
    val Surface     = Color(0xC0161B24)
    val Glass       = Color(0x0FFFFFFF)

    // Cinematic Accents
    val Gold        = Color(0xFFF5C518)
    val GoldLight   = Color(0xFFFFE082)
    val GoldDim     = Color(0xFFC59E27)
    val Amber       = Color(0xFFFF6B00)
    val Crimson     = Color(0xFFE50914)
    val Sage        = Color(0xFF10B981)
    val Steel       = Color(0xFF38BDF8)
    val Purple      = Color(0xFFA855F7)

    // Text hierarchy
    val Text        = Color(0xFFF9FAFB)
    val TextDim     = Color(0xFFD1D5DB)
    val TextMuted   = Color(0xFF828997)

    // Metallic & Glass Borders
    val Border       = Color(0x38F5C518)
    val BorderStrong = Color(0x8CF5C518)
    val GlassBorder  = Color(0x1FFFFFFF)

    val LiveGreen    = Color(0xFF10B981)

    // Gradients
    val GoldGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFE082), Color(0xFFF5C518), Color(0xFFD49E10))
    )

    val FireGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF8A00), Color(0xFFE52E71))
    )

    val GlassBorderGradient = Brush.linearGradient(
        colors = listOf(
            Color(0x59FFFFFF),
            Color(0x4DF5C518),
            Color(0x14FFFFFF)
        )
    )

    val HeroBackdropGradient = Brush.verticalGradient(
        0.0f to Color.Transparent,
        0.35f to Color(0x4D07080A),
        0.75f to Color(0xD907080A),
        1.0f to Color(0xFF07080A)
    )
}

object DFRadius {
    val sm = 6.dp
    val md = 10.dp
    val lg = 14.dp
    val xl = 20.dp
    val xxl = 26.dp
    val pill = 999.dp
}

object DFSpacing {
    val xs = 4.dp
    val sm = 6.dp
    val md = 8.dp
    val lg = 12.dp
    val xl = 16.dp
    val xxl = 20.dp
    val xxxl = 24.dp
    val sectionH = 32.dp
}

object DFTypography {
    val heroTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = DFColor.Text
    )
    val largeTitle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = DFColor.Text
    )
    val title = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = DFColor.Text
    )
    val title2 = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = DFColor.Text
    )
    val headline = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = DFColor.Text
    )
    val body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = DFColor.TextDim
    )
    val callout = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = DFColor.TextDim
    )
    val caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = DFColor.TextMuted
    )
    val small = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = DFColor.TextMuted
    )
}

fun Modifier.glassCard(
    cornerRadius: Dp = DFRadius.lg,
    shape: Shape = RoundedCornerShape(cornerRadius),
    elevation: Dp = 8.dp
): Modifier = this
    .shadow(elevation = elevation, shape = shape, ambientColor = Color.Black, spotColor = Color.Black)
    .background(DFColor.CardBg, shape = shape)
    .border(width = 0.8.dp, brush = DFColor.GlassBorderGradient, shape = shape)

fun Modifier.goldGlow(
    radius: Dp = 10.dp,
    shape: Shape = RoundedCornerShape(DFRadius.md)
): Modifier = this
    .shadow(elevation = radius, shape = shape, ambientColor = DFColor.Gold, spotColor = DFColor.Gold)

@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            DFColor.Bg3,
            DFColor.Bg4,
            DFColor.Bg3
        ),
        start = Offset(translateAnim - 500f, translateAnim - 500f),
        end = Offset(translateAnim, translateAnim)
    )
    return this.background(brush)
}

private val DarkColorScheme = darkColorScheme(
    primary = DFColor.Gold,
    secondary = DFColor.GoldLight,
    tertiary = DFColor.Steel,
    background = DFColor.Bg,
    surface = DFColor.Bg2,
    onPrimary = DFColor.Bg,
    onSecondary = DFColor.Bg,
    onBackground = DFColor.Text,
    onSurface = DFColor.Text
)

@Composable
fun DragonFilmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
