package com.example.musicplayer.presentation.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.usecase.SearchSongsUseCase
import com.example.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = SearchUiState()
        )

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(500L)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query -> performSearch(query) }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.value.let { current ->
            _uiState.value = current.copy(query = query)
        }
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                songs = emptyList(),
                isEmpty = false,
                error = null
            )
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            searchSongsUseCase(query).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.value =
                        _uiState.value.copy(isLoading = true, error = null)
                    is Resource.Success -> {
                        val songs = result.data?.songs ?: emptyList()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            songs = songs,
                            isEmpty = songs.isEmpty()
                        )
                    }
                    is Resource.Error -> _uiState.value =
                        _uiState.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun playSong(index: Int) {
        val songs = _uiState.value.songs
        if (index < songs.size) {
            playerController.playSong(songs[index], songs)
        }
    }
}