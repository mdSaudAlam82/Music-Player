package com.example.musicplayer.presentation.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicplayer.presentation.ui.components.SongCard
import com.example.musicplayer.presentation.ui.components.SongItem
import com.example.musicplayer.presentation.ui.components.SongListShimmer
import com.example.musicplayer.presentation.SharedViewModel

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

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                SongListShimmer(
                    modifier = Modifier.fillMaxSize()
                )
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // WiFi off icon
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Internet nahi hai",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Internet connection check karo\naur dobara try karo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Retry button
                    Button(
                        onClick = { viewModel.retry() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dobara Try Karo",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Library me jao — offline songs
                    OutlinedButton(
                        onClick = { onNavigateToDownloads() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Offline Songs Dekho")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Recently Played
                    if (recentlyPlayed.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recently Played",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(
                                    start = 16.dp,
                                    top = 24.dp,
                                    bottom = 12.dp
                                )
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(recentlyPlayed) { index, song ->
                                    SongCard(
                                        song = song,
                                        onClick = {
                                            viewModel.playSong(index)
                                            onSongClick()
                                        }
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // Trending Songs
                    item {
                        Text(
                            text = "Trending Songs",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                top = 24.dp,
                                bottom = 8.dp
                            )
                        )
                    }

                    itemsIndexed(uiState.trendingSongs) { index, song ->
                        SongItem(
                            song = song,
                            onClick = {
                                viewModel.playSong(index)
                                onSongClick()
                            },
                            onMoreClick = {
                                sharedViewModel.showAddToPlaylistDialog(song)
                            },
                            onDownloadClick = { sharedViewModel.downloadSong(it) },
                            isDownloading = downloadingIds.contains(song.id)
                        )
                    }
                }
            }
        }
    }
}