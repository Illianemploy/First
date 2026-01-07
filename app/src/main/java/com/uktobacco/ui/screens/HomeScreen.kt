package com.uktobacco.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uktobacco.FilterType
import com.uktobacco.SortOption
import com.uktobacco.TobaccoViewModel
import com.uktobacco.ui.components.EnhancedProductCard
import com.uktobacco.ui.components.ProductCardShimmer
import com.uktobacco.ui.theme.UberGreen
import com.uktobacco.ui.theme.UberTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProductClick: (String) -> Unit,
    onBrandClick: (String) -> Unit,
    viewModel: TobaccoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with gradient
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
                    text = "UK Tobacco Prices",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Real-time pricing • ${uiState.products.size} products",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UberTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.onSearchQueryChange(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search products, brands, retailers...") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        Row {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    viewModel.onSearchQueryChange("")
                                }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                            IconButton(onClick = { showFilters = !showFilters }) {
                                Icon(
                                    imageVector = if (showFilters) Icons.Filled.FilterListOff else Icons.Filled.FilterList,
                                    contentDescription = "Toggle filters",
                                    tint = if (showFilters) UberGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = UberGreen,
                    )
                )
            }
        }

        // Filters section with animation
        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            FiltersSection(
                uiState = uiState,
                onTypeFilterChange = viewModel::onTypeFilterChange,
                onRetailerFilterChange = viewModel::onRetailerFilterChange,
                onSortOptionChange = viewModel::onSortOptionChange,
                onClearFilters = viewModel::clearFilters
            )
        }

        // Product count and active filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.filteredProducts.size} products",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val activeFiltersCount = listOf(
                uiState.selectedType != FilterType.ALL,
                uiState.selectedRetailer != null,
                uiState.searchQuery.isNotBlank()
            ).count { it }

            if (activeFiltersCount > 0) {
                Surface(
                    color = UberGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$activeFiltersCount filter${if (activeFiltersCount > 1) "s" else ""} active",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = UberGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Products list
        if (uiState.isLoading) {
            ProductCardShimmer()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = uiState.filteredProducts,
                    key = { it.id }
                ) { product ->
                    EnhancedProductCard(
                        product = product,
                        isFavorite = viewModel.isFavorite(product.id),
                        onFavoriteClick = { viewModel.toggleFavorite(product.id) },
                        onClick = { onProductClick(product.id) },
                        smokingProfile = uiState.smokingProfile
                    )
                }

                if (uiState.filteredProducts.isEmpty()) {
                    item {
                        EmptyState()
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltersSection(
    uiState: com.uktobacco.UiState,
    onTypeFilterChange: (FilterType) -> Unit,
    onRetailerFilterChange: (String?) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearFilters) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                }
            }

            // Type filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Product Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = UberTextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FilterType.entries.toList()) { type ->
                        FilterChip(
                            selected = uiState.selectedType == type,
                            onClick = { onTypeFilterChange(type) },
                            label = {
                                Text(
                                    when (type) {
                                        FilterType.ALL -> "All Products"
                                        FilterType.CIGARETTES -> "Cigarettes"
                                        FilterType.ROLLING_TOBACCO -> "Rolling Tobacco"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UberGreen.copy(alpha = 0.2f),
                                selectedLabelColor = UberGreen
                            )
                        )
                    }
                }
            }

            // Retailer filter
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Retailer",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = UberTextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.selectedRetailer == null,
                            onClick = { onRetailerFilterChange(null) },
                            label = { Text("All Retailers") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UberGreen.copy(alpha = 0.2f),
                                selectedLabelColor = UberGreen
                            )
                        )
                    }
                    items(uiState.availableRetailers) { retailer ->
                        FilterChip(
                            selected = uiState.selectedRetailer == retailer,
                            onClick = { onRetailerFilterChange(retailer) },
                            label = { Text(retailer) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UberGreen.copy(alpha = 0.2f),
                                selectedLabelColor = UberGreen
                            )
                        )
                    }
                }
            }

            // Sort options
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Sort By",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = UberTextSecondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SortOption.entries.toList()) { option ->
                        FilterChip(
                            selected = uiState.sortOption == option,
                            onClick = { onSortOptionChange(option) },
                            label = {
                                Text(
                                    when (option) {
                                        SortOption.PRICE_LOW_TO_HIGH -> "Price: Low-High"
                                        SortOption.PRICE_HIGH_TO_LOW -> "Price: High-Low"
                                        SortOption.NAME_A_TO_Z -> "Name A-Z"
                                        SortOption.BRAND -> "Brand"
                                        SortOption.RETAILER -> "Retailer"
                                        SortOption.LAST_UPDATED -> "Recently Updated"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UberGreen.copy(alpha = 0.2f),
                                selectedLabelColor = UberGreen
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = UberTextSecondary.copy(alpha = 0.5f)
        )
        Text(
            text = "No products found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = UberTextSecondary
        )
        Text(
            text = "Try adjusting your filters or search query",
            style = MaterialTheme.typography.bodyMedium,
            color = UberTextSecondary.copy(alpha = 0.7f)
        )
    }
}
