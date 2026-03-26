package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface LocalMusicRepository {
    fun getLocalSongs(): Flow<Resource<List<Song>>>
    fun getRecentlyPlayed(): Flow<List<Song>>
    suspend fun addToRecentlyPlayed(song: Song)
    suspend fun clearRecentlyPlayed()

    // 👇 NAYA: Liked Songs ke naye functions
    fun getLikedSongs(): Flow<List<Song>>
    fun isSongLiked(songId: String): Flow<Boolean>
    suspend fun toggleLikeSong(song: Song)
}