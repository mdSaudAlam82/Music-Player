package com.example.musicplayer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SearchResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SearchDataDto?
)

data class SearchDataDto(
    @SerializedName("total") val total: Int,
    @SerializedName("start") val start: Int,
    @SerializedName("results") val results: List<SongDto>
)

data class SongResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<SongDto>?
)

data class SongDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("duration") val duration: Int?,
    @SerializedName("label") val label: String?,
    @SerializedName("explicitContent") val explicitContent: Boolean?,
    @SerializedName("playCount") val playCount: Long?,
    @SerializedName("language") val language: String?,
    @SerializedName("hasLyrics") val hasLyrics: Boolean?,
    @SerializedName("lyricsId") val lyricsId: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("copyright") val copyright: String?,
    @SerializedName("album") val album: AlbumRefDto?,
    @SerializedName("artists") val artists: ArtistsDto?,
    @SerializedName("image") val image: List<ImageDto>?,
    @SerializedName("downloadUrl") val downloadUrl: List<DownloadUrlDto>?
)

data class AlbumRefDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("url") val url: String?
)

data class ArtistsDto(
    @SerializedName("primary") val primary: List<ArtistDto>?,
    @SerializedName("featured") val featured: List<ArtistDto>?,
    @SerializedName("all") val all: List<ArtistDto>?
)

data class ArtistDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("image") val image: List<ImageDto>?,
    @SerializedName("type") val type: String?,
    @SerializedName("url") val url: String?
)

data class ImageDto(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url") val url: String?
)

data class DownloadUrlDto(
    @SerializedName("quality") val quality: String?,
    @SerializedName("url") val url: String?
)