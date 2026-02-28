package com.example.musicplayer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AlbumDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: AlbumDataDto?
)

data class AlbumDataDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("language") val language: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("image") val image: List<ImageDto>?,
    @SerializedName("artists") val artists: ArtistsDto?,
    @SerializedName("songs") val songs: List<SongDto>?
)