package com.example.musicplayer.domain.model

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,        // 👈 NAYA: Gaana load ho raha hai?
    val isWaitingForFocus: Boolean = false,  // 👈 NAYA: Call ya focus ka intezar hai?
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.NONE,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val sleepTimerRemaining: Long? = null
)

enum class PlaybackMode {
    NONE, REPEAT_ONE, REPEAT_ALL, SHUFFLE
}