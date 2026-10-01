package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Loud multi-sensory alarm controller.
 * Wakes the driver using the maximum available alarm stream, pulsing vibration patterns,
 * and high-priority tone pulses.
 */
class AlarmController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var ringtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null
    private var fallbackToneJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    @Volatile
    var isAlarmActive: Boolean = false
        private set

    fun triggerAlarm(soundEnabled: Boolean = true, vibrationEnabled: Boolean = true) {
        if (isAlarmActive) return
        isAlarmActive = true

        // 1. Play Loud Alarm Sound
        if (soundEnabled) {
            startSound()
        }

        // 2. Powerful repeating vibration pattern
        if (vibrationEnabled) {
            startVibration()
        }
    }

    fun stopAlarm() {
        if (!isAlarmActive) return
        isAlarmActive = false

        // Stop sound
        try {
            ringtone?.stop()
            ringtone = null
        } catch (_: Exception) {}

        fallbackToneJob?.cancel()
        fallbackToneJob = null

        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}

        // Stop vibration
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    private fun startSound() {
        try {
            // Find alarm URI, fallback to notification or ringtone
            var alertUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            if (alertUri != null) {
                ringtone = RingtoneManager.getRingtone(context, alertUri)?.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        isLooping = true
                    }
                    play()
                }
            }
        } catch (_: Exception) {
            ringtone = null
        }

        // Continuous high-pitch tone pulses backup in case ringtone is silent or short
        fallbackToneJob = scope.launch {
            try {
                val tone = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                toneGenerator = tone
                while (isActive && isAlarmActive) {
                    tone.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)
                    delay(500)
                    tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
                    delay(450)
                }
            } catch (_: Exception) {}
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 200, 500, 200, 800, 300)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0)

        vibrator?.let { v ->
            if (v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, amplitudes, 0)
                    v.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(pattern, 0)
                }
            }
        }
    }
}
