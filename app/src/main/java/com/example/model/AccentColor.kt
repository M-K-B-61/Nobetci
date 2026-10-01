package com.example.model

import androidx.compose.ui.graphics.Color

enum class AccentColor(
    val title: String,
    val darkPrimary: Color,
    val lightPrimary: Color,
    val darkContainer: Color,
    val lightContainer: Color,
    val onPrimaryColor: Color
) {
    BLUE(
        title = "Mavi (Varsayılan)",
        darkPrimary = Color(0xFF00B0FF),
        lightPrimary = Color(0xFF0284C7),
        darkContainer = Color(0xFF003855),
        lightContainer = Color(0xFFE0F2FE),
        onPrimaryColor = Color(0xFF001E30)
    ),
    EMERALD(
        title = "Zümrüt Yeşili",
        darkPrimary = Color(0xFF00E676),
        lightPrimary = Color(0xFF059669),
        darkContainer = Color(0xFF003D1E),
        lightContainer = Color(0xFFD1FAE5),
        onPrimaryColor = Color(0xFF002914)
    ),
    AMBER(
        title = "Kehribar Sarısı",
        darkPrimary = Color(0xFFFFB300),
        lightPrimary = Color(0xFFD97706),
        darkContainer = Color(0xFF4A3400),
        lightContainer = Color(0xFFFEF3C7),
        onPrimaryColor = Color(0xFF261900)
    ),
    PURPLE(
        title = "Kozmik Mor",
        darkPrimary = Color(0xFFA855F7),
        lightPrimary = Color(0xFF7C3AED),
        darkContainer = Color(0xFF3B0764),
        lightContainer = Color(0xFFF3E8FF),
        onPrimaryColor = Color(0xFF1E003A)
    )
}
