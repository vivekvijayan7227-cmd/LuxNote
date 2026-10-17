package com.audioplayer.hires.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.ui.MainViewModel
import com.audioplayer.hires.ui.theme.StudioColors

@Composable
fun DeviceInspectorScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val diagnosticsReport by viewModel.diagnosticsReport.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val usbDacInfo by viewModel.usbDacInfo.collectAsState()
    val nativeEngineDetails = viewModel.getNativeEngineDetails()
    val devices = viewModel.audioPlayer.getOutputDevices()

    var probeResult by remember { mutableStateOf<String?>(null) }
    var isProbing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshDiagnostics()
        viewModel.refreshUsbHardware()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = StudioColors.Gold
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "SYSTEM & AUDIO DIAGNOSTICS",
                        color = StudioColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Hardware, Storage & Playback Inspection",
                        color = StudioColors.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row {
                IconButton(
                    onClick = {
                        val reportText = diagnosticsReport?.toFormattedText() ?: "No report available"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("LuxNote Diagnostics", reportText))
                        Toast.makeText(context, "Diagnostics report copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Report",
                        tint = StudioColors.Blue
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.refreshDiagnostics()
                        viewModel.refreshUsbHardware()
                        Toast.makeText(context, "Diagnostics refreshed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = StudioColors.Gold
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {
            // Card 1: Storage & File Permissions
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = StudioColors.Gold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("STORAGE PERMISSIONS & ACCESS", color = StudioColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        diagnosticsReport?.permissions?.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(p.permissionName, color = StudioColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(p.description, color = StudioColors.TextMuted, fontSize = 10.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (p.isGranted) StudioColors.Success.copy(alpha = 0.15f) else StudioColors.Error.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (p.isGranted) StudioColors.Success else StudioColors.Error)
                                ) {
                                    Text(
                                        text = if (p.isGranted) "GRANTED" else "DENIED",
                                        color = if (p.isGranted) StudioColors.Success else StudioColors.Error,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Direct Storage Action Button
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Gold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enable All Files Access (Direct Storage Mode)", color = StudioColors.Background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Card 2: Local Storage Audio File Discovery
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = StudioColors.Blue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LOCAL STORAGE AUDIO DISCOVERY", color = StudioColors.Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val audit = diagnosticsReport?.storageAudit
                        Text("MediaStore Audio Items Detected: ${audit?.mediaStoreAudioCount ?: 0}", color = StudioColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("App Internal Cache Tracks: ${audit?.internalImportedCount ?: 0}", color = StudioColors.TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Text("Device Storage Directories:", color = StudioColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        audit?.folderAudits?.forEach { f ->
                            val statusStr = if (f.exists && f.isReadable) "${f.audioFileCount} tracks" else (if (!f.exists) "Missing" else "Unreadable")
                            val statusColor = if (f.audioFileCount > 0) StudioColors.Success else StudioColors.TextMuted
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(f.path, color = StudioColors.TextSecondary, fontSize = 10.sp, maxLines = 1)
                                Text(statusStr, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.scanDeviceMusic()
                                viewModel.refreshDiagnostics()
                                Toast.makeText(context, "Rescanning storage...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioColors.SurfaceElevated),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Blue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = StudioColors.Blue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Device Storage Now", color = StudioColors.Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Card 3: Audio Engine & Hardware Sink
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = StudioColors.Success, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AUDIO ENGINE & HARDWARE SINK", color = StudioColors.Success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("DSP Engine: $nativeEngineDetails", color = StudioColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("32-bit Float AudioTrack: ${if (diagnosticsReport?.floatAudioTrackSupported == true) "Supported" else "HAL Fallback to 16-bit"}", color = StudioColors.TextSecondary, fontSize = 11.sp)
                        Text("16-bit Universal AudioTrack: ${if (diagnosticsReport?.pcm16AudioTrackSupported == true) "Supported" else "Error"}", color = StudioColors.TextSecondary, fontSize = 11.sp)
                        Text("Background Service: ${if (diagnosticsReport?.serviceActive == true) "Live" else "Idle"}", color = StudioColors.TextSecondary, fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Active Output Devices:", color = StudioColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        devices.forEach { dev ->
                            Text("  • ${dev.name} (${dev.type})", color = StudioColors.TextPrimary, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Card 4: Playback Self-Test Probe
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = StudioColors.Gold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PLAYBACK SELF-TEST PROBE", color = StudioColors.Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val track = currentTrack ?: viewModel.libraryTracks.value.firstOrNull()
                        if (track != null) {
                            Text("Target: ${track.title} (${track.format})", color = StudioColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("Path: ${track.uriString}", color = StudioColors.TextMuted, fontSize = 10.sp, maxLines = 1)
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    isProbing = true
                                    viewModel.probePlayback(track) { result ->
                                        probeResult = result
                                        isProbing = false
                                        viewModel.refreshDiagnostics()
                                    }
                                },
                                enabled = !isProbing,
                                colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Gold),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isProbing) "Probing File & Decoder..." else "Run Playback Self-Test on Track", color = StudioColors.Background, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (probeResult != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = StudioColors.Background,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = probeResult ?: "",
                                        color = StudioColors.TextPrimary,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        } else {
                            Text("No track in library to test. Rescan storage first.", color = StudioColors.TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Card 5: Real-Time Event & Diagnostic Logs
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StudioColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = StudioColors.Blue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("LIVE AUDIO EVENT LOGS", color = StudioColors.Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("${diagnosticsReport?.recentLogs?.size ?: 0} events", color = StudioColors.TextMuted, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        val logs = diagnosticsReport?.recentLogs?.takeLast(30) ?: emptyList()
                        if (logs.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StudioColors.Background,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            ) {
                                LazyColumn(modifier = Modifier.padding(8.dp)) {
                                    items(logs) { line ->
                                        Text(
                                            text = line,
                                            color = if (line.contains("ERROR") || line.contains("failed")) StudioColors.Error else StudioColors.TextSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("No audio events logged yet.", color = StudioColors.TextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
