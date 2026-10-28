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
        sp.all.keys.filter { it.startsWith("p_") }.forEach { e.remove(it) }
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

    fun saveList(name: String, items: List<Item>) {
        sp.edit().putString("list_$name", gson.toJson(items)).apply()
    }

    /** التقدم: نخزن "الموضع/المدة" بالمللي ثانية. */
    fun getProgress(key: String): Pair<Long, Long>? {
        val v = sp.getString("p_$key", null) ?: return null
        val parts = v.split("/")
        if (parts.size != 2) return null
        val p = parts[0].toLongOrNull() ?: return null
        val d = parts[1].toLongOrNull() ?: return null
        return p to d
    }

    fun setProgress(key: String, pos: Long, dur: Long) {
        sp.edit().putString("p_$key", "$pos/$dur").apply()
    }
}
