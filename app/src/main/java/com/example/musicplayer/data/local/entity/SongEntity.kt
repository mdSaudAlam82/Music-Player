package com.example.musicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val artworkUrl: String,
    // streamUrl yahan NAHI bachayenge — expire ho jaata hai!
    val localPath: String? = null,
    val isLocal: Boolean = false,
    val isDownloaded: Boolean = false,
    val isTrending: Boolean = false,
    val hasLyrics: Boolean = false,
    // Timestamp — kab cache hua
    val cachedAt: Long = System.currentTimeMillis()
)