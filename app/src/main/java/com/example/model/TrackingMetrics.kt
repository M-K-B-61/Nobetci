package com.example.model

import android.graphics.RectF

enum class AppThemeMode(val title: String) {
    DARK("Koyu Tema"),
    LIGHT("Açık Tema"),
    SYSTEM("Sistem Varsayılanı")
}

data class TrackingMetrics(
    val trackingState: TrackingState = TrackingState.STOPPED,
    val vehicleType: VehicleType = VehicleType.HEAVY_VEHICLE,
    val fpsMode: AnalysisFpsMode = AnalysisFpsMode.IDLE_WATCH,
    val targetConfidence: Float = 0f,
    val targetMotionScore: Float = 0f,
    val globalMotionScore: Float = 0f,
    val sensorMotionScore: Float = 0f,
    val scaleChangeScore: Float = 0f,
    val movementConfidence: Float = 0f,
    val targetRoi: RectF? = null,
    val currentFps: Float = 0f,
    val processingTimeMs: Long = 0L,
    val batteryPct: Int = 100,
    val thermalStatusString: String = "Normal",
    val estimatedDistanceMeters: Float = 10f,
    val isPitchBlackMode: Boolean = true,
    val verificationProgress: Float = 0f
)

data class AppSettings(
    val selectedVehicleType: VehicleType = VehicleType.HEAVY_VEHICLE,
    val sensitivity: Sensitivity = Sensitivity.NORMAL,
    val approximateDistanceMeters: Int = 10,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val accentColor: AccentColor = AccentColor.BLUE,
    val fullDarkModeEnabled: Boolean = true,
    val soundAlertEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val thermalProtectionEnabled: Boolean = true,
    val hasAcceptedSafetyDisclaimer: Boolean = false,
    val isOnboardingCompleted: Boolean = false
)
