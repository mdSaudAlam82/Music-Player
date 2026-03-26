package com.example.musicplayer.presentation.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.example.musicplayer.domain.model.PlaybackMode
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.presentation.SharedViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onBackClick: () -> Unit,
    sharedViewModel: SharedViewModel,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadingIds by sharedViewModel.downloadingIds.collectAsState()
    val song = uiState.currentSong
    val context = LocalContext.current

    var showTimerDialog by remember { mutableStateOf(false) }
    var isCustomTime by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }

    var dominantColor by remember { mutableStateOf(Color(0xFF1A1A1A)) }
    val animatedBackground by animateColorAsState(targetValue = dominantColor, label = "bg_color")

    val artworkScale by animateFloatAsState(
        targetValue = if (uiState.isPlaying) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "artwork_scale"
    )

    val listState = rememberLazyListState()

    LaunchedEffect(song?.id) {
        if (song == null) return@LaunchedEffect
        dominantColor = Color(0xFF1A1A1A)
    }

    LaunchedEffect(uiState.currentLyricIndex) {
        if (uiState.parsedLyrics.isNotEmpty() && uiState.parsedLyrics[0].timeMs != -1L && !listState.isScrollInProgress) {
            listState.animateScrollToItem(uiState.currentLyricIndex, scrollOffset = -300)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedBackground.copy(alpha = 0.6f),
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.sleepTimerRemaining != null) {
                        Text(
                            text = viewModel.formatDuration(uiState.sleepTimerRemaining!!),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    // Share Button (Upar Top Bar Mein)
                    IconButton(onClick = { song?.let { shareSong(context, it) } }) {
                        Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    IconButton(onClick = viewModel::toggleLyrics) {
                        Icon(Icons.Default.Lyrics, "Lyrics", tint = if (uiState.showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = uiState.showLyrics,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "art_lyrics_transition",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) { showLyrics ->
                if (showLyrics) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            uiState.isLyricsLoading -> CircularProgressIndicator()
                            uiState.lyricsError != null -> Text(uiState.lyricsError ?: "Lyrics nahi mile")
                            uiState.parsedLyrics.isNotEmpty() -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    itemsIndexed(uiState.parsedLyrics) { index, line ->
                                        val isActive = index == uiState.currentLyricIndex && line.timeMs != -1L

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
                            .fillMaxSize()
                            .graphicsLayer { scaleX = artworkScale; scaleY = artworkScale }
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop,
                        onState = { state ->
                            if (state is AsyncImagePainter.State.Success) {
                                val bitmap = (state.result.drawable as? BitmapDrawable)?.bitmap
                                bitmap?.let {
                                    Palette.from(it).generate { palette ->
                                        val rgb = palette?.dominantSwatch?.rgb
                                            ?: palette?.mutedSwatch?.rgb
                                            ?: palette?.vibrantSwatch?.rgb
                                        rgb?.let { color -> dominantColor = Color(color) }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(song?.title ?: "Koi song nahi", style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song?.artist ?: "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                IconButton(onClick = viewModel::toggleLike) {
                    Icon(
                        imageVector = if (uiState.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (uiState.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
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

            // Main Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = viewModel::togglePlaybackMode) {
                        Icon(
                            when (uiState.playbackMode) {
                                PlaybackMode.SHUFFLE -> Icons.Default.Shuffle;
                                PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne;
                                else -> Icons.Default.Repeat
                            },
                            "Mode", tint = if (uiState.playbackMode != PlaybackMode.NONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = viewModel::previous) { Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(40.dp)) }

                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isBuffering) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            IconButton(onClick = viewModel::playPause) {
                                Icon(
                                    if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    IconButton(onClick = viewModel::next) { Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(40.dp)) }

                    // Download Button
                    if (song != null && !song.isLocal) {
                        IconButton(onClick = { sharedViewModel.downloadSong(song) }) {
                            val isDownloading = downloadingIds.contains(song.id)
                            when {
                                isDownloading -> CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                song.isDownloaded -> Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = "Downloaded",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                else -> Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    } else {
                        Box(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }

    // Timer Dialog
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = {
                showTimerDialog = false
                isCustomTime = false
                customMinutes = ""
            },
            title = { Text(if (isCustomTime) "Custom Time Set Karo" else "Sleep Timer Set Karo") },
            text = {
                if (isCustomTime) {
                    Column {
                        OutlinedTextField(
                            value = customMinutes,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() }) {
                                    customMinutes = newValue
                                }
                            },
                            label = { Text("Kitne minute? (e.g. 45)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column {
                        val options = listOf("Off" to 0, "15 Minutes" to 15, "30 Minutes" to 30, "60 Minutes" to 60)
                        options.forEach { (label, minutes) ->
                            TextButton(
                                onClick = {
                                    viewModel.setSleepTimer(minutes)
                                    showTimerDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        TextButton(
                            onClick = { isCustomTime = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Custom Time...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                if (isCustomTime) {
                    Button(onClick = {
                        val mins = customMinutes.toIntOrNull() ?: 0
                        viewModel.setSleepTimer(mins)
                        showTimerDialog = false
                        isCustomTime = false
                        customMinutes = ""
                    }) {
                        Text("Set Timer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (isCustomTime) {
                        isCustomTime = false
                        customMinutes = ""
                    } else {
                        showTimerDialog = false
                    }
                }) {
                    Text(if (isCustomTime) "Back" else "Cancel")
                }
            }
        )
    }
}

// Share karne ka logic
// Share karne ka logic (Ab Link ke sath!)
fun shareSong(context: Context, song: Song) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        if ((song.isDownloaded || song.isLocal) && song.localPath != null) {
            val file = File(song.localPath!!)
            if (file.exists()) {
                // Agar downloaded hai toh seedha Audio File bhejega (Premium Feature 🔥)
                type = "audio/*"
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Sath me text bhi bhejega (WhatsApp wagaira ke liye)
                putExtra(Intent.EXTRA_TEXT, "Mera favourite gaana suno: ${song.title} by ${song.artist}")
            } else {
                shareAsTextWithLink(this, song)
            }
        } else {
            shareAsTextWithLink(this, song)
        }
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Song via"))
}

// Online songs ke liye Text + Direct Audio Link
private fun shareAsTextWithLink(intent: Intent, song: Song) {
    intent.type = "text/plain"

    // Yahan song.streamUrl gaane ka direct link hai (jispar click karke browser me play ho jayega)
    val shareText = """
        🎵 MusicPlayer pe mast gaana suno!
        
        🎧 Title: ${song.title}
        🎤 Artist: ${song.artist}
        
        🔗 Direct Link: ${song.streamUrl}
    """.trimIndent()

    intent.putExtra(Intent.EXTRA_TEXT, shareText)
}