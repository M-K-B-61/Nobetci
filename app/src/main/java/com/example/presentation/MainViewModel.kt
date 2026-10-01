package com.example.presentation

import android.app.Application
import android.graphics.RectF
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmController
import com.example.battery.BatteryOptimizationManager
import com.example.camera.CameraManager
import com.example.data.SettingsRepository
import com.example.model.AccentColor
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.Sensitivity
import com.example.model.TrackingMetrics
import com.example.model.TrackingState
import com.example.model.VehicleType
import com.example.motion.ApproximateDistanceEstimator
import com.example.service.TrackingForegroundService
import com.example.thermal.ThermalManager
import com.example.tracking.HybridVehicleTrackingEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    HOME,
    TARGETING,
    SENTRY_ACTIVE,
    ALARM,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val settingsRepo = SettingsRepository(context)
    val trackingEngine = HybridVehicleTrackingEngine(context)
    val cameraManager = CameraManager(context)
    val alarmController = AlarmController(context)
    val batteryManager = BatteryOptimizationManager(context)
    val thermalManager = ThermalManager(context, ContextCompat.getMainExecutor(context))
    private val distanceEstimator = ApproximateDistanceEstimator()

    val settings: StateFlow<AppSettings> = settingsRepo.settings

    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _metrics = MutableStateFlow(TrackingMetrics())
    val metrics: StateFlow<TrackingMetrics> = _metrics.asStateFlow()

    private val _showDebug = MutableStateFlow(false)
    val showDebug: StateFlow<Boolean> = _showDebug.asStateFlow()

    private val _showSafetyDisclaimer = MutableStateFlow(false)
    val showSafetyDisclaimer: StateFlow<Boolean> = _showSafetyDisclaimer.asStateFlow()

    private var currentTargetRoi: RectF? = null

    init {
        batteryManager.start()
        thermalManager.start()
        cameraManager.initialize {}

        // Listen for foreground service actions (e.g. Stop or Test Alarm from ongoing notification)
        viewModelScope.launch {
            TrackingForegroundService.serviceActions.collect { action ->
                when (action) {
                    TrackingForegroundService.ACTION_STOP -> stopSentry()
                    TrackingForegroundService.ACTION_TEST_ALARM -> testAlarm()
                }
            }
        }

        // Combine telemetry for UI
        viewModelScope.launch {
            combine(
                trackingEngine.metrics,
                batteryManager.batteryState,
                thermalManager.thermalInfo
            ) { engineMetrics, battery, thermal ->
                engineMetrics.copy(
                    batteryPct = battery.percentage,
                    thermalStatusString = thermal.statusString
                )
            }.collect { combined ->
                _metrics.value = combined
                if (combined.trackingState == TrackingState.MOVEMENT_CONFIRMED ||
                    combined.trackingState == TrackingState.ALARMING
                ) {
                    triggerAlarm()
                }
            }
        }
    }

    fun onSplashFinished() {
        if (!settings.value.isOnboardingCompleted) {
            _currentScreen.value = AppScreen.ONBOARDING
        } else {
            _currentScreen.value = AppScreen.HOME
        }
    }

    fun finishOnboarding() {
        settingsRepo.setOnboardingCompleted(true)
        _currentScreen.value = AppScreen.HOME
    }

    fun replayOnboarding() {
        _currentScreen.value = AppScreen.ONBOARDING
    }

    fun onStartSentryFlow() {
        if (!settings.value.hasAcceptedSafetyDisclaimer) {
            _showSafetyDisclaimer.value = true
        } else {
            _currentScreen.value = AppScreen.TARGETING
        }
    }

    fun onAcceptSafetyDisclaimer() {
        settingsRepo.setSafetyAccepted()
        _showSafetyDisclaimer.value = false
        _currentScreen.value = AppScreen.TARGETING
    }

    fun onDismissSafetyDisclaimer() {
        _showSafetyDisclaimer.value = false
    }

    fun startTargetingCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val type = settings.value.selectedVehicleType
        val sens = settings.value.sensitivity
        trackingEngine.startTracking(type, sens)

        cameraManager.startTargetingPreview(lifecycleOwner, previewView) { yBuffer, width, height, rowStride ->
            if (trackingEngine.adaptiveController.shouldProcessFrame(System.currentTimeMillis())) {
                val updated = trackingEngine.processFrame(yBuffer, width, height, rowStride, isDeviceHot = false)
                currentTargetRoi = updated.targetRoi
            }
        }
    }

    fun onManualTargetSelected(roi: RectF) {
        currentTargetRoi = roi
        trackingEngine.manualSetTarget(roi)
    }

    fun confirmTargetAndStartSentry(lifecycleOwner: LifecycleOwner) {
        _currentScreen.value = AppScreen.SENTRY_ACTIVE
        TrackingForegroundService.startService(context)

        // Close preview completely! Only background image analysis at 640x480
        val isHot = thermalManager.thermalInfo.value.isElevated
        cameraManager.startSentryTracking(lifecycleOwner) { yBuffer, width, height, rowStride ->
            val now = System.currentTimeMillis()
            if (trackingEngine.adaptiveController.shouldProcessFrame(now)) {
                trackingEngine.processFrame(yBuffer, width, height, rowStride, isDeviceHot = isHot)
            }
        }
    }

    fun stopSentry() {
        trackingEngine.stopTracking()
        cameraManager.stop()
        alarmController.stopAlarm()
        TrackingForegroundService.stopService(context)
        _currentScreen.value = AppScreen.HOME
    }

    private fun triggerAlarm() {
        if (_currentScreen.value != AppScreen.ALARM) {
            _currentScreen.value = AppScreen.ALARM
            val sound = settings.value.soundAlertEnabled
            val vib = settings.value.vibrationEnabled
            alarmController.triggerAlarm(soundEnabled = sound, vibrationEnabled = vib)
        }
    }

    fun dismissAlarm() {
        alarmController.stopAlarm()
        stopSentry()
    }

    fun testAlarm() {
        triggerAlarm()
    }

    fun openSettings() {
        _currentScreen.value = AppScreen.SETTINGS
    }

    fun closeSettings() {
        _currentScreen.value = AppScreen.HOME
    }

    fun cancelTargeting() {
        cameraManager.stop()
        trackingEngine.stopTracking()
        _currentScreen.value = AppScreen.HOME
    }

    fun toggleDebug() {
        _showDebug.value = !_showDebug.value
    }

    fun updateThemeMode(mode: AppThemeMode) {
        settingsRepo.updateThemeMode(mode)
    }

    fun updateAccentColor(accent: AccentColor) {
        settingsRepo.updateAccentColor(accent)
    }

    fun selectVehicleType(type: VehicleType) {
        settingsRepo.updateVehicleType(type)
    }

    fun updateSensitivity(sensitivity: Sensitivity) {
        settingsRepo.updateSensitivity(sensitivity)
    }

    fun updateDistance(meters: Int) {
        settingsRepo.updateDistance(meters)
    }

    fun toggleFullDark(enabled: Boolean) {
        settingsRepo.updateFullDarkMode(enabled)
    }

    fun toggleSound(enabled: Boolean) {
        settingsRepo.updateSoundAlert(enabled)
    }

    fun toggleVibration(enabled: Boolean) {
        settingsRepo.updateVibration(enabled)
    }

    fun getFormattedDistanceLabel(): String {
        val dist = _metrics.value.estimatedDistanceMeters
        return distanceEstimator.formatDistanceLabel(dist)
    }

    override fun onCleared() {
        super.onCleared()
        batteryManager.stop()
        thermalManager.stop()
        cameraManager.release()
        alarmController.stopAlarm()
        trackingEngine.stopTracking()
    }
}
