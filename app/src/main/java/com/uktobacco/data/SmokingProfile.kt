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
        // Calculate packs needed
        val packsNeeded = ceil(smokingProfile.packsPerMonth * monthsToSupply).toInt()

        // Cap at legal limit
        val packsToBuy = minOf(packsNeeded, country.bulkBuyLimit)
        val actualMonthsCovered = packsToBuy / smokingProfile.packsPerMonth

        // Costs
        val costInCountry = packsToBuy * country.averagePackPrice
        val flightCost = country.averageFlightCostReturn
        val totalTripCost = costInCountry + flightCost

        // UK equivalent cost
        val ukCostForSamePacks = packsToBuy * smokingProfile.pricePerPack

        // Savings
        val totalSavings = ukCostForSamePacks - totalTripCost
        val savingsPerMonth = totalSavings / actualMonthsCovered

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
    }

    fun findBestOpportunities(
        smokingProfile: SmokingProfile,
        monthsToSupply: Int = 3
    ): List<TravelSavingsCalculation> {
        return GlobalTobaccoPricing.getCountriesWithin4Hours()
            .map { calculate(smokingProfile, it, monthsToSupply) }
            .filter { it.isWorthIt }
            .sortedByDescending { it.totalSavings }
    }
}
