package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSongByIdUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(id: String): Flow<Resource<Song>> {
        return repository.getSongById(id)
    }
}