package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.db.PlaylistDao
import com.example.musicplayer.data.local.entity.PlaylistEntity
import com.example.musicplayer.data.local.entity.PlaylistSongEntity
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().flatMapLatest { entities ->
            if (entities.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val playlistFlows = entities.map { entity ->
                playlistDao.getSongsOfPlaylist(entity.id).map { songs ->
                    Playlist(
                        id = entity.id,
                        name = entity.name,
                        songs = songs.map { it.toSong() },
                        createdAt = entity.createdAt
                    )
                }
            }
            combine(playlistFlows) { it.toList() }
        }
    }

    override fun getPlaylistById(id: Long): Flow<Playlist?> {
        return playlistDao.getPlaylistById(id).flatMapLatest { entity ->
            playlistDao.getSongsOfPlaylist(id).map { songs ->
                entity?.let {
                    Playlist(
                        id = it.id, name = it.name,
                        songs = songs.map { s -> s.toSong() },
                        createdAt = it.createdAt
                    )
                }
            }
        }
    }

    private fun PlaylistSongEntity.toSong() = Song(
        id = songId, title = title, artist = artist, artistId = "",
        album = album, albumId = "", duration = duration,
        artworkUrl = artworkUrl, streamUrl = "", localPath = localPath,
        isLocal = isLocal, isDownloaded = isDownloaded, hasLyrics = false
    )

    override suspend fun createPlaylist(name: String): Long = playlistDao.insertPlaylist(PlaylistEntity(name = name))
    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deleteAllSongsOfPlaylist(playlistId)
        playlistDao.deletePlaylistById(playlistId)
    }
    override suspend fun renamePlaylist(playlistId: Long, newName: String) = playlistDao.renamePlaylist(playlistId, newName)
    override suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        playlistDao.addSongToPlaylist(PlaylistSongEntity(playlistId = playlistId, songId = song.id, title = song.title, artist = song.artist, artworkUrl = song.artworkUrl, duration = song.duration, album = song.album))
    }
    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = playlistDao.removeSongFromPlaylist(playlistId, songId)
}