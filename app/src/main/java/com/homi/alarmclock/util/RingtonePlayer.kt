package com.homi.alarmclock.util

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import com.homi.alarmclock.model.Alarm
import java.io.File

/**
 * 铃音播放 + 震动管理
 */
object RingtonePlayer {

    private const val TAG = "RingtonePlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var fadeHandler: Handler? = null
    private var fadeRunnable: Runnable? = null
    private var isPlaying = false

    /**
     * 开始播放闹钟铃音
     */
    fun start(context: Context, alarm: Alarm) {
        stop()
        isPlaying = true

        // 播放铃音
        try {
            mediaPlayer = MediaPlayer().apply {
                val uri = getRingtoneUri(context, alarm)
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true

                // 音量
                val volume = alarm.volume / 100f
                if (alarm.fadeIn && alarm.fadeInDuration > 0) {
                    setVolume(0f, 0f)
                    startFadeIn(alarm.fadeInDuration, volume)
                } else {
                    setVolume(volume, volume)
                }

                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放铃音失败", e)
            // 尝试使用系统默认闹钟铃声
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, Settings.System.DEFAULT_ALARM_ALERT_URI)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    val volume = alarm.volume / 100f
                    setVolume(volume, volume)
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                Log.e(TAG, "默认铃音也失败了", e2)
            }
        }

        // 震动
        if (alarm.vibrate) {
            startVibration(context)
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        isPlaying = false
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null

        fadeRunnable?.let { fadeHandler?.removeCallbacks(it) }
        fadeRunnable = null
        fadeHandler = null

        stopVibration()
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying

    private fun getRingtoneUri(context: Context, alarm: Alarm): Uri {
        return when {
            // 自定义铃音
            !alarm.ringtoneUri.isNullOrEmpty() -> {
                val uri = Uri.parse(alarm.ringtoneUri)
                // 如果是本地文件路径
                if (uri.scheme == null || uri.scheme == "file") {
                    val file = File(uri.path ?: "")
                    if (file.exists()) {
                        return Uri.fromFile(file)
                    }
                }
                uri
            }
            // 系统默认闹钟铃声
            else -> Settings.System.DEFAULT_ALARM_ALERT_URI
        }
    }

    private fun startFadeIn(durationSec: Int, targetVolume: Float) {
        fadeHandler = Handler(Looper.getMainLooper())
        val steps = durationSec * 10 // 每100ms一步
        var currentStep = 0

        fadeRunnable = object : Runnable {
            override fun run() {
                currentStep++
                val progress = currentStep.toFloat() / steps
                val vol = (progress * targetVolume).coerceAtMost(targetVolume)
                mediaPlayer?.setVolume(vol, vol)
                if (currentStep < steps) {
                    fadeHandler?.postDelayed(this, 100)
                }
            }
        }
        fadeHandler?.postDelayed(fadeRunnable!!, 100)
    }

    private fun startVibration(context: Context) {
        val pattern = longArrayOf(0, 1000, 1000, 1000, 1000)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }
}