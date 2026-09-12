package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AiProcessingEngine
import com.example.data.CapturedMedia
import com.example.data.repository.MediaRepository
import com.example.filter.EditAdjustments
import com.example.filter.FilterEngine
import com.example.filter.PhotoFilter
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CameraBackground
import com.example.ui.theme.CameraSurface
import com.example.ui.theme.CameraSurfaceElevated
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraTextSecondary
import com.example.ui.theme.LeicaRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class EditorTab(val title: String) {
    FILTERS("Filters & LUTs"),
    ADJUST("Adjust"),
    CROP_ROTATE("Crop / Rotate"),
    AI_MAGIC("AI Magic")
}

@Composable
fun EditorScreen(
    media: CapturedMedia,
    repository: MediaRepository,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var baseBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(EditorTab.FILTERS) }

    // Active Filter
    var activeFilter by remember { mutableStateOf<PhotoFilter?>(null) }

    // Manual Adjustments
    var adjustments by remember { mutableStateOf(EditAdjustments()) }

    // AI states
    var bokehRadius by remember { mutableFloatStateOf(0.5f) }
    var selectedSkyPreset by remember { mutableStateOf("DRAMATIC_SUNSET") }

    // Custom LUT creator values
    var showCustomLutDialog by remember { mutableStateOf(false) }
    var customRedGain by remember { mutableFloatStateOf(1.0f) }
    var customGreenGain by remember { mutableFloatStateOf(1.0f) }
    var customBlueGain by remember { mutableFloatStateOf(1.0f) }
    var customSatGain by remember { mutableFloatStateOf(1.0f) }

    // Load initial image
    LaunchedEffect(media.filePath) {
        withContext(Dispatchers.IO) {
            val bmp = BitmapFactory.decodeFile(media.filePath)
            baseBitmap = bmp
            displayedBitmap = bmp
        }
    }

    // Re-render preview with active adjustments & filter
    fun recomputePreview() {
        val base = baseBitmap ?: return
        coroutineScope.launch(Dispatchers.Default) {
            val processed = FilterEngine.applyAdjustmentsAndFilter(base, activeFilter, adjustments)
            withContext(Dispatchers.Main) {
                displayedBitmap = processed
            }
        }
    }

    LaunchedEffect(activeFilter, adjustments) {
        recomputePreview()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBackground)
            .testTag("editor_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "LITE PRO EDITOR",
                    color = CameraTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = {
                        val currentBmp = displayedBitmap ?: return@Button
                        isSaving = true
                        coroutineScope.launch {
                            repository.saveEditedPhoto(
                                originalMedia = media,
                                editedBitmap = currentBmp,
                                newFilterName = activeFilter?.name ?: "Adjusted"
                            )
                            isSaving = false
                            onSaved()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("save_edited_photo_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Image Preview Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF07090C))
                    .testTag("editor_preview_container")
                    .pointerInput(Unit) {
                        // Healing brush simulation on tap
                        detectTapGestures { offset ->
                            val cur = displayedBitmap ?: return@detectTapGestures
                            coroutineScope.launch(Dispatchers.Default) {
                                val healed = AiProcessingEngine.applyObjectRemovalHeal(cur, offset.x, offset.y, 40f)
                                withContext(Dispatchers.Main) {
                                    displayedBitmap = healed
                                    baseBitmap = healed
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                displayedBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Photo Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: CircularProgressIndicator(color = AmberGold)

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AmberGold)
                    }
                }
            }

            // Editor Category Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = CameraSurfaceElevated,
                contentColor = AmberGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = AmberGold,
                        height = 3.dp
                    )
                }
            ) {
                EditorTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.title,
                                color = if (selectedTab == tab) AmberGold else CameraTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Tab Controls Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(CameraSurface)
                    .navigationBarsPadding()
                    .padding(12.dp)
            ) {
                when (selectedTab) {
                    EditorTab.FILTERS -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Preset LUTs & Films", color = CameraTextSecondary, fontSize = 11.sp)
                                Text(
                                    text = "Reset",
                                    color = LeicaRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { activeFilter = null }
                                        .padding(4.dp)
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
                                    val isSelected = activeFilter?.id == flt.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AmberGold else Color(0xFF222733))
                                            .border(1.dp, if (isSelected) AmberGold else Color(0xFF333B4D), RoundedCornerShape(8.dp))
                                            .clickable {
                                                activeFilter = if (flt.id == "normal") null else flt
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                            .testTag("editor_filter_${flt.id}")
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = flt.name,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = flt.category,
                                                color = if (isSelected) Color(0xFF222222) else CameraTextSecondary,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    EditorTab.ADJUST -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            AdjustmentSlider(
                                label = "Brightness",
                                value = adjustments.brightness,
                                onValueChange = { adjustments = adjustments.copy(brightness = it) },
                                valueRange = -100f..100f
                            )
                            AdjustmentSlider(
                                label = "Contrast",
                                value = adjustments.contrast,
                                onValueChange = { adjustments = adjustments.copy(contrast = it) },
                                valueRange = -100f..100f
                            )
                            AdjustmentSlider(
                                label = "Saturation",
                                value = adjustments.saturation,
                                onValueChange = { adjustments = adjustments.copy(saturation = it) },
                                valueRange = -100f..100f
                            )
                            AdjustmentSlider(
                                label = "Warmth / Temperature",
                                value = adjustments.temperature,
                                onValueChange = { adjustments = adjustments.copy(temperature = it) },
                                valueRange = -100f..100f
                            )
                            AdjustmentSlider(
                                label = "Tint (Green / Magenta)",
                                value = adjustments.tint,
                                onValueChange = { adjustments = adjustments.copy(tint = it) },
                                valueRange = -100f..100f
                            )
                            AdjustmentSlider(
                                label = "Vignette",
                                value = adjustments.vignette,
                                onValueChange = { adjustments = adjustments.copy(vignette = it) },
                                valueRange = 0f..100f
                            )
                            AdjustmentSlider(
                                label = "Film Grain",
                                value = adjustments.grain,
                                onValueChange = { adjustments = adjustments.copy(grain = it) },
                                valueRange = 0f..100f
                            )
                        }
                    }

                    EditorTab.CROP_ROTATE -> {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Transform & Perspective", color = CameraTextSecondary, fontSize = 11.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TransformButton(
                                    label = "Rotate 90°",
                                    icon = Icons.Default.Rotate90DegreesCw,
                                    onClick = {
                                        val nextAngle = (adjustments.rotationAngle + 90f) % 360f
                                        adjustments = adjustments.copy(rotationAngle = nextAngle)
                                    },
                                    testTag = "btn_rotate_90"
                                )
                                TransformButton(
                                    label = "Flip H",
                                    icon = Icons.Default.Flip,
                                    onClick = {
                                        adjustments = adjustments.copy(flipHorizontal = !adjustments.flipHorizontal)
                                    },
                                    testTag = "btn_flip_h"
                                )
                                TransformButton(
                                    label = "Flip V",
                                    icon = Icons.Default.Flip,
                                    onClick = {
                                        adjustments = adjustments.copy(flipVertical = !adjustments.flipVertical)
                                    },
                                    testTag = "btn_flip_v"
                                )
                                TransformButton(
                                    label = "Reset",
                                    icon = Icons.Default.Restore,
                                    onClick = {
                                        adjustments = adjustments.copy(rotationAngle = 0f, flipHorizontal = false, flipVertical = false)
                                    },
                                    testTag = "btn_transform_reset"
                                )
                            }
                        }
                    }

                    EditorTab.AI_MAGIC -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Computational AI Suite", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AiActionButton(
                                    title = "Portrait Bokeh",
                                    subtitle = "Segment & Blur",
                                    onClick = {
                                        val base = baseBitmap ?: return@AiActionButton
                                        isProcessing = true
                                        coroutineScope.launch(Dispatchers.Default) {
                                            val out = AiProcessingEngine.applyPortraitBokeh(base, bokehRadius)
                                            withContext(Dispatchers.Main) {
                                                displayedBitmap = out
                                                baseBitmap = out
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    testTag = "ai_btn_bokeh"
                                )
                                AiActionButton(
                                    title = "Auto HDR",
                                    subtitle = "Recover Range",
                                    onClick = {
                                        val base = baseBitmap ?: return@AiActionButton
                                        isProcessing = true
                                        coroutineScope.launch(Dispatchers.Default) {
                                            val out = AiProcessingEngine.applyAutoHdr(base)
                                            withContext(Dispatchers.Main) {
                                                displayedBitmap = out
                                                baseBitmap = out
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    testTag = "ai_btn_hdr"
                                )
                                AiActionButton(
                                    title = "Night Boost",
                                    subtitle = "Denoise & Lift",
                                    onClick = {
                                        val base = baseBitmap ?: return@AiActionButton
                                        isProcessing = true
                                        coroutineScope.launch(Dispatchers.Default) {
                                            val out = AiProcessingEngine.applyLowLightEnhancement(base)
                                            withContext(Dispatchers.Main) {
                                                displayedBitmap = out
                                                baseBitmap = out
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    testTag = "ai_btn_night"
                                )
                                AiActionButton(
                                    title = "Face Retouch",
                                    subtitle = "Smooth Glow",
                                    onClick = {
                                        val base = baseBitmap ?: return@AiActionButton
                                        isProcessing = true
                                        coroutineScope.launch(Dispatchers.Default) {
                                            val out = AiProcessingEngine.applyFaceRetouch(base)
                                            withContext(Dispatchers.Main) {
                                                displayedBitmap = out
                                                baseBitmap = out
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    testTag = "ai_btn_face"
                                )
                                AiActionButton(
                                    title = "Sharpen Blurry",
                                    subtitle = "Edge Restorer",
                                    onClick = {
                                        val base = baseBitmap ?: return@AiActionButton
                                        isProcessing = true
                                        coroutineScope.launch(Dispatchers.Default) {
                                            val out = AiProcessingEngine.applySharpenBlurry(base)
                                            withContext(Dispatchers.Main) {
                                                displayedBitmap = out
                                                baseBitmap = out
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    testTag = "ai_btn_sharpen"
                                )
                                AiActionButton(
                                    title = "Sky Replace",
                                    subtitle = "Sunset/Galaxy",
                                    onClick = {
                                        val base = baseBitmap ?: return@AiActionButton
                                        isProcessing = true
                                        coroutineScope.launch(Dispatchers.Default) {
                                            val out = AiProcessingEngine.applySkyReplacement(base, selectedSkyPreset)
                                            withContext(Dispatchers.Main) {
                                                displayedBitmap = out
                                                baseBitmap = out
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    testTag = "ai_btn_sky"
                                )
                            }

                            Text("Tip: Tap anywhere on the preview image to use AI Object Eraser healing brush.", color = CameraTextSecondary, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = CameraTextPrimary, fontSize = 11.sp)
            Text("${value.toInt()}", color = AmberGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = AmberGold,
                activeTrackColor = AmberGold,
                inactiveTrackColor = Color(0xFF333B4B)
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}

@Composable
private fun TransformButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF222733))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = AmberGold, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = CameraTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AiActionButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF202634))
            .border(1.dp, Color(0xFF374258), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AmberGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = CameraTextSecondary, fontSize = 9.sp)
        }
    }
}
