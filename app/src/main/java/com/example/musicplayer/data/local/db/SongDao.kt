package com.example.musicplayer.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musicplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity)

    @Query("SELECT * FROM songs WHERE isTrending = 1 LIMIT 20")
    fun getTrendingSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isDownloaded = 1")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Query("UPDATE songs SET isDownloaded = 1, localPath = :path WHERE id = :songId")
    suspend fun markAsDownloaded(songId: String, path: String)

    @Query("SELECT EXISTS(SELECT 1 FROM songs WHERE id = :songId AND isDownloaded = 1)")
    suspend fun isSongDownloaded(songId: String): Boolean

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: String): SongEntity?

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSong(songId: String)
}