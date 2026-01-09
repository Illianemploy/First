package com.uktobacco.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uktobacco.TobaccoProduct
import com.uktobacco.TobaccoType
import com.uktobacco.data.BrandDesign
import com.uktobacco.data.BrandDesigns
import com.uktobacco.data.LogoStyle
import com.uktobacco.ui.theme.CigaretteBlue
import com.uktobacco.ui.theme.TobaccoOrange
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Product3DViewer(
    product: TobaccoProduct,
    modifier: Modifier = Modifier
) {
    var rotation by remember { mutableStateOf(0f) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Auto-rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val autoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auto-rotation"
    )

    var isUserInteracting by remember { mutableStateOf(false) }
    val currentRotation = if (isUserInteracting) rotation else autoRotation

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, pan, zoom, rotationDelta ->
                        isUserInteracting = true
                        rotation += rotationDelta
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                )
            }
    ) {
        // 3D Product rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            val centerX = size.width / 2 + offsetX
            val centerY = size.height / 2 + offsetY

            // Get brand-specific design
            val brandDesign = BrandDesigns.getDesign(product.name, product.brand)

            if (product.type == TobaccoType.CIGARETTES) {
                drawCigarettePackage(
                    centerX = centerX,
                    centerY = centerY,
                    rotation = currentRotation,
                    scale = scale,
                    product = product,
                    design = brandDesign
                )
            } else {
                drawTobaccoPouch(
                    centerX = centerX,
                    centerY = centerY,
                    rotation = currentRotation,
                    scale = scale,
                    product = product,
                    design = brandDesign
                )
            }
        }

        // Controls hint
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🔄 Rotate • 🔍 Pinch to zoom • 👆 Drag to move",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "Scale: ${String.format("%.1f", scale)}x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        // Brand label
        Text(
            text = product.brand,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-100).dp)
        )
    }
}

private fun DrawScope.drawCigarettePackage(
    centerX: Float,
    centerY: Float,
    rotation: Float,
    scale: Float,
    product: TobaccoProduct,
    design: BrandDesign
) {
    val packageWidth = 120f * scale
    val packageHeight = 180f * scale
    val packageDepth = 40f * scale

    // Calculate 3D perspective
    val rotationRad = Math.toRadians(rotation.toDouble())
    val depthOffset = (packageDepth * sin(rotationRad)).toFloat()

    rotate(rotation, Offset(centerX, centerY)) {
        // Shadow beneath package
        drawOval(
            color = Color.Black.copy(alpha = 0.15f),
            topLeft = Offset(centerX - packageWidth / 2, centerY + packageHeight / 2 + 5f * scale),
            size = Size(packageWidth, 15f * scale)
        )

        // Back face (darker shade of primary color)
        drawRoundRect(
            color = design.primaryColor.copy(alpha = 0.4f),
            topLeft = Offset(centerX - packageWidth / 2 + depthOffset, centerY - packageHeight / 2),
            size = Size(packageWidth, packageHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale, 8f * scale)
        )

        // Side face with gradient
        val sideWidth = abs(depthOffset)
        if (sideWidth > 1) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        design.primaryColor.copy(alpha = 0.7f),
                        design.primaryColor.copy(alpha = 0.4f)
                    )
                ),
                topLeft = Offset(centerX + packageWidth / 2, centerY - packageHeight / 2),
                size = Size(sideWidth, packageHeight)
            )
        }

        // Front face (main) with brand-specific gradient
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    design.primaryColor,
                    design.primaryColor.copy(alpha = 0.85f),
                    design.primaryColor.copy(alpha = 0.75f)
                )
            ),
            topLeft = Offset(centerX - packageWidth / 2, centerY - packageHeight / 2),
            size = Size(packageWidth, packageHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale, 8f * scale)
        )

        // Top flap (lid)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    design.accentColor.copy(alpha = 0.9f),
                    design.primaryColor.copy(alpha = 0.8f)
                )
            ),
            topLeft = Offset(centerX - packageWidth / 2, centerY - packageHeight / 2 - 20f * scale),
            size = Size(packageWidth, 20f * scale)
        )

        // Brand-specific logo area based on LogoStyle
        when (design.logoStyle) {
            LogoStyle.CHEVRON -> {
                // Marlboro-style chevron
                drawChevronLogo(centerX, centerY, packageWidth, packageHeight, scale, design)
            }
            LogoStyle.CIRCLE -> {
                // Lucky Strike-style circle
                drawCircleLogo(centerX, centerY, packageWidth, packageHeight, scale, design)
            }
            LogoStyle.PREMIUM_GOLD -> {
                // Benson & Hedges premium gold style
                drawPremiumGoldLogo(centerX, centerY, packageWidth, packageHeight, scale, design)
            }
            LogoStyle.VINTAGE -> {
                // Classic vintage styling
                drawVintageLogo(centerX, centerY, packageWidth, packageHeight, scale, design)
            }
            LogoStyle.STANDARD -> {
                // Standard rectangular branding
                drawStandardLogo(centerX, centerY, packageWidth, packageHeight, scale, design)
            }
        }

        // Accent stripe at top
        drawRect(
            color = design.accentColor,
            topLeft = Offset(centerX - packageWidth / 2, centerY - packageHeight / 2 + 10f * scale),
            size = Size(packageWidth, 3f * scale)
        )

        // Accent stripe at bottom
        drawRect(
            color = design.accentColor,
            topLeft = Offset(centerX - packageWidth / 2, centerY + packageHeight / 2 - 15f * scale),
            size = Size(packageWidth, 3f * scale)
        )

        // Edge highlights for depth
        drawLine(
            color = Color.White.copy(alpha = 0.4f),
            start = Offset(centerX - packageWidth / 2, centerY - packageHeight / 2),
            end = Offset(centerX - packageWidth / 2, centerY + packageHeight / 2),
            strokeWidth = 2f * scale
        )

        // Premium glossy shine effect
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = Offset(centerX - packageWidth / 4, centerY - packageHeight / 3),
                radius = 60f * scale
            ),
            topLeft = Offset(centerX - packageWidth / 2, centerY - packageHeight / 2),
            size = Size(packageWidth, packageHeight / 2)
        )

        // Product name area hint (subtle)
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.05f),
            topLeft = Offset(centerX - packageWidth / 2 + 8f * scale, centerY + packageHeight / 4),
            size = Size(packageWidth - 16f * scale, 25f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * scale, 4f * scale)
        )
    }
}

// Helper functions for different logo styles
private fun DrawScope.drawChevronLogo(
    centerX: Float, centerY: Float,
    packageWidth: Float, packageHeight: Float,
    scale: Float, design: BrandDesign
) {
    // Marlboro-style chevron (inverted V)
    val chevronTop = centerY - packageHeight / 6
    val chevronBottom = centerY + packageHeight / 8
    val chevronWidth = packageWidth - 30f * scale

    // White chevron background
    drawRect(
        color = design.secondaryColor.copy(alpha = 0.95f),
        topLeft = Offset(centerX - chevronWidth / 2, chevronTop),
        size = Size(chevronWidth, chevronBottom - chevronTop)
    )

    // Accent lines forming chevron shape
    val path = Path().apply {
        moveTo(centerX - chevronWidth / 2, chevronTop)
        lineTo(centerX, chevronBottom)
        lineTo(centerX + chevronWidth / 2, chevronTop)
    }
    drawPath(
        path = path,
        color = design.accentColor,
        style = Stroke(width = 3f * scale)
    )
}

private fun DrawScope.drawCircleLogo(
    centerX: Float, centerY: Float,
    packageWidth: Float, packageHeight: Float,
    scale: Float, design: BrandDesign
) {
    // Lucky Strike-style circle
    val circleRadius = 35f * scale

    // Outer circle (red)
    drawCircle(
        color = design.secondaryColor,
        radius = circleRadius,
        center = Offset(centerX, centerY - packageHeight / 8)
    )

    // Inner circle (darker)
    drawCircle(
        color = design.accentColor,
        radius = circleRadius - 8f * scale,
        center = Offset(centerX, centerY - packageHeight / 8)
    )
}

private fun DrawScope.drawPremiumGoldLogo(
    centerX: Float, centerY: Float,
    packageWidth: Float, packageHeight: Float,
    scale: Float, design: BrandDesign
) {
    // Premium gold frame
    val frameWidth = packageWidth - 25f * scale
    val frameHeight = 55f * scale

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                design.secondaryColor,
                design.primaryColor,
                design.secondaryColor
            )
        ),
        topLeft = Offset(centerX - frameWidth / 2, centerY - frameHeight / 2),
        size = Size(frameWidth, frameHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale, 8f * scale)
    )

    // Inner border
    drawRoundRect(
        color = design.accentColor,
        topLeft = Offset(centerX - frameWidth / 2 + 3f * scale, centerY - frameHeight / 2 + 3f * scale),
        size = Size(frameWidth - 6f * scale, frameHeight - 6f * scale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale),
        style = Stroke(width = 2f * scale)
    )
}

private fun DrawScope.drawVintageLogo(
    centerX: Float, centerY: Float,
    packageWidth: Float, packageHeight: Float,
    scale: Float, design: BrandDesign
) {
    // Vintage ornate style
    val logoWidth = packageWidth - 20f * scale
    val logoHeight = 60f * scale

    // Background panel
    drawRoundRect(
        color = design.secondaryColor.copy(alpha = 0.9f),
        topLeft = Offset(centerX - logoWidth / 2, centerY - logoHeight / 2),
        size = Size(logoWidth, logoHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale)
    )

    // Decorative corners
    val cornerSize = 12f * scale
    listOf(
        Offset(centerX - logoWidth / 2 + 5f * scale, centerY - logoHeight / 2 + 5f * scale),
        Offset(centerX + logoWidth / 2 - 5f * scale, centerY - logoHeight / 2 + 5f * scale),
        Offset(centerX - logoWidth / 2 + 5f * scale, centerY + logoHeight / 2 - 5f * scale),
        Offset(centerX + logoWidth / 2 - 5f * scale, centerY + logoHeight / 2 - 5f * scale)
    ).forEach { corner ->
        drawCircle(
            color = design.accentColor,
            radius = 3f * scale,
            center = corner
        )
    }
}

private fun DrawScope.drawStandardLogo(
    centerX: Float, centerY: Float,
    packageWidth: Float, packageHeight: Float,
    scale: Float, design: BrandDesign
) {
    // Standard rectangular brand area
    val logoWidth = packageWidth - 20f * scale
    val logoHeight = 45f * scale

    drawRoundRect(
        color = design.secondaryColor.copy(alpha = 0.95f),
        topLeft = Offset(centerX - logoWidth / 2, centerY - logoHeight / 2),
        size = Size(logoWidth, logoHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale)
    )

    // Accent border
    drawRoundRect(
        color = design.accentColor,
        topLeft = Offset(centerX - logoWidth / 2, centerY - logoHeight / 2),
        size = Size(logoWidth, logoHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale),
        style = Stroke(width = 2f * scale)
    )
}

private fun DrawScope.drawTobaccoPouch(
    centerX: Float,
    centerY: Float,
    rotation: Float,
    scale: Float,
    product: TobaccoProduct,
    design: BrandDesign
) {
    val pouchWidth = 150f * scale
    val pouchHeight = 140f * scale
    val pouchDepth = 30f * scale

    // Calculate 3D perspective
    val rotationRad = Math.toRadians(rotation.toDouble())
    val depthOffset = (pouchDepth * sin(rotationRad)).toFloat()

    rotate(rotation, Offset(centerX, centerY)) {
        // Shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.2f),
            topLeft = Offset(centerX - pouchWidth / 2, centerY + pouchHeight / 2 + 10f * scale),
            size = Size(pouchWidth, 20f * scale)
        )

        // Back face - brand-specific color
        drawRoundRect(
            color = design.primaryColor.copy(alpha = 0.4f),
            topLeft = Offset(centerX - pouchWidth / 2 + depthOffset, centerY - pouchHeight / 2),
            size = Size(pouchWidth, pouchHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * scale, 12f * scale)
        )

        // Side depth
        val sideWidth = abs(depthOffset)
        if (sideWidth > 1) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        design.primaryColor.copy(alpha = 0.6f),
                        design.primaryColor.copy(alpha = 0.3f)
                    )
                ),
                topLeft = Offset(centerX + pouchWidth / 2, centerY - pouchHeight / 2),
                size = Size(sideWidth, pouchHeight)
            )
        }

        // Front face (main) - brand colors
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    design.primaryColor,
                    design.primaryColor.copy(alpha = 0.85f),
                    design.primaryColor.copy(alpha = 0.7f)
                ),
                center = Offset(centerX, centerY - pouchHeight / 4)
            ),
            topLeft = Offset(centerX - pouchWidth / 2, centerY - pouchHeight / 2),
            size = Size(pouchWidth, pouchHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * scale, 12f * scale)
        )

        // Zipper seal area - accent color
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    design.accentColor.copy(alpha = 0.8f),
                    design.accentColor.copy(alpha = 0.6f)
                )
            ),
            topLeft = Offset(centerX - pouchWidth / 2, centerY - pouchHeight / 2),
            size = Size(pouchWidth, 25f * scale)
        )

        // Zipper line - darker accent
        drawLine(
            color = design.accentColor.copy(alpha = 0.9f),
            start = Offset(centerX - pouchWidth / 2 + 10f * scale, centerY - pouchHeight / 2 + 12f * scale),
            end = Offset(centerX + pouchWidth / 2 - 10f * scale, centerY - pouchHeight / 2 + 12f * scale),
            strokeWidth = 3f * scale
        )

        // Label area - secondary color
        drawRoundRect(
            color = design.secondaryColor.copy(alpha = 0.95f),
            topLeft = Offset(centerX - pouchWidth / 2 + 15f * scale, centerY - 30f * scale),
            size = Size(pouchWidth - 30f * scale, 60f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale)
        )

        // Label border
        drawRoundRect(
            color = design.accentColor,
            topLeft = Offset(centerX - pouchWidth / 2 + 15f * scale, centerY - 30f * scale),
            size = Size(pouchWidth - 30f * scale, 60f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale),
            style = Stroke(width = 2f * scale)
        )

        // Texture lines with brand accent
        for (i in 0..8) {
            val yPos = centerY - pouchHeight / 2 + 30f * scale + i * 12f * scale
            drawLine(
                color = design.secondaryColor.copy(alpha = 0.15f),
                start = Offset(centerX - pouchWidth / 2, yPos),
                end = Offset(centerX + pouchWidth / 2, yPos),
                strokeWidth = 1f * scale
            )
        }

        // Premium shine effect
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = Offset(centerX - pouchWidth / 4, centerY - pouchHeight / 4),
                radius = 50f * scale
            ),
            topLeft = Offset(centerX - pouchWidth / 2, centerY - pouchHeight / 2),
            size = Size(pouchWidth / 2, pouchHeight / 2)
        )
    }
}
