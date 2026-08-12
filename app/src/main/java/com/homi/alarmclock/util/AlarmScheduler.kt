package com.homi.alarmclock.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.homi.alarmclock.model.Alarm
import com.homi.alarmclock.receiver.AlarmReceiver
import java.util.Calendar

/**
 * 闹钟调度管理 — 负责将闹钟注册到系统 AlarmManager
 */
object AlarmScheduler {

    private const val EXTRA_ALARM_ID = "alarm_id"
    private const val EXTRA_ALARM_LABEL = "alarm_label"
    private const val EXTRA_SNOOZE_COUNT = "snooze_count"

    /**
     * 调度单个闹钟
     */
    fun scheduleAlarm(context: Context, alarm: Alarm) {
        if (!alarm.enabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createAlarmIntent(context, alarm)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = calculateTriggerTime(alarm)

        // 使用精确闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // 降级为非精确
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * 取消闹钟
     */
    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.homi.alarmclock.ALARM_FIRED"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * 重新调度所有已启用的闹钟 (用于开机后恢复)
     */
    fun rescheduleAll(context: Context, alarms: List<Alarm>) {
        alarms.filter { it.enabled }.forEach { scheduleAlarm(context, it) }
    }

    /**
     * 贪睡: 在当前时间后 delayMinutes 分钟再次触发
     */
    fun scheduleSnooze(context: Context, alarm: Alarm, snoozeCount: Int) {
        if (snoozeCount >= alarm.snoozeCount) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createAlarmIntent(context, alarm).apply {
            putExtra(EXTRA_SNOOZE_COUNT, snoozeCount + 1)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarm.id * 100 + snoozeCount + 1).toInt(), // 不同的 requestCode
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + alarm.snoozeInterval * 60_000L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    /**
     * 计算下一次触发时间
     */
    private fun calculateTriggerTime(alarm: Alarm): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.isOneShot) {
            // 一次性闹钟: 如果时间已过, 设为明天
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // 重复闹钟: 找到下一个匹配的星期
            val now = Calendar.getInstance()
            var dayOffset = 0
            while (dayOffset <= 7) {
                val testDay = (now.get(Calendar.DAY_OF_WEEK) + dayOffset - 1) % 7 // 0=Sunday
                if ((alarm.daysOfWeek shr testDay) and 1 == 1) {
                    // 检查如果偏移为0且当前时间已过，则跳到下个匹配日
                    if (dayOffset == 0 && calendar.timeInMillis <= System.currentTimeMillis()) {
                        // 今天这个时间已过, 尝试下一个匹配
                        val tempCal = calendar.clone() as Calendar
                        // 找下一个匹配的日子
                        var nextOffset = 1
                        while (nextOffset <= 7) {
                            val nextDay = (now.get(Calendar.DAY_OF_WEEK) + nextOffset - 1) % 7
                            if ((alarm.daysOfWeek shr nextDay) and 1 == 1) {
                                calendar.add(Calendar.DAY_OF_YEAR, nextOffset)
                                break
                            }
                            nextOffset++
                        }
                        break
                    }
                    if (dayOffset > 0) {
                        calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
                    }
                    break
                }
                dayOffset++
            }
        }

        return calendar.timeInMillis
    }

    private fun createAlarmIntent(context: Context, alarm: Alarm): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            action = "com.homi.alarmclock.ALARM_FIRED"
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_LABEL, alarm.label.ifEmpty { "闹钟" })
            putExtra(EXTRA_SNOOZE_COUNT, 0)
        }
    }
}