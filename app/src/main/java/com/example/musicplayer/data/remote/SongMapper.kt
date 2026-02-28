package com.example.musicplayer.data.remote

import com.example.musicplayer.data.remote.dto.AlbumDataDto
import com.example.musicplayer.data.remote.dto.SongDto
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Song

fun SongDto.toDomain(): Song {
    // Best quality image lao (500x500)
    val artwork = image
        ?.lastOrNull()
        ?.url ?: ""

    // 320kbps URL lao, nahi mila to jo bhi mila
    val streamUrl = downloadUrl
        ?.lastOrNull()
        ?.url ?: ""

    // Primary artist ka naam lao
    val artistName = artists
        ?.primary
        ?.firstOrNull()
        ?.name ?: "Unknown Artist"

    val artistId = artists
        ?.primary
        ?.firstOrNull()
        ?.id ?: ""

    return Song(
        id = id,
        title = name,
        artist = artistName,
        artistId = artistId,
        album = album?.name ?: "Unknown Album",
        albumId = album?.id ?: "",
        duration = ((duration ?: 0) * 1000).toLong(), // seconds to ms
        artworkUrl = artwork,
        streamUrl = streamUrl,
        hasLyrics = hasLyrics ?: false,
        isLocal = false
    )
}

fun AlbumDataDto.toDomain(): Album {
    val artwork = image?.lastOrNull()?.url ?: ""
    val artistName = artists?.primary?.firstOrNull()?.name ?: "Unknown Artist"

    return Album(
        id = id ?: "",
        name = name ?: "",
        artist = artistName,
        artworkUrl = artwork,
        songCount = songs?.size ?: 0,
        year = year ?: "",
        language = language ?: "",
        url = url ?: "",
        songs = songs?.map { it.toDomain() } ?: emptyList()
    )
}