package com.example.musicplayer.domain.model

data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val artworkUrl: String,
    val songCount: Int = 0,
    val year: String = "",
    val language: String = "",
    val url: String = "",
    val songs: List<Song> = emptyList()
)