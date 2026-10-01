package com.example.motion

import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class OpticalFlowResult(
    val motionMagnitude: Float,
    val forwardDisplacement: Float,
    val lateralDisplacement: Float,
    val scaleChange: Float,
    val luminanceShift: Float,
    val confidence: Float
)

/**
 * High-efficiency, allocation-free block matching and optical flow analyzer.
 * Tracks feature patches within the vehicle target ROI between consecutive frames.
 */
class OpticalFlowAnalyzer {

    // Pre-allocated buffers to prevent GC churn during tracking
    private var prevGridSamples = IntArray(64)
    private var currGridSamples = IntArray(64)
    private var hasPrevReference = false
    private var initialSpanX: Float = 0f
    private var initialSpanY: Float = 0f

    fun reset() {
        hasPrevReference = false
        initialSpanX = 0f
        initialSpanY = 0f
    }

    fun analyzeRoiMotion(
        yBuffer: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        roi: RectF
    ): OpticalFlowResult {
        val roiLeft = (roi.left * width).toInt().coerceIn(0, width - 4)
        val roiTop = (roi.top * height).toInt().coerceIn(0, height - 4)
        val roiRight = (roi.right * width).toInt().coerceIn(roiLeft + 4, width)
        val roiBottom = (roi.bottom * height).toInt().coerceIn(roiTop + 4, height)

        val roiW = roiRight - roiLeft
        val roiH = roiBottom - roiTop

        val gridCols = 6
        val gridRows = 6
        val totalPoints = gridCols * gridRows

        if (prevGridSamples.size < totalPoints) {
            prevGridSamples = IntArray(totalPoints)
            currGridSamples = IntArray(totalPoints)
        }

        var sampleIndex = 0
        var totalLum = 0L

        // Sample current grid
        for (r in 0 until gridRows) {
            val y = roiTop + (roiH * (r + 1) / (gridRows + 1))
            val rowOffset = y * rowStride
            for (c in 0 until gridCols) {
                val x = roiLeft + (roiW * (c + 1) / (gridCols + 1))
                val offset = rowOffset + x
                val lum = if (offset in yBuffer.indices) yBuffer[offset].toInt() and 0xFF else 128
                currGridSamples[sampleIndex++] = lum
                totalLum += lum
            }
        }

        val currAvgLum = totalLum.toFloat() / totalPoints

        if (!hasPrevReference) {
            System.arraycopy(currGridSamples, 0, prevGridSamples, 0, totalPoints)
            hasPrevReference = true
            initialSpanX = roiW.toFloat()
            initialSpanY = roiH.toFloat()
            return OpticalFlowResult(
                motionMagnitude = 0f,
                forwardDisplacement = 0f,
                lateralDisplacement = 0f,
                scaleChange = 0f,
                luminanceShift = 0f,
                confidence = 0.90f
            )
        }

        // Compare with previous reference grid
        var totalAbsDiff = 0
        var lateralDelta = 0
        var forwardDelta = 0
        var prevAvgLum = 0f

        for (i in 0 until totalPoints) {
            val diff = currGridSamples[i] - prevGridSamples[i]
            totalAbsDiff += abs(diff)
            prevAvgLum += prevGridSamples[i]

            val col = i % gridCols
            val row = i / gridCols

            // Gradient correlation across rows indicates forward/vertical shift
            if (row < gridRows - 1) {
                val nextRowDiff = currGridSamples[i + gridCols] - prevGridSamples[i]
                forwardDelta += (nextRowDiff - diff)
            }
            // Lateral correlation across columns
            if (col < gridCols - 1) {
                val nextColDiff = currGridSamples[i + 1] - prevGridSamples[i]
                lateralDelta += (nextColDiff - diff)
            }
        }
        prevAvgLum /= totalPoints

        val meanDiff = totalAbsDiff.toFloat() / totalPoints
        val motionMagnitude = (meanDiff / 64f).coerceIn(0f, 1.5f)

        val forwardDisp = (abs(forwardDelta).toFloat() / (totalPoints * 40f)).coerceIn(0f, 1.2f)
        val lateralDisp = (abs(lateralDelta).toFloat() / (totalPoints * 40f)).coerceIn(0f, 1.2f)

        // Measure scale change (shrinking indicates moving forward away from us)
        val currentSpanX = roiW.toFloat()
        val scaleChange = if (initialSpanX > 0f) {
            ((initialSpanX - currentSpanX) / initialSpanX).coerceIn(-0.5f, 0.8f)
        } else 0f

        val luminanceShift = abs(currAvgLum - prevAvgLum) / 255f

        // Slowly adapt reference grid to accommodate natural gradual daylight changes (exponential moving average)
        for (i in 0 until totalPoints) {
            prevGridSamples[i] = ((prevGridSamples[i] * 4 + currGridSamples[i]) / 5)
        }

        val confidence = (1f - (luminanceShift * 1.5f)).coerceIn(0.4f, 0.98f)

        return OpticalFlowResult(
            motionMagnitude = motionMagnitude,
            forwardDisplacement = forwardDisp,
            lateralDisplacement = lateralDisp,
            scaleChange = scaleChange,
            luminanceShift = luminanceShift,
            confidence = confidence
        )
    }
}
