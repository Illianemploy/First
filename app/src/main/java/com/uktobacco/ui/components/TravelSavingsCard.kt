package com.uktobacco.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uktobacco.TobaccoProduct
import com.uktobacco.data.SmokingProfile
import com.uktobacco.data.TravelSavingsCalculation
import com.uktobacco.data.TravelSavingsCalculator
import com.uktobacco.ui.theme.UberGreen
import com.uktobacco.ui.theme.UberTextSecondary
import java.text.NumberFormat
import java.util.*

/**
 * Sleek, Uber-style travel savings card
 * Shows "Could you save money flying abroad to buy this?"
 */
@Composable
fun TravelSavingsCard(
    product: TobaccoProduct,
    smokingProfile: SmokingProfile?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    // Calculate best opportunities (if profile exists)
    val opportunities = remember(smokingProfile) {
        smokingProfile?.let { profile ->
            TravelSavingsCalculator.findBestOpportunities(profile, monthsToSupply = 3)
                .take(5) // Top 5 opportunities
        } ?: emptyList()
    }

    val hasSavings = opportunities.isNotEmpty()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasSavings)
                Color(0xFF003D00) // Dark green for savings
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        onClick = { if (hasSavings || smokingProfile == null) expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.FlightTakeoff,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (hasSavings) UberGreen else UberTextSecondary
                    )
                    Column {
                        Text(
                            text = if (smokingProfile == null) "Travel Arbitrage" else "Bulk-Buy Abroad?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasSavings) UberGreen else MaterialTheme.colorScheme.onSurface
                        )
                        if (hasSavings) {
                            Text(
                                text = "You could save £${String.format("%,.0f", opportunities.first().totalSavings)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = UberGreen.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = if (hasSavings) UberGreen else UberTextSecondary
                )
            }

            // No profile prompt
            if (smokingProfile == null && !expanded) {
                Text(
                    text = "Set up your smoking profile to see if flying abroad to bulk-buy could save you money",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UberTextSecondary
                )
            }

            // Expanded content
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (smokingProfile == null) {
                        // Explanation
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "How it works:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            InfoPoint(
                                number = "1",
                                text = "Enter your daily cigarette consumption in your Smoking Profile"
                            )
                            InfoPoint(
                                number = "2",
                                text = "We calculate how much you'd save by flying to cheaper countries"
                            )
                            InfoPoint(
                                number = "3",
                                text = "Buy up to legal limits (200-800 packs depending on country)"
                            )
                            InfoPoint(
                                number = "4",
                                text = "One-click booking to Skyscanner for flights"
                            )

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Example: Flying to Poland could save a 20/day smoker over £7,000 annually",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UberTextSecondary
                                )
                            }
                        }
                    } else {
                        // Show actual opportunities
                        if (opportunities.isEmpty()) {
                            Text(
                                "No profitable opportunities found based on your usage",
                                style = MaterialTheme.typography.bodyMedium,
                                color = UberTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                "Top Savings Opportunities:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasSavings) UberGreen else MaterialTheme.colorScheme.onSurface
                            )

                            opportunities.forEach { opportunity ->
                                TravelOpportunityItem(
                                    opportunity = opportunity,
                                    productPricePerPack = product.price
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPoint(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = UberGreen.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = UberGreen
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TravelOpportunityItem(
    opportunity: TravelSavingsCalculation,
    productPricePerPack: Double
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Country header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = opportunity.country.flagEmoji,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Column {
                        Text(
                            text = opportunity.country.countryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format("%.1f", opportunity.country.flightTimeHours)}h flight",
                            style = MaterialTheme.typography.bodySmall,
                            color = UberTextSecondary
                        )
                    }
                }

                // Savings badge
                Surface(
                    color = UberGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelSmall,
                            color = UberGreen
                        )
                        Text(
                            text = "£${formatNumber(opportunity.totalSavings)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = UberGreen
                        )
                    }
                }
            }

            Divider()

            // Breakdown
            DetailRow("Price per pack", "£${String.format("%.2f", opportunity.country.averagePackPrice)}")
            DetailRow("Packs to buy", "${opportunity.packsToBuy} (${String.format("%.1f", opportunity.monthsCovered)} months)")
            DetailRow("Cigarette cost", "£${formatNumber(opportunity.costInCountry)}")
            DetailRow("Flight cost", "£${formatNumber(opportunity.flightCost)}")
            DetailRow("Total trip cost", "£${formatNumber(opportunity.totalTripCost)}", isBold = true)

            Divider()

            DetailRow("UK cost (same amount)", "£${formatNumber(opportunity.packsToBuy * productPricePerPack)}", color = MaterialTheme.colorScheme.error)
            DetailRow("Your savings", "£${formatNumber(opportunity.totalSavings)}", color = UberGreen, isBold = true)

            // Book button
            Button(
                onClick = {
                    // Deep link to Skyscanner
                    val skyscannerUrl = buildSkyscannerUrl(
                        destination = opportunity.country.countryCode,
                        countryName = opportunity.country.countryName
                    )
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(skyscannerUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = UberGreen
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.FlightTakeoff, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Book Flight to ${opportunity.country.countryName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Legal limit: ${opportunity.legalLimit} packs for personal use",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = UberTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}

private fun formatNumber(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.UK).format(value.toInt())
}

/**
 * Builds Skyscanner deep link URL
 * Opens Skyscanner app or website with pre-filled search
 */
private fun buildSkyscannerUrl(destination: String, countryName: String): String {
    // Skyscanner deep link format
    // https://www.skyscanner.net/transport/flights/lond/{destination}/?adultsv2=1&cabinclass=economy&childrenv2=&inboundaltsenabled=false&outboundaltsenabled=false&preferdirects=false&ref=home&rtn=1

    val destinationCode = when (destination) {
        "PL" -> "waw" // Warsaw
        "CZ" -> "prg" // Prague
        "HU" -> "bud" // Budapest
        "BG" -> "sof" // Sofia
        "RO" -> "buh" // Bucharest
        "ES" -> "mad" // Madrid
        "PT" -> "lis" // Lisbon
        "IT" -> "rome" // Rome
        "GR" -> "ath" // Athens
        "HR" -> "zag" // Zagreb
        "FR" -> "pari" // Paris
        "BE" -> "bru" // Brussels
        "NL" -> "ams" // Amsterdam
        "DE" -> "berl" // Berlin
        "SE" -> "sto" // Stockholm
        "NO" -> "osl" // Oslo
        "DK" -> "cope" // Copenhagen
        "MA" -> "casa" // Casablanca
        "TN" -> "tuni" // Tunis
        "TR" -> "ist" // Istanbul
        else -> destination.lowercase()
    }

    return "https://www.skyscanner.net/transport/flights/lond/$destinationCode/?adultsv2=1&cabinclass=economy&rtn=1"
}

/**
 * Compact savings badge for product cards
 */
@Composable
fun BestDealBadge(
    savings: Double,
    countryName: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = UberGreen,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.FlightTakeoff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
            Column {
                Text(
                    text = "Best Deal: $countryName",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Save £${formatNumber(savings)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
