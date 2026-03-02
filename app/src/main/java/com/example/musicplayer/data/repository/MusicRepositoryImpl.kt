package com.example.musicplayer.data.repository

import com.example.musicplayer.data.local.db.SongDao
import com.example.musicplayer.data.local.toDomain
import com.example.musicplayer.data.local.toEntity
import com.example.musicplayer.data.remote.api.JioSaavnApi
import com.example.musicplayer.data.remote.toDomain
import com.example.musicplayer.domain.model.Album
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.SearchResult
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val api: JioSaavnApi,
    private val songDao: SongDao
) : MusicRepository {

    override suspend fun searchSongs(query: String, page: Int, limit: Int): Flow<Resource<SearchResult>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.searchSongs(query, page, limit)
            if (response.success && response.data != null) {
                val songs = response.data.results.map { it.toDomain() }

                // 👇 FIXED: Named arguments taaki confusion na ho
                emit(Resource.Success(SearchResult(
                    songs = songs,
                    albums = emptyList(), // Isse error 100% solve ho jayega
                    totalSongs = response.data.total
                )))
            } else {
                emit(Resource.Error("API Response Failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getSongById(id: String): Flow<Resource<Song>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getSongById(id)
            if (response.success && !response.data.isNullOrEmpty()) {
                emit(Resource.Success(response.data.first().toDomain()))
            }
        } catch (e: Exception) { emit(Resource.Error("Song fetch failed")) }
    }

    override suspend fun getAlbumById(id: String): Flow<Resource<Album>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getAlbumById(id)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            }
        } catch (e: Exception) { emit(Resource.Error("Album load failed")) }
    }

    override suspend fun getLyrics(songId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getLyrics(songId)
            if (response.success) emit(Resource.Success(response.data?.lyrics ?: ""))
        } catch (e: Exception) { emit(Resource.Error("Lyrics error")) }
    }
}