package com.example.musicplayer.presentation.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.usecase.SearchSongsUseCase
import com.example.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var currentPage = 1
    private var isFetchingMore = false

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(600L)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query -> performSearch(query, isNewSearch = true) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.update {
                it.copy(songs = emptyList(), isEmpty = false, error = null, hasReachedEnd = false)
            }
        }
    }

    fun forceSearch() {
        val query = _uiState.value.query.trim()
        if (query.isNotBlank()) {
            performSearch(query, isNewSearch = true)
        }
    }

    private fun performSearch(query: String, isNewSearch: Boolean) {
        if (isFetchingMore) return
        if (!isNewSearch && _uiState.value.hasReachedEnd) return

        viewModelScope.launch {
            if (isNewSearch) {
                currentPage = 1
                _uiState.update { it.copy(isLoading = true, error = null, hasReachedEnd = false) }
            } else {
                isFetchingMore = true
                _uiState.update { it.copy(isPaginating = true, error = null) }
            }

            searchSongsUseCase(query, currentPage, limit = 20).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val newSongs = result.data?.songs ?: emptyList()

                        val currentList = if (isNewSearch) emptyList() else _uiState.value.songs
                        val combinedList = (currentList + newSongs).distinctBy { it.id }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isPaginating = false,
                                songs = combinedList,
                                isEmpty = combinedList.isEmpty(),
                                hasReachedEnd = newSongs.isEmpty() || newSongs.size < 20
                            )
                        }

                        if (newSongs.isNotEmpty()) {
                            currentPage++
                        }
                        isFetchingMore = false
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, isPaginating = false, error = result.message)
                        }
                        isFetchingMore = false
                    }
                    is Resource.Loading -> { }
                }
            }
        }
    }

    fun loadNextPage() {
        val query = _uiState.value.query.trim()
        if (query.isNotBlank()) {
            performSearch(query, isNewSearch = false)
        }
    }

    fun playSong(index: Int) {
        val songs = _uiState.value.songs
        if (index < songs.size) {
            playerController.playSong(songs[index], songs)
        }
    }
}