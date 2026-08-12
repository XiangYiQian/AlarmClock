package com.homi.alarmclock.util

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * 音乐下载管理 — 从网络搜索并下载音乐文件到本地
 */
object MusicDownloadManager {

    private const val TAG = "MusicDownloadManager"
    private const val MUSIC_DIR = "AlarmClock/ringtones"

    /**
     * 获取铃声存储目录
     */
    fun getRingtonesDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), MUSIC_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 下载音乐文件
     * @param url 播放地址
     * @param fileName 保存文件名
     * @return 下载后的本地文件, null=失败
     */
    fun download(url: String, fileName: String, context: Context): File? {
        return try {
            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "下载失败: HTTP ${response.code}")
                return null
            }

            val targetFile = File(getRingtonesDir(context), fileName)
            response.body?.byteStream()?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 通知媒体扫描器
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf("audio/*")
            ) { _, _ -> }

            Log.i(TAG, "下载成功: ${targetFile.absolutePath}")
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "下载异常", e)
            null
        }
    }

    /**
     * 列出已下载的铃声文件
     */
    fun listLocalRingtones(context: Context): List<File> {
        val dir = getRingtonesDir(context)
        return dir.listFiles { f -> f.isFile && f.name.endsWith(".mp3") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * 删除铃声文件
     */
    fun deleteRingtone(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }
}