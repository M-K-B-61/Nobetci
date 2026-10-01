package com.example.detection

import android.graphics.RectF
import com.example.model.VehicleType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * On-device lightweight vehicle detector.
 * Identifies the vehicle directly ahead in the center camera corridor by analyzing
 * luminance edge profiles, horizontal symmetry of lights/bumpers, and geometric proportions.
 * Runs during initial lock and when re-acquisition is required.
 */
class VehicleDetector {

    fun detectVehicle(
        yBuffer: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        vehicleType: VehicleType,
        userSelectedRoi: RectF? = null
    ): DetectedVehicle? {
        if (width <= 0 || height <= 0 || yBuffer.isEmpty()) return null

        // If user manually tapped/pinned an ROI on screen, refine around it
        if (userSelectedRoi != null) {
            val clampedRoi = RectF(
                userSelectedRoi.left.coerceIn(0.05f, 0.95f),
                userSelectedRoi.top.coerceIn(0.05f, 0.95f),
                userSelectedRoi.right.coerceIn(0.05f, 0.95f),
                userSelectedRoi.bottom.coerceIn(0.05f, 0.95f)
            )
            val label = when (vehicleType) {
                VehicleType.HEAVY_VEHICLE -> "Tır / Kamyon Kilitlendi ✓"
                VehicleType.AUTOMOBILE -> "Otomobil Kilitlendi ✓"
            }
            return DetectedVehicle(
                boundingBox = clampedRoi,
                confidence = 0.95f,
                detectedType = vehicleType,
                label = label
            )
        }

        // Subsample search grid for fast detection without GC allocation
        val stepX = max(4, width / 80)
        val stepY = max(4, height / 80)

        // Center corridor constraints: we prioritize the vehicle in our direct lane
        // Trucks occupy a higher and broader area; cars occupy lower-center area
        val (targetMinX, targetMaxX, targetMinY, targetMaxY) = when (vehicleType) {
            VehicleType.HEAVY_VEHICLE -> floatArrayOf(0.12f, 0.88f, 0.15f, 0.90f)
            VehicleType.AUTOMOBILE -> floatArrayOf(0.20f, 0.80f, 0.28f, 0.88f)
        }

        val startX = (width * targetMinX).toInt()
        val endX = (width * targetMaxX).toInt()
        val startY = (height * targetMinY).toInt()
        val endY = (height * targetMaxY).toInt()

        var bestScore = 0f
        var bestLeft = targetMinX
        var bestTop = targetMinY
        var bestRight = targetMaxX
        var bestBottom = targetMaxY

        // Evaluate candidate vehicle bounding boxes in center corridor
        val candidateWidths = when (vehicleType) {
            VehicleType.HEAVY_VEHICLE -> floatArrayOf(0.70f, 0.60f, 0.50f)
            VehicleType.AUTOMOBILE -> floatArrayOf(0.52f, 0.42f, 0.34f)
        }

        val candidateHeights = when (vehicleType) {
            VehicleType.HEAVY_VEHICLE -> floatArrayOf(0.68f, 0.55f, 0.45f)
            VehicleType.AUTOMOBILE -> floatArrayOf(0.40f, 0.32f, 0.25f)
        }

        for (cw in candidateWidths) {
            for (ch in candidateHeights) {
                val boxW = (width * cw).toInt()
                val boxH = (height * ch).toInt()
                val boxX = (width - boxW) / 2 // centered horizontally
                // Trucks have higher vertical center, cars slightly lower
                val offsetYFrac = if (vehicleType == VehicleType.HEAVY_VEHICLE) 0.45f else 0.56f
                val boxY = ((height * offsetYFrac) - (boxH / 2)).toInt().coerceIn(startY, height - boxH - 2)

                // Measure edge energy and horizontal symmetry across the box
                val score = evaluateCandidateBox(
                    yBuffer = yBuffer,
                    rowStride = rowStride,
                    boxX = boxX,
                    boxY = boxY,
                    boxW = boxW,
                    boxH = boxH,
                    stepX = stepX,
                    stepY = stepY
                )

                if (score > bestScore) {
                    bestScore = score
                    bestLeft = boxX.toFloat() / width
                    bestTop = boxY.toFloat() / height
                    bestRight = (boxX + boxW).toFloat() / width
                    bestBottom = (boxY + boxH).toFloat() / height
                }
            }
        }

        val finalConfidence = (0.70f + (bestScore * 0.28f)).coerceIn(0.65f, 0.98f)
        val label = when (vehicleType) {
            VehicleType.HEAVY_VEHICLE -> "Tır / Kamyon algılandı ✓"
            VehicleType.AUTOMOBILE -> "Otomobil algılandı ✓"
        }

        return DetectedVehicle(
            boundingBox = RectF(bestLeft, bestTop, bestRight, bestBottom),
            confidence = finalConfidence,
            detectedType = vehicleType,
            label = label
        )
    }

    private fun evaluateCandidateBox(
        yBuffer: ByteArray,
        rowStride: Int,
        boxX: Int,
        boxY: Int,
        boxW: Int,
        boxH: Int,
        stepX: Int,
        stepY: Int
    ): Float {
        var edgeSum = 0L
        var symmetryDiff = 0L
        var sampleCount = 0

        val midX = boxX + (boxW / 2)

        var y = boxY + stepY
        while (y < boxY + boxH - stepY) {
            val rowOffset = y * rowStride
            val rowOffsetPrev = (y - stepY) * rowStride

            var x = boxX + stepX
            while (x < boxX + (boxW / 2)) {
                val idx1 = rowOffset + x
                val idx2 = rowOffset + x + stepX
                val idx3 = rowOffsetPrev + x

                val leftVal = if (idx1 in yBuffer.indices) yBuffer[idx1].toInt() and 0xFF else 128
                val leftRightVal = if (idx2 in yBuffer.indices) yBuffer[idx2].toInt() and 0xFF else 128
                val leftTopVal = if (idx3 in yBuffer.indices) yBuffer[idx3].toInt() and 0xFF else 128

                // Horizontal and vertical Sobel-like edge
                val gx = abs(leftRightVal - leftVal)
                val gy = abs(leftTopVal - leftVal)
                edgeSum += (gx + gy)

                // Symmetric point on the right half of the vehicle
                val mirrorX = midX + (midX - x)
                if (mirrorX in boxX until (boxX + boxW)) {
                    val mirrorIdx = rowOffset + mirrorX
                    val rightVal = if (mirrorIdx in yBuffer.indices) yBuffer[mirrorIdx].toInt() and 0xFF else 128
                    symmetryDiff += abs(leftVal - rightVal)
                }

                sampleCount++
                x += stepX
            }
            y += stepY
        }

        if (sampleCount == 0) return 0f

        val avgEdge = edgeSum.toFloat() / sampleCount
        val avgSymmetryDiff = symmetryDiff.toFloat() / sampleCount
        // Higher edge density + lower symmetry diff = higher vehicle rear structure score
        val symmetryScore = (1f - (avgSymmetryDiff / 255f)).coerceIn(0f, 1f)
        val edgeScore = (avgEdge / 50f).coerceIn(0f, 1f)

        return (edgeScore * 0.6f) + (symmetryScore * 0.4f)
    }
}
