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

    val recentlyPlayed = getRecentlyPlayedUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // 👇 NAYA: Alag-alag moods aur categories ki list
    private val trendingQueries = listOf(
        "trending hindi",
        "latest bollywood",
        "top 50 hindi",
        "viral songs",
        "lofi chill hindi",
        "punjabi hits",
        "new romantic hindi",
        "arijit singh hits"
    )

    init {
        loadTrendingSongs()
    }

    fun retry() {
        loadTrendingSongs()
    }

    fun loadTrendingSongs() {
        // 👇 NAYA: Har baar ek random category uthayega
        val randomQuery = trendingQueries.random()

        viewModelScope.launch {
            searchSongsUseCase(randomQuery, limit = 20).collect { result ->
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