package io.chalchitra

enum class Kind { REDDIT, BING, WALLHAVEN, PICSUM, APOD, ARTIC, GITHUB }

enum class Sort(val label: String) {
    HOT("Hot"),
    WEEK("Week"),
    MONTH("Month"),
    ALL("All time"),
}

/**
 * [portrait] = keep only tall images (Reddit only).
 * [extra] = raw query tail for Wallhaven, or "owner/repo" for GitHub ([sub] is then a path prefix).
 */
data class Source(
    val id: String,
    val label: String,
    val kind: Kind,
    val sub: String = "",
    val portrait: Boolean = true,
    val extra: String = "",
)

object Sources {
    private const val WH = "purity=100&atleast=1080x1920&ratios=portrait"

    val all = listOf(
        Source("bing", "Bing daily", Kind.BING),
        Source("wh", "Wallhaven", Kind.WALLHAVEN, extra = "$WH&categories=111&sorting=toplist&topRange=1M"),
        Source("wh-dark", "Wallhaven dark", Kind.WALLHAVEN, extra = "$WH&categories=111&colors=000000&sorting=toplist&topRange=1y"),
        Source("wh-anime", "Wallhaven anime", Kind.WALLHAVEN, extra = "$WH&categories=010&sorting=toplist&topRange=1M"),
        Source("picsum", "Photos", Kind.PICSUM),
        Source("apod", "NASA APOD", Kind.APOD),
        Source("artic", "Art", Kind.ARTIC),
        Source("gh-aes", "Aesthetic", Kind.GITHUB, "images/", extra = "D3Ext/aesthetic-wallpapers"),
        Source("gh-sc", "Scopic all", Kind.GITHUB, "", extra = "michaelScopic/Wallpapers"),
        Source("gh-sc-nature", "Scopic nature", Kind.GITHUB, "nature/", extra = "michaelScopic/Wallpapers"),
        Source("gh-sc-space", "Scopic space", Kind.GITHUB, "space/", extra = "michaelScopic/Wallpapers"),
        Source("gh-sc-anime", "Scopic anime", Kind.GITHUB, "anime/", extra = "michaelScopic/Wallpapers"),
        Source("gh-sc-cyber", "Scopic cyberpunk", Kind.GITHUB, "cyberpunk/", extra = "michaelScopic/Wallpapers"),
        Source("gh-sc-mono", "Scopic mono", Kind.GITHUB, "monochrome/", extra = "michaelScopic/Wallpapers"),
        Source("gh-sc-abstract", "Scopic abstract", Kind.GITHUB, extra = "michaelScopic/Wallpapers|abstract/"),
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
