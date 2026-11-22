package io.chalchitra

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Net {
    private const val UA = "android:io.chalchitra:1.0 (FOSS wallpaper app)"

    private fun open(url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", UA)
        return c
    }

    private fun check(c: HttpURLConnection) {
        val code = c.responseCode
        if (code !in 200..299) {
            throw IOException(if (code == 429 || code == 403) "Rate limited (HTTP $code), try later" else "HTTP $code")
        }
    }

    fun bytes(url: String): ByteArray {
        val c = open(url)
        try {
            check(c)
            return c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
    }

    fun text(url: String): String = String(bytes(url), Charsets.UTF_8)

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
