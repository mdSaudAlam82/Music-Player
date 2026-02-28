package com.example.musicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey
    val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val streamUrl: String,
    val duration: Long,
    val album: String = "",
    val albumId: String = "",
    val artistId: String = "",
    val localPath: String? = null,
    val isLocal: Boolean = false,
    val isDownloaded: Boolean = false,
    val language: String = "",
    val year: String = "",
    val hasLyrics: Boolean = false,
    val lyricsId: String? = null,
    val playCount: Long = 0,
    val isExplicit: Boolean = false,
    val playedAt: Long = System.currentTimeMillis()
)