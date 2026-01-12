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
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

// Game entities
data class Star(val x: Float, val y: Float, val size: Float, val speed: Float)
data class Enemy(val x: Float, var y: Float, val size: Float, val speed: Float, val type: Int, var timeAlive: Float = 0f)
data class Player(var x: Float, var y: Float, val size: Float = 60f)
data class Bullet(val x: Float, var y: Float, val speed: Float = 20f)
data class CurrencyNotification(val x: Float, val y: Float, val amount: Int, var alpha: Float = 1f, var timeAlive: Float = 0f)

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
    val scaledCost = baseCost * (1.0 + kotlin.math.pow(shopIndex.toDouble(), 0.6))
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
        description = "+15% $M gain",
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
            description = "Gain +50 $M now, +50% costs for 2 shops",
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
            description = "Double $M gain permanently. Disabled: Shields",
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
                            "Your $M: ",
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

    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    var player by remember { mutableStateOf(Player(screenWidth / 2, screenHeight - 150f)) }
    var enemies by remember { mutableStateOf<List<Enemy>>(emptyList()) }
    var bullets by remember { mutableStateOf<List<Bullet>>(emptyList()) }
    var stars by remember { mutableStateOf<List<Star>>(emptyList()) }
    var currencyNotifications by remember { mutableStateOf<List<CurrencyNotification>>(emptyList()) }

    // Game state
    var score by remember { mutableIntStateOf(0) }
    var earnedCurrency by remember { mutableIntStateOf(0) }
    var currentMultiplier by remember { mutableDoubleStateOf(1.0) }
    var isAlive by remember { mutableStateOf(true) }

    // Shop state
    val shopIntervalSeconds = 30
    var shopIndex by remember { mutableIntStateOf(0) }
    var isShopOpen by remember { mutableStateOf(false) }
    var shopAvailableNotification by remember { mutableStateOf(false) }
    var purchasesThisWindow by remember { mutableIntStateOf(0) }
    val maxPurchasesPerWindow = 3

    // Player upgrades
    var playerUpgrades by remember { mutableStateOf(PlayerUpgrades()) }
    var activeRisk by remember { mutableStateOf<RiskState?>(null) }
    var permanentMultiplierBonus by remember { mutableDoubleStateOf(0.0) }

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
        val gameStartTime = System.currentTimeMillis()
        var lastSpawn = 0L
        var lastBulletFire = 0L
        var lastScoreUpdate = gameStartTime
        var lastCurrencyAward = gameStartTime
        var gameTime = 0f

        while (isActive && isAlive) {
            delay(16) // ~60 FPS
            gameTime += 0.016f

            val currentTime = System.currentTimeMillis()
            val survivedMilliseconds = currentTime - gameStartTime

            // Calculate time-based multiplier for rewards
            // Include permanent bonus from survival challenges
            val baseScalingFactor = 0.6 + permanentMultiplierBonus
            currentMultiplier = calculateMultiplier(survivedMilliseconds, baseScalingFactor)

            // Apply player upgrades to multiplier
            val upgradeMultiplier = 1.0 + playerUpgrades.scoreBoostPercent + playerUpgrades.currencyBoostPercent
            currentMultiplier *= upgradeMultiplier

            // Update stars
            stars = stars.map { star ->
                val newY = star.y + star.speed * 3
                if (newY > screenHeight) {
                    star.copy(y = 0f, x = Random.nextFloat() * screenWidth)
                } else {
                    star.copy(y = newY)
                }
            }

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
            // Apply challenge speed modifier if active
            val challengeSpeedMultiplier = if (activeRisk?.type == ShopItemType.SURVIVAL_CHALLENGE && activeRisk?.isActive == true) 1.25f else 1.0f

            enemies = enemies.mapNotNull { enemy ->
                val updatedEnemy = enemy.copy(timeAlive = enemy.timeAlive + 0.016f)

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

            // Update score for survival (base 1 point per 100ms with multiplier)
            if (currentTime - lastScoreUpdate > 100) {
                val survivalPoints = (1 * currentMultiplier).toInt()
                score += survivalPoints
                lastScoreUpdate = currentTime
            }

            // Award currency ($M) periodically with multiplier
            // Award every 1 second to make notifications meaningful
            if (currentTime - lastCurrencyAward > 1000) {
                // Base currency award: 1 $M per second
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

            // Shop timing logic - Opens every 30 seconds
            val survivedSeconds = survivedMilliseconds / 1000
            val currentShopIndex = (survivedSeconds / shopIntervalSeconds).toInt()

            if (currentShopIndex > shopIndex && !isShopOpen) {
                // New shop window available
                shopIndex = currentShopIndex
                shopAvailableNotification = true
                purchasesThisWindow = 0

                // Decrement debt penalty counter
                if (playerUpgrades.debtPenaltyShopsRemaining > 0) {
                    playerUpgrades = playerUpgrades.copy(
                        debtPenaltyShopsRemaining = playerUpgrades.debtPenaltyShopsRemaining - 1
                    )
                }
            }

            // Auto-hide shop notification after 3 seconds
            if (shopAvailableNotification && (survivedSeconds % shopIntervalSeconds) > 3) {
                shopAvailableNotification = false
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

            // Draw currency notifications
            currencyNotifications.forEach { notification ->
                drawCurrencyNotification(notification)
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
                    text = "Earned $M: ",
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

        // Shop available notification
        if (shopAvailableNotification && !isShopOpen) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xDD00FF00) // Green with transparency
                )
            ) {
                Text(
                    text = "🛒 SHOP AVAILABLE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Shop access button (visible when shop is available)
        if (!isShopOpen && shopIndex > 0 && !isAlive.not()) {
            Button(
                onClick = { isShopOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FF00)
                )
            ) {
                Text(
                    text = "SHOP",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
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
                        text = "Your $M: ",
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
                                            text = "$cost $M",
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

/**
 * Draws a currency notification that floats up and fades out.
 * Shows the amount of $M earned in green text.
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
