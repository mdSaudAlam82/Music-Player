package com.example.musicplayer.domain.usecase

import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.SearchResult
import com.example.musicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchSongsUseCase @Inject constructor(
    private val repository: MusicRepository
) {
    suspend operator fun invoke(
        query: String,
        page: Int = 0,
        limit: Int = 20
    ): Flow<Resource<SearchResult>> {
        return repository.searchSongs(query, page, limit)
    }
}