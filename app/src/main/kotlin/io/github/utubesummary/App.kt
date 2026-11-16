package io.github.utubesummary

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

private const val IMAGE_CACHE_MAX_BYTES = 50L * 1024 * 1024
private const val IMAGE_MEMORY_PERCENT = 0.15

@HiltAndroidApp
class App : Application(), ImageLoaderFactory {

    /** Shared by thumbnails and favicons: small, capped caches that never grow unbounded. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this).maxSizePercent(IMAGE_MEMORY_PERCENT).build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(IMAGE_CACHE_MAX_BYTES)
                .build()
        }
        // Thumbnails never change: keep them until evicted instead of revalidating.
        .respectCacheHeaders(false)
        .build()
}
