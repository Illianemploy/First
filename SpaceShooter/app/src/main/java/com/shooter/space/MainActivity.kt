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
import androidx.compose.foundation.gestures.detectTapGestures
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
data class Star(var x: Float, var y: Float, val size: Float, val speed: Float, val layer: Int = 0)

// Enemy size tiers for visual and hitbox scaling
enum class SizeTier {
    SMALL,   // 40-60px
    MEDIUM,  // 70-90px
    LARGE,   // 100-130px
    ELITE    // 130-160px
}

// Enemy visual styles (procedural shapes + sprites)
enum class EnemyVisualStyle {
    SHAPE_TRIANGLE,
    SHAPE_SQUARE,
    SHAPE_RECT,
    SHAPE_CIRCLE,
    SPRITE_EVIL_SHIP_001
}

data class Enemy(
    var x: Float,
    var y: Float,
    var size: Float,  // Dynamic size based on sizeTier
    val speed: Float,
    val type: Int,
    var timeAlive: Float = 0f,
    var rotation: Float = 0f,
    val spriteVariant: Int = 0,  // Used for color variation
    var health: Int = 1,
    val maxHealth: Int = 1,
    var behaviorController: EnemyBehaviorController? = null,
    val sizeTier: SizeTier = SizeTier.MEDIUM,
    val visualStyle: EnemyVisualStyle = EnemyVisualStyle.SHAPE_CIRCLE
)

data class Player(var x: Float, var y: Float, val size: Float = 60f, var velocityX: Float = 0f, var velocityY: Float = 0f)
data class Bullet(val x: Float, var y: Float, val speed: Float = 20f)

// ============================================================================
// ENEMY VISUAL SYSTEM - Configuration & Tuning
// ============================================================================

/**
 * TUNING CONSTANTS - Adjust these to balance visual variety and gameplay
 *
 * Size ranges (min-max px):
 *   SMALL: 40-60px    (60% spawn rate)
 *   MEDIUM: 70-90px   (25% spawn rate)
 *   LARGE: 100-130px  (12% spawn rate)
 *   ELITE: 130-160px  (3% spawn rate)
 *
 * Visual style distribution:
 *   Shapes: 80% (triangle, square, rect, circle - equal distribution)
 *   Sprites: 20% (currently only evil_enemy_spaceship_001.png)
 *
 * To add new sprites:
 *   1. Add PNG to SpaceShooter/app/src/main/res/drawable/
 *   2. Add new enum: EnemyVisualStyle.SPRITE_YOUR_NAME
 *   3. Update EnemyRenderer.loadSprites() to cache bitmap
 *   4. Update drawEnemy() switch statement to render it
 *   5. Update randomVisualStyle() to include it in rotation
 */

/**
 * Get render size for a given size tier.
 * Returns a random size within the tier's range for variety.
 */
fun getSizeForTier(tier: SizeTier): Float {
    return when (tier) {
        SizeTier.SMALL -> Random.nextFloat() * 20f + 40f   // 40-60px
        SizeTier.MEDIUM -> Random.nextFloat() * 20f + 70f  // 70-90px
        SizeTier.LARGE -> Random.nextFloat() * 30f + 100f  // 100-130px
        SizeTier.ELITE -> Random.nextFloat() * 30f + 130f  // 130-160px
    }
}

/**
 * Randomly select a size tier based on spawn distribution.
 * Adjust percentages below to change enemy size variety.
 */
fun randomSizeTier(): SizeTier {
    val roll = Random.nextFloat() * 100f
    return when {
        roll < 60f -> SizeTier.SMALL   // 60% spawn rate
        roll < 85f -> SizeTier.MEDIUM  // 25% spawn rate
        roll < 97f -> SizeTier.LARGE   // 12% spawn rate
        else -> SizeTier.ELITE         // 3% spawn rate
    }
}

/**
 * Randomly select a visual style for enemy rendering.
 * Adjust percentages to change sprite vs shape distribution.
 */
fun randomVisualStyle(): EnemyVisualStyle {
    val roll = Random.nextFloat() * 100f
    return if (roll < 20f) {
        // 20% chance of sprite enemy
        EnemyVisualStyle.SPRITE_EVIL_SHIP_001
    } else {
        // 80% chance of procedural shape (equal distribution)
        when (Random.nextInt(4)) {
            0 -> EnemyVisualStyle.SHAPE_TRIANGLE
            1 -> EnemyVisualStyle.SHAPE_SQUARE
            2 -> EnemyVisualStyle.SHAPE_RECT
            else -> EnemyVisualStyle.SHAPE_CIRCLE
        }
    }
}

/**
 * Get health bonus for elite enemies (optional scaling).
 */
fun getHealthBonusForTier(tier: SizeTier, baseHealth: Int): Int {
    return when (tier) {
        SizeTier.ELITE -> baseHealth + 1  // Elite gets +1 health
        else -> baseHealth
    }
}

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

// ============================================================================
// POWER-UP SYSTEM - Data Models (Pure Kotlin)
// ============================================================================

/**
 * Power-up types corresponding to Bonuses-0001.png columns (5x5 grid).
 * Column mapping (left to right):
 *   col 0 = HEALTH
 *   col 1 = SHIELD
 *   col 2 = FIREPOWER
 *   col 3 = MULTISHOT
 *   col 4 = DOUBLE_REWARD
 */
enum class PowerUpType {
    HEALTH,        // Instant HP restore
    SHIELD,        // Timed damage reduction/absorption
    FIREPOWER,     // Timed increased damage/fire rate
    MULTISHOT,     // Timed multiple simultaneous bullets per shot
    DOUBLE_REWARD  // Timed 2x score and currency
}

/**
 * World power-up entity (collectible in game world).
 * @param tier 0-4, affects sprite row and optionally strength/duration
 */
data class WorldPowerUp(
    val id: Long,
    val type: PowerUpType,
    var x: Float,
    var y: Float,
    var tier: Int,            // 0-4: sprite row + optional strength scaling
    var alive: Boolean = true
)

/**
 * Active power-up effect on player.
 * @param remainingMs null for instant effects (HEALTH), countdown for timed effects
 */
data class ActiveEffect(
    val type: PowerUpType,
    var tier: Int,            // 0-4: visual + strength scaling
    var remainingMs: Long?    // null = instant (HEALTH), else countdown timer
)

/**
 * PowerUpSystem - Pure Kotlin gameplay logic
 * Manages world power-ups and active effects with pause-safe timers.
 *
 * Duration formulas (ms):
 *   SHIELD: 10000 + tier*2000
 *   FIREPOWER: 10000 + tier*2000
 *   MULTISHOT: 8000 + tier*2000
 *   DOUBLE_REWARD: 12000 + tier*2000
 *
 * Multishot bonus (simultaneous bullets per shot):
 *   tier 0 => +1 (2 total)
 *   tier 1 => +2 (3 total)
 *   tier 2 => +3 (4 total)
 *   tier 3 => +4 (5 total)
 *   tier 4 => +5 (6 total)
 */
class PowerUpSystem {
    private var nextId: Long = 0L
    val worldPowerUps = mutableListOf<WorldPowerUp>()
    val activeEffects = mutableMapOf<PowerUpType, ActiveEffect>()

    /**
     * Spawn a new power-up in the world.
     */
    fun spawnPowerUp(type: PowerUpType, tier: Int, x: Float, y: Float) {
        val clampedTier = tier.coerceIn(0, 4)
        worldPowerUps.add(
            WorldPowerUp(
                id = nextId++,
                type = type,
                x = x,
                y = y,
                tier = clampedTier,
                alive = true
            )
        )
    }

    /**
     * Update world power-ups and active effect timers.
     * @param dtMs delta time in milliseconds (0 when paused/shop open)
     */
    fun update(dtMs: Long) {
        if (dtMs <= 0) return  // Pause-safe: no updates when not running

        // Update timed effect countdowns
        activeEffects.values.removeIf { effect ->
            effect.remainingMs?.let { remaining ->
                effect.remainingMs = remaining - dtMs
                (remaining - dtMs) <= 0  // Remove if expired
            } ?: false  // Keep instant effects (should not be in map)
        }
    }

    /**
     * Check if player collides with any power-up and apply effect.
     * @param playerX player center X
     * @param playerY player center Y
     * @param playerRadius player collision radius
     * @param currentHealth for HEALTH instant effect
     * @param maxHealth for HEALTH clamping
     * @return new health value (only changed for HEALTH pickups)
     */
    fun handlePickupIfColliding(
        playerX: Float,
        playerY: Float,
        playerRadius: Float,
        currentHealth: Int,
        maxHealth: Int
    ): Int {
        var newHealth = currentHealth
        val pickupRadius = 30f  // Power-up collision radius

        worldPowerUps.forEach { powerUp ->
            if (!powerUp.alive) return@forEach

            val dx = playerX - powerUp.x
            val dy = playerY - powerUp.y
            val distanceSquared = dx * dx + dy * dy
            val collisionRadiusSquared = (playerRadius + pickupRadius) * (playerRadius + pickupRadius)

            if (distanceSquared < collisionRadiusSquared) {
                // Collision detected - apply pickup
                powerUp.alive = false
                newHealth = applyPickup(powerUp.type, powerUp.tier, currentHealth, maxHealth)
            }
        }

        // Clean up dead power-ups
        worldPowerUps.removeIf { !it.alive }

        return newHealth
    }

    /**
     * Apply power-up effect using stacking rules.
     * Stacking policy:
     *   - remainingMs = max(current, new)  (refresh to longer)
     *   - tier = max(current, new)         (keep strongest visual)
     *
     * @return new health value (only changed for HEALTH)
     */
    private fun applyPickup(
        type: PowerUpType,
        tier: Int,
        currentHealth: Int,
        maxHealth: Int
    ): Int {
        return when (type) {
            PowerUpType.HEALTH -> {
                // Instant effect: restore health
                val healthRestore = 1 + tier  // tier 0=>1 HP, tier 4=>5 HP
                (currentHealth + healthRestore).coerceAtMost(maxHealth)
            }

            PowerUpType.SHIELD -> {
                val duration = 10000L + tier * 2000L
                stackTimedEffect(type, tier, duration)
                currentHealth
            }

            PowerUpType.FIREPOWER -> {
                val duration = 10000L + tier * 2000L
                stackTimedEffect(type, tier, duration)
                currentHealth
            }

            PowerUpType.MULTISHOT -> {
                val duration = 8000L + tier * 2000L
                stackTimedEffect(type, tier, duration)
                currentHealth
            }

            PowerUpType.DOUBLE_REWARD -> {
                val duration = 12000L + tier * 2000L
                stackTimedEffect(type, tier, duration)
                currentHealth
            }
        }
    }

    /**
     * Stack or refresh a timed effect.
     */
    private fun stackTimedEffect(type: PowerUpType, tier: Int, duration: Long) {
        val existing = activeEffects[type]
        if (existing != null) {
            // Refresh: take max of timers and tiers
            existing.remainingMs = maxOf(existing.remainingMs ?: 0L, duration)
            existing.tier = maxOf(existing.tier, tier)
        } else {
            // New effect
            activeEffects[type] = ActiveEffect(
                type = type,
                tier = tier,
                remainingMs = duration
            )
        }
    }

    // Query helpers
    fun hasEffect(type: PowerUpType): Boolean = activeEffects.containsKey(type)
    fun effectTier(type: PowerUpType): Int = activeEffects[type]?.tier ?: 0
    fun remainingMs(type: PowerUpType): Long? = activeEffects[type]?.remainingMs

    /**
     * Get multishot bullet count bonus.
     * tier 0=>+1, tier 1=>+2, tier 2=>+3, tier 3=>+4, tier 4=>+5
     */
    fun getMultishotBulletCount(): Int {
        if (!hasEffect(PowerUpType.MULTISHOT)) return 1
        val tier = effectTier(PowerUpType.MULTISHOT)
        return 1 + (tier + 1)  // tier 0=>2, tier 1=>3, ..., tier 4=>6
    }

    /**
     * Get firepower damage multiplier.
     * tier 0=>1.2x, tier 4=>2.0x (linear scaling)
     */
    fun getFirepowerMultiplier(): Float {
        if (!hasEffect(PowerUpType.FIREPOWER)) return 1.0f
        val tier = effectTier(PowerUpType.FIREPOWER)
        return 1.2f + (tier * 0.2f)  // tier 0=>1.2x, tier 1=>1.4x, ..., tier 4=>2.0x
    }

    /**
     * Get shield damage reduction multiplier.
     * tier 0=>0.5x (50% reduction), tier 4=>0.1x (90% reduction)
     */
    fun getShieldDamageMultiplier(): Float {
        if (!hasEffect(PowerUpType.SHIELD)) return 1.0f
        val tier = effectTier(PowerUpType.SHIELD)
        return 0.5f - (tier * 0.1f)  // tier 0=>0.5x, tier 1=>0.4x, ..., tier 4=>0.1x
    }

    /**
     * Get reward multiplier (score and currency).
     */
    fun getRewardMultiplier(): Float {
        return if (hasEffect(PowerUpType.DOUBLE_REWARD)) 2.0f else 1.0f
    }
}

// Debug overlay metrics (only available in debug builds)
data class DebugMetrics(
    var fps: Int = 0,
    var avgFrameTime: Float = 0f,
    var worstFrameTime: Float = 0f,
    var enemyCount: Int = 0,
    var bulletCount: Int = 0,
    var spawnInterval: Long = 0L,
    var speedMultiplier: Float = 0f,
    var enemyHealth: Int = 0,
    var score: Int = 0,
    var currency: Int = 0,
    var difficultyLevel: Int = 0
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

// ============================================================================
// GAME ARCHITECTURE - Core Systems
// ============================================================================

/**
 * Enemy behavior states for modular AI system.
 * Each state represents a distinct behavior pattern.
 */
enum class EnemyState {
    APPROACH,  // Move directly toward player
    STRAFE,    // Move sideways while maintaining distance
    IDLE,      // Move at constant speed (default behavior)
    FLEE       // Move away from player
}

/**
 * Weapon modifier system for data-driven weapon customization.
 * Modifiers are stackable and affect weapon behavior without visual changes.
 */
data class WeaponModifier(
    val id: String,
    val fireRateMultiplier: Float = 1f,      // <1 = faster, >1 = slower
    val projectileCount: Int = 1,             // Number of bullets per shot
    val spreadAngle: Float = 0f,              // Angle spread in degrees (0 = straight)
    val isPiercing: Boolean = false,          // Bullets go through enemies
    val isChaining: Boolean = false,          // Bullets chain between nearby enemies
    val chainRange: Float = 100f,             // Range for chain effect
    val maxChains: Int = 3                    // Max number of chain bounces
)

/**
 * Current weapon statistics, computed from base stats + active modifiers.
 */
data class WeaponStats(
    val baseFireRate: Long = 200L,           // Base time between shots (ms)
    val activeModifiers: List<WeaponModifier> = emptyList()
) {
    // Computed fire rate after all modifiers
    val effectiveFireRate: Long
        get() {
            val multiplier = activeModifiers.fold(1f) { acc, mod -> acc * mod.fireRateMultiplier }
            return (baseFireRate * multiplier).toLong()
        }

    // Total bullets fired per shot
    val totalProjectileCount: Int
        get() = activeModifiers.sumOf { it.projectileCount }.coerceAtLeast(1)

    // Maximum spread angle
    val maxSpreadAngle: Float
        get() = activeModifiers.maxOfOrNull { it.spreadAngle } ?: 0f

    // Check if any modifier has piercing
    val hasPiercing: Boolean
        get() = activeModifiers.any { it.isPiercing }

    // Check if any modifier has chaining
    val hasChaining: Boolean
        get() = activeModifiers.any { it.isChaining }

    // Maximum chain range
    val chainRange: Float
        get() = activeModifiers.filter { it.isChaining }.maxOfOrNull { it.chainRange } ?: 0f

    // Maximum chain count
    val maxChains: Int
        get() = activeModifiers.filter { it.isChaining }.maxOfOrNull { it.maxChains } ?: 0
}

/**
 * Dynamic difficulty configuration.
 * All parameters are tunable for game balance.
 */
data class DifficultyConfig(
    // Spawn rate scaling
    val baseSpawnInterval: Long = 1000L,      // Base time between spawns (ms)
    val minSpawnInterval: Long = 300L,        // Minimum spawn interval (ms)
    val spawnScalingFactor: Float = 0.95f,    // Multiplier per difficulty level

    // Enemy speed scaling
    val baseSpeedMultiplier: Float = 1.0f,    // Base enemy speed multiplier
    val maxSpeedMultiplier: Float = 2.5f,     // Maximum speed multiplier
    val speedScalingFactor: Float = 0.05f,    // Speed increase per difficulty level

    // Enemy health scaling
    val baseHealth: Int = 1,                  // Base enemy health (hits to kill)
    val maxHealth: Int = 5,                   // Maximum enemy health
    val healthScalingInterval: Int = 5,       // Difficulty levels per health increase

    // Difficulty progression
    val scorePerLevel: Int = 500,             // Score required per difficulty level
    val timePerLevel: Long = 30000L           // Time (ms) per difficulty level (whichever comes first)
)

/**
 * Difficulty scaler that adjusts game parameters based on progression.
 * Uses both time survived and score to determine difficulty level.
 */
class DifficultyScaler(private val config: DifficultyConfig = DifficultyConfig()) {
    private var currentLevel: Int = 0

    /**
     * Update difficulty based on time and score.
     * Returns current difficulty level.
     */
    fun update(survivedMillis: Long, currentScore: Int): Int {
        val timeLevel = (survivedMillis / config.timePerLevel).toInt()
        val scoreLevel = currentScore / config.scorePerLevel

        // Use whichever progression is further
        currentLevel = maxOf(timeLevel, scoreLevel)
        return currentLevel
    }

    /**
     * Get current spawn interval based on difficulty.
     */
    fun getSpawnInterval(): Long {
        val scaled = (config.baseSpawnInterval * config.spawnScalingFactor.pow(currentLevel)).toLong()
        return scaled.coerceAtLeast(config.minSpawnInterval)
    }

    /**
     * Get current enemy speed multiplier based on difficulty.
     */
    fun getSpeedMultiplier(): Float {
        val increase = config.speedScalingFactor * currentLevel
        return (config.baseSpeedMultiplier + increase).coerceAtMost(config.maxSpeedMultiplier)
    }

    /**
     * Get current enemy health based on difficulty.
     */
    fun getEnemyHealth(): Int {
        val healthLevel = currentLevel / config.healthScalingInterval
        return (config.baseHealth + healthLevel).coerceAtMost(config.maxHealth)
    }

    /**
     * Get current difficulty level (useful for UI or debugging).
     */
    fun getCurrentLevel(): Int = currentLevel

    /**
     * Reset difficulty to initial state.
     */
    fun reset() {
        currentLevel = 0
    }
}

/**
 * Enemy behavior controller.
 * Manages state transitions and behavior execution.
 * Optimized to avoid per-frame allocations.
 */
class EnemyBehaviorController {
    private var currentState: EnemyState = EnemyState.IDLE
    private var stateTimer: Float = 0f
    private var nextStateChange: Float = Random.nextFloat() * 3f + 2f  // 2-5 seconds

    // Reusable variables to avoid allocations
    private var lastPlayerX: Float = 0f
    private var lastPlayerY: Float = 0f
    private var cachedDistance: Float = 0f
    private var updateTimer: Float = 0f
    private val stateUpdateInterval: Float = 0.2f  // Update state every 200ms, not every frame

    /**
     * Update enemy behavior state based on conditions.
     * Uses time-slicing - only updates state every 200ms to reduce CPU load.
     * Returns the current state after update.
     */
    fun update(
        enemy: Enemy,
        playerX: Float,
        playerY: Float,
        deltaTime: Float,
        health: Int,
        maxHealth: Int
    ): EnemyState {
        stateTimer += deltaTime
        updateTimer += deltaTime

        // Time-sliced state updates: only evaluate transitions every 200ms
        if (updateTimer >= stateUpdateInterval) {
            updateTimer = 0f

            // Cache player position and distance for this update cycle
            lastPlayerX = playerX
            lastPlayerY = playerY

            val dx = enemy.x - playerX
            val dy = enemy.y - playerY
            // Use squared distance to avoid expensive sqrt
            val distanceSquared = dx * dx + dy * dy
            cachedDistance = kotlin.math.sqrt(distanceSquared)

            // State transition logic
            when (currentState) {
                EnemyState.IDLE -> {
                    // Randomly switch to other states or when player is close
                    if (stateTimer >= nextStateChange) {
                        currentState = when {
                            distanceSquared < 40000f && Random.nextFloat() > 0.5f -> EnemyState.STRAFE  // 200px
                            health < maxHealth * 0.3f -> EnemyState.FLEE
                            else -> EnemyState.APPROACH
                        }
                        resetStateTimer()
                    }
                }
                EnemyState.APPROACH -> {
                    // Switch to strafe if too close or flee if low health
                    if (distanceSquared < 22500f || stateTimer >= nextStateChange) {  // 150px
                        currentState = if (health < maxHealth * 0.3f) {
                            EnemyState.FLEE
                        } else {
                            EnemyState.STRAFE
                        }
                        resetStateTimer()
                    }
                }
                EnemyState.STRAFE -> {
                    // Return to approach or flee based on health
                    if (stateTimer >= nextStateChange) {
                        currentState = if (health < maxHealth * 0.3f) {
                            EnemyState.FLEE
                        } else {
                            EnemyState.APPROACH
                        }
                        resetStateTimer()
                    }
                }
                EnemyState.FLEE -> {
                    // Return to idle when far enough or health recovered
                    if (distanceSquared > 160000f || stateTimer >= nextStateChange) {  // 400px
                        currentState = EnemyState.IDLE
                        resetStateTimer()
                    }
                }
            }
        }

        return currentState
    }

    /**
     * Apply movement to enemy based on current state.
     * Directly modifies enemy position to avoid Pair allocation.
     * Uses cached distance to avoid recalculation.
     */
    fun applyMovement(
        enemy: Enemy,
        playerX: Float,
        playerY: Float,
        baseSpeed: Float,
        speedMultiplier: Float
    ) {
        val dx = playerX - enemy.x
        val dy = playerY - enemy.y
        val finalSpeed = baseSpeed * speedMultiplier

        when (currentState) {
            EnemyState.APPROACH -> {
                // Move toward player (use cached distance if available)
                val dist = if (cachedDistance > 0f) cachedDistance else kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist > 0) {
                    enemy.x += (dx / dist) * finalSpeed * 0.5f
                    enemy.y += (dy / dist) * finalSpeed * 0.5f
                } else {
                    enemy.y += finalSpeed
                }
            }
            EnemyState.STRAFE -> {
                // Move sideways relative to player
                val dist = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val perpX = -dy / dist
                val perpY = dx / dist
                enemy.x += perpX * finalSpeed * 0.7f
                enemy.y += finalSpeed
            }
            EnemyState.FLEE -> {
                // Move away from player
                val dist = if (cachedDistance > 0f) cachedDistance else kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist > 0) {
                    enemy.x += (-dx / dist) * finalSpeed * 0.3f
                    enemy.y += (-dy / dist) * finalSpeed * 0.3f
                } else {
                    enemy.y += finalSpeed
                }
            }
            EnemyState.IDLE -> {
                // Default downward movement
                enemy.y += finalSpeed
            }
        }
    }

    private fun resetStateTimer() {
        stateTimer = 0f
        nextStateChange = Random.nextFloat() * 3f + 2f
    }
}

// Parallax background system
data class ScrollingLayer(
    val bitmap: ImageBitmap,
    var y1: Float,  // First instance position
    var y2: Float   // Second instance position for seamless looping
)

/**
 * Manages a 2-layer parallax background system.
 * Layer 1 (Foreground): Static planet surface with transparency
 * Layer 2 (Background): Scrolling starfield that loops seamlessly
 */
class ParallaxBackgroundManager(
    private val context: Context,
    private val screenWidth: Float,
    private val screenHeight: Float
) {
    // Layer 1: Static foreground (planet surface)
    private var foregroundLayer: ImageBitmap? = null

    // Layer 2: Scrolling background (stars)
    private var scrollingLayer: ScrollingLayer? = null

    // Slow scroll speed for background depth effect
    private val scrollSpeed: Float = 0.8f // pixels per frame (~48 px/sec at 60 FPS)

    /**
     * Loads a background bitmap from resources by ID.
     * Scales the bitmap to fit screen dimensions while maintaining aspect ratio.
     */
    private fun loadBackground(resourceId: Int): ImageBitmap {
        // Load with efficient options
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 // Support transparency
            inScaled = false
        }

        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

        // Scale bitmap to screen dimensions (maintain aspect ratio, crop if needed)
        val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
            bitmap,
            screenWidth.toInt(),
            screenHeight.toInt(),
            true // Use bilinear filtering for smooth scaling
        )

        // Recycle original if different from scaled
        if (bitmap != scaledBitmap) {
            bitmap.recycle()
        }

        return scaledBitmap.asImageBitmap()
    }

    /**
     * Initializes the parallax background system.
     * Loads the static foreground layer and two instances of the scrolling background.
     */
    fun initialize() {
        // Load static foreground layer (planet surface)
        foregroundLayer = loadBackground(R.drawable.parallax_background_001)

        // Load scrolling background layer (stars) with two instances for seamless looping
        val starfieldBitmap = loadBackground(R.drawable.parallax_background_002)
        scrollingLayer = ScrollingLayer(
            bitmap = starfieldBitmap,
            y1 = 0f,              // First instance at screen top
            y2 = -screenHeight    // Second instance directly above
        )
    }

    /**
     * Updates the scrolling background layer position.
     * The foreground layer remains static.
     */
    fun update(deltaTime: Float) {
        scrollingLayer?.let { layer ->
            // Scroll both instances downward
            layer.y1 += scrollSpeed
            layer.y2 += scrollSpeed

            // When first instance scrolls completely off-screen, move it back to top
            if (layer.y1 >= screenHeight) {
                layer.y1 = layer.y2 - screenHeight
            }

            // When second instance scrolls completely off-screen, move it back to top
            if (layer.y2 >= screenHeight) {
                layer.y2 = layer.y1 - screenHeight
            }
        }
    }

    /**
     * Draws the parallax background system.
     * Z-order: Scrolling background (stars) → Static foreground (planet) → Game elements
     */
    fun draw(drawScope: DrawScope) {
        // Draw scrolling background layer (stars) - furthest back
        scrollingLayer?.let { layer ->
            // Draw first instance
            drawScope.drawImage(
                image = layer.bitmap,
                dstOffset = IntOffset(0, layer.y1.toInt()),
                dstSize = IntSize(screenWidth.toInt(), screenHeight.toInt())
            )

            // Draw second instance for seamless looping
            drawScope.drawImage(
                image = layer.bitmap,
                dstOffset = IntOffset(0, layer.y2.toInt()),
                dstSize = IntSize(screenWidth.toInt(), screenHeight.toInt())
            )
        }

        // Draw static foreground layer (planet surface) - on top of scrolling stars
        foregroundLayer?.let { layer ->
            drawScope.drawImage(
                image = layer,
                dstOffset = IntOffset(0, 0),
                dstSize = IntSize(screenWidth.toInt(), screenHeight.toInt())
            )
        }
    }

    /**
     * Resets the parallax system to the initial state.
     * Useful for level restart or respawn.
     */
    fun reset() {
        scrollingLayer?.let { layer ->
            layer.y1 = 0f
            layer.y2 = -screenHeight
        }
    }
}

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
internal fun calculateItemCost(baseCost: Int, shopIndex: Int, debtPenalty: Double = 1.0): Int {
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
internal fun generateShopItems(shopIndex: Int, survivedSeconds: Long): List<ShopItem> {
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

    // Initialize enemy renderer (loads all enemy sprites)
    val enemyRenderer = remember {
        EnemyRenderer(context).apply {
            loadSprites()
        }
    }

    // Load power-up sprite sheet (bonuses_0001.png - 5x5 grid)
    val powerUpSprite = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.bonuses_0001).asImageBitmap()
    }

    // Load space center sprite
    val spaceCenterSprite = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.space_center02).asImageBitmap()
    }

    // Initialize parallax background manager
    val backgroundManager = remember {
        ParallaxBackgroundManager(context, screenWidth, screenHeight).apply {
            initialize()
        }
    }

    // Initialize GameEngine (owns all game state and logic)
    val gameEngine = remember {
        GameEngine(
            context = context,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            powerUpSprite = powerUpSprite,
            spaceCenterSprite = spaceCenterSprite,
            backgroundManager = backgroundManager
        )
    }

    // Observe game state for rendering
    val gameState by gameEngine.state

    // Debug overlay state (UI-only, not part of game state)
    var debugOverlayEnabled by remember { mutableStateOf(false) }
    var debugMetrics by remember { mutableStateOf(DebugMetrics()) }
    var debugTapCount by remember { mutableIntStateOf(0) }
    var lastDebugTap by remember { mutableLongStateOf(0L) }

    // Game loop - measures real delta time for frame-independent movement
    LaunchedEffect(gameState.isAlive) {
        val frameTimes = mutableListOf<Long>()
        var lastUpdateTime = System.currentTimeMillis()
        var lastDebugUpdate = 0L

        while (isActive && gameState.isAlive) {
            delay(16) // Target ~60 FPS, but actual delta may vary

            val currentTime = System.currentTimeMillis()
            val dtMs = (currentTime - lastUpdateTime).coerceIn(0L, 50L) // Clamp to 50ms max (prevent spiral of death)
            lastUpdateTime = currentTime

            // Track frame time for debug overlay (only if debug is enabled)
            if (BuildConfig.DEBUG && debugOverlayEnabled) {
                frameTimes.add(dtMs)

                // Keep only last 60 frames (1 second at 60 FPS)
                if (frameTimes.size > 60) {
                    frameTimes.removeAt(0)
                }

                // Update debug metrics every 250ms
                if (currentTime - lastDebugUpdate > 250) {
                    debugMetrics = debugMetrics.copy(
                        fps = if (frameTimes.isNotEmpty()) (1000f / frameTimes.average().toFloat()).toInt() else 0,
                        avgFrameTime = frameTimes.average().toFloat(),
                        worstFrameTime = frameTimes.maxOrNull()?.toFloat() ?: 0f,
                        enemyCount = gameState.enemies.size,
                        bulletCount = gameState.bullets.size,
                        spawnInterval = 0L,
                        speedMultiplier = 0f,
                        enemyHealth = 0,
                        score = gameState.score,
                        currency = gameState.earnedCurrency,
                        difficultyLevel = gameState.difficultyLevel
                    )
                    lastDebugUpdate = currentTime
                }
            }

            // Update game engine with real delta time (frame-independent)
            gameEngine.update(dtMs)
        }

        if (!gameState.isAlive) {
            delay(2000)
            onGameOver(gameState.score, gameState.earnedCurrency)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        gameEngine.handlePlayerDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .pointerInput(BuildConfig.DEBUG) {
                    // Debug overlay toggle: 5 quick taps in top-right corner (only in debug builds)
                    if (BuildConfig.DEBUG) {
                        detectTapGestures { offset ->
                            val debugZoneSize = 150f
                            val isInDebugZone = offset.x > (screenWidth - debugZoneSize) && offset.y < debugZoneSize

                            if (isInDebugZone) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastDebugTap < 500) {
                                    debugTapCount++
                                    if (debugTapCount >= 4) {  // 5th tap toggles (0-indexed, so >= 4)
                                        debugOverlayEnabled = !debugOverlayEnabled
                                        debugTapCount = 0
                                    }
                                } else {
                                    debugTapCount = 0
                                }
                                lastDebugTap = currentTime
                            }
                        }
                    }
                }
        ) {
            // Draw parallax backgrounds: scrolling stars (back) → static planet (front)
            backgroundManager.draw(this)

            // Draw stars with parallax layers
            gameState.stars.forEach { star ->
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
            }

            // Draw space center (floating shop)
            gameState.spaceCenter?.let { center ->
                drawSpaceCenter(center, spaceCenterSprite)
            }

            // Draw bullets
            gameState.bullets.forEach { bullet ->
                drawBullet(bullet)
            }

            // Draw power-ups
            gameState.powerUps.forEach { powerUp ->
                if (powerUp.alive) {
                    drawPowerUp(powerUp, powerUpSprite)
                }
            }

            // Draw enemies (using procedural shapes + sprite system)
            gameState.enemies.forEach { enemy ->
                drawEnemy(enemy, enemyRenderer)
            }

            // Draw player
            if (gameState.isAlive) {
                drawPlayer(gameState.player, playerSprite)
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
                text = "SCORE: ${gameState.score}",
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
                    text = "${gameState.earnedCurrency}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FF00) // Green
                )
            }

            Text(
                text = "x${String.format("%.2f", gameState.currentMultiplier)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700), // Gold
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Interaction prompt when near space center
        if (gameState.playerInsideShop && !gameState.isShopOpen && gameState.spaceCenter != null) {
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
        if (gameState.isShopOpen) {
            ShopOverlay(
                currentCurrency = gameState.earnedCurrency,
                shopIndex = gameState.shopIndex,
                shopItems = gameState.shopItems,
                playerUpgrades = gameState.playerUpgrades,
                purchasesRemaining = gameState.maxPurchasesPerWindow - gameState.purchasesThisWindow,
                onPurchase = { item ->
                    gameEngine.purchaseShopItem(item)
                },
                onClose = { gameEngine.closeShop() }
            )
        }

        if (!gameState.isAlive) {
            Text(
                text = "GAME OVER",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Debug overlay (only in debug builds)
        if (BuildConfig.DEBUG && debugOverlayEnabled) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xCC000000) // Semi-transparent black
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "DEBUG OVERLAY",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF00) // Green
                    )
                    Text(
                        text = "FPS: ${debugMetrics.fps}",
                        fontSize = 12.sp,
                        color = when {
                            debugMetrics.fps >= 55 -> Color(0xFF00FF00) // Green
                            debugMetrics.fps >= 45 -> Color(0xFFFFAA00) // Orange
                            else -> Color(0xFFFF0000) // Red
                        }
                    )
                    Text(
                        text = "Avg Frame: ${String.format("%.1f", debugMetrics.avgFrameTime)}ms",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Worst Frame: ${String.format("%.1f", debugMetrics.worstFrameTime)}ms",
                        fontSize = 12.sp,
                        color = if (debugMetrics.worstFrameTime > 33f) Color(0xFFFF0000) else Color.White
                    )
                    Text(
                        text = "Enemies: ${debugMetrics.enemyCount}",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Bullets: ${debugMetrics.bulletCount}",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Spawn Interval: ${debugMetrics.spawnInterval}ms",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Speed Mult: ${String.format("%.2f", debugMetrics.speedMultiplier)}x",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Enemy HP: ${debugMetrics.enemyHealth}",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Difficulty: Lvl ${debugMetrics.difficultyLevel}",
                        fontSize = 12.sp,
                        color = Color(0xFFFFD700) // Gold
                    )
                    Text(
                        text = "Score: ${debugMetrics.score}",
                        fontSize = 12.sp,
                        color = Color.Cyan
                    )
                    Text(
                        text = "Currency: ${debugMetrics.currency} \$M",
                        fontSize = 12.sp,
                        color = Color(0xFF00FF00)
                    )
                }
            }

            // Debug toggle hint
            Text(
                text = "Tap 5x in top-right to toggle",
                fontSize = 10.sp,
                color = Color(0x88FFFFFF),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
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

// Draw bullet (simple flat design)
fun DrawScope.drawBullet(bullet: Bullet) {
    drawCircle(
        color = Color.Cyan,
        radius = 4f,
        center = Offset(bullet.x, bullet.y)
    )
}

/**
 * Draw power-up using Bonuses-0001.png sprite atlas (5x5 grid).
 * Column mapping: HEALTH=0, SHIELD=1, FIREPOWER=2, MULTISHOT=3, DOUBLE_REWARD=4
 * Row mapping: tier 0-4 (top to bottom)
 */
fun DrawScope.drawPowerUp(powerUp: WorldPowerUp, spriteAtlas: ImageBitmap?) {
    if (spriteAtlas == null) {
        // Fallback: draw colored circle if sprite not loaded
        val fallbackColor = when (powerUp.type) {
            PowerUpType.HEALTH -> Color(0xFF00FF00)      // Green
            PowerUpType.SHIELD -> Color(0xFF0088FF)      // Blue
            PowerUpType.FIREPOWER -> Color(0xFFFF4400)   // Orange
            PowerUpType.MULTISHOT -> Color(0xFFFFFF00)   // Yellow
            PowerUpType.DOUBLE_REWARD -> Color(0xFFFFD700) // Gold
        }
        drawCircle(
            color = fallbackColor,
            radius = 30f,
            center = Offset(powerUp.x, powerUp.y)
        )
        return
    }

    // Calculate sprite atlas dimensions
    val cellW = spriteAtlas.width / 5
    val cellH = spriteAtlas.height / 5

    // Map type to column (ordinal matches column order)
    val col = powerUp.type.ordinal

    // Map tier to row (clamped 0-4)
    val row = powerUp.tier.coerceIn(0, 4)

    // Source rectangle in atlas
    val srcOffset = IntOffset(col * cellW, row * cellH)
    val srcSize = IntSize(cellW, cellH)

    // Destination position and size
    val renderSize = 60f  // Power-up visual size
    val destOffset = IntOffset(
        (powerUp.x - renderSize / 2).toInt(),
        (powerUp.y - renderSize / 2).toInt()
    )
    val destSize = IntSize(renderSize.toInt(), renderSize.toInt())

    // Draw sprite from atlas
    drawImage(
        image = spriteAtlas,
        srcOffset = srcOffset,
        srcSize = srcSize,
        dstOffset = destOffset,
        dstSize = destSize
    )
}

/**
 * Enemy Renderer - handles all enemy visual rendering
 * Supports both procedural shapes and sprite-based rendering
 *
 * To add new sprites:
 * 1. Add new sprite to res/drawable/
 * 2. Add new EnemyVisualStyle enum value
 * 3. Update loadSprites() to cache the bitmap
 * 4. Update drawEnemy() switch statement
 */
class EnemyRenderer(private val context: Context) {
    // Cached sprites (loaded once, reused for performance)
    internal var evilShipSprite: ImageBitmap? = null

    /**
     * Load and cache all enemy sprites.
     * Call this once during initialization.
     */
    fun loadSprites() {
        evilShipSprite = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.evil_enemy_spaceship_001).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Draw enemy using procedural shapes or sprite based on visual style.
 * This is the main entry point for enemy rendering.
 */
fun DrawScope.drawEnemy(enemy: Enemy, renderer: EnemyRenderer?) {
    val centerX = enemy.x
    val centerY = enemy.y
    val size = enemy.size
    val halfSize = size / 2

    when (enemy.visualStyle) {
        EnemyVisualStyle.SHAPE_TRIANGLE -> {
            // Draw triangle pointing down
            val path = Path().apply {
                moveTo(centerX, centerY + halfSize)  // Bottom point
                lineTo(centerX - halfSize, centerY - halfSize)  // Top left
                lineTo(centerX + halfSize, centerY - halfSize)  // Top right
                close()
            }
            drawPath(
                path = path,
                color = getEnemyColor(enemy.spriteVariant)
            )
            // Optional thin outline
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 2f)
            )
        }

        EnemyVisualStyle.SHAPE_SQUARE -> {
            // Draw square
            drawRect(
                color = getEnemyColor(enemy.spriteVariant),
                topLeft = Offset(centerX - halfSize, centerY - halfSize),
                size = Size(size, size)
            )
            // Optional thin outline
            drawRect(
                color = Color.White,
                topLeft = Offset(centerX - halfSize, centerY - halfSize),
                size = Size(size, size),
                style = Stroke(width = 2f)
            )
        }

        EnemyVisualStyle.SHAPE_RECT -> {
            // Draw rectangle (wider than tall)
            val width = size * 1.4f
            val height = size * 0.7f
            drawRect(
                color = getEnemyColor(enemy.spriteVariant),
                topLeft = Offset(centerX - width / 2, centerY - height / 2),
                size = Size(width, height)
            )
            // Optional thin outline
            drawRect(
                color = Color.White,
                topLeft = Offset(centerX - width / 2, centerY - height / 2),
                size = Size(width, height),
                style = Stroke(width = 2f)
            )
        }

        EnemyVisualStyle.SHAPE_CIRCLE -> {
            // Draw circle
            drawCircle(
                color = getEnemyColor(enemy.spriteVariant),
                radius = halfSize,
                center = Offset(centerX, centerY)
            )
            // Optional thin outline
            drawCircle(
                color = Color.White,
                radius = halfSize,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2f)
            )
        }

        EnemyVisualStyle.SPRITE_EVIL_SHIP_001 -> {
            // Draw sprite if available, fallback to circle
            renderer?.evilShipSprite?.let { sprite ->
                val destOffset = IntOffset((centerX - halfSize).toInt(), (centerY - halfSize).toInt())
                val destSize = IntSize(size.toInt(), size.toInt())

                drawImage(
                    image = sprite,
                    dstOffset = destOffset,
                    dstSize = destSize
                )
            } ?: run {
                // Fallback to circle if sprite not loaded
                drawCircle(
                    color = Color(0xFFFF00FF),
                    radius = halfSize,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}

/**
 * Get color for an enemy based on sprite variant.
 * Used for procedural shapes.
 */
private fun getEnemyColor(variant: Int): Color {
    return when (variant % 3) {
        0 -> Color(0xFFFF4444) // Red
        1 -> Color(0xFF4444FF) // Blue
        else -> Color(0xFF44FF44) // Green
    }
}

// Draw floating space center (shop)
fun DrawScope.drawSpaceCenter(center: SpaceCenter, spriteSheet: ImageBitmap) {
    val centerX = center.x
    val centerY = center.y
    val size = center.size

    // Calculate destination position (centered)
    val destOffset = IntOffset((centerX - size / 2).toInt(), (centerY - size / 2).toInt())
    val destSize = IntSize(size.toInt(), size.toInt())

    // Draw the space center sprite
    drawImage(
        image = spriteSheet,
        dstOffset = destOffset,
        dstSize = destSize
    )
}
