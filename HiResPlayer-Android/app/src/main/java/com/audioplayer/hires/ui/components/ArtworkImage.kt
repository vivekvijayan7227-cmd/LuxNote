package com.audioplayer.hires.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
 * Memory-bounded LRU cache for album artwork to prevent excessive allocations and OOM.
 */
object ArtworkCache {
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
    private val cacheSizeKb = (maxMemoryKb / 8).coerceIn(4096, 24576)

    private val cache = object : LruCache<String, ImageBitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return (value.width * value.height * 4) / 1024
        }
    }

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }

    fun clear() {
        cache.evictAll()
    }
}

/**
 * Decodes a scaled, memory-bounded bitmap from a Content or File URI.
 */
fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int = 512, reqHeight: Int = 512): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Asynchronous album artwork loader with memory-bounded LRU caching.
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
    val cacheKey = artworkUri ?: trackUri ?: ""

    val bitmapState by produceState<ImageBitmap?>(
        initialValue = if (cacheKey.isNotBlank()) ArtworkCache.get(cacheKey) else null,
        key1 = artworkUri,
        key2 = trackUri
    ) {
        if (cacheKey.isNotBlank()) {
            val cached = ArtworkCache.get(cacheKey)
            if (cached != null) {
                value = cached
                return@produceState
            }
        }

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
                        val bmp = decodeSampledBitmapFromUri(context, uri, 512, 512)
                        if (bmp != null) {
                            decoded = bmp.asImageBitmap()
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

            if (decoded != null && cacheKey.isNotBlank()) {
                ArtworkCache.put(cacheKey, decoded)
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
