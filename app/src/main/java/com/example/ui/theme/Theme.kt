package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AccentColor
import com.example.model.AppThemeMode

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    accentColor: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = accentColor.darkPrimary,
            onPrimary = accentColor.onPrimaryColor,
            primaryContainer = accentColor.darkContainer,
            onPrimaryContainer = Color.White,
            secondary = SentryAmber,
            onSecondary = PitchBlack,
            secondaryContainer = Color(0xFF452B00),
            onSecondaryContainer = Color(0xFFFFDFA0),
            tertiary = ElectricBlue,
            background = DarkBackground,
            onBackground = DarkTextPrimary,
            surface = DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkBorder,
            error = SentryAlertRed,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = accentColor.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = accentColor.lightContainer,
            onPrimaryContainer = accentColor.darkContainer,
            secondary = Color(0xFFD97706),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFEF3C7),
            onSecondaryContainer = Color(0xFF78350F),
            tertiary = Color(0xFF0284C7),
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightTextSecondary,
            outline = LightBorder,
            error = SentryAlertRed,
            onError = Color.White
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
