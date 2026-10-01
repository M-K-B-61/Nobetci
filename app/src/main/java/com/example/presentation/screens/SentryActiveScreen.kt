package com.example.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrackingMetrics
import com.example.model.TrackingState
import com.example.model.VehicleType
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.RadarEmerald
import com.example.ui.theme.SentryAlertRed
import com.example.ui.theme.SentryAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SentryActiveScreen(
    vehicleType: VehicleType,
    metrics: TrackingMetrics,
    distanceLabel: String,
    onStopSentry: () -> Unit,
    showDebugPanel: Boolean,
    onToggleDebugPanel: () -> Unit
) {
    BackHandler { onStopSentry() }

    var isInfoVisible by remember { mutableStateOf(true) }
    var userTouchTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // Auto-dim into 100% pitch-black after 4 seconds of inactivity
    LaunchedEffect(userTouchTimestamp) {
        isInfoVisible = true
        delay(4500L)
        isInfoVisible = false
    }

    val isCameraOrSensorMoving = metrics.trackingState == TrackingState.CAMERA_MOVED ||
            metrics.trackingState == TrackingState.STABILIZING

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .testTag("sentry_active_screen")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                userTouchTimestamp = System.currentTimeMillis()
            }
    ) {
        // Minimal pulse indicator always visible in corner (super low OLED power)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isCameraOrSensorMoving) SentryAmber.copy(alpha = 0.3f) else RadarEmerald.copy(alpha = 0.25f),
                modifier = Modifier.size(10.dp)
            ) {}
        }

        // Auto-hiding HUD
        AnimatedVisibility(
            visible = isInfoVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NÖBETÇİ",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = RadarEmerald,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nöbet aktif",
                                style = MaterialTheme.typography.bodySmall,
                                color = RadarEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Debug Toggle Button
                    IconButton(
                        onClick = onToggleDebugPanel,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sentry_debug_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debug Paneli",
                            tint = if (showDebugPanel) RadarEmerald else TextMuted
                        )
                    }
                }

                // Center Information Card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = vehicleType.iconEmoji,
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = vehicleType.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Öndeki araç takip ediliyor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Approximate distance tag
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = distanceLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Battery & Adaptive Mode Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Pil Optimizasyonu: Aktif (${metrics.fpsMode.targetFps} FPS)",
                            style = MaterialTheme.typography.bodySmall,
                            color = RadarEmerald,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Phone shake warning if camera is wobbling
                    if (isCameraOrSensorMoving) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SentryAmber.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SentryAmber)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = SentryAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cihaz sarsıntısı algılandı — Sabitleniyor...",
                                    fontSize = 12.sp,
                                    color = SentryAmber,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (showDebugPanel) {
                        Spacer(modifier = Modifier.height(16.dp))
                        DebugOverlay(metrics = metrics)
                    }
                }

                // Bottom Stop Button & Touch hint
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ekran pil tasarrufu için kararacaktır. Dokunarak uyandırabilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onStopSentry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("stop_sentry_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceVariant,
                            contentColor = SentryAlertRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nöbeti Bitir",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
