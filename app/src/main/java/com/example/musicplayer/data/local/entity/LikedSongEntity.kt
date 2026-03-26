package com.example.musicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Ye theek hai mostly — sirf ek change: albumId aur artistId remove,
// ye kabhi use nahi hote liked songs display mein
@Entity(tableName = "liked_songs")
data class LikedSongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String?,
    val artworkUrl: String?,
    val streamUrl: String,
    val duration: Long,
    val localPath: String? = null,
    val isLocal: Boolean,
    val isDownloaded: Boolean = false,
    val hasLyrics: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
    // REMOVED: albumId, artistId — ye liked songs list mein kabhi dikhte nahi the
)