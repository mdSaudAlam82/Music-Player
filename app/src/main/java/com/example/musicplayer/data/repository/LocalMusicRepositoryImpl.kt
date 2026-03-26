package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.MediaStoreHelper
import com.example.musicplayer.data.local.dao.LikedSongDao
import com.example.musicplayer.data.local.db.RecentlyPlayedDao
import com.example.musicplayer.data.local.entity.LikedSongEntity
import com.example.musicplayer.data.local.entity.RecentlyPlayedEntity
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.LocalMusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMusicRepositoryImpl @Inject constructor(
    private val mediaStoreHelper: MediaStoreHelper,
    private val recentlyPlayedDao: RecentlyPlayedDao,
    private val likedSongDao: LikedSongDao
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
            list.map { entity -> entity.toSong() }
        }
    }

    override suspend fun addToRecentlyPlayed(song: Song) {
        // Sirf zaruri fields store karo
        val entity = RecentlyPlayedEntity(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            artworkUrl = song.artworkUrl,
            streamUrl = song.streamUrl,
            duration = song.duration,
            localPath = song.localPath,
            isLocal = song.isLocal,
            isDownloaded = song.isDownloaded,
            hasLyrics = song.hasLyrics,
            playedAt = System.currentTimeMillis()
        )
        recentlyPlayedDao.insertRecentlyPlayed(entity)
        // 30 se zyada hone par purana trim karo
        recentlyPlayedDao.trimOldEntries()
    }

    override suspend fun clearRecentlyPlayed() {
        recentlyPlayedDao.clearAll()
    }

    // Liked Songs
    override fun getLikedSongs(): Flow<List<Song>> {
        return likedSongDao.getAllLikedSongs().map { list ->
            list.map { entity -> entity.toSong() }
        }
    }

    override fun isSongLiked(songId: String): Flow<Boolean> {
        return likedSongDao.isSongLiked(songId)
    }

    override suspend fun toggleLikeSong(song: Song) {
        val isLiked = likedSongDao.isSongLiked(song.id).first()
        if (isLiked) {
            likedSongDao.deleteLikedSong(song.id)
        } else {
            val entity = LikedSongEntity(
                id = song.id,
                title = song.title,
                artist = song.artist,
                artworkUrl = song.artworkUrl,
                streamUrl = song.streamUrl,
                duration = song.duration,
                localPath = song.localPath,
                isLocal = song.isLocal,
                isDownloaded = song.isDownloaded,
                hasLyrics = song.hasLyrics,
                addedAt = System.currentTimeMillis()
            )
            likedSongDao.insertLikedSong(entity)
        }
    }

    // --- Private mappers ---

    private fun RecentlyPlayedEntity.toSong() = Song(
        id = songId,
        title = title,
        artist = artist,
        album = "",          // recently played mein album dikhta nahi
        duration = duration,
        artworkUrl = artworkUrl,
        streamUrl = streamUrl,
        localPath = localPath,
        isLocal = isLocal,
        isDownloaded = isDownloaded,
        hasLyrics = hasLyrics
    )

    private fun LikedSongEntity.toSong() = Song(
        id = id,
        title = title,
        artist = artist ?: "Unknown",
        album = "Liked Songs",
        duration = duration,
        artworkUrl = artworkUrl ?: "",
        streamUrl = streamUrl,
        localPath = localPath,
        isLocal = isLocal,
        isDownloaded = isDownloaded,
        hasLyrics = hasLyrics
    )
}