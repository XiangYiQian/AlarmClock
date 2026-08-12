package com.homi.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.homi.alarmclock.service.AlarmService
import com.homi.alarmclock.util.RingtonePlayer

/**
 * 通知栏操作接收器 — 处理贪睡和关闭操作
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationAction"
        const val ACTION_SNOOZE = "com.homi.alarmclock.SNOOZE"
        const val ACTION_DISMISS = "com.homi.alarmclock.DISMISS"
        const val EXTRA_ALARM_ID = "alarm_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        when (intent.action) {
            ACTION_SNOOZE -> {
                Log.i(TAG, "贪睡: alarmId=$alarmId")
                RingtonePlayer.stop()
                // 闹钟会由 AlarmService 重新调度贪睡
                // 停止 Service
                context.stopService(Intent(context, AlarmService::class.java))
            }
            ACTION_DISMISS -> {
                Log.i(TAG, "关闭: alarmId=$alarmId")
                RingtonePlayer.stop()
                context.stopService(Intent(context, AlarmService::class.java))
            }
        }
    }
}