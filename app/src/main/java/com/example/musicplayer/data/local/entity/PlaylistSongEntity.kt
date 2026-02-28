package com.example.musicplayer.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val duration: Long,
    val album: String = "",
    val albumId: String = "",
    val artistId: String = "",
    val localPath: String? = null,
    val isLocal: Boolean = false,
    val isDownloaded: Boolean = false,
    val hasLyrics: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis() // ✅ Naam 'cachedAt' rakhein
)