package com.homi.alarmclock.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.homi.alarmclock.AlarmClockApp
import com.homi.alarmclock.data.AlarmDatabase
import com.homi.alarmclock.databinding.ActivityAlarmFiringBinding
import com.homi.alarmclock.model.Alarm
import com.homi.alarmclock.receiver.AlarmReceiver
import com.homi.alarmclock.receiver.NotificationActionReceiver
import com.homi.alarmclock.service.AlarmService
import com.homi.alarmclock.util.AlarmScheduler
import com.homi.alarmclock.util.RingtonePlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 闹钟响铃全屏界面 — 全屏显示闹钟信息, 提供贪睡和关闭按钮
 */
class AlarmFiringActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AlarmFiring"
    }

    private lateinit var binding: ActivityAlarmFiringBinding
    private var alarmId: Long = -1
    private var label: String = "闹钟"
    private var snoozeCount: Int = 0
    private var alarm: Alarm? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 确保在锁屏上方显示
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        binding = ActivityAlarmFiringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1)
        label = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "闹钟"
        snoozeCount = intent.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_COUNT, 0)

        loadAlarm()
        setupUI()
        startTimeUpdate()
    }

    private fun loadAlarm() {
        if (alarmId > 0) {
            lifecycleScope.launch {
                alarm = withContext(Dispatchers.IO) {
                    AlarmDatabase.getInstance(this@AlarmFiringActivity).alarmDao().getAlarmById(alarmId)
                }
                alarm?.let { a ->
                    binding.textLabel.text = a.label.ifEmpty { "闹钟" }
                }
            }
        }
    }

    private fun setupUI() {
        binding.textLabel.text = label

        binding.btnSnooze.setOnClickListener {
            Log.i(TAG, "贪睡 tapped")
            alarm?.let { a ->
                if (a.snoozeEnabled && snoozeCount < a.snoozeCount) {
                    AlarmScheduler.scheduleSnooze(this, a, snoozeCount)
                    Toast.makeText(this, "${a.snoozeInterval}分钟后再次提醒", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "贪睡次数已用完", Toast.LENGTH_SHORT).show()
                }
            }
            dismissAlarm()
        }

        binding.btnDismiss.setOnClickListener {
            Log.i(TAG, "关闭 tapped")
            dismissAlarm()
        }

        // 滑动关闭
        binding.root.setOnTouchListener { _, _ ->
            false
        }
    }

    private fun dismissAlarm() {
        RingtonePlayer.stop()
        stopService(Intent(this, AlarmService::class.java))
        finishAndRemoveTask()
    }

    private fun startTimeUpdate() {
        val handler = android.os.Handler(mainLooper)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("M月d日 E", Locale.CHINA)

        val runnable = object : Runnable {
            override fun run() {
                val now = Date()
                binding.textTime.text = timeFormat.format(now)
                binding.textDate.text = dateFormat.format(now)
                handler.postDelayed(this, 1000)
            }
        }
        runnable.run()
    }

    override fun onBackPressed() {
        // 禁止返回键关闭闹钟, 必须点按钮
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}