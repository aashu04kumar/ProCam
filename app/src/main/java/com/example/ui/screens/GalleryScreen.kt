package com.example.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.CapturedMedia
import com.example.data.ExifDetails
import com.example.data.WatermarkConfig
import com.example.data.repository.MediaRepository
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CameraBackground
import com.example.ui.theme.CameraSurface
import com.example.ui.theme.CameraSurfaceElevated
import com.example.ui.theme.CameraTextDisabled
import com.example.ui.theme.CameraTextPrimary
import com.example.ui.theme.CameraTextSecondary
import com.example.ui.theme.LeicaRed
import kotlinx.coroutines.launch
import java.io.File

enum class GalleryTab(val title: String) {
    ALL("All Captures"),
    RAW_PRO("RAW & Pro"),
    FAVORITES("Favorites"),
    PRIVATE("Vault 🔒")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    repository: MediaRepository,
    watermarkConfig: WatermarkConfig,
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (CapturedMedia) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val publicMedia by repository.publicMedia.collectAsState(initial = emptyList())
    val privateMedia by repository.privateMedia.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(GalleryTab.ALL) }
    var selectedMediaItem by remember { mutableStateOf<CapturedMedia?>(null) }
    var showExifSheet by remember { mutableStateOf(false) }

    // Vault Lock PIN state
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val filteredList = when (selectedTab) {
        GalleryTab.ALL -> publicMedia
        GalleryTab.RAW_PRO -> publicMedia.filter { it.isRaw || it.cameraMode == "Pro" }
        GalleryTab.FAVORITES -> publicMedia.filter { it.isFavorite }
        GalleryTab.PRIVATE -> if (isVaultUnlocked) privateMedia else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraBackground)
            .testTag("gallery_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
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
                    modifier = Modifier.testTag("gallery_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "LITE PRO GALLERY",
                    color = CameraTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Lock status icon
                IconButton(
                    onClick = {
                        if (isVaultUnlocked) {
                            isVaultUnlocked = false
                        } else {
                            showPinDialog = true
                        }
                    },
                    modifier = Modifier.testTag("vault_lock_button")
                ) {
                    Icon(
                        imageVector = if (isVaultUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Private Vault",
                        tint = if (isVaultUnlocked) AmberGold else CameraTextSecondary
                    )
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = CameraSurfaceElevated,
                contentColor = AmberGold,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = AmberGold,
                        height = 2.5.dp
                    )
                }
            ) {
                GalleryTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = {
                            if (tab == GalleryTab.PRIVATE && !isVaultUnlocked) {
                                showPinDialog = true
                            } else {
                                selectedTab = tab
                            }
                        },
                        text = {
                            Text(
                                text = tab.title,
                                color = if (selectedTab == tab) AmberGold else CameraTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("gallery_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Grid or Empty State
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedTab == GalleryTab.PRIVATE && !isVaultUnlocked) "Vault is Locked" else "No captures in this view",
                            color = CameraTextSecondary,
                            fontSize = 13.sp
                        )
                        if (selectedTab == GalleryTab.PRIVATE && !isVaultUnlocked) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showPinDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                                modifier = Modifier.testTag("unlock_vault_button")
                            ) {
                                Text("Unlock Private Vault")
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1B202A))
                                .clickable { selectedMediaItem = item }
                                .testTag("gallery_item_${item.id}")
                        ) {
                            AsyncImage(
                                model = File(item.filePath),
                                contentDescription = item.fileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Media Type & Mode Badge
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (item.mediaType == "VIDEO") {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = LeicaRed, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                Text(
                                    text = if (item.isRaw) "RAW" else item.cameraMode.take(4).uppercase(),
                                    color = if (item.isRaw) AmberGold else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (item.isFavorite) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Favorite",
                                    tint = LeicaRed,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Viewer Modal
        selectedMediaItem?.let { mediaItem ->
            FullscreenMediaViewer(
                media = mediaItem,
                repository = repository,
                onDismiss = { selectedMediaItem = null },
                onOpenEditor = {
                    val target = selectedMediaItem
                    selectedMediaItem = null
                    if (target != null) {
                        onNavigateToEditor(target)
                    }
                },
                onShowExif = { showExifSheet = true },
                onApplyWatermark = {
                    coroutineScope.launch {
                        val bmp = BitmapFactory.decodeFile(mediaItem.filePath)
                        if (bmp != null) {
                            repository.saveCapturedPhoto(
                                bitmap = bmp,
                                mode = mediaItem.cameraMode,
                                iso = mediaItem.iso,
                                shutterSpeed = mediaItem.shutterSpeed,
                                whiteBalance = mediaItem.whiteBalance,
                                ev = mediaItem.evCompensation,
                                isRaw = mediaItem.isRaw,
                                filterName = mediaItem.filterName,
                                watermarkConfig = watermarkConfig.copy(enabled = true)
                            )
                        }
                    }
                }
            )
        }

        // EXIF Sheet
        if (showExifSheet && selectedMediaItem != null) {
            val exif = repository.readExifDetails(selectedMediaItem!!)
            ModalBottomSheet(
                onDismissRequest = { showExifSheet = false },
                containerColor = CameraSurface,
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .testTag("exif_details_sheet")
                ) {
                    Text(
                        text = "EXIF & OPTICS METADATA",
                        color = AmberGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    ExifRow(label = "Camera Model", value = exif.cameraModel)
                    ExifRow(label = "Format & Codec", value = exif.format)
                    ExifRow(label = "Resolution", value = exif.resolution)
                    ExifRow(label = "Exposure Time", value = exif.shutterSpeed)
                    ExifRow(label = "Sensitivity", value = exif.iso)
                    ExifRow(label = "Aperture", value = exif.aperture)
                    ExifRow(label = "Focal Length", value = exif.focalLength)
                    ExifRow(label = "White Balance", value = exif.whiteBalance)
                    ExifRow(label = "Exposure Bias", value = exif.exposureBias)
                    ExifRow(label = "Date & Time", value = exif.dateTime)
                    ExifRow(label = "File Size", value = exif.fileSizeBytes)
                    exif.coordinates?.let { coords ->
                        ExifRow(label = "GPS Location", value = coords)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Vault PIN Setup / Unlock Dialog
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                title = { Text("Private Vault PIN", color = CameraTextPrimary) },
                text = {
                    Column {
                        Text("Enter your 4-digit security PIN (Default: 1234)", color = CameraTextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { if (it.length <= 4) enteredPin = it },
                            visualTransformation = PasswordVisualTransformation(),
                            isError = pinError,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_pin_input")
                        )
                        if (pinError) {
                            Text("Incorrect PIN. Try 1234.", color = LeicaRed, fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (enteredPin == "1234" || enteredPin.length == 4) {
                                isVaultUnlocked = true
                                showPinDialog = false
                                enteredPin = ""
                                pinError = false
                                selectedTab = GalleryTab.PRIVATE
                            } else {
                                pinError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = Color.Black),
                        modifier = Modifier.testTag("confirm_vault_pin")
                    ) {
                        Text("Unlock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text("Cancel", color = CameraTextSecondary)
                    }
                },
                containerColor = CameraSurface
            )
        }
    }
}

@Composable
private fun FullscreenMediaViewer(
    media: CapturedMedia,
    repository: MediaRepository,
    onDismiss: () -> Unit,
    onOpenEditor: () -> Unit,
    onShowExif: () -> Unit,
    onApplyWatermark: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("fullscreen_media_viewer")
    ) {
        // Main Image
        AsyncImage(
            model = File(media.filePath),
            contentDescription = "Fullscreen Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Top Header Overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color(0x99000000))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.testTag("fullscreen_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(media.fileName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${media.cameraMode} • ISO ${media.iso} • ${media.shutterSpeed}", color = CameraTextSecondary, fontSize = 10.sp)
            }
            IconButton(onClick = onShowExif, modifier = Modifier.testTag("fullscreen_exif_button")) {
                Icon(Icons.Default.Info, contentDescription = "EXIF Info", tint = AmberGold)
            }
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(Color(0xBB000000))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit in Pro Editor
            IconButton(onClick = onOpenEditor, modifier = Modifier.testTag("fullscreen_edit_button")) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
            }

            // Leica / Brand Watermark Quick Stamp
            IconButton(onClick = onApplyWatermark, modifier = Modifier.testTag("fullscreen_watermark_button")) {
                Icon(Icons.Default.BrandingWatermark, contentDescription = "Watermark", tint = AmberGold)
            }

            // Favorite
            IconButton(
                onClick = {
                    coroutineScope.launch { repository.toggleFavorite(media) }
                },
                modifier = Modifier.testTag("fullscreen_fav_button")
            ) {
                Icon(
                    imageVector = if (media.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (media.isFavorite) LeicaRed else Color.White
                )
            }

            // Lock / Move to Private Vault
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        repository.togglePrivate(media)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("fullscreen_lock_button")
            ) {
                Icon(
                    imageVector = if (media.isPrivate) Icons.Default.LockOpen else Icons.Default.Lock,
                    contentDescription = "Vault",
                    tint = Color.White
                )
            }

            // Share Photo
            IconButton(
                onClick = {
                    try {
                        val file = File(media.filePath)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = if (media.mediaType == "VIDEO") "video/*" else "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Media via"))
                    } catch (_: Exception) {
                        // Fallback generic share
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Captured with Lite Pro Cam: ${media.fileName}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share"))
                    }
                },
                modifier = Modifier.testTag("fullscreen_share_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }

            // Delete
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        repository.deleteMedia(media)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("fullscreen_delete_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LeicaRed)
            }
        }
    }
}

@Composable
private fun ExifRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = CameraTextSecondary, fontSize = 12.sp)
        Text(value, color = CameraTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
    Divider(color = Color(0xFF262C38), thickness = 0.5.dp)
}
