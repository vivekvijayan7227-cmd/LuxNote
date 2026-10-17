package com.audioplayer.hires.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.audioplayer.hires.ui.theme.StudioColors

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.LibraryMusic,
            contentDescription = null,
            tint = StudioColors.Gold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(title, style = MaterialTheme.typography.titleLarge, color = StudioColors.TextPrimary)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = StudioColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioColors.Gold,
                    contentColor = StudioColors.Background
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            androidx.compose.material3.OutlinedButton(
                onClick = onSecondaryAction,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = StudioColors.Gold
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Gold.copy(alpha = 0.6f)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Text(secondaryActionLabel)
            }
        }
    }
}
