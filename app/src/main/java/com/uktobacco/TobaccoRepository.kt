package com.uktobacco

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import kotlin.random.Random

class TobaccoRepository {

    private val products = mutableListOf<TobaccoProduct>()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // Popular UK cigarette brands
        val cigaretteBrands = listOf(
            Triple("Marlboro Gold", "Marlboro", 14.50),
            Triple("Marlboro Red", "Marlboro", 14.50),
            Triple("Richmond Superkings", "Richmond", 12.80),
            Triple("Lambert & Butler", "Lambert & Butler", 13.20),
            Triple("Benson & Hedges Gold", "Benson & Hedges", 14.20),
            Triple("Mayfair King Size", "Mayfair", 13.50),
            Triple("Sterling Dual", "Sterling", 12.90),
            Triple("Royals King Size", "Royals", 12.50),
            Triple("JPS Players", "JPS", 13.00),
            Triple("Pall Mall Red", "Pall Mall", 13.40),
            Triple("Camel Blue", "Camel", 14.30),
            Triple("Lucky Strike Red", "Lucky Strike", 13.80),
            Triple("Windsor Blue", "Windsor", 12.70),
            Triple("Sovereign King Size", "Sovereign", 12.60),
            Triple("Berkeley Superkings", "Berkeley", 12.40)
        )

        // Popular UK rolling tobacco brands
        val tobaccoBrands = listOf(
            Triple("Amber Leaf", "Amber Leaf", 30g to 17.50),
            Triple("Amber Leaf", "Amber Leaf", 50g to 28.00),
            Triple("Golden Virginia", "Golden Virginia", 30g to 18.20),
            Triple("Golden Virginia", "Golden Virginia", 50g to 29.50),
            Triple("Cutters Choice", "Cutters Choice", 30g to 16.80),
            Triple("Cutters Choice", "Cutters Choice", 50g to 27.00),
            Triple("Drum Original", "Drum", 30g to 17.00),
            Triple("Drum Original", "Drum", 50g to 27.50),
            Triple("Old Holborn", "Old Holborn", 30g to 16.50),
            Triple("Old Holborn", "Old Holborn", 50g to 26.50),
            Triple("Pall Mall Blue", "Pall Mall", 30g to 16.20),
            Triple("Pall Mall Blue", "Pall Mall", 50g to 26.00),
            Triple("Pueblo Blue", "Pueblo", 30g to 18.00),
            Triple("Bali Shag", "Bali Shag", 40g to 19.50),
            Triple("Samson Original", "Samson", 30g to 16.00),
            Triple("JPS Volume Tobacco", "JPS", 50g to 25.80),
            Triple("Sterling Rolling Tobacco", "Sterling", 30g to 15.80),
            Triple("Gauloises Blondes", "Gauloises", 30g to 17.80)
        )

        val retailers = listOf("Tesco", "Sainsbury's", "Co-op", "Morrisons", "ASDA", "Local Shop")

        // Add cigarettes
        cigaretteBrands.forEachIndexed { index, (name, brand, basePrice) ->
            retailers.take(3).forEach { retailer ->
                val priceVariation = Random.nextDouble(-0.30, 0.50)
                val price = (basePrice + priceVariation).coerceAtLeast(12.00)

                products.add(
                    TobaccoProduct(
                        id = "cig_${index}_${retailer.replace(" ", "_")}",
                        name = name,
                        brand = brand,
                        type = TobaccoType.CIGARETTES,
                        price = String.format("%.2f", price).toDouble(),
                        size = "20 pack",
                        retailer = retailer,
                        lastUpdated = LocalDateTime.now().minusMinutes(Random.nextLong(1, 120)),
                        pricePerUnit = String.format("%.3f", price / 20).toDouble()
                    )
                )
            }
        }

        // Add rolling tobacco
        tobaccoBrands.forEachIndexed { index, (name, brand, sizePrice) ->
            val (size, basePrice) = sizePrice
            retailers.take(3).forEach { retailer ->
                try {
                    val priceVariation = Random.nextDouble(-0.50, 1.00)
                    val price = (basePrice + priceVariation).coerceAtLeast(15.00)

                    // Safely parse weight from size string
                    val weightStr = size.removeSuffix("g")
                    val weight = weightStr.toIntOrNull() ?: run {
                        println("Error: Invalid weight format '$size' for product $name. Skipping.")
                        return@forEach
                    }

                    if (weight <= 0) {
                        println("Error: Invalid weight value $weight for product $name. Skipping.")
                        return@forEach
                    }

                    products.add(
                        TobaccoProduct(
                            id = "tob_${index}_${weight}_${retailer.replace(" ", "_")}",
                            name = "$name $size",
                            brand = brand,
                            type = TobaccoType.ROLLING_TOBACCO,
                            price = String.format("%.2f", price).toDouble(),
                            size = "${weight}g pouch",
                            retailer = retailer,
                            lastUpdated = LocalDateTime.now().minusMinutes(Random.nextLong(1, 120)),
                            pricePerUnit = String.format("%.2f", price / weight).toDouble()
                        )
                    )
                } catch (e: Exception) {
                    println("Error: Failed to create tobacco product $name: ${e.message}")
                }
            }
        }
    }

    // Simulate real-time updates
    fun observeProducts(): Flow<List<TobaccoProduct>> = flow {
        try {
            while (true) {
                // Emit current products
                emit(products.toList())

                // Simulate price updates every 30 seconds
                delay(30000)

                // Randomly update some prices (simulate real-time data)
                if (products.isEmpty()) {
                    println("Warning: No products available for price updates")
                    continue
                }

                val numToUpdate = Random.nextInt(3, 8).coerceAtMost(products.size)
                val productsToUpdate = products.shuffled().take(numToUpdate)

                productsToUpdate.forEach { product ->
                    try {
                        val priceChange = Random.nextDouble(-0.20, 0.20)
                        val updatedProduct = product.copy(
                            price = String.format("%.2f", (product.price + priceChange).coerceAtLeast(10.00)).toDouble(),
                            lastUpdated = LocalDateTime.now()
                        )
                        val index = products.indexOfFirst { it.id == product.id }
                        if (index != -1) {
                            products[index] = updatedProduct
                        }
                    } catch (e: Exception) {
                        println("Error: Failed to update price for product ${product.id}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Error: Product observation flow interrupted: ${e.message}")
            throw e
        }
    }

    fun getAllProducts(): List<TobaccoProduct> = products.toList()

    fun searchProducts(query: String): List<TobaccoProduct> {
        return products.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.brand.contains(query, ignoreCase = true) ||
            it.retailer.contains(query, ignoreCase = true)
        }
    }

    fun filterByType(type: TobaccoType): List<TobaccoProduct> {
        return products.filter { it.type == type }
    }

    fun filterByRetailer(retailer: String): List<TobaccoProduct> {
        return products.filter { it.retailer == retailer }
    }

    fun sortByPrice(ascending: Boolean = true): List<TobaccoProduct> {
        return if (ascending) {
            products.sortedBy { it.price }
        } else {
            products.sortedByDescending { it.price }
        }
    }

    fun getCheapestByBrand(): Map<String, TobaccoProduct> {
        return try {
            products.groupBy { it.brand }
                .mapNotNull { (brand, brandProducts) ->
                    val cheapest = brandProducts.minByOrNull { it.price }
                    if (cheapest != null) {
                        brand to cheapest
                    } else {
                        println("Warning: No products found for brand $brand")
                        null
                    }
                }
                .toMap()
        } catch (e: Exception) {
            println("Error: Failed to get cheapest products by brand: ${e.message}")
            emptyMap()
        }
    }
}
