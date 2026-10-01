package com.example.motion

import android.graphics.RectF
import kotlin.math.abs

/**
 * Global Motion Estimator (Ego-motion / Phone Vibration filter).
 * Samples areas outside the vehicle ROI (distant horizon, roadside periphery)
 * to distinguish whole-scene camera displacement from target vehicle movement.
 */
class GlobalMotionEstimator {

    private val prevBackgroundSamples = IntArray(16)
    private val currBackgroundSamples = IntArray(16)
    private var hasPrevBackground = false

    fun reset() {
        hasPrevBackground = false
    }

    fun estimateGlobalMotion(
        yBuffer: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        vehicleRoi: RectF
    ): Float {
        if (width <= 0 || height <= 0 || yBuffer.isEmpty()) return 0f

        // Sample 4 background points: Top Left, Top Right, Far Left, Far Right
        var idx = 0

        // 1. Top left (periphery)
        samplePatch(yBuffer, (width * 0.08f).toInt(), (height * 0.10f).toInt(), rowStride, currBackgroundSamples, idx)
        idx += 4

        // 2. Top right (periphery)
        samplePatch(yBuffer, (width * 0.92f).toInt(), (height * 0.10f).toInt(), rowStride, currBackgroundSamples, idx)
        idx += 4

        // 3. Far mid-left (roadside)
        val leftX = (width * 0.05f).toInt()
        val midY = (height * 0.50f).toInt()
        samplePatch(yBuffer, leftX, midY, rowStride, currBackgroundSamples, idx)
        idx += 4

        // 4. Far mid-right (roadside)
        val rightX = (width * 0.95f).toInt()
        samplePatch(yBuffer, rightX, midY, rowStride, currBackgroundSamples, idx)

        if (!hasPrevBackground) {
            System.arraycopy(currBackgroundSamples, 0, prevBackgroundSamples, 0, 16)
            hasPrevBackground = true
            return 0f
        }

        var totalDiff = 0
        for (i in 0 until 16) {
            totalDiff += abs(currBackgroundSamples[i] - prevBackgroundSamples[i])
            // Adaptive reference update
            prevBackgroundSamples[i] = (prevBackgroundSamples[i] * 3 + currBackgroundSamples[i]) / 4
        }

        val avgBgDiff = totalDiff.toFloat() / 16f
        // Normalize 0..1+
        return (avgBgDiff / 45f).coerceIn(0f, 1.5f)
    }

    private fun samplePatch(
        yBuffer: ByteArray,
        centerX: Int,
        centerY: Int,
        rowStride: Int,
        targetArray: IntArray,
        startOffset: Int
    ) {
        val maxRows = if (rowStride > 0) yBuffer.size / rowStride else 0
        if (maxRows <= 4 || rowStride <= 4) return

        val cx = centerX.coerceIn(2, rowStride - 3)
        val cy = centerY.coerceIn(2, maxRows - 3)

        targetArray[startOffset] = safeGet(yBuffer, (cy - 1) * rowStride + (cx - 1))
        targetArray[startOffset + 1] = safeGet(yBuffer, (cy - 1) * rowStride + (cx + 1))
        targetArray[startOffset + 2] = safeGet(yBuffer, (cy + 1) * rowStride + (cx - 1))
        targetArray[startOffset + 3] = safeGet(yBuffer, (cy + 1) * rowStride + (cx + 1))
    }

    private fun safeGet(yBuffer: ByteArray, offset: Int): Int {
        return if (offset in yBuffer.indices) yBuffer[offset].toInt() and 0xFF else 128
    }
}
