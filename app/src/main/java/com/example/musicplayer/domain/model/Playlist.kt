package com.example.musicplayer.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val artworkUrl: String? = null  // pehle song ka artwork use hoga
)