package com.example.musicplayer.domain.model

data class SearchResult(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val totalSongs: Int = 0
)