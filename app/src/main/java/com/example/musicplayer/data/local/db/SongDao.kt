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

    @Query("SELECT * FROM songs WHERE isDownloaded = 1")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?

    @Query("UPDATE songs SET isDownloaded = 1, localPath = :path WHERE id = :id")
    suspend fun markAsDownloaded(id: String, path: String)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM songs WHERE id = :id AND isDownloaded = 1)")
    suspend fun isSongDownloaded(id: String): Boolean

    // Trending songs cache
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrendingSongs(songs: List<SongEntity>)

    @Query("SELECT * FROM songs WHERE isTrending = 1 ORDER BY cachedAt DESC")
    fun getTrendingSongs(): Flow<List<SongEntity>>

    @Query("UPDATE songs SET isTrending = 1 WHERE id = :id")
    suspend fun markAsTrending(id: String)
}