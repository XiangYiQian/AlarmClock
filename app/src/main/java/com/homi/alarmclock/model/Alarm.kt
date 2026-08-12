package com.homi.alarmclock.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 闹钟实体
 * @param id 自增主键
 * @param hour 小时 0-23
 * @param minute 分钟 0-59
 * @param label 闹钟标签 (如 "起床", "开会")
 * @param enabled 是否启用
 * @param daysOfWeek 重复星期, bitmap: bit0=周日, bit1=周一, ... bit6=周六. 0=不重复(仅一次)
 * @param ringtoneUri 铃音URI, null=默认
 * @param ringtoneName 铃音名称
 * @param volume 音量 0-100
 * @param vibrate 是否震动
 * @param snoozeEnabled 是否允许贪睡
 * @param snoozeInterval 贪睡间隔(分钟)
 * @param snoozeCount 贪睡次数
 * @param fadeIn 是否渐强
 * @param fadeInDuration 渐强时长(秒)
 */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val daysOfWeek: Int = 0,
    val ringtoneUri: String? = null,
    val ringtoneName: String = "默认铃声",
    val volume: Int = 70,
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeInterval: Int = 5,
    val snoozeCount: Int = 3,
    val fadeIn: Boolean = false,
    val fadeInDuration: Int = 30
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readLong(),
        hour = parcel.readInt(),
        minute = parcel.readInt(),
        label = parcel.readString() ?: "",
        enabled = parcel.readByte().toInt() != 0,
        daysOfWeek = parcel.readInt(),
        ringtoneUri = parcel.readString(),
        ringtoneName = parcel.readString() ?: "默认铃声",
        volume = parcel.readInt(),
        vibrate = parcel.readByte().toInt() != 0,
        snoozeEnabled = parcel.readByte().toInt() != 0,
        snoozeInterval = parcel.readInt(),
        snoozeCount = parcel.readInt(),
        fadeIn = parcel.readByte().toInt() != 0,
        fadeInDuration = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeInt(hour)
        parcel.writeInt(minute)
        parcel.writeString(label)
        parcel.writeByte(if (enabled) 1 else 0)
        parcel.writeInt(daysOfWeek)
        parcel.writeString(ringtoneUri)
        parcel.writeString(ringtoneName)
        parcel.writeInt(volume)
        parcel.writeByte(if (vibrate) 1 else 0)
        parcel.writeByte(if (snoozeEnabled) 1 else 0)
        parcel.writeInt(snoozeInterval)
        parcel.writeInt(snoozeCount)
        parcel.writeByte(if (fadeIn) 1 else 0)
        parcel.writeInt(fadeInDuration)
    }

    override fun describeContents(): Int = 0

    /**
     * 是否为一次性闹钟 (不重复)
     */
    val isOneShot: Boolean get() = daysOfWeek == 0

    /**
     * 是否在指定星期重复
     * @param dayOfWeek Calendar.SUNDAY=1 ... Calendar.SATURDAY=7
     */
    fun isRepeatOn(dayOfWeek: Int): Boolean {
        val bitIndex = dayOfWeek % 7 // SUNDAY=1 -> bit0
        return (daysOfWeek shr bitIndex) and 1 == 1
    }

    /**
     * 获取重复天数的可读描述
     */
    fun getRepeatDescription(): String {
        if (isOneShot) return "仅一次"
        if (daysOfWeek == 0x7F) return "每天"
        if (daysOfWeek == 0b0111110) return "周一至周五"
        if (daysOfWeek == 0b1000001) return "周末"

        val names = arrayOf("日", "一", "二", "三", "四", "五", "六")
        val result = StringBuilder()
        for (i in 0..6) {
            if ((daysOfWeek shr i) and 1 == 1) {
                if (result.isNotEmpty()) result.append("、")
                result.append(names[i])
            }
        }
        return "每周$result"
    }

    /**
     * 格式化时间显示 e.g. "07:30"
     */
    fun formatTime(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    companion object CREATOR : Parcelable.Creator<Alarm> {
        override fun createFromParcel(parcel: Parcel): Alarm = Alarm(parcel)
        override fun newArray(size: Int): Array<Alarm?> = arrayOfNulls(size)
    }
}