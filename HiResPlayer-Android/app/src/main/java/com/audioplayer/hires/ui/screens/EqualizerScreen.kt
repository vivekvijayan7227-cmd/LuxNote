package com.audioplayer.hires.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.audio.BiquadType
import com.audioplayer.hires.audio.GraphicEqualizer
import com.audioplayer.hires.audio.ParametricBandConfig
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
            .statusBarsPadding()
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
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to previous screen",
                    tint = StudioAiCyan
                )
            }

            Text(
                text = "STUDIO EQUALIZER",
                color = StudioTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Master Bypass Switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (dspEnabled) "ON" else "BYPASS",
                    color = if (dspEnabled) StudioVuGreen else StudioTextMuted,
                    fontSize = 12.sp,
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
                    Text(text = "Preamp:", color = StudioTextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%+.1f dB", preampGain),
                        color = StudioAiCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Clipping Lamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (hasClipped) StudioPeakCrimson else Color(0xFF333D4B))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (hasClipped) "CLIPPING" else "CLEAN",
                        color = if (hasClipped) StudioPeakCrimson else StudioVuGreen,
                        fontSize = 12.sp,
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
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = StudioCardBg,
            contentColor = StudioAiCyan
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("10-Band Graphic", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("12-Band Parametric", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            // 10-Band Graphic EQ Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "ISO Standard Frequency Bands", color = StudioTextMuted, fontSize = 13.sp)

                Box {
                    Button(
                        onClick = { showPresetMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCardBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Presets ▼", color = StudioAiCyan, fontSize = 13.sp)
                    }

                    DropdownMenu(
                        expanded = showPresetMenu,
                        onDismissRequest = { showPresetMenu = false },
                        modifier = Modifier.background(StudioCardBg)
                    ) {
                        FactoryEqPresets.GRAPHIC_PRESETS.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.name, color = StudioTextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    viewModel.applyGraphicPreset(preset)
                                    showPresetMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Scrollable List of 10 Graphic Equalizer Faders
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .background(StudioCardBg, RoundedCornerShape(10.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                val freqs = GraphicEqualizer.ISO_FREQUENCIES
                items(10) { i ->
                    val gain = graphicGains[i]
                    val label = when {
                        freqs[i] < 1000f -> "${freqs[i].toInt()} Hz"
                        else -> "${(freqs[i] / 1000).toInt()} kHz"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = StudioTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(65.dp)
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
                            text = String.format("%+.1f dB", gain),
                            color = if (gain != 0f) StudioVuGreen else StudioTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(65.dp)
                        )
                    }
                    if (i < 9) {
                        HorizontalDivider(color = StudioBorder.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        } else {
            // 12-Band Parametric EQ View with Complete Expandable Editors
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
                    ParametricBandEditorCard(
                        index = index,
                        band = band,
                        onUpdate = { enabled, type, freq, gain, q ->
                            viewModel.setParametricBand(index, enabled, type, freq, gain, q)
                        }
                    )
                    if (index < bands.size - 1) {
                        HorizontalDivider(color = StudioBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ParametricBandEditorCard(
    index: Int,
    band: ParametricBandConfig,
    onUpdate: (Boolean, BiquadType, Float, Float, Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var enabled by remember(band.enabled) { mutableStateOf(band.enabled) }
    var type by remember(band.type) { mutableStateOf(band.type) }
    var freq by remember(band.frequency) { mutableStateOf(band.frequency) }
    var gain by remember(band.gainDb) { mutableStateOf(band.gainDb) }
    var q by remember(band.q) { mutableStateOf(band.q) }
    var showTypeMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (expanded) Color(0xFF161E2E) else Color.Transparent)
            .padding(6.dp)
    ) {
        // Collapsed Summary Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    onUpdate(enabled, type, freq, gain, q)
                },
                colors = CheckboxDefaults.colors(checkedColor = StudioAiCyan)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Band #${index + 1}",
                        color = StudioTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = StudioAiCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = type.name,
                            color = StudioAiCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${freq.toInt()} Hz  •  ${String.format("%+.1f dB", gain)}  •  Q=${String.format("%.2f", q)}",
                    color = if (enabled) StudioVuGreen else StudioTextMuted,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse band editor" else "Expand band editor",
                    tint = StudioAiCyan
                )
            }
        }

        // Expanded Complete Parameter Controls
        if (expanded) {
            Spacer(modifier = Modifier.height(6.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                // Filter Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Filter Type:", color = StudioTextMuted, fontSize = 13.sp)
                    Box {
                        OutlinedButton(
                            onClick = { showTypeMenu = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioAiCyan),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(text = "${type.name} ▼", fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = showTypeMenu,
                            onDismissRequest = { showTypeMenu = false },
                            modifier = Modifier.background(StudioCardBg)
                        ) {
                            BiquadType.values().forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.name, color = StudioTextPrimary, fontSize = 13.sp) },
                                    onClick = {
                                        type = t
                                        showTypeMenu = false
                                        onUpdate(enabled, type, freq, gain, q)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Frequency Control (20 Hz - 20 kHz Logarithmic Slider)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Frequency:", color = StudioTextMuted, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                    Slider(
                        value = freq,
                        onValueChange = {
                            freq = it
                            onUpdate(enabled, type, freq, gain, q)
                        },
                        valueRange = 20.0f..20000.0f,
                        colors = SliderDefaults.colors(thumbColor = StudioAiCyan, activeTrackColor = StudioAiCyan),
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "${freq.toInt()} Hz", color = StudioAiCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                }

                // Gain Control (-18 dB to +18 dB)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Gain:", color = StudioTextMuted, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                    Slider(
                        value = gain,
                        onValueChange = {
                            gain = it
                            onUpdate(enabled, type, freq, gain, q)
                        },
                        valueRange = -18.0f..18.0f,
                        colors = SliderDefaults.colors(thumbColor = StudioVuGreen, activeTrackColor = StudioVuGreen),
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = String.format("%+.1f dB", gain), color = if (gain != 0f) StudioVuGreen else StudioTextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                }

                // Q Factor Control (0.1 to 10.0)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Q Factor:", color = StudioTextMuted, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                    Slider(
                        value = q,
                        onValueChange = {
                            q = it
                            onUpdate(enabled, type, freq, gain, q)
                        },
                        valueRange = 0.1f..10.0f,
                        colors = SliderDefaults.colors(thumbColor = StudioGold, activeTrackColor = StudioGold),
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = String.format("%.2f", q), color = StudioGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                }

                // Reset Band to default Flat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            gain = 0.0f
                            onUpdate(enabled, type, freq, 0.0f, q)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset gain", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Gain (0 dB)", fontSize = 12.sp, color = StudioTextMuted)
                    }
                }
            }
        }
    }
}
