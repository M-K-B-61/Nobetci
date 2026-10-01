package com.example.presentation.components

import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.SentryAmber

@Composable
fun TargetingReticle(
    targetRoi: RectF?,
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val primaryColor = if (isLocked) RadarEmerald else SentryAmber
        val colorWithAlpha = primaryColor.copy(alpha = pulseAlpha)

        val roi = targetRoi ?: RectF(0.20f, 0.25f, 0.80f, 0.75f)

        val left = roi.left * w
        val top = roi.top * h
        val right = roi.right * w
        val bottom = roi.bottom * h
        val boxWidth = right - left
        val boxHeight = bottom - top

        val cornerLen = (boxWidth * 0.18f).coerceIn(24f, 60f)
        val strokeW = 4f

        // 4 Corner Brackets
        // Top-Left
        drawLine(colorWithAlpha, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(colorWithAlpha, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)

        // Top-Right
        drawLine(colorWithAlpha, Offset(right, top), Offset(right - cornerLen, top), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(colorWithAlpha, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)

        // Bottom-Left
        drawLine(colorWithAlpha, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(colorWithAlpha, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)

        // Bottom-Right
        drawLine(colorWithAlpha, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(colorWithAlpha, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = strokeW, cap = StrokeCap.Round)

        // Center Crosshair
        val centerX = left + (boxWidth / 2f)
        val centerY = top + (boxHeight / 2f)
        val chLen = 14f

        drawLine(colorWithAlpha.copy(alpha = 0.5f), Offset(centerX - chLen, centerY), Offset(centerX + chLen, centerY), strokeWidth = 2f)
        drawLine(colorWithAlpha.copy(alpha = 0.5f), Offset(centerX, centerY - chLen), Offset(centerX, centerY + chLen), strokeWidth = 2f)

        // Subtle bounding guide
        drawRect(
            color = primaryColor.copy(alpha = 0.12f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            style = Stroke(width = 1.5f)
        )
    }
}
