package com.example.musicplayer.presentation.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.musicplayer.presentation.SharedViewModel
import com.example.musicplayer.presentation.ui.components.SongItem

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
    val downloadingIds by sharedViewModel.downloadingIds.collectAsState()

    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState() // ✅ Scroll track karne ke liye

    // 👇 NAYA: Infinite Scroll Logic (Jab aakhiri gaane par pahuchega, agla page mangwayega)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null &&
                    lastVisibleIndex >= uiState.songs.size - 3 && // List khatam hone se 3 item pehle
                    !uiState.isLoading &&
                    !uiState.isPaginating &&
                    !uiState.hasReachedEnd
                ) {
                    viewModel.loadNextPage()
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search Bar
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
            // 👇 NAYA: Keyboard me Search/Enter dabane ka action
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    focusManager.clearFocus() // Keyboard band karo
                    viewModel.forceSearch()   // Exact search trigger karo
                }
            )
        )

        // Result Content
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
                        state = listState, // ✅ List State pass kiya
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
                                onMoreClick = { /* Handle more options if needed */ }
                            )
                        }

                        // 👇 NAYA: Niche aane par spinner dikhao jab naya page load ho raha ho
                        if (uiState.isPaginating) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
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
}