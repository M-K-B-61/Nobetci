package com.example.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryStatus(
    val percentage: Int = 100,
    val isCharging: Boolean = false,
    val isLowBatteryWarning: Boolean = false,
    val isCriticalBatteryWarning: Boolean = false
)

class BatteryOptimizationManager(private val context: Context) {

    private val _batteryState = MutableStateFlow(BatteryStatus())
    val batteryState: StateFlow<BatteryStatus> = _batteryState.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateFromIntent(it) }
        }
    }

    private var isRegistered = false

    fun start() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(receiver, filter)
            intent?.let { updateFromIntent(it) }
            isRegistered = true
        }
    }

    fun stop() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            isRegistered = false
        }
    }

    private fun updateFromIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val pct = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else 100

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        _batteryState.value = BatteryStatus(
            percentage = pct,
            isCharging = isCharging,
            isLowBatteryWarning = pct <= 20 && !isCharging,
            isCriticalBatteryWarning = pct <= 10 && !isCharging
        )
    }
}
