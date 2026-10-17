package com.audioplayer.hires.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asynchronous album artwork loader.
 * Loads high-resolution embedded or MediaStore album art on background I/O dispatcher.
 * Gracefully displays [fallbackContent] if no album artwork is available.
 */
@Composable
fun TrackArtwork(
    artworkUri: String?,
    trackUri: String? = null,
    contentDescription: String = "Album Artwork",
    modifier: Modifier = Modifier,
    fallbackContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val bitmapState by produceState<ImageBitmap?>(initialValue = null, key1 = artworkUri, key2 = trackUri) {
        value = withContext(Dispatchers.IO) {
            var decoded: ImageBitmap? = null

            // 1. Try loading from artworkUri
            if (!artworkUri.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(artworkUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
                        try {
                            val bmp = context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
                            decoded = bmp.asImageBitmap()
                        } catch (_: Exception) {}
                    }
                    if (decoded == null) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) {
                                decoded = bmp.asImageBitmap()
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Try loading thumbnail from track content URI if available
            if (decoded == null && !trackUri.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val uri = Uri.parse(trackUri)
                    if (uri.scheme == "content") {
                        val bmp = context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
                        decoded = bmp.asImageBitmap()
                    }
                } catch (_: Exception) {}
            }

            decoded
        }
    }

    if (bitmapState != null) {
        Image(
            bitmap = bitmapState!!,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            fallbackContent()
        }
    }
}
