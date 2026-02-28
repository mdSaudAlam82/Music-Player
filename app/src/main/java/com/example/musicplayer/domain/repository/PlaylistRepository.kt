package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {

    // Saari playlists lao
    fun getAllPlaylists(): Flow<List<Playlist>>

    // Ek playlist detail
    fun getPlaylistById(id: Long): Flow<Playlist?>

    // Playlist banao
    suspend fun createPlaylist(name: String): Long

    // Playlist delete karo
    suspend fun deletePlaylist(playlistId: Long)

    // Playlist ka naam badlo
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    // Song add karo playlist me
    suspend fun addSongToPlaylist(playlistId: Long, song: Song)

    // Song remove karo playlist se
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)
}