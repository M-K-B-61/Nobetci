package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AccentColor
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.Sensitivity
import com.example.model.VehicleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("nobetci_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val vehicleName = prefs.getString("vehicle_type", VehicleType.HEAVY_VEHICLE.name)
        val vehicleType = try {
            VehicleType.valueOf(vehicleName ?: VehicleType.HEAVY_VEHICLE.name)
        } catch (_: Exception) {
            VehicleType.HEAVY_VEHICLE
        }

        val sensName = prefs.getString("sensitivity", Sensitivity.NORMAL.name)
        val sensitivity = try {
            Sensitivity.valueOf(sensName ?: Sensitivity.NORMAL.name)
        } catch (_: Exception) {
            Sensitivity.NORMAL
        }

        val themeName = prefs.getString("theme_mode", AppThemeMode.DARK.name)
        val themeMode = try {
            AppThemeMode.valueOf(themeName ?: AppThemeMode.DARK.name)
        } catch (_: Exception) {
            AppThemeMode.DARK
        }

        val accentName = prefs.getString("accent_color", AccentColor.BLUE.name)
        val accentColor = try {
            AccentColor.valueOf(accentName ?: AccentColor.BLUE.name)
        } catch (_: Exception) {
            AccentColor.BLUE
        }

        return AppSettings(
            selectedVehicleType = vehicleType,
            sensitivity = sensitivity,
            approximateDistanceMeters = prefs.getInt("distance_meters", 10),
            themeMode = themeMode,
            accentColor = accentColor,
            fullDarkModeEnabled = prefs.getBoolean("full_dark_mode", true),
            soundAlertEnabled = prefs.getBoolean("sound_alert", true),
            vibrationEnabled = prefs.getBoolean("vibration", true),
            thermalProtectionEnabled = prefs.getBoolean("thermal_protection", true),
            hasAcceptedSafetyDisclaimer = prefs.getBoolean("accepted_safety", false),
            isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        )
    }

    fun updateThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun updateAccentColor(accent: AccentColor) {
        prefs.edit().putString("accent_color", accent.name).apply()
        _settings.value = _settings.value.copy(accentColor = accent)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean("onboarding_completed", completed).apply()
        _settings.value = _settings.value.copy(isOnboardingCompleted = completed)
    }

    fun updateVehicleType(type: VehicleType) {
        prefs.edit().putString("vehicle_type", type.name).apply()
        _settings.value = _settings.value.copy(selectedVehicleType = type)
    }

    fun updateSensitivity(sensitivity: Sensitivity) {
        prefs.edit().putString("sensitivity", sensitivity.name).apply()
        _settings.value = _settings.value.copy(sensitivity = sensitivity)
    }

    fun updateDistance(meters: Int) {
        prefs.edit().putInt("distance_meters", meters).apply()
        _settings.value = _settings.value.copy(approximateDistanceMeters = meters)
    }

    fun updateFullDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("full_dark_mode", enabled).apply()
        _settings.value = _settings.value.copy(fullDarkModeEnabled = enabled)
    }

    fun updateSoundAlert(enabled: Boolean) {
        prefs.edit().putBoolean("sound_alert", enabled).apply()
        _settings.value = _settings.value.copy(soundAlertEnabled = enabled)
    }

    fun updateVibration(enabled: Boolean) {
        prefs.edit().putBoolean("vibration", enabled).apply()
        _settings.value = _settings.value.copy(vibrationEnabled = enabled)
    }

    fun setSafetyAccepted() {
        prefs.edit().putBoolean("accepted_safety", true).apply()
        _settings.value = _settings.value.copy(hasAcceptedSafetyDisclaimer = true)
    }
}
