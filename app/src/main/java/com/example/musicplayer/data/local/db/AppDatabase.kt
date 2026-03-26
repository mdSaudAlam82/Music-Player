package com.example.musicplayer.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.musicplayer.data.local.dao.LikedSongDao
import com.example.musicplayer.data.local.entity.LikedSongEntity
import com.example.musicplayer.data.local.entity.PlaylistEntity
import com.example.musicplayer.data.local.entity.PlaylistSongEntity
import com.example.musicplayer.data.local.entity.RecentlyPlayedEntity
import com.example.musicplayer.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        RecentlyPlayedEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        LikedSongEntity::class
    ],
    version = 5, // 4 se 5 — RecentlyPlayed aur LikedSong cleanup
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun likedSongDao(): LikedSongDao
}