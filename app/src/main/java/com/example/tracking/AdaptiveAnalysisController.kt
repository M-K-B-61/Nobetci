package com.example.tracking

import com.example.model.AnalysisFpsMode
import com.example.model.TrackingState

/**
 * Dynamically adjusts frame analysis rates according to tracking state to preserve battery
 * and keep device thermal load low during long traffic/border waits.
 */
class AdaptiveAnalysisController {

    var currentMode: AnalysisFpsMode = AnalysisFpsMode.IDLE_WATCH
        private set

    private var lastAnalysisTimestamp: Long = 0L

    fun reset() {
        currentMode = AnalysisFpsMode.IDLE_WATCH
        lastAnalysisTimestamp = 0L
    }

    /**
     * Determines whether the incoming camera frame should be processed or skipped
     * based on current FPS mode interval.
     */
    fun shouldProcessFrame(currentTimeMs: Long): Boolean {
        val interval = currentMode.intervalMs
        if (currentTimeMs - lastAnalysisTimestamp >= interval) {
            lastAnalysisTimestamp = currentTimeMs
            return true
        }
        return false
    }

    /**
     * Adapts FPS mode based on tracking state.
     */
    fun updateState(trackingState: TrackingState, isDeviceHot: Boolean) {
        val targetMode = when (trackingState) {
            TrackingState.VERIFYING_MOVEMENT,
            TrackingState.MOVEMENT_CONFIRMED -> {
                if (isDeviceHot) AnalysisFpsMode.POSSIBLE_MOVEMENT else AnalysisFpsMode.VERIFYING_MOVEMENT
            }
            TrackingState.POSSIBLE_MOVEMENT -> {
                AnalysisFpsMode.POSSIBLE_MOVEMENT
            }
            TrackingState.INITIALIZING,
            TrackingState.SEARCHING_VEHICLE -> {
                AnalysisFpsMode.POSSIBLE_MOVEMENT
            }
            else -> {
                AnalysisFpsMode.IDLE_WATCH
            }
        }
        currentMode = targetMode
    }
}
