package com.ayham.player.data

import com.ayham.player.BuildConfig
import com.google.gson.JsonParser
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * الأسماء العربية للأفلام والمسلسلات من TMDB (language=ar).
 * نطابق بالاسم الإنجليزي الأصلي فقط، فإذا لم تتطابق النتيجة بوضوح نرجع null ويبقى الاسم الأصلي.
 * This product uses the TMDB API but is not endorsed or certified by TMDB.
 */
object ArTitles {
    private val cache = ConcurrentHashMap<String, String>()
    private val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS).build()
    private val arabic = Regex("[\\u0600-\\u06FF]")

    fun hasArabic(s: String): Boolean = arabic.containsMatchIn(s)

    /** يرجع الاسم العربي أو null. */
    fun lookup(kind: Kind, name: String): String? {
        if (!Tmdb.enabled || kind == Kind.LIVE || hasArabic(name)) return null
        val core = Versions.core(name)
        if (core.length < 2) return null
        val key = kind.name + ":" + core
        cache[key]?.let { return if (it.isEmpty()) null else it }
        val found = runCatching { search(kind, core) }.getOrNull()
        cache[key] = found.orEmpty()
        return found
    }

    private fun search(kind: Kind, core: String): String? {
        val path = if (kind == Kind.SERIES) "tv" else "movie"
        val q = URLEncoder.encode(core, "UTF-8")
        val url = "https://api.themoviedb.org/3/search/$path?language=ar&include_adult=false&query=$q&api_key=" + BuildConfig.TMDB_KEY
        val json = client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) null else r.body?.string()
        } ?: return null
        val arr = JsonParser.parseString(json).asJsonObject.getAsJsonArray("results") ?: return null
        for (e in arr) {
            val o = e.asJsonObject
            fun s(k: String) = o.get(k)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
            val orig = s("original_title").ifEmpty { s("original_name") }
            val title = s("title").ifEmpty { s("name") }
            if (Versions.core(orig) != core && Versions.core(title) != core) continue
            if (hasArabic(title)) return title
        }
        return null
    }
}
