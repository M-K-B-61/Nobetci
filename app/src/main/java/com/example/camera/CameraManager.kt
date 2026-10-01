package com.example.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages CameraX lifecycle, switching between:
 * 1) Targeting Mode (Preview + Lightweight Analysis for reticle alignment)
 * 2) Pure Sentry Mode (Preview completely UNBOUND; ImageAnalysis ONLY at 640x480 for lowest battery & thermal impact).
 */
class CameraManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var activeCamera: Camera? = null
    private var currentPreview: Preview? = null

    // Preallocated byte buffer for Y luminance plane to eliminate GC allocations
    private var cachedYBuffer: ByteArray? = null

    fun initialize(onReady: () -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                onReady()
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Mode 1: Targeting Preview. User sees live feed to point at the car ahead.
     */
    fun startTargetingPreview(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrame: (yBuffer: ByteArray, width: Int, height: Int, rowStride: Int) -> Unit
    ) {
        val provider = cameraProvider ?: return
        detachPreview()
        provider.unbindAll()

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        currentPreview = preview

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(640, 480),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(imageProxy, onFrame)
        }

        try {
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            activeCamera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        } catch (_: Exception) {}
    }

    /**
     * Mode 2: Pure Sentry Analysis.
     * Preview is completely UNBOUND and removed from GPU pipeline.
     * Only 640x480 ImageAnalysis executes in background to maximize battery life.
     */
    fun startSentryTracking(
        lifecycleOwner: LifecycleOwner,
        onFrame: (yBuffer: ByteArray, width: Int, height: Int, rowStride: Int) -> Unit
    ) {
        val provider = cameraProvider ?: return
        detachPreview()
        provider.unbindAll()

        val resolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(640, 480),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(imageProxy, onFrame)
        }

        try {
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            activeCamera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
        } catch (_: Exception) {}
    }

    private fun detachPreview() {
        try {
            currentPreview?.setSurfaceProvider(null)
        } catch (_: Exception) {}
        currentPreview = null
    }

    fun stop() {
        detachPreview()
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
        activeCamera = null
    }

    fun release() {
        stop()
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
    }

    private fun processImageProxy(
        imageProxy: ImageProxy,
        onFrame: (ByteArray, Int, Int, Int) -> Unit
    ) {
        try {
            val yPlane = imageProxy.planes[0]
            val buffer = yPlane.buffer
            val remaining = buffer.remaining()

            var yArray = cachedYBuffer
            if (yArray == null || yArray.size < remaining) {
                yArray = ByteArray(remaining)
                cachedYBuffer = yArray
            }

            buffer.get(yArray, 0, remaining)
            onFrame(yArray, imageProxy.width, imageProxy.height, yPlane.rowStride)
        } catch (_: Exception) {
        } finally {
            imageProxy.close()
        }
    }
}
