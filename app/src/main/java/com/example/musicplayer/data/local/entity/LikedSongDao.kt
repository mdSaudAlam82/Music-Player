package com.example.musicplayer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musicplayer.data.local.entity.LikedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LikedSongDao {

    // Saare liked songs laane ke liye (Naye wale sabse upar)
    @Query("SELECT * FROM liked_songs ORDER BY addedAt DESC")
    fun getAllLikedSongs(): Flow<List<LikedSongEntity>>

    // Check karne ke liye ki gaana already liked hai ya nahi (Heart ko lal karne ke kaam aayega)
    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE id = :songId)")
    fun isSongLiked(songId: String): Flow<Boolean>

    // Gaana Like karne ke liye
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikedSong(song: LikedSongEntity)

    // Gaana Unlike (Delete) karne ke liye
    @Query("DELETE FROM liked_songs WHERE id = :songId")
    suspend fun deleteLikedSong(songId: String)
}