package com.example.musicplayer.presentation.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.LocalMusicRepository
import com.example.musicplayer.domain.usecase.GetLocalSongsUseCase
import com.example.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getLocalSongsUseCase: GetLocalSongsUseCase,
    private val localMusicRepository: LocalMusicRepository,
    val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLikedSongs()
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasPermission = true) }
        loadLocalSongs()
    }

    fun onTabSelected(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun loadLocalSongs() {
        viewModelScope.launch {
            getLocalSongsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> _uiState.update { it.copy(isLoading = false, localSongs = result.data ?: emptyList()) }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun loadLikedSongs() {
        viewModelScope.launch {
            localMusicRepository.getLikedSongs().collect { songs ->
                _uiState.update { it.copy(likedSongs = songs) }
            }
        }
    }

    fun playSong(index: Int) {
        val songs = when (_uiState.value.selectedTab) {
            LibraryTab.LOCAL -> _uiState.value.localSongs
            LibraryTab.LIKED_SONGS -> _uiState.value.likedSongs
        }
        if (index < songs.size) {
            playerController.playSong(songs[index], songs)
        }
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            localMusicRepository.toggleLikeSong(song)
        }
    }
}