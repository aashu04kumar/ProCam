package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.camera.CameraManager
import com.example.data.CapturedMedia
import com.example.data.WatermarkConfig
import com.example.data.repository.MediaRepository
import com.example.ui.components.CameraPermissionHandler
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CameraBackground
import com.example.ui.theme.MyApplicationTheme

enum class AppDestination {
    CAMERA, GALLERY, EDITOR, SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var cameraManager: CameraManager
    private lateinit var repository: MediaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = MediaRepository(applicationContext)
        cameraManager = CameraManager(applicationContext)

        setContent {
            MyApplicationTheme {
                MainAppContent(
                    cameraManager = cameraManager,
                    repository = repository
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.release()
    }
}

@Composable
fun MainAppContent(
    cameraManager: CameraManager,
    repository: MediaRepository
) {
    var currentScreen by remember { mutableStateOf(AppDestination.CAMERA) }
    var activeEditingMedia by remember { mutableStateOf<CapturedMedia?>(null) }
    var watermarkConfig by remember { mutableStateOf(WatermarkConfig()) }
    var isSoundEnabled by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CameraBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppDestination.CAMERA -> {
                    // Handle camera & storage permissions gracefully before CameraX session begins
                    CameraPermissionHandler {
                        CameraScreen(
                            cameraManager = cameraManager,
                            repository = repository,
                            watermarkConfig = watermarkConfig,
                            isSoundEnabled = isSoundEnabled,
                            onNavigateToGallery = { currentScreen = AppDestination.GALLERY },
                            onNavigateToSettings = { currentScreen = AppDestination.SETTINGS },
                            onNavigateToEditor = { media ->
                                activeEditingMedia = media
                                currentScreen = AppDestination.EDITOR
                            }
                        )
                    }
                }

                AppDestination.GALLERY -> {
                    GalleryScreen(
                        repository = repository,
                        watermarkConfig = watermarkConfig,
                        onNavigateBack = { currentScreen = AppDestination.CAMERA },
                        onNavigateToEditor = { media ->
                            activeEditingMedia = media
                            currentScreen = AppDestination.EDITOR
                        }
                    )
                }

                AppDestination.EDITOR -> {
                    activeEditingMedia?.let { media ->
                        EditorScreen(
                            media = media,
                            repository = repository,
                            onNavigateBack = { currentScreen = AppDestination.GALLERY },
                            onSaved = { currentScreen = AppDestination.GALLERY }
                        )
                    } ?: run {
                        currentScreen = AppDestination.CAMERA
                    }
                }

                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        watermarkConfig = watermarkConfig,
                        onWatermarkConfigChanged = { watermarkConfig = it },
                        isSoundEnabled = isSoundEnabled,
                        onSoundToggle = { isSoundEnabled = it },
                        onNavigateBack = { currentScreen = AppDestination.CAMERA }
                    )
                }
            }
        }
    }
}
