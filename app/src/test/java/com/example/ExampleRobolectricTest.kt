package com.example

import android.content.Context
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import com.example.data.SettingsRepository
import com.example.model.AppThemeMode
import com.example.model.Sensitivity
import com.example.model.TrackingState
import com.example.model.VehicleType
import com.example.motion.ApproximateDistanceEstimator
import com.example.motion.VehicleMovementDecisionEngine
import com.example.tracking.automobile.AutomobileTrackingProfile
import com.example.tracking.heavyvehicle.HeavyVehicleTrackingProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies Nobetci name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nöbetçi", appName)
    }

    @Test
    fun `test theme switching persistence in SettingsRepository`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = SettingsRepository(context)

        repo.updateThemeMode(AppThemeMode.LIGHT)
        assertEquals(AppThemeMode.LIGHT, repo.settings.value.themeMode)

        repo.updateThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, repo.settings.value.themeMode)

        repo.updateThemeMode(AppThemeMode.SYSTEM)
        assertEquals(AppThemeMode.SYSTEM, repo.settings.value.themeMode)
    }

    @Test
    fun `test heavy vehicle profile parameters`() {
        val profile = HeavyVehicleTrackingProfile()
        assertEquals(VehicleType.HEAVY_VEHICLE, profile.vehicleType)
        assertTrue(profile.minVerificationFrames >= 6)
        assertTrue(profile.vibrationDampingFactor > 0.3f)

        // Net motion with flare/brake light should be dampened
        val motion = profile.calculateNetMotion(
            targetMotion = 0.25f,
            globalMotion = 0.05f,
            sensorMotion = 0.02f,
            scaleChange = 0.0f,
            lateralMotion = 0.0f,
            luminanceShift = 0.70f
        )
        // Damping and flare penalty reduce false motion
        assertTrue(motion < 0.20f)
    }

    @Test
    fun `test automobile profile parameters`() {
        val profile = AutomobileTrackingProfile()
        assertEquals(VehicleType.AUTOMOBILE, profile.vehicleType)
        assertTrue(profile.lateralMotionPenalty > profile.globalMotionWeight)

        // Lateral motion (adjacent lane) should be strongly penalized
        val netWithLateral = profile.calculateNetMotion(
            targetMotion = 0.60f,
            globalMotion = 0.0f,
            sensorMotion = 0.0f,
            scaleChange = 0.0f,
            lateralMotion = 0.40f,
            luminanceShift = 0.0f
        )
        assertTrue("Adjacent lane lateral motion must be heavily penalized", netWithLateral < 0.30f)
    }

    @Test
    fun `test camera shake suppresses movement decision`() {
        val decisionEngine = VehicleMovementDecisionEngine()
        val profile = HeavyVehicleTrackingProfile()

        // When phone is disturbed or whole scene is moving (ego-motion)
        val decision = decisionEngine.evaluate(
            currentState = TrackingState.WATCHING,
            profile = profile,
            sensitivity = Sensitivity.NORMAL,
            targetMotionScore = 0.8f,
            globalMotionScore = 0.75f,
            sensorMotionScore = 0.6f,
            scaleChangeScore = 0.1f,
            lateralMotionScore = 0f,
            luminanceShiftScore = 0f,
            trackingConfidence = 0.9f,
            isPhoneDisturbed = true
        )

        assertFalse("Camera shake must NEVER trigger an alarm", decision.shouldTriggerAlarm)
        assertEquals(TrackingState.CAMERA_MOVED, decision.nextState)
    }

    @Test
    fun `test sustained target forward movement triggers confirmation`() {
        val decisionEngine = VehicleMovementDecisionEngine()
        val profile = AutomobileTrackingProfile()

        var lastDecision = decisionEngine.evaluate(
            currentState = TrackingState.WATCHING,
            profile = profile,
            sensitivity = Sensitivity.MINIMAL_MOVEMENT,
            targetMotionScore = 0.85f,
            globalMotionScore = 0.02f,
            sensorMotionScore = 0.01f,
            scaleChangeScore = 0.25f,
            lateralMotionScore = 0.0f,
            luminanceShiftScore = 0.05f,
            trackingConfidence = 0.95f,
            isPhoneDisturbed = false
        )

        // Feed subsequent verified forward movement frames
        for (i in 1..profile.minVerificationFrames + 2) {
            lastDecision = decisionEngine.evaluate(
                currentState = lastDecision.nextState,
                profile = profile,
                sensitivity = Sensitivity.MINIMAL_MOVEMENT,
                targetMotionScore = 0.85f,
                globalMotionScore = 0.02f,
                sensorMotionScore = 0.01f,
                scaleChangeScore = 0.25f,
                lateralMotionScore = 0.0f,
                luminanceShiftScore = 0.05f,
                trackingConfidence = 0.95f,
                isPhoneDisturbed = false
            )
        }

        assertTrue("Sustained real forward movement must trigger alarm", lastDecision.shouldTriggerAlarm)
        assertEquals(TrackingState.MOVEMENT_CONFIRMED, lastDecision.nextState)
    }

    @Test
    fun `test approximate distance estimation`() {
        val estimator = ApproximateDistanceEstimator()
        val carRoi = RectF(0.2f, 0.3f, 0.8f, 0.7f) // height = 0.4
        val distCar = estimator.estimateDistanceMeters(carRoi, VehicleType.AUTOMOBILE)
        assertTrue(distCar in 2f..20f)

        val label = estimator.formatDistanceLabel(distCar)
        assertTrue(label.startsWith("Yaklaşık"))
    }
}
