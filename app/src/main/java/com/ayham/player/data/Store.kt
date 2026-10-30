package com.ayham.player.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ayham_player", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadAccount(): Account? =
        sp.getString("account", null)?.let { runCatching { gson.fromJson(it, Account::class.java) }.getOrNull() }

    fun saveAccount(a: Account?) {
        sp.edit().apply {
            if (a == null) remove("account") else putString("account", gson.toJson(a))
        }.apply()
    }

    var lang: String
        get() = sp.getString("lang", "ar") ?: "ar"
        set(v) { sp.edit().putString("lang", v).apply() }

    /** آخر مرة تم فيها تحميل القائمة كاملة (للكاش). */
    var catalogTime: Long
        get() = sp.getLong("catalog_time", 0L)
        set(v) { sp.edit().putLong("catalog_time", v).apply() }

    /** 0 = بدون حد (4K لو متوفر)، 1080 = أخف على الشاشة. */
    var maxRes: Int
        get() = sp.getInt("max_res", 0)
        set(v) { sp.edit().putInt("max_res", v).apply() }

    /** تخطيط القنوات: grid (شبكة) أو list (قائمة بالطول). */
    var liveLayout: String
        get() = sp.getString("live_layout", "grid") ?: "grid"
        set(v) { sp.edit().putString("live_layout", v).apply() }

    var liveFormat: String
        get() = sp.getString("live_fmt", "m3u8") ?: "m3u8"
        set(v) { sp.edit().putString("live_fmt", v).apply() }

    // ---- الترجمات المضافة لكل عمل ----
    fun savedSubs(key: String): List<SavedSub> {
        val json = sp.getString("subs_$key", null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<SavedSub>>(json, object : TypeToken<List<SavedSub>>() {}.type)
        }.getOrNull() ?: emptyList()
    }
    fun saveSubs(key: String, list: List<SavedSub>) {
        if (list.isEmpty()) sp.edit().remove("subs_$key").apply()
        else sp.edit().putString("subs_$key", gson.toJson(list)).apply()
    }
    fun prefSub(key: String): String? = sp.getString("subpref_$key", null)
    fun setPrefSub(key: String, uri: String?) {
        if (uri == null) sp.edit().remove("subpref_$key").apply() else sp.edit().putString("subpref_$key", uri).apply()
    }
    var subSize: Int
        get() = sp.getInt("sub_size", 1)
        set(v) { sp.edit().putInt("sub_size", v).apply() }

    // ---- الملف الشخصي ----
    var profileName: String
        get() = sp.getString("profile_name", "") ?: ""
        set(v) { sp.edit().putString("profile_name", v).apply() }
    /** رقم الأفاتار الجاهز، أو -1 لو الصورة من الجهاز. */
    var profileAvatar: Int
        get() = sp.getInt("profile_avatar", 0)
        set(v) { sp.edit().putInt("profile_avatar", v).apply() }

    fun clearProgress() {
        val e = sp.edit()
        sp.all.keys.filter { it.startsWith("p_") && it != "p_cleared" || it.startsWith("pt_") }.forEach { e.remove(it) }
        e.putLong("p_cleared", System.currentTimeMillis())
        e.apply()
    }

    /** تقييم IMDb محفوظ (فاضي = ما لقيناه) حتى ما نكرر الطلبات. */
    fun imdb(key: String): String? = sp.getString("imdb_$key", null)
    fun setImdb(key: String, v: String) { sp.edit().putString("imdb_$key", v).apply() }

    /** تخطيط الأفلام والمسلسلات: grid أو list. */
    var vodLayout: String
        get() = sp.getString("vod_layout", "grid") ?: "grid"
        set(v) { sp.edit().putString("vod_layout", v).apply() }

    var extAuto: Boolean
        get() = sp.getBoolean("ext_auto", true)
        set(v) { sp.edit().putBoolean("ext_auto", v).apply() }

    fun loadList(name: String): List<Item> {
        val json = sp.getString("list_$name", null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<Item>>(json, object : TypeToken<List<Item>>() {}.type)
        }.getOrNull() ?: emptyList()
    }

    fun saveList(name: String, items: List<Item>, time: Long = System.currentTimeMillis()) {
        sp.edit().putString("list_$name", gson.toJson(items)).putLong("lt_$name", time).apply()
    }

    /** وقت آخر تعديل للقائمة (للمزامنة). */
    fun listTime(name: String): Long = sp.getLong("lt_$name", 0L)

    /** التقدم: نخزن "الموضع/المدة" بالمللي ثانية. */
    fun getProgress(key: String): Pair<Long, Long>? {
        val v = sp.getString("p_$key", null) ?: return null
        val parts = v.split("/")
        if (parts.size != 2) return null
        val p = parts[0].toLongOrNull() ?: return null
        val d = parts[1].toLongOrNull() ?: return null
        return p to d
    }

    fun setProgress(key: String, pos: Long, dur: Long, time: Long = System.currentTimeMillis()) {
        sp.edit().putString("p_$key", "$pos/$dur").putLong("pt_$key", time).apply()
    }

    /** كل تقدم المشاهدة مع وقت تسجيله: المفتاح -> (الموضع، المدة، الوقت). */
    fun allProgress(): Map<String, Triple<Long, Long, Long>> {
        val out = HashMap<String, Triple<Long, Long, Long>>()
        for ((k, v) in sp.all) {
            if (!k.startsWith("p_") || v !is String) continue
            val key = k.removePrefix("p_")
            val parts = v.split("/")
            val p = parts.getOrNull(0)?.toLongOrNull() ?: continue
            val d = parts.getOrNull(1)?.toLongOrNull() ?: continue
            out[key] = Triple(p, d, sp.getLong("pt_$key", 0L))
        }
        return out
    }

    fun removeProgress(key: String) { sp.edit().remove("p_$key").remove("pt_$key").apply() }

    /** وقت آخر مسح لسجل التقدم (حتى لا تعيده المزامنة). */
    var progressClearedAt: Long
        get() = sp.getLong("p_cleared", 0L)
        set(v) { sp.edit().putLong("p_cleared", v).apply() }

    /** رقم نسخة البيانات على الخادم التي دمجنا معها آخر مرة. */
    var syncVer: Int
        get() = sp.getInt("sync_ver", 0)
        set(v) { sp.edit().putInt("sync_ver", v).apply() }

    // ---- كاش قسم الرائج (TMDB): قائمة عناوين مع وقت الجلب ----
    fun trendCache(kind: String): Pair<Long, String>? {
        val v = sp.getString("trend_$kind", null) ?: return null
        val i = v.indexOf('|'); if (i < 1) return null
        return (v.substring(0, i).toLongOrNull() ?: return null) to v.substring(i + 1)
    }
    fun setTrendCache(kind: String, json: String) {
        sp.edit().putString("trend_$kind", System.currentTimeMillis().toString() + "|" + json).apply()
    }
}
