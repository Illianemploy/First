package com.uktobacco.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uktobacco.data.CompanyHistoryRepository
import com.uktobacco.data.TimelineEvent
import com.uktobacco.ui.theme.UberGreen
import com.uktobacco.ui.theme.UberTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyHistoryScreen(
    brandName: String,
    onBackClick: () -> Unit
) {
    val companyHistory = remember { CompanyHistoryRepository.getCompanyHistory(brandName) }
    val scrollState = rememberScrollState()

    if (companyHistory == null) {
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
                Text("Brand history not found", style = MaterialTheme.typography.titleLarge)
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
                    Text(
                        "Brand History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
            // Header card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box {
                    // Gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        companyHistory.logoColor.copy(alpha = 0.3f),
                                        companyHistory.logoColor.copy(alpha = 0.1f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = companyHistory.logoColor,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = companyHistory.brandName.first().toString(),
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = companyHistory.brandName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = companyHistory.tagline,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = UberTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Key info chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            InfoChip(
                                icon = Icons.Filled.CalendarMonth,
                                label = "Est. ${companyHistory.founded}"
                            )
                            InfoChip(
                                icon = Icons.Filled.LocationOn,
                                label = companyHistory.headquarters.substringAfterLast(", ")
                            )
                        }

                        Text(
                            text = companyHistory.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        DetailRow(label = "Founded by", value = companyHistory.founder)
                        DetailRow(label = "Headquarters", value = companyHistory.headquarters)
                        DetailRow(
                            label = "Years of history",
                            value = "${java.time.Year.now().value - companyHistory.founded} years"
                        )
                    }
                }
            }

            // Timeline section
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Timeline",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = UberGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${companyHistory.timeline.size} milestones",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = UberGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Timeline items
                companyHistory.timeline.forEachIndexed { index, event ->
                    TimelineItem(
                        event = event,
                        isFirst = index == 0,
                        isLast = index == companyHistory.timeline.lastIndex,
                        accentColor = companyHistory.logoColor
                    )
                }
            }

            // Fun facts section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Lightbulb,
                            contentDescription = null,
                            tint = UberGreen
                        )
                        Text(
                            text = "Did You Know?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    companyHistory.facts.forEach { fact ->
                        FactItem(fact)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TimelineItem(
    event: TimelineEvent,
    isFirst: Boolean,
    isLast: Boolean,
    accentColor: Color
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInHorizontally(
            initialOffsetX = { -100 },
            animationSpec = tween(500)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timeline line and dot
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(if (isLast) 80.dp else 120.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Vertical line
                if (!isFirst) {
                    Canvas(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .align(Alignment.TopCenter)
                            .offset(y = (-20).dp)
                    ) {
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.3f),
                                    accentColor.copy(alpha = 0.1f)
                                )
                            ),
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Dot
                val dotSize by animateDpAsState(
                    targetValue = if (event.isKeyMilestone) 20.dp else 14.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dot-size"
                )

                Surface(
                    modifier = Modifier.size(dotSize),
                    color = if (event.isKeyMilestone) accentColor else UberGreen,
                    shape = RoundedCornerShape(50),
                    shadowElevation = if (event.isKeyMilestone) 4.dp else 2.dp
                ) {}

                // Continuing line
                if (!isLast) {
                    Canvas(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .align(Alignment.BottomCenter)
                            .offset(y = 20.dp)
                    ) {
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.1f),
                                    accentColor.copy(alpha = 0.05f)
                                )
                            ),
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Event content
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = if (event.isKeyMilestone)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = if (event.isKeyMilestone) 4.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = event.year.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (event.isKeyMilestone) accentColor else UberGreen
                        )

                        if (event.isKeyMilestone) {
                            Surface(
                                color = accentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "KEY MILESTONE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = UberTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = UberGreen
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FactItem(fact: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = UberGreen,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = fact,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}
