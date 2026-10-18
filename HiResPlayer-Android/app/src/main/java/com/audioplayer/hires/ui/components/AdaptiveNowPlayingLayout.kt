package com.audioplayer.hires.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.audioplayer.hires.ui.theme.StudioColors

/**
 * Responsive layout shell for LuxNote's existing NowPlayingScreen content.
 *
 * Integration: split the existing screen's top-level Column children into the
 * six slots below and pass the requested artwork diameter to the current art
 * composable. Do not nest this inside another verticalScroll or duplicate
 * statusBarsPadding/navigationBarsPadding: this shell handles safe insets.
 *
 * Three cases: wide device 2-column; tall portrait anchored controls; short
 * window scrollable center area with transport and actions always reachable.
 */
@Composable
fun AdaptiveNowPlayingLayout(
    header: @Composable () -> Unit,
    artwork: @Composable (diameter: Dp) -> Unit,
    trackDetails: @Composable () -> Unit,
    waveformAndSeek: @Composable () -> Unit,
    transportControls: @Composable () -> Unit,
    bottomActions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = StudioColors.Background
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val isWide = maxWidth >= 700.dp && maxHeight >= 480.dp
        val isShort = maxHeight < 680.dp
        val portraitArtSize = minOf(
            (maxWidth - 16.dp).coerceAtLeast(120.dp),
            (maxHeight * if (isShort) 0.35f else 0.42f).coerceIn(170.dp, 340.dp)
        )

        if (isWide) {
            val wideArtSize = minOf(maxWidth * 0.40f, maxHeight * 0.75f, 380.dp)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                header()
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        artwork(wideArtSize)
                    }
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        trackDetails()
                        waveformAndSeek()
                        transportControls()
                        bottomActions()
                    }
                }
            }
        } else if (isShort) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                header()
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    artwork(portraitArtSize)
                    trackDetails()
                    waveformAndSeek()
                }
                transportControls()
                bottomActions()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                header()
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    artwork(portraitArtSize)
                    trackDetails()
                    waveformAndSeek()
                }
                transportControls()
                bottomActions()
            }
        }
    }
}
