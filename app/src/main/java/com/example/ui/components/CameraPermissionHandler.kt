package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.AmberGold
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.CameraBackground
import com.example.ui.theme.CameraSurface
import com.example.ui.theme.CameraSurfaceBorder
import com.example.ui.theme.CameraSurfaceElevated
import com.example.ui.theme.CameraTextDisabled
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraTextSecondary
import com.example.ui.theme.PeakingGreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * CameraPermissionHandler manages runtime permissions for Camera, Storage, and Audio.
 * Ensures permissions are requested and resolved before any CameraX session begins.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionHandler(
    modifier: Modifier = Modifier,
    onAllPermissionsGranted: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionsList = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsList)

    // Re-check permissions when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Trigger refresh if needed
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Callback when all permissions are granted
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onAllPermissionsGranted?.invoke()
        }
    }

    // If all permissions are granted, smoothly render the CameraX session content
    if (permissionsState.allPermissionsGranted) {
        content()
    } else {
        // Otherwise, show the professional permission onboarding view
        CameraPermissionScreen(
            modifier = modifier,
            permissionsState = permissionsState,
            onRequestPermissions = {
                permissionsState.launchMultiplePermissionRequest()
            },
            onOpenSettings = {
                openAppSettings(context)
            }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionScreen(
    modifier: Modifier = Modifier,
    permissionsState: MultiplePermissionsState,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Determine camera and storage specific statuses
    val cameraPermission = permissionsState.permissions.find { it.permission == Manifest.permission.CAMERA }
    val audioPermission = permissionsState.permissions.find { it.permission == Manifest.permission.RECORD_AUDIO }
    val storagePermissions = permissionsState.permissions.filter {
        it.permission == Manifest.permission.READ_MEDIA_IMAGES ||
        it.permission == Manifest.permission.READ_MEDIA_VIDEO ||
        it.permission == Manifest.permission.READ_EXTERNAL_STORAGE ||
        it.permission == Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    val isCameraGranted = cameraPermission?.status?.isGranted == true
    val isAudioGranted = audioPermission?.status?.isGranted == true
    val isStorageGranted = storagePermissions.isNotEmpty() && storagePermissions.all { it.status.isGranted }

    val showRationale = permissionsState.shouldShowRationale

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CameraBackground)
            .testTag("camera_permission_handler_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Graphic & Branding
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(CameraSurfaceElevated, CameraSurface)
                        )
                    )
                    .border(2.dp, AmberGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Lens",
                    tint = AmberGold,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "LITE PRO CAM",
                color = CameraTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.SansSerif
            )

            Text(
                text = "STUDIO OPTICS ENGINE",
                color = AmberGoldLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "To unlock full manual exposure controls, RAW capture, 4K recording, and AI scene analysis, Lite Pro Cam requires access to your camera and storage.",
                color = CameraTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Permission items breakdown cards
            PermissionDetailCard(
                icon = Icons.Default.CameraAlt,
                title = "Camera Hardware Access",
                description = "Required for live sensor viewfinder, RAW/DNG photo capture, optical zoom, and manual ISO/shutter controls.",
                isGranted = isCameraGranted,
                testTag = "permission_item_camera"
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionDetailCard(
                icon = Icons.Default.Folder,
                title = "Storage & Gallery Access",
                description = "Required to save pro captures & videos to your gallery, export RAW files, and import photos for AI filter grading.",
                isGranted = isStorageGranted,
                testTag = "permission_item_storage"
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionDetailCard(
                icon = Icons.Default.Mic,
                title = "Microphone Access",
                description = "Required for high-fidelity stereo audio capture during video, slow-motion, and time-lapse recording.",
                isGranted = isAudioGranted,
                testTag = "permission_item_audio"
            )

            // Rationale explanation if previously denied
            AnimatedVisibility(
                visible = showRationale,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x33FFB300))
                        .border(1.dp, Color(0x66FFB300), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Rationale Note",
                            tint = AmberGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permission Notice",
                            color = AmberGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CameraX cannot initialize the physical camera session without these permissions. Your media stays strictly on your device.",
                        color = CameraTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGold,
                    contentColor = Color(0xFF0C0E12)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("grant_permissions_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Grant Camera & Storage Access",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CameraTextSecondary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        listOf(CameraSurfaceBorder, CameraSurfaceBorder)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = CameraTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Open App Settings",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionDetailCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    testTag: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CameraSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (isGranted) listOf(PeakingGreen.copy(alpha = 0.5f), PeakingGreen.copy(alpha = 0.5f))
                else listOf(CameraSurfaceBorder, CameraSurfaceBorder)
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CameraSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isGranted) PeakingGreen else AmberGold,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = CameraTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = CameraTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = CircleShape,
                color = if (isGranted) PeakingGreen.copy(alpha = 0.15f) else CameraSurfaceBorder.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGranted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Granted",
                            tint = PeakingGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Allowed",
                            color = PeakingGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Needed",
                            color = CameraTextDisabled,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val genericIntent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(genericIntent)
    }
}
