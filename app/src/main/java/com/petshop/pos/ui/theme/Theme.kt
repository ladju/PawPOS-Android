package com.petshop.pos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = OneUiBlue,
    secondary = OneUiAmber,
    tertiary = OneUiCoral,
    background = OneUiOledBlack,
    surface = OneUiDarkSurface,
    surfaceVariant = OneUiDarkSurface2,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = OneUiOledBlack,
    onBackground = androidx.compose.ui.graphics.Color(0xFFFAFAFA),
    onSurface = androidx.compose.ui.graphics.Color(0xFFFAFAFA)
)

private val LightColorScheme = lightColorScheme(
    primary = OneUiBlue,
    secondary = OneUiAmber,
    tertiary = OneUiCoral,
    background = OneUiLightCanvas,
    surface = OneUiLightSurface,
    surfaceVariant = OneUiLightSurface2,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = OneUiOledBlack,
    onBackground = OneUiOledBlack,
    onSurface = OneUiOledBlack
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
        content = content
    )
}
