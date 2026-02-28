package com.example.musicplayer.presentation.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.usecase.ManageDownloadsUseCase
import com.example.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val manageDownloadsUseCase: ManageDownloadsUseCase,
    val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        loadDownloadedSongs()
    }

    private fun loadDownloadedSongs() {
        viewModelScope.launch {
            manageDownloadsUseCase.getDownloadedSongs().collect { songs ->
                _uiState.update { it.copy(songs = songs) }
            }
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            manageDownloadsUseCase.downloadSong(song).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(
                            downloadingSongId = song.id,
                            downloadProgress = result.data ?: 0
                        )
                    }
                    is Resource.Success -> _uiState.update {
                        it.copy(
                            downloadingSongId = null,
                            downloadProgress = 0
                        )
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(
                            downloadingSongId = null,
                            downloadProgress = 0,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun deleteSong(songId: String) {
        viewModelScope.launch {
            manageDownloadsUseCase.deleteSong(songId)
        }
    }

    fun playSong(index: Int) {
        val songs = _uiState.value.songs
        if (index < songs.size) {
            playerController.playSong(songs[index], songs)
        }
    }
}