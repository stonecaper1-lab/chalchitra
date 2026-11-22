package io.chalchitra

import org.json.JSONObject

data class Wall(
    val id: String,
    val title: String,
    val thumb: String,
    val full: String,
)

data class Page(val items: List<Wall>, val after: String?)

object Feed {
    private val direct = Regex("\\.(jpe?g|png|webp)(\\?.*)?$", RegexOption.IGNORE_CASE)

    fun load(src: Source, sort: Sort, after: String?): Page =
        if (src.kind == Kind.BING) bing() else reddit(src, sort, after)

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

    private fun reddit(src: Source, sort: Sort, after: String?): Page {
        val path = when (sort) {
            Sort.HOT -> "hot.json?limit=50"
            Sort.WEEK -> "top.json?t=week&limit=50"
            Sort.MONTH -> "top.json?t=month&limit=50"
            Sort.ALL -> "top.json?t=all&limit=50"
        }
        val url = "https://www.reddit.com/r/${src.sub}/$path&raw_json=1" +
            (if (after != null) "&after=$after" else "")
        val data = JSONObject(Net.text(url)).getJSONObject("data")
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
