package com.dragonfilm.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.dragonfilm.app.data.model.CatalogFilter
import com.dragonfilm.app.data.model.CatalogFilterKind
import com.dragonfilm.app.data.model.CatalogOption
import com.dragonfilm.app.data.model.Genre
import com.dragonfilm.app.ui.theme.DFColor
import com.dragonfilm.app.ui.theme.DFRadius
import com.dragonfilm.app.ui.theme.DFSpacing
import com.dragonfilm.app.ui.theme.DFTypography
import com.dragonfilm.app.ui.theme.glassCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterDialog(
    initialFilter: CatalogFilter,
    onApply: (CatalogFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(initialFilter) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DFSpacing.md)
                .glassCard(cornerRadius = DFRadius.xl),
            color = DFColor.CardBgSolid,
            shape = RoundedCornerShape(DFRadius.xl)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DFSpacing.xl)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bộ Lọc Phim",
                        style = DFTypography.title,
                        color = DFColor.Text
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = DFColor.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Types section
                Text(text = "ĐỊNH DẠNG", style = DFTypography.small, color = DFColor.Gold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CatalogOption.types.forEach { genre ->
                        val isSelected = selectedFilter.kind == CatalogFilterKind.TYPE && selectedFilter.slug == genre.slug
                                || (selectedFilter.isEmpty && genre.slug.isEmpty())
                        FilterChip(
                            text = genre.name,
                            isSelected = isSelected,
                            onClick = {
                                selectedFilter = if (genre.slug.isEmpty()) {
                                    CatalogFilter()
                                } else {
                                    CatalogFilter(kind = CatalogFilterKind.TYPE, slug = genre.slug, label = genre.name)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Genres section
                Text(text = "THỂ LOẠI", style = DFTypography.small, color = DFColor.Gold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CatalogOption.genres.forEach { genre ->
                        val isSelected = selectedFilter.kind == CatalogFilterKind.GENRE && selectedFilter.slug == genre.slug
                        FilterChip(
                            text = genre.name,
                            isSelected = isSelected,
                            onClick = {
                                selectedFilter = if (genre.slug.isEmpty()) {
                                    CatalogFilter()
                                } else {
                                    CatalogFilter(kind = CatalogFilterKind.GENRE, slug = genre.slug, label = genre.name)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Countries section
                Text(text = "QUỐC GIA", style = DFTypography.small, color = DFColor.Gold)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CatalogOption.countries.forEach { country ->
                        val isSelected = selectedFilter.kind == CatalogFilterKind.COUNTRY && selectedFilter.slug == country.slug
                        FilterChip(
                            text = country.name,
                            isSelected = isSelected,
                            onClick = {
                                selectedFilter = if (country.slug.isEmpty()) {
                                    CatalogFilter()
                                } else {
                                    CatalogFilter(kind = CatalogFilterKind.COUNTRY, slug = country.slug, label = country.name)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Apply button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = DFColor.Gold, spotColor = DFColor.Gold)
                        .clip(CircleShape)
                        .background(DFColor.GoldGradient)
                        .clickable {
                            onApply(selectedFilter)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Áp Dụng Bộ Lọc",
                        style = DFTypography.headline.copy(color = Color(0xFF07080A))
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) DFColor.Gold else Color.White.copy(alpha = 0.08f))
            .border(
                width = 0.8.dp,
                color = if (isSelected) DFColor.Gold else Color.White.copy(alpha = 0.15f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = DFTypography.caption.copy(
                color = if (isSelected) Color(0xFF07080A) else DFColor.Text,
                fontSize = 11.5.sp
            )
        )
    }
}
