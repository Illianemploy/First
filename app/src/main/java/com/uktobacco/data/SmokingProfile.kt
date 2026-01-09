package com.uktobacco.data

import kotlin.math.ceil

data class SmokingProfile(
    val cigarettesPerDay: Int = 0,
    val preferredBrand: String = "",
    val pricePerPack: Double = 14.00, // UK average
    val packsPerMonth: Double = 0.0,
    val monthlyExpenditure: Double = 0.0,
    val yearlyExpenditure: Double = 0.0
) {
    companion object {
        fun calculate(cigarettesPerDay: Int, pricePerPack: Double): SmokingProfile {
            // Validate inputs
            if (cigarettesPerDay < 0) {
                println("Error: cigarettesPerDay cannot be negative. Using 0.")
                return SmokingProfile()
            }

            if (pricePerPack < 0) {
                println("Error: pricePerPack cannot be negative. Using default value.")
                return SmokingProfile(cigarettesPerDay = cigarettesPerDay)
            }

            try {
                val cigarettesPerMonth = cigarettesPerDay * 30.0
                val packsPerMonth = cigarettesPerMonth / 20.0
                val monthlyExpenditure = packsPerMonth * pricePerPack
                val yearlyExpenditure = monthlyExpenditure * 12.0

                return SmokingProfile(
                    cigarettesPerDay = cigarettesPerDay,
                    pricePerPack = pricePerPack,
                    packsPerMonth = packsPerMonth,
                    monthlyExpenditure = monthlyExpenditure,
                    yearlyExpenditure = yearlyExpenditure
                )
            } catch (e: Exception) {
                println("Error: Failed to calculate smoking profile: ${e.message}")
                return SmokingProfile()
            }
        }
    }
}

data class TravelSavingsCalculation(
    val country: CountryPricing,
    val ukMonthlySpend: Double,
    val packsToBuy: Int,
    val costInCountry: Double,
    val flightCost: Double,
    val totalTripCost: Double,
    val monthsCovered: Double,
    val totalSavings: Double,
    val savingsPerMonth: Double,
    val isWorthIt: Boolean,
    val breakEvenMonths: Double,
    val legalLimit: Int
)

object TravelSavingsCalculator {

    fun calculate(
        smokingProfile: SmokingProfile,
        country: CountryPricing,
        monthsToSupply: Int = 3 // How many months worth to buy
    ): TravelSavingsCalculation {
        // Validate inputs
        if (monthsToSupply <= 0) {
            println("Error: monthsToSupply must be positive. Using default value of 3.")
            return calculate(smokingProfile, country, 3)
        }

        if (smokingProfile.packsPerMonth <= 0) {
            println("Error: Invalid smoking profile with packsPerMonth = ${smokingProfile.packsPerMonth}")
            // Return a calculation with zero values
            return TravelSavingsCalculation(
                country = country,
                ukMonthlySpend = 0.0,
                packsToBuy = 0,
                costInCountry = 0.0,
                flightCost = country.averageFlightCostReturn,
                totalTripCost = country.averageFlightCostReturn,
                monthsCovered = 0.0,
                totalSavings = 0.0,
                savingsPerMonth = 0.0,
                isWorthIt = false,
                breakEvenMonths = Double.MAX_VALUE,
                legalLimit = country.bulkBuyLimit
            )
        }

        try {
            // Calculate packs needed
            val packsNeeded = ceil(smokingProfile.packsPerMonth * monthsToSupply).toInt()

            // Cap at legal limit
            val packsToBuy = minOf(packsNeeded, country.bulkBuyLimit)

            // Safely calculate months covered (avoid division by zero)
            val actualMonthsCovered = if (smokingProfile.packsPerMonth > 0) {
                packsToBuy / smokingProfile.packsPerMonth
            } else {
                0.0
            }

            // Costs
            val costInCountry = packsToBuy * country.averagePackPrice
            val flightCost = country.averageFlightCostReturn
            val totalTripCost = costInCountry + flightCost

            // UK equivalent cost
            val ukCostForSamePacks = packsToBuy * smokingProfile.pricePerPack

            // Savings
            val totalSavings = ukCostForSamePacks - totalTripCost

            // Safely calculate savings per month (avoid division by zero)
            val savingsPerMonth = if (actualMonthsCovered > 0) {
                totalSavings / actualMonthsCovered
            } else {
                0.0
            }

            // Is it worth it? (positive savings and covers at least 1 month)
            val isWorthIt = totalSavings > 50.0 && actualMonthsCovered >= 1.0

            // Break even
            val breakEvenMonths = if (savingsPerMonth > 0) {
                flightCost / savingsPerMonth
            } else {
                Double.MAX_VALUE
            }

            return TravelSavingsCalculation(
                country = country,
                ukMonthlySpend = smokingProfile.monthlyExpenditure,
                packsToBuy = packsToBuy,
                costInCountry = costInCountry,
                flightCost = flightCost,
                totalTripCost = totalTripCost,
                monthsCovered = actualMonthsCovered,
                totalSavings = totalSavings,
                savingsPerMonth = savingsPerMonth,
                isWorthIt = isWorthIt,
                breakEvenMonths = breakEvenMonths,
                legalLimit = country.bulkBuyLimit
            )
        } catch (e: Exception) {
            println("Error: Failed to calculate travel savings for ${country.countryName}: ${e.message}")
            // Return a safe default calculation
            return TravelSavingsCalculation(
                country = country,
                ukMonthlySpend = smokingProfile.monthlyExpenditure,
                packsToBuy = 0,
                costInCountry = 0.0,
                flightCost = country.averageFlightCostReturn,
                totalTripCost = country.averageFlightCostReturn,
                monthsCovered = 0.0,
                totalSavings = 0.0,
                savingsPerMonth = 0.0,
                isWorthIt = false,
                breakEvenMonths = Double.MAX_VALUE,
                legalLimit = country.bulkBuyLimit
            )
        }
    }

    fun findBestOpportunities(
        smokingProfile: SmokingProfile,
        monthsToSupply: Int = 3
    ): List<TravelSavingsCalculation> {
        return try {
            val countries = GlobalTobaccoPricing.getCountriesWithin4Hours()

            if (countries.isEmpty()) {
                println("Warning: No countries available within 4 hours flight time")
                return emptyList()
            }

            countries
                .mapNotNull { country ->
                    try {
                        calculate(smokingProfile, country, monthsToSupply)
                    } catch (e: Exception) {
                        println("Error: Failed to calculate savings for ${country.countryName}: ${e.message}")
                        null
                    }
                }
                .filter { it.isWorthIt }
                .sortedByDescending { it.totalSavings }
        } catch (e: Exception) {
            println("Error: Failed to find best travel opportunities: ${e.message}")
            emptyList()
        }
    }
}
