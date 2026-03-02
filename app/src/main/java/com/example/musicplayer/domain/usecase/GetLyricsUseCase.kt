package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLyricsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    // 👇 ID ki jagah Title, Artist
    suspend operator fun invoke(title: String, artist: String): Flow<Resource<String>> {
        return repository.getLyrics(title, artist)
    }
}