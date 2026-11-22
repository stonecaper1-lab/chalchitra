package io.chalchitra

enum class Kind { REDDIT, BING }

enum class Sort(val label: String) {
    HOT("Hot"),
    WEEK("Week"),
    MONTH("Month"),
    ALL("All time"),
}

/** [portrait] = keep only tall images (height >= 1.2 x width). */
data class Source(
    val id: String,
    val label: String,
    val kind: Kind,
    val sub: String = "",
    val portrait: Boolean = true,
)

object Sources {
    val all = listOf(
        Source("bing", "Bing daily", Kind.BING),
        Source("MobileWallpaper", "Mobile", Kind.REDDIT, "MobileWallpaper"),
        Source("phonewallpapers", "Phone", Kind.REDDIT, "phonewallpapers"),
        Source("iWallpaper", "iWallpaper", Kind.REDDIT, "iWallpaper"),
        Source("Amoledbackgrounds", "AMOLED", Kind.REDDIT, "Amoledbackgrounds"),
        Source("Verticalwallpapers", "Vertical", Kind.REDDIT, "Verticalwallpapers"),
        Source("androidthemes", "Android themes", Kind.REDDIT, "androidthemes"),
        Source("Homescreens", "Homescreens", Kind.REDDIT, "Homescreens"),
        Source("AnimePhoneWallpapers", "Anime", Kind.REDDIT, "AnimePhoneWallpapers"),
        Source("MinimalWallpaper", "Minimal", Kind.REDDIT, "MinimalWallpaper", portrait = false),
        Source("EarthPorn", "Earth", Kind.REDDIT, "EarthPorn", portrait = false),
        Source("SpacePorn", "Space", Kind.REDDIT, "SpacePorn", portrait = false),
    )

    fun byId(id: String): Source = all.firstOrNull { it.id == id } ?: all[0]
}
