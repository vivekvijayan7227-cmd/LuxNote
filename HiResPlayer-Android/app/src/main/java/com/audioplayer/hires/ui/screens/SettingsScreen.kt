package com.audioplayer.hires.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.ui.MainViewModel
import com.audioplayer.hires.ui.StudioScreen
import com.audioplayer.hires.ui.theme.StudioColors

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val libraryTracks by viewModel.libraryTracks.collectAsState()
    val queues by viewModel.queues.collectAsState()
    val isGaplessEnabled by viewModel.isGaplessEnabled.collectAsState()
    val isRestoreSessionEnabled by viewModel.isRestoreSessionEnabled.collectAsState()
    val isResumeAfterInterruptionEnabled by viewModel.isResumeAfterInterruptionEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = StudioColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "AUDIO & CONFIGURATION",
                style = MaterialTheme.typography.bodySmall,
                color = StudioColors.TextMuted,
                letterSpacing = 1.sp
            )
        }

        // Section: Studio Audio & DSP
        SettingsSectionHeader(title = "STUDIO AUDIO & DSP")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsNavigationRow(
                    icon = Icons.Default.GraphicEq,
                    iconTint = StudioColors.Blue,
                    title = "Studio Equalizer & DSP",
                    subtitle = "10-Band ISO Biquad Cascade & Live Spectrum",
                    onClick = { viewModel.setScreen(StudioScreen.EQUALIZER) }
                )
                HorizontalDivider(color = StudioColors.Divider, thickness = 1.dp)
                SettingsNavigationRow(
                    icon = Icons.Default.SpatialAudioOff,
                    iconTint = StudioColors.Gold,
                    title = "Vocal Studio Matrix",
                    subtitle = "Mid/Side Formant Separator & Center Attenuation",
                    onClick = { viewModel.setScreen(StudioScreen.VOCAL_STUDIO) }
                )
                HorizontalDivider(color = StudioColors.Divider, thickness = 1.dp)
                SettingsNavigationRow(
                    icon = Icons.Default.BugReport,
                    iconTint = StudioColors.Success,
                    title = "System & Audio Diagnostics",
                    subtitle = "Storage Audit, Direct File Access, Playback Self-Test & Event Logs",
                    onClick = { viewModel.setScreen(StudioScreen.DEVICE_INSPECTOR) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Playback
        SettingsSectionHeader(title = "PLAYBACK ENGINE")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsSwitchRow(
                    icon = Icons.Default.AllInclusive,
                    title = "Gapless Playback",
                    subtitle = "Seamless serial AudioTrack handoff between consecutive tracks",
                    checked = isGaplessEnabled,
                    onCheckedChange = { viewModel.setGaplessEnabled(it) }
                )
                HorizontalDivider(color = StudioColors.Divider, thickness = 1.dp)
                SettingsSwitchRow(
                    icon = Icons.Default.Restore,
                    title = "Restore Last Session",
                    subtitle = "Restore queue and position when app reopens",
                    checked = isRestoreSessionEnabled,
                    onCheckedChange = { viewModel.setRestoreSessionEnabled(it) }
                )
                HorizontalDivider(color = StudioColors.Divider, thickness = 1.dp)
                SettingsSwitchRow(
                    icon = Icons.Default.PhoneInTalk,
                    title = "Resume After Interruption",
                    subtitle = "Automatically resume playback after phone calls or alarms",
                    checked = isResumeAfterInterruptionEnabled,
                    onCheckedChange = { viewModel.setResumeAfterInterruptionEnabled(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: Library & Storage
        SettingsSectionHeader(title = "LIBRARY & STORAGE")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsNavigationRow(
                    icon = Icons.Default.Refresh,
                    iconTint = StudioColors.Gold,
                    title = "Rescan Device Storage",
                    subtitle = "Direct physical storage & MediaStore auto-scan",
                    onClick = { viewModel.scanDeviceMusic() }
                )
                HorizontalDivider(color = StudioColors.Divider, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Indexed Collection", color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("${libraryTracks.size} tracks across ${queues.size} queues", color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = "100% OFFLINE",
                        color = StudioColors.Success,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: About & Engineering
        SettingsSectionHeader(title = "ABOUT")
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = StudioColors.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "LuxNote",
                    color = StudioColors.Gold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version ${com.audioplayer.hires.BuildConfig.VERSION_NAME} (LuxNote Master Architecture)",
                    color = StudioColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Native C++20 SIMD DSP Engine (libhires_audio.so)\n• Format-Aware Low-Latency PCM Pipeline\n• 100% Offline & Zero Network Permissions",
                    color = StudioColors.TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = StudioColors.TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(StudioColors.SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = StudioColors.TextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(StudioColors.SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = StudioColors.TextSecondary, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = StudioColors.Gold,
                checkedTrackColor = StudioColors.SurfaceElevated,
                uncheckedThumbColor = StudioColors.TextMuted,
                uncheckedTrackColor = StudioColors.Background
            )
        )
    }
}
