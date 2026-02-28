package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.db.SongDao
import com.example.musicplayer.data.local.toDomain // Ye add kiya hai Room DB songs ke liye
import com.example.musicplayer.data.local.toEntity
import com.example.musicplayer.data.remote.api.JioSaavnApi
import com.example.musicplayer.data.remote.toDomain
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.SearchResult
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val api: JioSaavnApi,
    private val songDao: SongDao
) : MusicRepository {

    override suspend fun searchSongs(
        query: String,
        page: Int,
        limit: Int
    ): Flow<Resource<SearchResult>> = flow {
        emit(Resource.Loading())

        // Step 1: Pehle cache se emit karo — instant loading!
        val cachedSongs = songDao.getTrendingSongs().first()
        if (cachedSongs.isNotEmpty()) {
            emit(Resource.Success(
                SearchResult(
                    songs = cachedSongs.map { it.toDomain() },
                    totalSongs = cachedSongs.size
                )
            ))
        }

        // Step 2: Background me API call karo
        try {
            val response = api.searchSongs(query, page, limit)
            if (response.success && response.data != null) {
                val songs = response.data.results.map { it.toDomain() }

                // Step 3: Cache me save karo
                songs.forEach { song ->
                    songDao.insertSong(song.toEntity().copy(isTrending = true))
                }

                // Step 4: Fresh data emit karo
                emit(Resource.Success(
                    SearchResult(
                        songs = songs,
                        totalSongs = response.data.total
                    )
                ))
            } else if (cachedSongs.isEmpty()) {
                emit(Resource.Error("Kuch gadbad hui"))
            }
        } catch (e: Exception) {
            // Cache tha to error mat dikhao
            if (cachedSongs.isEmpty()) {
                emit(Resource.Error(e.localizedMessage ?: "Network error"))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getSongById(id: String): Flow<Resource<Song>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getSongById(id)
            if (response.success && !response.data.isNullOrEmpty()) {
                emit(Resource.Success(response.data.first().toDomain()))
            } else {
                emit(Resource.Error("Song nahi mila"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        }
    }

    override suspend fun getAlbumById(id: String): Flow<Resource<Album>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getAlbumById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error("Album nahi mila"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        }
    }

    override suspend fun getLyrics(songId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getLyrics(songId)
            if (response.success && response.data?.lyrics != null) {
                emit(Resource.Success(response.data.lyrics))
            } else {
                emit(Resource.Error("Lyrics nahi mile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        }
    }
}