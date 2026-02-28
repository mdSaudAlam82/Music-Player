package com.example.musicplayer.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.musicplayer.presentation.SharedViewModel
import com.example.musicplayer.presentation.ui.downloads.DownloadsScreen
import com.example.musicplayer.presentation.ui.home.HomeScreen
import com.example.musicplayer.presentation.ui.library.LibraryScreen
import com.example.musicplayer.presentation.ui.player.NowPlayingScreen
import com.example.musicplayer.presentation.ui.playlist.PlaylistDetailScreen
import com.example.musicplayer.presentation.ui.playlist.PlaylistScreen
import com.example.musicplayer.presentation.ui.search.SearchScreen

private const val ANIM_DURATION = 300

@Composable
fun MusicNavGraph(
    navController: NavHostController,
    sharedViewModel: SharedViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(ANIM_DURATION)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(ANIM_DURATION)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(ANIM_DURATION)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(ANIM_DURATION)
            )
        }
    ) {
        composable(
            route = Screen.Home.route,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            HomeScreen(
                onSongClick = {
                    navController.navigate(Screen.NowPlaying.route)
                },
                onNavigateToDownloads = {
                    navController.navigate(Screen.Downloads.route)  // ← Ye add karo
                },
                sharedViewModel = sharedViewModel
            )
        }

        composable(
            route = Screen.Search.route,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            SearchScreen(
                onSongClick = {
                    navController.navigate(Screen.NowPlaying.route)
                },
                sharedViewModel = sharedViewModel
            )
        }

        composable(
            route = Screen.Library.route,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            LibraryScreen(
                onSongClick = {
                    navController.navigate(Screen.NowPlaying.route)
                },
                sharedViewModel = sharedViewModel
            )
        }

        composable(
            route = Screen.Playlist.route,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            PlaylistScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(
                        Screen.PlaylistDetail.createRoute(playlistId)
                    )
                }
            )
        }

        composable(
            route = Screen.Downloads.route,
            enterTransition = { fadeIn(tween(ANIM_DURATION)) },
            exitTransition = { fadeOut(tween(ANIM_DURATION)) }
        ) {
            DownloadsScreen(
                onSongClick = {
                    navController.navigate(Screen.NowPlaying.route)
                }
            )
        }

        composable(
            route = Screen.NowPlaying.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(ANIM_DURATION)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(ANIM_DURATION)
                )
            },
            popEnterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(ANIM_DURATION)
                )
            },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(ANIM_DURATION)
                )
            }
        ) {
            NowPlayingScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBackClick = { navController.popBackStack() },
                onSongClick = {
                    navController.navigate(Screen.NowPlaying.route)
                }
            )
        }
    }
}