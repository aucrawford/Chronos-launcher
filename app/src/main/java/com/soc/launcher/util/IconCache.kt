package com.soc.launcher.util

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object IconCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8th of available memory

    private val cache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    suspend fun getIcon(context: Context, packageName: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(packageName)?.let { return@withContext it }

        return@withContext try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val originalBitmap = drawable.toBitmap()
            
            // Scale down to a reasonable size for a launcher (e.g., 144px) to save memory
            val size = 144 
            val bitmap = if (originalBitmap.width > size || originalBitmap.height > size) {
                Bitmap.createScaledBitmap(originalBitmap, size, size, true)
            } else {
                originalBitmap
            }

            cache.put(packageName, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        cache.evictAll()
    }
}
