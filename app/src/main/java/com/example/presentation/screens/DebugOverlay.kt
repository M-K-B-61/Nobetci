package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrackingMetrics
import com.example.presentation.components.MetricRow
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.SentryAlertRed
import com.example.ui.theme.SentryAmber

@Composable
fun DebugOverlay(
    metrics: TrackingMetrics,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("debug_telemetry_hud")
            .background(Color(0xEE0B0F14), RoundedCornerShape(12.dp))
            .border(1.dp, ElectricBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "⚡ TELEMETRİ & DEBUG HUD",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = ElectricBlue
            )
            Spacer(modifier = Modifier.height(6.dp))

            MetricRow(
                label = "Tracking State",
                value = metrics.trackingState.name,
                valueColor = if (metrics.trackingState.name.contains("ALARM") || metrics.trackingState.name.contains("CONFIRMED"))
                    SentryAlertRed else RadarEmerald
            )
            MetricRow(
                label = "Mode",
                value = metrics.vehicleType.name
            )
            MetricRow(
                label = "FPS Mode",
                value = "${metrics.fpsMode.name} (~${metrics.fpsMode.targetFps} FPS)",
                valueColor = SentryAmber
            )
            MetricRow(
                label = "Analysis FPS",
                value = "%.1f FPS".format(metrics.currentFps)
            )
            MetricRow(
                label = "Frame Latency",
                value = "${metrics.processingTimeMs} ms"
            )
            MetricRow(
                label = "Target Confidence",
                value = "%.0f%%".format(metrics.targetConfidence * 100f),
                valueColor = if (metrics.targetConfidence > 0.6f) RadarEmerald else SentryAlertRed
            )
            MetricRow(
                label = "Movement Conf.",
                value = "%.0f%%".format(metrics.movementConfidence * 100f),
                valueColor = if (metrics.movementConfidence > 0.6f) SentryAlertRed else RadarEmerald
            )
            MetricRow(
                label = "Target Motion",
                value = "%.3f".format(metrics.targetMotionScore)
            )
            MetricRow(
                label = "Global Ego-Motion",
                value = "%.3f".format(metrics.globalMotionScore)
            )
            MetricRow(
                label = "Sensor Motion",
                value = "%.3f".format(metrics.sensorMotionScore),
                valueColor = if (metrics.sensorMotionScore > 0.35f) SentryAmber else Color.White
            )
            MetricRow(
                label = "Scale Change",
                value = "%.3f".format(metrics.scaleChangeScore)
            )
            MetricRow(
                label = "Verification",
                value = "%.0f%%".format(metrics.verificationProgress * 100f)
            )
            MetricRow(
                label = "Camera Resolution",
                value = "640x480 (YUV420)"
            )
            MetricRow(
                label = "Battery",
                value = "${metrics.batteryPct}%"
            )
            MetricRow(
                label = "Thermal Status",
                value = metrics.thermalStatusString
            )
        }
    }
}
