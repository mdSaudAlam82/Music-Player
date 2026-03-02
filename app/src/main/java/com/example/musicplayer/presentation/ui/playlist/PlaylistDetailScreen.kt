package com.example.musicplayer.presentation.ui.playlist

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Delete // ✅ Naya import delete ke liye
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBackClick: () -> Unit,
    onSongClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailUiState.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylistDetail(playlistId)
    }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Text(
                    text = detailState.playlist?.name ?: "Playlist",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = viewModel::showRenameDialog) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                }
            }
        )

        val songs = detailState.playlist?.songs ?: emptyList()

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Is playlist me koi song nahi",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Search se songs add karo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
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
                            viewModel.removeSongFromPlaylist(song.id)
                        },
                        // 👇 AB YAHAN 3-DOT KI JAGAH TRASH ICON DIKHEGA
                        moreIcon = Icons.Default.Delete
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (detailState.showRenameDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideRenameDialog,
            title = { Text("Playlist Rename Karo") },
            text = {
                OutlinedTextField(
                    value = detailState.newName,
                    onValueChange = viewModel::onRenameChange,
                    label = { Text("Naya naam") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = viewModel::renamePlaylist) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideRenameDialog) {
                    Text("Cancel")
                }
            }
        )
    }
}