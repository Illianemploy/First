package com.uktobacco.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uktobacco.data.TobaccoStatistics
import com.uktobacco.ui.theme.UberGreen
import com.uktobacco.ui.theme.UberTextSecondary
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.*

@Composable
fun AwarenessScreen() {
    var showDeathTicker by remember { mutableStateOf(true) }
    var globalDeaths by remember { mutableStateOf(0) }
    var ukExpenditure by remember { mutableStateOf(0.0) }

    // Live death ticker
    LaunchedEffect(Unit) {
        val yearStart = java.time.LocalDate.now().withDayOfYear(1)
        val daysElapsed = java.time.Period.between(yearStart, java.time.LocalDate.now()).days + 1
        val initialDeaths = (TobaccoStatistics.DEATHS_PER_DAY_GLOBAL * daysElapsed).toInt()

        TobaccoStatistics.observeGlobalDeaths(initialDeaths).collectLatest {
            globalDeaths = it
        }
    }

    // Live expenditure ticker
    LaunchedEffect(Unit) {
        TobaccoStatistics.observeUKExpenditure().collectLatest {
            ukExpenditure = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A0000), // Dark red
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
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Global Impact",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Real-time data on tobacco's true cost",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UberTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Toggle between views
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showDeathTicker,
                    onClick = { showDeathTicker = true },
                    label = { Text("Death Counter") },
                    leadingIcon = if (showDeathTicker) {
                        { Icon(Icons.Filled.RadioButtonChecked, contentDescription = null, Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.error
                    )
                )

                FilterChip(
                    selected = !showDeathTicker,
                    onClick = { showDeathTicker = false },
                    label = { Text("UK Spending") },
                    leadingIcon = if (!showDeathTicker) {
                        { Icon(Icons.Filled.RadioButtonChecked, contentDescription = null, Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UberGreen.copy(alpha = 0.2f),
                        selectedLabelColor = UberGreen
                    )
                )
            }

            // Main ticker display
            AnimatedContent(
                targetState = showDeathTicker,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) with
                            fadeOut(animationSpec = tween(500))
                },
                label = "ticker-transition"
            ) { showDeath ->
                if (showDeath) {
                    DeathTickerCard(globalDeaths)
                } else {
                    ExpenditureTickerCard(ukExpenditure)
                }
            }

            // Statistics cards
            StatisticsSection()

            // Information section
            InfoSection()

            // Call to action
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = UberGreen.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.HealthAndSafety,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = UberGreen
                    )

                    Text(
                        "Ready to Quit?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        "Use your Smoking Profile to see how much you could save. Explore Global Prices to understand the true cost. Every cigarette you don't smoke is a step toward better health and wealth.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = UberTextSecondary
                    )

                    Button(
                        onClick = { /* Navigate to resources */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UberGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Quitting Resources")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeathTickerCard(deaths: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D0000) // Very dark red
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                "Tobacco-Related Deaths This Year",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Text(
                text = NumberFormat.getNumberInstance(Locale.UK).format(deaths),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            )

            Text(
                "~15 deaths per minute globally",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Divider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow("Per day", NumberFormat.getNumberInstance().format(TobaccoStatistics.DEATHS_PER_DAY_GLOBAL))
                StatRow("Per hour", NumberFormat.getNumberInstance().format(TobaccoStatistics.DEATHS_PER_HOUR_GLOBAL))
                StatRow("Annual estimate", "8 million globally")
            }
        }
    }
}

@Composable
private fun ExpenditureTickerCard(expenditure: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF003D00) // Dark green
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.AttachMoney,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = UberGreen
            )

            Text(
                "UK Spending on Tobacco Today",
                style = MaterialTheme.typography.titleMedium,
                color = UberGreen,
                textAlign = TextAlign.Center
            )

            Text(
                text = "£${formatLargeNumber(expenditure)}",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = UberGreen
            )

            Text(
                "and counting...",
                style = MaterialTheme.typography.bodyMedium,
                color = UberGreen.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Divider(color = UberGreen.copy(alpha = 0.3f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow("Daily average", "£${formatLargeNumber(TobaccoStatistics.DAILY_UK_EXPENDITURE)}")
                StatRow("Yearly estimate", "£${formatLargeNumber(TobaccoStatistics.ANNUAL_UK_EXPENDITURE)}")
                StatRow("Smokers in UK", "${NumberFormat.getNumberInstance().format(TobaccoStatistics.SMOKERS_IN_UK)}")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatisticsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Key Facts",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        FactCard(
            icon = Icons.Filled.Public,
            title = "Global Impact",
            facts = listOf(
                "8 million deaths annually worldwide",
                "1.3 billion tobacco users globally",
                "80% of smokers live in low- and middle-income countries"
            )
        )

        FactCard(
            icon = Icons.Filled.LocationOn,
            title = "UK Statistics",
            facts = listOf(
                "78,000 deaths per year in the UK",
                "6.9 million adult smokers",
                "Smoking costs NHS £2.5 billion annually"
            )
        )

        FactCard(
            icon = Icons.Filled.Science,
            title = "Health Effects",
            facts = listOf(
                "7,000+ chemicals in cigarette smoke",
                "250+ harmful chemicals, 69 known carcinogens",
                "Each cigarette reduces life expectancy by 11 minutes"
            )
        )
    }
}

@Composable
private fun FactCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    facts: List<String>
) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = UberGreen
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            facts.forEach { fact ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", color = UberGreen)
                    Text(
                        fact,
                        style = MaterialTheme.typography.bodyMedium,
                        color = UberTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Data Sources",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "All statistics are based on World Health Organization (WHO) data, UK Office for National Statistics, and peer-reviewed medical research.",
                style = MaterialTheme.typography.bodySmall,
                color = UberTextSecondary
            )
        }
    }
}

private fun formatLargeNumber(value: Double): String {
    return when {
        value >= 1_000_000 -> String.format("%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format("%.1fK", value / 1_000)
        else -> String.format("%.0f", value)
    }
}
