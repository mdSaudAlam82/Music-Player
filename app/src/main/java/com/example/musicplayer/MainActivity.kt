package com.example.musicplayer

import android.content.ComponentName // ✅ Naya Import
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.session.MediaController // ✅ Naya Import
import androidx.media3.session.SessionToken // ✅ Naya Import
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.presentation.SharedViewModel
import com.example.musicplayer.presentation.navigation.MusicNavGraph
import com.example.musicplayer.presentation.navigation.Screen
import com.example.musicplayer.presentation.ui.components.AddToPlaylistDialog
import com.example.musicplayer.presentation.ui.components.MiniPlayer
import com.example.musicplayer.presentation.ui.home.HomeViewModel
import com.example.musicplayer.presentation.ui.theme.MusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import com.example.musicplayer.service.MusicPlayerService

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Notification permission Android 13+ ke liye
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Battery Optimization Bypass (CRASH FREE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 👇 NAYA CODE: Service ko zinda rakhne ke liye Lifeline (Binding)
        // Ye code insure karega ki gaana chalte hi Notification 100% dikhe
        val sessionToken = SessionToken(
            this,
            ComponentName(this, MusicPlayerService::class.java)
        )
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        // 👆 NAYA CODE KHATAM

        setContent {
            MusicPlayerTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val homeViewModel: HomeViewModel = hiltViewModel()
                val playerState by homeViewModel.playerController.playerState.collectAsState()

                val sharedViewModel: SharedViewModel = hiltViewModel()
                val showAddToPlaylist by sharedViewModel.showAddToPlaylist.collectAsState()
                val playlists by sharedViewModel.playlists.collectAsState()
                val toastMessage by sharedViewModel.toastMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }
                var showNewPlaylistDialog by rememberSaveable { mutableStateOf(false) }
                var newPlaylistName by rememberSaveable { mutableStateOf("") }

                // Toast show karo
                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        sharedViewModel.clearToast()
                    }
                }

                val bottomNavItems = listOf(
                    Triple(Screen.Home.route, Icons.Default.Home, "Home"),
                    Triple(Screen.Search.route, Icons.Default.Search, "Search"),
                    Triple(Screen.Library.route, Icons.Default.LibraryMusic, "Library"),
                    Triple(Screen.Playlist.route, Icons.Default.PlaylistPlay, "Playlists"),
                    Triple(Screen.Downloads.route, Icons.Default.Download, "Downloads")
                )

                val showBottomBar = currentRoute != Screen.NowPlaying.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar) {
                            Column {
                                MiniPlayer(
                                    playerState = playerState,
                                    onPlayPause = {
                                        homeViewModel.playerController.pauseResume()
                                    },
                                    onNext = {
                                        homeViewModel.playerController.next()
                                    },
                                    onPlayerClick = {
                                        navController.navigate(Screen.NowPlaying.route)
                                    }
                                )
                                NavigationBar {
                                    bottomNavItems.forEach { (route, icon, label) ->
                                        NavigationBarItem(
                                            selected = currentRoute == route,
                                            onClick = {
                                                navController.navigate(route) {
                                                    popUpTo(Screen.Home.route) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = {
                                                Icon(icon, contentDescription = label)
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    MusicNavGraph(
                        navController = navController,
                        sharedViewModel = sharedViewModel,
                        modifier = Modifier.padding(paddingValues)
                    )

                    // Add to Playlist Dialog
                    showAddToPlaylist?.let { song ->
                        AddToPlaylistDialog(
                            song = song,
                            playlists = playlists,
                            onPlaylistSelected = { playlist ->
                                sharedViewModel.addSongToPlaylist(playlist, song)
                            },
                            onCreateNew = {
                                showNewPlaylistDialog = true
                                newPlaylistName = ""
                            },
                            onDismiss = {
                                sharedViewModel.hideAddToPlaylistDialog()
                            }
                        )
                    }

                    // Nai playlist banao dialog
                    if (showNewPlaylistDialog) {
                        AlertDialog(
                            onDismissRequest = { showNewPlaylistDialog = false },
                            title = { Text("Nai Playlist") },
                            text = {
                                OutlinedTextField(
                                    value = newPlaylistName,
                                    onValueChange = { newPlaylistName = it },
                                    label = { Text("Playlist naam") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    showAddToPlaylist?.let { song ->
                                        sharedViewModel.createPlaylistAndAdd(
                                            newPlaylistName,
                                            song
                                        )
                                    }
                                    showNewPlaylistDialog = false
                                }) { Text("Banao") }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showNewPlaylistDialog = false
                                }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    }
}