package com.tindahan.tracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Local-only product images. Files live in the app-private product_images dir
 * (no permissions needed, no cloud). Source images are downsampled on save
 * (max 1024px) and decoded with inSampleSize for list display.
 * Only file NAMES are stored in the database.
 */
object ImageStore {
    const val DIR_NAME = "product_images"
    const val MAX_SAVED_DIM = 1024
    const val JPEG_QUALITY = 85
    private const val TAG = "ImageStore"
    private const val MAX_SOURCE_BYTES = 30_000_000

    // Small memory cache (~8MB): key = "$name@$sizePx", size in KB
    private val cache = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }

    // Capped decoder pool so fast scrolling can't saturate all IO threads.
    private val decoder = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    fun dir(context: Context): File = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    /**
     * Decode + downsample [uri] and save as JPEG. Returns file name, or null on failure.
     * Reads the full bytes first and decodes from memory: some content providers
     * hand out streams that BitmapFactory.decodeStream cannot handle, which made
     * every photo fail on affected devices.
     */
    fun saveFromUri(context: Context, uri: Uri, maxDim: Int = MAX_SAVED_DIM): String? {
        return try {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (t: Throwable) {
                Log.e(TAG, "saveFromUri: cannot open $uri", t)
                null
            } ?: return null
            if (bytes.isEmpty() || bytes.size > MAX_SOURCE_BYTES) {
                Log.e(TAG, "saveFromUri: bad size ${bytes.size} for $uri")
                return null
            }
            saveJpegBytes(context, bytes, maxDim)
        } catch (t: Throwable) {
            Log.e(TAG, "saveFromUri failed for $uri", t)
            null
        }
    }

    private fun saveJpegBytes(context: Context, bytes: ByteArray, maxDim: Int): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                Log.e(TAG, "saveJpegBytes: not an image (${bytes.size} bytes)")
                return null
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxDim)
                // Photos need no alpha; halves decode memory on low-end phones.
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: run {
                Log.e(TAG, "saveJpegBytes: decode failed")
                return null
            }
            val scaled = scaleDown(bmp, maxDim)
            if (scaled !== bmp) bmp.recycle()
            val name = UUID.randomUUID().toString() + ".jpg"
            FileOutputStream(File(dir(context), name)).use { out ->
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                    Log.e(TAG, "saveJpegBytes: compress failed")
                    return null
                }
            }
            if (!scaled.isRecycled) scaled.recycle()
            name
        } catch (t: Throwable) {
            Log.e(TAG, "saveJpegBytes failed", t)
            null
        }
    }

    /** Write raw bytes (e.g. from backup restore) as a new image file. Returns file name or null. */
    fun saveBytes(context: Context, bytes: ByteArray): String? {
        if (bytes.isEmpty() || bytes.size > 8_000_000) return null
        return saveJpegBytes(context, bytes, MAX_SAVED_DIM)
    }

    fun readBytes(context: Context, name: String): ByteArray? {
        return try {
            val f = File(dir(context), name)
            if (!f.exists() || f.length() > 8_000_000) null else f.readBytes()
        } catch (e: Exception) {
            null
        }
    }

    fun delete(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        try {
            File(dir(context), name).takeIf { it.exists() }?.delete()
            // Evict cached sizes for this file
            // (LruCache has no prefix eviction; entries expire naturally.)
        } catch (e: Exception) { /* ignore */
        }
    }

    /**
     * Load a downsampled bitmap for display; main-safe, cached in memory.
     * Decoding runs on a small dedicated pool to keep scrolling smooth.
     */
    suspend fun loadThumbnail(context: Context, name: String?, targetPx: Int): Bitmap? {
        if (name.isNullOrBlank()) return null
        val key = "$name@$targetPx"
        cache.get(key)?.let { return it }
        return withContext(decoder) {
            cache.get(key)?.let { return@withContext it }
            try {
                val f = File(dir(context), name)
                if (!f.exists()) return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.absolutePath, bounds)
                if (bounds.outWidth <= 0) return@withContext null
                val bmp = BitmapFactory.decodeFile(
                    f.absolutePath,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetPx) }
                ) ?: return@withContext null
                // Cap cache memory: skip caching huge bitmaps
                if (bmp.byteCount < 4_000_000) cache.put(key, bmp)
                bmp
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Power-of-two downsampling factor so the longest side fits within [maxDim]. Pure math, unit-testable. */
    fun sampleSize(width: Int, height: Int, maxDim: Int): Int {
        if (width <= 0 || height <= 0 || maxDim <= 0) return 1
        var sample = 1
        while ((width / sample) > maxDim || (height / sample) > maxDim) sample *= 2
        return sample
    }

    private fun scaleDown(bmp: Bitmap, maxDim: Int): Bitmap {
        val w = bmp.width
        val h = bmp.height
        if (w <= maxDim && h <= maxDim) return bmp
        val ratio = maxDim / maxOf(w, h).toFloat()
        return Bitmap.createScaledBitmap(bmp, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }
}
