package com.homi.alarmclock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.homi.alarmclock.R
import com.homi.alarmclock.data.AlarmDatabase
import com.homi.alarmclock.model.Alarm
import com.homi.alarmclock.receiver.NotificationActionReceiver
import com.homi.alarmclock.ui.AlarmFiringActivity
import com.homi.alarmclock.util.AlarmScheduler
import com.homi.alarmclock.util.RingtonePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 闹钟前台服务 — 播放铃音 + 显示通知
 */
class AlarmService : LifecycleService() {

    companion object {
        private const val TAG = "AlarmService"
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_SNOOZE_COUNT = "snooze_count"
    }

    private var alarmId: Long = -1
    private var label: String = "闹钟"
    private var snoozeCount: Int = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        intent?.let {
            alarmId = it.getLongExtra(EXTRA_ALARM_ID, -1)
            label = it.getStringExtra(EXTRA_ALARM_LABEL) ?: "闹钟"
            snoozeCount = it.getIntExtra(EXTRA_SNOOZE_COUNT, 0)
        }

        Log.i(TAG, "Service 启动: alarmId=$alarmId, label=$label, snooze=$snoozeCount")

        // 显示通知
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 获取闹钟信息并播放铃音
        if (alarmId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarm = AlarmDatabase.getInstance(this@AlarmService).alarmDao().getAlarmById(alarmId)
                    if (alarm != null) {
                        RingtonePlayer.start(this@AlarmService, alarm)
                    } else {
                        // 找不到闹钟, 播默认铃音
                        RingtonePlayer.start(this@AlarmService, Alarm(
                            hour = 0, minute = 0
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "获取闹钟信息失败", e)
                    RingtonePlayer.start(this@AlarmService, Alarm(
                        hour = 0, minute = 0
                    ))
                }
            }
        }

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        // 启动全屏闹钟界面
        val fullScreenIntent = Intent(this, AlarmFiringActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_SNOOZE_COUNT, snoozeCount)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 贪睡操作
        val snoozeIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this, 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 关闭操作
        val dismissIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(NotificationActionReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ $label")
            .setContentText("闹钟响了 — ${if (snoozeCount > 0) "第${snoozeCount}次贪睡" else "点击关闭"}")
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.drawable.ic_snooze, "贪睡", snoozePendingIntent)
            .addAction(R.drawable.ic_close, "关闭", dismissPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "闹钟提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "闹钟铃响通知"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        RingtonePlayer.stop()
        super.onDestroy()
    }
}