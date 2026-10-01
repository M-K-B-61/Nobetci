package com.example.tracking

import android.graphics.RectF
import com.example.model.VehicleType

interface TrackingProfile {
    val vehicleType: VehicleType
    val defaultRoi: RectF
    val minMovementThreshold: Float
    val minVerificationFrames: Int
    val vibrationDampingFactor: Float
    val globalMotionWeight: Float
    val sensorMotionWeight: Float
    val scaleChangeWeight: Float
    val lateralMotionPenalty: Float
    val brakeLightThresholdTolerance: Float

    fun calculateNetMotion(
        targetMotion: Float,
        globalMotion: Float,
        sensorMotion: Float,
        scaleChange: Float,
        lateralMotion: Float,
        luminanceShift: Float
    ): Float
}
