package com.uktobacco.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uktobacco.TobaccoViewModel
import com.uktobacco.ui.components.EnhancedProductCard
import com.uktobacco.ui.theme.UberTextSecondary

@Composable
fun FavoritesScreen(
    onProductClick: (String) -> Unit,
    viewModel: TobaccoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val favoriteProducts = viewModel.getFavoriteProducts()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (favoriteProducts.isEmpty())
                        "Your saved products will appear here"
                    else
                        "${favoriteProducts.size} saved product${if (favoriteProducts.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UberTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Content
        if (favoriteProducts.isEmpty()) {
            EmptyFavoritesState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = favoriteProducts,
                    key = { it.id }
                ) { product ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        EnhancedProductCard(
                            product = product,
                            isFavorite = true,
                            onFavoriteClick = { viewModel.toggleFavorite(product.id) },
                            onClick = { onProductClick(product.id) },
                            smokingProfile = uiState.smokingProfile
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoritesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = UberTextSecondary.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No favorites yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the heart icon on any product to save it here for quick access",
            style = MaterialTheme.typography.bodyLarge,
            color = UberTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}
