package com.example.musicplayer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LyricsResponseDto(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: LyricsDataDto?
)

data class LyricsDataDto(
    @SerializedName("lyrics") val lyrics: String?,
    @SerializedName("copyright") val copyright: String?,
    @SerializedName("snippet") val snippet: String?
)