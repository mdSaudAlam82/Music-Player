package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.MediaStoreHelper
import com.example.musicplayer.data.local.db.RecentlyPlayedDao
import com.example.musicplayer.data.local.entity.RecentlyPlayedEntity
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.LocalMusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMusicRepositoryImpl @Inject constructor(
    private val mediaStoreHelper: MediaStoreHelper,
    private val recentlyPlayedDao: RecentlyPlayedDao
) : LocalMusicRepository {

    override fun getLocalSongs(): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        try {
            val songs = mediaStoreHelper.scanLocalSongs()
            emit(Resource.Success(songs))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error"))
        }
    }

    override fun getRecentlyPlayed(): Flow<List<Song>> {
        return recentlyPlayedDao.getRecentlyPlayed().map { list ->
            list.map { entity ->
                Song(
                    id = entity.songId,
                    title = entity.title,
                    artist = entity.artist,
                    artistId = entity.artistId,
                    album = entity.album,
                    albumId = entity.albumId,
                    duration = entity.duration,
                    artworkUrl = entity.artworkUrl,
                    streamUrl = entity.streamUrl,
                    localPath = entity.localPath,
                    isLocal = entity.isLocal,
                    isDownloaded = entity.isDownloaded,
                    hasLyrics = entity.hasLyrics
                )
            }
        }
    }

    override suspend fun addToRecentlyPlayed(song: Song) {
        val entity = RecentlyPlayedEntity(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            artistId = song.artistId,
            album = song.album,
            albumId = song.albumId,
            duration = song.duration,
            artworkUrl = song.artworkUrl,
            streamUrl = song.streamUrl,
            localPath = song.localPath,
            isLocal = song.isLocal,
            isDownloaded = song.isDownloaded,
            hasLyrics = song.hasLyrics,
            playedAt = System.currentTimeMillis()
        )
        recentlyPlayedDao.insertRecentlyPlayed(entity)
    }

    override suspend fun clearRecentlyPlayed() {
        recentlyPlayedDao.clearAll()
    }
}