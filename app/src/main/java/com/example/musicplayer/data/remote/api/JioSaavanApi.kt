package com.example.musicplayer.data.remote.api

import com.example.musicplayer.data.remote.dto.AlbumDto
import com.example.musicplayer.data.remote.dto.LyricsResponseDto
import com.example.musicplayer.data.remote.dto.SearchResponseDto
import com.example.musicplayer.data.remote.dto.SongResponseDto
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// 👇 NAYA: LRCLIB se aane wale data ka model
data class LrcLibResponseDto(
    @SerializedName("plainLyrics") val plainLyrics: String?,
    @SerializedName("syncedLyrics") val syncedLyrics: String?
)

interface JioSaavnApi {

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int = 0,
        @Query("limit") limit: Int = 20
    ): SearchResponseDto

    @GET("songs/{id}")
    suspend fun getSongById(
        @Path("id") id: String
    ): SongResponseDto

    @GET("albums")
    suspend fun getAlbumById(
        @Query("id") id: String
    ): AlbumDto

    // 👇 NAYA: LRCLIB ka Direct Endpoint (Bina base URL change kiye)
    @GET("https://lrclib.net/api/get")
    suspend fun getLrcLibLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): LrcLibResponseDto
}