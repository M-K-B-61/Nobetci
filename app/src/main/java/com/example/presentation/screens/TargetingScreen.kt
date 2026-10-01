package com.example.presentation.screens

import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.TrackingMetrics
import com.example.model.VehicleType
import com.example.presentation.components.TargetingReticle
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SentryAmber

@Composable
fun TargetingScreen(
    vehicleType: VehicleType,
    metrics: TrackingMetrics,
    previewView: PreviewView,
    onManualTargetSelect: (RectF) -> Unit,
    onConfirmTarget: () -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }
    val colorScheme = MaterialTheme.colorScheme

    val isVehicleLocked = metrics.targetConfidence >= 0.65f

    val statusMessage = when {
        isVehicleLocked && vehicleType == VehicleType.HEAVY_VEHICLE -> "Tır / Kamyon algılandı ✓"
        isVehicleLocked && vehicleType == VehicleType.AUTOMOBILE -> "Otomobil algılandı ✓"
        else -> "Öndeki araç taranıyor..."
    }

    val activeColor = if (isVehicleLocked) colorScheme.primary else SentryAmber

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        // Camera Preview with Tap-to-re-aim
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_targeting_preview")
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val normX = (offset.x / size.width).coerceIn(0.1f, 0.9f)
                        val normY = (offset.y / size.height).coerceIn(0.1f, 0.9f)
                        val halfW = if (vehicleType == VehicleType.HEAVY_VEHICLE) 0.35f else 0.25f
                        val halfH = if (vehicleType == VehicleType.HEAVY_VEHICLE) 0.35f else 0.20f

                        val manualRoi = RectF(
                            (normX - halfW).coerceIn(0.05f, 0.95f),
                            (normY - halfH).coerceIn(0.05f, 0.95f),
                            (normX + halfW).coerceIn(0.05f, 0.95f),
                            (normY + halfH).coerceIn(0.05f, 0.95f)
                        )
                        onManualTargetSelect(manualRoi)
                    }
                }
        )

        // Targeting Reticle HUD Overlay
        TargetingReticle(
            targetRoi = metrics.targetRoi,
            isLocked = isVehicleLocked,
            modifier = Modifier.fillMaxSize()
        )

        // Top Gradient & Header Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBackground.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("targeting_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Hedef Belirleme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${vehicleType.iconEmoji} ${vehicleType.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.primary
                    )
                }
            }
        }

        // Status Card & Action Button at the Bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.92f), DarkBackground)
                    )
                )
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Status pill with real-time loading or locked check
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = activeColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeColor),
                    modifier = Modifier.testTag("targeting_status_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isVehicleLocked) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = activeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                color = activeColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusMessage,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Hedefi değiştirmek için ekrandaki araca dokunabilirsiniz",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onConfirmTarget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("confirm_target_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = activeColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = PitchBlack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NÖBETİ BAŞLAT",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = PitchBlack
                    )
                }
            }
        }
    }
}
