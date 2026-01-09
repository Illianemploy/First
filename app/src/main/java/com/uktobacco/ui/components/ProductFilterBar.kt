package com.uktobacco.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uktobacco.R
import com.uktobacco.TobaccoType
import com.uktobacco.ui.theme.UberGreen

/**
 * Advanced filter bar for tobacco products
 * Features clean, sleek 3-tile navigation system with glossy icons
 */
@Composable
fun ProductFilterBar(
    selectedFilter: TobaccoType?,
    onFilterSelected: (TobaccoType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title
        Text(
            text = "Browse by Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Filter tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cigarettes filter
            FilterTile(
                icon = R.drawable.ic_filter_cigarette,
                label = "Cigarettes",
                isSelected = selectedFilter == TobaccoType.CIGARETTES,
                onClick = {
                    onFilterSelected(
                        if (selectedFilter == TobaccoType.CIGARETTES) null
                        else TobaccoType.CIGARETTES
                    )
                },
                modifier = Modifier.weight(1f)
            )

            // Rolling Tobacco filter
            FilterTile(
                icon = R.drawable.ic_filter_rolling_tobacco,
                label = "Rolling\nTobacco",
                isSelected = selectedFilter == TobaccoType.ROLLING_TOBACCO,
                onClick = {
                    onFilterSelected(
                        if (selectedFilter == TobaccoType.ROLLING_TOBACCO) null
                        else TobaccoType.ROLLING_TOBACCO
                    )
                },
                modifier = Modifier.weight(1f)
            )

            // Tobacco Pouches filter
            FilterTile(
                icon = R.drawable.ic_filter_tobacco_pouch,
                label = "Tobacco\nPouches",
                isSelected = selectedFilter == TobaccoType.TOBACCO_POUCHES,
                onClick = {
                    onFilterSelected(
                        if (selectedFilter == TobaccoType.TOBACCO_POUCHES) null
                        else TobaccoType.TOBACCO_POUCHES
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Clear filter hint (when filter active)
        if (selectedFilter != null) {
            Text(
                text = "Tap again to show all products",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * Individual filter tile with glossy icon and label
 * Apple-esque design: clean, interactive, delightful
 */
@Composable
private fun FilterTile(
    icon: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animated scale for interactive feedback
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "filter-scale"
    )

    // Animated background color
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) UberGreen.copy(alpha = 0.1f) else Color.Transparent,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "filter-bg"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Glossy icon
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFFAFAFA)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = label,
                        modifier = Modifier.size(48.dp),
                        tint = Color.Unspecified // Preserve icon colors
                    )
                }
            }

            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) UberGreen else MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = MaterialTheme.typography.labelMedium.fontSize * 1.2
            )

            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(UberGreen)
                )
            }
        }
    }
}
