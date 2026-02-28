package com.example.musicplayer.presentation.ui.search

import com.example.musicplayer.domain.model.Song

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val error: String? = null,
    val isEmpty: Boolean = false
)