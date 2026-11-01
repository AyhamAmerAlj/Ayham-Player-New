package com.ayham.player.data

import com.google.gson.JsonParser
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

data class NextMatch(val home: String, val away: String, val league: String, val epochMs: Long)

/**
 * المباراة القادمة لبرشلونة من TheSportsDB (مفتاح المطوّر المجاني).
 * أي فشل في الشبكة أو شكل الرد يخفي البطاقة فقط، ولا يؤثر على التطبيق.
 */
object Matches {
    private const val BASE = "https://www.thesportsdb.com/api/v1/json/3/"
    private const val TTL = 30L * 60 * 1000
    private val client = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()
    @Volatile private var cached: Pair<Long, NextMatch?>? = null
    @Volatile private var teamId: String? = null

    private fun get(path: String): com.google.gson.JsonObject? =
        client.newCall(Request.Builder().url(BASE + path).build()).execute().use { r ->
            if (!r.isSuccessful) null else r.body?.string()?.let { JsonParser.parseString(it).asJsonObject }
        }

    private fun teamId(): String {
        teamId?.let { return it }
        val id = runCatching {
            get("searchteams.php?t=Barcelona")?.getAsJsonArray("teams")?.map { it.asJsonObject }?.firstOrNull {
                it.get("strTeam")?.asString == "Barcelona" && it.get("strSport")?.asString == "Soccer"
            }?.get("idTeam")?.asString
        }.getOrNull() ?: "133739"
        teamId = id
        return id
    }

    private fun parseTime(o: com.google.gson.JsonObject): Long? {
        fun s(k: String) = o.get(k)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
        val ts = s("strTimestamp").ifEmpty { s("dateEvent") + "T" + s("strTime").ifEmpty { "00:00:00" } }
        if (ts.length < 19) return null
        val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return runCatching { f.parse(ts.substring(0, 19))?.time }.getOrNull()
    }

    fun next(): NextMatch? {
        val now = System.currentTimeMillis()
        cached?.let { if (now - it.first < TTL) return it.second }
        val res = runCatching {
            val events = get("eventsnext.php?id=" + teamId())?.getAsJsonArray("events") ?: return@runCatching null
            events.mapNotNull { e ->
                val o = e.asJsonObject
                fun s(k: String) = o.get(k)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
                val home = s("strHomeTeam"); val away = s("strAwayTeam")
                if (!home.contains("Barcelona", true) && !away.contains("Barcelona", true)) return@mapNotNull null
                val t = parseTime(o) ?: return@mapNotNull null
                if (t < now - 2 * 3600_000L) null else NextMatch(home, away, s("strLeague"), t)
            }.minByOrNull { it.epochMs }
        }.getOrNull()
        cached = now to res
        return res
    }
}
