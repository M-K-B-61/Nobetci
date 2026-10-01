package com.example.tracking.automobile

import android.graphics.RectF
import com.example.model.VehicleType
import com.example.tracking.TrackingProfile
import kotlin.math.max

/**
 * Optimized profile for passenger cars and light commercial vehicles (Otomobil, SUV, Hafif Ticari).
 * Focuses on narrower center corridor, fast forward pull-away, strictly ignoring
 * passing traffic in adjacent left/right lanes.
 */
class AutomobileTrackingProfile : TrackingProfile {
    override val vehicleType: VehicleType = VehicleType.AUTOMOBILE

    // Focused lower-center ROI for cars in traffic queues
    override val defaultRoi: RectF = RectF(0.25f, 0.32f, 0.75f, 0.82f)

    // Standard automotive pull-away threshold
    override val minMovementThreshold: Float = 0.38f

    // Quicker verification since cars pull away more rapidly
    override val minVerificationFrames: Int = 4

    // Passenger vehicles have smoother cabin vibration
    override val vibrationDampingFactor: Float = 0.25f

    override val globalMotionWeight: Float = 1.15f
    override val sensorMotionWeight: Float = 1.30f
    override val scaleChangeWeight: Float = 1.40f

    // Strong penalty for lateral motion to reject adjacent lane cars speeding past
    override val lateralMotionPenalty: Float = 1.75f

    override val brakeLightThresholdTolerance: Float = 0.45f

    override fun calculateNetMotion(
        targetMotion: Float,
        globalMotion: Float,
        sensorMotion: Float,
        scaleChange: Float,
        lateralMotion: Float,
        luminanceShift: Float
    ): Float {
        val flarePenalty = if (luminanceShift > brakeLightThresholdTolerance && targetMotion < 0.3f) {
            luminanceShift * 0.4f
        } else {
            0f
        }

        // Heavy suppression if motion is purely sideways (adjacent lane overtake)
        val motionSuppression = (globalMotion * globalMotionWeight) +
                (sensorMotion * sensorMotionWeight) +
                (lateralMotion * lateralMotionPenalty) +
                flarePenalty

        val positiveScaleBonus = max(0f, scaleChange) * scaleChangeWeight
        val rawNet = (targetMotion + positiveScaleBonus) - motionSuppression

        return if (rawNet < vibrationDampingFactor * 0.4f) {
            0f
        } else {
            max(0f, rawNet)
        }
    }
}
