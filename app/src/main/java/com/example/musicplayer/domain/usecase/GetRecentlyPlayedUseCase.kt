package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.LocalMusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentlyPlayedUseCase @Inject constructor(
    private val repository: LocalMusicRepository
) {
    operator fun invoke(): Flow<List<Song>> {
        return repository.getRecentlyPlayed()
    }
}