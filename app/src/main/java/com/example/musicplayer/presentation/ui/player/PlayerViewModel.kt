package com.example.musicplayer.presentation.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.PlaybackMode
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.repository.LocalMusicRepository
import com.example.musicplayer.domain.usecase.GetLyricsUseCase
import com.example.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getLyricsUseCase: GetLyricsUseCase,
    val playerController: PlayerController,
    private val localMusicRepository: LocalMusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerController.playerState.collect { playerState ->
                val isNewSong = playerState.currentSong?.id != _uiState.value.currentSong?.id

                // 👇 NAYA FIX: Naya gaana bajne par check karo (aur purana overwrite mat karo)
                if (isNewSong && playerState.currentSong != null) {
                    viewModelScope.launch {
                        localMusicRepository.isSongLiked(playerState.currentSong.id).collect { liked ->
                            _uiState.update { it.copy(isLiked = liked) }
                        }
                    }
                }

                val parsed = _uiState.value.parsedLyrics
                val pos = playerState.currentPosition
                val newIndex = if (parsed.isNotEmpty()) {
                    val idx = parsed.indexOfLast { it.timeMs <= pos }
                    if (idx != -1) idx else 0
                } else 0

                _uiState.update {
                    it.copy(
                        currentSong = playerState.currentSong,
                        isPlaying = playerState.isPlaying,
                        currentPosition = pos,
                        duration = playerState.duration,
                        playbackMode = playerState.playbackMode,
                        lyrics = if (isNewSong) null else it.lyrics,
                        parsedLyrics = if (isNewSong) emptyList() else it.parsedLyrics,
                        lyricsError = if (isNewSong) null else it.lyricsError,
                        showLyrics = if (isNewSong) false else it.showLyrics,
                        isLyricsLoading = if (isNewSong) false else it.isLyricsLoading,
                        currentLyricIndex = if (isNewSong) 0 else newIndex,
                        sleepTimerRemaining = playerState.sleepTimerRemaining,
                        isBuffering = playerState.isBuffering,
                        isWaitingForFocus = playerState.isWaitingForFocus,
                        // 👇 NAYA FIX: Ye logic miss ho rahi thi UI State update ke main block mein
                        isLiked = if (isNewSong) false else it.isLiked
                    )
                }
            }
        }
    }

    fun playPause() = playerController.pauseResume()
    fun next() = playerController.next()
    fun setSleepTimer(minutes: Int) = playerController.setSleepTimer(minutes)
    fun previous() = playerController.previous()
    fun seekTo(position: Long) = playerController.seekTo(position)

    fun toggleLike() {
        val currentSong = _uiState.value.currentSong ?: return
        viewModelScope.launch {
            localMusicRepository.toggleLikeSong(currentSong)
        }
    }

    fun togglePlaybackMode() {
        val currentMode = _uiState.value.playbackMode
        val nextMode = when (currentMode) {
            PlaybackMode.NONE -> PlaybackMode.REPEAT_ALL
            PlaybackMode.REPEAT_ALL -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.NONE
        }
        playerController.setPlaybackMode(nextMode)
    }

    fun toggleLyrics() {
        val currentSong = _uiState.value.currentSong ?: return
        if (_uiState.value.isLyricsLoading) return
        if (_uiState.value.showLyrics) {
            _uiState.update { it.copy(showLyrics = false) }
            return
        }
        if (_uiState.value.lyrics == null) {
            loadLyrics(currentSong.title, currentSong.artist ?: "")
        }
        _uiState.update { it.copy(showLyrics = true) }
    }

    private fun loadLyrics(title: String, artist: String) {
        viewModelScope.launch {
            getLyricsUseCase(title, artist).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(isLyricsLoading = true, lyricsError = null)
                    }
                    is Resource.Success -> {
                        val parsed = parseLrc(result.data ?: "")
                        _uiState.update {
                            it.copy(
                                isLyricsLoading = false,
                                lyrics = result.data,
                                parsedLyrics = parsed,
                                lyricsError = null
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLyricsLoading = false, lyricsError = result.message)
                    }
                }
            }
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        if (lrc.isBlank()) return emptyList()
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")
        lrc.lines().forEach { line ->
            val match = regex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toLong()
                val msPart = match.groupValues[3]
                val ms = if (msPart.length == 2) msPart.toLong() * 10 else msPart.toLong()
                val text = match.groupValues[4].trim()
                val totalMs = min * 60000 + sec * 1000 + ms
                if (text.isNotEmpty()) lines.add(LyricLine(totalMs, text))
            }
        }
        if (lines.isEmpty() && lrc.isNotBlank()) {
            return lrc.lines().filter { it.isNotBlank() }.map { LyricLine(-1L, it.trim()) }
        }
        return lines
    }
}