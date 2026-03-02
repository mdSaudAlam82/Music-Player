package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.SearchResult
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {

    suspend fun searchSongs(
        query: String,
        page: Int = 0,
        limit: Int = 20
    ): Flow<Resource<SearchResult>>

    suspend fun getSongById(id: String): Flow<Resource<Song>>

    suspend fun getAlbumById(id: String): Flow<Resource<Album>>

    // 👇 Yahan Title aur Artist kiya hai
    suspend fun getLyrics(title: String, artist: String): Flow<Resource<String>>
}