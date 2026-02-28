package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.SearchResult
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {

    // Search
    suspend fun searchSongs(
        query: String,
        page: Int = 0,
        limit: Int = 20
    ): Flow<Resource<SearchResult>>

    // Song detail — stream URL lene ke liye
    suspend fun getSongById(id: String): Flow<Resource<Song>>

    // Album detail
    suspend fun getAlbumById(id: String): Flow<Resource<Album>>

    // Lyrics
    suspend fun getLyrics(songId: String): Flow<Resource<String>>
}