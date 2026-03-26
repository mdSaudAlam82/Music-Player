package com.example.musicplayer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.PlayerState
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.usecase.ManageDownloadsUseCase
import com.example.musicplayer.domain.usecase.ManagePlaylistUseCase
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
class SharedViewModel @Inject constructor(
    private val managePlaylistUseCase: ManagePlaylistUseCase,
    private val manageDownloadsUseCase: ManageDownloadsUseCase,
    // PlayerController yahan inject karo — ab HomeViewModel ki zarurat nahi MainActivity mein
    val playerController: PlayerController
) : ViewModel() {

    // --- Player State (ab yahan se milega, HomeViewModel se nahi) ---
    // stateIn use karo taaki activity rotate hone par bhi state bani rahe
    val playerState: StateFlow<PlayerState> = playerController.playerState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Player hamesha active hai
            initialValue = PlayerState()
        )

    // --- Playlist State ---
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _showAddToPlaylist = MutableStateFlow<Song?>(null)
    val showAddToPlaylist: StateFlow<Song?> = _showAddToPlaylist.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // --- Download State ---
    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds: StateFlow<Set<String>> = _downloadingIds.asStateFlow()

    init {
        loadPlaylists()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            managePlaylistUseCase.getAllPlaylists().collect { playlists ->
                _playlists.update { playlists }
            }
        }
    }

    // --- Playlist functions ---
    fun showAddToPlaylistDialog(song: Song) {
        _showAddToPlaylist.update { song }
    }

    fun hideAddToPlaylistDialog() {
        _showAddToPlaylist.update { null }
    }

    fun addSongToPlaylist(playlist: Playlist, song: Song) {
        viewModelScope.launch {
            managePlaylistUseCase.addSong(playlist.id, song)
            _showAddToPlaylist.update { null }
            _toastMessage.update { "'${song.title}' ko '${playlist.name}' me add kiya!" }
        }
    }

    fun createPlaylistAndAdd(name: String, song: Song) {
        viewModelScope.launch {
            val id = managePlaylistUseCase.createPlaylist(name)
            managePlaylistUseCase.addSong(id, song)
            _showAddToPlaylist.update { null }
            _toastMessage.update { "Nai playlist '$name' bani aur song add hua!" }
        }
    }

    // --- Download functions ---
    fun downloadSong(song: Song) {
        if (_downloadingIds.value.contains(song.id)) return
        if (song.isDownloaded) {
            _toastMessage.update { "'${song.title}' pehle se downloaded hai!" }
            return
        }

        viewModelScope.launch {
            manageDownloadsUseCase.downloadSong(song).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _downloadingIds.update { it + song.id }
                    }
                    is Resource.Success -> {
                        _downloadingIds.update { it - song.id }
                        _toastMessage.update { "'${song.title}' download ho gaya!" }
                    }
                    is Resource.Error -> {
                        _downloadingIds.update { it - song.id }
                        _toastMessage.update { "Download fail hua: ${result.message}" }
                    }
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.update { null }
    }
}