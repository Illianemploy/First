package com.uktobacco.data

import androidx.compose.ui.graphics.Color

/**
 * Brand-specific packaging designs using old-style (pre-plain packaging) branding
 * This ensures each brand is visually distinctive and recognizable
 */
data class BrandDesign(
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val textColor: Color,
    val hasChevron: Boolean = false,
    val hasCircleLogo: Boolean = false,
    val logoStyle: LogoStyle = LogoStyle.STANDARD
)

enum class LogoStyle {
    STANDARD,      // Regular rectangular branding
    CHEVRON,       // Marlboro-style chevron
    CIRCLE,        // Lucky Strike-style circle
    PREMIUM_GOLD,  // Benson & Hedges gold style
    VINTAGE        // Classic heritage styling
}

object BrandDesigns {

    // Classic brand color schemes (pre-plain packaging era)
    private val designs = mapOf(
        // Marlboro - Iconic red and white with chevron
        "Marlboro Red" to BrandDesign(
            primaryColor = Color(0xFFDC143C),    // Crimson red
            secondaryColor = Color(0xFFFFFFFF),   // White
            accentColor = Color(0xFFB22222),      // Darker red
            textColor = Color(0xFFFFFFFF),
            hasChevron = true,
            logoStyle = LogoStyle.CHEVRON
        ),

        "Marlboro Gold" to BrandDesign(
            primaryColor = Color(0xFFFFD700),     // Gold
            secondaryColor = Color(0xFFFFFFFF),   // White
            accentColor = Color(0xFFDAA520),      // Goldenrod
            textColor = Color(0xFF8B4513),        // Brown text
            hasChevron = true,
            logoStyle = LogoStyle.CHEVRON
        ),

        // Richmond - Blue and silver
        "Richmond" to BrandDesign(
            primaryColor = Color(0xFF1E3A8A),     // Royal blue
            secondaryColor = Color(0xFFC0C0C0),   // Silver
            accentColor = Color(0xFF60A5FA),      // Light blue
            textColor = Color(0xFFFFFFFF)
        ),

        // Lambert & Butler - Classic blue and silver
        "Lambert & Butler" to BrandDesign(
            primaryColor = Color(0xFF3B82F6),     // Blue
            secondaryColor = Color(0xFFE5E7EB),   // Light grey/silver
            accentColor = Color(0xFF1E40AF),      // Dark blue
            textColor = Color(0xFFFFFFFF)
        ),

        // Benson & Hedges - Premium gold packaging
        "Benson & Hedges" to BrandDesign(
            primaryColor = Color(0xFFFFD700),     // Gold
            secondaryColor = Color(0xFFF4F4F4),   // Off-white
            accentColor = Color(0xFFDAA520),      // Darker gold
            textColor = Color(0xFF000000),
            logoStyle = LogoStyle.PREMIUM_GOLD
        ),

        // Mayfair - Red and white
        "Mayfair" to BrandDesign(
            primaryColor = Color(0xFFDC2626),     // Red
            secondaryColor = Color(0xFFFFFFFF),   // White
            accentColor = Color(0xFF991B1B),      // Dark red
            textColor = Color(0xFFFFFFFF)
        ),

        // Sterling - Distinctive green/blue
        "Sterling" to BrandDesign(
            primaryColor = Color(0xFF059669),     // Emerald green
            secondaryColor = Color(0xFF3B82F6),   // Blue
            accentColor = Color(0xFF047857),      // Dark green
            textColor = Color(0xFFFFFFFF)
        ),

        // Royals - Red and gold (premium style)
        "Royals" to BrandDesign(
            primaryColor = Color(0xFF991B1B),     // Dark red
            secondaryColor = Color(0xFFFFD700),   // Gold
            accentColor = Color(0xFF7F1D1D),      // Darker red
            textColor = Color(0xFFFFFFFF),
            logoStyle = LogoStyle.VINTAGE
        ),

        // JPS - Black and gold
        "JPS" to BrandDesign(
            primaryColor = Color(0xFF1F2937),     // Dark grey/black
            secondaryColor = Color(0xFFFFD700),   // Gold
            accentColor = Color(0xFF111827),      // Black
            textColor = Color(0xFFFFD700)
        ),

        // Pall Mall - Classic red and white
        "Pall Mall" to BrandDesign(
            primaryColor = Color(0xFFEF4444),     // Red
            secondaryColor = Color(0xFFFFFFFF),   // White
            accentColor = Color(0xFFDC2626),      // Darker red
            textColor = Color(0xFFFFFFFF)
        ),

        // Camel - Iconic beige/tan with gold
        "Camel" to BrandDesign(
            primaryColor = Color(0xFFD4A574),     // Camel/tan
            secondaryColor = Color(0xFFFFD700),   // Gold
            accentColor = Color(0xFFC19A6B),      // Darker tan
            textColor = Color(0xFF8B4513),        // Brown
            logoStyle = LogoStyle.VINTAGE
        ),

        // Lucky Strike - Iconic red circle on white
        "Lucky Strike" to BrandDesign(
            primaryColor = Color(0xFFFFFFFF),     // White
            secondaryColor = Color(0xFFDC143C),   // Red
            accentColor = Color(0xFF000000),      // Black
            textColor = Color(0xFF000000),
            hasCircleLogo = true,
            logoStyle = LogoStyle.CIRCLE
        ),

        // Windsor - Blue
        "Windsor" to BrandDesign(
            primaryColor = Color(0xFF2563EB),     // Bright blue
            secondaryColor = Color(0xFFFFFFFF),   // White
            accentColor = Color(0xFF1E40AF),      // Dark blue
            textColor = Color(0xFFFFFFFF)
        ),

        // Sovereign - Red and gold
        "Sovereign" to BrandDesign(
            primaryColor = Color(0xFFB91C1C),     // Red
            secondaryColor = Color(0xFFFFD700),   // Gold
            accentColor = Color(0xFF991B1B),      // Dark red
            textColor = Color(0xFFFFFFFF),
            logoStyle = LogoStyle.VINTAGE
        ),

        // Berkeley - Blue and silver
        "Berkeley" to BrandDesign(
            primaryColor = Color(0xFF0EA5E9),     // Sky blue
            secondaryColor = Color(0xFFD1D5DB),   // Silver/grey
            accentColor = Color(0xFF0284C7),      // Dark blue
            textColor = Color(0xFFFFFFFF)
        )
    )

    // Default design for unknown brands
    private val defaultDesign = BrandDesign(
        primaryColor = Color(0xFF3B82F6),
        secondaryColor = Color(0xFFFFFFFF),
        accentColor = Color(0xFF1E40AF),
        textColor = Color(0xFFFFFFFF)
    )

    /**
     * Get the brand-specific design by product name or brand name
     * Supports both specific variants (e.g., "Marlboro Gold") and general brands (e.g., "Marlboro")
     */
    fun getDesign(productName: String, brandName: String): BrandDesign {
        // First try exact product name match
        designs[productName]?.let { return it }

        // Then try matching by brand name
        designs[brandName]?.let { return it }

        // Try partial match on product name
        designs.entries.firstOrNull { (key, _) ->
            productName.contains(key, ignoreCase = true)
        }?.value?.let { return it }

        // Return default if no match found
        return defaultDesign
    }

    /**
     * Get all available brand designs for testing/preview
     */
    fun getAllDesigns(): Map<String, BrandDesign> = designs
}
