package com.example.musicplayer.presentation.ui.player

import com.example.musicplayer.domain.model.PlaybackMode
import com.example.musicplayer.domain.model.Song

data class LyricLine(val timeMs: Long, val text: String)

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.NONE,
    val isLyricsLoading: Boolean = false,
    val lyrics: String? = null,
    val parsedLyrics: List<LyricLine> = emptyList(),
    val currentLyricIndex: Int = 0,
    val lyricsError: String? = null,
    val showLyrics: Boolean = false,
    val sleepTimerRemaining: Long? = null,
    val isLiked: Boolean = false,
    val isBuffering: Boolean = false,
    val isWaitingForFocus: Boolean = false
)