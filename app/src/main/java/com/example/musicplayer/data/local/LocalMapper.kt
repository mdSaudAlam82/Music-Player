package com.example.musicplayer.data.local

import com.example.musicplayer.data.local.entity.SongEntity
import com.example.musicplayer.domain.model.Song

fun SongEntity.toDomain(): Song {
    return Song(
        id = id,
        title = title,
        artist = artist,
        artistId = "",
        album = album,
        albumId = "",
        duration = duration,
        artworkUrl = artworkUrl,
        streamUrl = "",
        localPath = localPath,
        isLocal = isLocal,
        isDownloaded = isDownloaded,
        hasLyrics = hasLyrics
    )
}

fun Song.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        artworkUrl = artworkUrl,
        localPath = localPath,
        isLocal = isLocal,
        isDownloaded = isDownloaded,
        hasLyrics = hasLyrics,
        cachedAt = System.currentTimeMillis()
    )
}