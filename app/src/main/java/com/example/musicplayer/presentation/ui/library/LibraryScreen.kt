package com.example.musicplayer.presentation.ui.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayer.presentation.ui.components.SongItem
import com.example.musicplayer.presentation.SharedViewModel

@Composable
fun LibraryScreen(
    onSongClick: () -> Unit,
    sharedViewModel: SharedViewModel,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val downloadingIds by sharedViewModel.downloadingIds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Permission launcher
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

    // Auto permission maango
    LaunchedEffect(Unit) {
        permissionLauncher.launch(permission)
    }

    Column(modifier = modifier.fillMaxSize()) {

        // Header
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 24.dp,
                bottom = 16.dp
            )
        )

        // Tabs
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal
        ) {
            LibraryTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = uiState.selectedTab.ordinal == index,
                    onClick = { viewModel.onTabSelected(tab) },
                    text = { Text(tab.title) }
                )
            }
        }

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                !uiState.hasPermission -> {
                    // Permission nahi mili
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Device ke songs dekhne ke liye permission chahiye",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(permission) }
                        ) {
                            Text("Permission Do")
                        }
                    }
                }

                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }

                else -> {
                    val songs = when (uiState.selectedTab) {
                        LibraryTab.LOCAL -> uiState.localSongs
                        LibraryTab.DOWNLOADED -> uiState.downloadedSongs
                    }

                    if (songs.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when (uiState.selectedTab) {
                                    LibraryTab.LOCAL -> "Koi local song nahi mila"
                                    LibraryTab.DOWNLOADED -> "Koi downloaded song nahi"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            item {
                                Text(
                                    text = "${songs.size} songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
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
                                        if (uiState.selectedTab == LibraryTab.DOWNLOADED) {
                                            viewModel.deleteDownload(song.id)
                                        } else {
                                            sharedViewModel.showAddToPlaylistDialog(song)
                                        }
                                    },
                                    // Local songs pe download nahi dikhega automatically
                                    onDownloadClick = if (!song.isLocal) {
                                        { sharedViewModel.downloadSong(it) }
                                    } else null,
                                    isDownloading = downloadingIds.contains(song.id)
                                )

                            }
                        }
                    }
                }
            }
        }
    }
}