package com.homi.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.homi.alarmclock.data.AlarmDatabase
import com.homi.alarmclock.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 开机完成 / 时间变更 后重新调度所有闹钟
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_TIME_SET,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.i(TAG, "收到广播: ${intent.action}, 重新调度闹钟")

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val alarms = AlarmDatabase.getInstance(context).alarmDao().getEnabledAlarms()
                        AlarmScheduler.rescheduleAll(context, alarms)
                        Log.i(TAG, "重新调度 ${alarms.size} 个闹钟")
                    } catch (e: Exception) {
                        Log.e(TAG, "重新调度失败", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}