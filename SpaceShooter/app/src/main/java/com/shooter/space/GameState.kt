package com.shooter.space

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.random.Random

/**
 * Immutable snapshot of all game state at a point in time.
 * Published by GameEngine for UI rendering only.
 * UI must NOT mutate any fields - all changes go through InputEvent.
 */
data class GameState(
    // === CORE ENTITIES ===
    val player: Player,
    val enemies: List<Enemy>,
    val bullets: List<Bullet>,
    val stars: List<Star>,

    // === INTERACTIVE OBJECTS ===
    val spaceCenter: SpaceCenter?,
    val powerUps: List<WorldPowerUp>, // From PowerUpSystem.worldPowerUps

    // === GAME METRICS ===
    val score: Int,
    val earnedCurrency: Int,
    val currentMultiplier: Double,
    val playerHealth: Int,
    val maxPlayerHealth: Int,
    val isAlive: Boolean,

    // === TIMING ===
    val gameTime: Float, // Total elapsed time (for compatibility)
    val survivedMilliseconds: Long, // Time excluding paused intervals

    // === SHOP STATE ===
    val isShopOpen: Boolean,
    val shopIndex: Int,
    val purchasesThisWindow: Int,
    val maxPurchasesPerWindow: Int,
    val playerInsideShop: Boolean,
    val shopItems: List<ShopItem>, // Generated items for current shop

    // === UPGRADES & MODIFIERS ===
    val playerUpgrades: PlayerUpgrades,
    val activeRisk: RiskState?,
    val permanentMultiplierBonus: Double,
    val weaponStats: WeaponStats,
    val activePowerUpEffects: Map<PowerUpType, ActiveEffect>, // From PowerUpSystem

    // === DIFFICULTY ===
    val difficultyLevel: Int,

    // === RENDERING ASSETS (cached references) ===
    val powerUpSprite: ImageBitmap?,
    val spaceCenterSprite: ImageBitmap?,

    // === BACKGROUND STATE ===
    val backgroundScrollOffset: Float // From ParallaxBackgroundManager
) {
    companion object {
        /**
         * Helper to generate random float in range [min, max]
         */
        private fun randFloat(min: Float, max: Float): Float =
            Random.nextFloat() * (max - min) + min

        /**
         * Factory method for initial game state.
         * Called once at game start or restart.
         */
        fun initial(
            screenWidth: Float,
            screenHeight: Float,
            powerUpSprite: ImageBitmap?,
            spaceCenterSprite: ImageBitmap?
        ): GameState {
            return GameState(
                player = Player(screenWidth / 2, screenHeight - 150f),
                enemies = emptyList(),
                bullets = emptyList(),
                stars = List(100) {
                    Star(
                        x = Random.nextInt(0, screenWidth.toInt() + 1).toFloat(),
                        y = Random.nextInt(0, screenHeight.toInt() + 1).toFloat(),
                        size = randFloat(1f, 3f),
                        speed = randFloat(2f, 5f),
                        layer = Random.nextInt(0, 3)
                    )
                },
                spaceCenter = null,
                powerUps = emptyList(),
                score = 0,
                earnedCurrency = 0,
                currentMultiplier = 1.0,
                playerHealth = 3,
                maxPlayerHealth = 3,
                isAlive = true,
                gameTime = 0f,
                survivedMilliseconds = 0L,
                isShopOpen = false,
                shopIndex = 0,
                purchasesThisWindow = 0,
                maxPurchasesPerWindow = 3,
                playerInsideShop = false,
                shopItems = emptyList(),
                playerUpgrades = PlayerUpgrades(),
                activeRisk = null,
                permanentMultiplierBonus = 0.0,
                weaponStats = WeaponStats(),
                activePowerUpEffects = emptyMap(),
                difficultyLevel = 0,
                powerUpSprite = powerUpSprite,
                spaceCenterSprite = spaceCenterSprite,
                backgroundScrollOffset = 0f
            )
        }
    }
}
