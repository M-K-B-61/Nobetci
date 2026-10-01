package com.example.tracking.heavyvehicle

import android.graphics.RectF
import com.example.model.VehicleType
import com.example.tracking.TrackingProfile
import kotlin.math.abs
import kotlin.math.max

/**
 * Optimized profile for heavy commercial vehicles (Tır, Kamyon, Çekici, Otobüs).
 * Accounts for large frame coverage, slow creeping queue speed, engine rumble,
 * suspension oscillations, and sudden brake lights.
 */
class HeavyVehicleTrackingProfile : TrackingProfile {
    override val vehicleType: VehicleType = VehicleType.HEAVY_VEHICLE

    // Covers 70% width and 70% height centered
    override val defaultRoi: RectF = RectF(0.15f, 0.15f, 0.85f, 0.85f)

    // Higher sensitivity to subtle forward creeping
    override val minMovementThreshold: Float = 0.28f

    // Longer verification required to distinguish forward creeping from trailer rocking or driver shifts
    override val minVerificationFrames: Int = 8

    // Heavy cabin rumble / engine idle damping
    override val vibrationDampingFactor: Float = 0.45f

    override val globalMotionWeight: Float = 1.35f
    override val sensorMotionWeight: Float = 1.50f
    override val scaleChangeWeight: Float = 1.25f

    // Heavy vehicles don't change lanes instantly, but trailer sways; moderate penalty
    override val lateralMotionPenalty: Float = 0.80f

    // Highly tolerant to brake light intensity jumps without triggering false movement
    override val brakeLightThresholdTolerance: Float = 0.60f

    override fun calculateNetMotion(
        targetMotion: Float,
        globalMotion: Float,
        sensorMotion: Float,
        scaleChange: Float,
        lateralMotion: Float,
        luminanceShift: Float
    ): Float {
        // Sudden pure luminance spike (brake light / headlights) without physical displacement is filtered
        val flarePenalty = if (luminanceShift > brakeLightThresholdTolerance && targetMotion < 0.4f) {
            luminanceShift * 0.5f
        } else {
            0f
        }

        // Subtract global camera shift and phone accelerometer/gyro tremors
        val motionSuppression = (globalMotion * globalMotionWeight) +
                (sensorMotion * sensorMotionWeight) +
                (lateralMotion * lateralMotionPenalty) +
                flarePenalty

        // Scale change (vehicle moving forward away from camera shrinks in perspective)
        val positiveScaleBonus = max(0f, scaleChange) * scaleChangeWeight

        val rawNet = (targetMotion + positiveScaleBonus) - motionSuppression

        // Apply vibration deadband damping
        return if (rawNet < vibrationDampingFactor * 0.5f) {
            0f
        } else {
            max(0f, rawNet)
        }
    }
}
