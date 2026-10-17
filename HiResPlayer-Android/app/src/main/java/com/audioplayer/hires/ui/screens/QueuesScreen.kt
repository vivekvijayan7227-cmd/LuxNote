package com.audioplayer.hires.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.model.StudioQueue
import com.audioplayer.hires.model.Track
import com.audioplayer.hires.model.formatTime
import com.audioplayer.hires.ui.MainViewModel
import com.audioplayer.hires.ui.StudioScreen
import com.audioplayer.hires.ui.theme.*

@Composable
fun QueuesScreen(viewModel: MainViewModel) {
    val queues by viewModel.queues.collectAsState()
    val activeQueueId by viewModel.activeQueueId.collectAsState()
    val activeQueueTracks by viewModel.activeQueue.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val libraryTracks by viewModel.libraryTracks.collectAsState()

    var showCreateQueueDialog by remember { mutableStateOf(false) }
    var newQueueName by remember { mutableStateOf("") }
    var queueToRename by remember { mutableStateOf<StudioQueue?>(null) }
    var renameText by remember { mutableStateOf("") }
    var queueToClear by remember { mutableStateOf<StudioQueue?>(null) }

    val trackMap = remember(libraryTracks) { libraryTracks.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Top Header
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
                        text = "Queues",
                        style = MaterialTheme.typography.titleLarge,
                        color = StudioColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "INDEPENDENT PLAYBACK QUEUES",
                        color = StudioColors.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Button(
                onClick = { showCreateQueueDialog = true },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Gold, contentColor = StudioColors.Background)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Queue",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Queue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Queue Selection Cards
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(queues, key = { it.id }) { q ->
                val isActive = q.id == activeQueueId
                val queueTracks = q.trackIds.mapNotNull { trackMap[it] }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isActive) StudioColors.SurfaceSelected else StudioColors.Surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isActive) StudioColors.Gold else StudioColors.Divider
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.switchQueue(q.id) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isActive) StudioColors.Gold else StudioColors.TextMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = q.name,
                                    color = if (isActive) StudioColors.Gold else StudioColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StudioColors.Gold.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Gold)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = StudioColors.Gold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        queueToRename = q
                                        renameText = q.name
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename queue ${q.name}", tint = StudioTextMuted, modifier = Modifier.size(20.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (queueTracks.isNotEmpty()) {
                                            queueToClear = q
                                        } else {
                                            viewModel.clearQueue(q.id)
                                        }
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear queue ${q.name}", tint = StudioTextMuted, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${queueTracks.size} Tracks • Track ${(q.currentIndex + 1).coerceAtMost(queueTracks.size.coerceAtLeast(1))} of ${queueTracks.size}",
                                color = StudioTextMuted,
                                fontSize = 13.sp
                            )
                            if (q.positionMs > 0) {
                                Text(
                                    text = "Saved pos: ${formatTime(q.positionMs)}",
                                    color = StudioGold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (isActive && queueTracks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = StudioBorder)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "NOW PLAYING FROM THIS QUEUE:",
                                color = StudioGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val nowTrack = queueTracks.getOrNull(q.currentIndex) ?: queueTracks.firstOrNull()
                            if (nowTrack != null) {
                                Text(
                                    text = "${nowTrack.title} — ${nowTrack.artist}",
                                    color = StudioTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Create Queue
    if (showCreateQueueDialog) {
        AlertDialog(
            onDismissRequest = { showCreateQueueDialog = false; newQueueName = "" },
            title = { Text("CREATE QUEUE", color = StudioAiCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newQueueName,
                    onValueChange = { newQueueName = it },
                    placeholder = { Text("e.g. Master Recordings, Night Drive...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioAiCyan,
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newQueueName.isNotBlank()) {
                        viewModel.createQueue(newQueueName)
                        showCreateQueueDialog = false
                        newQueueName = ""
                    }
                }) {
                    Text("Create", color = StudioAiCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateQueueDialog = false; newQueueName = "" }) {
                    Text("Cancel", color = StudioTextMuted)
                }
            },
            containerColor = StudioCardBg
        )
    }

    // Modal: Rename Queue
    if (queueToRename != null) {
        AlertDialog(
            onDismissRequest = { queueToRename = null },
            title = { Text("RENAME QUEUE", color = StudioAiCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioAiCyan,
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        viewModel.renameQueue(queueToRename!!.id, renameText)
                        queueToRename = null
                    }
                }) {
                    Text("Save", color = StudioAiCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { queueToRename = null }) {
                    Text("Cancel", color = StudioTextMuted)
                }
            },
            containerColor = StudioCardBg
        )
    }

    // Modal: Confirmation Dialog for Clear Queue
    if (queueToClear != null) {
        AlertDialog(
            onDismissRequest = { queueToClear = null },
            title = { Text("CLEAR QUEUE", color = StudioGold, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to clear all tracks from '${queueToClear!!.name}'?",
                    color = StudioTextPrimary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearQueue(queueToClear!!.id)
                    queueToClear = null
                }) {
                    Text("Clear", color = StudioPeakCrimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { queueToClear = null }) {
                    Text("Cancel", color = StudioTextMuted)
                }
            },
            containerColor = StudioCardBg
        )
    }
}
