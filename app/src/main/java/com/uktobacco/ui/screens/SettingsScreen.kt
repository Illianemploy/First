package com.uktobacco.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uktobacco.ui.theme.UberGreen
import com.uktobacco.ui.theme.UberTextSecondary

@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToGlobalPrices: () -> Unit = {},
    onNavigateToAwareness: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {}
) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var autoRefreshEnabled by remember { mutableStateOf(true) }

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

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Manage your preferences",
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
            // Favorites section
            SettingsSection(title = "Favorites") {
                SettingsItem(
                    icon = Icons.Filled.Favorite,
                    title = "My Favorites",
                    description = "View your saved products",
                    onClick = onNavigateToFavorites
                )
            }

            // Anti-Smoking Tools section
            SettingsSection(title = "Anti-Smoking Tools") {
                SettingsItem(
                    icon = Icons.Filled.Person,
                    title = "Your Smoking Profile",
                    description = "Calculate your personal cost of smoking",
                    onClick = onNavigateToProfile
                )

                SettingsItem(
                    icon = Icons.Filled.Public,
                    title = "Global Tobacco Prices",
                    description = "Compare prices and taxes worldwide",
                    onClick = onNavigateToGlobalPrices
                )

                SettingsItem(
                    icon = Icons.Filled.Warning,
                    title = "Global Impact & Awareness",
                    description = "Live statistics on tobacco's toll",
                    onClick = onNavigateToAwareness
                )
            }

            // Preferences section
            SettingsSection(title = "Preferences") {
                SettingsSwitchItem(
                    icon = Icons.Filled.Notifications,
                    title = "Price Alerts",
                    description = "Get notified when prices drop",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )

                SettingsSwitchItem(
                    icon = Icons.Filled.Refresh,
                    title = "Auto Refresh",
                    description = "Automatically update prices",
                    checked = autoRefreshEnabled,
                    onCheckedChange = { autoRefreshEnabled = it }
                )
            }

            // Display section
            SettingsSection(title = "Display") {
                SettingsItem(
                    icon = Icons.Filled.Palette,
                    title = "Theme",
                    description = "Dark mode (default)",
                    onClick = { }
                )

                SettingsItem(
                    icon = Icons.Filled.TextFields,
                    title = "Text Size",
                    description = "Medium",
                    onClick = { }
                )
            }

            // Data section
            SettingsSection(title = "Data & Privacy") {
                SettingsItem(
                    icon = Icons.Filled.CloudSync,
                    title = "Sync Favorites",
                    description = "Back up your favorites",
                    onClick = { }
                )

                SettingsItem(
                    icon = Icons.Filled.DeleteForever,
                    title = "Clear Cache",
                    description = "Free up storage space",
                    onClick = { }
                )
            }

            // About section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "Version",
                    description = "1.0.0",
                    onClick = { }
                )

                SettingsItem(
                    icon = Icons.Filled.Description,
                    title = "Terms & Privacy",
                    description = "View our policies",
                    onClick = { }
                )

                SettingsItem(
                    icon = Icons.Filled.OpenInNew,
                    title = "Open Source Licenses",
                    description = "View licenses",
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = UberTextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = UberGreen
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = UberTextSecondary
                )
            }

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = UberTextSecondary
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = UberGreen
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = UberTextSecondary
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = UberGreen,
                    checkedTrackColor = UberGreen.copy(alpha = 0.5f)
                )
            )
        }
    }
}
