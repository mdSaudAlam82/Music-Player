package com.example.musicplayer.presentation.ui.player

import com.example.musicplayer.domain.model.PlaybackMode
import com.example.musicplayer.domain.model.Song

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.NONE,
    val isLyricsLoading: Boolean = false,
    val lyrics: String? = null,
    val lyricsError: String? = null,
    val showLyrics: Boolean = false
)