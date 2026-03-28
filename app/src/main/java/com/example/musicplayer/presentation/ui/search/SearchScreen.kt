package com.example.musicplayer.presentation.ui.search

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.musicplayer.domain.model.Song
import com.example.musicplayer.presentation.SharedViewModel
import com.example.musicplayer.presentation.ui.components.AddToPlaylistDialog
import com.example.musicplayer.presentation.ui.components.SongItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSongClick: () -> Unit,
    sharedViewModel: SharedViewModel,
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val context = LocalContext.current

    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showPlaylistDialog by remember { mutableStateOf<Song?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= uiState.songs.size - 3 &&
                    !uiState.isLoading &&
                    !uiState.isPaginating &&
                    !uiState.hasReachedEnd
                ) {
                    viewModel.loadNextPage()
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Songs, Artists ya Albums search karein...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (uiState.query.isNotEmpty()) {
                    IconButton(onClick = {
                        viewModel.onQueryChange("")
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus()
                    viewModel.forceSearch()
                }
            )
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Error aagaya",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                uiState.isEmpty -> {
                    Text(
                        text = "Koi result nahi mila",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(uiState.songs, key = { _, song -> song.id }) { index, song ->
                            SongItem(
                                song = song,
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.playSong(index)
                                    onSongClick()
                                },
                                onMoreClick = { selectedSongForOptions = song }
                            )
                        }

                        if (uiState.isPaginating) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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

                ListItem(
                    headlineContent = { Text("Add to Playlist") },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showPlaylistDialog = selectedSongForOptions
                        selectedSongForOptions = null
                    }
                )

                ListItem(
                    headlineContent = { Text("Share Song") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        shareSongFromSearch(context, selectedSongForOptions!!)
                        selectedSongForOptions = null
                    }
                )
            }
        }
    }

    if (showPlaylistDialog != null) {
        val playlists by sharedViewModel.playlists.collectAsState(initial = emptyList())

        AddToPlaylistDialog(
            song = showPlaylistDialog!!,
            playlists = playlists,
            onPlaylistSelected = { playlist ->
                // 👇 FIX: Yahan 'playlist.id' ki jagah sirf 'playlist' bhejna tha
                sharedViewModel.addSongToPlaylist(playlist, showPlaylistDialog!!)
                showPlaylistDialog = null
            },
            onCreateNew = { /* Navigate to create playlist screen if needed */ },
            onDismiss = { showPlaylistDialog = null }
        )
    }
}

fun shareSongFromSearch(context: Context, song: Song) {
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