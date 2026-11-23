package io.chalchitra

import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

data class Wall(
    val id: String,
    val title: String,
    val thumb: String,
    val full: String,
)

data class Page(val items: List<Wall>, val after: String?)

object Feed {
    /** Free NASA demo key (rate limited). Get your own at api.nasa.gov and paste it here. */
    private const val NASA_KEY = "DEMO_KEY"

    private val direct = Regex("\\.(jpe?g|png|webp)(\\?.*)?$", RegexOption.IGNORE_CASE)

    /** Sources whose cursor is a plain page number, so auto-change can jump to a random page. */
    fun isPaged(k: Kind) = k == Kind.WALLHAVEN || k == Kind.ARTIC || k == Kind.PICSUM

    fun load(src: Source, sort: Sort, after: String?): Page = when (src.kind) {
        Kind.BING -> bing()
        Kind.REDDIT -> reddit(src, sort, after)
        Kind.WALLHAVEN -> wallhaven(src, after)
        Kind.PICSUM -> picsum(after)
        Kind.APOD -> apod()
        Kind.ARTIC -> artic(after)
    }

    private fun bing(): Page {
        val arr = JSONObject(
            Net.text("https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=8&mkt=en-US")
        ).getJSONArray("images")
        val out = ArrayList<Wall>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val base = "https://www.bing.com" + o.getString("urlbase")
            val title = o.optString("title").ifEmpty { o.optString("copyright").substringBefore(" (") }
            out += Wall(o.getString("hsh"), title, base + "_480x800.jpg", base + "_1080x1920.jpg")
        }
        return Page(out, null)
    }

    private fun wallhaven(src: Source, after: String?): Page {
        val page = after?.toIntOrNull() ?: 1
        val j = JSONObject(Net.text("https://wallhaven.cc/api/v1/search?${src.extra}&page=$page"))
        val arr = j.getJSONArray("data")
        val out = ArrayList<Wall>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val th = o.getJSONObject("thumbs")
            val id = o.getString("id")
            out += Wall("wh$id", "Wallhaven $id", th.optString("large", th.optString("small")), o.getString("path"))
        }
        val last = j.optJSONObject("meta")?.optInt("last_page", page) ?: page
        return Page(out, if (page < last) (page + 1).toString() else null)
    }

    private fun picsum(after: String?): Page {
        val page = after?.toIntOrNull() ?: 1
        val arr = JSONArray(Net.text("https://picsum.photos/v2/list?page=$page&limit=30"))
        val (sw, sh) = 1080 to 2400
        val out = ArrayList<Wall>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.getString("id")
            out += Wall(
                "ps$id", "Photo by ${o.optString("author")}",
                "https://picsum.photos/id/$id/360/640",
                "https://picsum.photos/id/$id/$sw/$sh",
            )
        }
        return Page(out, if (arr.length() == 30 && page < 30) (page + 1).toString() else null)
    }

    private fun apod(): Page {
        val arr = JSONArray(Net.text("https://api.nasa.gov/planetary/apod?api_key=$NASA_KEY&count=24"))
        val out = ArrayList<Wall>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("media_type") != "image") continue
            val url = o.optString("url")
            if (url.isEmpty()) continue
            val hd = o.optString("hdurl").ifEmpty { url }
            out += Wall("ap" + o.optString("date"), o.optString("title"), url, hd)
        }
        // Random sample every call, so there is always "more".
        return Page(out, "more")
    }

    private fun artic(after: String?): Page {
        val page = after?.toIntOrNull() ?: 1
        val q = "https://api.artic.edu/api/v1/artworks/search?" +
            "query%5Bterm%5D%5Bis_public_domain%5D=true&fields=id,title,image_id&limit=40&page=$page"
        val j = JSONObject(Net.text(q))
        val iiif = j.optJSONObject("config")?.optString("iiif_url", "https://www.artic.edu/iiif/2")
            ?: "https://www.artic.edu/iiif/2"
        val arr = j.getJSONArray("data")
        val out = ArrayList<Wall>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val img = o.optString("image_id")
            if (img.isEmpty() || img == "null") continue
            out += Wall(
                "ar${o.getInt("id")}", o.optString("title"),
                "$iiif/$img/full/400,/0/default.jpg",
                "$iiif/$img/full/1686,/0/default.jpg",
            )
        }
        val total = j.optJSONObject("pagination")?.optInt("total_pages", page) ?: page
        return Page(out, if (page < total && page < 200) (page + 1).toString() else null)
    }

    /** Reddit blocks plain API clients, so try a browser UA across several hosts. */
    private fun redditText(sub: String, kind: String, query: String): String {
        var err: IOException? = null
        for ((host, ext) in listOf(
            "https://www.reddit.com" to ".json",
            "https://old.reddit.com" to ".json",
            "https://api.reddit.com" to "",
        )) {
            try {
                return Net.text("$host/r/$sub/$kind$ext?$query&raw_json=1", Net.BROWSER_UA)
            } catch (e: IOException) {
                err = e
            }
        }
        throw err ?: IOException("Reddit unreachable")
    }

    private fun reddit(src: Source, sort: Sort, after: String?): Page {
        val (kind, query) = when (sort) {
            Sort.HOT -> "hot" to "limit=50"
            Sort.WEEK -> "top" to "t=week&limit=50"
            Sort.MONTH -> "top" to "t=month&limit=50"
            Sort.ALL -> "top" to "t=all&limit=50"
        }
        val q = query + (if (after != null) "&after=$after" else "")
        val data = JSONObject(redditText(src.sub, kind, q)).getJSONObject("data")
        val kids = data.getJSONArray("children")
        val out = ArrayList<Wall>()
        for (i in 0 until kids.length()) {
            val d = kids.getJSONObject(i).getJSONObject("data")
            if (d.optBoolean("over_18")) continue
            if (d.optString("post_hint") != "image") continue
            val img = d.optJSONObject("preview")?.optJSONArray("images")?.optJSONObject(0) ?: continue
            val s = img.getJSONObject("source")
            val w = s.getInt("width")
            val h = s.getInt("height")
            if (src.portrait && h < w * 1.2) continue
            val link = d.optString("url_overridden_by_dest", d.optString("url"))
            val full = if (direct.containsMatchIn(link)) link else s.getString("url")
            val res = img.optJSONArray("resolutions")
            var thumb = s.getString("url")
            if (res != null) {
                for (j in 0 until res.length()) {
                    val r = res.getJSONObject(j)
                    if (r.getInt("width") >= 300) {
                        thumb = r.getString("url")
                        break
                    }
                }
            }
            out += Wall(d.getString("name"), d.optString("title"), thumb, full)
        }
        val next = data.optString("after", "")
        return Page(out, if (next.isEmpty() || next == "null") null else next)
    }
}
