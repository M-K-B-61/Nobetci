package com.example.motion

import android.graphics.RectF
import com.example.model.VehicleType
import kotlin.math.roundToInt

/**
 * Approximate Distance Estimator.
 * Estimates distance based on normalized vehicle ROI dimensions on the camera sensor.
 * Strictly presents estimates as approximate ("Yaklaşık X metre") per optical physics limitations.
 */
class ApproximateDistanceEstimator {

    fun estimateDistanceMeters(roi: RectF, vehicleType: VehicleType): Float {
        val roiHeightNorm = roi.height().coerceIn(0.10f, 0.95f)

        // Geometric pinhole model approximation
        // Heavy vehicles (trucks/buses) are taller (~3.5m - 4.0m)
        // Passenger cars are ~1.5m tall
        val realVehicleHeightMeters = when (vehicleType) {
            VehicleType.HEAVY_VEHICLE -> 3.6f
            VehicleType.AUTOMOBILE -> 1.5f
        }

        // Empirical sensor focal mapping for smartphone wide-angle lenses (~70-80 deg FoV)
        val estimatedDist = (realVehicleHeightMeters / (roiHeightNorm * 0.85f)).coerceIn(2f, 35f)
        return (estimatedDist * 10f).roundToInt() / 10f
    }

    fun formatDistanceLabel(meters: Float): String {
        val rounded = meters.roundToInt()
        return "Yaklaşık $rounded metre"
    }
}
