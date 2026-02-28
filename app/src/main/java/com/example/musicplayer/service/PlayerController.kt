package com.example.musicplayer.service

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicplayer.domain.model.PlayerState
import com.example.musicplayer.domain.model.PlaybackMode
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.model.Resource // ← Import add kiya
import com.example.musicplayer.domain.repository.MusicRepository // ← Import add kiya
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    private val player: ExoPlayer,
    private val musicRepository: MusicRepository // ← Constructor mein inject kiya
) {
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    init {
        setupPlayerListener()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        _playerState.update {
                            it.copy(duration = player.duration.coerceAtLeast(0L))
                        }
                    }
                    Player.STATE_ENDED -> stopProgressTracking()
                    else -> {}
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTracking()
                else stopProgressTracking()
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                _playerState.update {
                    it.copy(currentIndex = player.currentMediaItemIndex)
                }
                updateCurrentSong()
            }
        })
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        val fullQueue = if (queue.isEmpty()) listOf(song) else queue
        val startIndex = fullQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        scope.launch {
            if (song.isDownloaded || song.isLocal) {
                playDirectly(song, fullQueue, startIndex)
                return@launch
            }

            _playerState.update { it.copy(isLoading = true) }
            try {
                musicRepository.getSongById(song.id).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val freshSong = result.data ?: song
                            playDirectly(freshSong, fullQueue, startIndex)
                        }
                        is Resource.Error -> {
                            playDirectly(song, fullQueue, startIndex)
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                playDirectly(song, fullQueue, startIndex)
            }
            _playerState.update { it.copy(isLoading = false) }
        }
    }

    private fun playDirectly(song: Song, queue: List<Song>, startIndex: Int) {
        val mediaItems = queue.map { queueSong ->
            if (queueSong.id == song.id) {
                song.toMediaItem()
            } else {
                queueSong.toMediaItem()
            }
        }

        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()

        _playerState.update {
            it.copy(
                currentSong = song,
                queue = queue,
                currentIndex = startIndex,
                isPlaying = true,
                isLoading = false
            )
        }
    }

    fun pauseResume() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    fun previous() {
        if (player.currentPosition > 3000L) player.seekTo(0L)
        else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
        _playerState.update { it.copy(currentPosition = position) }
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        when (mode) {
            PlaybackMode.NONE -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = false
            }
            PlaybackMode.REPEAT_ONE -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.shuffleModeEnabled = false
            }
            PlaybackMode.REPEAT_ALL -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                player.shuffleModeEnabled = false
            }
            PlaybackMode.SHUFFLE -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                player.shuffleModeEnabled = true
            }
        }
        _playerState.update { it.copy(playbackMode = mode) }
    }

    fun addToQueue(song: Song) {
        player.addMediaItem(song.toMediaItem())
        _playerState.update { it.copy(queue = it.queue + song) }
    }

    private fun updateCurrentSong() {
        val currentIndex = player.currentMediaItemIndex
        val queue = _playerState.value.queue
        if (currentIndex < queue.size) {
            _playerState.update { it.copy(currentSong = queue[currentIndex]) }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                if (player.isPlaying) {
                    _playerState.update {
                        it.copy(currentPosition = player.currentPosition)
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracking()
        scope.cancel()
    }

    private fun Song.toMediaItem(): MediaItem {
        val uri = when {
            isDownloaded && !localPath.isNullOrBlank() -> {
                val file = File(localPath)
                if (file.exists()) android.net.Uri.fromFile(file)
                else android.net.Uri.parse(streamUrl)
            }
            isLocal && !localPath.isNullOrBlank() -> {
                android.net.Uri.parse(localPath)
            }
            else -> android.net.Uri.parse(streamUrl)
        }

        val artworkUri = try {
            if (artworkUrl.isNotBlank()) android.net.Uri.parse(artworkUrl)
            else null
        } catch (e: Exception) { null }

        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artworkUri)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }
}