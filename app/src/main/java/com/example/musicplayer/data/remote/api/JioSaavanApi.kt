package com.example.musicplayer.data.remote.api

import com.example.musicplayer.data.remote.dto.AlbumDto
import com.example.musicplayer.data.remote.dto.LyricsResponseDto
import com.example.musicplayer.data.remote.dto.SearchResponseDto
import com.example.musicplayer.data.remote.dto.SongResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JioSaavnApi {

    // Songs search
    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20
    ): SearchResponseDto

    // Song by ID
    @GET("songs/{id}")
    suspend fun getSongById(
        @Path("id") id: String
    ): SongResponseDto

    // Album by ID
    @GET("albums")
    suspend fun getAlbumById(
        @Query("id") id: String
    ): AlbumDto

    // Lyrics
    @GET("lyrics")
    suspend fun getLyrics(
        @Query("id") songId: String
    ): LyricsResponseDto
}