package com.example.musicplayer.presentation.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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

    // 👇 LYRICS PARSING LOGIC 👇
    // Ye line server se aaye hue text ko list mein tod degi
    val parsedLyrics = remember(uiState.lyrics) { parseLrc(uiState.lyrics ?: "") }
    val listState = rememberLazyListState()

    // Pata karo ki gaane ke time ke hisaab se abhi kaunsi line chal rahi hai
    val currentLineIndex = remember(uiState.currentPosition, parsedLyrics) {
        val index = parsedLyrics.indexOfLast { it.timeMs <= uiState.currentPosition }
        if (index != -1) index else 0
    }

    // Jaise hi line change hogi, screen apne aap wahan scroll ho jayegi
    LaunchedEffect(currentLineIndex) {
        if (parsedLyrics.isNotEmpty() && parsedLyrics[0].timeMs != -1L) {
            listState.animateScrollToItem(currentLineIndex, scrollOffset = -300)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            uiState.isLyricsLoading -> CircularProgressIndicator()
                            uiState.lyricsError != null -> Text(uiState.lyricsError ?: "Lyrics nahi mile")
                            parsedLyrics.isNotEmpty() -> {
                                // 👇 AUTO SCROLLING LYRICS UI 👇
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    itemsIndexed(parsedLyrics) { index, line ->
                                        // Highlight condition: Current line ho aur wo synced format mein ho
                                        val isActive = index == currentLineIndex && line.timeMs != -1L

                                        Text(
                                            text = line.text,
                                            style = if (isActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 12.dp)
                                        )
                                    }
                                }
                            }
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

// ==========================================
// 👇 YE RAHI PARSER CLASS AUR FUNCTION 👇
// Ise yahi file ke sabse neeche rehne dena
// ==========================================

data class LyricLine(val timeMs: Long, val text: String)

fun parseLrc(lrc: String): List<LyricLine> {
    if (lrc.isBlank()) return emptyList()

    val lines = mutableListOf<LyricLine>()
    val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    lrc.lines().forEach { line ->
        val match = regex.find(line)
        if (match != null) {
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val msPart = match.groupValues[3]
            // Agar millisecond 2 digit ka hai toh use 3 digit banate hain
            val ms = if (msPart.length == 2) msPart.toLong() * 10 else msPart.toLong()
            val text = match.groupValues[4].trim()
            val totalMs = min * 60000 + sec * 1000 + ms

            if (text.isNotEmpty()) {
                lines.add(LyricLine(totalMs, text))
            }
        }
    }

    // Agar gaane mein synced (time wale) lyrics nahi hain, toh usko normal text ki tarah padh lo
    if (lines.isEmpty() && lrc.isNotBlank()) {
        return lrc.lines().filter { it.isNotBlank() }.map { LyricLine(-1L, it.trim()) }
    }

    return lines
}