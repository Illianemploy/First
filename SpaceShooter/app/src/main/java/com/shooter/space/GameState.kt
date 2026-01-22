package com.shooter.space

import androidx.compose.ui.graphics.ImageBitmap

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
                        x = (0..screenWidth.toInt()).random().toFloat(),
                        y = (0..screenHeight.toInt()).random().toFloat(),
                        size = (1f..3f).random(),
                        speed = (2f..5f).random(),
                        layer = (0..2).random()
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
