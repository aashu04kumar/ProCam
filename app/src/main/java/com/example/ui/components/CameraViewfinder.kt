package com.example.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AiScene
import com.example.data.GridType
import com.example.ui.theme.AmberGold
import com.example.ui.theme.LeicaRed
import com.example.ui.theme.PeakingGreen
import kotlin.math.abs

@Composable
fun CameraViewfinder(
    modifier: Modifier = Modifier,
    previewView: PreviewView,
    isHardwareCameraAvailable: Boolean = true,
    gridType: GridType,
    showLevelIndicator: Boolean,
    showHistogram: Boolean,
    showFocusPeaking: Boolean,
    showZebraStripes: Boolean,
    rollDegrees: Float,
    pitchDegrees: Float,
    isRecording: Boolean,
    audioDb: Float,
    aiScene: AiScene,
    compositionAdvice: String,
    onTapToFocus: (Float, Float, Int, Int) -> Unit
) {
    var tapFocusPoint by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("camera_viewfinder_container")
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    tapFocusPoint = offset
                    onTapToFocus(offset.x, offset.y, size.width, size.height)
                }
            }
    ) {
        // CameraX live preview (COMPATIBLE TextureView)
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_preview_surface")
        )

        // Simulated high-end optical viewfinder canvas when hardware camera sensor is not ready/available (e.g. simulator)
        if (!isHardwareCameraAvailable) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Atmospheric gradient canvas
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF141923),
                            Color(0xFF263345),
                            Color(0xFF8D5B4C),
                            Color(0xFF1A1F29)
                        )
                    )
                )

                // Lens optic center aperture vignette
                drawCircle(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xAA000000)),
                        center = Offset(w / 2f, h / 2f),
                        radius = (w * 0.7f)
                    )
                )
            }
        }

        // Zebra Stripes Overlay (for exposure checking)
        if (showZebraStripes) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 28.dp.toPx()
                val strokeWidth = 3.dp.toPx()
                var currentX = -size.height
                while (currentX < size.width) {
                    drawLine(
                        color = Color(0x33FFFFFF),
                        start = Offset(currentX, 0f),
                        end = Offset(currentX + size.height, size.height),
                        strokeWidth = strokeWidth
                    )
                    currentX += step
                }
            }
        }

        // Focus Peaking Simulation Canvas
        if (showFocusPeaking) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Highlight edges with vivid green sparkles
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawCircle(
                    color = PeakingGreen.copy(alpha = 0.6f),
                    center = Offset(cx, cy),
                    radius = 90.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = PeakingGreen.copy(alpha = 0.35f),
                    center = Offset(cx, cy),
                    radius = 120.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        // Grid Lines Overlay
        if (gridType != GridType.NONE) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val lineColor = Color(0x66FFFFFF)
                val stroke = Stroke(width = 1.dp.toPx())

                when (gridType) {
                    GridType.RULE_OF_THIRDS -> {
                        // Vertical lines
                        drawLine(lineColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
                        drawLine(lineColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = 1.dp.toPx())
                        // Horizontal lines
                        drawLine(lineColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
                        drawLine(lineColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = 1.dp.toPx())
                    }
                    GridType.GOLDEN_RATIO -> {
                        val phi = 0.618f
                        val invPhi = 1f - phi
                        drawLine(lineColor, Offset(w * invPhi, 0f), Offset(w * invPhi, h), strokeWidth = 1.dp.toPx())
                        drawLine(lineColor, Offset(w * phi, 0f), Offset(w * phi, h), strokeWidth = 1.dp.toPx())
                        drawLine(lineColor, Offset(0f, h * invPhi), Offset(w, h * invPhi), strokeWidth = 1.dp.toPx())
                        drawLine(lineColor, Offset(0f, h * phi), Offset(w, h * phi), strokeWidth = 1.dp.toPx())
                    }
                    GridType.SQUARE -> {
                        val side = minOf(w, h)
                        val left = (w - side) / 2f
                        val top = (h - side) / 2f
                        drawRect(lineColor, topLeft = Offset(left, top), size = Size(side, side), style = stroke)
                    }
                    GridType.CROSSHAIR -> {
                        val cx = w / 2f
                        val cy = h / 2f
                        val arm = 24.dp.toPx()
                        drawLine(lineColor, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 1.5.dp.toPx())
                        drawLine(lineColor, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 1.5.dp.toPx())
                        drawCircle(lineColor, radius = 6.dp.toPx(), center = Offset(cx, cy), style = stroke)
                    }
                    else -> {}
                }
            }
        }

        // Artificial Horizon / Level Indicator
        if (showLevelIndicator) {
            val isLevel = abs(rollDegrees) <= 1.0f
            val levelColor = if (isLevel) PeakingGreen else AmberGold
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("artificial_horizon_indicator")
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val barLength = 90.dp.toPx()

                // Center fixed reference ticks
                drawLine(
                    color = Color(0x88FFFFFF),
                    start = Offset(cx - 20.dp.toPx(), cy),
                    end = Offset(cx - 8.dp.toPx(), cy),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color(0x88FFFFFF),
                    start = Offset(cx + 8.dp.toPx(), cy),
                    end = Offset(cx + 20.dp.toPx(), cy),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = Color(0x88FFFFFF),
                    radius = 3.dp.toPx(),
                    center = Offset(cx, cy)
                )

                // Tilting horizon line based on accelerometer roll
                val angleRad = Math.toRadians(-rollDegrees.toDouble())
                val dx = (barLength * Math.cos(angleRad)).toFloat()
                val dy = (barLength * Math.sin(angleRad)).toFloat()

                drawLine(
                    color = levelColor,
                    start = Offset(cx - dx, cy - dy),
                    end = Offset(cx + dx, cy + dy),
                    strokeWidth = if (isLevel) 3.dp.toPx() else 2.dp.toPx()
                )
            }

            // Degree badge in center bottom of preview
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 70.dp)
                    .background(Color(0x88000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${String.format("%.1f", rollDegrees)}°",
                    color = levelColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Tap to focus reticle indicator
        tapFocusPoint?.let { pt ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = AmberGold,
                    radius = 32.dp.toPx(),
                    center = pt,
                    style = Stroke(width = 2.dp.toPx())
                )
                // Small corner brackets
                val bLen = 8.dp.toPx()
                drawLine(AmberGold, Offset(pt.x - 32.dp.toPx(), pt.y), Offset(pt.x - 32.dp.toPx() + bLen, pt.y), 2.dp.toPx())
                drawLine(AmberGold, Offset(pt.x + 32.dp.toPx(), pt.y), Offset(pt.x + 32.dp.toPx() - bLen, pt.y), 2.dp.toPx())
                drawLine(AmberGold, Offset(pt.x, pt.y - 32.dp.toPx()), Offset(pt.x, pt.y - 32.dp.toPx() + bLen), 2.dp.toPx())
                drawLine(AmberGold, Offset(pt.x, pt.y + 32.dp.toPx()), Offset(pt.x, pt.y + 32.dp.toPx() - bLen), 2.dp.toPx())
            }
        }

        // Live Histogram (Top Left corner)
        if (showHistogram) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 56.dp)
                    .size(width = 110.dp, height = 56.dp)
                    .background(Color(0xBB14171E), RoundedCornerShape(8.dp))
                    .padding(6.dp)
                    .testTag("live_histogram_view")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Simulated live RGB/Luma waveform
                    val lumaPath = Path().apply {
                        moveTo(0f, h * 0.85f)
                        cubicTo(w * 0.25f, h * 0.35f, w * 0.5f, h * 0.15f, w * 0.75f, h * 0.65f)
                        lineTo(w, h * 0.95f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(lumaPath, Color(0x66FFFFFF))

                    val redPath = Path().apply {
                        moveTo(0f, h * 0.9f)
                        cubicTo(w * 0.2f, h * 0.5f, w * 0.6f, h * 0.25f, w, h * 0.9f)
                    }
                    drawPath(redPath, LeicaRed.copy(alpha = 0.8f), style = Stroke(width = 1.5.dp.toPx()))

                    val greenPath = Path().apply {
                        moveTo(0f, h * 0.95f)
                        cubicTo(w * 0.3f, h * 0.3f, w * 0.7f, h * 0.4f, w, h * 0.92f)
                    }
                    drawPath(greenPath, PeakingGreen.copy(alpha = 0.8f), style = Stroke(width = 1.5.dp.toPx()))

                    val bluePath = Path().apply {
                        moveTo(0f, h * 0.92f)
                        cubicTo(w * 0.4f, h * 0.2f, w * 0.8f, h * 0.5f, w, h * 0.88f)
                    }
                    drawPath(bluePath, Color(0xFF448AFF).copy(alpha = 0.8f), style = Stroke(width = 1.5.dp.toPx()))
                }
            }
        }

        // AI Scene detection badge & Composition suggestion
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .background(Color(0xCC12161E), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("ai_scene_badge")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Scene",
                    tint = AmberGold,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${aiScene.label} • $compositionAdvice",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 5.dp)
                )
            }
        }

        // Live Audio VU Level Meter (during video recording)
        if (isRecording) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("audio_vu_meter")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Audio Level",
                        tint = LeicaRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "MIC",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, end = 6.dp)
                    )
                    // Decibel amplitude bars
                    val normAmp = ((audioDb + 40f) / 40f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(6.dp)
                            .background(Color(0xFF333333), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(fraction = normAmp)
                                .background(
                                    if (normAmp > 0.85f) LeicaRed else if (normAmp > 0.65f) AmberGold else PeakingGreen,
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                    Text(
                        text = "${audioDb.toInt()} dB",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}
