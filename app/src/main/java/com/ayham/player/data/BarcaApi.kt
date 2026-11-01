package com.ayham.player.data

import com.ayham.player.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * BARÇA ZONE: بيانات برشلونة من API-Football عبر خادم الترخيص (المفتاح سرّ على الخادم ولا يصل للتطبيق).
 * لا توجد بيانات وهمية: عند الفشل نرجع رمز الخطأ فقط.
 */
object BarcaApi {
    const val TEAM_ID = 529
    data class Resp(val ok: Boolean, val data: JsonElement? = null, val err: String = "", val msg: String = "", val stale: Boolean = false, val t: Long = 0L)

    private val gson = Gson()
    private val JSON = "application/json".toMediaType()
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    private val memo = ConcurrentHashMap<String, Pair<Long, Resp>>()

    // كاش قصير على الجهاز حتى لا نكرر نفس الطلب عند التنقل بين الأقسام (الخادم عنده كاش أطول).
    private fun ttl(ep: String) = when (ep) { "match" -> 45_000L; "fixtures" -> 60_000L; "standings" -> 120_000L; else -> 300_000L }

    val available: Boolean get() = BuildConfig.LICENSE_URL.isNotBlank()

    fun call(ep: String, extra: Map<String, Any?> = emptyMap()): Resp {
        if (!available) return Resp(false, err = "no_license")
        val code = LicenseManager.savedCode() ?: return Resp(false, err = "no_license")
        val key = ep + extra.toString()
        memo[key]?.let { (t, r) -> if (System.currentTimeMillis() - t < ttl(ep) && r.ok) return r }
        return try {
            val body = HashMap<String, Any?>(extra)
            body["ep"] = ep; body["code"] = code; body["device"] = LicenseManager.deviceId()
            val req = Request.Builder().url(BuildConfig.LICENSE_URL.trimEnd('/') + "/v1/barca").post(gson.toJson(body).toRequestBody(JSON)).build()
            client.newCall(req).execute().use { r ->
                if (r.code >= 500) return@use Resp(false, err = "net")
                val o = JsonParser.parseString(r.body?.string().orEmpty()).asJsonObject
                val res = if (o.get("ok")?.asBoolean == true) Resp(true, o.get("data"), stale = o.get("stale")?.asBoolean == true, t = o.get("t")?.asLong ?: 0L)
                else Resp(false, err = o.get("err")?.asString ?: "upstream", msg = o.get("msg")?.asString.orEmpty())
                if (res.ok) memo[key] = System.currentTimeMillis() to res
                res
            }
        } catch (e: Exception) { memo[key]?.second?.let { return it.copy(stale = true) } ?: Resp(false, err = "net") }
    }

    fun clearMemo() = memo.clear()
}

// ---------------- النماذج ----------------
data class BTeam(val id: Int, val name: String, val logo: String)
data class BFixture(
    val id: Int, val ts: Long, val st: String, val el: Int?, val venue: String,
    val league: String, val leagueLogo: String, val round: String,
    val h: BTeam, val a: BTeam, val gh: Int?, val ga: Int?, val pen: Pair<Int, Int>?,
) {
    val barcaHome: Boolean get() = h.id == BarcaApi.TEAM_ID
    val opp: BTeam get() = if (barcaHome) a else h
    val live: Boolean get() = st in BarcaStatus.LIVE
    val finished: Boolean get() = st in BarcaStatus.DONE
    val upcoming: Boolean get() = st in BarcaStatus.PRE
    val ms: Long get() = ts * 1000
    /** نتيجة برشلونة: 1 فوز، 0 تعادل، -1 خسارة، null غير معروفة. */
    val result: Int? get() {
        val g1 = gh ?: return null; val g2 = ga ?: return null
        val b = if (barcaHome) g1 - g2 else g2 - g1
        return if (b > 0) 1 else if (b < 0) -1 else 0
    }
}
object BarcaStatus {
    val LIVE = setOf("1H", "HT", "2H", "ET", "BT", "P", "LIVE", "INT")
    val DONE = setOf("FT", "AET", "PEN", "AWD", "WO")
    val PRE = setOf("NS", "TBD")
    fun label(st: String, ar: Boolean): String = if (ar) when (st) {
        "NS" -> "لم تبدأ"; "TBD" -> "يُحدد لاحقًا"; "1H" -> "الشوط الأول"; "HT" -> "استراحة"; "2H" -> "الشوط الثاني"; "ET" -> "وقت إضافي"
        "BT" -> "استراحة الإضافي"; "P" -> "ركلات الترجيح"; "FT" -> "انتهت"; "AET" -> "انتهت (إضافي)"; "PEN" -> "انتهت (ترجيح)"; "SUSP" -> "موقوفة"
        "INT" -> "متوقفة"; "PST" -> "مؤجلة"; "CANC" -> "ملغاة"; "ABD" -> "ملغاة"; "AWD", "WO" -> "فوز إداري"; "LIVE" -> "مباشر"; else -> st
    } else when (st) {
        "NS" -> "Not started"; "TBD" -> "TBD"; "1H" -> "1st half"; "HT" -> "Half time"; "2H" -> "2nd half"; "ET" -> "Extra time"; "BT" -> "Break"
        "P" -> "Penalties"; "FT" -> "Full time"; "AET" -> "After ET"; "PEN" -> "After pens"; "SUSP" -> "Suspended"; "INT" -> "Interrupted"
        "PST" -> "Postponed"; "CANC", "ABD" -> "Cancelled"; "AWD", "WO" -> "Awarded"; "LIVE" -> "Live"; else -> st
    }
}
data class BEvent(val minute: Int, val extra: Int?, val teamId: Int, val player: String, val assist: String, val type: String, val detail: String)
data class BLinePlayer(val number: Int?, val name: String, val pos: String, val grid: String)
data class BLineup(val teamId: Int, val formation: String, val coach: String, val xi: List<BLinePlayer>, val subs: List<BLinePlayer>)
data class BMatch(val fx: BFixture, val events: List<BEvent>, val stats: Map<Int, List<Pair<String, String>>>, val lineups: List<BLineup>)
data class BStanding(val rank: Int, val id: Int, val name: String, val logo: String, val pts: Int, val gd: Int, val p: Int, val w: Int, val d: Int, val l: Int, val gf: Int, val ga: Int, val form: String, val desc: String)
data class BSquadPlayer(val id: Int, val name: String, val age: Int?, val number: Int?, val pos: String, val photo: String)
data class BPlayerStat(val id: Int, val name: String, val photo: String, val pos: String, val apps: Int, val min: Int, val goals: Int, val ast: Int, val yc: Int, val rc: Int, val rating: Double?)
data class BCoach(val name: String, val age: Int?, val nat: String, val photo: String)

// ---------------- التحويل من JSON ----------------
private fun JsonObject.s(k: String): String = get(k)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.asString.orEmpty()
private fun JsonObject.i(k: String): Int? = get(k)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { runCatching { it.asInt }.getOrNull() }
private fun JsonObject.d(k: String): Double? = get(k)?.takeIf { !it.isJsonNull && it.isJsonPrimitive }?.let { runCatching { it.asDouble }.getOrNull() }
private fun JsonObject.o(k: String): JsonObject? = get(k)?.takeIf { it.isJsonObject }?.asJsonObject
private fun JsonObject.a(k: String): JsonArray = get(k)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
private fun JsonElement.obj(): JsonObject = asJsonObject

private fun team(o: JsonObject?) = BTeam(o?.i("id") ?: 0, o?.s("name").orEmpty(), o?.s("logo").orEmpty())

fun parseFixture(o: JsonObject): BFixture {
    val lg = o.o("lg"); val pen = o.a("pen")
    return BFixture(
        id = o.i("id") ?: 0, ts = o.get("ts")?.asLong ?: 0L, st = o.s("st"), el = o.i("el"), venue = o.s("venue"),
        league = lg?.s("name").orEmpty(), leagueLogo = lg?.s("logo").orEmpty(), round = lg?.s("round").orEmpty(),
        h = team(o.o("h")), a = team(o.o("a")), gh = o.i("gh"), ga = o.i("ga"),
        pen = if (pen.size() == 2 && !pen[0].isJsonNull && !pen[1].isJsonNull) pen[0].asInt to pen[1].asInt else null,
    )
}
fun parseFixtures(e: JsonElement): List<BFixture> = e.asJsonArray.map { parseFixture(it.obj()) }.sortedBy { it.ts }
fun parseMatch(e: JsonElement): BMatch {
    val o = e.obj()
    val stats = HashMap<Int, List<Pair<String, String>>>()
    o.a("stt").forEach { x -> val so = x.obj(); stats[so.i("tid") ?: 0] = so.a("s").map { it.obj().s("t") to it.obj().s("v") } }
    return BMatch(
        parseFixture(o),
        o.a("ev").map { x -> val v = x.obj(); BEvent(v.i("m") ?: 0, v.i("x"), v.i("tid") ?: 0, v.s("p"), v.s("a"), v.s("ty"), v.s("d")) },
        stats,
        o.a("lu").map { x ->
            val v = x.obj()
            fun pl(j: JsonElement) = j.obj().let { BLinePlayer(it.i("n"), it.s("name"), it.s("pos"), it.s("grid")) }
            BLineup(v.i("tid") ?: 0, v.s("form"), v.s("coach"), v.a("xi").map(::pl), v.a("sub").map(::pl))
        },
    )
}
fun parseStandings(e: JsonElement): List<BStanding> = e.asJsonArray.map { x ->
    val v = x.obj()
    BStanding(v.i("rank") ?: 0, v.i("id") ?: 0, v.s("name"), v.s("logo"), v.i("pts") ?: 0, v.i("gd") ?: 0, v.i("p") ?: 0, v.i("w") ?: 0, v.i("d") ?: 0, v.i("l") ?: 0, v.i("gf") ?: 0, v.i("ga") ?: 0, v.s("form"), v.s("desc"))
}
fun parseSquad(e: JsonElement): List<BSquadPlayer> = e.asJsonArray.map { x -> val v = x.obj(); BSquadPlayer(v.i("id") ?: 0, v.s("name"), v.i("age"), v.i("n"), v.s("pos"), v.s("photo")) }
fun parsePlayerStats(e: JsonElement): List<BPlayerStat> = e.asJsonArray.map { x ->
    val v = x.obj()
    BPlayerStat(v.i("id") ?: 0, v.s("name"), v.s("photo"), v.s("pos"), v.i("apps") ?: 0, v.i("min") ?: 0, v.i("goals") ?: 0, v.i("ast") ?: 0, v.i("yc") ?: 0, v.i("rc") ?: 0, v.d("rating"))
}
fun parseCoach(e: JsonElement): BCoach? = if (e.isJsonObject) e.obj().let { BCoach(it.s("name"), it.i("age"), it.s("nat"), it.s("photo")) } else null
