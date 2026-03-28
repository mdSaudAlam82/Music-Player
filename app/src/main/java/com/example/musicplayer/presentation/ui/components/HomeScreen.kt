package com.example.musicplayer.presentation.ui.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.presentation.SharedViewModel
import com.example.musicplayer.presentation.ui.components.AddToPlaylistDialog
import com.example.musicplayer.presentation.ui.components.SongCard
import com.example.musicplayer.presentation.ui.components.SongItem
import com.example.musicplayer.presentation.ui.components.SongListShimmer
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSongClick: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    sharedViewModel: SharedViewModel,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadingIds by sharedViewModel.downloadingIds.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val context = LocalContext.current

    // 👇 NAYA: Options Menu aur Playlist Dialog ke states
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showPlaylistDialog by remember { mutableStateOf<Song?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            while (true) {
                delay(3000L)
                viewModel.retry()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> { SongListShimmer(modifier = Modifier.fillMaxSize()) }
            uiState.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Internet nahi hai", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Internet connection check karo\naur dobara try karo", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.retry() }, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dobara Try Karo", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { onNavigateToDownloads() }, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.LibraryMusic, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Offline Songs Dekho")
                    }
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    if (recentlyPlayed.isNotEmpty()) {
                        item { Text("Recently Played", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)) }
                        item {
                            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(recentlyPlayed) { index, song ->
                                    SongCard(song = song, onClick = {
                                        // Pura queue bhejne ke liye, but simplify rakhte hain
                                        viewModel.playerController.playSong(song, recentlyPlayed)
                                        onSongClick()
                                    })
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    item { Text("For You", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)) }

                    itemsIndexed(uiState.trendingSongs) { index, song ->
                        SongItem(
                            song = song,
                            onClick = { viewModel.playSong(index); onSongClick() },
                            // 👇 YAHAN CHANGE KIYA: 3-dot par click karne se bottom sheet open hoga
                            onMoreClick = { selectedSongForOptions = song },
                            onDownloadClick = { sharedViewModel.downloadSong(it) },
                            isDownloading = downloadingIds.contains(song.id)
                        )
                    }
                }
            }
        }
    }

    // 👇 3-Dot Options Bottom Sheet 👇
    if (selectedSongForOptions != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSongForOptions = null },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(
                    text = selectedSongForOptions!!.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                HorizontalDivider()

                // 1. Add To Playlist
                ListItem(
                    headlineContent = { Text("Add to Playlist") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showPlaylistDialog = selectedSongForOptions
                        selectedSongForOptions = null
                    }
                )

                // 2. Share Song
                ListItem(
                    headlineContent = { Text("Share Song") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        shareSongFromHome(context, selectedSongForOptions!!)
                        selectedSongForOptions = null
                    }
                )
            }
        }
    }

    // 👇 Playlist Dialog 👇
    if (showPlaylistDialog != null) {
        val playlists by sharedViewModel.playlists.collectAsState(initial = emptyList())

        AddToPlaylistDialog(
            song = showPlaylistDialog!!,
            playlists = playlists,
            onPlaylistSelected = { playlist ->
                sharedViewModel.addSongToPlaylist(playlist, showPlaylistDialog!!)
                showPlaylistDialog = null
            },
            onCreateNew = { /* Custom logic if needed */ },
            onDismiss = { showPlaylistDialog = null }
        )
    }
}

// Helper Function for Sharing (Same as SearchScreen)
fun shareSongFromHome(context: Context, song: Song) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        if ((song.isDownloaded || song.isLocal) && song.localPath != null) {
            val file = File(song.localPath!!)
            if (file.exists()) {
                type = "audio/*"
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_TEXT, "Mera favourite gaana suno: ${song.title} by ${song.artist}")
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "🎵 MusicPlayer pe gaana suno: ${song.title}\n🔗 Link: ${song.streamUrl}")
            }
        } else {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "🎵 MusicPlayer pe gaana suno: ${song.title}\n🔗 Link: ${song.streamUrl}")
        }
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Song via"))
}