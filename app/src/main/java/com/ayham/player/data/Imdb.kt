package com.ayham.player.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request

/** تقييم IMDb من خدمة Cinemeta العامة (بدون مفتاح). يرجع null لو ما لقى الفيلم. */
object Imdb {
    private const val BASE = "https://v3-cinemeta.strem.io"

    private fun clean(raw: String): String {
        var s = raw
        s = s.replace(Regex("\\[[^\\]]*\\]"), " ").replace(Regex("\\([^)]*\\)"), " ")
        s = s.replace(Regex("^\\s*[A-Za-z]{2,3}\\s*[-:|]\\s*"), "")
        s = s.replace(Regex("\\b(19|20)\\d{2}\\b"), " ")
        s = s.replace(Regex("\\b(4k|uhd|fhd|hd|hdr|1080p|720p|multi|dubbed|arabic|sub)\\b", RegexOption.IGNORE_CASE), " ")
        return s.replace(Regex("\\s+"), " ").trim()
    }

    private fun key(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private fun get(client: OkHttpClient, url: String): JsonObject? =
        runCatching {
            client.newCall(Request.Builder().url(url).header("User-Agent", "AyhamPlayer/1.0").build()).execute().use { r ->
                if (!r.isSuccessful) return@use null
                JsonParser.parseString(r.body?.string().orEmpty()).asJsonObject
            }
        }.getOrNull()

    private fun str(o: JsonObject, k: String): String =
        o.get(k)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()

    fun fetch(client: OkHttpClient, title: String, year: String, series: Boolean): String? {
        val q = clean(title)
        if (q.length < 2) return null
        val type = if (series) "series" else "movie"
        val enc = URLEncoder.encode(q, "UTF-8").replace("+", "%20")
        val root = get(client, "$BASE/catalog/$type/top/search=$enc.json") ?: return null
        val metas = root.getAsJsonArray("metas") ?: return null
        val list = metas.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
        if (list.isEmpty()) return null
        val want = key(q)
        val y = year.take(4).toIntOrNull()
        fun yearOk(o: JsonObject): Boolean {
            if (y == null) return true
            val oy = str(o, "releaseInfo").take(4).toIntOrNull() ?: str(o, "year").take(4).toIntOrNull() ?: return true
            return kotlin.math.abs(oy - y) <= 1
        }
        val pick = list.firstOrNull { key(str(it, "name")) == want && yearOk(it) }
            ?: list.firstOrNull { key(str(it, "name")) == want }
            ?: return null
        var rating = str(pick, "imdbRating")
        if (rating.isBlank()) {
            val id = str(pick, "imdb_id").ifBlank { str(pick, "id") }
            if (id.isNotBlank()) {
                val meta = get(client, "$BASE/meta/$type/$id.json")?.getAsJsonObject("meta")
                if (meta != null) rating = str(meta, "imdbRating")
            }
        }
        return rating.takeIf { it.isNotBlank() && it != "0" }
    }
}
