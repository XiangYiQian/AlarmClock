package com.homi.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.homi.alarmclock.data.AlarmDatabase
import com.homi.alarmclock.model.Alarm
import com.homi.alarmclock.service.AlarmService
import com.homi.alarmclock.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 闹钟触发接收器 — 收到 AlarmManager 广播后启动 Service 播放铃音
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_SNOOZE_COUNT = "snooze_count"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "闹钟"
        val snoozeCount = intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0)

        Log.i(TAG, "闹钟触发: id=$alarmId, label=$label, snooze=$snoozeCount")

        // 启动前台 Service 播放铃音
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_SNOOZE_COUNT, snoozeCount)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 如果是重复闹钟, 重新调度下一次
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = AlarmDatabase.getInstance(context).alarmDao().getAlarmById(alarmId)
                if (alarm != null && alarm.enabled && !alarm.isOneShot) {
                    AlarmScheduler.scheduleAlarm(context, alarm)
                }
            } catch (e: Exception) {
                Log.e(TAG, "重新调度失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}