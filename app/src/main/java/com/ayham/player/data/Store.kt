package com.ayham.player.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Store(private val ctx: Context) {
    /** التخزين العام (الحساب واللغة والكاش والملفات الشخصية). وهو نفسه تخزين الملف الرئيسي حتى لا تضيع بيانات المستخدمين القدامى. */
    private val gsp = ctx.getSharedPreferences("ayham_player", Context.MODE_PRIVATE)
    private val gson = Gson()
    @Volatile private var cur: android.content.SharedPreferences = gsp
    /** تخزين الملف الشخصي النشط: مفضلته وسجله وتفضيلاته وإعداداته مستقلة تماماً. */
    private val sp: android.content.SharedPreferences get() = cur

    var activeId: String = "main"
        private set

    private fun prefsFor(id: String): android.content.SharedPreferences =
        if (id == "main") gsp else ctx.getSharedPreferences("ayham_profile_$id", Context.MODE_PRIVATE)

    init {
        val list = profiles()
        val want = gsp.getString("active_profile", null) ?: "main"
        activeId = if (list.any { it.id == want }) want else (list.firstOrNull { it.type == "MAIN" }?.id ?: "main")
        cur = prefsFor(activeId)
    }

    // ---- سجل الملفات الشخصية ----
    fun profiles(): List<Profile> {
        val json = gsp.getString("profiles", null)
        val l = json?.let {
            runCatching { gson.fromJson<List<Profile>>(it, object : TypeToken<List<Profile>>() {}.type) }.getOrNull()
        }?.filter { it.id.isNotBlank() }
        if (!l.isNullOrEmpty()) return l
        // أول تشغيل بعد الترقية: الملف الرئيسي يرث اسم وصورة النظام القديم وبياناته كلها
        val legacy = gsp.getString("profile_name", "") ?: ""
        val main = Profile(
            id = "main", name = legacy.ifBlank { "Ayham" }, type = "MAIN",
            avatar = gsp.getInt("profile_avatar", 5), style = 1, bg = 1, frame = 1,
            syncId = legacy.trim().ifBlank { "main" },
        )
        saveProfiles(listOf(main))
        return listOf(main)
    }
    fun saveProfiles(l: List<Profile>) { gsp.edit().putString("profiles", gson.toJson(l)).apply() }

    var defaultProfileId: String
        get() = gsp.getString("default_profile", "") ?: ""
        set(v) { gsp.edit().putString("default_profile", v).apply() }

    /** يبدّل الملف النشط. الضيف لا يُحفظ كآخر ملف مستخدم. */
    fun switchTo(id: String, remember: Boolean = true) {
        activeId = id
        if (remember) gsp.edit().putString("active_profile", id).apply()
        cur = prefsFor(id)
    }

    /** يمسح كل بيانات ملف (حذف الملف أو انتهاء جلسة الضيف). */
    fun wipe(id: String) {
        if (id == "main") return
        prefsFor(id).edit().clear().commit()
    }

    private val settingKeys = listOf(
        "live_layout", "vod_layout", "app_theme", "ui_sounds", "ui_sound_pack", "clock_weather", "clock_region", "clock_city",
        "saver_min", "match_card", "live_fmt", "max_res", "sub_style", "sub_size", "live_shift", "resume_live", "ext_auto",
    )
    /** ينسخ الإعدادات (لا المشاهدة ولا المفضلة) من ملف إلى آخر. */
    fun copySettings(from: String, to: String) {
        val a = prefsFor(from)
        val e = prefsFor(to).edit()
        for (k in settingKeys) {
            when (val v = a.all[k]) {
                is Boolean -> e.putBoolean(k, v)
                is Int -> e.putInt(k, v)
                is Long -> e.putLong(k, v)
                is String -> e.putString(k, v)
                null -> e.remove(k)
                else -> {}
            }
        }
        e.commit()
    }

    /** مساحة مزامنة الملف النشط (فاضي = بدون مزامنة، مثل الضيف). */
    fun syncId(): String = profiles().firstOrNull { it.id == activeId }?.syncId?.ifBlank { "p_$activeId" } ?: ""

    /** آخر قسم فتحه المستخدم في هذا الملف. */
    var lastTab: Int
        get() = sp.getInt("last_tab", 0)
        set(v) { sp.edit().putInt("last_tab", v).apply() }

    fun loadAccount(): Account? =
        gsp.getString("account", null)?.let { runCatching { gson.fromJson(it, Account::class.java) }.getOrNull() }

    fun saveAccount(a: Account?) {
        gsp.edit().apply {
            if (a == null) remove("account") else putString("account", gson.toJson(a))
        }.apply()
    }

    /** الثيم المختار: original أو barcelona. */
    var appTheme: String
        get() = sp.getString("app_theme", "original") ?: "original"
        set(v) { sp.edit().putString("app_theme", v).apply() }

    var lang: String
        get() = gsp.getString("lang", "ar") ?: "ar"
        set(v) { gsp.edit().putString("lang", v).apply() }

    /** آخر مرة تم فيها تحميل القائمة كاملة (للكاش). */
    var catalogTime: Long
        get() = gsp.getLong("catalog_time", 0L)
        set(v) { gsp.edit().putLong("catalog_time", v).apply() }

    /** 0 = بدون حد (4K لو متوفر)، 1080 = أخف على الشاشة. */
    var maxRes: Int
        get() = sp.getInt("max_res", 0)
        set(v) { sp.edit().putInt("max_res", v).apply() }

    /** تخطيط القنوات: grid (شبكة) أو list (قائمة بالطول). */
    var liveLayout: String
        get() = sp.getString("live_layout", "grid") ?: "grid"
        set(v) { sp.edit().putString("live_layout", v).apply() }

    /** بطاقة المباراة القادمة لبرشلونة في الرئيسية (تظهر دائمًا في ثيم برشلونة). */
    var matchCard: Boolean
        get() = sp.getBoolean("match_card", false)
        set(v) { sp.edit().putBoolean("match_card", v).apply() }

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
    var resumeLive: Boolean
        get() = sp.getBoolean("resume_live", false)
        set(v) { sp.edit().putBoolean("resume_live", v).apply() }
    var lastLiveId: String
        get() = sp.getString("last_live_id", "") ?: ""
        set(v) { sp.edit().putString("last_live_id", v).apply() }

    var saverMin: Int
        get() = sp.getInt("saver_min", 2)
        set(v) { sp.edit().putInt("saver_min", v).apply() }

    var liveShift: Boolean
        get() = sp.getBoolean("live_shift", false)
        set(v) { sp.edit().putBoolean("live_shift", v).apply() }

    var subStyle: Int
        get() = sp.getInt("sub_style", 0)
        set(v) { sp.edit().putInt("sub_style", v).apply() }

    fun recentQueries(): List<String> = (sp.getString("recent_q", "") ?: "").split("\n").filter { it.isNotBlank() }
    fun addQuery(q: String) {
        val t = q.trim()
        val l = (listOf(t) + recentQueries().filter { !it.equals(t, true) }).take(8)
        sp.edit().putString("recent_q", l.joinToString("\n")).apply()
    }
    fun clearQueries() { sp.edit().remove("recent_q").apply() }

    var subSize: Int
        get() = sp.getInt("sub_size", 1)
        set(v) { sp.edit().putInt("sub_size", v).apply() }

    fun clearProgress() {
        val e = sp.edit()
        sp.all.keys.filter { it.startsWith("p_") && it != "p_cleared" || it.startsWith("pt_") }.forEach { e.remove(it) }
        e.putLong("p_cleared", System.currentTimeMillis())
        e.apply()
    }

    /** تقييم IMDb محفوظ (فاضي = ما لقيناه) حتى ما نكرر الطلبات. */
    fun ratingsCache(k: String): Pair<Long, String>? {
        val v = gsp.getString("rtg_$k", null) ?: return null
        val i = v.indexOf('|')
        if (i < 0) return null
        return (v.substring(0, i).toLongOrNull() ?: return null) to v.substring(i + 1)
    }
    fun setRatingsCache(k: String, json: String) { gsp.edit().putString("rtg_$k", "${System.currentTimeMillis()}|$json").apply() }

    fun raw(k: String): String? = sp.getString(k, null)
    fun putRaw(k: String, v: String) { sp.edit().putString(k, v).apply() }

    fun imdb(key: String): String? = gsp.getString("imdb_$key", null)
    fun setImdb(key: String, v: String) { gsp.edit().putString("imdb_$key", v).apply() }

    /** تخطيط الأفلام والمسلسلات: grid أو list. */
    var vodLayout: String
        get() = sp.getString("vod_layout", "grid") ?: "grid"
        set(v) { sp.edit().putString("vod_layout", v).apply() }

    var uiSounds: Boolean
        get() = sp.getBoolean("ui_sounds", true)
        set(v) { sp.edit().putBoolean("ui_sounds", v).apply() }

    var uiSoundPack: Int
        get() = sp.getInt("ui_sound_pack", 0)
        set(v) { sp.edit().putInt("ui_sound_pack", v).apply() }

    var clockWeather: Boolean
        get() = sp.getBoolean("clock_weather", false)
        set(v) { sp.edit().putBoolean("clock_weather", v).apply() }
    var clockRegion: Boolean
        get() = sp.getBoolean("clock_region", false)
        set(v) { sp.edit().putBoolean("clock_region", v).apply() }
    var clockCity: Int
        get() = sp.getInt("clock_city", 0)
        set(v) { sp.edit().putInt("clock_city", v).apply() }

    var annDismissed: String
        get() = gsp.getString("ann_dismissed", "") ?: ""
        set(v) { gsp.edit().putString("ann_dismissed", v).apply() }

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

    /** ضبط المقدمة لكل مسلسل: (البداية، النهاية) بالمللي ثانية. النهاية 0 = غير مضبوطة. */
    fun introProfile(key: String): Pair<Long, Long>? {
        val v = sp.getString("intro_$key", null) ?: return null
        val p = v.split("/")
        return (p.getOrNull(0)?.toLongOrNull() ?: return null) to (p.getOrNull(1)?.toLongOrNull() ?: return null)
    }
    /** إعداد الصوتيات المحفوظ لكل قناة: mode(s/e) ⇥ مسار ⇥ تأخير التعليق ms ⇥ معرف القناة الصوتية ⇥ اسمها */
    fun audioPref(key: String): List<String>? = sp.getString("apref_$key", null)?.split("\t")?.takeIf { it.size >= 5 }
    fun setAudioPref(key: String, v: String?) {
        if (v == null) sp.edit().remove("apref_$key").apply() else sp.edit().putString("apref_$key", v).apply()
    }
    fun setIntro(key: String, start: Long, end: Long) {
        if (start <= 0L && end <= 0L) sp.edit().remove("intro_$key").apply()
        else sp.edit().putString("intro_$key", "$start/$end").apply()
    }

    /** تحديثات "ما الجديد؟" التي شاهدها المستخدم (لا تظهر مرتين). */
    fun newsSeen(): Set<String> = (gsp.getString("news_seen", "") ?: "").split(",").filter { it.isNotBlank() }.toSet()
    fun markNewsSeen(id: String) { gsp.edit().putString("news_seen", (newsSeen() + id).toList().takeLast(100).joinToString(",")).apply() }

    var whatsNewSeen: String
        get() = gsp.getString("whatsnew_seen", "") ?: ""
        set(v) { gsp.edit().putString("whatsnew_seen", v).apply() }

    /** يضيف ثواني مشاهدة للأسبوع الحالي لقناة أو فيلم، ويحفظ أعلى 30 فقط. */
    fun addWatch(key: String, name: String, sec: Long) {
        if (sec <= 0L) return
        val wk = System.currentTimeMillis() / 604_800_000L
        val total = sp.getLong("wt_$wk", 0L) + sec
        val rows = (sp.getString("wr_$wk", "") ?: "").lines().filter { it.isNotBlank() }
            .map { it.split("\t") }
            .associate { it[0] to Pair(it.getOrElse(1) { "" }, it.getOrElse(2) { "0" }.toLongOrNull() ?: 0L) }
            .toMutableMap()
        val prev = rows[key]
        rows[key] = Pair(name.replace("\t", " ").replace("\n", " "), (prev?.second ?: 0L) + sec)
        val text = rows.entries.sortedByDescending { it.value.second }.take(30)
            .joinToString("\n") { "${it.key}\t${it.value.first}\t${it.value.second}" }
        sp.edit().putLong("wt_$wk", total).putString("wr_$wk", text).apply()
    }

    fun watchWeek(): WatchWeek {
        val wk = System.currentTimeMillis() / 604_800_000L
        val total = sp.getLong("wt_$wk", 0L)
        val top = (sp.getString("wr_$wk", "") ?: "").lines().filter { it.isNotBlank() }
            .map { it.split("\t") }
            .maxByOrNull { it.getOrElse(2) { "0" }.toLongOrNull() ?: 0L }
        return WatchWeek(total, top?.getOrElse(1) { "" }.orEmpty(), top?.getOrElse(2) { "0" }?.toLongOrNull() ?: 0L)
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
        val v = gsp.getString("trend_$kind", null) ?: return null
        val i = v.indexOf('|'); if (i < 1) return null
        return (v.substring(0, i).toLongOrNull() ?: return null) to v.substring(i + 1)
    }
    fun setTrendCache(kind: String, json: String) {
        gsp.edit().putString("trend_$kind", System.currentTimeMillis().toString() + "|" + json).apply()
    }
}

