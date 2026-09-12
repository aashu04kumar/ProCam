package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.data.WatermarkConfig
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CameraBackground
import com.example.ui.theme.CameraSurface
import com.example.ui.theme.CameraSurfaceElevated
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraTextSecondary

@Composable
fun SettingsScreen(
    watermarkConfig: WatermarkConfig,
    onWatermarkConfigChanged: (WatermarkConfig) -> Unit,
    isSoundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    var isGeotaggingOn by remember { mutableStateOf(true) }
    var isRawJpegSyncOn by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf(watermarkConfig.customText) }
    var isLeicaStrip by remember { mutableStateOf(watermarkConfig.isLeicaStyle) }
    var includeDate by remember { mutableStateOf(watermarkConfig.includeDateTime) }
    var isWatermarkEnabled by remember { mutableStateOf(watermarkConfig.enabled) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBackground)
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "CAMERA PREFERENCES",
                    color = CameraTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Watermark Customization Card
                SettingsCard(title = "Signature & Watermark") {
                    SettingsToggleRow(
                        title = "Enable Photo Watermark",
                        subtitle = "Stamp device & optics signature on capture",
                        checked = isWatermarkEnabled,
                        onCheckedChange = {
                            isWatermarkEnabled = it
                            onWatermarkConfigChanged(watermarkConfig.copy(enabled = it))
                        }
                    )
                    if (isWatermarkEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = watermarkText,
                            onValueChange = {
                                watermarkText = it
                                onWatermarkConfigChanged(watermarkConfig.copy(customText = it))
                            },
                            label = { Text("Watermark Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("watermark_text_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsToggleRow(
                            title = "Leica Iconic Bottom Bar",
                            subtitle = "Clean red dot & optics telemetry border",
                            checked = isLeicaStrip,
                            onCheckedChange = {
                                isLeicaStrip = it
                                onWatermarkConfigChanged(watermarkConfig.copy(isLeicaStyle = it))
                            }
                        )
                        SettingsToggleRow(
                            title = "Include Capture Timestamp",
                            subtitle = "Show ISO-standard date & time",
                            checked = includeDate,
                            onCheckedChange = {
                                includeDate = it
                                onWatermarkConfigChanged(watermarkConfig.copy(includeDateTime = it))
                            }
                        )
                    }
                }

                // General Capture Settings Card
                SettingsCard(title = "Optics & Audio") {
                    SettingsToggleRow(
                        title = "Shutter Sound",
                        subtitle = "Play acoustic mechanical shutter click",
                        checked = isSoundEnabled,
                        onCheckedChange = onSoundToggle
                    )
                    SettingsToggleRow(
                        title = "GPS Geotagging",
                        subtitle = "Record latitude & longitude in EXIF headers",
                        checked = isGeotaggingOn,
                        onCheckedChange = { isGeotaggingOn = it }
                    )
                    SettingsToggleRow(
                        title = "Simultaneous RAW + JPEG",
                        subtitle = "Store uncompressed 14-bit DNG alongside processed JPEG",
                        checked = isRawJpegSyncOn,
                        onCheckedChange = { isRawJpegSyncOn = it }
                    )
                }

                // Privacy & Security Card
                SettingsCard(title = "Privacy & Vault") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Vault Lock PIN", color = CameraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Current: 1234 (Tap to modify)", color = CameraTextSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AmberGold)
                    }
                }

                // Cloud & Storage Card
                SettingsCard(title = "Storage & Sync") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cloud Backup Sync", color = CameraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Local SSD storage: 100% offline & private", color = CameraTextSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = AmberGold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CameraSurface)
            .padding(16.dp)
    ) {
        Text(
            text = title.uppercase(),
            color = AmberGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = CameraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = CameraTextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AmberGold,
                checkedTrackColor = Color(0xFF3E3314),
                uncheckedThumbColor = Color(0xFF888888),
                uncheckedTrackColor = Color(0xFF222733)
            )
        )
    }
}
