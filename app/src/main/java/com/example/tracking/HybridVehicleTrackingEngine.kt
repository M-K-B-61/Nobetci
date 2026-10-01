package com.example.tracking

import android.content.Context
import android.graphics.RectF
import com.example.detection.DetectedVehicle
import com.example.detection.VehicleDetector
import com.example.model.AnalysisFpsMode
import com.example.model.Sensitivity
import com.example.model.TrackingMetrics
import com.example.model.TrackingState
import com.example.model.VehicleType
import com.example.motion.ApproximateDistanceEstimator
import com.example.motion.GlobalMotionEstimator
import com.example.motion.OpticalFlowAnalyzer
import com.example.motion.VehicleMovementDecisionEngine
import com.example.sensors.SensorMotionDetector
import com.example.tracking.automobile.AutomobileTrackingProfile
import com.example.tracking.heavyvehicle.HeavyVehicleTrackingProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * Hybrid Vehicle Tracking Engine.
 * Integrates lightweight computer vision, optical flow, global motion estimation,
 * hardware gyro/accelerometer sensor fusion, and adaptive FPS regulation.
 */
class HybridVehicleTrackingEngine(private val context: Context) {

    private val detector = VehicleDetector()
    private val opticalFlow = OpticalFlowAnalyzer()
    private val globalMotionEstimator = GlobalMotionEstimator()
    private val sensorDetector = SensorMotionDetector(context)
    private val decisionEngine = VehicleMovementDecisionEngine()
    private val distanceEstimator = ApproximateDistanceEstimator()
    val adaptiveController = AdaptiveAnalysisController()

    private var activeProfile: TrackingProfile = HeavyVehicleTrackingProfile()
    private var activeSensitivity: Sensitivity = Sensitivity.NORMAL

    private var targetRoi: RectF? = null
    private var trackingConfidence: Float = 0f
    private var currentState: TrackingState = TrackingState.STOPPED

    private var frameCount = 0
    private var lastFpsTimestamp = System.currentTimeMillis()
    private var measuredFps = 0f

    private val _metrics = MutableStateFlow(TrackingMetrics())
    val metrics: StateFlow<TrackingMetrics> = _metrics.asStateFlow()

    @Volatile
    var isRunning: Boolean = false
        private set

    fun startTracking(
        vehicleType: VehicleType,
        sensitivity: Sensitivity,
        userTargetRoi: RectF? = null
    ) {
        isRunning = true
        activeProfile = when (vehicleType) {
            VehicleType.HEAVY_VEHICLE -> HeavyVehicleTrackingProfile()
            VehicleType.AUTOMOBILE -> AutomobileTrackingProfile()
        }
        activeSensitivity = sensitivity
        targetRoi = userTargetRoi ?: activeProfile.defaultRoi
        trackingConfidence = 0.85f
        currentState = TrackingState.SEARCHING_VEHICLE

        opticalFlow.reset()
        globalMotionEstimator.reset()
        decisionEngine.reset()
        adaptiveController.reset()
        sensorDetector.startListening()

        _metrics.value = TrackingMetrics(
            trackingState = currentState,
            vehicleType = vehicleType,
            targetRoi = targetRoi,
            fpsMode = AnalysisFpsMode.IDLE_WATCH
        )
    }

    fun stopTracking() {
        isRunning = false
        currentState = TrackingState.STOPPED
        sensorDetector.stopListening()
        opticalFlow.reset()
        globalMotionEstimator.reset()
        decisionEngine.reset()
        adaptiveController.reset()

        _metrics.value = _metrics.value.copy(
            trackingState = TrackingState.STOPPED,
            fpsMode = AnalysisFpsMode.IDLE_WATCH
        )
    }

    fun manualSetTarget(roi: RectF) {
        targetRoi = roi
        trackingConfidence = 0.95f
        opticalFlow.reset()
        globalMotionEstimator.reset()
        decisionEngine.reset()
        currentState = TrackingState.VEHICLE_LOCKED
    }

    /**
     * Main analysis method called per CameraX frame.
     * Operates purely on luminance byte array without allocating Bitmaps.
     */
    fun processFrame(
        yBuffer: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        isDeviceHot: Boolean = false
    ): TrackingMetrics {
        if (!isRunning) return _metrics.value

        val startTime = System.currentTimeMillis()

        // FPS measurement
        frameCount++
        if (startTime - lastFpsTimestamp >= 1000L) {
            measuredFps = frameCount.toFloat()
            frameCount = 0
            lastFpsTimestamp = startTime
        }

        // 1. Initial lock or target recovery
        if (targetRoi == null || currentState == TrackingState.SEARCHING_VEHICLE || currentState == TrackingState.TARGET_LOST) {
            val detected = detector.detectVehicle(
                yBuffer = yBuffer,
                width = width,
                height = height,
                rowStride = rowStride,
                vehicleType = activeProfile.vehicleType,
                userSelectedRoi = targetRoi
            )

            if (detected != null) {
                targetRoi = detected.boundingBox
                trackingConfidence = detected.confidence
                currentState = TrackingState.VEHICLE_LOCKED
                opticalFlow.reset()
                globalMotionEstimator.reset()
            } else {
                trackingConfidence = 0.2f
                currentState = TrackingState.TARGET_LOST
            }
        }

        val currentRoi = targetRoi ?: activeProfile.defaultRoi

        // 2. Optical Flow & Feature motion inside ROI
        val flowResult = opticalFlow.analyzeRoiMotion(
            yBuffer = yBuffer,
            width = width,
            height = height,
            rowStride = rowStride,
            roi = currentRoi
        )

        // 3. Background Ego-Motion Estimation
        val globalMotion = globalMotionEstimator.estimateGlobalMotion(
            yBuffer = yBuffer,
            width = width,
            height = height,
            rowStride = rowStride,
            vehicleRoi = currentRoi
        )

        // 4. Hardware Sensors (Gyro + Accel)
        val sensorMotion = sensorDetector.sensorMotionScore
        val isPhoneDisturbed = sensorDetector.isPhoneDisturbed

        // Update tracking confidence smoothly
        trackingConfidence = (trackingConfidence * 0.8f + flowResult.confidence * 0.2f).coerceIn(0.2f, 0.99f)

        // 5. Multi-sensor Movement Decision
        val decision = decisionEngine.evaluate(
            currentState = currentState,
            profile = activeProfile,
            sensitivity = activeSensitivity,
            targetMotionScore = flowResult.motionMagnitude,
            globalMotionScore = globalMotion,
            sensorMotionScore = sensorMotion,
            scaleChangeScore = flowResult.scaleChange,
            lateralMotionScore = flowResult.lateralDisplacement,
            luminanceShiftScore = flowResult.luminanceShift,
            trackingConfidence = trackingConfidence,
            isPhoneDisturbed = isPhoneDisturbed
        )

        if (decision.shouldRecalibrate) {
            opticalFlow.reset()
            globalMotionEstimator.reset()
        }

        currentState = decision.nextState
        adaptiveController.updateState(currentState, isDeviceHot)

        val estimatedDistance = distanceEstimator.estimateDistanceMeters(currentRoi, activeProfile.vehicleType)
        val elapsed = System.currentTimeMillis() - startTime

        val updatedMetrics = TrackingMetrics(
            trackingState = currentState,
            vehicleType = activeProfile.vehicleType,
            fpsMode = adaptiveController.currentMode,
            targetConfidence = trackingConfidence,
            targetMotionScore = flowResult.motionMagnitude,
            globalMotionScore = globalMotion,
            sensorMotionScore = sensorMotion,
            scaleChangeScore = flowResult.scaleChange,
            movementConfidence = decision.movementConfidence,
            targetRoi = currentRoi,
            currentFps = measuredFps,
            processingTimeMs = elapsed,
            estimatedDistanceMeters = estimatedDistance,
            verificationProgress = decision.verificationProgress
        )

        _metrics.value = updatedMetrics
        return updatedMetrics
    }
}
