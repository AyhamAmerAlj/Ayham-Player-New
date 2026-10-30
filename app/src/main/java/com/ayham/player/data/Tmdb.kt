package com.ayham.player.data

import com.ayham.player.BuildConfig
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * قسم "رائج الآن": يجلب قائمة الرائج الأسبوعي من TMDB ثم نطابق العناوين مع قائمتك.
 * يحتاج مفتاح TMDB (يُضبط وقت البناء). بدون مفتاح يختفي القسم تلقائيًا.
 * This product uses the TMDB API but is not endorsed or certified by TMDB.
 */
data class TmdbTitle(val title: String, val original: String, val year: Int)

object Tmdb {
    val enabled: Boolean get() = BuildConfig.TMDB_KEY.isNotBlank()
    private val client = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build()
    private const val TTL = 12L * 3600 * 1000

    /** kind: "movie" أو "tv". */
    fun trending(store: Store, kind: String): List<TmdbTitle> {
        if (!enabled) return emptyList()
        val cached = store.trendCache(kind)
        if (cached != null && System.currentTimeMillis() - cached.first < TTL) return parse(cached.second)
        val json = try {
            val url = "https://api.themoviedb.org/3/trending/$kind/week?language=en-US&api_key=" + BuildConfig.TMDB_KEY
            client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) null else r.body?.string()
            }
        } catch (e: Exception) { null }
        if (json != null && parse(json).isNotEmpty()) { store.setTrendCache(kind, json); return parse(json) }
        return cached?.let { parse(it.second) } ?: emptyList()
    }

    private fun parse(json: String): List<TmdbTitle> = runCatching {
        val arr = JsonParser.parseString(json).asJsonObject.getAsJsonArray("results")
        arr.map { e ->
            val o = e.asJsonObject
            fun s(k: String) = o.get(k)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
            val title = s("title").ifEmpty { s("name") }
            val orig = s("original_title").ifEmpty { s("original_name") }
            val date = s("release_date").ifEmpty { s("first_air_date") }
            TmdbTitle(title, orig, date.take(4).toIntOrNull() ?: 0)
        }
    }.getOrDefault(emptyList())
}
