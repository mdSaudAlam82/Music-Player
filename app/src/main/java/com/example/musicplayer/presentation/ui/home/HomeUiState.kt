package com.example.musicplayer.presentation.ui.home

import com.example.musicplayer.domain.model.Song

data class HomeUiState(
    val isLoading: Boolean = false,
    val trendingSongs: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val error: String? = null
)