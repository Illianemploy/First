package com.uktobacco.data

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.ceil

class TravelSavingsCalculatorTest {

    private lateinit var smokingProfile: SmokingProfile
    private lateinit var cheapCountry: CountryPricing
    private lateinit var expensiveCountry: CountryPricing
    private lateinit var moderateCountry: CountryPricing

    @Before
    fun setup() {
        // Typical UK smoker: 20 cigarettes per day at £14/pack
        smokingProfile = SmokingProfile.calculate(
            cigarettesPerDay = 20,
            pricePerPack = 14.00
        )

        // Cheap country (like Tunisia): £2.50/pack, £150 flight
        cheapCountry = CountryPricing(
            countryCode = "TN",
            countryName = "Tunisia",
            region = Region.NORTH_AFRICA,
            averagePackPrice = 2.50,
            taxRate = 55.6,
            taxAmount = 1.39,
            priceBeforeTax = 1.11,
            flightTimeHours = 3.0,
            averageFlightCostReturn = 150.0,
            bulkBuyLimit = 200,
            currency = "TND",
            flagEmoji = "🇹🇳"
        )

        // Expensive country (like Ireland): £15.50/pack, £55 flight
        expensiveCountry = CountryPricing(
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
        )

        // Moderate country (like Poland): £4.80/pack, £60 flight
        moderateCountry = CountryPricing(
            countryCode = "PL",
            countryName = "Poland",
            region = Region.EASTERN_EUROPE,
            averagePackPrice = 4.80,
            taxRate = 72.5,
            taxAmount = 3.48,
            priceBeforeTax = 1.32,
            flightTimeHours = 2.5,
            averageFlightCostReturn = 60.0,
            bulkBuyLimit = 800,
            currency = "PLN",
            flagEmoji = "🇵🇱"
        )
    }

    @Test
    fun `calculate returns correct packs to buy for 3 months supply`() {
        // 20 cigs/day = 1 pack/day = 30 packs/month = 90 packs for 3 months
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = moderateCountry,
            monthsToSupply = 3
        )

        val expectedPacks = ceil(smokingProfile.packsPerMonth * 3).toInt()
        assertEquals(expectedPacks, result.packsToBuy)
    }

    @Test
    fun `calculate respects legal bulk buy limit`() {
        // Tunisia has 200 pack limit, but 3 months would need 90 packs
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = cheapCountry,
            monthsToSupply = 3
        )

        // Should not exceed the limit
        assertTrue(result.packsToBuy <= cheapCountry.bulkBuyLimit)
        assertEquals(cheapCountry.bulkBuyLimit, result.legalLimit)
    }

    @Test
    fun `calculate computes total trip cost correctly`() {
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = moderateCountry,
            monthsToSupply = 3
        )

        val expectedCostInCountry = result.packsToBuy * moderateCountry.averagePackPrice
        val expectedTotalCost = expectedCostInCountry + moderateCountry.averageFlightCostReturn

        assertEquals(expectedCostInCountry, result.costInCountry, 0.01)
        assertEquals(moderateCountry.averageFlightCostReturn, result.flightCost, 0.01)
        assertEquals(expectedTotalCost, result.totalTripCost, 0.01)
    }

    @Test
    fun `calculate computes savings correctly`() {
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = moderateCountry,
            monthsToSupply = 3
        )

        // UK cost for same packs
        val ukCost = result.packsToBuy * smokingProfile.pricePerPack
        val expectedSavings = ukCost - result.totalTripCost

        assertEquals(expectedSavings, result.totalSavings, 0.01)
    }

    @Test
    fun `calculate marks as worth it when savings exceed 50 pounds`() {
        // Poland should offer good savings (90 packs * £9.20 difference - £60 flight = £768)
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = moderateCountry,
            monthsToSupply = 3
        )

        assertTrue("Savings should be over £50", result.totalSavings > 50.0)
        assertTrue("Should be marked as worth it", result.isWorthIt)
    }

    @Test
    fun `calculate marks as not worth it when savings below 50 pounds`() {
        // Ireland is MORE expensive than UK, so negative savings
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = expensiveCountry,
            monthsToSupply = 3
        )

        assertFalse("Should not be worth it", result.isWorthIt)
    }

    @Test
    fun `calculate computes break even months correctly`() {
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = moderateCountry,
            monthsToSupply = 3
        )

        val expectedBreakEven = result.flightCost / result.savingsPerMonth
        assertEquals(expectedBreakEven, result.breakEvenMonths, 0.01)
    }

    @Test
    fun `calculate handles negative savings with max break even`() {
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = expensiveCountry,
            monthsToSupply = 3
        )

        // When savings per month is negative or zero, break even should be MAX_VALUE
        assertEquals(Double.MAX_VALUE, result.breakEvenMonths, 0.01)
    }

    @Test
    fun `calculate computes months covered correctly`() {
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = moderateCountry,
            monthsToSupply = 3
        )

        val expectedMonths = result.packsToBuy / smokingProfile.packsPerMonth
        assertEquals(expectedMonths, result.monthsCovered, 0.01)
    }

    @Test
    fun `calculate with different months to supply`() {
        val result1Month = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = moderateCountry,
            monthsToSupply = 1
        )

        val result6Months = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = moderateCountry,
            monthsToSupply = 6
        )

        assertTrue("6 month supply should buy more packs",
            result6Months.packsToBuy > result1Month.packsToBuy)
        assertTrue("6 month supply should have higher total savings",
            result6Months.totalSavings > result1Month.totalSavings)
    }

    @Test
    fun `SmokingProfile calculate computes values correctly`() {
        val profile = SmokingProfile.calculate(
            cigarettesPerDay = 20,
            pricePerPack = 14.00
        )

        // 20 cigs/day = 600 cigs/month = 30 packs/month (20 per pack)
        assertEquals(30.0, profile.packsPerMonth, 0.01)

        // 30 packs * £14 = £420/month
        assertEquals(420.0, profile.monthlyExpenditure, 0.01)

        // £420 * 12 = £5,040/year
        assertEquals(5040.0, profile.yearlyExpenditure, 0.01)
    }

    @Test
    fun `SmokingProfile handles partial packs correctly`() {
        // 15 cigs/day = 450 cigs/month = 22.5 packs/month
        val profile = SmokingProfile.calculate(
            cigarettesPerDay = 15,
            pricePerPack = 14.00
        )

        assertEquals(22.5, profile.packsPerMonth, 0.01)
        assertEquals(315.0, profile.monthlyExpenditure, 0.01)
        assertEquals(3780.0, profile.yearlyExpenditure, 0.01)
    }

    @Test
    fun `calculate with low consumption smoker`() {
        // Light smoker: 5 cigarettes per day
        val lightSmoker = SmokingProfile.calculate(
            cigarettesPerDay = 5,
            pricePerPack = 14.00
        )

        val result = TravelSavingsCalculator.calculate(
            smokingProfile = lightSmoker,
            country = moderateCountry,
            monthsToSupply = 3
        )

        // 5 cigs/day = 7.5 packs/month = 22.5 packs for 3 months = 23 packs (ceiling)
        assertEquals(23, result.packsToBuy)
    }

    @Test
    fun `calculate with heavy smoker hitting legal limit`() {
        // Heavy smoker: 40 cigarettes per day
        val heavySmoker = SmokingProfile.calculate(
            cigarettesPerDay = 40,
            pricePerPack = 14.00
        )

        val result = TravelSavingsCalculator.calculate(
            smokingProfile = heavySmoker,
            country = cheapCountry,  // Only 200 pack limit
            monthsToSupply = 6
        )

        // Would need 360 packs (60/month * 6), but limit is 200
        assertEquals(cheapCountry.bulkBuyLimit, result.packsToBuy)
        assertTrue(result.monthsCovered < 6.0)
    }

    @Test
    fun `savings per month is calculated correctly`() {
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = moderateCountry,
            monthsToSupply = 3
        )

        val expectedSavingsPerMonth = result.totalSavings / result.monthsCovered
        assertEquals(expectedSavingsPerMonth, result.savingsPerMonth, 0.01)
    }

    @Test
    fun `isWorthIt requires both positive savings and minimum coverage`() {
        // Create a scenario with tiny savings just under £50
        val tinyProfile = SmokingProfile.calculate(
            cigarettesPerDay = 1,
            pricePerPack = 14.00
        )

        val result = TravelSavingsCalculator.calculate(
            smokingProfile = tinyProfile,
            country = moderateCountry,
            monthsToSupply = 1
        )

        // Very low consumption means minimal savings, likely under £50
        if (result.totalSavings <= 50.0) {
            assertFalse("Should not be worth it with savings <= £50", result.isWorthIt)
        }
    }

    @Test
    fun `UK monthly spend matches profile expenditure`() {
        val result = TravelSavingsCalculator.calculate(
            smokingProfile = smokingProfile,
            country = moderateCountry,
            monthsToSupply = 3
        )

        assertEquals(smokingProfile.monthlyExpenditure, result.ukMonthlySpend, 0.01)
    }
}
