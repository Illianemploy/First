package com.uktobacco.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val UberColorScheme = darkColorScheme(
    primary = UberGreen,
    onPrimary = UberBlack,
    primaryContainer = UberGreenDark,
    onPrimaryContainer = UberTextPrimary,

    secondary = CigaretteBlue,
    onSecondary = UberTextPrimary,
    secondaryContainer = Color(0xFF004C99),
    onSecondaryContainer = UberTextPrimary,

    tertiary = TobaccoOrange,
    onTertiary = UberBlack,
    tertiaryContainer = Color(0xFFCC7700),
    onTertiaryContainer = UberTextPrimary,

    background = UberBlack,
    onBackground = UberTextPrimary,

    surface = UberSurface,
    onSurface = UberTextPrimary,
    surfaceVariant = UberSurfaceVariant,
    onSurfaceVariant = UberTextSecondary,

    surfaceTint = UberGreen,
    inverseSurface = UberTextPrimary,
    inverseOnSurface = UberBlack,

    error = UberError,
    onError = UberTextPrimary,
    errorContainer = Color(0xFF990000),
    onErrorContainer = UberTextPrimary,

    outline = UberLightGray,
    outlineVariant = UberMediumGray,

    scrim = Color(0x99000000)
)

@Composable
fun UKTobaccoPricesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = UberColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = UberTypography,
        content = content
    )
}
