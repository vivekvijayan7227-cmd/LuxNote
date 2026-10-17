package com.audioplayer.hires.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    var vocalVol by remember { mutableStateOf(1.0f) }
    var instVol by remember { mutableStateOf(1.0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .statusBarsPadding() // FIX: Clean spacing below Android system status bar
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Bar with Prominent Back Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = StudioAiCyan
                )
            }

            Text(
                text = "VOCAL & INSTRUMENTAL STUDIO",
                color = StudioTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                text = "100% NON-AI DSP",
                color = StudioVuGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mode Selector Buttons
        Text(
            text = "Select Processing Mode",
            color = StudioTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))

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
                subtitle = "Center Attenuation",
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
                        text = "Processing Intensity",
                        color = StudioTextPrimary,
                        fontSize = 13.sp,
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
                            "Boosting 2.6 kHz vocal formant body (+${String.format("%.1f", currentAmount * 6f)} dB) and cleaning up low-end mud."
                        CenterChannelProcessor.MODE_INSTRUMENTAL_FOCUS ->
                            "Surgically attenuating center vocalist (-${String.format("%.1f", currentAmount * 24f)} dB) while 100% preserving sub-bass (<140 Hz) and high-air stereo sheen."
                        else ->
                            "Pure bit-exact original signal path. Zero filter interaction."
                    },
                    color = StudioTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Stem Balance Faders
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = StudioCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Mid / Side Stem Balance",
                    color = StudioTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vocal (Mid):", color = StudioTextMuted, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = vocalVol,
                        onValueChange = {
                            vocalVol = it
                            viewModel.dspPipeline.centerChannelProcessor.processStems(floatArrayOf(0f, 0f), 0, vocalVol, instVol)
                        },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = StudioVuGreen, activeTrackColor = StudioVuGreen),
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format("%.1fx", vocalVol), color = StudioVuGreen, fontSize = 11.sp, modifier = Modifier.width(35.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Instruments (Side):", color = StudioTextMuted, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = instVol,
                        onValueChange = {
                            instVol = it
                            viewModel.dspPipeline.centerChannelProcessor.processStems(floatArrayOf(0f, 0f), 0, vocalVol, instVol)
                        },
                        valueRange = 0.0f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = StudioGold, activeTrackColor = StudioGold),
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format("%.1fx", instVol), color = StudioGold, fontSize = 11.sp, modifier = Modifier.width(35.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Audiophile Engineering Truth Disclosure Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF111722),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Acoustic Engineering Disclosure (100% Non-AI)",
                    color = StudioGold,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "This processor utilizes phase-coherent Mid/Side matrixing and Linkwitz-Riley crossover filtering. Unlike neural models that introduce latency and phase artifacts, this offline DSP is 100% deterministic with ZERO comb-filtering or watery IEM noise.",
                    color = StudioTextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
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
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) StudioAiCyan else StudioBorder
        ),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = if (isSelected) StudioObsidian.copy(alpha = 0.8f) else StudioTextMuted
            )
        }
    }
}
