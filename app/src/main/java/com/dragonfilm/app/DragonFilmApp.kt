package com.dragonfilm.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.dragonfilm.app.data.repository.MovieRepository
import com.dragonfilm.app.data.storage.AnalyticsManager
import com.dragonfilm.app.data.storage.AuthManager
import com.dragonfilm.app.data.storage.CloudSync
import com.dragonfilm.app.data.storage.LocalStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DragonFilmApp : Application(), ImageLoaderFactory {

    lateinit var localStore: LocalStore
        private set

    lateinit var authManager: AuthManager
        private set

    lateinit var movieRepository: MovieRepository
        private set

    lateinit var cloudSync: CloudSync
        private set

    lateinit var analyticsManager: AnalyticsManager
        private set

    override fun onCreate() {
        super.onCreate()
        localStore = LocalStore(this)
        authManager = AuthManager(this)
        movieRepository = MovieRepository()
        cloudSync = CloudSync(localStore, authManager)
        analyticsManager = AnalyticsManager(this, authManager)

        // Track App Open to D1
        analyticsManager.trackAppOpen()

        // Initial background cloud sync
        CoroutineScope(Dispatchers.IO).launch {
            cloudSync.sync()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
