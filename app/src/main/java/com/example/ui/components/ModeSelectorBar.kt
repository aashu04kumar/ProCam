package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CameraMode
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CameraTextDisabled
import com.example.ui.theme.CameraTextPrimary

@Composable
fun ModeSelectorBar(
    modifier: Modifier = Modifier,
    currentMode: CameraMode,
    onModeSelected: (CameraMode) -> Unit
) {
    val scrollState = rememberScrollState()

    // Auto-scroll when mode changes
    val modes = CameraMode.entries
    LaunchedEffect(currentMode) {
        val index = modes.indexOf(currentMode)
        if (index >= 0) {
            scrollState.animateScrollTo((index * 80).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x990A0C10))
            .padding(vertical = 8.dp)
            .testTag("mode_selector_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEach { mode ->
                val isSelected = mode == currentMode
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) AmberGold else CameraTextDisabled,
                    label = "mode_text_color"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onModeSelected(mode) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("mode_item_${mode.name.lowercase()}")
                ) {
                    Text(
                        text = mode.title.uppercase(),
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(20.dp)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(AmberGold)
                        )
                    }
                }
            }
        }
    }
}
