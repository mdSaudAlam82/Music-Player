package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    fun downloadSong(song: Song): Flow<Resource<Int>> =
        repository.downloadSong(song)

    fun getDownloadedSongs(): Flow<List<Song>> =
        repository.getDownloadedSongs()

    suspend fun isSongDownloaded(songId: String): Boolean =
        repository.isSongDownloaded(songId)

    suspend fun deleteSong(songId: String) =
        repository.deleteDownloadedSong(songId)
}