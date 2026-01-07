package com.uktobacco.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uktobacco.data.SmokingProfile
import com.uktobacco.ui.theme.UberGreen
import com.uktobacco.ui.theme.UberTextSecondary
import java.text.NumberFormat
import java.util.*

@Composable
fun SmokingProfileScreen(
    currentProfile: SmokingProfile?,
    onProfileSaved: (SmokingProfile) -> Unit
) {
    var cigarettesPerDay by remember { mutableStateOf(currentProfile?.cigarettesPerDay?.toString() ?: "") }
    var pricePerPack by remember { mutableStateOf(currentProfile?.pricePerPack?.toString() ?: "14.00") }
    var showResults by remember { mutableStateOf(currentProfile != null) }

    val calculatedProfile = remember(cigarettesPerDay, pricePerPack) {
        val cigs = cigarettesPerDay.toIntOrNull() ?: 0
        val price = pricePerPack.toDoubleOrNull() ?: 14.00
        if (cigs > 0) {
            SmokingProfile.calculate(cigs, price)
        } else null
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
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = UberGreen
                    )
                    Text(
                        text = "Your Smoking Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Calculate your true cost of smoking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = UberTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Input section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Your Habits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = cigarettesPerDay,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                cigarettesPerDay = it
                                if (it.toIntOrNull() != null && it.toInt() > 0) {
                                    showResults = true
                                }
                            }
                        },
                        label = { Text("Cigarettes per day") },
                        leadingIcon = {
                            Icon(Icons.Filled.SmokingRooms, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = UberGreen,
                            focusedLabelColor = UberGreen
                        )
                    )

                    OutlinedTextField(
                        value = pricePerPack,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                pricePerPack = it
                            }
                        },
                        label = { Text("Price per pack (20)") },
                        leadingIcon = {
                            Text("£", style = MaterialTheme.typography.titleMedium)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = UberGreen,
                            focusedLabelColor = UberGreen
                        )
                    )

                    Button(
                        onClick = {
                            calculatedProfile?.let { onProfileSaved(it) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = calculatedProfile != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UberGreen
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Calculate Impact",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Results section
            AnimatedVisibility(
                visible = showResults && calculatedProfile != null,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                calculatedProfile?.let { profile ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Monthly cost card
                        ImpactCard(
                            icon = Icons.Filled.CalendarMonth,
                            title = "Monthly Cost",
                            value = "£${String.format("%.2f", profile.monthlyExpenditure)}",
                            subtitle = "${String.format("%.1f", profile.packsPerMonth)} packs per month",
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Yearly cost card
                        ImpactCard(
                            icon = Icons.Filled.TrendingUp,
                            title = "Yearly Cost",
                            value = "£${formatCurrency(profile.yearlyExpenditure)}",
                            subtitle = "Based on current smoking habits",
                            color = MaterialTheme.colorScheme.error
                        )

                        // Lifetime projection (assuming 30 years)
                        val lifetimeCost = profile.yearlyExpenditure * 30
                        ImpactCard(
                            icon = Icons.Filled.Warning,
                            title = "30-Year Projection",
                            value = "£${formatCurrency(lifetimeCost)}",
                            subtitle = "If you continue smoking",
                            color = Color(0xFFFF6B6B)
                        )

                        // Health impact
                        val cigarettesPerYear = (profile.cigarettesPerDay * 365).toLong()
                        val minutesLost = cigarettesPerYear * 11
                        val hoursLost = minutesLost / 60
                        val daysLost = hoursLost / 24

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.HealthAndSafety,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        "Health Impact (Yearly)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                HealthStatRow("Cigarettes", formatNumber(cigarettesPerYear))
                                HealthStatRow("Life expectancy reduced", "${daysLost} days")
                                HealthStatRow("Tar inhaled", "${String.format("%.1f", cigarettesPerYear * 11.0 / 1000.0)}g")
                                HealthStatRow("Chemicals inhaled", formatNumber(cigarettesPerYear * 7000))
                            }
                        }

                        // Motivational message
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = UberGreen.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = UberGreen
                                )
                                Text(
                                    "These calculations show the true cost of smoking. Explore the Global Prices and Awareness sections to learn more about how smoking impacts you and the world.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
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
private fun ImpactCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UberTextSecondary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = UberTextSecondary
                )
            }
        }
    }
}

@Composable
private fun HealthStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

private fun formatCurrency(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.UK).format(value.toInt())
}

private fun formatNumber(value: Long): String {
    return NumberFormat.getNumberInstance(Locale.UK).format(value)
}
