package com.example.musicplayer.domain.model

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val role: String = "",
    val url: String = ""
)