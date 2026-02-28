package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManagePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistRepository.getAllPlaylists()

    fun getPlaylistById(id: Long): Flow<Playlist?> =
        playlistRepository.getPlaylistById(id)

    suspend fun createPlaylist(name: String): Long =
        playlistRepository.createPlaylist(name)

    suspend fun deletePlaylist(id: Long) =
        playlistRepository.deletePlaylist(id)

    suspend fun renamePlaylist(id: Long, name: String) =
        playlistRepository.renamePlaylist(id, name)

    // addSong → addSongToPlaylist
    suspend fun addSong(playlistId: Long, song: Song) =
        playlistRepository.addSongToPlaylist(playlistId, song)

    // removeSong → removeSongFromPlaylist
    suspend fun removeSong(playlistId: Long, songId: String) =
        playlistRepository.removeSongFromPlaylist(playlistId, songId)
}