package com.example.thermal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor

data class ThermalInfo(
    val statusString: String = "Normal",
    val isElevated: Boolean = false,
    val isCritical: Boolean = false,
    val warningMessage: String? = null
)

class ThermalManager(private val context: Context, private val mainExecutor: Executor) {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _thermalInfo = MutableStateFlow(ThermalInfo())
    val thermalInfo: StateFlow<ThermalInfo> = _thermalInfo.asStateFlow()

    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            val listener = PowerManager.OnThermalStatusChangedListener { status ->
                handleThermalStatus(status)
            }
            thermalListener = listener
            try {
                powerManager.addThermalStatusListener(mainExecutor, listener)
                handleThermalStatus(powerManager.currentThermalStatus)
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            thermalListener?.let {
                try {
                    powerManager.removeThermalStatusListener(it)
                } catch (_: Exception) {}
            }
            thermalListener = null
        }
    }

    private fun handleThermalStatus(status: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (status) {
                PowerManager.THERMAL_STATUS_NONE,
                PowerManager.THERMAL_STATUS_LIGHT -> {
                    _thermalInfo.value = ThermalInfo(
                        statusString = "Normal",
                        isElevated = false,
                        isCritical = false,
                        warningMessage = null
                    )
                }
                PowerManager.THERMAL_STATUS_MODERATE -> {
                    _thermalInfo.value = ThermalInfo(
                        statusString = "Orta Sıcaklık",
                        isElevated = true,
                        isCritical = false,
                        warningMessage = "Telefon ısınıyor — Nöbetçi güç tüketimini azalttı."
                    )
                }
                PowerManager.THERMAL_STATUS_SEVERE,
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> {
                    _thermalInfo.value = ThermalInfo(
                        statusString = "Yüksek Sıcaklık",
                        isElevated = true,
                        isCritical = true,
                        warningMessage = "Telefon aşırı ısındı. Nöbetçi minimum enerji modunda çalışıyor."
                    )
                }
                else -> {
                    _thermalInfo.value = ThermalInfo(statusString = "Normal")
                }
            }
        }
    }
}
