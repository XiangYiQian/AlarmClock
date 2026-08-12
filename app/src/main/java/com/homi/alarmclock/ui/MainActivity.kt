package com.homi.alarmclock.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.homi.alarmclock.AlarmClockApp
import com.homi.alarmclock.R
import com.homi.alarmclock.databinding.ActivityMainBinding
import com.homi.alarmclock.databinding.ItemAlarmBinding
import com.homi.alarmclock.model.Alarm
import com.homi.alarmclock.util.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AlarmListAdapter

    // 用于接收 AddEditAlarmActivity 的返回结果
    private var pendingTogglePosition: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFab()
        observeAlarms()
    }

    private fun setupRecyclerView() {
        adapter = AlarmListAdapter(
            onToggle = { alarm, enabled ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        (application as AlarmClockApp).database.alarmDao().setEnabled(alarm.id, enabled)
                    }
                    if (enabled) {
                        AlarmScheduler.scheduleAlarm(this@MainActivity, alarm.copy(enabled = true))
                    } else {
                        AlarmScheduler.cancelAlarm(this@MainActivity, alarm.id)
                    }
                }
            },
            onClick = { alarm ->
                // 编辑闹钟
                val intent = Intent(this, AddEditAlarmActivity::class.java).apply {
                    putExtra(AddEditAlarmActivity.EXTRA_ALARM, alarm)
                }
                startActivity(intent)
            },
            onDelete = { alarm ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("删除闹钟")
                    .setMessage("确定删除「${alarm.label.ifEmpty { alarm.formatTime() }}」吗？")
                    .setNegativeButton("取消") { _, _ -> }
                    .setPositiveButton("删除") { _, _ ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                (application as AlarmClockApp).database.alarmDao().delete(alarm)
                            }
                            AlarmScheduler.cancelAlarm(this@MainActivity, alarm.id)
                        }
                    }
                    .show()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, AddEditAlarmActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeAlarms() {
        lifecycleScope.launch {
            (application as AlarmClockApp).database.alarmDao().getAllAlarms().collectLatest { alarms ->
                adapter.submitList(alarms)
                binding.emptyView.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
}

/**
 * 闹钟列表适配器
 */
class AlarmListAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onClick: (Alarm) -> Unit,
    private val onDelete: (Alarm) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Alarm, AlarmListAdapter.AlarmViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Alarm>() {
        override fun areItemsTheSame(a: Alarm, b: Alarm) = a.id == b.id
        override fun areContentsTheSame(a: Alarm, b: Alarm) = a == b
    }
) {

    inner class AlarmViewHolder(val binding: ItemAlarmBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = getItem(position)
        with(holder.binding) {
            textTime.text = alarm.formatTime()
            textLabel.text = alarm.label.ifEmpty { "闹钟" }
            textRepeat.text = alarm.getRepeatDescription()
            textRingtone.text = alarm.ringtoneName

            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = alarm.enabled
            switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(alarm, isChecked)
            }

            root.setOnClickListener { onClick(alarm) }

            root.setOnLongClickListener {
                onDelete(alarm)
                true
            }
        }
    }
}