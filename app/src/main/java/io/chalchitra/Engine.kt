package io.chalchitra

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Shader
import android.os.Build
import android.view.WindowManager
import java.io.File
import java.io.IOException

object Engine {
    /** Portrait screen size in px. */
    fun screen(ctx: Context): Pair<Int, Int> {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val (a, b) = if (Build.VERSION.SDK_INT >= 30) {
            val r = wm.maximumWindowMetrics.bounds
            r.width() to r.height()
        } else {
            val p = Point()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(p)
            p.x to p.y
        }
        return minOf(a, b) to maxOf(a, b)
    }

    fun statusBarPx(ctx: Context): Int {
        val r = ctx.resources
        val id = r.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) r.getDimensionPixelSize(id) else (24 * r.displayMetrics.density).toInt()
    }

    /** Length of the dim gradient: status bar height plus a soft falloff. */
    fun dimLengthPx(ctx: Context): Int = (statusBarPx(ctx) * 2.5f).toInt()

    private fun cacheDir(ctx: Context) = File(ctx.cacheDir, "dl").apply { mkdirs() }

    fun clearCache(ctx: Context) {
        cacheDir(ctx).listFiles()?.forEach { it.delete() }
    }

    private fun cached(ctx: Context, url: String): File {
        val dir = cacheDir(ctx)
        val f = File(dir, Integer.toHexString(url.hashCode()) + ".img")
        if (!f.exists() || f.length() == 0L) {
            dir.listFiles()?.forEach { it.delete() }
            val tmp = File(dir, "part")
            Net.download(url, tmp)
            if (!tmp.renameTo(f)) throw IOException("cache rename failed")
        }
        return f
    }

    /**
     * Downloads [url] to disk, decodes only the centre-crop region that matches the
     * screen aspect ratio (so huge landscape photos never fully load into RAM)
     * and returns a screen-sized bitmap.
     */
    fun render(ctx: Context, url: String): Bitmap {
        val (sw, sh) = screen(ctx)
        val f = cached(ctx, url)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        val bw = bounds.outWidth
        val bh = bounds.outHeight
        if (bw <= 0 || bh <= 0) throw IOException("bad image")

        val ar = sw.toFloat() / sh
        val cw: Int
        val ch: Int
        if (bw.toFloat() / bh > ar) {
            ch = bh
            cw = (bh * ar).toInt().coerceAtLeast(1)
        } else {
            cw = bw
            ch = (bw / ar).toInt().coerceAtLeast(1)
        }
        val left = (bw - cw) / 2
        val top = (bh - ch) / 2
        val rect = Rect(left, top, left + cw, top + ch)

        var sample = 1
        while (cw / (sample * 2) >= sw) sample *= 2

        val dec = BitmapRegionDecoder.newInstance(f.path) ?: throw IOException("decoder")
        val decoded: Bitmap? = try {
            dec.decodeRegion(rect, BitmapFactory.Options().apply { inSampleSize = sample })
        } finally {
            dec.recycle()
        }
        val region = decoded ?: throw IOException("decode failed")

        if (region.width == sw && region.height == sh) return region
        val scaled = Bitmap.createScaledBitmap(region, sw, sh, true)
        if (scaled !== region) region.recycle()
        return scaled
    }

    private fun dimTop(b: Bitmap, strengthPct: Int, lengthPx: Int) {
        val a = (strengthPct.coerceIn(0, 100) * 255) / 100
        val p = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, lengthPx.toFloat(),
                Color.argb(a, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP,
            )
        }
        Canvas(b).drawRect(0f, 0f, b.width.toFloat(), lengthPx.toFloat(), p)
    }

    /** [target]: 0 = home, 1 = lock, 2 = both. Does not modify [src]. */
    fun apply(ctx: Context, src: Bitmap, target: Int, dim: Boolean, strength: Int) {
        val out = src.copy(Bitmap.Config.ARGB_8888, true) ?: throw IOException("copy failed")
        try {
            if (dim) dimTop(out, strength, dimLengthPx(ctx))
            val flags = when (target) {
                0 -> WallpaperManager.FLAG_SYSTEM
                1 -> WallpaperManager.FLAG_LOCK
                else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            WallpaperManager.getInstance(ctx).setBitmap(out, null, true, flags)
        } finally {
            out.recycle()
        }
    }
}
