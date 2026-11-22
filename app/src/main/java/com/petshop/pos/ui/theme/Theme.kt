package com.petshop.pos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = OneUiBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF0D253F),
    onPrimaryContainer = OneUiBlueTint,
    secondary = OneUiAmber,
    onSecondary = OneUiOledBlack,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF452200),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD599),
    tertiary = OneUiCoral,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFF4C100D),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFB4AB),
    background = OneUiOledBlack,
    onBackground = OnSurfaceDark,
    surface = OneUiDarkSurface,
    onSurface = OnSurfaceDark,
    surfaceVariant = OneUiDarkSurface2,
    onSurfaceVariant = OnSurfaceMutedDark,
    outline = OneUiDarkDivider,
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF3A3A3C),
    surfaceContainer = OneUiDarkSurface,
    surfaceContainerHigh = OneUiDarkSurface2,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF000000),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF141416)
)

private val LightColorScheme = lightColorScheme(
    primary = OneUiBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = OneUiBlueTint,
    onPrimaryContainer = OneUiBlueDark,
    secondary = OneUiAmber,
    onSecondary = OneUiOledBlack,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFEF3C7),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF92400E),
    tertiary = OneUiCoral,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = androidx.compose.ui.graphics.Color(0xFFFFE5E5),
    onTertiaryContainer = androidx.compose.ui.graphics.Color(0xFFB91C1C),
    background = OneUiLightCanvas,
    onBackground = OnSurfaceLight,
    surface = OneUiLightSurface,
    onSurface = OnSurfaceLight,
    surfaceVariant = OneUiLightSurface2,
    onSurfaceVariant = OnSurfaceMutedLight,
    outline = OneUiLightDivider,
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFD1D1D6),
    surfaceContainer = OneUiLightSurface,
    surfaceContainerHigh = OneUiLightSurface2,
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFFF7F7FA)
)

/** One UI squircle scale from DESIGN.md: 14 chips/fields, 16 list items, 22 cards, 26 sheets/dialogs. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun PetShopPOSTheme(
    darkTheme: Boolean = true, // Default to Samsung One UI OLED Dark Mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
