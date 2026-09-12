package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CameraPreset
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CameraSurface
import com.example.ui.theme.CameraSurfaceBorder
import com.example.ui.theme.CameraSurfaceElevated
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraTextSecondary
import com.example.ui.theme.LeicaRed
import com.example.ui.theme.PeakingGreen

enum class ProParameter {
    NONE, ISO, SHUTTER, EV, FOCUS, WB, ASSISTANTS, PRESETS
}

@Composable
fun ProControlsBar(
    modifier: Modifier = Modifier,
    currentIso: Int,
    onIsoChanged: (Int) -> Unit,
    currentShutter: String,
    onShutterChanged: (String) -> Unit,
    currentEv: Float,
    onEvChanged: (Float) -> Unit,
    currentWb: String,
    onWbChanged: (String) -> Unit,
    wbKelvin: Int,
    onWbKelvinChanged: (Int) -> Unit,
    isManualFocus: Boolean,
    onManualFocusToggle: (Boolean) -> Unit,
    focusDistance: Float,
    onFocusDistanceChanged: (Float) -> Unit,
    isRawEnabled: Boolean,
    onRawToggle: () -> Unit,
    showHistogram: Boolean,
    onHistogramToggle: () -> Unit,
    showLevel: Boolean,
    onLevelToggle: () -> Unit,
    showPeaking: Boolean,
    onPeakingToggle: () -> Unit,
    showZebra: Boolean,
    onZebraToggle: () -> Unit,
    presets: List<CameraPreset>,
    onLoadPreset: (CameraPreset) -> Unit,
    onSavePreset: (String) -> Unit
) {
    var activeParam by remember { mutableStateOf(ProParameter.NONE) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    val isoOptions = listOf(0, 50, 100, 200, 400, 800, 1600, 3200, 6400)
    val shutterOptions = listOf(
        "Auto", "1/8000s", "1/4000s", "1/2000s", "1/1000s",
        "1/500s", "1/250s", "1/125s", "1/60s", "1/30s",
        "1/15s", "1/4s", "1s", "2s", "4s", "8s", "15s", "30s"
    )
    val wbOptions = listOf("Auto", "3000K", "4000K", "5500K", "6500K", "7500K")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xEE12151B))
            .testTag("pro_controls_bar")
    ) {
        // Expanded Parameter Sub-dial / Slider
        AnimatedVisibility(visible = activeParam != ProParameter.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CameraSurfaceElevated)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (activeParam) {
                    ProParameter.ISO -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            isoOptions.forEach { iso ->
                                val selected = currentIso == iso
                                ParameterPill(
                                    label = if (iso == 0) "Auto" else "$iso",
                                    isSelected = selected,
                                    onClick = { onIsoChanged(iso) },
                                    testTag = "iso_pill_$iso"
                                )
                            }
                        }
                    }

                    ProParameter.SHUTTER -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            shutterOptions.forEach { s ->
                                val selected = currentShutter == s
                                ParameterPill(
                                    label = s,
                                    isSelected = selected,
                                    onClick = { onShutterChanged(s) },
                                    testTag = "shutter_pill_$s"
                                )
                            }
                        }
                    }

                    ProParameter.EV -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Exposure Value", color = CameraTextSecondary, fontSize = 12.sp)
                                Text(
                                    text = String.format("%+.1f EV", currentEv),
                                    color = AmberGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = currentEv,
                                onValueChange = onEvChanged,
                                valueRange = -3.0f..3.0f,
                                steps = 17,
                                colors = SliderDefaults.colors(
                                    thumbColor = AmberGold,
                                    activeTrackColor = AmberGold,
                                    inactiveTrackColor = Color(0xFF374151)
                                ),
                                modifier = Modifier.testTag("ev_slider")
                            )
                        }
                    }

                    ProParameter.FOCUS -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ParameterPill(
                                        label = "AF",
                                        isSelected = !isManualFocus,
                                        onClick = { onManualFocusToggle(false) },
                                        testTag = "focus_af_pill"
                                    )
                                    ParameterPill(
                                        label = "MF",
                                        isSelected = isManualFocus,
                                        onClick = { onManualFocusToggle(true) },
                                        testTag = "focus_mf_pill"
                                    )
                                }
                                Text(
                                    text = if (isManualFocus) "Distance: ${(focusDistance * 100).toInt()}%" else "Continuous Auto",
                                    color = AmberGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (isManualFocus) {
                                Slider(
                                    value = focusDistance,
                                    onValueChange = onFocusDistanceChanged,
                                    valueRange = 0.0f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PeakingGreen,
                                        activeTrackColor = PeakingGreen,
                                        inactiveTrackColor = Color(0xFF374151)
                                    ),
                                    modifier = Modifier.testTag("focus_distance_slider")
                                )
                            }
                        }
                    }

                    ProParameter.WB -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                wbOptions.forEach { wb ->
                                    val selected = currentWb == wb
                                    ParameterPill(
                                        label = wb,
                                        isSelected = selected,
                                        onClick = {
                                            onWbChanged(wb)
                                            if (wb.contains("K")) {
                                                onWbKelvinChanged(wb.replace("K", "").toIntOrNull() ?: 5500)
                                            }
                                        },
                                        testTag = "wb_pill_$wb"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Kelvin Tuning", color = CameraTextSecondary, fontSize = 11.sp)
                                Text("${wbKelvin}K", color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = wbKelvin.toFloat(),
                                onValueChange = { onWbKelvinChanged(it.toInt()) },
                                valueRange = 2500f..9500f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AmberGold,
                                    activeTrackColor = AmberGold,
                                    inactiveTrackColor = Color(0xFF374151)
                                ),
                                modifier = Modifier.testTag("wb_kelvin_slider")
                            )
                        }
                    }

                    ProParameter.ASSISTANTS -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ParameterPill(
                                label = "Histogram",
                                isSelected = showHistogram,
                                onClick = onHistogramToggle,
                                testTag = "toggle_histogram"
                            )
                            ParameterPill(
                                label = "Horizon Level",
                                isSelected = showLevel,
                                onClick = onLevelToggle,
                                testTag = "toggle_level"
                            )
                            ParameterPill(
                                label = "Focus Peaking",
                                isSelected = showPeaking,
                                onClick = onPeakingToggle,
                                testTag = "toggle_peaking"
                            )
                            ParameterPill(
                                label = "Zebra Stripes",
                                isSelected = showZebra,
                                onClick = onZebraToggle,
                                testTag = "toggle_zebra"
                            )
                            ParameterPill(
                                label = if (isRawEnabled) "RAW DNG" else "JPEG",
                                isSelected = isRawEnabled,
                                onClick = onRawToggle,
                                testTag = "toggle_raw"
                            )
                        }
                    }

                    ProParameter.PRESETS -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pro Presets", color = CameraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Button(
                                    onClick = { showSavePresetDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                                    modifier = Modifier.testTag("save_preset_button")
                                ) {
                                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Current", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presets.forEach { p ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF262C38))
                                            .clickable { onLoadPreset(p) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                            .testTag("preset_item_${p.id}")
                                    ) {
                                        Column {
                                            Text(p.name, color = AmberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("ISO ${p.iso} • ${p.shutterSpeed} • ${p.whiteBalanceKelvin}K", color = CameraTextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }

        // Horizontal Pro Dials Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DialButton(
                title = "ISO",
                value = if (currentIso == 0) "Auto" else "$currentIso",
                isActive = activeParam == ProParameter.ISO,
                onClick = { activeParam = if (activeParam == ProParameter.ISO) ProParameter.NONE else ProParameter.ISO },
                testTag = "dial_iso"
            )
            DialButton(
                title = "S",
                value = currentShutter,
                isActive = activeParam == ProParameter.SHUTTER,
                onClick = { activeParam = if (activeParam == ProParameter.SHUTTER) ProParameter.NONE else ProParameter.SHUTTER },
                testTag = "dial_shutter"
            )
            DialButton(
                title = "EV",
                value = String.format("%+.1f", currentEv),
                isActive = activeParam == ProParameter.EV,
                onClick = { activeParam = if (activeParam == ProParameter.EV) ProParameter.NONE else ProParameter.EV },
                testTag = "dial_ev"
            )
            DialButton(
                title = "FOCUS",
                value = if (isManualFocus) "MF" else "AF",
                isActive = activeParam == ProParameter.FOCUS,
                onClick = { activeParam = if (activeParam == ProParameter.FOCUS) ProParameter.NONE else ProParameter.FOCUS },
                testTag = "dial_focus"
            )
            DialButton(
                title = "WB",
                value = if (currentWb == "Auto") "Auto" else "${wbKelvin}K",
                isActive = activeParam == ProParameter.WB,
                onClick = { activeParam = if (activeParam == ProParameter.WB) ProParameter.NONE else ProParameter.WB },
                testTag = "dial_wb"
            )
            DialButton(
                title = "ASSIST",
                value = if (showPeaking || showZebra || showHistogram) "ON" else "OFF",
                isActive = activeParam == ProParameter.ASSISTANTS,
                onClick = { activeParam = if (activeParam == ProParameter.ASSISTANTS) ProParameter.NONE else ProParameter.ASSISTANTS },
                testTag = "dial_assistants"
            )
            DialButton(
                title = "PRESETS",
                value = "${presets.size}",
                isActive = activeParam == ProParameter.PRESETS,
                onClick = { activeParam = if (activeParam == ProParameter.PRESETS) ProParameter.NONE else ProParameter.PRESETS },
                testTag = "dial_presets"
            )
        }
    }

    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Manual Preset", color = CameraTextPrimary) },
            text = {
                Column {
                    Text("Store current ISO, Shutter, EV, Focus and WB parameters.", color = CameraTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("Preset Name") },
                        placeholder = { Text("e.g., Midnight Long Exp") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preset_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            onSavePreset(newPresetName.trim())
                            newPresetName = ""
                            showSavePresetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_save_preset")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSavePresetDialog = false }) {
                    Text("Cancel", color = CameraTextSecondary)
                }
            },
            containerColor = CameraSurface
        )
    }
}

@Composable
private fun DialButton(
    title: String,
    value: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) AmberGold else Color(0xFF1E232B))
            .border(1.dp, if (isActive) AmberGold else CameraSurfaceBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = if (isActive) Color.Black else CameraTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                color = if (isActive) Color.Black else CameraTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ParameterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) AmberGold else Color(0xFF232832))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else CameraTextPrimary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
