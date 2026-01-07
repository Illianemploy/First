package com.uktobacco.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uktobacco.data.CountryPricing
import com.uktobacco.data.GlobalTobaccoPricing
import com.uktobacco.data.Region
import com.uktobacco.ui.theme.UberGreen
import com.uktobacco.ui.theme.UberTextSecondary

@Composable
fun GlobalPricesScreen() {
    var selectedRegion by remember { mutableStateOf<Region?>(null) }
    var sortBy by remember { mutableStateOf(SortOption.PRICE_LOW_HIGH) }

    val countries = remember(selectedRegion, sortBy) {
        val filtered = selectedRegion?.let {
            GlobalTobaccoPricing.getCountriesByRegion(it)
        } ?: GlobalTobaccoPricing.allCountries

        when (sortBy) {
            SortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.averagePackPrice }
            SortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.averagePackPrice }
            SortOption.TAX_HIGH_LOW -> filtered.sortedByDescending { it.taxRate }
            SortOption.TAX_LOW_HIGH -> filtered.sortedBy { it.taxRate }
            SortOption.ALPHABETICAL -> filtered.sortedBy { it.countryName }
        }
    }

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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Public,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = UberGreen
                    )
                    Text(
                        text = "Global Tobacco Prices",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${countries.size} countries • Real-time pricing & tax data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UberTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Filters
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Region filter
            Text(
                "Filter by Region",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = UberTextSecondary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedRegion == null,
                        onClick = { selectedRegion = null },
                        label = { Text("All Regions") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = UberGreen.copy(alpha = 0.2f),
                            selectedLabelColor = UberGreen
                        )
                    )
                }

                items(GlobalTobaccoPricing.getAllRegions()) { region ->
                    FilterChip(
                        selected = selectedRegion == region,
                        onClick = { selectedRegion = region },
                        label = { Text(region.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = region.color.copy(alpha = 0.2f),
                            selectedLabelColor = region.color
                        )
                    )
                }
            }

            // Sort options
            Text(
                "Sort By",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = UberTextSecondary
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SortOption.values().toList()) { option ->
                    FilterChip(
                        selected = sortBy == option,
                        onClick = { sortBy = option },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = UberGreen.copy(alpha = 0.2f),
                            selectedLabelColor = UberGreen
                        )
                    )
                }
            }
        }

        // Countries list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(countries, key = { it.countryCode }) { country ->
                CountryPriceCard(country)
            }
        }
    }
}

@Composable
private fun CountryPriceCard(country: CountryPricing) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
                .padding(16.dp)
        ) {
            // Main info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = country.flagEmoji,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Column {
                        Text(
                            text = country.countryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = country.region.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = country.region.color
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "£${String.format("%.2f", country.averagePackPrice)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = UberGreen
                    )
                    Text(
                        text = "per 20 pack",
                        style = MaterialTheme.typography.bodySmall,
                        color = UberTextSecondary
                    )
                }
            }

            // Tax indicator bar
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Tax: ${String.format("%.1f", country.taxRate)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = UberTextSecondary
                    )
                    Text(
                        "£${String.format("%.2f", country.taxAmount)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LinearProgressIndicator(
                    progress = (country.taxRate / 100.0).toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        country.taxRate > 80 -> MaterialTheme.colorScheme.error
                        country.taxRate > 70 -> Color(0xFFFF9800)
                        else -> UberGreen
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Expanded details
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider()

                    DetailRow("Price before tax", "£${String.format("%.2f", country.priceBeforeTax)}")
                    DetailRow("Tax amount", "£${String.format("%.2f", country.taxAmount)}")
                    DetailRow("Total price", "£${String.format("%.2f", country.averagePackPrice)}")

                    if (country.flightTimeHours > 0) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        DetailRow("Flight from UK", "${String.format("%.1f", country.flightTimeHours)}h")
                        DetailRow("Avg. return flight", "£${String.format("%.0f", country.averageFlightCostReturn)}")
                        DetailRow("Import limit", "${country.bulkBuyLimit} packs")
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (country.averagePackPrice < 10.0)
                            UberGreen.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (country.averagePackPrice < 10.0) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = if (country.averagePackPrice < 10.0) UberGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                if (country.averagePackPrice < 10.0)
                                    "Significantly cheaper than UK (${String.format("%.0f", ((14.0 - country.averagePackPrice) / 14.0) * 100)}% less)"
                                else
                                    "Similar or higher price than UK",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (country.averagePackPrice < 10.0)
                                    UberGreen
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class SortOption(val label: String) {
    PRICE_LOW_HIGH("Price: Low-High"),
    PRICE_HIGH_LOW("Price: High-Low"),
    TAX_HIGH_LOW("Tax: High-Low"),
    TAX_LOW_HIGH("Tax: Low-High"),
    ALPHABETICAL("A-Z")
}
