package com.homi.alarmclock

import android.app.Application
import com.homi.alarmclock.data.AlarmDatabase

class AlarmClockApp : Application() {
    val database: AlarmDatabase by lazy { AlarmDatabase.getInstance(this) }
}