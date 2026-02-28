package com.example.musicplayer.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.usecase.GetRecentlyPlayedUseCase
import com.example.musicplayer.domain.usecase.SearchSongsUseCase
import com.example.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val getRecentlyPlayedUseCase: GetRecentlyPlayedUseCase,
    val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Recently played — stateIn use karo
    // Ye sirf tab collect karega jab UI observe kar raha ho
    val recentlyPlayed = getRecentlyPlayedUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        loadTrendingSongs()
    }

    // Retry button ke liye public function
    fun retry() {
        loadTrendingSongs()
    }

    fun loadTrendingSongs() {
        viewModelScope.launch {
            searchSongsUseCase("trending hindi songs", limit = 20).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(isLoading = true, error = null)
                    }
                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            trendingSongs = result.data?.songs ?: emptyList(),
                            error = null
                        )
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }

    fun playSong(index: Int) {
        val songs = _uiState.value.trendingSongs
        if (index < songs.size) {
            playerController.playSong(songs[index], songs)
        }
    }
}