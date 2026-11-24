package io.chalchitra

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Net {
    private const val UA = "android:io.chalchitra:1.0 (FOSS wallpaper app)"
    const val BROWSER_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

    private fun open(url: String, ua: String = UA): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", ua)
        c.setRequestProperty("Accept", "*/*")
        return c
    }

    private fun check(c: HttpURLConnection) {
        val code = c.responseCode
        if (code !in 200..299) {
            throw IOException(
                when (code) {
                    403 -> "Server said no (HTTP 403). Try another source, there are plenty."
                    404 -> "Not found (HTTP 404). That repo or page may be gone."
                    429 -> "Slow down (HTTP 429). Try again in a minute."
                    else -> "HTTP $code"
                }
            )
        }
    }

    fun bytes(url: String, ua: String = UA): ByteArray {
        val c = open(url, ua)
        try {
            check(c)
            return c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
    }

    fun text(url: String, ua: String = UA): String = String(bytes(url, ua), Charsets.UTF_8)

    /** Streams straight to disk so big images never sit in the heap. */
    fun download(url: String, dest: File) {
        val c = open(url)
        try {
            check(c)
            c.inputStream.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
        } finally {
            c.disconnect()
        }
    }
}
