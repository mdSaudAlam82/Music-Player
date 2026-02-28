package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.db.PlaylistDao
import com.example.musicplayer.data.local.entity.PlaylistEntity
import com.example.musicplayer.data.local.entity.PlaylistSongEntity
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                Playlist(id = entity.id, name = entity.name, songs = emptyList(), createdAt = entity.createdAt)
            }
        }
    }

    override fun getPlaylistById(id: Long): Flow<Playlist?> {
        return playlistDao.getPlaylistById(id).flatMapLatest { playlistEntity ->
            playlistDao.getSongsOfPlaylist(id).map { songEntities ->
                playlistEntity?.let {
                    Playlist(
                        id = it.id, name = it.name,
                        songs = songEntities.map { songEntity ->
                            Song(id = songEntity.songId, title = songEntity.title, artist = songEntity.artist, artistId = "", album = songEntity.album, albumId = "", duration = songEntity.duration, artworkUrl = songEntity.artworkUrl, streamUrl = "", localPath = null, isLocal = false, isDownloaded = false, hasLyrics = false)
                        },
                        createdAt = it.createdAt
                    )
                }
            }
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deleteAllSongsOfPlaylist(playlistId)
        playlistDao.deletePlaylistById(playlistId) // ✅ Naya method call kiya
    }

    override suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName)
    }

    override suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        playlistDao.addSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId, songId = song.id, title = song.title, artist = song.artist,
                artworkUrl = song.artworkUrl, duration = song.duration, album = song.album,
                cachedAt = System.currentTimeMillis() // ✅ cachedAt use kiya
            )
        )
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }
}