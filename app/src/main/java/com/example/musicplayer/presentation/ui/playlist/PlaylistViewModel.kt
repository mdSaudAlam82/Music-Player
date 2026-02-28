package com.example.musicplayer.presentation.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.usecase.ManagePlaylistUseCase
import com.example.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val managePlaylistUseCase: ManagePlaylistUseCase,
    val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private val _detailUiState = MutableStateFlow(PlaylistDetailUiState())
    val detailUiState: StateFlow<PlaylistDetailUiState> = _detailUiState.asStateFlow()

    init {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            managePlaylistUseCase.getAllPlaylists().collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun loadPlaylistDetail(id: Long) {
        viewModelScope.launch {
            managePlaylistUseCase.getPlaylistById(id).collect { playlist ->
                _detailUiState.update { it.copy(playlist = playlist) }
            }
        }
    }

    // Create Dialog
    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, newPlaylistName = "") }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, newPlaylistName = "") }
    }

    fun onPlaylistNameChange(name: String) {
        _uiState.update { it.copy(newPlaylistName = name) }
    }

    fun createPlaylist() {
        val name = _uiState.value.newPlaylistName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            managePlaylistUseCase.createPlaylist(name)
            _uiState.update { it.copy(showCreateDialog = false, newPlaylistName = "") }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            managePlaylistUseCase.deletePlaylist(id)
        }
    }

    // Rename Dialog
    fun showRenameDialog() {
        val currentName = _detailUiState.value.playlist?.name ?: ""
        _detailUiState.update {
            it.copy(showRenameDialog = true, newName = currentName)
        }
    }

    fun hideRenameDialog() {
        _detailUiState.update { it.copy(showRenameDialog = false) }
    }

    fun onRenameChange(name: String) {
        _detailUiState.update { it.copy(newName = name) }
    }

    fun renamePlaylist() {
        val id = _detailUiState.value.playlist?.id ?: return
        val name = _detailUiState.value.newName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            managePlaylistUseCase.renamePlaylist(id, name)
            _detailUiState.update { it.copy(showRenameDialog = false) }
        }
    }

    fun removeSongFromPlaylist(songId: String) {
        val playlistId = _detailUiState.value.playlist?.id ?: return
        viewModelScope.launch {
            managePlaylistUseCase.removeSong(playlistId, songId)
        }
    }

    fun playSong(index: Int) {
        val songs = _detailUiState.value.playlist?.songs ?: return
        if (index < songs.size) {
            playerController.playSong(songs[index], songs)
        }
    }
}