package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface LocalMusicRepository {

    // Device scan karke saare songs lao
    fun getLocalSongs(): Flow<Resource<List<Song>>>

    // Recently played songs
    fun getRecentlyPlayed(): Flow<List<Song>>

    // Recently played me add karo
    suspend fun addToRecentlyPlayed(song: Song)

    // Recently played clear karo
    suspend fun clearRecentlyPlayed()
}