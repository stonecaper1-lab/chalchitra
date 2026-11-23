package io.chalchitra

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("p", Context.MODE_PRIVATE)

    var autoOn: Boolean
        get() = sp.getBoolean("auto", false)
        set(v) = sp.edit().putBoolean("auto", v).apply()

    /** Minutes between changes. */
    var interval: Int
        get() = sp.getInt("iv", 360)
        set(v) = sp.edit().putInt("iv", v).apply()

    var sources: Set<String>
        get() = sp.getStringSet("src", null)?.toSet() ?: setOf("bing", "wh-dark")
        set(v) = sp.edit().putStringSet("src", v).apply()

    /** 0 = home, 1 = lock, 2 = both. */
    var autoTarget: Int
        get() = sp.getInt("tg", 2)
        set(v) = sp.edit().putInt("tg", v).apply()

    var dimOn: Boolean
        get() = sp.getBoolean("dim", true)
        set(v) = sp.edit().putBoolean("dim", v).apply()

    var dimStrength: Int
        get() = sp.getInt("ds", 55)
        set(v) = sp.edit().putInt("ds", v).apply()

    private val seen: List<String>
        get() = sp.getString("seen", "")!!.split(',').filter { it.isNotEmpty() }

    fun isSeen(id: String) = id in seen

    fun markSeen(id: String) {
        val l = (seen + id).takeLast(200)
        sp.edit().putString("seen", l.joinToString(",")).apply()
    }
}
