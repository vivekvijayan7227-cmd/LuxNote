package com.audioplayer.hires.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.audio.BiquadType
import com.audioplayer.hires.audio.GraphicEqualizer
import com.audioplayer.hires.model.FactoryEqPresets
import com.audioplayer.hires.ui.MainViewModel
import com.audioplayer.hires.ui.components.EqCurveCanvas
import com.audioplayer.hires.ui.theme.*

@Composable
fun EqualizerScreen(viewModel: MainViewModel) {
    val dspEnabled by viewModel.dspEnabled.collectAsState()
    val preampGain by viewModel.preampGainDb.collectAsState()
    val hasClipped by viewModel.hasClipped.collectAsState()
    val graphicGains by viewModel.graphicGains.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Graphic, 1 = Parametric
    var showPresetMenu by remember { mutableStateOf(false) }

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
                text = "STUDIO EQUALIZER",
                color = StudioTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Master Bypass Switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (dspEnabled) "ON" else "BYPASS",
                    color = if (dspEnabled) StudioVuGreen else StudioTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = dspEnabled,
                    onCheckedChange = { viewModel.toggleDspEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StudioVuGreen,
                        checkedTrackColor = StudioCardBg,
                        uncheckedThumbColor = StudioTextMuted,
                        uncheckedTrackColor = StudioBorder
                    )
                )
            }
        }

        // Predicted Real-Time Frequency Response Curve
        EqCurveCanvas(
            evaluator = { f -> viewModel.dspPipeline.evaluateCombinedCurveDb(f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Preamp Master Staging & Clipping Indicator
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = StudioCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Preamp:", color = StudioTextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%+.1f dB", preampGain),
                        color = StudioAiCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Clipping Lamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (hasClipped) StudioPeakCrimson else Color(0xFF333D4B))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasClipped) "CLIPPING" else "CLEAN",
                        color = if (hasClipped) StudioPeakCrimson else StudioVuGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = preampGain,
                    onValueChange = { viewModel.setPreampGain(it) },
                    valueRange = -18.0f..6.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioAiCyan,
                        activeTrackColor = StudioAiCyan,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier.width(130.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Selector: Graphic EQ vs Parametric EQ
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioCardBg,
            contentColor = StudioAiCyan,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("10-Band Graphic", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("12-Band Parametric", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            // 10-Band Graphic EQ View
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "ISO Standard Bands", color = StudioTextMuted, fontSize = 11.sp)

                Box {
                    Button(
                        onClick = { showPresetMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Presets ▼", color = StudioAiCyan, fontSize = 11.sp)
                    }

                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { showPresetMenu = false },
                        modifier = Modifier.background(StudioCardBg)
                    ) {
                        FactoryEqPresets.GRAPHIC_PRESETS.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name, color = StudioTextPrimary, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.applyGraphicPreset(preset)
                                    showPresetMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 10 Sliders with proper mobile layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .background(StudioCardBg, RoundedCornerShape(10.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val freqs = GraphicEqualizer.ISO_FREQUENCIES
                for (i in 0 until 10) {
                    val gain = graphicGains[i]
                    val label = when {
                        freqs[i] < 1000f -> "${freqs[i].toInt()}Hz"
                        else -> "${(freqs[i] / 1000).toInt()}k"
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = String.format("%+.0f", gain),
                            color = if (gain != 0f) StudioVuGreen else StudioTextMuted,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Slider(
                            value = gain,
                            onValueChange = { viewModel.setGraphicBandGain(i, it) },
                            valueRange = -12.0f..12.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = if (gain != 0f) StudioVuGreen else StudioTextMuted,
                                activeTrackColor = StudioVuGreen,
                                inactiveTrackColor = StudioBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = label,
                            color = StudioTextPrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            // 12-Band Parametric EQ View
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .background(StudioCardBg, RoundedCornerShape(10.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                val bands = viewModel.dspPipeline.parametricEq.getAllBands()
                items(bands.size) { index ->
                    val band = bands[index]
                    ParametricBandRow(
                        index = index,
                        band = band,
                        onUpdate = { enabled, type, freq, gain, q ->
                            viewModel.dspPipeline.parametricEq.configureBand(
                                index, enabled, type, freq, gain, q
                            )
                            if (enabled && !viewModel.dspEnabled.value) {
                                viewModel.toggleDspEnabled(true)
                            }
                        }
                    )
                    Divider(color = StudioBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun ParametricBandRow(
    index: Int,
    band: com.audioplayer.hires.audio.ParametricBandConfig,
    onUpdate: (Boolean, BiquadType, Float, Float, Float) -> Unit
) {
    var enabled by remember(band.enabled) { mutableStateOf(band.enabled) }
    var gain by remember(band.gainDb) { mutableStateOf(band.gainDb) }
    var freq by remember(band.frequency) { mutableStateOf(band.frequency) }
    var q by remember(band.q) { mutableStateOf(band.q) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Checkbox(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                onUpdate(enabled, band.type, freq, gain, q)
            },
            colors = CheckboxDefaults.colors(checkedColor = StudioAiCyan)
        )

        Column(modifier = Modifier.width(70.dp)) {
            Text(text = "Band #${index + 1}", color = StudioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = band.type.name, color = StudioTextMuted, fontSize = 9.sp)
        }

        Column(modifier = Modifier.width(65.dp)) {
            Text(text = "${freq.toInt()} Hz", color = StudioAiCyan, fontSize = 10.sp)
            Text(text = String.format("%+.1f dB", gain), color = if (gain != 0f) StudioVuGreen else StudioTextMuted, fontSize = 9.sp)
        }

        Slider(
            value = gain,
            onValueChange = {
                gain = it
                onUpdate(enabled, band.type, freq, gain, q)
            },
            valueRange = -18.0f..18.0f,
            colors = SliderDefaults.colors(
                thumbColor = StudioAiCyan,
                activeTrackColor = StudioAiCyan,
                inactiveTrackColor = StudioBorder
            ),
            modifier = Modifier.width(130.dp)
        )
    }
}
