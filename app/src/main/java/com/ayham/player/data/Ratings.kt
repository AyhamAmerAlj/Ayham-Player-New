package com.ayham.player.data

import com.ayham.player.BuildConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** تقييمات من مصادر مرخّصة (TMDB وOMDb وTrakt) عبر خادمنا، وكل مصدر مستقل: غياب أحدها لا يؤثر على الباقي. */
data class RatingsData(
    val tmdb: Pair<Double, Int>? = null,
    val imdb: Pair<Double, Int>? = null,
    val rt: Int? = null,
    val mc: Int? = null,
    val trakt: Pair<Double, Int>? = null,
    val rta: Int? = null,
    val lb: Pair<Double, Int>? = null,
    val imdbId: String = "",
    val tmdbId: Int = 0,
) {
    val any get() = tmdb != null || imdb != null || rt != null || mc != null || trakt != null || rta != null || lb != null
}

object Ratings {
    private const val TTL = 3L * 24 * 3600 * 1000

    private fun parse(o: JsonObject): RatingsData {
        fun pair(k: String): Pair<Double, Int>? = o.getAsJsonObject(k)?.let {
            val r = it.get("r")?.takeIf { x -> !x.isJsonNull }?.asDouble ?: return null
            r to (it.get("v")?.takeIf { x -> !x.isJsonNull }?.asInt ?: 0)
        }
        fun int(k: String) = o.get(k)?.takeIf { !it.isJsonNull }?.asInt
        return RatingsData(pair("tmdb"), pair("imdb"), int("rt"), int("mc"), pair("trakt"), int("rta"), pair("lb"),
            o.get("imdbId")?.takeIf { !it.isJsonNull }?.asString.orEmpty(), int("tmdbId") ?: 0)
    }

    fun fetch(client: OkHttpClient, store: Store, key: String, title: String, year: String, series: Boolean): RatingsData? {
        store.ratingsCache(key + "_v2")?.let { (t, j) ->
            if (System.currentTimeMillis() - t < TTL) return runCatching { parse(JsonParser.parseString(j).asJsonObject) }.getOrNull()
        }
        if (BuildConfig.LICENSE_URL.isBlank()) return null
        val body = JsonObject().apply {
            addProperty("title", Imdb.clean(title)); addProperty("year", year.take(4)); addProperty("type", if (series) "tv" else "movie")
        }.toString()
        return runCatching {
            val req = Request.Builder().url(BuildConfig.LICENSE_URL.trimEnd('/') + "/v1/ratings")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@use null
                val txt = r.body?.string().orEmpty()
                val o = JsonParser.parseString(txt).asJsonObject
                if (o.get("ok")?.asBoolean != true) return@use null
                store.setRatingsCache(key + "_v2", txt)
                parse(o)
            }
        }.getOrNull()
    }
}
