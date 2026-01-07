package com.uktobacco.data

import androidx.compose.ui.graphics.Color

// Countries within 4 hours flight from UK
data class CountryPricing(
    val countryCode: String,
    val countryName: String,
    val region: Region,
    val averagePackPrice: Double, // Price for 20 pack in GBP equivalent
    val taxRate: Double, // Percentage of price that is tax
    val taxAmount: Double, // Actual tax in GBP
    val priceBeforeTax: Double,
    val flightTimeHours: Double,
    val averageFlightCostReturn: Double, // Average return flight cost from UK
    val bulkBuyLimit: Int, // Legal limit for personal import (packs)
    val currency: String,
    val flagEmoji: String
)

enum class Region(val displayName: String, val color: Color) {
    WESTERN_EUROPE("Western Europe", Color(0xFF4A90E2)),
    EASTERN_EUROPE("Eastern Europe", Color(0xFF9B59B6)),
    SOUTHERN_EUROPE("Southern Europe", Color(0xFFE67E22)),
    NORTHERN_EUROPE("Northern Europe", Color(0xFF3498DB)),
    NORTH_AFRICA("North Africa", Color(0xFFF39C12)),
    MIDDLE_EAST("Middle East", Color(0xFF16A085))
}

object GlobalTobaccoPricing {

    // Realistic data based on 2024-2026 global cigarette prices and taxes
    val allCountries = listOf(
        // UK baseline
        CountryPricing(
            countryCode = "GB",
            countryName = "United Kingdom",
            region = Region.NORTHERN_EUROPE,
            averagePackPrice = 14.00,
            taxRate = 88.4,
            taxAmount = 12.38,
            priceBeforeTax = 1.62,
            flightTimeHours = 0.0,
            averageFlightCostReturn = 0.0,
            bulkBuyLimit = 800, // From UK
            currency = "GBP",
            flagEmoji = "🇬🇧"
        ),

        // Western Europe - High tax, expensive
        CountryPricing(
            countryCode = "FR",
            countryName = "France",
            region = Region.WESTERN_EUROPE,
            averagePackPrice = 12.50,
            taxRate = 83.2,
            taxAmount = 10.40,
            priceBeforeTax = 2.10,
            flightTimeHours = 1.5,
            averageFlightCostReturn = 85.0,
            bulkBuyLimit = 800,
            currency = "EUR",
            flagEmoji = "🇫🇷"
        ),
        CountryPricing(
            countryCode = "IE",
            countryName = "Ireland",
            region = Region.WESTERN_EUROPE,
            averagePackPrice = 15.50,
            taxRate = 85.7,
            taxAmount = 13.28,
            priceBeforeTax = 2.22,
            flightTimeHours = 1.0,
            averageFlightCostReturn = 55.0,
            bulkBuyLimit = 800,
            currency = "EUR",
            flagEmoji = "🇮🇪"
        ),
        CountryPricing(
            countryCode = "NL",
            countryName = "Netherlands",
            region = Region.WESTERN_EUROPE,
            averagePackPrice = 8.50,
            taxRate = 76.5,
            taxAmount = 6.50,
            priceBeforeTax = 2.00,
            flightTimeHours = 1.0,
            averageFlightCostReturn = 65.0,
            bulkBuyLimit = 800,
            currency = "EUR",
            flagEmoji = "🇳🇱"
        ),
        CountryPricing(
            countryCode = "BE",
            countryName = "Belgium",
            region = Region.WESTERN_EUROPE,
            averagePackPrice = 8.00,
            taxRate = 77.8,
            taxAmount = 6.22,
            priceBeforeTax = 1.78,
            flightTimeHours = 1.0,
            averageFlightCostReturn = 60.0,
            bulkBuyLimit = 800,
            currency = "EUR",
            flagEmoji = "🇧🇪"
        ),
        CountryPricing(
            countryCode = "DE",
            countryName = "Germany",
            region = Region.WESTERN_EUROPE,
            averagePackPrice = 7.50,
            taxRate = 75.2,
            taxAmount = 5.64,
            priceBeforeTax = 1.86,
            flightTimeHours = 1.5,
            averageFlightCostReturn = 70.0,
            bulkBuyLimit = 800,
            currency = "EUR",
            flagEmoji = "🇩🇪"
        ),

        // Eastern Europe - Lower prices, good arbitrage opportunities
        CountryPricing(
            countryCode = "PL",
            countryName = "Poland",
            region = Region.EASTERN_EUROPE,
            averagePackPrice = 4.80,
            taxRate = 68.5,
            taxAmount = 3.29,
            priceBeforeTax = 1.51,
            flightTimeHours = 2.5,
            averageFlightCostReturn = 75.0,
            bulkBuyLimit = 800,
            currency = "PLN",
            flagEmoji = "🇵🇱"
        ),
        CountryPricing(
            countryCode = "CZ",
            countryName = "Czech Republic",
            region = Region.EASTERN_EUROPE,
            averagePackPrice = 4.50,
            taxRate = 67.8,
            taxAmount = 3.05,
            priceBeforeTax = 1.45,
            flightTimeHours = 2.0,
            averageFlightCostReturn = 80.0,
            bulkBuyLimit = 800,
            currency = "CZK",
            flagEmoji = "🇨🇿"
        ),
        CountryPricing(
            countryCode = "HU",
            countryName = "Hungary",
            region = Region.EASTERN_EUROPE,
            averagePackPrice = 4.20,
            taxRate = 70.2,
            taxAmount = 2.95,
            priceBeforeTax = 1.25,
            flightTimeHours = 2.5,
            averageFlightCostReturn = 85.0,
            bulkBuyLimit = 800,
            currency = "HUF",
            flagEmoji = "🇭🇺"
        ),
        CountryPricing(
            countryCode = "BG",
            countryName = "Bulgaria",
            region = Region.EASTERN_EUROPE,
            averagePackPrice = 3.80,
            taxRate = 65.5,
            taxAmount = 2.49,
            priceBeforeTax = 1.31,
            flightTimeHours = 3.5,
            averageFlightCostReturn = 95.0,
            bulkBuyLimit = 800,
            currency = "BGN",
            flagEmoji = "🇧🇬"
        ),
        CountryPricing(
            countryCode = "RO",
            countryName = "Romania",
            region = Region.EASTERN_EUROPE,
            averagePackPrice = 4.00,
            taxRate = 66.8,
            taxAmount = 2.67,
            priceBeforeTax = 1.33,
            flightTimeHours = 3.0,
            averageFlightCostReturn = 90.0,
            bulkBuyLimit = 800,
            currency = "RON",
            flagEmoji = "🇷🇴"
        ),

        // Southern Europe - Moderate prices
        CountryPricing(
            countryCode = "ES",
            countryName = "Spain",
            region = Region.SOUTHERN_EUROPE,
            averagePackPrice = 5.50,
            taxRate = 72.4,
            taxAmount = 3.98,
            priceBeforeTax = 1.52,
            flightTimeHours = 2.5,
            averageFlightCostReturn = 70.0,
            bulkBuyLimit = 800,
            currency = "EUR",
            flagEmoji = "🇪🇸"
        ),
        CountryPricing(
            countryCode = "PT",
            countryName = "Portugal",
            region = Region.SOUTHERN_EUROPE,
            averagePackPrice = 5.20,
            taxRate = 73.1,
            taxAmount = 3.80,
            priceBeforeTax = 1.40,
            flightTimeHours = 2.5,
            averageFlightCostReturn = 75.0,
            bulkBuyLimit = 800,
            currency = "EUR",
            flagEmoji = "🇵🇹"
        ),
        CountryPricing(
            countryCode = "IT",
            countryName = "Italy",
            region = Region.SOUTHERN_EUROPE,
            averagePackPrice = 6.00,
            taxRate = 74.5,
            taxAmount = 4.47,
            priceBeforeTax = 1.53,
            flightTimeHours = 2.5,
            averageFlightCostReturn = 80.0,
            bulkBuyLimit = 800,
            currency = "EUR",
            flagEmoji = "🇮🇹"
        ),
        CountryPricing(
            countryCode = "GR",
            countryName = "Greece",
            region = Region.SOUTHERN_EUROPE,
            averagePackPrice = 4.70,
            taxRate = 71.3,
            taxAmount = 3.35,
            priceBeforeTax = 1.35,
            flightTimeHours = 3.5,
            averageFlightCostReturn = 90.0,
            bulkBuyLimit = 800,
            currency = "EUR",
            flagEmoji = "🇬🇷"
        ),
        CountryPricing(
            countryCode = "HR",
            countryName = "Croatia",
            region = Region.SOUTHERN_EUROPE,
            averagePackPrice = 4.50,
            taxRate = 69.8,
            taxAmount = 3.14,
            priceBeforeTax = 1.36,
            flightTimeHours = 2.5,
            averageFlightCostReturn = 85.0,
            bulkBuyLimit = 800,
            currency = "EUR",
            flagEmoji = "🇭🇷"
        ),

        // Northern Europe - High tax
        CountryPricing(
            countryCode = "SE",
            countryName = "Sweden",
            region = Region.NORTHERN_EUROPE,
            averagePackPrice = 8.80,
            taxRate = 76.9,
            taxAmount = 6.77,
            priceBeforeTax = 2.03,
            flightTimeHours = 2.5,
            averageFlightCostReturn = 85.0,
            bulkBuyLimit = 800,
            currency = "SEK",
            flagEmoji = "🇸🇪"
        ),
        CountryPricing(
            countryCode = "NO",
            countryName = "Norway",
            region = Region.NORTHERN_EUROPE,
            averagePackPrice = 13.50,
            taxRate = 82.6,
            taxAmount = 11.15,
            priceBeforeTax = 2.35,
            flightTimeHours = 2.0,
            averageFlightCostReturn = 95.0,
            bulkBuyLimit = 200, // Norway has stricter limits
            currency = "NOK",
            flagEmoji = "🇳🇴"
        ),
        CountryPricing(
            countryCode = "DK",
            countryName = "Denmark",
            region = Region.NORTHERN_EUROPE,
            averagePackPrice = 7.20,
            taxRate = 75.8,
            taxAmount = 5.46,
            priceBeforeTax = 1.74,
            flightTimeHours = 1.5,
            averageFlightCostReturn = 70.0,
            bulkBuyLimit = 800,
            currency = "DKK",
            flagEmoji = "🇩🇰"
        ),

        // North Africa - Very cheap, best arbitrage
        CountryPricing(
            countryCode = "MA",
            countryName = "Morocco",
            region = Region.NORTH_AFRICA,
            averagePackPrice = 2.80,
            taxRate = 58.2,
            taxAmount = 1.63,
            priceBeforeTax = 1.17,
            flightTimeHours = 3.5,
            averageFlightCostReturn = 110.0,
            bulkBuyLimit = 200, // International travel limits
            currency = "MAD",
            flagEmoji = "🇲🇦"
        ),
        CountryPricing(
            countryCode = "TN",
            countryName = "Tunisia",
            region = Region.NORTH_AFRICA,
            averagePackPrice = 2.50,
            taxRate = 55.6,
            taxAmount = 1.39,
            priceBeforeTax = 1.11,
            flightTimeHours = 3.0,
            averageFlightCostReturn = 120.0,
            bulkBuyLimit = 200,
            currency = "TND",
            flagEmoji = "🇹🇳"
        ),

        // Middle East - Moderate to cheap
        CountryPricing(
            countryCode = "TR",
            countryName = "Turkey",
            region = Region.MIDDLE_EAST,
            averagePackPrice = 3.20,
            taxRate = 63.4,
            taxAmount = 2.03,
            priceBeforeTax = 1.17,
            flightTimeHours = 4.0,
            averageFlightCostReturn = 100.0,
            bulkBuyLimit = 200,
            currency = "TRY",
            flagEmoji = "🇹🇷"
        ),
        CountryPricing(
            countryCode = "AE",
            countryName = "UAE (Dubai)",
            region = Region.MIDDLE_EAST,
            averagePackPrice = 4.50,
            taxRate = 40.0, // Low tax region
            taxAmount = 1.80,
            priceBeforeTax = 2.70,
            flightTimeHours = 7.0, // Too far but included for comparison
            averageFlightCostReturn = 350.0,
            bulkBuyLimit = 400,
            currency = "AED",
            flagEmoji = "🇦🇪"
        )
    )

    fun getCountriesByRegion(region: Region): List<CountryPricing> {
        return allCountries.filter { it.region == region }.sortedBy { it.averagePackPrice }
    }

    fun getCountriesWithin4Hours(): List<CountryPricing> {
        return allCountries.filter { it.flightTimeHours <= 4.0 && it.countryCode != "GB" }
            .sortedBy { it.averagePackPrice }
    }

    fun getCheapestCountries(limit: Int = 10): List<CountryPricing> {
        return allCountries.filter { it.countryCode != "GB" }
            .sortedBy { it.averagePackPrice }
            .take(limit)
    }

    fun getHighestTaxCountries(limit: Int = 10): List<CountryPricing> {
        return allCountries.sortedByDescending { it.taxRate }.take(limit)
    }

    fun getAllRegions(): List<Region> {
        return Region.entries.toList()
    }
}
