package com.example.musicplayer.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.musicplayer.data.local.entity.RecentlyPlayedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyPlayedDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentlyPlayed(song: RecentlyPlayedEntity)

    // Latest 30 songs, time ke hisaab se
    @Query("SELECT * FROM recently_played ORDER BY playedAt DESC LIMIT 30")
    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedEntity>>

    @Query("DELETE FROM recently_played")
    suspend fun clearAll()

    // 30 se zyada hone pe purana delete karo
    @Query("""
        DELETE FROM recently_played 
        WHERE songId NOT IN (
            SELECT songId FROM recently_played 
            ORDER BY playedAt DESC 
            LIMIT 30
        )
    """)
    suspend fun trimOldEntries()
}