package com.ayham.player.data

import com.ayham.player.BuildConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** تحليل طلب المستخدم بلغته الطبيعية (عربي/إنجليزي) إلى فلاتر. */
data class SmartQ(
    val series: Boolean? = null,
    val minRating: Double? = null,
    val from: Int? = null,
    val to: Int? = null,
    val genres: List<String> = emptyList(),
    val noGore: Boolean = false,
    val similarTo: String? = null,
    val words: Int = 0,
) {
    val hasFilters get() = minRating != null || from != null || genres.isNotEmpty() || noGore
    /** وصف حرّ (قصة) بلا فلاتر: يُرسل للذكاء الاصطناعي. */
    val isStory get() = !hasFilters && similarTo == null && words >= 4
    val natural get() = hasFilters || similarTo != null || isStory
}

data class SmartTitle(val t: TmdbTitle, val series: Boolean)

object Smart {
    private val client = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build()

    private fun norm(s: String): String {
        val digits = "٠١٢٣٤٥٦٧٨٩"
        val sb = StringBuilder()
        for (c in s.lowercase()) {
            val i = digits.indexOf(c)
            sb.append(if (i >= 0) ('0' + i) else when (c) { 'أ', 'إ', 'آ' -> 'ا'; 'ى' -> 'ي'; 'ة' -> 'ه'; else -> c })
        }
        return sb.toString()
    }

    // أولوية: العبارات الأطول أولاً (مثل خيال علمي قبل خيال)
    private val genreWords = linkedMapOf(
        "scifi" to listOf("خيال علمي", "sci-fi", "scifi", "science fiction"),
        "action" to listOf("اكشن", "حركه", "action"),
        "adventure" to listOf("مغامره", "مغامرات", "adventure"),
        "animation" to listOf("انيميشن", "كرتون", "انمي", "animation", "anime"),
        "comedy" to listOf("كوميدي", "كوميديا", "مضحك", "comedy"),
        "crime" to listOf("جريمه", "جرائم", "crime"),
        "documentary" to listOf("وثائقي", "documentary"),
        "drama" to listOf("دراما", "drama"),
        "family" to listOf("عائلي", "عائليه", "للعائله", "اطفال", "family", "kids"),
        "fantasy" to listOf("فانتازيا", "خيال", "fantasy"),
        "history" to listOf("تاريخي", "تاريخ", "history"),
        "horror" to listOf("رعب", "مرعب", "horror"),
        "mystery" to listOf("غموض", "لغز", "mystery"),
        "romance" to listOf("رومانسي", "رومانسيه", "romance"),
        "thriller" to listOf("اثاره", "تشويق", "thriller"),
        "war" to listOf("حرب", "war"),
    )
    private val movieIds = mapOf("action" to 28, "adventure" to 12, "animation" to 16, "comedy" to 35, "crime" to 80, "documentary" to 99,
        "drama" to 18, "family" to 10751, "fantasy" to 14, "history" to 36, "horror" to 27, "mystery" to 9648, "romance" to 10749,
        "scifi" to 878, "thriller" to 53, "war" to 10752)
    private val tvIds = mapOf("action" to 10759, "adventure" to 10759, "animation" to 16, "comedy" to 35, "crime" to 80, "documentary" to 99,
        "drama" to 18, "family" to 10751, "fantasy" to 10765, "mystery" to 9648, "scifi" to 10765, "war" to 10768, "history" to 18)

    fun parse(raw: String): SmartQ {
        var s = " " + norm(raw).trim() + " "
        val words = s.trim().split(Regex("\\s+")).size
        var series: Boolean? = null
        if (Regex("(مسلسل|مسلسلات|series|\\bshows?\\b)").containsMatchIn(s)) series = true
        else if (Regex("(فيلم|افلام|movies?|\\bfilms?\\b)").containsMatchIn(s)) series = false
        var similar: String? = null
        Regex("(?:تشبه|شبيه ب|شبيهه ب|مثل|similar to|like)\\s+(.{2,60})").find(s)?.let { similar = it.groupValues[1].trim(); s = s.replace(it.value, " ") }
        var min: Double? = null
        Regex("(?:اعلي من|اعلى من|اكثر من|فوق|above|over|higher than|rating)\\s*(\\d(?:\\.\\d)?)").find(s)?.let { min = it.groupValues[1].toDoubleOrNull()?.takeIf { v -> v in 1.0..9.9 }; s = s.replace(it.value, " ") }
        var from: Int? = null; var to: Int? = null
        val yr = "((?:19|20)\\d\\d)"
        val range = Regex("(?:من|from)?\\s*$yr\\s*(?:الي|الى|to|-|حتي|حتى)\\s*$yr").find(s)
        if (range != null) { from = range.groupValues[1].toInt(); to = range.groupValues[2].toInt(); s = s.replace(range.value, " ") }
        else {
            Regex("(?:بعد|after|since)\\s*$yr").find(s)?.let { from = it.groupValues[1].toInt(); s = s.replace(it.value, " ") }
            if (from == null) Regex("\\b$yr\\b").find(s)?.let { from = it.groupValues[1].toInt(); to = from; s = s.replace(it.value, " ") }
        }
        val noGore = Regex("(بدون (?:مشاهد )?(?:دم|دماء|دموي|عنف)|no gore|without gore|not violent)").containsMatchIn(s)
        val genres = ArrayList<String>()
        for ((g, ws) in genreWords) {
            val w = ws.firstOrNull { s.contains(it) } ?: continue
            genres.add(g); s = s.replace(w, " ")
        }
        return SmartQ(series, min, from, to, genres, noGore, similar, words)
    }

    private fun titles(arr: com.google.gson.JsonArray?): List<TmdbTitle> = arr?.mapNotNull { e ->
        val o = e.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        fun s(k: String) = o.get(k)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
        val date = s("release_date").ifEmpty { s("first_air_date") }
        TmdbTitle(s("title").ifEmpty { s("name") }, s("original_title").ifEmpty { s("original_name") }, date.take(4).toIntOrNull() ?: 0)
    }.orEmpty()

    private fun get(url: String): JsonObject? = runCatching {
        client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) null else JsonParser.parseString(r.body?.string().orEmpty()).asJsonObject
        }
    }.getOrNull()

    private const val B = "https://api.themoviedb.org/3"
    private val key get() = BuildConfig.TMDB_KEY

    /** اكتشاف حسب الفلاتر (تصنيف/تقييم/سنوات) من TMDB. */
    fun discover(q: SmartQ, series: Boolean): List<TmdbTitle> {
        if (!Tmdb.enabled) return emptyList()
        val ids = q.genres.mapNotNull { (if (series) tvIds else movieIds)[it] }.distinct()
        if (q.genres.isNotEmpty() && ids.isEmpty()) return emptyList()
        val sb = StringBuilder("$B/discover/${if (series) "tv" else "movie"}?api_key=$key&language=en-US&include_adult=false")
        if (ids.isNotEmpty()) sb.append("&with_genres=").append(ids.joinToString(","))
        if (q.noGore) sb.append("&without_genres=").append(if (series) "80" else "27,53")
        if (q.minRating != null) sb.append("&vote_average.gte=${q.minRating}&vote_count.gte=300&sort_by=vote_average.desc")
        else sb.append("&sort_by=popularity.desc")
        val d = if (series) "first_air_date" else "primary_release_date"
        q.from?.let { sb.append("&$d.gte=$it-01-01") }
        q.to?.let { sb.append("&$d.lte=$it-12-31") }
        return (1..2).flatMap { p -> titles(get("$sb&page=$p")?.getAsJsonArray("results")) }
    }

    /** أعمال مشابهة (قصة وأجواء) من توصيات TMDB. */
    fun recommendations(title: String, year: String, series: Boolean): List<TmdbTitle> {
        if (!Tmdb.enabled || title.length < 2) return emptyList()
        val kind = if (series) "tv" else "movie"
        val enc = java.net.URLEncoder.encode(title, "UTF-8")
        val y = year.take(4).takeIf { it.length == 4 }?.let { (if (series) "&first_air_date_year=" else "&year=") + it }.orEmpty()
        val want = title.lowercase().filter { it.isLetterOrDigit() }
        val hit = get("$B/search/$kind?api_key=$key&query=$enc$y")?.getAsJsonArray("results")?.mapNotNull { it.asJsonObject }
            ?.firstOrNull { o ->
                fun n(k: String) = o.get(k)?.takeIf { !it.isJsonNull }?.asString.orEmpty().lowercase().filter { it.isLetterOrDigit() }
                want == n("title") || want == n("name") || want == n("original_title") || want == n("original_name")
            } ?: return emptyList()
        val id = hit.get("id").asInt
        val rec = titles(get("$B/$kind/$id/recommendations?api_key=$key&language=en-US")?.getAsJsonArray("results"))
        return rec.ifEmpty { titles(get("$B/$kind/$id/similar?api_key=$key&language=en-US")?.getAsJsonArray("results")) }
    }

    /** وصف قصة عمل: يحدد العناوين المحتملة عبر خادمنا (ذكاء اصطناعي)، ثم نطابقها مع مكتبتك فقط. */
    fun story(q: String): List<SmartTitle> {
        if (BuildConfig.LICENSE_URL.isBlank()) return emptyList()
        val body = JsonObject().apply { addProperty("q", q.take(240)) }.toString()
        return runCatching {
            val req = Request.Builder().url(BuildConfig.LICENSE_URL.trimEnd('/') + "/v1/ai")
                .post(body.toRequestBody("application/json".toMediaType())).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@use emptyList<SmartTitle>()
                val o = JsonParser.parseString(r.body?.string().orEmpty()).asJsonObject
                if (o.get("ok")?.asBoolean != true) return@use emptyList()
                o.getAsJsonArray("list").map { e ->
                    val x = e.asJsonObject
                    SmartTitle(TmdbTitle(x.get("t").asString, "", x.get("y")?.asInt ?: 0), x.get("k")?.asString == "tv")
                }
            }
        }.getOrDefault(emptyList())
    }
}
