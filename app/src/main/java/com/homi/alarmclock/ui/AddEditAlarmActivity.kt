package com.homi.alarmclock.ui

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.homi.alarmclock.AlarmClockApp
import com.homi.alarmclock.R
import com.homi.alarmclock.databinding.ActivityAddEditAlarmBinding
import com.homi.alarmclock.model.Alarm
import com.homi.alarmclock.util.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddEditAlarmActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ALARM = "extra_alarm"
        private const val TAG = "AddEditAlarm"
    }

    private lateinit var binding: ActivityAddEditAlarmBinding
    private var editingAlarm: Alarm? = null

    // 当前选中的星期 (bitmap)
    private var selectedDays = 0
    private val dayButtons by lazy {
        listOf(
            binding.btnSun, binding.btnMon, binding.btnTue,
            binding.btnWed, binding.btnThu, binding.btnFri,
            binding.btnSat
        )
    }

    // 当前选择的铃音
    private var selectedRingtoneUri: String? = null
    private var selectedRingtoneName: String = "默认铃声"

    // 时间
    private var hour: Int = 7
    private var minute: Int = 0

    private lateinit var permissionLauncher: ActivityResultContracts.RequestMultiplePermissions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // 权限回调
        }

        // 接收编辑数据
        editingAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ALARM, Alarm::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ALARM)
        }

        setupUI()
        loadAlarmData()
    }

    private fun setupUI() {
        // 返回按钮
        binding.topAppBar.setNavigationOnClickListener { finish() }

        // 时间选择
        binding.layoutTime.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                hour = h
                minute = m
                updateTimeDisplay()
            }, hour, minute, true).show()
        }

        // 星期选择 (toggle)
        dayButtons.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                val bitIndex = index // 0=Sunday ... 6=Saturday
                val bit = 1 shl bitIndex
                if (selectedDays and bit != 0) {
                    selectedDays = selectedDays and bit.inv()
                    chip.isSelected = false
                } else {
                    selectedDays = selectedDays or bit
                    chip.isSelected = true
                }
                updateRepeatSummary()
            }
        }

        // 铃音选择: 本地文件
        binding.layoutRingtone.setOnClickListener {
            showRingtonePicker()
        }

        // 铃音选择: 在线搜索
        binding.btnSearchMusic.setOnClickListener {
            val intent = Intent(this, MusicSearchActivity::class.java)
            startActivityForResult(intent, REQUEST_MUSIC_SEARCH)
        }

        // 震动开关
        binding.switchVibrate.setOnCheckedChangeListener { _, _ -> }

        // 渐强开关
        binding.switchFadeIn.setOnCheckedChangeListener { _, _ -> }

        // 贪睡开关
        binding.switchSnooze.setOnCheckedChangeListener { _, _ -> }

        // 保存
        binding.btnSave.setOnClickListener { saveAlarm() }
    }

    private fun loadAlarmData() {
        val alarm = editingAlarm
        if (alarm != null) {
            hour = alarm.hour
            minute = alarm.minute
            selectedDays = alarm.daysOfWeek
            selectedRingtoneUri = alarm.ringtoneUri
            selectedRingtoneName = alarm.ringtoneName

            binding.editLabel.setText(alarm.label)
            binding.switchVibrate.isChecked = alarm.vibrate
            binding.switchFadeIn.isChecked = alarm.fadeIn
            binding.sliderVolume.value = alarm.volume.toFloat()
            binding.switchSnooze.isChecked = alarm.snoozeEnabled

            // 选中星期按钮
            dayButtons.forEachIndexed { index, chip ->
                chip.isSelected = (selectedDays shr index) and 1 == 1
            }

            binding.topAppBar.title = "编辑闹钟"
        } else {
            // 默认设为当前时间+1小时
            val now = java.util.Calendar.getInstance()
            hour = (now.get(java.util.Calendar.HOUR_OF_DAY) + 1) % 24
            minute = 0
            binding.topAppBar.title = "新建闹钟"
        }

        updateTimeDisplay()
        updateRepeatSummary()
        updateRingtoneDisplay()
    }

    private fun updateTimeDisplay() {
        binding.textTime.text = String.format("%02d:%02d", hour, minute)
    }

    private fun updateRepeatSummary() {
        val tempAlarm = Alarm(hour = hour, minute = minute, daysOfWeek = selectedDays)
        binding.textRepeatSummary.text = tempAlarm.getRepeatDescription()
    }

    private fun updateRingtoneDisplay() {
        binding.textRingtone.text = selectedRingtoneName
    }

    private fun showRingtonePicker() {
        val items = arrayOf("默认铃声", "从本地文件选择", "在线搜索音乐")
        MaterialAlertDialogBuilderX(items)
    }

    private fun MaterialAlertDialogBuilderX(items: Array<String>) {
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        builder.setTitle("选择铃音")
        builder.setItems(items) { _, which ->
            when (which) {
                0 -> {
                    selectedRingtoneUri = null
                    selectedRingtoneName = "默认铃声"
                    updateRingtoneDisplay()
                }
                1 -> {
                    // 从文件选择
                    openFilePicker()
                }
                2 -> {
                    // 在线搜索
                    val intent = Intent(this, MusicSearchActivity::class.java)
                    startActivityForResult(intent, REQUEST_MUSIC_SEARCH)
                }
            }
        }
        builder.show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "选择音频文件"), REQUEST_FILE_PICK)
    }

    @Deprecated("Use registerForActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_FILE_PICK -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        selectedRingtoneUri = uri.toString()
                        selectedRingtoneName = "本地音频"
                        updateRingtoneDisplay()
                        Toast.makeText(this, "已选择: 本地音频", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            REQUEST_MUSIC_SEARCH -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.let {
                        selectedRingtoneUri = it.getStringExtra(MusicSearchActivity.RESULT_RINGTONE_URI)
                        selectedRingtoneName = it.getStringExtra(MusicSearchActivity.RESULT_RINGTONE_NAME) ?: "自定义铃音"
                        updateRingtoneDisplay()
                        Toast.makeText(this, "已选择: $selectedRingtoneName", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun saveAlarm() {
        val label = binding.editLabel.text.toString().trim()

        val alarm = Alarm(
            id = editingAlarm?.id ?: 0,
            hour = hour,
            minute = minute,
            label = label,
            enabled = true,
            daysOfWeek = selectedDays,
            ringtoneUri = selectedRingtoneUri,
            ringtoneName = selectedRingtoneName,
            volume = binding.sliderVolume.value.toInt(),
            vibrate = binding.switchVibrate.isChecked,
            snoozeEnabled = binding.switchSnooze.isChecked,
            snoozeInterval = 5,
            snoozeCount = 3,
            fadeIn = binding.switchFadeIn.isChecked,
            fadeInDuration = 30
        )

        // 验证时间: 一次性闹钟不能设过去时间
        if (alarm.isOneShot) {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(this, "请选择大于当前时间的时间", Toast.LENGTH_SHORT).show()
                return
            }
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (editingAlarm != null) {
                    // 更新
                    (application as AlarmClockApp).database.alarmDao().update(alarm)
                    AlarmScheduler.cancelAlarm(this@AddEditAlarmActivity, alarm.id)
                } else {
                    // 新建
                    val id = (application as AlarmClockApp).database.alarmDao().insert(alarm)
                    val newAlarm = alarm.copy(id = id)
                    AlarmScheduler.scheduleAlarm(this@AddEditAlarmActivity, newAlarm)
                    return@withContext
                }
            }
            // 重新调度
            AlarmScheduler.scheduleAlarm(this@AddEditAlarmActivity, alarm)
            Toast.makeText(this@AddEditAlarmActivity, "闹钟已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        private const val REQUEST_FILE_PICK = 1001
        private const val REQUEST_MUSIC_SEARCH = 1002
    }
}