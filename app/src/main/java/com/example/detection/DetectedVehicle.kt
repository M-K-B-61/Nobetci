package com.example.detection

import android.graphics.RectF
import com.example.model.VehicleType

data class DetectedVehicle(
    val boundingBox: RectF,
    val confidence: Float,
    val detectedType: VehicleType,
    val label: String
)
