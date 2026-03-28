package com.example.musicplayer.service

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicplayer.domain.model.PlayerState
import com.example.musicplayer.domain.model.PlaybackMode
import com.example.musicplayer.domain.model.Resource
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.domain.repository.LocalMusicRepository
import com.example.musicplayer.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: ExoPlayer,
    private val musicRepository: MusicRepository,
    private val localMusicRepository: LocalMusicRepository
) {
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var retryCount = 0

    // Cancels ongoing API fetch when user quickly changes song
    private var currentFetchJob: Job? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var wasPlayingBeforeCall = false

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (player.isPlaying) {
                    wasPlayingBeforeCall = true
                    player.pause()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (wasPlayingBeforeCall) {
                    player.play()
                    wasPlayingBeforeCall = false
                }
            }
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    init {
        setupPlayerListener()
        scope.launch {
            if (player.mediaItemCount == 0) {
                try {
                    val lastSongs = localMusicRepository.getRecentlyPlayed().firstOrNull()
                    if (!lastSongs.isNullOrEmpty()) {
                        val mediaItems = lastSongs.map { it.toMediaItem() }
                        player.setMediaItems(mediaItems, 0, 0L)
                        player.prepare()
                        _playerState.update {
                            it.copy(
                                currentSong = lastSongs.first(),
                                queue = lastSongs,
                                currentIndex = 0
                            )
                        }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val currentSong = _playerState.value.currentSong
                if (currentSong != null && !currentSong.isLocal && !currentSong.isDownloaded) {
                    if (retryCount < 2) {
                        retryCount++
                        playSong(currentSong, _playerState.value.queue)
                    } else {
                        retryCount = 0
                        next()
                    }
                } else next()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady) {
                    if (player.mediaItemCount == 0) {
                        scope.launch {
                            try {
                                val lastSongs = localMusicRepository.getRecentlyPlayed().firstOrNull()
                                if (!lastSongs.isNullOrEmpty()) {
                                    playSong(lastSongs.first(), lastSongs)
                                }
                            } catch (e: Exception) { }
                        }
                    } else if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                        player.prepare()
                        player.play()
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> _playerState.update { it.copy(duration = player.duration.coerceAtLeast(0L), isBuffering = false) }
                    Player.STATE_BUFFERING -> _playerState.update { it.copy(isBuffering = true) }
                    Player.STATE_ENDED -> {
                        stopProgressTracking()
                        checkAndPlaySimilarSongs()
                    }
                    else -> _playerState.update { it.copy(isBuffering = false) }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressTracking() else stopProgressTracking()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                retryCount = 0
                val newIndex = player.currentMediaItemIndex
                if (newIndex != _playerState.value.currentIndex) {
                    _playerState.update {
                        it.copy(
                            currentIndex = newIndex,
                            currentPosition = 0L,
                            duration = 0L,
                            // Ensure fast reset to prevent laggy old UI
                            isBuffering = true
                        )
                    }
                    updateCurrentSong()
                }
            }
        })
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        requestAudioFocus()
        wasPlayingBeforeCall = false

        val fullQueue = if (queue.isEmpty()) listOf(song) else queue
        val startIndex = fullQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        // UI instantly reset
        _playerState.update {
            it.copy(
                currentSong = song,
                currentPosition = 0L,
                duration = 0L,
                isLoading = true,
                isBuffering = true
            )
        }

        // Cancel previous pending network load to prioritize user's new click immediately
        currentFetchJob?.cancel()

        // FAST PATH: Agar link already available hai toh network delay ko completely bypass karo
        if (song.isDownloaded || song.isLocal || song.streamUrl.isNotBlank()) {
            playDirectly(song, fullQueue, startIndex)
            return
        }

        // SLOW PATH: Agar link nahi hai (sirf playlists me hota hai), toh API fetch ko async chalao.
        currentFetchJob = scope.launch {
            try {
                musicRepository.getSongById(song.id).collect { result ->
                    if (result is Resource.Success) {
                        playDirectly(result.data ?: song, fullQueue, startIndex)
                    } else if (result is Resource.Error) {
                        playDirectly(song, fullQueue, startIndex)
                    }
                }
            } catch (e: Exception) {
                playDirectly(song, fullQueue, startIndex)
            }
        }
    }

    private fun playDirectly(song: Song, queue: List<Song>, startIndex: Int) {
        val currentQueueIds = _playerState.value.queue.map { it.id }
        val newQueueIds = queue.map { it.id }

        // Smart Queue Detection
        val isSameQueue = currentQueueIds == newQueueIds && player.mediaItemCount == queue.size

        val updatedQueue = queue.toMutableList()
        if (startIndex in updatedQueue.indices) {
            updatedQueue[startIndex] = song
        }

        if (isSameQueue) {
            // Seek ko fast banane ke liye Media Item update sirf tab karo jab actually URL change hua ho
            val newItem = song.toMediaItem()
            val oldItem = player.getMediaItemAt(startIndex)

            if (oldItem.localConfiguration?.uri != newItem.localConfiguration?.uri) {
                player.replaceMediaItem(startIndex, newItem)
            }
            player.seekTo(startIndex, 0L)
        } else {
            // Nayi list banana heavy ho sakta hai, par isko humne UI reset hone ke baad chalaya hai
            val mediaItems = updatedQueue.map { it.toMediaItem() }
            player.setMediaItems(mediaItems, startIndex, 0L)
        }

        // Ye immediately play function start karega
        player.prepare()
        player.play()

        _playerState.update {
            it.copy(
                currentSong = song,
                queue = updatedQueue,
                currentIndex = startIndex,
                isPlaying = true,
                isLoading = false,
                isBuffering = false // Buffering ab player k hud handle karega
            )
        }
    }

    fun pauseResume() {
        if (player.isPlaying) {
            player.pause()
            wasPlayingBeforeCall = false
        } else {
            requestAudioFocus()
            player.prepare()
            player.play()
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            var remaining = minutes * 60 * 1000L
            _playerState.update { it.copy(sleepTimerRemaining = remaining) }
            sleepTimerJob = scope.launch {
                while (remaining > 0) {
                    delay(1000L)
                    remaining -= 1000L
                    _playerState.update { it.copy(sleepTimerRemaining = remaining) }
                }
                if (player.isPlaying) player.pause()
                _playerState.update { it.copy(sleepTimerRemaining = null) }
            }
        } else {
            _playerState.update { it.copy(sleepTimerRemaining = null) }
        }
    }

    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            checkAndPlaySimilarSongs()
        }
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
            PlaybackMode.NONE -> { player.repeatMode = Player.REPEAT_MODE_OFF; player.shuffleModeEnabled = false }
            PlaybackMode.REPEAT_ONE -> { player.repeatMode = Player.REPEAT_MODE_ONE; player.shuffleModeEnabled = false }
            PlaybackMode.REPEAT_ALL -> { player.repeatMode = Player.REPEAT_MODE_ALL; player.shuffleModeEnabled = false }
            PlaybackMode.SHUFFLE -> { player.repeatMode = Player.REPEAT_MODE_OFF; player.shuffleModeEnabled = true }
        }
        _playerState.update { it.copy(playbackMode = mode) }
    }

    private fun updateCurrentSong() {
        val currentIndex = player.currentMediaItemIndex
        val queue = _playerState.value.queue
        if (currentIndex in queue.indices) {
            val nextSong = queue[currentIndex]
            if (nextSong.streamUrl.isBlank() && !nextSong.isLocal && !nextSong.isDownloaded) {
                playSong(nextSong, queue)
            } else {
                _playerState.update {
                    it.copy(currentSong = nextSong)
                }
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                if (player.isPlaying) {
                    _playerState.update { it.copy(currentPosition = player.currentPosition) }
                }
                delay(100L)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracking()
        player.clearMediaItems()
    }

    private fun checkAndPlaySimilarSongs() {
        if (player.repeatMode != Player.REPEAT_MODE_ALL &&
            player.repeatMode != Player.REPEAT_MODE_ONE &&
            player.currentMediaItemIndex == player.mediaItemCount - 1) {

            val currentSong = _playerState.value.currentSong ?: return
            _playerState.update { it.copy(isLoading = true) }

            scope.launch {
                musicRepository.getSimilarSongs(currentSong).collect { result ->
                    if (result is Resource.Success && !result.data.isNullOrEmpty()) {
                        val similarSongs = result.data
                        similarSongs.forEach { song -> player.addMediaItem(song.toMediaItem()) }

                        val updatedQueue = _playerState.value.queue + similarSongs
                        _playerState.update { it.copy(queue = updatedQueue, isLoading = false) }

                        if (player.hasNextMediaItem()) {
                            player.seekToNextMediaItem()
                            player.prepare()
                            player.play()
                        }
                    } else {
                        _playerState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    private fun Song.toMediaItem(): MediaItem {
        val uri = when {
            isDownloaded && !localPath.isNullOrBlank() -> android.net.Uri.fromFile(File(localPath))
            else -> android.net.Uri.parse(streamUrl.ifBlank { "http://empty" })
        }
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(if (artworkUrl.isNotBlank()) android.net.Uri.parse(artworkUrl) else null)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }
}