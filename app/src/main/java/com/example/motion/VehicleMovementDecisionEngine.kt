package com.example.motion

import com.example.model.Sensitivity
import com.example.model.TrackingState
import com.example.tracking.TrackingProfile
import kotlin.math.max

data class DecisionResult(
    val nextState: TrackingState,
    val movementConfidence: Float,
    val verificationProgress: Float,
    val shouldTriggerAlarm: Boolean,
    val shouldRecalibrate: Boolean
)

/**
 * Multi-sensor vehicle movement decision engine.
 * Synthesizes optical flow, bounding box displacement, background ego-motion,
 * gyroscope, accelerometer, and profile parameters.
 * Guarantees phone disturbance / vehicle cabin vibration does NOT trigger false alarms.
 */
class VehicleMovementDecisionEngine {

    private var consecutiveVerifiedFrames = 0
    private var stabilizationFrames = 0

    fun reset() {
        consecutiveVerifiedFrames = 0
        stabilizationFrames = 0
    }

    fun evaluate(
        currentState: TrackingState,
        profile: TrackingProfile,
        sensitivity: Sensitivity,
        targetMotionScore: Float,
        globalMotionScore: Float,
        sensorMotionScore: Float,
        scaleChangeScore: Float,
        lateralMotionScore: Float,
        luminanceShiftScore: Float,
        trackingConfidence: Float,
        isPhoneDisturbed: Boolean
    ): DecisionResult {
        // 1. Phone motion / Cabin shake rejection:
        // If phone sensor is disturbed OR background is moving strongly together with target:
        val isWholeSceneMoving = globalMotionScore > 0.35f && (globalMotionScore >= targetMotionScore * 0.7f)
        val isCameraShaking = isPhoneDisturbed || isWholeSceneMoving || (sensorMotionScore > 0.38f)

        if (isCameraShaking) {
            consecutiveVerifiedFrames = 0
            stabilizationFrames = 5 // Stay in stabilizing state for 5 frames after vibration stops
            return DecisionResult(
                nextState = TrackingState.CAMERA_MOVED,
                movementConfidence = 0f,
                verificationProgress = 0f,
                shouldTriggerAlarm = false,
                shouldRecalibrate = true
            )
        }

        // 2. If recovering from camera shake:
        if (stabilizationFrames > 0) {
            stabilizationFrames--
            consecutiveVerifiedFrames = 0
            return DecisionResult(
                nextState = TrackingState.STABILIZING,
                movementConfidence = 0f,
                verificationProgress = 0f,
                shouldTriggerAlarm = false,
                shouldRecalibrate = stabilizationFrames == 0
            )
        }

        // 3. Target loss check:
        if (trackingConfidence < 0.35f) {
            consecutiveVerifiedFrames = 0
            return DecisionResult(
                nextState = TrackingState.TARGET_LOST,
                movementConfidence = 0f,
                verificationProgress = 0f,
                shouldTriggerAlarm = false,
                shouldRecalibrate = false
            )
        }

        // 4. Compute Net Motion using the active vehicle profile:
        val netMotion = profile.calculateNetMotion(
            targetMotion = targetMotionScore,
            globalMotion = globalMotionScore,
            sensorMotion = sensorMotionScore,
            scaleChange = scaleChangeScore,
            lateralMotion = lateralMotionScore,
            luminanceShift = luminanceShiftScore
        )

        // Adjust threshold based on user sensitivity setting
        val threshold = (profile.minMovementThreshold * (sensitivity.motionThreshold / 0.50f))
        val rawConfidence = if (netMotion >= threshold) {
            ((netMotion - threshold) / (threshold * 0.8f) + 0.60f).coerceIn(0.60f, 1.0f)
        } else {
            (netMotion / threshold * 0.50f).coerceIn(0f, 0.50f)
        }

        val finalMovementConfidence = rawConfidence * trackingConfidence
        val targetVerificationFrames = max(sensitivity.verificationFrames, profile.minVerificationFrames)

        // 5. State Machine progression:
        return when {
            finalMovementConfidence >= 0.65f -> {
                consecutiveVerifiedFrames++
                val progress = (consecutiveVerifiedFrames.toFloat() / targetVerificationFrames).coerceIn(0f, 1f)

                if (consecutiveVerifiedFrames >= targetVerificationFrames) {
                    DecisionResult(
                        nextState = TrackingState.MOVEMENT_CONFIRMED,
                        movementConfidence = finalMovementConfidence,
                        verificationProgress = 1f,
                        shouldTriggerAlarm = true,
                        shouldRecalibrate = false
                    )
                } else {
                    DecisionResult(
                        nextState = TrackingState.VERIFYING_MOVEMENT,
                        movementConfidence = finalMovementConfidence,
                        verificationProgress = progress,
                        shouldTriggerAlarm = false,
                        shouldRecalibrate = false
                    )
                }
            }
            finalMovementConfidence >= 0.35f -> {
                // Potential movement hint, ramp up analysis FPS
                consecutiveVerifiedFrames = max(0, consecutiveVerifiedFrames - 1)
                val progress = (consecutiveVerifiedFrames.toFloat() / targetVerificationFrames).coerceIn(0f, 1f)
                DecisionResult(
                    nextState = TrackingState.POSSIBLE_MOVEMENT,
                    movementConfidence = finalMovementConfidence,
                    verificationProgress = progress,
                    shouldTriggerAlarm = false,
                    shouldRecalibrate = false
                )
            }
            else -> {
                // Stable watch
                consecutiveVerifiedFrames = 0
                DecisionResult(
                    nextState = TrackingState.WATCHING,
                    movementConfidence = finalMovementConfidence,
                    verificationProgress = 0f,
                    shouldTriggerAlarm = false,
                    shouldRecalibrate = false
                )
            }
        }
    }
}
