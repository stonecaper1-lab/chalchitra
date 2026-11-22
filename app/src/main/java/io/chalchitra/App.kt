package io.chalchitra

import android.app.Application
import android.net.http.HttpResponseCache
import java.io.File

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Free HTTP disk cache from the framework: no extra dependency, 24 MB cap.
        try {
            HttpResponseCache.install(File(cacheDir, "http"), 24L * 1024 * 1024)
        } catch (_: Exception) {
        }
    }
}
