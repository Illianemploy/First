package com.shooter.space

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Game entities
data class Star(val x: Float, val y: Float, val size: Float, val speed: Float)
data class Enemy(val x: Float, var y: Float, val size: Float, val speed: Float, val type: Int)
data class Player(var x: Float, var y: Float, val size: Float = 60f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var gameStarted by remember { mutableStateOf(false) }

            MaterialTheme {
                if (!gameStarted) {
                    MenuScreen(onStartGame = { gameStarted = true })
                } else {
                    GameScreen(onGameOver = { gameStarted = false })
                }
            }
        }
    }
}

@Composable
fun MenuScreen(onStartGame: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF000033),
                        Color(0xFF000055),
                        Color(0xFF000033)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "SPACE",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Cyan,
                letterSpacing = 8.sp
            )

            Text(
                "SHOOTER",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x44FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "How to Play",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "• Drag your spaceship to avoid enemies\n• Survive to earn points\n• Don't let enemies hit you!",
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }

            Button(
                onClick = onStartGame,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D9FF)
                )
            ) {
                Text(
                    "START GAME",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun GameScreen(onGameOver: () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    var player by remember { mutableStateOf(Player(screenWidth / 2, screenHeight - 150f)) }
    var enemies by remember { mutableStateOf<List<Enemy>>(emptyList()) }
    var stars by remember { mutableStateOf<List<Star>>(emptyList()) }
    var score by remember { mutableIntStateOf(0) }
    var isAlive by remember { mutableStateOf(true) }

    // Initialize stars
    LaunchedEffect(Unit) {
        stars = List(50) {
            Star(
                x = Random.nextFloat() * screenWidth,
                y = Random.nextFloat() * screenHeight,
                size = Random.nextFloat() * 2f + 1f,
                speed = Random.nextFloat() * 2f + 1f
            )
        }
    }

    // Game loop
    LaunchedEffect(isAlive) {
        var lastSpawn = 0L
        var lastScoreUpdate = System.currentTimeMillis()

        while (isActive && isAlive) {
            delay(16) // ~60 FPS

            val currentTime = System.currentTimeMillis()

            // Update stars
            stars = stars.map { star ->
                val newY = star.y + star.speed * 3
                if (newY > screenHeight) {
                    star.copy(y = 0f, x = Random.nextFloat() * screenWidth)
                } else {
                    star.copy(y = newY)
                }
            }

            // Spawn enemies
            if (currentTime - lastSpawn > 1000) {
                val newEnemy = Enemy(
                    x = Random.nextFloat() * (screenWidth - 80f) + 40f,
                    y = -50f,
                    size = 50f,
                    speed = Random.nextFloat() * 3f + 2f,
                    type = Random.nextInt(3)
                )
                enemies = enemies + newEnemy
                lastSpawn = currentTime
            }

            // Update enemies
            enemies = enemies.mapNotNull { enemy ->
                val newY = enemy.y + enemy.speed * 4
                if (newY > screenHeight + 100) null
                else enemy.copy(y = newY)
            }

            // Check collisions
            enemies.forEach { enemy ->
                val dx = player.x - enemy.x
                val dy = player.y - enemy.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                if (distance < (player.size + enemy.size) / 2) {
                    isAlive = false
                }
            }

            // Update score
            if (currentTime - lastScoreUpdate > 100) {
                score += 1
                lastScoreUpdate = currentTime
            }
        }

        if (!isAlive) {
            delay(2000)
            onGameOver()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF000033),
                            Color(0xFF000055),
                            Color(0xFF000033)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        player = player.copy(
                            x = (player.x + dragAmount.x).coerceIn(0f, screenWidth),
                            y = (player.y + dragAmount.y).coerceIn(0f, screenHeight)
                        )
                    }
                }
        ) {
            // Draw stars
            stars.forEach { star ->
                drawCircle(
                    color = Color.White,
                    radius = star.size,
                    center = Offset(star.x, star.y),
                    alpha = 0.8f
                )
            }

            // Draw enemies
            enemies.forEach { enemy ->
                drawEnemy(enemy)
            }

            // Draw player
            if (isAlive) {
                drawPlayer(player)
            } else {
                drawExplosion(player)
            }
        }

        // Score UI
        Text(
            text = "SCORE: $score",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Cyan,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
        )

        if (!isAlive) {
            Text(
                text = "GAME OVER",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// Draw player spaceship
fun DrawScope.drawPlayer(player: Player) {
    val centerX = player.x
    val centerY = player.y
    val size = player.size

    // Draw spaceship as a triangle with glow
    val path = Path().apply {
        moveTo(centerX, centerY - size / 2)
        lineTo(centerX - size / 3, centerY + size / 2)
        lineTo(centerX + size / 3, centerY + size / 2)
        close()
    }

    // Glow effect
    drawPath(
        path = path,
        color = Color.Cyan,
        alpha = 0.3f,
        style = Stroke(width = 8f)
    )

    // Main ship
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(Color.Cyan, Color(0xFF00D9FF))
        )
    )

    // Cockpit
    drawCircle(
        color = Color(0xFF00FFFF),
        radius = size / 6,
        center = Offset(centerX, centerY - size / 6)
    )
}

// Draw enemy
fun DrawScope.drawEnemy(enemy: Enemy) {
    val colors = listOf(
        listOf(Color.Red, Color(0xFFFF6B6B)),
        listOf(Color.Magenta, Color(0xFFFF00FF)),
        listOf(Color.Yellow, Color(0xFFFFAA00))
    )

    val colorPair = colors[enemy.type % colors.size]

    // Draw enemy as a hexagon
    val path = Path().apply {
        val radius = enemy.size / 2
        val centerX = enemy.x
        val centerY = enemy.y

        for (i in 0..5) {
            val angle = (i * 60f - 90f) * Math.PI / 180f
            val x = centerX + (radius * cos(angle)).toFloat()
            val y = centerY + (radius * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    // Glow
    drawPath(
        path = path,
        color = colorPair[0],
        alpha = 0.5f,
        style = Stroke(width = 4f)
    )

    // Main body
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = colorPair,
            center = Offset(enemy.x, enemy.y)
        )
    )
}

// Draw explosion effect
fun DrawScope.drawExplosion(player: Player) {
    for (i in 0..8) {
        val angle = (i * 40f) * Math.PI / 180f
        val radius = player.size * 1.5f
        val x = player.x + (radius * cos(angle)).toFloat()
        val y = player.y + (radius * sin(angle)).toFloat()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Yellow, Color.Red, Color.Transparent)
            ),
            radius = player.size / 3,
            center = Offset(x, y),
            alpha = 0.7f
        )
    }
}
