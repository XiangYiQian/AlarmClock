package com.homi.alarmclock.model

import com.google.gson.annotations.SerializedName

/**
 * 音乐搜索结果
 */
data class MusicSearchResult(
    @SerializedName("result") val result: MusicResultData? = null,
    @SerializedName("code") val code: Int = 0
)

data class MusicResultData(
    @SerializedName("songs") val songs: List<MusicSong>? = null,
    @SerializedName("songCount") val songCount: Int = 0
)

data class MusicSong(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("artists") val artists: List<MusicArtist>? = null,
    @SerializedName("album") val album: MusicAlbum? = null,
    @SerializedName("duration") val duration: Long = 0,
    @SerializedName("fee") val fee: Int = 0 // 0=免费, 1=VIP
) {
    fun getArtistNames(): String {
        return artists?.joinToString("/") { it.name } ?: "未知歌手"
    }

    fun getDurationFormatted(): String {
        val sec = duration / 1000
        return String.format("%02d:%02d", sec / 60, sec % 60)
    }
}

data class MusicArtist(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String = ""
)

data class MusicAlbum(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("name") val name: String = "",
    @SerializedName("picUrl") val picUrl: String? = null
)

/**
 * 歌曲详情 (含播放URL)
 */
data class MusicSongDetail(
    @SerializedName("data") val data: MusicSongDetailData? = null,
    @SerializedName("code") val code: Int = 0
)

data class MusicSongDetailData(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("url") val url: String? = null,
    @SerializedName("br") val br: Long = 0,
    @SerializedName("size") val size: Long = 0,
    @SerializedName("type") val type: String? = null
)