package com.example.musicplayer.domain.model

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackMode: PlaybackMode = PlaybackMode.NONE,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false  // ← Ye add karo
)

enum class PlaybackMode {
    NONE,       // normal
    REPEAT_ONE, // ek song repeat
    REPEAT_ALL, // sab repeat
    SHUFFLE     // random
}