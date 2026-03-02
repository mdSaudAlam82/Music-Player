package com.example.musicplayer.presentation.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.domain.model.PlaybackMode
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.usecase.GetLyricsUseCase
import com.example.musicplayer.service.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getLyricsUseCase: GetLyricsUseCase,
    val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerController.playerState.collect { playerState ->
                // 👇 Naya gaana aane par purane lyrics hata do
                val isNewSong = playerState.currentSong?.id != _uiState.value.currentSong?.id

                _uiState.update {
                    it.copy(
                        currentSong = playerState.currentSong,
                        isPlaying = playerState.isPlaying,
                        currentPosition = playerState.currentPosition,
                        duration = playerState.duration,
                        playbackMode = playerState.playbackMode,
                        // Naye gaane ke liye sab reset
                        lyrics = if (isNewSong) null else it.lyrics,
                        lyricsError = if (isNewSong) null else it.lyricsError,
                        showLyrics = if (isNewSong) false else it.showLyrics,
                        isLyricsLoading = if (isNewSong) false else it.isLyricsLoading
                    )
                }
            }
        }
    }

    fun playPause() = playerController.pauseResume()
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun seekTo(position: Long) = playerController.seekTo(position)

    fun togglePlaybackMode() {
        val currentMode = _uiState.value.playbackMode
        val nextMode = when (currentMode) {
            PlaybackMode.NONE -> PlaybackMode.REPEAT_ALL
            PlaybackMode.REPEAT_ALL -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.NONE
        }
        playerController.setPlaybackMode(nextMode)
    }

    fun toggleLyrics() {
        val currentSong = _uiState.value.currentSong ?: return

        if (_uiState.value.showLyrics) {
            _uiState.update { it.copy(showLyrics = false) }
            return
        }

        // 👇 Yahan ID ki jagah Title aur Artist pass kar rahe hain
        if (_uiState.value.lyrics == null) {
            loadLyrics(currentSong.title, currentSong.artist ?: "")
        }
        _uiState.update { it.copy(showLyrics = true) }
    }

    private fun loadLyrics(title: String, artist: String) {
        viewModelScope.launch {
            getLyricsUseCase(title, artist).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(isLyricsLoading = true, lyricsError = null)
                    }
                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLyricsLoading = false,
                            lyrics = result.data,
                            lyricsError = null
                        )
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(
                            isLyricsLoading = false,
                            lyricsError = result.message
                        )
                    }
                }
            }
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}