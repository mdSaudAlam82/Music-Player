package com.example.musicplayer.presentation.ui.library

import com.example.musicplayer.domain.model.Song

data class LibraryUiState(
    val isLoading: Boolean = false,
    val localSongs: List<Song> = emptyList(),
    val likedSongs: List<Song> = emptyList(),
    val error: String? = null,
    val selectedTab: LibraryTab = LibraryTab.LOCAL,
    val hasPermission: Boolean = false
)

// 👇 UPDATED: Sirf do tabs rakhe hain ab
enum class LibraryTab(val title: String) {
    LOCAL("Device Songs"),
    LIKED_SONGS("Liked Songs")
}