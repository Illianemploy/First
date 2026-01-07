package com.uktobacco.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uktobacco.TobaccoType
import com.uktobacco.TobaccoViewModel
import com.uktobacco.ui.components.Product3DViewer
import com.uktobacco.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    onBackClick: () -> Unit,
    onBrandClick: (String) -> Unit,
    viewModel: TobaccoViewModel = viewModel()
) {
    val product = viewModel.getProductById(productId)
    val isFavorite by remember {
        derivedStateOf { viewModel.isFavorite(productId) }
    }

    val scrollState = rememberScrollState()
    val headerCollapsed by remember {
        derivedStateOf { scrollState.value > 200 }
    }

    if (product == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = UberTextSecondary
                )
                Text("Product not found", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onBackClick) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = headerCollapsed,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Text(
                            product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    val scale by animateFloatAsState(
                        targetValue = if (isFavorite) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "favorite-scale"
                    )

                    IconButton(
                        onClick = { viewModel.toggleFavorite(productId) },
                        modifier = Modifier.scale(scale)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) UberGreen else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Filled.Share, "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(
                        alpha = if (headerCollapsed) 1f else 0f
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 3D Product Viewer
            Product3DViewer(
                product = product,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )

            // Product info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-30).dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
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
                            text = if (product.type == TobaccoType.CIGARETTES) "CIGARETTES" else "ROLLING TOBACCO",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (product.type == TobaccoType.CIGARETTES) CigaretteBlue else TobaccoOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Product name and brand
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onBrandClick(product.brand) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "by ${product.brand}",
                                style = MaterialTheme.typography.titleMedium,
                                color = UberGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = UberGreen
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Price section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Current Price",
                                style = MaterialTheme.typography.labelMedium,
                                color = UberTextSecondary
                            )
                            Text(
                                text = "£${product.price}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = UberGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Per Unit",
                                style = MaterialTheme.typography.labelMedium,
                                color = UberTextSecondary
                            )
                            Text(
                                text = "£${product.pricePerUnit}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "per ${if (product.type == TobaccoType.CIGARETTES) "cigarette" else "gram"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = UberTextSecondary
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Details grid
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailRow(label = "Size", value = product.size)
                        DetailRow(label = "Retailer", value = product.retailer)
                        DetailRow(
                            label = "Last Updated",
                            value = formatDetailTime(product.lastUpdated)
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Product info
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Product Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        InfoCard(
                            icon = Icons.Filled.Info,
                            title = "Package Type",
                            description = if (product.type == TobaccoType.CIGARETTES)
                                "Factory-made cigarettes in a sealed package"
                            else
                                "Hand-rolling tobacco in a resealable pouch"
                        )

                        InfoCard(
                            icon = Icons.Filled.Store,
                            title = "Availability",
                            description = "Available at ${product.retailer} locations across the UK"
                        )

                        InfoCard(
                            icon = Icons.Filled.TrendingUp,
                            title = "Real-time Pricing",
                            description = "Prices are updated in real-time from retailer sources"
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action button
                    Button(
                        onClick = { onBrandClick(product.brand) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UberGreen
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Learn About ${product.brand}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = UberTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = UberGreen
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UberTextSecondary
                )
            }
        }
    }
}

private fun formatDetailTime(dateTime: java.time.LocalDateTime): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    return dateTime.format(formatter)
}
