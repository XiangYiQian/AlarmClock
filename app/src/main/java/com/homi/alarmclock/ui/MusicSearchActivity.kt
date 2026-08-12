package com.homi.alarmclock.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.homi.alarmclock.R
import com.homi.alarmclock.databinding.ActivityMusicSearchBinding
import com.homi.alarmclock.databinding.ItemMusicSearchBinding
import com.homi.alarmclock.model.MusicSong
import com.homi.alarmclock.util.MusicDownloadManager
import com.homi.alarmclock.util.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 音乐搜索页面 — 搜索在线流行音乐, 选择后下载并设为铃音
 */
class MusicSearchActivity : AppCompatActivity() {

    companion object {
        const val RESULT_RINGTONE_URI = "result_ringtone_uri"
        const val RESULT_RINGTONE_NAME = "result_ringtone_name"
        private const val TAG = "MusicSearch"
    }

    private lateinit var binding: ActivityMusicSearchBinding
    private lateinit var adapter: MusicSearchAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.topAppBar.setNavigationOnClickListener { finish() }

        adapter = MusicSearchAdapter { song ->
            downloadAndSelect(song)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                if (keyword.isNotEmpty()) {
                    searchJob = lifecycleScope.launch {
                        delay(500) // debounce
                        searchMusic(keyword)
                    }
                } else {
                    adapter.submitList(emptyList())
                }
            }
        })

        // 热门搜索推荐
        binding.chipGroupHot.setOnCheckedStateChangeListener { group, _ ->
            val chip = group.findViewById<com.google.android.material.chip.Chip>(group.checkedChipId)
            binding.editSearch.setText(chip.text)
        }
    }

    private fun searchMusic(keyword: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.musicApi.searchMusic(keyword)
                val songs = response.result?.songs ?: emptyList()

                binding.progressBar.visibility = View.GONE

                if (songs.isEmpty()) {
                    binding.emptyView.visibility = View.VISIBLE
                    binding.textEmpty.text = "没有找到「$keyword」相关音乐"
                } else {
                    binding.emptyView.visibility = View.GONE
                    adapter.submitList(songs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索失败", e)
                binding.progressBar.visibility = View.GONE
                binding.emptyView.visibility = View.VISIBLE
                binding.textEmpty.text = "搜索失败: ${e.message}\n请检查网络连接"
            }
        }
    }

    private fun downloadAndSelect(song: MusicSong) {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBarText.visibility = View.VISIBLE
        binding.progressBarText.text = "正在获取「${song.name}」的播放地址..."

        lifecycleScope.launch {
            try {
                // 获取播放URL
                val detail = RetrofitClient.musicApi.getSongUrl(song.id)
                val url = detail.data?.url

                if (url.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        ToastX("无法获取播放地址 (可能是 VIP 歌曲)")
                    }
                    return@launch
                }

                binding.progressBarText.text = "正在下载「${song.name}」..."

                // 下载
                val fileName = "${song.name}_${song.getArtistNames()}.mp3"
                val file = withContext(Dispatchers.IO) {
                    MusicDownloadManager.download(url, fileName, this@MusicSearchActivity)
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.progressBarText.visibility = View.GONE

                    if (file != null) {
                        // 返回结果
                        val resultIntent = Intent().apply {
                            putExtra(RESULT_RINGTONE_URI, file.absolutePath)
                            putExtra(RESULT_RINGTONE_NAME, "${song.name} - ${song.getArtistNames()}")
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        ToastX("已设为铃音: ${song.name}")
                        finish()
                    } else {
                        ToastX("下载失败, 请重试")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载失败", e)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.progressBarText.visibility = View.GONE
                    ToastX("下载失败: ${e.message}")
                }
            }
        }
    }

    private fun ToastX(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * 音乐搜索结果适配器
 */
class MusicSearchAdapter(
    private val onClick: (MusicSong) -> Unit
) : androidx.recyclerview.widget.ListAdapter<MusicSong, MusicSearchAdapter.MusicViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<MusicSong>() {
        override fun areItemsTheSame(a: MusicSong, b: MusicSong) = a.id == b.id
        override fun areContentsTheSame(a: MusicSong, b: MusicSong) = a == b
    }
) {

    inner class MusicViewHolder(val binding: ItemMusicSearchBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val song = getItem(position)
        with(holder.binding) {
            textSongName.text = song.name
            textArtist.text = song.getArtistNames()
            textDuration.text = song.getDurationFormatted()
            textAlbum.text = song.album?.name ?: ""

            // VIP 标记
            textVipTag.visibility = if (song.fee == 1) View.VISIBLE else View.GONE

            // 专辑封面
            song.album?.picUrl?.let { url ->
                Glide.with(imageCover)
                    .load(url)
                    .placeholder(R.drawable.ic_music_note)
                    .into(imageCover)
            } ?: run {
                imageCover.setImageResource(R.drawable.ic_music_note)
            }

            root.setOnClickListener { onClick(song) }
        }
    }
}