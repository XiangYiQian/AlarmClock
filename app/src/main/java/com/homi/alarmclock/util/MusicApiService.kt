package com.homi.alarmclock.util

import com.homi.alarmclock.model.MusicSearchResult
import com.homi.alarmclock.model.MusicSongDetail
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 音乐搜索 API 接口
 * 使用网易云音乐非官方 API
 */
interface MusicApiService {

    /**
     * 搜索歌曲
     * @param keywords 关键词
     * @param limit 返回数量 (默认30)
     * @param offset 偏移量
     */
    @GET("search")
    suspend fun searchMusic(
        @Query("keywords") keywords: String,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0,
        @Query("type") type: Int = 1 // 1=单曲
    ): MusicSearchResult

    /**
     * 获取歌曲播放URL
     * @param id 歌曲ID
     * @param br 码率 (默认320000 = 320kbps)
     */
    @GET("song/url")
    suspend fun getSongUrl(
        @Query("id") id: Long,
        @Query("br") br: Long = 320000
    ): MusicSongDetail
}