package com.uktobacco.data

data class TimelineEvent(
    val year: Int,
    val title: String,
    val description: String,
    val isKeyMilestone: Boolean = false
)

data class CompanyHistory(
    val brandName: String,
    val founded: Int,
    val founder: String,
    val headquarters: String,
    val tagline: String,
    val description: String,
    val timeline: List<TimelineEvent>,
    val facts: List<String>,
    val logoColor: androidx.compose.ui.graphics.Color
)

object CompanyHistoryRepository {

    fun getCompanyHistory(brandName: String): CompanyHistory? {
        return companyHistories[brandName]
    }

    fun getAllBrands(): List<String> = companyHistories.keys.toList()

    private val companyHistories = mapOf(
        "Marlboro" to CompanyHistory(
            brandName = "Marlboro",
            founded = 1924,
            founder = "Philip Morris",
            headquarters = "Richmond, Virginia, USA",
            tagline = "Come to where the flavor is",
            description = "Marlboro is the world's best-selling cigarette brand, known for its iconic cowboy imagery and premium quality. Originally marketed as a women's cigarette in the 1920s, it was repositioned in the 1950s as a masculine brand, becoming a global phenomenon.",
            timeline = listOf(
                TimelineEvent(1847, "Philip Morris Founded", "Philip Morris opens a tobacco shop in London", true),
                TimelineEvent(1924, "Marlboro Introduced", "Marlboro cigarettes launched as a women's brand with the slogan 'Mild as May'"),
                TimelineEvent(1955, "Marlboro Man Campaign", "Iconic Marlboro Man advertising campaign begins, transforming the brand", true),
                TimelineEvent(1972, "World's Top Brand", "Marlboro becomes the world's best-selling cigarette brand", true),
                TimelineEvent(1980, "Marlboro Lights", "Introduction of Marlboro Lights to meet changing consumer preferences"),
                TimelineEvent(1990, "Global Expansion", "Marlboro expands significantly into international markets"),
                TimelineEvent(2000, "Premium Positioning", "Brand reinforces premium quality positioning"),
                TimelineEvent(2024, "100th Anniversary", "Marlboro celebrates a century of brand heritage", true)
            ),
            facts = listOf(
                "Best-selling cigarette brand worldwide for over 50 years",
                "The Marlboro Man campaign is considered one of the most successful advertising campaigns in history",
                "Available in over 180 countries",
                "Originally had a red filter tip to hide lipstick stains"
            ),
            logoColor = androidx.compose.ui.graphics.Color(0xFFCC0000)
        ),

        "Lambert & Butler" to CompanyHistory(
            brandName = "Lambert & Butler",
            founded = 1834,
            founder = "William Lambert",
            headquarters = "London, United Kingdom",
            tagline = "Britain's Favourite",
            description = "Lambert & Butler is one of the UK's oldest and most popular cigarette brands. With nearly two centuries of heritage, it represents British tobacco craftsmanship and has been a staple in UK newsagents since the Victorian era.",
            timeline = listOf(
                TimelineEvent(1834, "Company Founded", "William Lambert establishes tobacco business in London", true),
                TimelineEvent(1901, "Merger", "Lambert merges with Butler to form Lambert & Butler"),
                TimelineEvent(1920, "Mass Production", "Introduces modern manufacturing techniques"),
                TimelineEvent(1970, "UK Market Leader", "Becomes one of the top-selling brands in the UK", true),
                TimelineEvent(1990, "Imperial Tobacco", "Acquired by Imperial Tobacco Group"),
                TimelineEvent(2010, "Modern Range", "Expands product range with various strengths and formats")
            ),
            facts = listOf(
                "One of the UK's oldest tobacco brands with 190 years of history",
                "Consistently in the top 3 best-selling cigarette brands in the UK",
                "Originally supplied tobacco to the British Royal Family",
                "The brand name combines two historic tobacco merchants"
            ),
            logoColor = androidx.compose.ui.graphics.Color(0xFF1E3A8A)
        ),

        "Richmond" to CompanyHistory(
            brandName = "Richmond",
            founded = 1968,
            founder = "Imperial Tobacco",
            headquarters = "Bristol, United Kingdom",
            tagline = "Quality at the Right Price",
            description = "Richmond is a popular UK value cigarette brand known for offering quality tobacco at an accessible price point. It has become one of the best-selling brands in the UK market.",
            timeline = listOf(
                TimelineEvent(1968, "Brand Launch", "Richmond introduced to the UK market", true),
                TimelineEvent(1980, "Superkings Format", "Launches Richmond Superkings, a longer format cigarette"),
                TimelineEvent(2000, "Market Growth", "Becomes a leading value brand in the UK"),
                TimelineEvent(2015, "New Packaging", "Updated packaging design while maintaining heritage")
            ),
            facts = listOf(
                "One of the UK's best-selling value cigarette brands",
                "Named after Richmond, Virginia, the historic tobacco capital",
                "Superkings format is among the most popular in the UK",
                "Known for consistent quality at competitive pricing"
            ),
            logoColor = androidx.compose.ui.graphics.Color(0xFF991B1B)
        ),

        "Benson & Hedges" to CompanyHistory(
            brandName = "Benson & Hedges",
            founded = 1873,
            founder = "Richard Benson and William Hedges",
            headquarters = "London, United Kingdom",
            tagline = "Pure Gold",
            description = "Benson & Hedges is a British luxury cigarette brand with a rich heritage. Known for its gold packaging and premium positioning, it has been associated with sophistication and quality since the Victorian era.",
            timeline = listOf(
                TimelineEvent(1873, "Partnership Formed", "Richard Benson and William Hedges establish partnership in London", true),
                TimelineEvent(1900, "Royal Warrant", "Receives Royal Warrant as tobacconist to the British Royal Family", true),
                TimelineEvent(1954, "Filter Cigarettes", "Introduces filter-tipped cigarettes"),
                TimelineEvent(1960, "Gold Packaging", "Launches iconic gold pack design", true),
                TimelineEvent(1984, "Silk Cut Success", "Becomes part of Gallaher Group"),
                TimelineEvent(2007, "Japan Tobacco", "Acquired by Japan Tobacco International")
            ),
            facts = listOf(
                "Held a Royal Warrant for over 100 years",
                "The gold packaging became an iconic design in the tobacco industry",
                "One of the first brands to introduce king-size filter cigarettes in the UK",
                "Known for innovative advertising campaigns in the 1970s-80s"
            ),
            logoColor = androidx.compose.ui.graphics.Color(0xFFD97706)
        ),

        "Amber Leaf" to CompanyHistory(
            brandName = "Amber Leaf",
            founded = 1993,
            founder = "JTI (Japan Tobacco International)",
            headquarters = "Geneva, Switzerland",
            tagline = "Feel the Spirit",
            description = "Amber Leaf is the UK's leading hand-rolling tobacco brand. Known for its smooth, medium-strength Virginia tobacco blend, it has become synonymous with quality roll-your-own tobacco.",
            timeline = listOf(
                TimelineEvent(1993, "Brand Launch", "Amber Leaf introduced to UK market", true),
                TimelineEvent(2000, "Market Leader", "Becomes UK's number one rolling tobacco brand", true),
                TimelineEvent(2010, "Product Range", "Expands range with different pouch sizes"),
                TimelineEvent(2020, "Sustainability", "Introduces eco-friendly packaging initiatives")
            ),
            facts = listOf(
                "UK's best-selling hand-rolling tobacco for over 20 years",
                "Made from premium Virginia tobacco leaves",
                "Available in various sizes from 12.5g to 50g pouches",
                "Known for its distinctive amber-colored packaging"
            ),
            logoColor = androidx.compose.ui.graphics.Color(0xFFB45309)
        ),

        "Golden Virginia" to CompanyHistory(
            brandName = "Golden Virginia",
            founded = 1877,
            founder = "Imperial Tobacco",
            headquarters = "Bristol, United Kingdom",
            tagline = "The Original",
            description = "Golden Virginia is one of the oldest and most respected hand-rolling tobacco brands in the UK. With over 140 years of heritage, it's known for its distinctive golden Virginia tobacco blend.",
            timeline = listOf(
                TimelineEvent(1877, "Brand Created", "Golden Virginia tobacco first blended", true),
                TimelineEvent(1950, "National Distribution", "Expands to become nationally available"),
                TimelineEvent(1980, "Market Leader", "Establishes position as premium rolling tobacco"),
                TimelineEvent(2005, "New Blends", "Introduces Yellow and Original variants"),
                TimelineEvent(2015, "Heritage Packaging", "Updates packaging while celebrating heritage")
            ),
            facts = listOf(
                "Over 145 years of tobacco blending heritage",
                "Made from specially selected Virginia tobacco leaves",
                "One of the smoothest rolling tobacco blends available",
                "Consistently voted among the UK's favorite rolling tobacco brands"
            ),
            logoColor = androidx.compose.ui.graphics.Color(0xFFCA8A04)
        ),

        "Mayfair" to CompanyHistory(
            brandName = "Mayfair",
            founded = 1992,
            founder = "Gallaher Group",
            headquarters = "Weybridge, United Kingdom",
            tagline = "Smooth and Satisfying",
            description = "Mayfair is a popular British cigarette brand known for its smooth taste and accessible pricing. It has grown to become one of the UK's favorite cigarette brands.",
            timeline = listOf(
                TimelineEvent(1992, "Brand Launch", "Mayfair introduced to UK market", true),
                TimelineEvent(2000, "Rapid Growth", "Becomes one of UK's fastest-growing cigarette brands"),
                TimelineEvent(2007, "JTI Acquisition", "Becomes part of JTI portfolio"),
                TimelineEvent(2015, "Product Innovation", "Launches new variants and pack sizes")
            ),
            facts = listOf(
                "Named after London's prestigious Mayfair district",
                "Known for smooth, accessible taste",
                "One of the top-selling cigarette brands in the UK",
                "Available in various formats including King Size and Superkings"
            ),
            logoColor = androidx.compose.ui.graphics.Color(0xFF7C2D12)
        )
    )
}
