package com.ayham.player.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.concurrent.TimeUnit
import com.ayham.player.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * مزامنة بيانات المشاهدة (المفضلة، شاهد لاحقًا، أكمل المشاهدة، السجل، تقدم كل فيلم وحلقة)
 * بين أجهزة نفس كود التفعيل. لكل ملف شخصي (الاسم) مساحة مستقلة على الخادم.
 * الدمج: لكل قائمة تفوز الأحدث تعديلًا، ولكل عنصر تقدم يفوز الأحدث.
 * ما يتأثر شي من بيانات الاشتراك (السيرفر وكلمة المرور) فهي لا تُرسل أبدًا.
 */
class SyncEngine(private val store: Store, private val onLists: (Map<String, List<Item>>) -> Unit) {
    private val gson = Gson()
    private val mutex = Mutex()
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    private val JSON = "application/json".toMediaType()
    private val names = listOf("fav", "later", "watched", "recent", "history")

    val available: Boolean get() = LicenseManager.enabled && LicenseManager.isOk && LicenseManager.savedCode() != null

    private fun profile(): String = store.profileName.trim().ifBlank { "main" }

    private fun post(path: String, extra: Map<String, Any?>): JsonObject? = try {
        val body = HashMap<String, Any?>(extra)
        body["code"] = LicenseManager.savedCode()
        body["device"] = LicenseManager.deviceId()
        body["profile"] = profile()
        val req = Request.Builder().url(BuildConfig.LICENSE_URL.trimEnd('/') + path)
            .post(gson.toJson(body).toRequestBody(JSON)).build()
        client.newCall(req).execute().use { r ->
            if (r.code >= 500) null else JsonParser.parseString(r.body?.string().orEmpty()).asJsonObject
        }
    } catch (e: Exception) { null }

    private fun localBlob(): JsonObject {
        val o = JsonObject()
        val lists = JsonObject()
        for (n in names) {
            val l = JsonObject()
            l.addProperty("t", store.listTime(n))
            l.add("items", gson.toJsonTree(store.loadList(n).take(200)))
            lists.add(n, l)
        }
        o.add("lists", lists)
        val pr = JsonObject()
        store.allProgress().entries.sortedByDescending { it.value.third }.take(250).forEach { (k, v) ->
            val a = JsonArray(); a.add(v.first); a.add(v.second); a.add(v.third); pr.add(k, a)
        }
        o.add("progress", pr)
        o.addProperty("pclr", store.progressClearedAt)
        return o
    }

    /** يدمج بيانات الخادم داخل التخزين المحلي. يرجع true لو بقي عندنا شي أحدث يحتاج رفع. */
    private fun merge(remote: JsonObject?): Boolean {
        if (remote == null) return true
        var needPush = false
        val changedLists = HashMap<String, List<Item>>()
        val rl = remote.getAsJsonObject("lists")
        for (n in names) {
            val r = rl?.getAsJsonObject(n)
            val rt = r?.get("t")?.asLong ?: 0L
            val lt = store.listTime(n)
            if (rt > lt && r != null) {
                val items = runCatching {
                    gson.fromJson<List<Item>>(r.getAsJsonArray("items"), object : com.google.gson.reflect.TypeToken<List<Item>>() {}.type)
                }.getOrNull()
                if (items != null) {
                    store.saveList(n, items, rt)
                    changedLists[n] = items
                }
            } else if (lt > rt) needPush = true
        }
        val rclr = remote.get("pclr")?.asLong ?: 0L
        if (rclr > store.progressClearedAt) {
            // الجهاز الآخر مسح السجل: نمسح التقدم الأقدم من وقت المسح
            val clrAt = rclr
            store.allProgress().forEach { (k, v) -> if (v.third < clrAt) store.removeProgress(k) }
            store.progressClearedAt = rclr
        }
        val clr = store.progressClearedAt
        val local = store.allProgress()
        val rp = remote.getAsJsonObject("progress")
        if (rp != null) for ((k, e) in rp.entrySet()) {
            val a = e.asJsonArray
            if (a.size() < 3) continue
            val t = a[2].asLong
            if (t < clr) continue
            val l = local[k]
            if (l == null || t > l.third) store.setProgress(k, a[0].asLong, a[1].asLong, t)
        }
        for ((k, v) in local) {
            val r = rp?.getAsJsonArray(k)
            if (v.third >= clr && (r == null || v.third > r[2].asLong)) { needPush = true; break }
        }
        if (clr > rclr) needPush = true
        if (changedLists.isNotEmpty()) onLists(changedLists)
        return needPush
    }

    /** مزامنة كاملة: سحب، دمج، ثم رفع لو عندنا شي أحدث. آمنة للاستدعاء المتكرر. */
    suspend fun run(): Boolean = withContext(Dispatchers.IO) {
        if (!available) return@withContext false
        mutex.withLock {
            // بيانات قديمة قبل المزامنة ما لها وقت: نعطيها وقت الآن مرة واحدة حتى تُرفع
            for (n in names) if (store.listTime(n) == 0L) {
                val l = store.loadList(n)
                if (l.isNotEmpty()) store.saveList(n, l)
            }
            repeat(3) {
                val g = post("/v1/sync/get", emptyMap()) ?: return@withLock false
                if (g.get("ok")?.asBoolean != true) return@withLock false
                val ver = g.get("ver")?.asInt ?: 0
                val data = g.get("data")?.takeIf { it.isJsonObject }?.asJsonObject
                val needPush = merge(data)
                store.syncVer = ver
                if (!needPush) return@withLock true
                val p = post("/v1/sync/put", mapOf("base" to ver, "data" to localBlob())) ?: return@withLock false
                if (p.get("ok")?.asBoolean == true) { store.syncVer = p.get("ver")?.asInt ?: (ver + 1); return@withLock true }
                if (p.get("err")?.asString != "conflict") return@withLock false
            }
            false
        }
    }
}
