package com.shooter.space

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Game entities
data class Star(val x: Float, val y: Float, val size: Float, val speed: Float)
data class Enemy(val x: Float, var y: Float, val size: Float, val speed: Float, val type: Int, var timeAlive: Float = 0f)
data class Player(var x: Float, var y: Float, val size: Float = 60f)
data class Bullet(val x: Float, var y: Float, val speed: Float = 20f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var gameStarted by remember { mutableStateOf(false) }
            var highScore by remember { mutableIntStateOf(getHighScore(context)) }

            MaterialTheme {
                if (!gameStarted) {
                    MenuScreen(
                        highScore = highScore,
                        onStartGame = { gameStarted = true }
                    )
                } else {
                    GameScreen(
                        onGameOver = { score ->
                            if (score > highScore) {
                                highScore = score
                                saveHighScore(context, score)
                            }
                            gameStarted = false
                        }
                    )
                }
            }
        }
    }
}

// High score persistence
private fun getHighScore(context: Context): Int {
    val prefs = context.getSharedPreferences("SpaceShooterPrefs", Context.MODE_PRIVATE)
    return prefs.getInt("high_score", 0)
}

private fun saveHighScore(context: Context, score: Int) {
    val prefs = context.getSharedPreferences("SpaceShooterPrefs", Context.MODE_PRIVATE)
    prefs.edit().putInt("high_score", score).apply()
}

@Composable
fun MenuScreen(highScore: Int, onStartGame: () -> Unit) {
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
                        "• Drag to move\n• Auto-fire bullets\n• Shoot enemies to earn points\n• Survive as long as possible!",
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    if (highScore > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "HIGH SCORE: $highScore",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
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
fun GameScreen(onGameOver: (Int) -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    var player by remember { mutableStateOf(Player(screenWidth / 2, screenHeight - 150f)) }
    var enemies by remember { mutableStateOf<List<Enemy>>(emptyList()) }
    var bullets by remember { mutableStateOf<List<Bullet>>(emptyList()) }
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
        var lastBulletFire = 0L
        var lastScoreUpdate = System.currentTimeMillis()
        var gameTime = 0f

        while (isActive && isAlive) {
            delay(16) // ~60 FPS
            gameTime += 0.016f

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

            // Auto-fire bullets
            if (currentTime - lastBulletFire > 200) {
                bullets = bullets + Bullet(player.x, player.y - player.size / 2)
                lastBulletFire = currentTime
            }

            // Update bullets
            bullets = bullets.mapNotNull { bullet ->
                val newY = bullet.y - bullet.speed
                if (newY < -10) null
                else bullet.copy(y = newY)
            }

            // Spawn enemies with different types
            if (currentTime - lastSpawn > 1000) {
                val enemyType = Random.nextInt(5) // 5 different enemy types
                val newEnemy = Enemy(
                    x = Random.nextFloat() * (screenWidth - 80f) + 40f,
                    y = -50f,
                    size = 50f,
                    speed = when (enemyType) {
                        3 -> Random.nextFloat() * 2f + 5f // Fast enemy
                        else -> Random.nextFloat() * 3f + 2f
                    },
                    type = enemyType,
                    timeAlive = 0f
                )
                enemies = enemies + newEnemy
                lastSpawn = currentTime
            }

            // Update enemies with different behaviors
            enemies = enemies.mapNotNull { enemy ->
                val updatedEnemy = enemy.copy(timeAlive = enemy.timeAlive + 0.016f)

                when (updatedEnemy.type) {
                    0 -> {
                        // Straight down
                        val newY = updatedEnemy.y + updatedEnemy.speed * 4
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(y = newY)
                    }
                    1 -> {
                        // Zigzag pattern
                        val newY = updatedEnemy.y + updatedEnemy.speed * 3
                        val zigzagX = updatedEnemy.x + sin(updatedEnemy.timeAlive * 3f) * 5f
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(x = zigzagX.coerceIn(0f, screenWidth), y = newY)
                    }
                    2 -> {
                        // Follow player horizontally
                        val newY = updatedEnemy.y + updatedEnemy.speed * 3
                        val targetX = if (player.x > updatedEnemy.x) updatedEnemy.x + 3f else updatedEnemy.x - 3f
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(x = targetX.coerceIn(0f, screenWidth), y = newY)
                    }
                    3 -> {
                        // Fast straight down
                        val newY = updatedEnemy.y + updatedEnemy.speed * 4
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(y = newY)
                    }
                    4 -> {
                        // Diagonal swoop
                        val newY = updatedEnemy.y + updatedEnemy.speed * 3
                        val swoopX = updatedEnemy.x + cos(updatedEnemy.timeAlive * 2f) * 4f
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(x = swoopX.coerceIn(0f, screenWidth), y = newY)
                    }
                    else -> {
                        val newY = updatedEnemy.y + updatedEnemy.speed * 4
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(y = newY)
                    }
                }
            }

            // Check bullet-enemy collisions
            val enemiesToRemove = mutableSetOf<Enemy>()
            val bulletsToRemove = mutableSetOf<Bullet>()

            bullets.forEach { bullet ->
                enemies.forEach { enemy ->
                    val dx = bullet.x - enemy.x
                    val dy = bullet.y - enemy.y
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (distance < enemy.size / 2) {
                        enemiesToRemove.add(enemy)
                        bulletsToRemove.add(bullet)
                        score += 10 // Points for destroying enemy
                    }
                }
            }

            enemies = enemies.filter { it !in enemiesToRemove }
            bullets = bullets.filter { it !in bulletsToRemove }

            // Check player-enemy collisions
            enemies.forEach { enemy ->
                val dx = player.x - enemy.x
                val dy = player.y - enemy.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                if (distance < (player.size + enemy.size) / 2) {
                    isAlive = false
                }
            }

            // Update score for survival
            if (currentTime - lastScoreUpdate > 100) {
                score += 1
                lastScoreUpdate = currentTime
            }
        }

        if (!isAlive) {
            delay(2000)
            onGameOver(score)
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

            // Draw bullets
            bullets.forEach { bullet ->
                drawBullet(bullet)
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

// Draw bullet
fun DrawScope.drawBullet(bullet: Bullet) {
    // Draw glowing bullet
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color.Cyan, Color(0xFF00D9FF)),
            center = Offset(bullet.x, bullet.y),
            radius = 8f
        ),
        radius = 8f,
        center = Offset(bullet.x, bullet.y)
    )

    // Outer glow
    drawCircle(
        color = Color.Cyan,
        radius = 12f,
        center = Offset(bullet.x, bullet.y),
        alpha = 0.3f
    )
}

// Draw enemy
fun DrawScope.drawEnemy(enemy: Enemy) {
    val colors = listOf(
        listOf(Color.Red, Color(0xFFFF6B6B)),           // Type 0: Straight
        listOf(Color.Magenta, Color(0xFFFF00FF)),       // Type 1: Zigzag
        listOf(Color(0xFF00FF00), Color(0xFF88FF88)),  // Type 2: Follower (green)
        listOf(Color(0xFFFF4500), Color(0xFFFF8C00)),  // Type 3: Fast (orange)
        listOf(Color.Yellow, Color(0xFFFFAA00))         // Type 4: Swoop
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
