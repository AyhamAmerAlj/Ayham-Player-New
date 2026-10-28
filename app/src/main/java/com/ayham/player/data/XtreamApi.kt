package com.ayham.player.data

import android.net.Uri
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private fun JsonObject.s(key: String): String {
    val e = get(key) ?: return ""
    if (e.isJsonNull) return ""
    if (e.isJsonPrimitive) return e.asString ?: ""
    if (e.isJsonArray && e.asJsonArray.size() > 0) {
        val f = e.asJsonArray[0]
        if (f.isJsonPrimitive) return f.asString ?: ""
    }
    return ""
}

private fun JsonObject.o(key: String): JsonObject? {
    val e = get(key) ?: return null
    return if (e.isJsonObject) e.asJsonObject else null
}

private fun JsonElement?.arr(): JsonArray =
    if (this != null && this.isJsonArray) this.asJsonArray else JsonArray()

class XtreamApi(val account: Account) {

    companion object {
        val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        fun normalizeServer(raw: String): String {
            var s = raw.trim()
            if (s.isEmpty()) return s
            if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) s = "http://$s"
            while (s.endsWith("/")) s = s.dropLast(1)
            // لو الشخص كتب رابط كامل مثل .../player_api.php أو get.php نحذف الباقي
            val idx = s.indexOf("/player_api.php")
            if (idx > 0) s = s.substring(0, idx)
            val idx2 = s.indexOf("/get.php")
            if (idx2 > 0) s = s.substring(0, idx2)
            return s
        }
    }

    private fun fetch(action: String?, params: Map<String, String> = emptyMap()): JsonElement {
        val base = account.server.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("عنوان السيرفر غير صحيح")
        val b = base.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", account.user)
            .addQueryParameter("password", account.pass)
        if (action != null) b.addQueryParameter("action", action)
        params.forEach { (k, v) -> b.addQueryParameter(k, v) }
        val req = Request.Builder().url(b.build()).header("User-Agent", "AyhamPlayer/1.0").build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val body = resp.body ?: throw IllegalStateException("empty")
            return JsonParser.parseReader(body.charStream())
        }
    }

    private fun b64(s: String): String =
        runCatching { String(android.util.Base64.decode(s, android.util.Base64.DEFAULT), Charsets.UTF_8) }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: s

    /** دليل البرامج: قصير (الحالي والقادم) أو كامل لو full. */
    suspend fun epg(streamId: String, full: Boolean = false): List<EpgEntry> = withContext(Dispatchers.IO) {
        val root = if (full) fetch("get_simple_data_table", mapOf("stream_id" to streamId))
        else fetch("get_short_epg", mapOf("stream_id" to streamId, "limit" to "6"))
        if (!root.isJsonObject) return@withContext emptyList()
        root.asJsonObject.get("epg_listings").arr().mapNotNull { e ->
            if (!e.isJsonObject) return@mapNotNull null
            val o = e.asJsonObject
            val st = o.s("start_timestamp").toLongOrNull() ?: return@mapNotNull null
            val en = o.s("stop_timestamp").toLongOrNull() ?: o.s("end_timestamp").toLongOrNull() ?: return@mapNotNull null
            val title = b64(o.s("title"))
            if (title.isBlank()) null else EpgEntry(title, b64(o.s("description")), st, en)
        }.sortedBy { it.start }
    }

    suspend fun login(): AccountInfo = withContext(Dispatchers.IO) {
        val root = fetch(null)
        if (!root.isJsonObject) throw IllegalStateException("رد غير متوقع من السيرفر")
        val ui = root.asJsonObject.o("user_info") ?: throw IllegalStateException("بيانات الدخول غير صحيحة")
        val auth = ui.s("auth")
        if (auth == "0") throw IllegalStateException("اسم المستخدم أو كلمة المرور غير صحيحة")
        val exp = ui.s("exp_date")
        val expText = exp.toLongOrNull()?.let {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(it * 1000))
        } ?: ""
        AccountInfo(ui.s("status"), expText, ui.s("max_connections"))
    }

    /** سجل واحد يُعاد استخدامه لكل عنصر: يقلل الذاكرة والـ GC على الأجهزة الضعيفة. */
    class Rec(private val names: Array<String>) {
        private val vals = arrayOfNulls<String>(names.size)
        fun clear() { java.util.Arrays.fill(vals, null) }
        fun indexOf(n: String): Int { for (i in names.indices) if (names[i] == n) return i; return -1 }
        fun set(i: Int, v: String) { vals[i] = v }
        operator fun get(n: String): String? { val i = indexOf(n); return if (i < 0) null else vals[i] }
    }

    /** قراءة تدريجية: لا نبني شجرة JSON كاملة بالذاكرة، ونشتغل بأولوية منخفضة حتى لا نثقل الشاشة. */
    private val calls = java.util.concurrent.ConcurrentHashMap<String, okhttp3.Call>()

    /** إلغاء تحميل قسم جارٍ (live / movie / series). */
    fun cancel(tag: String) { calls.remove(tag)?.cancel() }

    private fun streamEach(
        action: String,
        wanted: Set<String>,
        tag: String,
        onProgress: (Int) -> Unit = {},
        onItem: (Rec) -> Unit,
    ) {
        val base = account.server.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("عنوان السيرفر غير صحيح")
        val url = base.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", account.user)
            .addQueryParameter("password", account.pass)
            .addQueryParameter("action", action)
            .build()
        val req = Request.Builder().url(url).header("User-Agent", "AyhamPlayer/1.0").build()
        val t = Thread.currentThread()
        val oldPriority = t.priority
        t.priority = Thread.MIN_PRIORITY
        try {
            val call = client.newCall(req)
            calls[tag] = call
            call.execute().use { resp ->
                if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
                val body = resp.body ?: throw IllegalStateException("empty")
                JsonReader(body.charStream().buffered(64 * 1024)).use { r ->
                    r.isLenient = true
                    if (r.peek() != JsonToken.BEGIN_ARRAY) return
                    val rec = Rec(wanted.toTypedArray())
                    r.beginArray()
                    var n = 0
                    while (r.hasNext()) {
                        if (r.peek() != JsonToken.BEGIN_OBJECT) { r.skipValue(); continue }
                        rec.clear()
                        r.beginObject()
                        while (r.hasNext()) {
                            val i = rec.indexOf(r.nextName())
                            if (i >= 0) {
                                rec.set(
                                    i,
                                    when (r.peek()) {
                                        JsonToken.STRING, JsonToken.NUMBER -> r.nextString()
                                        JsonToken.BOOLEAN -> r.nextBoolean().toString()
                                        JsonToken.NULL -> { r.nextNull(); "" }
                                        else -> { r.skipValue(); "" }
                                    },
                                )
                            } else {
                                r.skipValue()
                            }
                        }
                        r.endObject()
                        onItem(rec)
                        if (++n % 500 == 0) { onProgress(n); Thread.yield() }
                    }
                    r.endArray()
                    onProgress(n)
                }
            }
        } finally {
            calls.remove(tag)
            t.priority = oldPriority
        }
    }

    private val catFields = setOf("category_id", "category_name")

    private fun cats(action: String): List<Category> {
        val out = ArrayList<Category>()
        streamEach(action, catFields, "cats") { m ->
            out.add(Category(m["category_id"].orEmpty(), m["category_name"].orEmpty()))
        }
        return out.distinctBy { it.id }
    }

    private val liveFields = setOf("stream_id", "name", "stream_icon", "category_id", "added")
    private val vodFields = setOf("stream_id", "name", "stream_icon", "category_id", "rating", "container_extension", "added")
    private val seriesFields = setOf("series_id", "name", "cover", "category_id", "rating", "last_modified")

    /** يحمّل قسمًا كاملًا ويكتبه مباشرة في قاعدة البيانات (بدون تخزينه بالذاكرة). */
    suspend fun loadInto(kind: Kind, db: CatalogDb, onProgress: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val catAction: String
        val listAction: String
        val fields: Set<String>
        val tag: String
        when (kind) {
            Kind.LIVE -> { catAction = "get_live_categories"; listAction = "get_live_streams"; fields = liveFields; tag = "live" }
            Kind.MOVIE -> { catAction = "get_vod_categories"; listAction = "get_vod_streams"; fields = vodFields; tag = "movie" }
            Kind.SERIES -> { catAction = "get_series_categories"; listAction = "get_series"; fields = seriesFields; tag = "series" }
        }
        val categories = runCatching { cats(catAction) }.getOrDefault(emptyList())
        db.clearKind(kind)
        val w = db.writer(kind)
        try {
            streamEach(listAction, fields, tag, onProgress) { m ->
                val id = (if (kind == Kind.SERIES) m["series_id"] else m["stream_id"]).orEmpty()
                if (id.isNotEmpty()) {
                    when (kind) {
                        Kind.LIVE -> w.add(id, m["name"].orEmpty(), m["stream_icon"].orEmpty(), m["category_id"].orEmpty(), 0.0, "", m["added"]?.toLongOrNull() ?: 0L)
                        Kind.MOVIE -> w.add(
                            id, m["name"].orEmpty(), m["stream_icon"].orEmpty(), m["category_id"].orEmpty(),
                            m["rating"]?.toDoubleOrNull() ?: 0.0, m["container_extension"].orEmpty().ifEmpty { "mp4" },
                            m["added"]?.toLongOrNull() ?: 0L,
                        )
                        Kind.SERIES -> w.add(
                            id, m["name"].orEmpty(), m["cover"].orEmpty(), m["category_id"].orEmpty(),
                            m["rating"]?.toDoubleOrNull() ?: 0.0, "", m["last_modified"]?.toLongOrNull() ?: 0L,
                        )
                    }
                }
            }
            w.commit()
        } catch (e: Throwable) {
            w.abort()
            db.clearKind(kind)
            throw e
        }
        db.replaceCats(kind, categories)
    }

    suspend fun movieDetails(id: String): Details = withContext(Dispatchers.IO) {
        val root = fetch("get_vod_info", mapOf("vod_id" to id))
        val info = if (root.isJsonObject) root.asJsonObject.o("info") else null
        if (info == null) Details() else Details(
            plot = info.s("plot").ifEmpty { info.s("description") },
            cast = info.s("cast").ifEmpty { info.s("actors") },
            genre = info.s("genre"),
            year = info.s("releasedate").take(4).ifEmpty { info.s("releaseDate").take(4) },
            duration = info.s("duration"),
            rating = info.s("rating"),
            backdrop = info.s("backdrop_path"),
            trailer = info.s("youtube_trailer"),
        )
    }

    suspend fun seriesInfo(id: String): SeriesInfo = withContext(Dispatchers.IO) {
        val root = fetch("get_series_info", mapOf("series_id" to id))
        if (!root.isJsonObject) return@withContext SeriesInfo(Details(), emptyMap())
        val r = root.asJsonObject
        val info = r.o("info")
        val details = if (info == null) Details() else Details(
            plot = info.s("plot"),
            cast = info.s("cast"),
            genre = info.s("genre"),
            year = info.s("releaseDate").take(4).ifEmpty { info.s("release_date").take(4) },
            duration = info.s("episode_run_time"),
            rating = info.s("rating"),
            backdrop = info.s("backdrop_path"),
            trailer = info.s("youtube_trailer"),
        )
        val out = linkedMapOf<Int, List<Episode>>()
        val eps = r.get("episodes")

        fun parseEp(e: JsonElement, fallbackSeason: Int): Episode? {
            if (!e.isJsonObject) return null
            val o = e.asJsonObject
            val inf = o.o("info")
            val season = o.s("season").toIntOrNull() ?: fallbackSeason
            return Episode(
                id = o.s("id"),
                season = season,
                num = o.s("episode_num").toIntOrNull() ?: 0,
                title = o.s("title").ifEmpty { "Episode " + o.s("episode_num") },
                ext = o.s("container_extension").ifEmpty { "mp4" },
                image = inf?.s("movie_image") ?: "",
                plot = inf?.s("plot") ?: "",
                duration = inf?.s("duration") ?: "",
            )
        }

        if (eps != null && eps.isJsonObject) {
            for ((k, v) in eps.asJsonObject.entrySet()) {
                val season = k.toIntOrNull() ?: continue
                out[season] = v.arr().mapNotNull { parseEp(it, season) }.sortedBy { it.num }
            }
        } else if (eps != null && eps.isJsonArray) {
            var i = 1
            for (sea in eps.asJsonArray) {
                if (sea.isJsonArray) {
                    out[i] = sea.asJsonArray.mapNotNull { parseEp(it, i) }.sortedBy { it.num }
                    i++
                }
            }
        }
        SeriesInfo(details, out.toSortedMap())
    }

    // ---------- روابط التشغيل ----------
    private fun enc(s: String) = Uri.encode(s)

    fun liveUrl(id: String, fmt: String) =
        "${account.server}/live/${enc(account.user)}/${enc(account.pass)}/$id.$fmt"

    fun movieUrl(id: String, ext: String) =
        "${account.server}/movie/${enc(account.user)}/${enc(account.pass)}/$id.$ext"

    fun episodeUrl(id: String, ext: String) =
        "${account.server}/series/${enc(account.user)}/${enc(account.pass)}/$id.$ext"
}
