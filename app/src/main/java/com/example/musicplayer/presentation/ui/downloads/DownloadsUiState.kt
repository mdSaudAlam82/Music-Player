package com.example.musicplayer.presentation.ui.downloads

import com.example.musicplayer.domain.model.Song

data class DownloadsUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val downloadingSongId: String? = null,
    val downloadProgress: Int = 0
)