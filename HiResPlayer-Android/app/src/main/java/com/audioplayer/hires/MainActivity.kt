package com.audioplayer.hires

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.playback.HiResAudioService
import com.audioplayer.hires.ui.MainViewModel
import com.audioplayer.hires.ui.StudioScreen
import com.audioplayer.hires.ui.components.StudioMiniPlayer
import com.audioplayer.hires.ui.screens.*
import com.audioplayer.hires.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permissions for Android 13+ and legacy storage
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 101)
        } else {
            // Permissions already granted - auto-scan device storage for music
            viewModel.scanDeviceMusic()
        }

        setContent {
            HiResStudioTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()

                // Android Back Navigation: Handles Android hardware back button and swipe back gesture
                BackHandler(enabled = currentScreen != StudioScreen.LIBRARY) {
                    viewModel.navigateBack()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = StudioColors.Background,
                    bottomBar = {
                        // Only show bottom bar & mini-player when not on full Now Playing screen
                        if (currentScreen != StudioScreen.NOW_PLAYING) {
                            Column(modifier = Modifier.navigationBarsPadding()) {
                                // Docked Persistent Mini Player (visible across all tabs whenever audio is active)
                                StudioMiniPlayer(viewModel)

                                NavigationBar(
                                    containerColor = StudioColors.Surface,
                                    tonalElevation = 6.dp
                                ) {
                                    // 1. Library Tab (Image 4)
                                    NavigationBarItem(
                                        selected = currentScreen == StudioScreen.LIBRARY,
                                        onClick = { viewModel.setScreen(StudioScreen.LIBRARY) },
                                        icon = { Icon(Icons.Default.MusicNote, contentDescription = "Library") },
                                        label = { Text("Library", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = StudioColors.Gold,
                                            selectedTextColor = StudioColors.Gold,
                                            unselectedIconColor = StudioColors.TextMuted,
                                            unselectedTextColor = StudioColors.TextMuted,
                                            indicatorColor = StudioColors.Gold.copy(alpha = 0.15f)
                                        )
                                    )

                                    // 2. Queues Tab
                                    NavigationBarItem(
                                        selected = currentScreen == StudioScreen.QUEUES,
                                        onClick = { viewModel.setScreen(StudioScreen.QUEUES) },
                                        icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queues") },
                                        label = { Text("Queues", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = StudioColors.Gold,
                                            selectedTextColor = StudioColors.Gold,
                                            unselectedIconColor = StudioColors.TextMuted,
                                            unselectedTextColor = StudioColors.TextMuted,
                                            indicatorColor = StudioColors.Gold.copy(alpha = 0.15f)
                                        )
                                    )

                                    // 3. Favorites Tab
                                    NavigationBarItem(
                                        selected = currentScreen == StudioScreen.FAVORITES,
                                        onClick = { viewModel.setScreen(StudioScreen.FAVORITES) },
                                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                                        label = { Text("Favorites", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = StudioColors.Gold,
                                            selectedTextColor = StudioColors.Gold,
                                            unselectedIconColor = StudioColors.TextMuted,
                                            unselectedTextColor = StudioColors.TextMuted,
                                            indicatorColor = StudioColors.Gold.copy(alpha = 0.15f)
                                        )
                                    )

                                    // 4. Settings Tab
                                    NavigationBarItem(
                                        selected = currentScreen == StudioScreen.SETTINGS ||
                                                   currentScreen == StudioScreen.EQUALIZER ||
                                                   currentScreen == StudioScreen.VOCAL_STUDIO ||
                                                   currentScreen == StudioScreen.DEVICE_INSPECTOR,
                                        onClick = { viewModel.setScreen(StudioScreen.SETTINGS) },
                                        icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                                        label = { Text("Settings", fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = StudioColors.Gold,
                                            selectedTextColor = StudioColors.Gold,
                                            unselectedIconColor = StudioColors.TextMuted,
                                            unselectedTextColor = StudioColors.TextMuted,
                                            indicatorColor = StudioColors.Gold.copy(alpha = 0.15f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        when (currentScreen) {
                            StudioScreen.NOW_PLAYING -> NowPlayingScreen(viewModel)
                            StudioScreen.LIBRARY -> LibraryScreen(viewModel)
                            StudioScreen.QUEUES -> QueuesScreen(viewModel)
                            StudioScreen.FAVORITES -> FavoritesScreen(viewModel)
                            StudioScreen.SETTINGS -> SettingsScreen(viewModel)
                            StudioScreen.EQUALIZER -> EqualizerScreen(viewModel)
                            StudioScreen.VOCAL_STUDIO -> VocalStudioScreen(viewModel)
                            StudioScreen.DEVICE_INSPECTOR -> DeviceInspectorScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.scanDeviceMusic(showIndicator = false)
        viewModel.refreshDiagnostics()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            viewModel.scanDeviceMusic()
            viewModel.refreshDiagnostics()
        }
    }
}
