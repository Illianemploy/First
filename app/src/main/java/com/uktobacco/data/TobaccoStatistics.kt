package com.uktobacco.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

/**
 * Global tobacco statistics for awareness and education
 * Based on WHO (World Health Organization) data
 */
object TobaccoStatistics {

    // WHO estimates - 8 million deaths per year from tobacco
    const val ANNUAL_TOBACCO_DEATHS_GLOBAL = 8_000_000
    const val DEATHS_PER_DAY_GLOBAL = 21_917 // 8M / 365
    const val DEATHS_PER_HOUR_GLOBAL = 913 // ~913 per hour
    const val DEATHS_PER_MINUTE_GLOBAL = 15.2 // ~15 per minute
    const val DEATHS_PER_SECOND_GLOBAL = 0.25 // ~1 every 4 seconds

    // UK specific statistics
    const val ANNUAL_TOBACCO_DEATHS_UK = 78_000
    const val DEATHS_PER_DAY_UK = 214
    const val SMOKERS_IN_UK = 6_900_000 // ~6.9 million adult smokers

    // Economic statistics
    const val AVERAGE_UK_PACK_PRICE = 14.00
    const val AVERAGE_CIGARETTES_PER_DAY_UK = 11.0 // Average UK smoker
    const val PACKS_PER_YEAR_AVERAGE = 200.0 // ~11 cigs/day = ~200 packs/year

    // Calculated expenditures
    val DAILY_UK_EXPENDITURE = calculateDailyUKExpenditure()
    val ANNUAL_UK_EXPENDITURE = DAILY_UK_EXPENDITURE * 365

    private fun calculateDailyUKExpenditure(): Double {
        // 6.9M smokers * average 0.55 packs per day * £14 per pack
        val averagePacksPerDay = AVERAGE_CIGARETTES_PER_DAY_UK / 20.0
        return SMOKERS_IN_UK * averagePacksPerDay * AVERAGE_UK_PACK_PRICE
    }

    data class LiveStatistic(
        val timestamp: LocalDateTime,
        val globalDeaths: Int,
        val ukDeaths: Int,
        val ukDailySpend: Double,
        val ukYearlySpend: Double
    )

    /**
     * Simulates live death counter
     * Increments based on realistic WHO statistics
     */
    fun observeGlobalDeaths(startingCount: Int = 0): Flow<Int> = flow {
        var count = startingCount
        val millisecondsPerDeath = (1000.0 / DEATHS_PER_SECOND_GLOBAL).toLong()

        while (true) {
            emit(count)
            delay(millisecondsPerDeath)
            count++
        }
    }

    /**
     * Simulates live UK expenditure counter
     * Increments based on realistic spending patterns
     */
    fun observeUKExpenditure(): Flow<Double> = flow {
        val startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0)
        val expenditurePerSecond = DAILY_UK_EXPENDITURE / 86400.0 // Seconds in a day

        while (true) {
            val now = LocalDateTime.now()
            val secondsSinceStartOfDay = java.time.Duration.between(startOfDay, now).seconds
            val currentExpenditure = secondsSinceStartOfDay * expenditurePerSecond

            emit(currentExpenditure)
            delay(100) // Update every 100ms for smooth animation
        }
    }

    /**
     * Get statistics snapshot
     */
    fun getCurrentStatistics(): LiveStatistic {
        val now = LocalDateTime.now()
        val dayOfYear = now.dayOfYear

        return LiveStatistic(
            timestamp = now,
            globalDeaths = (DEATHS_PER_DAY_GLOBAL * dayOfYear).toInt(),
            ukDeaths = (DEATHS_PER_DAY_UK * dayOfYear).toInt(),
            ukDailySpend = DAILY_UK_EXPENDITURE,
            ukYearlySpend = ANNUAL_UK_EXPENDITURE
        )
    }

    data class HealthImpact(
        val cigarettesSmoked: Long,
        val minutesOfLifeLost: Long,
        val moneySpent: Double,
        val chemicalsInhaled: Long,
        val tarInhaled: Double // in grams
    ) {
        companion object {
            fun calculate(cigarettesPerDay: Int, daysSmoked: Int, pricePerPack: Double): HealthImpact {
                val totalCigarettes = cigarettesPerDay * daysSmoked.toLong()

                // Each cigarette reduces life expectancy by ~11 minutes (studies)
                val minutesOfLifeLost = totalCigarettes * 11

                // Money spent
                val packsSmoked = totalCigarettes / 20.0
                val moneySpent = packsSmoked * pricePerPack

                // Each cigarette contains ~7000 chemicals, 250+ harmful
                val chemicalsInhaled = totalCigarettes * 7000

                // Average 10-12mg tar per cigarette
                val tarInhaled = (totalCigarettes * 11.0) / 1000.0 // in grams

                return HealthImpact(
                    cigarettesSmoked = totalCigarettes,
                    minutesOfLifeLost = minutesOfLifeLost,
                    moneySpent = moneySpent,
                    chemicalsInhaled = chemicalsInhaled,
                    tarInhaled = tarInhaled
                )
            }
        }
    }
}
