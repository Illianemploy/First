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

            if (product.type == TobaccoType.CIGARETTES) {
                drawCigarettePackage(
                    centerX = centerX,
                    centerY = centerY,
                    rotation = currentRotation,
                    scale = scale,
                    product = product
                )
            } else {
                drawTobaccoPouch(
                    centerX = centerX,
                    centerY = centerY,
                    rotation = currentRotation,
                    scale = scale,
                    product = product
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
    product: TobaccoProduct
) {
    val packageWidth = 120f * scale
    val packageHeight = 180f * scale
    val packageDepth = 40f * scale

    // Calculate 3D perspective
    val rotationRad = Math.toRadians(rotation.toDouble())
    val depthOffset = (packageDepth * sin(rotationRad)).toFloat()

    rotate(rotation, Offset(centerX, centerY)) {
        // Back face (darker)
        drawRoundRect(
            color = CigaretteBlue.copy(alpha = 0.4f),
            topLeft = Offset(centerX - packageWidth / 2 + depthOffset, centerY - packageHeight / 2),
            size = Size(packageWidth, packageHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale, 8f * scale)
        )

        // Side face
        val sideWidth = abs(depthOffset)
        if (sideWidth > 1) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        CigaretteBlue.copy(alpha = 0.6f),
                        CigaretteBlue.copy(alpha = 0.3f)
                    )
                ),
                topLeft = Offset(centerX + packageWidth / 2, centerY - packageHeight / 2),
                size = Size(sideWidth, packageHeight)
            )
        }

        // Front face (main)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    CigaretteBlue,
                    CigaretteBlue.copy(alpha = 0.8f)
                )
            ),
            topLeft = Offset(centerX - packageWidth / 2, centerY - packageHeight / 2),
            size = Size(packageWidth, packageHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * scale, 8f * scale)
        )

        // Top flap
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    CigaretteBlue.copy(alpha = 0.9f),
                    CigaretteBlue.copy(alpha = 0.7f)
                )
            ),
            topLeft = Offset(centerX - packageWidth / 2, centerY - packageHeight / 2 - 20f * scale),
            size = Size(packageWidth, 20f * scale)
        )

        // Brand label on package
        drawRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(centerX - packageWidth / 2 + 10f * scale, centerY - 20f * scale),
            size = Size(packageWidth - 20f * scale, 40f * scale)
        )

        // Detail lines
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(centerX - packageWidth / 2, centerY - packageHeight / 2),
            end = Offset(centerX - packageWidth / 2, centerY + packageHeight / 2),
            strokeWidth = 2f * scale
        )
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(centerX + packageWidth / 2, centerY - packageHeight / 2),
            end = Offset(centerX + packageWidth / 2, centerY + packageHeight / 2),
            strokeWidth = 2f * scale
        )

        // Shine effect
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                start = Offset(centerX - packageWidth / 2, centerY - packageHeight / 2),
                end = Offset(centerX, centerY)
            ),
            topLeft = Offset(centerX - packageWidth / 2, centerY - packageHeight / 2),
            size = Size(packageWidth / 2, packageHeight / 2)
        )
    }
}

private fun DrawScope.drawTobaccoPouch(
    centerX: Float,
    centerY: Float,
    rotation: Float,
    scale: Float,
    product: TobaccoProduct
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

        // Back face
        drawRoundRect(
            color = TobaccoOrange.copy(alpha = 0.4f),
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
                        TobaccoOrange.copy(alpha = 0.6f),
                        TobaccoOrange.copy(alpha = 0.3f)
                    )
                ),
                topLeft = Offset(centerX + pouchWidth / 2, centerY - pouchHeight / 2),
                size = Size(sideWidth, pouchHeight)
            )
        }

        // Front face (main)
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    TobaccoOrange,
                    TobaccoOrange.copy(alpha = 0.8f)
                ),
                center = Offset(centerX, centerY - pouchHeight / 4)
            ),
            topLeft = Offset(centerX - pouchWidth / 2, centerY - pouchHeight / 2),
            size = Size(pouchWidth, pouchHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * scale, 12f * scale)
        )

        // Zipper seal area
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = 0.8f),
                    Color(0xFFFFD700).copy(alpha = 0.6f)
                )
            ),
            topLeft = Offset(centerX - pouchWidth / 2, centerY - pouchHeight / 2),
            size = Size(pouchWidth, 25f * scale)
        )

        // Zipper line
        drawLine(
            color = Color(0xFFB8860B),
            start = Offset(centerX - pouchWidth / 2 + 10f * scale, centerY - pouchHeight / 2 + 12f * scale),
            end = Offset(centerX + pouchWidth / 2 - 10f * scale, centerY - pouchHeight / 2 + 12f * scale),
            strokeWidth = 3f * scale
        )

        // Label area
        drawRoundRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(centerX - pouchWidth / 2 + 15f * scale, centerY - 30f * scale),
            size = Size(pouchWidth - 30f * scale, 60f * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale)
        )

        // Texture lines
        for (i in 0..8) {
            val yPos = centerY - pouchHeight / 2 + 30f * scale + i * 12f * scale
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(centerX - pouchWidth / 2, yPos),
                end = Offset(centerX + pouchWidth / 2, yPos),
                strokeWidth = 1f * scale
            )
        }

        // Highlight
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = Offset(centerX - pouchWidth / 4, centerY - pouchHeight / 4),
                radius = 40f * scale
            ),
            topLeft = Offset(centerX - pouchWidth / 2, centerY - pouchHeight / 2),
            size = Size(pouchWidth / 2, pouchHeight / 2)
        )
    }
}
