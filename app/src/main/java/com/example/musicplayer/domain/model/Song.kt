package com.example.musicplayer.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String,
    val albumId: String = "",
    val duration: Long,
    val artworkUrl: String,
    val streamUrl: String,
    val localPath: String? = null,
    val isLocal: Boolean = false,
    val isDownloaded: Boolean = false,
    val hasLyrics: Boolean = false
)