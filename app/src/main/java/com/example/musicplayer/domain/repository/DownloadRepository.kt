package com.example.musicplayer.domain.repository

import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {

    // Song download karo
    fun downloadSong(song: Song): Flow<Resource<Int>> // Int = progress 0-100

    // Downloaded songs lao
    fun getDownloadedSongs(): Flow<List<Song>>

    // Song downloaded hai ya nahi check karo
    suspend fun isSongDownloaded(songId: String): Boolean

    // Downloaded song delete karo
    suspend fun deleteDownloadedSong(songId: String)
}