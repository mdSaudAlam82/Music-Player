package com.example.musicplayer.presentation.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.musicplayer.domain.model.PlaybackMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val song = uiState.currentSong

    val artworkScale by animateFloatAsState(
        targetValue = if (uiState.isPlaying) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "artwork_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface, // Solid Color for clean exit
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.KeyboardArrowDown, "Back", modifier = Modifier.size(32.dp))
                }
                Text("Now Playing", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = viewModel::toggleLyrics) {
                    Icon(Icons.Default.Lyrics, "Lyrics", tint = if (uiState.showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = uiState.showLyrics,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "art_lyrics_transition"
            ) { showLyrics ->
                if (showLyrics) {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            uiState.isLyricsLoading -> CircularProgressIndicator()
                            uiState.lyricsError != null -> Text("Lyrics nahi mile")
                            uiState.lyrics != null -> Text(uiState.lyrics!!, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()))
                        }
                    }
                } else {
                    AsyncImage(
                        model = song?.artworkUrl,
                        contentDescription = song?.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .graphicsLayer { scaleX = artworkScale; scaleY = artworkScale }
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(song?.title ?: "Koi song nahi", style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song?.artist ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Slider(
                value = if (uiState.duration > 0) uiState.currentPosition.toFloat() / uiState.duration.toFloat() else 0f,
                onValueChange = { progress -> viewModel.seekTo((progress * uiState.duration).toLong()) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(viewModel.formatDuration(uiState.currentPosition), style = MaterialTheme.typography.bodySmall)
                Text(viewModel.formatDuration(uiState.duration), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::togglePlaybackMode) {
                    Icon(
                        when (uiState.playbackMode) { PlaybackMode.SHUFFLE -> Icons.Default.Shuffle; PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne; else -> Icons.Default.Repeat },
                        "Mode", tint = if (uiState.playbackMode != PlaybackMode.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = viewModel::previous) { Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(40.dp)) }
                Box(modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    IconButton(onClick = viewModel::playPause) { Icon(if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp)) }
                }
                IconButton(onClick = viewModel::next) { Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(40.dp)) }
                Box(modifier = Modifier.size(48.dp))
            }
        }
    }
}