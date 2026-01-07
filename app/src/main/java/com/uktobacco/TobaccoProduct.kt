package com.uktobacco

import java.time.LocalDateTime

enum class TobaccoType {
    CIGARETTES,
    ROLLING_TOBACCO
}

data class TobaccoProduct(
    val id: String,
    val name: String,
    val brand: String,
    val type: TobaccoType,
    val price: Double,
    val size: String, // e.g., "20 pack", "50g pouch"
    val retailer: String,
    val lastUpdated: LocalDateTime,
    val pricePerUnit: Double, // e.g., price per cigarette or price per gram
    val imageUrl: String? = null
)

data class PriceHistory(
    val productId: String,
    val timestamp: LocalDateTime,
    val price: Double,
    val retailer: String
)
