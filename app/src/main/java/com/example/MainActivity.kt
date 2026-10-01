package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.model.AppThemeMode
import com.example.presentation.AppScreen
import com.example.presentation.MainViewModel
import com.example.presentation.screens.AlarmScreen
import com.example.presentation.screens.HomeScreen
import com.example.presentation.screens.SafetyDialog
import com.example.presentation.screens.SentryActiveScreen
import com.example.presentation.screens.SettingsScreen
import com.example.presentation.screens.SplashScreen
import com.example.presentation.screens.TargetingScreen
import com.example.presentation.screens.WelcomeOnboardingScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onStartSentryFlow()
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on during sentry operations
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Wake screen when locked for alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            MyApplicationTheme(
                themeMode = settings.themeMode,
                accentColor = settings.accentColor
            ) {
                NobetciApp(
                    viewModel = viewModel,
                    onRequestCameraPermission = { checkAndRequestCamera() },
                    onAdjustBrightness = { brightness -> setScreenBrightness(brightness) }
                )
            }
        }
    }

    private fun checkAndRequestCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onStartSentryFlow()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setScreenBrightness(brightness: Float) {
        val layout = window.attributes
        layout.screenBrightness = brightness
        window.attributes = layout
    }
}

@Composable
fun NobetciApp(
    viewModel: MainViewModel,
    onRequestCameraPermission: () -> Unit,
    onAdjustBrightness: (Float) -> Unit
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val showDebug by viewModel.showDebug.collectAsState()
    val showSafetyDialog by viewModel.showSafetyDisclaimer.collectAsState()

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    // Dynamic screen brightness optimization
    LaunchedEffect(currentScreen) {
        when (currentScreen) {
            AppScreen.SENTRY_ACTIVE -> {
                // Minimum usable brightness to protect battery
                onAdjustBrightness(0.01f)
            }
            AppScreen.ALARM -> {
                // Wake user up with high brightness
                onAdjustBrightness(1.0f)
            }
            else -> {
                // Normal system brightness
                onAdjustBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (currentScreen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onSplashFinished = { viewModel.onSplashFinished() }
                )
            }
            AppScreen.ONBOARDING -> {
                WelcomeOnboardingScreen(
                    onFinishOnboarding = { viewModel.finishOnboarding() }
                )
            }
            AppScreen.HOME -> {
                HomeScreen(
                    settings = settings,
                    metrics = metrics,
                    onSelectVehicleType = { viewModel.selectVehicleType(it) },
                    onStartTargeting = onRequestCameraPermission,
                    onOpenSettings = { viewModel.openSettings() },
                    showDebugPanel = showDebug,
                    onToggleDebug = { viewModel.toggleDebug() },
                    onToggleTheme = {
                        val nextTheme = when (settings.themeMode) {
                            AppThemeMode.DARK -> AppThemeMode.LIGHT
                            AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
                            AppThemeMode.SYSTEM -> AppThemeMode.DARK
                        }
                        viewModel.updateThemeMode(nextTheme)
                    }
                )
            }
            AppScreen.TARGETING -> {
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    viewModel.startTargetingCamera(lifecycleOwner, previewView)
                    onDispose {
                        if (viewModel.currentScreen.value != AppScreen.SENTRY_ACTIVE) {
                            viewModel.cameraManager.stop()
                        }
                    }
                }

                TargetingScreen(
                    vehicleType = settings.selectedVehicleType,
                    metrics = metrics,
                    previewView = previewView,
                    onManualTargetSelect = { roi -> viewModel.onManualTargetSelected(roi) },
                    onConfirmTarget = { viewModel.confirmTargetAndStartSentry(lifecycleOwner) },
                    onCancel = { viewModel.cancelTargeting() }
                )
            }
            AppScreen.SENTRY_ACTIVE -> {
                SentryActiveScreen(
                    vehicleType = settings.selectedVehicleType,
                    metrics = metrics,
                    distanceLabel = viewModel.getFormattedDistanceLabel(),
                    onStopSentry = { viewModel.stopSentry() },
                    showDebugPanel = showDebug,
                    onToggleDebugPanel = { viewModel.toggleDebug() }
                )
            }
            AppScreen.ALARM -> {
                AlarmScreen(
                    onDismissAlarm = { viewModel.dismissAlarm() }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    settings = settings,
                    onBack = { viewModel.closeSettings() },
                    onUpdateTheme = { viewModel.updateThemeMode(it) },
                    onUpdateAccentColor = { viewModel.updateAccentColor(it) },
                    onUpdateSensitivity = { viewModel.updateSensitivity(it) },
                    onUpdateDistance = { viewModel.updateDistance(it) },
                    onToggleFullDark = { viewModel.toggleFullDark(it) },
                    onToggleSound = { viewModel.toggleSound(it) },
                    onToggleVibration = { viewModel.toggleVibration(it) },
                    onTestAlarm = { viewModel.testAlarm() },
                    onReplayOnboarding = { viewModel.replayOnboarding() }
                )
            }
        }

        if (showSafetyDialog) {
            SafetyDialog(
                onDismiss = { viewModel.onAcceptSafetyDisclaimer() }
            )
        }
    }
}
