package com.example.musicplayer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// CHANGE: Sirf songId aur playedAt rakhenge + woh fields jo
// Song object se milte nahi (streamUrl expire hota hai, isliye rakhna zaruri hai)
// Baaki sab (title, artist, artworkUrl) SongEntity ya RecentlyPlayedEntity
// ke paas directly rakhte hain taaki join na karna pade
// (simple app ke liye ye tradeoff theek hai — full join se avoid karte hain)
@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey
    val songId: String,
    // Ye fields rakhne zaruri hain kyunki local songs SongEntity mein nahi hote
    // aur online songs ka streamUrl expire hota hai (fetch on play karte hain)
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val streamUrl: String,
    val duration: Long,
    val localPath: String? = null,
    val isLocal: Boolean = false,
    val isDownloaded: Boolean = false,
    val hasLyrics: Boolean = false,
    val playedAt: Long = System.currentTimeMillis()
    // REMOVED: album, albumId, artistId, language, year, playCount,
    //          isExplicit, lyricsId — ye kabhi use nahi hote the
)