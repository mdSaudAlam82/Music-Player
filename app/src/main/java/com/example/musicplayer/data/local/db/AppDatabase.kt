package com.example.musicplayer.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.musicplayer.data.local.entity.PlaylistEntity
import com.example.musicplayer.data.local.entity.PlaylistSongEntity
import com.example.musicplayer.data.local.entity.RecentlyPlayedEntity
import com.example.musicplayer.data.local.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        RecentlyPlayedEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun playlistDao(): PlaylistDao
}