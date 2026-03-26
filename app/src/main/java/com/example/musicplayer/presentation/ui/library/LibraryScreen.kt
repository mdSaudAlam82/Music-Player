package com.example.musicplayer.presentation.ui.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite    // 👈 Naya Import
import androidx.compose.material.icons.filled.MoreVert    // 👈 Naya Import (Isse error theek hoga)
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayer.presentation.ui.components.SongItem
import com.example.musicplayer.presentation.SharedViewModel
import androidx.compose.material.icons.filled.Delete

@Composable
fun LibraryScreen(
    onSongClick: () -> Unit,
    sharedViewModel: SharedViewModel,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val downloadingIds by sharedViewModel.downloadingIds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.onPermissionGranted()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permission)
    }

    Column(modifier = modifier.fillMaxSize()) {

        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 16.dp)
        )

        // Sirf 2 Tabs: Device Songs aur Liked Songs
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            LibraryTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = uiState.selectedTab.ordinal == index,
                    onClick = { viewModel.onTabSelected(tab) },
                    text = { Text(tab.title) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !uiState.hasPermission && uiState.selectedTab == LibraryTab.LOCAL -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Device songs ke liye permission chahiye", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(permission) }) {
                            Text("Permission Do")
                        }
                    }
                }

                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    val songs = when (uiState.selectedTab) {
                        LibraryTab.LOCAL -> uiState.localSongs
                        LibraryTab.LIKED_SONGS -> uiState.likedSongs
                    }

                    if (songs.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (uiState.selectedTab) {
                                    LibraryTab.LOCAL -> "Koi local song nahi mila"
                                    LibraryTab.LIKED_SONGS -> "Koi liked song nahi hai ❤️"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                            item {
                                Text(
                                    text = "${songs.size} songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            itemsIndexed(songs) { index, song ->
                                SongItem(
                                    song = song,
                                    onClick = {
                                        viewModel.playSong(index)
                                        onSongClick()
                                    },
                                    onMoreClick = {
                                        if (uiState.selectedTab == LibraryTab.LIKED_SONGS) {
                                            viewModel.toggleLike(song) // Logic waise hi rahega
                                        } else {
                                            sharedViewModel.showAddToPlaylistDialog(song)
                                        }
                                    },
                                    onDownloadClick = if (!song.isLocal) { { sharedViewModel.downloadSong(it) } } else null,
                                    isDownloading = downloadingIds.contains(song.id),

                                    // 👇 YAHAN BADLAAV KIYA HAI: Heart ❤️ ki jagah Delete 🗑️ icon
                                    moreIcon = if (uiState.selectedTab == LibraryTab.LIKED_SONGS) Icons.Default.Delete else Icons.Default.MoreVert
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}