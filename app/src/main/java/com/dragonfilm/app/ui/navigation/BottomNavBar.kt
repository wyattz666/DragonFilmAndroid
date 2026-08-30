package com.dragonfilm.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFTypography

enum class NavScreen(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "Trang Chủ", Icons.Default.Home),
    SEARCH("search", "Tìm Kiếm", Icons.Default.Search),
    LIBRARY("library", "Thư Viện", Icons.Default.VideoLibrary),
    PROFILE("profile", "Thành Viên", Icons.Default.Person)
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(DFRadius.xl), spotColor = Color.Black)
                .clip(RoundedCornerShape(DFRadius.xl))
                .background(DFColor.CardBgSolid.copy(alpha = 0.96f))
                .border(width = 0.8.dp, brush = DFColor.GlassBorderGradient, shape = RoundedCornerShape(DFRadius.xl))
                .padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavScreen.entries.forEach { screen ->
                val isSelected = currentRoute == screen.route
                Column(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onNavigate(screen) }
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        tint = if (isSelected) DFColor.Gold else DFColor.TextMuted,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screen.title,
                        style = DFTypography.small.copy(
                            color = if (isSelected) DFColor.Gold else DFColor.TextMuted,
                            fontSize = 9.5.sp
                        )
                    )
                }
            }
        }
    }
}
