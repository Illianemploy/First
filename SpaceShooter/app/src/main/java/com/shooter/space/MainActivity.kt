package com.shooter.space

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// Game entities
data class Star(val x: Float, val y: Float, val size: Float, val speed: Float, val layer: Int = 0)
data class Enemy(
    val x: Float,
    var y: Float,
    val size: Float,
    val speed: Float,
    val type: Int,
    var timeAlive: Float = 0f,
    var rotation: Float = 0f,
    val spriteVariant: Int = 0  // 0 = left/orange, 1 = right/blue
)
data class Player(var x: Float, var y: Float, val size: Float = 60f, var velocityX: Float = 0f, var velocityY: Float = 0f)
data class Bullet(val x: Float, var y: Float, val speed: Float = 20f)
data class CurrencyNotification(val x: Float, val y: Float, val amount: Int, var alpha: Float = 1f, var timeAlive: Float = 0f)

// Visual effects
data class Particle(val x: Float, val y: Float, var vx: Float, var vy: Float, val color: Color, var alpha: Float = 1f, var timeAlive: Float = 0f, val maxLife: Float = 0.5f, val size: Float = 3f)
data class TrailSegment(val x: Float, val y: Float, var alpha: Float = 1f, var timeAlive: Float = 0f)
data class Nebula(val x: Float, val y: Float, val radius: Float, val color: Color, val speed: Float)

// Interactive objects
data class SpaceCenter(
    var x: Float,
    var y: Float,
    val size: Float = 400f,
    var rotation: Float = 0f,
    var timeAlive: Float = 0f,
    val speed: Float = 1.5f,
    var isActive: Boolean = true
)

// Shop system
enum class ShopItemType {
    FIRE_RATE, BULLET_SPEED, SCORE_BOOST, CURRENCY_BOOST,
    GLASS_CANNON, DEBT_ADVANCE, SURVIVAL_CHALLENGE, EXTREME_MULTIPLIER
}

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val type: ShopItemType,
    val baseCost: Int,
    val isHighRisk: Boolean = false,
    val tier: Int = 1
)

data class PlayerUpgrades(
    var fireRateLevel: Int = 0,
    var bulletSpeedLevel: Int = 0,
    var scoreBoostPercent: Double = 0.0,
    var currencyBoostPercent: Double = 0.0,
    var glassCannonActive: Boolean = false,
    var debtPenaltyShopsRemaining: Int = 0,
    var extremeMultiplierActive: Boolean = false
)

data class RiskState(
    val type: ShopItemType,
    val description: String,
    var timeRemaining: Long, // milliseconds
    var isActive: Boolean = true,
    val onSuccess: () -> Unit,
    val onFailure: () -> Unit
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            var gameStarted by remember { mutableStateOf(false) }
            var highScore by remember { mutableIntStateOf(getHighScore(context)) }
            var totalCurrency by remember { mutableIntStateOf(getCurrency(context)) }

            MaterialTheme {
                if (!gameStarted) {
                    MenuScreen(
                        highScore = highScore,
                        currency = totalCurrency,
                        onStartGame = { gameStarted = true }
                    )
                } else {
                    GameScreen(
                        onGameOver = { score, earnedCurrency ->
                            if (score > highScore) {
                                highScore = score
                                saveHighScore(context, score)
                            }
                            totalCurrency += earnedCurrency
                            saveCurrency(context, totalCurrency)
                            gameStarted = false
                        }
                    )
                }
            }
        }
    }
}

// Persistence functions
private fun getHighScore(context: Context): Int {
    val prefs = context.getSharedPreferences("SpaceShooterPrefs", Context.MODE_PRIVATE)
    return prefs.getInt("high_score", 0)
}

private fun saveHighScore(context: Context, score: Int) {
    val prefs = context.getSharedPreferences("SpaceShooterPrefs", Context.MODE_PRIVATE)
    prefs.edit().putInt("high_score", score).apply()
}

private fun getCurrency(context: Context): Int {
    val prefs = context.getSharedPreferences("SpaceShooterPrefs", Context.MODE_PRIVATE)
    return prefs.getInt("currency", 0)
}

private fun saveCurrency(context: Context, currency: Int) {
    val prefs = context.getSharedPreferences("SpaceShooterPrefs", Context.MODE_PRIVATE)
    prefs.edit().putInt("currency", currency).apply()
}

/**
 * Calculates the time-based multiplier for score and currency rewards.
 * Uses logarithmic scaling to provide diminishing returns over time.
 *
 * Formula: multiplier = 1 + a * ln(1 + t)
 * Where:
 *   t = survivedMilliseconds / 1000.0 (time in seconds)
 *   a = 0.6 (tunable constant)
 *   ln = natural logarithm
 *
 * @param survivedMilliseconds Total time survived in milliseconds
 * @param scalingFactor The 'a' constant (default 0.6)
 * @param maxMultiplier Maximum allowed multiplier (default 10.0)
 * @return The calculated multiplier (clamped to maxMultiplier)
 */
private fun calculateMultiplier(
    survivedMilliseconds: Long,
    scalingFactor: Double = 0.6,
    maxMultiplier: Double = 10.0
): Double {
    val timeInSeconds = survivedMilliseconds / 1000.0
    val multiplier = 1.0 + scalingFactor * ln(1.0 + timeInSeconds)
    return min(multiplier, maxMultiplier)
}

/**
 * Calculates the scaled cost for a shop item based on shop visit index.
 * Uses power scaling to provide smooth price increases.
 *
 * Formula: cost = baseCost * (1 + shopIndex ^ 0.6)
 *
 * @param baseCost Base price of the item
 * @param shopIndex Current shop window index (0-based)
 * @param debtPenalty Additional cost multiplier from debt effects
 * @return Scaled cost
 */
private fun calculateItemCost(baseCost: Int, shopIndex: Int, debtPenalty: Double = 1.0): Int {
    val scaledCost = baseCost * (1.0 + shopIndex.toDouble().pow(0.6))
    return (scaledCost * debtPenalty).toInt()
}

/**
 * Generates a list of shop items for the current shop window.
 * Items scale based on shopIndex and player progress.
 * High-risk items are excluded from early shops (before 60s).
 *
 * @param shopIndex Current shop window index
 * @param survivedSeconds Total survival time in seconds
 * @return List of available shop items
 */
private fun generateShopItems(shopIndex: Int, survivedSeconds: Long): List<ShopItem> {
    val items = mutableListOf<ShopItem>()
    val allowHighRisk = survivedSeconds >= 60

    // Standard upgrades (always available)
    items.add(ShopItem(
        id = "fire_rate",
        name = "Rapid Fire",
        description = "Decrease time between shots by 20%",
        type = ShopItemType.FIRE_RATE,
        baseCost = 15,
        tier = 1
    ))

    items.add(ShopItem(
        id = "bullet_speed",
        name = "Bullet Velocity",
        description = "Increase bullet speed by 30%",
        type = ShopItemType.BULLET_SPEED,
        baseCost = 12,
        tier = 1
    ))

    items.add(ShopItem(
        id = "score_boost",
        name = "Score Amplifier",
        description = "+15% score from all sources",
        type = ShopItemType.SCORE_BOOST,
        baseCost = 20,
        tier = 1
    ))

    items.add(ShopItem(
        id = "currency_boost",
        name = "Currency Magnet",
        description = "+15% \$M gain",
        type = ShopItemType.CURRENCY_BOOST,
        baseCost = 25,
        tier = 1
    ))

    // High-risk items (only after 60s)
    if (allowHighRisk) {
        items.add(ShopItem(
            id = "glass_cannon",
            name = "Glass Cannon",
            description = "+40% gains but -30% health",
            type = ShopItemType.GLASS_CANNON,
            baseCost = 50,
            isHighRisk = true,
            tier = 2
        ))

        items.add(ShopItem(
            id = "debt_advance",
            name = "Debt Advance",
            description = "Gain +50 \$M now, +50% costs for 2 shops",
            type = ShopItemType.DEBT_ADVANCE,
            baseCost = 10,
            isHighRisk = true,
            tier = 2
        ))

        items.add(ShopItem(
            id = "survival_challenge",
            name = "Survival Trial",
            description = "Enemies +25% speed for 30s. Survive = +0.15 to multiplier 'a'",
            type = ShopItemType.SURVIVAL_CHALLENGE,
            baseCost = 40,
            isHighRisk = true,
            tier = 3
        ))

        items.add(ShopItem(
            id = "extreme_multiplier",
            name = "Overcharge (ONE TIME)",
            description = "Double \$M gain permanently. Disabled: Shields",
            type = ShopItemType.EXTREME_MULTIPLIER,
            baseCost = 80,
            isHighRisk = true,
            tier = 3
        ))
    }

    return items
}

@Composable
fun MenuScreen(highScore: Int, currency: Int, onStartGame: () -> Unit) {
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

                    // Display total currency
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Your \$M: ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "$currency",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FF00) // Green for currency
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
fun GameScreen(onGameOver: (Int, Int) -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val context = LocalContext.current

    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Load player sprite sheet
    val playerSprite = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.player_ship).asImageBitmap()
    }

    // Load asteroid sprite sheet (2 columns × 3 rows)
    val asteroidSprite = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.asteroids).asImageBitmap()
    }

    // Load space center sprite
    val spaceCenterSprite = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.space_center02).asImageBitmap()
    }

    var player by remember { mutableStateOf(Player(screenWidth / 2, screenHeight - 150f)) }
    var enemies by remember { mutableStateOf<List<Enemy>>(emptyList()) }
    var bullets by remember { mutableStateOf<List<Bullet>>(emptyList()) }
    var stars by remember { mutableStateOf<List<Star>>(emptyList()) }
    var currencyNotifications by remember { mutableStateOf<List<CurrencyNotification>>(emptyList()) }

    // Visual effects
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var trailSegments by remember { mutableStateOf<List<TrailSegment>>(emptyList()) }
    var nebulae by remember { mutableStateOf<List<Nebula>>(emptyList()) }
    var damageFlashAlpha by remember { mutableFloatStateOf(0f) }

    // Interactive objects
    var spaceCenter by remember { mutableStateOf<SpaceCenter?>(null) }
    var playerInsideShop by remember { mutableStateOf(false) }
    var shopExitTime by remember { mutableLongStateOf(0L) }

    // Game state
    var score by remember { mutableIntStateOf(0) }
    var earnedCurrency by remember { mutableIntStateOf(0) }
    var currentMultiplier by remember { mutableDoubleStateOf(1.0) }
    var isAlive by remember { mutableStateOf(true) }
    var gameTime by remember { mutableFloatStateOf(0f) }

    // Shop state
    val shopRespawnSeconds = 30  // Time after exiting shop before next spawn
    var shopIndex by remember { mutableIntStateOf(0) }
    var isShopOpen by remember { mutableStateOf(false) }
    var purchasesThisWindow by remember { mutableIntStateOf(0) }
    val maxPurchasesPerWindow = 3

    // Player upgrades
    var playerUpgrades by remember { mutableStateOf(PlayerUpgrades()) }
    var activeRisk by remember { mutableStateOf<RiskState?>(null) }
    var permanentMultiplierBonus by remember { mutableDoubleStateOf(0.0) }

    // Initialize stars with parallax layers
    LaunchedEffect(Unit) {
        stars = List(100) {
            val layer = Random.nextInt(3) // 0 = far, 1 = mid, 2 = near
            Star(
                x = Random.nextFloat() * screenWidth,
                y = Random.nextFloat() * screenHeight,
                size = when (layer) {
                    0 -> Random.nextFloat() * 1.5f + 0.5f  // Small, far stars
                    1 -> Random.nextFloat() * 2f + 1f      // Medium stars
                    else -> Random.nextFloat() * 2.5f + 1.5f // Large, near stars
                },
                speed = when (layer) {
                    0 -> Random.nextFloat() * 1f + 0.5f    // Slow
                    1 -> Random.nextFloat() * 2f + 1.5f    // Medium
                    else -> Random.nextFloat() * 3f + 2.5f // Fast
                },
                layer = layer
            )
        }

        // Initialize nebulae (slow-moving background clouds)
        nebulae = List(5) {
            Nebula(
                x = Random.nextFloat() * screenWidth,
                y = Random.nextFloat() * screenHeight,
                radius = Random.nextFloat() * 150f + 100f,
                color = when (Random.nextInt(3)) {
                    0 -> Color(0x33FF00FF) // Purple
                    1 -> Color(0x3300FFFF) // Cyan
                    else -> Color(0x330000FF) // Blue
                },
                speed = Random.nextFloat() * 0.3f + 0.1f
            )
        }
    }

    // Game loop
    LaunchedEffect(isAlive) {
        val gameStartTime = System.currentTimeMillis()
        var lastSpawn = 0L
        var lastBulletFire = 0L
        var lastScoreUpdate = gameStartTime
        var lastCurrencyAward = gameStartTime
        var pausedTime = 0L // Track time spent in shop

        while (isActive && isAlive) {
            delay(16) // ~60 FPS

            val currentTime = System.currentTimeMillis()
            val survivedMilliseconds = currentTime - gameStartTime - pausedTime

            // Handle space center updates separately (needed even when shop is open)
            // Space Center (floating shop) spawning and update logic
            if (!isShopOpen) {
                // Only update space center position when shop is CLOSED
                spaceCenter?.let { center ->
                    val updatedCenter = center.copy(
                        timeAlive = center.timeAlive + 0.016f,
                        y = center.y + center.speed,  // Float downward
                        x = center.x + sin(center.timeAlive * 0.5f) * 0.3f,  // Gentle horizontal sway
                        rotation = sin(center.timeAlive * 0.3f) * 2f  // Subtle rotation (±2°)
                    )

                    // If space center is off-screen (below), remove it
                    if (updatedCenter.y > screenHeight + 300f) {
                        spaceCenter = null
                    } else {
                        spaceCenter = updatedCenter
                    }
                }

                // Spawn logic (only when shop is closed and no space center exists)
                if (spaceCenter == null) {
                    val timeSinceExit = if (shopExitTime == 0L) {
                        // First spawn - immediate
                        999L
                    } else {
                        (currentTime - shopExitTime) / 1000
                    }

                    if (timeSinceExit >= shopRespawnSeconds) {
                        // Spawn new space center at top of screen
                        spaceCenter = SpaceCenter(
                            x = screenWidth / 2,
                            y = -200f,  // Start above screen
                            rotation = 0f,
                            timeAlive = 0f
                        )
                        shopIndex++
                        purchasesThisWindow = 0

                        // Decrement debt penalty counter
                        if (playerUpgrades.debtPenaltyShopsRemaining > 0) {
                            playerUpgrades = playerUpgrades.copy(
                                debtPenaltyShopsRemaining = playerUpgrades.debtPenaltyShopsRemaining - 1
                            )
                        }
                    }
                }
            }

            // Check player collision with space center (even when shop is open to detect exit)
            spaceCenter?.let { center ->
                val dx = player.x - center.x
                val dy = player.y - center.y
                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                val interactionRadius = center.size * 0.325f  // 65% of size radius

                val wasInsideShop = playerInsideShop
                playerInsideShop = distance < interactionRadius

                // Open shop when entering zone
                if (playerInsideShop && !isShopOpen) {
                    isShopOpen = true
                }

                // Auto-close shop when leaving zone (only if shop is open and player moved out)
                if (!playerInsideShop && wasInsideShop && isShopOpen) {
                    isShopOpen = false
                    shopExitTime = currentTime
                }
            }

            // Skip all game updates when shop is open (pause the game)
            if (isShopOpen) {
                pausedTime += 16
                continue
            }

            gameTime += 0.016f

            // Calculate time-based multiplier for rewards
            // Include permanent bonus from survival challenges
            val baseScalingFactor = 0.6 + permanentMultiplierBonus
            currentMultiplier = calculateMultiplier(survivedMilliseconds, baseScalingFactor)

            // Apply player upgrades to multiplier
            val upgradeMultiplier = 1.0 + playerUpgrades.scoreBoostPercent + playerUpgrades.currencyBoostPercent
            currentMultiplier *= upgradeMultiplier

            // Update nebulae (slow-moving background)
            nebulae = nebulae.map { nebula ->
                val newY = nebula.y + nebula.speed
                if (newY > screenHeight + nebula.radius) {
                    nebula.copy(y = -nebula.radius, x = Random.nextFloat() * screenWidth)
                } else {
                    nebula.copy(y = newY)
                }
            }

            // Update stars with parallax layers
            stars = stars.map { star ->
                val layerSpeed = when (star.layer) {
                    0 -> 1f  // Far layer - slow
                    1 -> 2f  // Mid layer
                    else -> 3f // Near layer - fast
                }
                val newY = star.y + star.speed * layerSpeed
                if (newY > screenHeight) {
                    star.copy(y = 0f, x = Random.nextFloat() * screenWidth)
                } else {
                    star.copy(y = newY)
                }
            }

            // Update particles (hit sparks, explosions)
            // Limit to 200 particles for performance
            particles = particles.mapNotNull { particle ->
                val updated = particle.copy(
                    x = particle.x + particle.vx,
                    y = particle.y + particle.vy,
                    vy = particle.vy + 0.2f, // Gravity
                    alpha = (1f - particle.timeAlive / particle.maxLife).coerceIn(0f, 1f),
                    timeAlive = particle.timeAlive + 0.016f
                )
                if (updated.timeAlive > updated.maxLife) null else updated
            }.take(200)

            // Update player trail segments
            // Limit to 30 segments for performance
            trailSegments = trailSegments.mapNotNull { segment ->
                val updated = segment.copy(
                    alpha = (1f - segment.timeAlive / 0.3f).coerceIn(0f, 1f),
                    timeAlive = segment.timeAlive + 0.016f
                )
                if (updated.timeAlive > 0.3f) null else updated
            }.takeLast(30)

            // Add new trail segment for player (every few frames)
            if (gameTime.toInt() % 2 == 0) {
                trailSegments = trailSegments + TrailSegment(
                    x = player.x,
                    y = player.y + player.size / 3
                )
            }

            // Fade out damage flash
            if (damageFlashAlpha > 0f) {
                damageFlashAlpha = (damageFlashAlpha - 0.05f).coerceAtLeast(0f)
            }

            // Apply velocity decay to player (smooth return to center tilt)
            player = player.copy(
                velocityX = player.velocityX * 0.85f,
                velocityY = player.velocityY * 0.85f
            )

            // Auto-fire bullets (with fire rate upgrades)
            val fireRateDelay = (200 * (1.0 - playerUpgrades.fireRateLevel * 0.2).coerceAtLeast(0.2)).toLong()
            if (currentTime - lastBulletFire > fireRateDelay) {
                // Apply bullet speed upgrade
                val bulletSpeed = 20f * (1f + playerUpgrades.bulletSpeedLevel * 0.3f)
                bullets = bullets + Bullet(player.x, player.y - player.size / 2, bulletSpeed)
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
                        3 -> Random.nextFloat() * 2f + 5f // Fast enemy (small asteroid)
                        else -> Random.nextFloat() * 3f + 2f
                    },
                    type = enemyType,
                    timeAlive = 0f,
                    rotation = Random.nextFloat() * 360f,
                    spriteVariant = Random.nextInt(2) // 0 = orange, 1 = blue
                )
                enemies = enemies + newEnemy
                lastSpawn = currentTime
            }

            // Update enemies with different behaviors
            // Apply challenge speed modifier if active
            val challengeSpeedMultiplier = if (activeRisk?.type == ShopItemType.SURVIVAL_CHALLENGE && activeRisk?.isActive == true) 1.25f else 1.0f

            enemies = enemies.mapNotNull { enemy ->
                val updatedEnemy = enemy.copy(
                    timeAlive = enemy.timeAlive + 0.016f,
                    rotation = enemy.rotation + (2f * enemy.type) // Different rotation speeds per type
                )

                when (updatedEnemy.type) {
                    0 -> {
                        // Straight down
                        val newY = updatedEnemy.y + updatedEnemy.speed * 4 * challengeSpeedMultiplier
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(y = newY)
                    }
                    1 -> {
                        // Zigzag pattern
                        val newY = updatedEnemy.y + updatedEnemy.speed * 3 * challengeSpeedMultiplier
                        val zigzagX = updatedEnemy.x + sin(updatedEnemy.timeAlive * 3f) * 5f
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(x = zigzagX.coerceIn(0f, screenWidth), y = newY)
                    }
                    2 -> {
                        // Follow player horizontally
                        val newY = updatedEnemy.y + updatedEnemy.speed * 3 * challengeSpeedMultiplier
                        val targetX = if (player.x > updatedEnemy.x) updatedEnemy.x + 3f else updatedEnemy.x - 3f
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(x = targetX.coerceIn(0f, screenWidth), y = newY)
                    }
                    3 -> {
                        // Fast straight down
                        val newY = updatedEnemy.y + updatedEnemy.speed * 4 * challengeSpeedMultiplier
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(y = newY)
                    }
                    4 -> {
                        // Diagonal swoop
                        val newY = updatedEnemy.y + updatedEnemy.speed * 3 * challengeSpeedMultiplier
                        val swoopX = updatedEnemy.x + cos(updatedEnemy.timeAlive * 2f) * 4f
                        if (newY > screenHeight + 100) null
                        else updatedEnemy.copy(x = swoopX.coerceIn(0f, screenWidth), y = newY)
                    }
                    else -> {
                        val newY = updatedEnemy.y + updatedEnemy.speed * 4 * challengeSpeedMultiplier
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

                        // Award score with multiplier for destroying enemy
                        val basePoints = 10
                        val earnedPoints = (basePoints * currentMultiplier).toInt()
                        score += earnedPoints

                        // Create hit spark particles matching asteroid color
                        val sparkColor = if (enemy.spriteVariant == 0) {
                            Color(0xFFFF6633) // Orange for orange asteroids
                        } else {
                            Color(0xFF6688FF) // Blue for blue asteroids
                        }

                        repeat(8) { i ->
                            val angle = (i * 45f) * Math.PI / 180f
                            val speed = Random.nextFloat() * 3f + 2f
                            particles = particles + Particle(
                                x = enemy.x,
                                y = enemy.y,
                                vx = (cos(angle) * speed).toFloat(),
                                vy = (sin(angle) * speed).toFloat(),
                                color = sparkColor,
                                maxLife = 0.4f,
                                size = Random.nextFloat() * 2f + 2f
                            )
                        }

                        // Create explosion particles matching asteroid color
                        val explosionColors = if (enemy.spriteVariant == 0) {
                            listOf(Color(0xFFFF8844), Color(0xFFFFAA44), Color.Yellow)
                        } else {
                            listOf(Color(0xFF6688FF), Color(0xFF88AAFF), Color.Cyan)
                        }

                        repeat(12) {
                            val angle = Random.nextFloat() * 2f * Math.PI
                            val speed = Random.nextFloat() * 4f + 1f
                            particles = particles + Particle(
                                x = enemy.x,
                                y = enemy.y,
                                vx = (cos(angle) * speed).toFloat(),
                                vy = (sin(angle) * speed).toFloat() - 2f, // Slight upward bias
                                color = explosionColors.random(),
                                maxLife = 0.6f,
                                size = Random.nextFloat() * 3f + 3f
                            )
                        }
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
                    damageFlashAlpha = 1f

                    // Create massive explosion particles
                    repeat(30) {
                        val angle = Random.nextFloat() * 2f * Math.PI
                        val speed = Random.nextFloat() * 6f + 2f
                        particles = particles + Particle(
                            x = player.x,
                            y = player.y,
                            vx = (cos(angle) * speed).toFloat(),
                            vy = (sin(angle) * speed).toFloat() - 3f,
                            color = when (Random.nextInt(4)) {
                                0 -> Color.Red
                                1 -> Color.Yellow
                                2 -> Color(0xFFFF8800)
                                else -> Color.White
                            },
                            maxLife = 1.0f,
                            size = Random.nextFloat() * 4f + 3f
                        )
                    }
                }
            }

            // Update score for survival (base 1 point per 100ms with multiplier)
            if (currentTime - lastScoreUpdate > 100) {
                val survivalPoints = (1 * currentMultiplier).toInt()
                score += survivalPoints
                lastScoreUpdate = currentTime
            }

            // Award currency (\$M) periodically with multiplier
            // Award every 1 second to make notifications meaningful
            if (currentTime - lastCurrencyAward > 1000) {
                // Base currency award: 1 \$M per second
                val baseCurrency = 1
                val awardedCurrency = (baseCurrency * currentMultiplier).toInt()

                if (awardedCurrency > 0) {
                    earnedCurrency += awardedCurrency

                    // Create visual notification at random position near top
                    val notifX = screenWidth * 0.2f + Random.nextFloat() * screenWidth * 0.6f
                    val notifY = screenHeight * 0.15f + Random.nextFloat() * 50f
                    currencyNotifications = currencyNotifications + CurrencyNotification(
                        x = notifX,
                        y = notifY,
                        amount = awardedCurrency
                    )
                }

                lastCurrencyAward = currentTime
            }

            // Update currency notifications (fade out and float up)
            currencyNotifications = currencyNotifications.mapNotNull { notification ->
                val updatedNotif = notification.copy(
                    timeAlive = notification.timeAlive + 0.016f,
                    alpha = (1f - notification.timeAlive / 2f).coerceIn(0f, 1f)
                )

                // Remove notifications after 2 seconds
                if (updatedNotif.timeAlive > 2f) null
                else updatedNotif
            }

            // Update active risk challenges
            activeRisk?.let { risk ->
                risk.timeRemaining -= 16 // Decrease by frame time
                if (risk.timeRemaining <= 0) {
                    if (isAlive && risk.isActive) {
                        // Successfully completed challenge
                        risk.onSuccess()
                    } else {
                        // Failed challenge
                        risk.onFailure()
                    }
                    activeRisk = null
                }
            }
        }

        if (!isAlive) {
            delay(2000)
            onGameOver(score, earnedCurrency)
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
                            y = (player.y + dragAmount.y).coerceIn(0f, screenHeight),
                            velocityX = dragAmount.x,
                            velocityY = dragAmount.y
                        )
                    }
                }
        ) {
            // Draw nebulae (background clouds)
            nebulae.forEach { nebula ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            nebula.color,
                            nebula.color.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(nebula.x, nebula.y),
                        radius = nebula.radius
                    ),
                    radius = nebula.radius,
                    center = Offset(nebula.x, nebula.y)
                )
            }

            // Draw stars with parallax layers
            stars.forEach { star ->
                val alpha = when (star.layer) {
                    0 -> 0.4f  // Far stars - dim
                    1 -> 0.6f  // Mid stars
                    else -> 0.9f // Near stars - bright
                }
                drawCircle(
                    color = Color.White,
                    radius = star.size,
                    center = Offset(star.x, star.y),
                    alpha = alpha
                )
                // Add glow to larger stars
                if (star.layer == 2) {
                    drawCircle(
                        color = Color.White,
                        radius = star.size + 2f,
                        center = Offset(star.x, star.y),
                        alpha = alpha * 0.2f
                    )
                }
            }

            // Draw space center (floating shop)
            spaceCenter?.let { center ->
                drawSpaceCenter(center, spaceCenterSprite)
            }

            // Draw player trail segments
            trailSegments.forEach { segment ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Cyan.copy(alpha = segment.alpha * 0.6f),
                            Color.Cyan.copy(alpha = segment.alpha * 0.3f),
                            Color.Transparent
                        ),
                        radius = 15f
                    ),
                    radius = 15f,
                    center = Offset(segment.x, segment.y)
                )
            }

            // Draw bullets
            bullets.forEach { bullet ->
                drawBullet(bullet)
            }

            // Draw enemies
            enemies.forEach { enemy ->
                drawEnemy(enemy, asteroidSprite)
            }

            // Draw player with thruster effects
            if (isAlive) {
                drawPlayer(player, playerSprite)
                // Draw thrusters
                drawThrusters(player, gameTime)
            } else {
                drawExplosion(player)
            }

            // Draw particles (hit sparks, explosions)
            particles.forEach { particle ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            particle.color.copy(alpha = particle.alpha),
                            particle.color.copy(alpha = particle.alpha * 0.5f),
                            Color.Transparent
                        ),
                        radius = particle.size
                    ),
                    radius = particle.size,
                    center = Offset(particle.x, particle.y)
                )
            }

            // Draw currency notifications
            currencyNotifications.forEach { notification ->
                drawCurrencyNotification(notification)
            }

            // Draw damage flash overlay
            if (damageFlashAlpha > 0f) {
                drawRect(
                    color = Color.Red,
                    alpha = damageFlashAlpha * 0.3f,
                    size = Size(size.width, size.height)
                )
            }
        }

        // Score and Currency UI (Top)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SCORE: $score",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Cyan
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Earned \$M: ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$earnedCurrency",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF00) // Green
                )
            }

            Text(
                text = "x${String.format("%.2f", currentMultiplier)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700), // Gold
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Interaction prompt when near space center
        if (playerInsideShop && !isShopOpen && spaceCenter != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xDD00FF00) // Green with transparency
                )
            ) {
                Text(
                    text = "🛒 ENTERING SHOP...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Shop overlay
        if (isShopOpen) {
            ShopOverlay(
                currentCurrency = earnedCurrency,
                shopIndex = shopIndex,
                shopItems = generateShopItems(shopIndex, (System.currentTimeMillis() - 0) / 1000),
                playerUpgrades = playerUpgrades,
                purchasesRemaining = maxPurchasesPerWindow - purchasesThisWindow,
                onPurchase = { item ->
                    val debtMultiplier = if (playerUpgrades.debtPenaltyShopsRemaining > 0) 1.5 else 1.0
                    val cost = calculateItemCost(item.baseCost, shopIndex, debtMultiplier)

                    if (earnedCurrency >= cost && purchasesThisWindow < maxPurchasesPerWindow) {
                        earnedCurrency -= cost
                        purchasesThisWindow++

                        // Apply upgrade effects
                        when (item.type) {
                            ShopItemType.FIRE_RATE -> playerUpgrades = playerUpgrades.copy(fireRateLevel = playerUpgrades.fireRateLevel + 1)
                            ShopItemType.BULLET_SPEED -> playerUpgrades = playerUpgrades.copy(bulletSpeedLevel = playerUpgrades.bulletSpeedLevel + 1)
                            ShopItemType.SCORE_BOOST -> playerUpgrades = playerUpgrades.copy(scoreBoostPercent = playerUpgrades.scoreBoostPercent + 0.15)
                            ShopItemType.CURRENCY_BOOST -> playerUpgrades = playerUpgrades.copy(currencyBoostPercent = playerUpgrades.currencyBoostPercent + 0.15)
                            ShopItemType.GLASS_CANNON -> {
                                playerUpgrades = playerUpgrades.copy(
                                    glassCannonActive = true,
                                    scoreBoostPercent = playerUpgrades.scoreBoostPercent + 0.4,
                                    currencyBoostPercent = playerUpgrades.currencyBoostPercent + 0.4
                                )
                            }
                            ShopItemType.DEBT_ADVANCE -> {
                                earnedCurrency += 50
                                playerUpgrades = playerUpgrades.copy(debtPenaltyShopsRemaining = 2)
                            }
                            ShopItemType.SURVIVAL_CHALLENGE -> {
                                // Activate challenge: +25% enemy speed for 30s
                                activeRisk = RiskState(
                                    type = ShopItemType.SURVIVAL_CHALLENGE,
                                    description = "Survive 30s with faster enemies",
                                    timeRemaining = 30000,
                                    onSuccess = {
                                        permanentMultiplierBonus += 0.15
                                    },
                                    onFailure = { /* No reward */ }
                                )
                            }
                            ShopItemType.EXTREME_MULTIPLIER -> {
                                playerUpgrades = playerUpgrades.copy(
                                    extremeMultiplierActive = true,
                                    currencyBoostPercent = playerUpgrades.currencyBoostPercent + 1.0 // Double
                                )
                            }
                        }
                    }
                },
                onClose = { isShopOpen = false }
            )
        }

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

@Composable
fun ShopOverlay(
    currentCurrency: Int,
    shopIndex: Int,
    shopItems: List<ShopItem>,
    playerUpgrades: PlayerUpgrades,
    purchasesRemaining: Int,
    onPurchase: (ShopItem) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)) // Semi-transparent black
            .pointerInput(Unit) { /* Block touches */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF001122)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛒 SHOP #${shopIndex + 1}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF00)
                    )
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text("X", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Currency display
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your \$M: ",
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        text = "$currentCurrency",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF00)
                    )
                }

                Text(
                    text = "Purchases remaining: $purchasesRemaining",
                    fontSize = 16.sp,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Shop items list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    shopItems.forEach { item ->
                        val debtMultiplier = if (playerUpgrades.debtPenaltyShopsRemaining > 0) 1.5 else 1.0
                        val cost = calculateItemCost(item.baseCost, shopIndex, debtMultiplier)
                        val canAfford = currentCurrency >= cost
                        val canPurchase = canAfford && purchasesRemaining > 0

                        // Filter out already purchased one-time items
                        val shouldShow = when (item.type) {
                            ShopItemType.EXTREME_MULTIPLIER -> !playerUpgrades.extremeMultiplierActive
                            else -> true
                        }

                        if (shouldShow) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.isHighRisk) Color(0xFF330000) else Color(0xFF003333)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (item.isHighRisk) "⚠️ ${item.name}" else item.name,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isHighRisk) Color(0xFFFF4444) else Color.White
                                        )
                                        Text(
                                            text = item.description,
                                            fontSize = 14.sp,
                                            color = Color(0xFFCCCCCC)
                                        )
                                    }

                                    Button(
                                        onClick = { if (canPurchase) onPurchase(item) },
                                        enabled = canPurchase,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (item.isHighRisk) Color(0xFFFF4444) else Color(0xFF00FF00),
                                            disabledContainerColor = Color.Gray
                                        )
                                    ) {
                                        Text(
                                            text = "$cost \$M",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Draw player spaceship using sprite sheet
fun DrawScope.drawPlayer(player: Player, spriteSheet: ImageBitmap) {
    val centerX = player.x
    val centerY = player.y
    val size = player.size

    // Sprite sheet layout: 2 rows × 5 columns
    val totalColumns = 5
    val totalRows = 2
    val frameWidth = spriteSheet.width / totalColumns
    val frameHeight = spriteSheet.height / totalRows

    // Determine column based on horizontal velocity (tilt)
    // Column 0: fully left, 1: slightly left, 2: center, 3: slightly right, 4: fully right
    val column = when {
        player.velocityX < -5f -> 0      // Fully left
        player.velocityX < -1f -> 1      // Slightly left
        player.velocityX > 5f -> 4       // Fully right
        player.velocityX > 1f -> 3       // Slightly right
        else -> 2                         // Center
    }

    // Determine row based on vertical velocity (thrust state)
    // Row 0: normal/idle, Row 1: thrusting/moving up
    val row = if (player.velocityY < -2f) 1 else 0

    // Calculate source rectangle (which part of sprite sheet to draw)
    val srcOffset = IntOffset(column * frameWidth, row * frameHeight)
    val srcSize = IntSize(frameWidth, frameHeight)

    // Calculate destination position (centered on player position)
    val destOffset = IntOffset((centerX - size / 2).toInt(), (centerY - size / 2).toInt())
    val destSize = IntSize(size.toInt(), size.toInt())

    // Draw the sprite frame
    drawImage(
        image = spriteSheet,
        srcOffset = srcOffset,
        srcSize = srcSize,
        dstOffset = destOffset,
        dstSize = destSize
    )
}

// Draw thruster effects
fun DrawScope.drawThrusters(player: Player, gameTime: Float) {
    val centerX = player.x
    val centerY = player.y
    val size = player.size

    // Pulsing thruster effect
    val pulsePhase = (gameTime * 10f) % 1f
    val thrusterAlpha = 0.6f + pulsePhase * 0.4f
    val thrusterLength = size / 3 + pulsePhase * 10f

    // Left thruster
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFAA00).copy(alpha = thrusterAlpha),
                Color(0xFFFF6600).copy(alpha = thrusterAlpha * 0.6f),
                Color.Transparent
            ),
            radius = 12f
        ),
        radius = 12f,
        center = Offset(centerX - size / 4, centerY + size / 2 + thrusterLength / 2)
    )

    // Right thruster
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFAA00).copy(alpha = thrusterAlpha),
                Color(0xFFFF6600).copy(alpha = thrusterAlpha * 0.6f),
                Color.Transparent
            ),
            radius = 12f
        ),
        radius = 12f,
        center = Offset(centerX + size / 4, centerY + size / 2 + thrusterLength / 2)
    )
}

// Draw bullet with enhanced glow
fun DrawScope.drawBullet(bullet: Bullet) {
    // Outer glow (large)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Cyan.copy(alpha = 0.1f),
                Color.Transparent
            ),
            radius = 18f
        ),
        radius = 18f,
        center = Offset(bullet.x, bullet.y)
    )

    // Middle glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Cyan.copy(alpha = 0.4f),
                Color.Cyan.copy(alpha = 0.1f),
                Color.Transparent
            ),
            radius = 12f
        ),
        radius = 12f,
        center = Offset(bullet.x, bullet.y)
    )

    // Core bullet
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color.Cyan,
                Color(0xFF00D9FF)
            ),
            center = Offset(bullet.x, bullet.y),
            radius = 6f
        ),
        radius = 6f,
        center = Offset(bullet.x, bullet.y)
    )

    // Bright center
    drawCircle(
        color = Color.White,
        radius = 3f,
        center = Offset(bullet.x, bullet.y),
        alpha = 0.9f
    )
}

// Draw enemy using asteroid sprite sheet
fun DrawScope.drawEnemy(enemy: Enemy, asteroidSprite: ImageBitmap) {
    val centerX = enemy.x
    val centerY = enemy.y
    val size = enemy.size

    // Asteroid sprite sheet layout: 2 columns × 3 rows
    // Columns: 0 = orange, 1 = blue
    // Rows: 0 = small (fast), 1 = medium, 2 = large (slow)
    val totalColumns = 2
    val totalRows = 3
    val frameWidth = asteroidSprite.width / totalColumns
    val frameHeight = asteroidSprite.height / totalRows

    // Map enemy type to asteroid size row
    val row = when (enemy.type) {
        0 -> 1  // Straight Diver: Medium
        1 -> 1  // Zigzagger: Medium
        2 -> 2  // Follower: Large
        3 -> 0  // Speed Demon: Small (fastest)
        4 -> 2  // Swooper: Large
        else -> 1
    }

    // Use sprite variant (0 = orange, 1 = blue)
    val column = enemy.spriteVariant

    // Calculate source rectangle
    val srcOffset = IntOffset(column * frameWidth, row * frameHeight)
    val srcSize = IntSize(frameWidth, frameHeight)

    // Calculate destination position (centered on enemy position)
    val destOffset = IntOffset((centerX - size / 2).toInt(), (centerY - size / 2).toInt())
    val destSize = IntSize(size.toInt(), size.toInt())

    // Draw outer glow for visual consistency
    val glowColor = if (column == 0) Color(0xFFFF8844) else Color(0xFF8888FF)
    drawCircle(
        color = glowColor,
        radius = size / 2 + 8f,
        center = Offset(centerX, centerY),
        alpha = 0.2f
    )

    // Draw inner glow
    drawCircle(
        color = glowColor,
        radius = size / 2 + 4f,
        center = Offset(centerX, centerY),
        alpha = 0.3f
    )

    // Draw the asteroid sprite
    // Note: Rotation is handled by the sprite's visual appearance
    // For true rotation, we'd need to use rotate() transform
    drawImage(
        image = asteroidSprite,
        srcOffset = srcOffset,
        srcSize = srcSize,
        dstOffset = destOffset,
        dstSize = destSize
    )
}

// Draw enhanced explosion effect
fun DrawScope.drawExplosion(player: Player) {
    // Central bright flash
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color.Yellow,
                Color(0xFFFF8800),
                Color.Transparent
            ),
            radius = player.size * 1.2f
        ),
        radius = player.size * 1.2f,
        center = Offset(player.x, player.y),
        alpha = 0.9f
    )

    // Explosion bursts
    for (i in 0..11) {
        val angle = (i * 30f) * Math.PI / 180f
        val radius = player.size * 1.8f
        val x = player.x + (radius * cos(angle)).toFloat()
        val y = player.y + (radius * sin(angle)).toFloat()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Yellow.copy(alpha = 0.8f),
                    Color(0xFFFF6600).copy(alpha = 0.6f),
                    Color.Red.copy(alpha = 0.3f),
                    Color.Transparent
                )
            ),
            radius = player.size / 2.5f,
            center = Offset(x, y)
        )
    }

    // Outer shockwave
    drawCircle(
        color = Color(0xFFFF8800),
        radius = player.size * 2.2f,
        center = Offset(player.x, player.y),
        alpha = 0.2f,
        style = Stroke(width = 4f)
    )
}

/**
 * Draws a currency notification that floats up and fades out.
 * Shows the amount of \$M earned in green text.
 *
 * @param notification The currency notification to draw
 */
fun DrawScope.drawCurrencyNotification(notification: CurrencyNotification) {
    val floatOffset = notification.timeAlive * 30f // Float upward over time

    // Draw background glow
    drawCircle(
        color = Color(0xFF00FF00),
        radius = 40f,
        center = Offset(notification.x, notification.y - floatOffset),
        alpha = notification.alpha * 0.2f
    )

    // Draw text outline for visibility (approximate with multiple offset draws)
    val textColor = Color(0xFF00FF00) // Green
    val outlineColor = Color.Black
    val text = "+${notification.amount} \$M"

    // Note: DrawScope doesn't have native text drawing, so we're drawing
    // a visual representation using circles and shapes
    // In a production app, you'd use androidx.compose.ui.text.drawText or a custom Canvas

    // Draw a simple visual indicator (green glowing circle with size indicating amount)
    val indicatorSize = 15f + (notification.amount * 2f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF00FF00), Color(0xFF00FF00).copy(alpha = 0.3f), Color.Transparent),
            center = Offset(notification.x, notification.y - floatOffset),
            radius = indicatorSize
        ),
        radius = indicatorSize,
        center = Offset(notification.x, notification.y - floatOffset),
        alpha = notification.alpha
    )

    // Draw amount as multiple dots (visual representation)
    for (i in 0 until notification.amount.coerceAtMost(5)) {
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(
                notification.x - 10f + i * 5f,
                notification.y - floatOffset + 10f
            ),
            alpha = notification.alpha
        )
    }
}

// Draw floating space center (shop)
fun DrawScope.drawSpaceCenter(center: SpaceCenter, spriteSheet: ImageBitmap) {
    val centerX = center.x
    val centerY = center.y
    val size = center.size

    // Draw outer glow for shop indicator
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF00FF00).copy(alpha = 0.15f),
                Color(0xFF00FF00).copy(alpha = 0.05f),
                Color.Transparent
            ),
            radius = size / 2 + 40f
        ),
        radius = size / 2 + 40f,
        center = Offset(centerX, centerY),
        alpha = 0.8f + sin(center.timeAlive * 2f) * 0.2f  // Pulsing glow
    )

    // Draw inner glow
    drawCircle(
        color = Color(0xFF00FF00),
        radius = size / 2 + 20f,
        center = Offset(centerX, centerY),
        alpha = 0.15f
    )

    // Calculate destination position (centered)
    val destOffset = IntOffset((centerX - size / 2).toInt(), (centerY - size / 2).toInt())
    val destSize = IntSize(size.toInt(), size.toInt())

    // Draw the space center sprite
    // Note: Using rotation would require drawImage with transformation
    // For now, keeping it simple with just the sprite
    drawImage(
        image = spriteSheet,
        dstOffset = destOffset,
        dstSize = destSize
    )

    // Draw interaction zone indicator (subtle circle)
    val interactionRadius = size * 0.325f  // 65% of size
    drawCircle(
        color = Color(0xFF00FF00),
        radius = interactionRadius,
        center = Offset(centerX, centerY),
        alpha = 0.1f + sin(center.timeAlive * 3f) * 0.05f,  // Subtle pulse
        style = Stroke(width = 2f)
    )
}
