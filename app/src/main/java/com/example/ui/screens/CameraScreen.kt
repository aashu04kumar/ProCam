package com.example.ui.screens

import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.HdrAuto
import androidx.compose.material.icons.filled.HdrOff
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ai.AiProcessingEngine
import com.example.camera.CameraManager
import com.example.data.AspectRatioOption
import com.example.data.CameraMode
import com.example.data.CameraPreset
import com.example.data.CapturedMedia
import com.example.data.GridType
import com.example.data.LensType
import com.example.data.WatermarkConfig
import com.example.data.repository.MediaRepository
import com.example.filter.FilterEngine
import com.example.filter.PhotoFilter
import com.example.ui.components.CameraViewfinder
import com.example.ui.components.ModeSelectorBar
import com.example.ui.components.ProControlsBar
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CameraBackground
import com.example.ui.theme.CameraSurfaceElevated
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraTextSecondary
import com.example.ui.theme.LeicaRed
import com.example.ui.theme.PeakingGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraScreen(
    cameraManager: CameraManager,
    repository: MediaRepository,
    watermarkConfig: WatermarkConfig,
    isSoundEnabled: Boolean,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditor: (CapturedMedia) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Camera State
    var currentMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var selectedLens by remember { mutableStateOf(LensType.WIDE) }
    var aspectRatio by remember { mutableStateOf(AspectRatioOption.RATIO_4_3) }
    var isHdrOn by remember { mutableStateOf(true) }
    var isRawOn by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var countdownRemaining by remember { mutableIntStateOf(0) }
    var isCapturing by remember { mutableStateOf(false) }
    var gridType by remember { mutableStateOf(GridType.RULE_OF_THIRDS) }

    // Pro Parameters State
    var proIso by remember { mutableIntStateOf(100) }
    var proShutter by remember { mutableStateOf("1/125s") }
    var proEv by remember { mutableFloatStateOf(0.0f) }
    var proWb by remember { mutableStateOf("5500K") }
    var proWbKelvin by remember { mutableIntStateOf(5500) }
    var isManualFocus by remember { mutableStateOf(false) }
    var focusDistance by remember { mutableFloatStateOf(0.5f) }
    var showHistogram by remember { mutableStateOf(true) }
    var showLevel by remember { mutableStateOf(true) }
    var showPeaking by remember { mutableStateOf(false) }
    var showZebra by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf<PhotoFilter?>(null) }

    // Sensor & Video state from CameraManager
    val isHardwareAvailable by cameraManager.isHardwareCameraAvailable.collectAsState()
    val roll by cameraManager.rollDegrees.collectAsState()
    val pitch by cameraManager.pitchDegrees.collectAsState()
    val isRecording by cameraManager.isRecording.collectAsState()
    val recDuration by cameraManager.recordingDurationSec.collectAsState()
    val audioDb by cameraManager.audioAmpDb.collectAsState()
    val isFlashOn by cameraManager.isFlashOn.collectAsState()
    val presets by repository.presets.collectAsState(initial = emptyList())
    val publicMediaList by repository.publicMedia.collectAsState(initial = emptyList())
    val latestCapture = publicMediaList.firstOrNull()

    // AI Scene and composition advice
    val aiScene = remember(currentMode, roll) {
        AiProcessingEngine.analyzeScene(
            luminanceAvg = if (currentMode == CameraMode.NIGHT) 0.15f else 0.5f,
            warmthRatio = if (currentMode == CameraMode.FOOD) 1.4f else 1.0f,
            isFaceDetected = currentMode == CameraMode.PORTRAIT,
            mode = currentMode
        )
    }
    val compositionAdvice = remember(roll, pitch, currentMode) {
        AiProcessingEngine.getCompositionAdvice(roll, pitch, currentMode == CameraMode.PORTRAIT)
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    val isVideoMode = currentMode == CameraMode.VIDEO ||
                      currentMode == CameraMode.SLOW_MO ||
                      currentMode == CameraMode.TIME_LAPSE

    LaunchedEffect(isVideoMode) {
        cameraManager.setVideoMode(isVideoMode)
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        cameraManager.bindCamera(lifecycleOwner, previewView, isVideoMode = isVideoMode)
        cameraManager.startSensors()
        onDispose {
            cameraManager.stopSensors()
            cameraManager.unbindCamera()
        }
    }

    // Capture Photo Trigger
    fun executeCapture() {
        if (isCapturing) return
        isCapturing = true

        coroutineScope.launch {
            if (timerSeconds > 0) {
                countdownRemaining = timerSeconds
                while (countdownRemaining > 0) {
                    delay(1000)
                    countdownRemaining--
                }
            }

            cameraManager.playShutterSound(isSoundEnabled)

            cameraManager.captureImage(
                onSuccess = { rawBmp ->
                    coroutineScope.launch {
                        // Apply active filter or mode enhancement
                        val processedBmp = when {
                            currentMode == CameraMode.PORTRAIT -> {
                                AiProcessingEngine.applyPortraitBokeh(rawBmp, 0.6f)
                            }
                            currentMode == CameraMode.NIGHT -> {
                                AiProcessingEngine.applyLowLightEnhancement(rawBmp)
                            }
                            isHdrOn -> {
                                AiProcessingEngine.applyAutoHdr(rawBmp)
                            }
                            activeFilter != null -> {
                                FilterEngine.applyAdjustmentsAndFilter(rawBmp, activeFilter, com.example.filter.EditAdjustments())
                            }
                            else -> rawBmp
                        }

                        val saved = repository.saveCapturedPhoto(
                            bitmap = processedBmp,
                            mode = currentMode.title,
                            iso = if (proIso == 0) 100 else proIso,
                            shutterSpeed = proShutter,
                            whiteBalance = "${proWbKelvin}K",
                            ev = proEv,
                            isRaw = isRawOn,
                            filterName = activeFilter?.name ?: "Original",
                            watermarkConfig = watermarkConfig
                        )
                        isCapturing = false
                        snackbarHostState.showSnackbar("Saved: ${saved.fileName}")
                    }
                },
                onError = { exc ->
                    isCapturing = false
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Capture failed: ${exc.message}")
                    }
                }
            )
        }
    }

    // Video Recording Trigger
    fun toggleRecording() {
        if (isRecording) {
            cameraManager.stopRecording()
        } else {
            val videoFile = File(context.filesDir, "VID_${System.currentTimeMillis()}.mp4")
            cameraManager.startRecording(
                outputFile = videoFile,
                onFinished = { file ->
                    coroutineScope.launch {
                        repository.saveVideo(file, currentMode.title, "4K 60fps")
                        snackbarHostState.showSnackbar("Video saved: ${file.name}")
                    }
                },
                onError = { err ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Recording error: $err")
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBackground)
            .testTag("camera_screen")
    ) {
        // Main Viewfinder
        CameraViewfinder(
            modifier = Modifier.fillMaxSize(),
            previewView = previewView,
            isHardwareCameraAvailable = isHardwareAvailable,
            gridType = gridType,
            showLevelIndicator = showLevel,
            showHistogram = showHistogram,
            showFocusPeaking = showPeaking,
            showZebraStripes = showZebra,
            rollDegrees = roll,
            pitchDegrees = pitch,
            isRecording = isRecording,
            audioDb = audioDb,
            aiScene = aiScene,
            compositionAdvice = compositionAdvice,
            onTapToFocus = { x, y, w, h ->
                cameraManager.triggerFocus(x, y, w, h)
            }
        )

        // Countdown Timer Overlay
        if (countdownRemaining > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x77000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$countdownRemaining",
                    color = AmberGold,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Top Controls Bar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xAA0E1116), RoundedCornerShape(24.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash Toggle
                IconButton(
                    onClick = { cameraManager.toggleFlash() },
                    modifier = Modifier.testTag("flash_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isFlashOn) AmberGold else Color.White
                    )
                }

                // HDR Toggle
                IconButton(
                    onClick = { isHdrOn = !isHdrOn },
                    modifier = Modifier.testTag("hdr_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isHdrOn) Icons.Default.HdrOn else Icons.Default.HdrOff,
                        contentDescription = "HDR",
                        tint = if (isHdrOn) AmberGold else CameraTextSecondary
                    )
                }

                // RAW / DNG Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isRawOn) AmberGold else Color(0x33FFFFFF))
                        .clickable { isRawOn = !isRawOn }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("raw_toggle_button")
                ) {
                    Text(
                        text = "RAW",
                        color = if (isRawOn) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Aspect Ratio
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable {
                            aspectRatio = when (aspectRatio) {
                                AspectRatioOption.RATIO_4_3 -> AspectRatioOption.RATIO_16_9
                                AspectRatioOption.RATIO_16_9 -> AspectRatioOption.RATIO_1_1
                                AspectRatioOption.RATIO_1_1 -> AspectRatioOption.RATIO_FULL
                                AspectRatioOption.RATIO_FULL -> AspectRatioOption.RATIO_4_3
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("aspect_ratio_button")
                ) {
                    Text(
                        text = aspectRatio.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Timer Toggle
                IconButton(
                    onClick = {
                        timerSeconds = when (timerSeconds) {
                            0 -> 3
                            3 -> 10
                            else -> 0
                        }
                    },
                    modifier = Modifier.testTag("timer_toggle_button")
                ) {
                    Icon(
                        imageVector = if (timerSeconds > 0) Icons.Default.Timer3 else Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = if (timerSeconds > 0) AmberGold else Color.White
                    )
                }

                // Settings
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }

        // Lens Switcher / Zoom Selector (Above mode selector)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (currentMode == CameraMode.PRO) 210.dp else 145.dp)
                .background(Color(0x9910131A), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("lens_switcher_bar")
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LensType.entries.forEach { lens ->
                    val isSelected = selectedLens == lens
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AmberGold else Color(0x33252A36))
                            .clickable {
                                selectedLens = lens
                                cameraManager.setZoom(lens.zoomFactor)
                            }
                            .testTag("lens_button_${lens.label}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lens.label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Live Filter Selection Sheet
        AnimatedVisibility(
            visible = showFilterSheet,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 190.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xF0141822), RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .testTag("live_filter_carousel")
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Color Grading & LUT Filters", color = AmberGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = activeFilter?.name ?: "Original",
                            color = CameraTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterEngine.filters.forEach { flt ->
                            val isChosen = activeFilter?.id == flt.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isChosen) AmberGold else Color(0xFF222834))
                                    .clickable {
                                        activeFilter = if (flt.id == "normal") null else flt
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("filter_option_${flt.id}")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = flt.name,
                                        color = if (isChosen) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = flt.category,
                                        color = if (isChosen) Color(0xFF222222) else CameraTextSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pro Controls HUD (if PRO mode active)
        if (currentMode == CameraMode.PRO) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
            ) {
                ProControlsBar(
                    currentIso = proIso,
                    onIsoChanged = { proIso = it },
                    currentShutter = proShutter,
                    onShutterChanged = { proShutter = it },
                    currentEv = proEv,
                    onEvChanged = {
                        proEv = it
                        cameraManager.setExposure((it * 3).toInt())
                    },
                    currentWb = proWb,
                    onWbChanged = { proWb = it },
                    wbKelvin = proWbKelvin,
                    onWbKelvinChanged = { proWbKelvin = it },
                    isManualFocus = isManualFocus,
                    onManualFocusToggle = { isManualFocus = it },
                    focusDistance = focusDistance,
                    onFocusDistanceChanged = { focusDistance = it },
                    isRawEnabled = isRawOn,
                    onRawToggle = { isRawOn = !isRawOn },
                    showHistogram = showHistogram,
                    onHistogramToggle = { showHistogram = !showHistogram },
                    showLevel = showLevel,
                    onLevelToggle = { showLevel = !showLevel },
                    showPeaking = showPeaking,
                    onPeakingToggle = { showPeaking = !showPeaking },
                    showZebra = showZebra,
                    onZebraToggle = { showZebra = !showZebra },
                    presets = presets,
                    onLoadPreset = { preset ->
                        proIso = preset.iso
                        proShutter = preset.shutterSpeed
                        proEv = preset.evCompensation
                        proWbKelvin = preset.whiteBalanceKelvin
                        focusDistance = preset.focusDistance
                    },
                    onSavePreset = { name ->
                        coroutineScope.launch {
                            repository.insertPreset(
                                CameraPreset(
                                    name = name,
                                    iso = proIso,
                                    shutterSpeed = proShutter,
                                    evCompensation = proEv,
                                    whiteBalanceKelvin = proWbKelvin,
                                    focusDistance = focusDistance,
                                    lens = selectedLens.label
                                )
                            )
                            snackbarHostState.showSnackbar("Preset '$name' saved")
                        }
                    }
                )
            }
        }

        // Mode Selector Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        ) {
            ModeSelectorBar(
                currentMode = currentMode,
                onModeSelected = { currentMode = it }
            )
        }

        // Bottom Action Bar: Gallery Preview, Shutter Button, Camera Switch
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(90.dp)
                .background(CameraBackground)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Thumbnail / Quick Jump
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .border(2.dp, AmberGold, CircleShape)
                        .clickable(onClick = onNavigateToGallery)
                        .testTag("gallery_thumbnail_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (latestCapture != null) {
                        AsyncImage(
                            model = File(latestCapture.filePath),
                            contentDescription = "Recent Capture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color.White
                        )
                    }
                }

                // Shutter Button
                val isVideoMode = currentMode in listOf(CameraMode.VIDEO, CameraMode.SLOW_MO, CameraMode.TIME_LAPSE)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(3.dp, if (isVideoMode) LeicaRed else Color.White, CircleShape)
                        .padding(5.dp)
                        .clickable {
                            if (isVideoMode) {
                                toggleRecording()
                            } else {
                                executeCapture()
                            }
                        }
                        .testTag("master_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = AmberGold,
                            modifier = Modifier.size(54.dp),
                            strokeWidth = 3.dp
                        )
                    } else if (isRecording) {
                        // Square record stop indicator
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(LeicaRed)
                        )
                    } else {
                        // Solid inner circle
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(if (isVideoMode) LeicaRed else Color.White)
                        )
                    }
                }

                // Right button: Filter Carousel or Camera Switch
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showFilterSheet = !showFilterSheet },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (showFilterSheet) AmberGold else Color(0xFF1E232E))
                            .testTag("filter_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterVintage,
                            contentDescription = "Filters",
                            tint = if (showFilterSheet) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { cameraManager.toggleCamera(lifecycleOwner, previewView) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E232E))
                            .testTag("camera_switch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Snackbar host for user feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
    }
}
