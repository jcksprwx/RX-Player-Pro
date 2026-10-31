package com.rxplayer.pro.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.rxplayer.pro.core.security.BiometricAuthManager
import com.rxplayer.pro.core.security.SecureStorage
import com.rxplayer.pro.ui.components.NovaBottomBar
import com.rxplayer.pro.ui.screens.about.AboutScreen
import com.rxplayer.pro.ui.screens.audioextract.AudioExtractScreen
import com.rxplayer.pro.ui.screens.cutter.VideoCutterScreen
import com.rxplayer.pro.ui.screens.equalizer.EqualizerScreen
import com.rxplayer.pro.ui.screens.folders.FolderDetailScreen
import com.rxplayer.pro.ui.screens.folders.FoldersScreen
import com.rxplayer.pro.ui.screens.home.HomeScreen
import com.rxplayer.pro.ui.screens.music.MusicPlayerScreen
import com.rxplayer.pro.ui.screens.music.MusicScreen
import com.rxplayer.pro.ui.screens.network.AddServerScreen
import com.rxplayer.pro.ui.screens.network.NetworkScreen
import com.rxplayer.pro.ui.screens.onboarding.OnboardingScreen
import com.rxplayer.pro.ui.screens.permission.StoragePermissionScreen
import com.rxplayer.pro.ui.screens.player.VideoPlayerScreen
import com.rxplayer.pro.ui.screens.playlists.PlaylistDetailScreen
import com.rxplayer.pro.ui.screens.playlists.PlaylistsScreen
import com.rxplayer.pro.ui.screens.privacy.AppLockScreen
import com.rxplayer.pro.ui.screens.privacy.PrivacyPolicyScreen
import com.rxplayer.pro.ui.screens.settings.SettingsScreen
import com.rxplayer.pro.ui.screens.splash.SplashScreen
import java.net.URLDecoder

@Composable
fun NovaNavGraph(
    navController: NavHostController,
    secureStorage: SecureStorage,
    biometricAuthManager: BiometricAuthManager,
    hasStoragePermission: Boolean
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = listOf(
        Screen.Home.route,
        Screen.Videos.route,
        Screen.Folders.route,
        Screen.Music.route,
        Screen.Network.route,
        Screen.Playlists.route
    )

    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NovaBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onSplashFinished = {
                            if (secureStorage.isAppLockEnabled() && biometricAuthManager.canAuthenticate()) {
                                navController.navigate(Screen.AppLock.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            } else if (!hasStoragePermission) {
                                navController.navigate(Screen.Permission.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.AppLock.route) {
                    AppLockScreen(
                        biometricAuthManager = biometricAuthManager,
                        onUnlocked = {
                            if (!hasStoragePermission) {
                                navController.navigate(Screen.Permission.route) {
                                    popUpTo(Screen.AppLock.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.AppLock.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onGetStarted = {
                            if (!hasStoragePermission) {
                                navController.navigate(Screen.Permission.route)
                            } else {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.Permission.route) {
                    StoragePermissionScreen(
                        onPermissionGranted = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Permission.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToVideos = { navController.navigate(Screen.Videos.route) },
                        onNavigateToFolders = { navController.navigate(Screen.Folders.route) },
                        onNavigateToMusic = { navController.navigate(Screen.Music.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onPlayVideo = { mediaId ->
                            navController.navigate(Screen.VideoPlayer.createRoute(mediaId))
                        }
                    )
                }

                composable(Screen.Videos.route) {
                    com.rxplayer.pro.ui.screens.videos.VideosScreen(
                        onPlayVideo = { mediaId ->
                            navController.navigate(Screen.VideoPlayer.createRoute(mediaId))
                        },
                        onTrimVideo = { mediaId ->
                            navController.navigate(Screen.VideoCutter.createRoute(mediaId))
                        },
                        onExtractAudio = { mediaId ->
                            navController.navigate(Screen.AudioExtract.createRoute(mediaId))
                        }
                    )
                }

                composable(Screen.Folders.route) {
                    FoldersScreen(
                        onNavigateToFolder = { path, name ->
                            navController.navigate(Screen.FolderDetail.createRoute(path, name))
                        }
                    )
                }

                composable(
                    route = Screen.FolderDetail.route,
                    arguments = listOf(
                        navArgument("folderPath") { type = NavType.StringType },
                        navArgument("folderName") { type = NavType.StringType }
                    )
                ) { backStack ->
                    val path = URLDecoder.decode(backStack.arguments?.getString("folderPath") ?: "", "UTF-8")
                    val name = URLDecoder.decode(backStack.arguments?.getString("folderName") ?: "", "UTF-8")
                    FolderDetailScreen(
                        folderPath = path,
                        folderName = name,
                        onBackClick = { navController.popBackStack() },
                        onPlayVideo = { mediaId ->
                            navController.navigate(Screen.VideoPlayer.createRoute(mediaId))
                        },
                        onTrimVideo = { mediaId ->
                            navController.navigate(Screen.VideoCutter.createRoute(mediaId))
                        },
                        onExtractAudio = { mediaId ->
                            navController.navigate(Screen.AudioExtract.createRoute(mediaId))
                        }
                    )
                }

                composable(Screen.Music.route) {
                    MusicScreen(
                        onNavigateToPlayer = { navController.navigate(Screen.MusicPlayer.route) },
                        onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) }
                    )
                }

                composable(Screen.MusicPlayer.route) {
                    MusicPlayerScreen(
                        onBackClick = { navController.popBackStack() },
                        onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) }
                    )
                }

                composable(
                    route = Screen.VideoPlayer.route,
                    arguments = listOf(navArgument("mediaId") { type = NavType.LongType }),
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(200))
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(200))
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
                    }
                ) { backStack ->
                    val mediaId = backStack.arguments?.getLong("mediaId") ?: 0L
                    VideoPlayerScreen(
                        mediaId = mediaId,
                        onBackClick = { navController.popBackStack() },
                        onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) }
                    )
                }

                composable(Screen.Equalizer.route) {
                    EqualizerScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Network.route) {
                    NetworkScreen(
                        onNavigateToAddServer = { navController.navigate(Screen.AddServer.route) }
                    )
                }

                composable(Screen.AddServer.route) {
                    AddServerScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Playlists.route) {
                    PlaylistsScreen(
                        onNavigateToDetail = { id, name ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(id, name))
                        }
                    )
                }

                composable(
                    route = Screen.PlaylistDetail.route,
                    arguments = listOf(
                        navArgument("playlistId") { type = NavType.LongType },
                        navArgument("playlistName") { type = NavType.StringType }
                    )
                ) { backStack ->
                    val id = backStack.arguments?.getLong("playlistId") ?: 0L
                    val name = URLDecoder.decode(backStack.arguments?.getString("playlistName") ?: "", "UTF-8")
                    PlaylistDetailScreen(
                        playlistId = id,
                        playlistName = name,
                        onBackClick = { navController.popBackStack() },
                        onPlayMedia = { mediaId ->
                            navController.navigate(Screen.VideoPlayer.createRoute(mediaId))
                        }
                    )
                }

                composable(
                    route = Screen.VideoCutter.route,
                    arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
                ) { backStack ->
                    val mediaId = backStack.arguments?.getLong("mediaId") ?: 0L
                    VideoCutterScreen(
                        mediaId = mediaId,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.AudioExtract.route,
                    arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
                ) { backStack ->
                    val mediaId = backStack.arguments?.getLong("mediaId") ?: 0L
                    AudioExtractScreen(
                        mediaId = mediaId,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBackClick = { navController.popBackStack() },
                        onNavigateToAbout = { navController.navigate(Screen.About.route) },
                        onNavigateToPrivacy = { navController.navigate(Screen.PrivacyPolicy.route) },
                        onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) }
                    )
                }

                composable(Screen.About.route) {
                    AboutScreen(onBackClick = { navController.popBackStack() })
                }

                composable(Screen.PrivacyPolicy.route) {
                    PrivacyPolicyScreen(onBackClick = { navController.popBackStack() })
                }
            }
        }
    }
}
