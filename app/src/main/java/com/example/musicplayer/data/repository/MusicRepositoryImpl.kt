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
                emit(Resource.Success(SearchResult(
                    songs = songs,
                    albums = emptyList(),
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

    override suspend fun getLyrics(title: String, artist: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val cleanTitle = title.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()
            val cleanArtist = artist.split(",").first().trim()

            val response = api.getLrcLibLyrics(trackName = cleanTitle, artistName = cleanArtist)

            val finalLyrics = response.syncedLyrics ?: response.plainLyrics

            if (!finalLyrics.isNullOrBlank()) {
                emit(Resource.Success(finalLyrics))
            } else {
                emit(Resource.Error("Is gaane ke lyrics LRCLIB par nahi mile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Lyrics nahi mile (LRCLIB)"))
        }
    }.flowOn(Dispatchers.IO)

    // 👇 NAYA LOGIC: Similar Songs (Endless Playback ke liye) 👇
    override suspend fun getSimilarSongs(song: Song): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        try {
            // Hum gaane ke artist se naye gaane dhoondhenge taaki vibe match kare
            val query = song.artist?.split(",")?.first()?.trim() ?: song.title
            val response = api.searchSongs(query, page = 0, limit = 15)
            val data = response.data

            if (response.success && data != null) {
                val similarSongs = data.results
                    .map { it.toDomain() }
                    .filter { it.id != song.id } // Purana gaana dobara na aaye

                emit(Resource.Success(similarSongs))
            } else {
                emit(Resource.Error("Naye gaane nahi mile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Network error"))
        }
    }.flowOn(Dispatchers.IO)
}