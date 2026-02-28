package com.example.musicplayer.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.musicplayer.data.local.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    // 1. Pehle bina purana mitaye insert try karenge
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongIgnore(song: SongEntity): Long

    // 2. Agar gaana pehle se hai, toh sirf update karenge
    @Update
    suspend fun updateSong(song: SongEntity)

    // 👇 YE HAI SAFE INSERT (Overwrite Fix) 👇
    @Transaction
    suspend fun insertSongSafe(song: SongEntity) {
        val id = insertSongIgnore(song)
        if (id == -1L) { // Iska matlab gaana database mein pehle se hai
            val oldSong = getSongById(song.id)
            if (oldSong != null) {
                // Purane gaane ka download status bacha kar naya save karenge
                val updatedSong = song.copy(
                    isDownloaded = oldSong.isDownloaded,
                    localPath = oldSong.localPath,
                    // Agar purana trending tha, toh usko bhi bacha lenge
                    isTrending = oldSong.isTrending || song.isTrending
                )
                updateSong(updatedSong)
            }
        }
    }

    // Purane function ko safe wale se replace kar diya
    @Transaction
    suspend fun insertSong(song: SongEntity) {
        insertSongSafe(song)
    }

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

    // 👇 Trending songs ke liye bhi safe wala chalayenge 👇
    @Transaction
    suspend fun insertTrendingSongs(songs: List<SongEntity>) {
        songs.forEach { insertSongSafe(it) }
    }

    @Query("SELECT * FROM songs WHERE isTrending = 1 ORDER BY cachedAt DESC")
    fun getTrendingSongs(): Flow<List<SongEntity>>

    @Query("UPDATE songs SET isTrending = 1 WHERE id = :id")
    suspend fun markAsTrending(id: String)
}