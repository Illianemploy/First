package com.uktobacco.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uktobacco.TobaccoProduct
import com.uktobacco.TobaccoType
import com.uktobacco.data.SmokingProfile
import com.uktobacco.data.TravelSavingsCalculator
import com.uktobacco.ui.theme.CigaretteBlue
import com.uktobacco.ui.theme.TobaccoOrange
import com.uktobacco.ui.theme.UberGreen
import com.uktobacco.ui.theme.UberTextSecondary
import java.time.Duration
import java.time.LocalDateTime
import kotlin.random.Random

@Composable
fun EnhancedProductCard(
    product: TobaccoProduct,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    smokingProfile: SmokingProfile? = null
) {
    // Calculate best opportunity if smoking profile exists
    val bestOpportunity = remember(smokingProfile) {
        smokingProfile?.let { profile ->
            TravelSavingsCalculator.findBestOpportunities(profile, monthsToSupply = 3)
                .firstOrNull()
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card-scale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 6.dp,
        animationSpec = tween(100),
        label = "card-elevation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation,
            pressedElevation = 2.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            // Gradient background accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = if (product.type == TobaccoType.CIGARETTES)
                                listOf(CigaretteBlue, CigaretteBlue.copy(alpha = 0.5f))
                            else
                                listOf(TobaccoOrange, TobaccoOrange.copy(alpha = 0.5f))
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type badge
                    Surface(
                        color = if (product.type == TobaccoType.CIGARETTES)
                            CigaretteBlue.copy(alpha = 0.15f)
                        else
                            TobaccoOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (product.type == TobaccoType.CIGARETTES) "CIGARETTES" else "TOBACCO",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (product.type == TobaccoType.CIGARETTES) CigaretteBlue else TobaccoOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Favorite button with animation
                    val favoriteScale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.2f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        ),
                        label = "favorite-scale"
                    )

                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.scale(favoriteScale)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) UberGreen else UberTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Brand and retailer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.labelLarge,
                        color = UberTextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        color = UberGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = product.retailer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = UberGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Product name
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Best Deal Badge
                if (bestOpportunity != null) {
                    BestDealBadge(
                        savings = bestOpportunity.totalSavings,
                        countryName = bestOpportunity.country.countryName,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price section with animation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "£${product.price}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = UberGreen
                            )

                            // Random price trend indicator (simulated)
                            val trend = remember { Random.nextBoolean() }
                            Surface(
                                color = if (trend) UberGreen.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (trend) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (trend) UberGreen else Color.Red
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "£0.${Random.nextInt(10, 50)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (trend) UberGreen else Color.Red
                                    )
                                }
                            }
                        }

                        Text(
                            text = product.size,
                            style = MaterialTheme.typography.bodyMedium,
                            color = UberTextSecondary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "£${product.pricePerUnit}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "per ${if (product.type == TobaccoType.CIGARETTES) "cig" else "gram"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = UberTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Updated time with pulse indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isRecent = remember {
                        Duration.between(product.lastUpdated, LocalDateTime.now()).toMinutes() < 10
                    }

                    if (isRecent) {
                        PulseLoadingDot(
                            modifier = Modifier.size(8.dp),
                            color = UberGreen
                        )
                    }

                    Text(
                        text = "Updated ${formatTime(product.lastUpdated)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isRecent) UberGreen else UberTextSecondary
                    )
                }
            }
        }
    }
}

private fun formatTime(dateTime: LocalDateTime): String {
    val now = LocalDateTime.now()
    val minutes = Duration.between(dateTime, now).toMinutes()

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1440}d ago"
    }
}
