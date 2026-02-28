package com.example.musicplayer.presentation.ui.playlist

import com.example.musicplayer.domain.model.Playlist
import com.example.musicplayer.domain.model.Song

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val newPlaylistName: String = ""
)

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showRenameDialog: Boolean = false,
    val newName: String = ""
)