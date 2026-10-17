package com.audioplayer.hires.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.audio.CenterChannelProcessor
import com.audioplayer.hires.ui.MainViewModel
import com.audioplayer.hires.ui.theme.*

@Composable
fun VocalStudioScreen(viewModel: MainViewModel) {
    val currentMode by viewModel.vocalFocusMode.collectAsState()
    val currentAmount by viewModel.vocalFocusAmount.collectAsState()
    val vocalVol by viewModel.vocalBalance.collectAsState()
    val instVol by viewModel.instrumentBalance.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar with Prominent Back Navigation & Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to previous screen",
                    tint = StudioAiCyan
                )
            }

            Text(
                text = "VOCAL STUDIO",
                color = StudioTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            TextButton(
                onClick = { viewModel.resetVocalStudio() }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Vocal Studio",
                    tint = StudioGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", color = StudioGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mode Selector Buttons
        Text(
            text = "Processing Mode",
            color = StudioTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeSelectButton(
                title = "Original Mix",
                subtitle = "Bypass M/S",
                isSelected = currentMode == CenterChannelProcessor.MODE_OFF,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.setVocalFocus(CenterChannelProcessor.MODE_OFF, 0.0f)
            }

            ModeSelectButton(
                title = "Vocal Focus",
                subtitle = "Formant Boost",
                isSelected = currentMode == CenterChannelProcessor.MODE_VOCAL_FOCUS,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.setVocalFocus(CenterChannelProcessor.MODE_VOCAL_FOCUS, if (currentAmount == 0f) 0.5f else currentAmount)
            }

            ModeSelectButton(
                title = "Instrumental Focus",
                subtitle = "Lead Vocal Cut",
                isSelected = currentMode == CenterChannelProcessor.MODE_INSTRUMENTAL_FOCUS,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.setVocalFocus(CenterChannelProcessor.MODE_INSTRUMENTAL_FOCUS, if (currentAmount == 0f) 0.75f else currentAmount)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Intensity Slider Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = StudioCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus Intensity",
                        color = StudioTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(currentAmount * 100).toInt()}%",
                        color = StudioAiCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = currentAmount,
                    onValueChange = { viewModel.setVocalFocus(currentMode, it) },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioAiCyan,
                        activeTrackColor = StudioAiCyan,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = when (currentMode) {
                        CenterChannelProcessor.MODE_VOCAL_FOCUS ->
                            "Enhancing 2.6 kHz lead vocal formants while preserving stereo warmth."
                        CenterChannelProcessor.MODE_INSTRUMENTAL_FOCUS ->
                            "Attenuating center vocalists while preserving sub-bass (<140 Hz) and stereo ambience."
                        else ->
                            "Original stereo mix without mid/side processing."
                    },
                    color = StudioTextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Stem Balance Faders (Fully connected to live audio pipeline)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = StudioCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Live Mid / Side Balance",
                    color = StudioTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Vocal (Mid Channel) Fader
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vocal (Mid):", color = StudioTextMuted, fontSize = 13.sp, modifier = Modifier.width(110.dp))
                    Slider(
                        value = vocalVol,
                        onValueChange = {
                            viewModel.setStemBalance(it, instVol)
                        },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = StudioVuGreen, activeTrackColor = StudioVuGreen),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format("%.1fx", vocalVol),
                        color = StudioVuGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(45.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Accompaniment (Side Channel) Fader
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Instruments (Side):", color = StudioTextMuted, fontSize = 13.sp, modifier = Modifier.width(110.dp))
                    Slider(
                        value = instVol,
                        onValueChange = {
                            viewModel.setStemBalance(vocalVol, it)
                        },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = StudioGold, activeTrackColor = StudioGold),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format("%.1fx", instVol),
                        color = StudioGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(45.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Signal Processing Information Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF111722),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Deterministic Mid/Side Acoustic Processing",
                    color = StudioGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Processes center lead components and wide stereo accompaniment in real time using minimum-phase biquad filters without introducing network latency or phasing artifacts.",
                    color = StudioTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ModeSelectButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) StudioAiCyan else StudioCardBg,
            contentColor = if (isSelected) StudioObsidian else StudioTextPrimary
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) StudioAiCyan else StudioBorder
        ),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = if (isSelected) StudioObsidian.copy(alpha = 0.85f) else StudioTextMuted
            )
        }
    }
}
