package com.example.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Real-time hardware motion sensor fusion.
 * Listens to Gyroscope and Accelerometer to detect phone movements, mount wobbles,
 * cabin jolts, and driver touches.
 */
class SensorMotionDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    @Volatile
    var sensorMotionScore: Float = 0f
        private set

    @Volatile
    var isPhoneDisturbed: Boolean = false
        private set

    private var lastAccelX = 0f
    private var lastAccelY = 0f
    private var lastAccelZ = 9.8f
    private var isFirstReading = true

    fun startListening() {
        isFirstReading = true
        sensorMotionScore = 0f
        isPhoneDisturbed = false
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        sensorMotionScore = 0f
        isPhoneDisturbed = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                if (isFirstReading) {
                    lastAccelX = x
                    lastAccelY = y
                    lastAccelZ = z
                    isFirstReading = false
                    return
                }

                val deltaX = abs(x - lastAccelX)
                val deltaY = abs(y - lastAccelY)
                val deltaZ = abs(z - lastAccelZ)

                lastAccelX = x
                lastAccelY = y
                lastAccelZ = z

                // High-pass jerk detection
                val jerk = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
                val normalizedJerk = (jerk / 3.0f).coerceIn(0f, 2.0f)

                // Smooth update
                sensorMotionScore = (sensorMotionScore * 0.7f) + (normalizedJerk * 0.3f)
                isPhoneDisturbed = sensorMotionScore > 0.40f
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]
                val angularSpeed = sqrt((gx * gx + gy * gy + gz * gz).toDouble()).toFloat()
                val normalizedAngular = (angularSpeed / 1.5f).coerceIn(0f, 2.0f)

                sensorMotionScore = (sensorMotionScore * 0.6f) + (normalizedAngular * 0.4f)
                if (normalizedAngular > 0.35f) {
                    isPhoneDisturbed = true
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
