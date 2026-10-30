package com.ayham.player

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.ayham.player.data.Account
import com.ayham.player.data.AccountInfo
import com.ayham.player.data.CatalogDb
import com.ayham.player.data.LiveSource
import com.ayham.player.data.Category
import com.ayham.player.data.Details
import com.ayham.player.data.SeriesInfo
import com.ayham.player.data.Episode
import com.ayham.player.data.EpgEntry
import com.ayham.player.data.SavedSub
import kotlinx.coroutines.sync.withPermit
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import com.ayham.player.data.PlayRequest
import com.ayham.player.data.QueueEntry
import com.ayham.player.data.Store
import com.ayham.player.data.XtreamApi
import com.ayham.player.ui.Strings
import com.ayham.player.ui.normalizeForSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Phase { SPLASH, LOGIN, LOADING, READY }

enum class LoadState { IDLE, LOADING, DONE, FAILED }

sealed interface Screen {
    data class Detail(val item: Item) : Screen
    data class Player(val req: PlayRequest) : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val store = Store(app)
    private val db = CatalogDb(app)

    // ---- مزامنة بيانات المشاهدة بين الأجهزة ----
    val sync = com.ayham.player.data.SyncEngine(store) { lists -> applyRemoteLists(lists) }
    private var syncJob: kotlinx.coroutines.Job? = null
    private fun applyRemoteLists(m: Map<String, List<Item>>) {
        viewModelScope.launch(Dispatchers.Main) {
            m["fav"]?.let { favorites = it }
            m["later"]?.let { later = it }
            m["watched"]?.let { watched = it }
            m["recent"]?.let { recent = it }
            m["history"]?.let { history = it }
        }
    }
    /** نرفع التغييرات بعد 15 ثانية من آخر تعديل (حتى ما نرسل مع كل حفظ تقدم). */
    private fun markDirty() {
        if (!sync.available) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch { delay(15_000); sync.run() }
    }
    // ---- ما الجديد؟ ----
    var newsList by mutableStateOf<List<com.ayham.player.data.NewsEntry>>(emptyList())
        private set
    var newsShow by mutableStateOf<List<com.ayham.player.data.NewsEntry>?>(null)
        private set
    private fun curBuild() = com.ayham.player.data.Updater.currentBuild(getApplication())
    /** auto=true: يعرض أحدث تحديث منشور لم يره المستخدم بعد (مرة واحدة). */
    suspend fun loadNews(auto: Boolean) {
        val r = withContext(Dispatchers.IO) { com.ayham.player.data.NewsApi.fetch() } ?: return
        newsList = r
        if (auto) {
            val seen = store.newsSeen()
            val c = r.firstOrNull { it.id !in seen && (it.build == 0 || it.build <= curBuild()) }
            if (c != null && newsShow == null) newsShow = listOf(c)
        }
    }
    fun openNewsAll() {
        val l = newsList.filter { it.build == 0 || it.build <= curBuild() }
        if (l.isNotEmpty()) newsShow = l
    }
    fun dismissNews() {
        newsShow?.forEach { store.markNewsSeen(it.id) }
        newsShow = null
    }

    /** أفلام/مسلسلات الرعب من تصنيفات قائمتك فعلًا (لموسم الهالوين). */
    suspend fun horror(kind: Kind, n: Int = 24): List<Item> = withContext(Dispatchers.IO) {
        val keys = listOf("horror", "رعب", "scary", "هالوين", "halloween")
        val cats = runCatching { db.categories(kind) }.getOrDefault(emptyList())
            .filter { c -> val nn = com.ayham.player.ui.normalizeForSearch(c.name); keys.any { nn.contains(it) } }
            .take(4).map { it.id }
        runCatching { db.topInCats(kind, cats, n) }.getOrDefault(emptyList())
    }

    suspend fun syncOnce() { sync.run() }
    fun flushSync() { if (sync.available) { syncJob?.cancel(); viewModelScope.launch { sync.run() } } }

    var lang by mutableStateOf(store.lang)
        private set
    var liveFormat by mutableStateOf(store.liveFormat)
        private set
    var maxRes by mutableStateOf(store.maxRes)
        private set
    var liveLayout by mutableStateOf(store.liveLayout)
    var vodLayout by mutableStateOf(store.vodLayout)
        private set

    // ---- المشغّل المصغّر للقنوات ----
    var livePlayer: ExoPlayer? = null
        private set
    var miniItem by mutableStateOf<Item?>(null)
        private set
    var miniSource: LiveSource = LiveSource()

    var phase by mutableStateOf(Phase.SPLASH)
        private set
    var loadStep by mutableIntStateOf(0)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var savedAccount by mutableStateOf(store.loadAccount())
        private set

    var api: XtreamApi? = null
        private set
    var accountInfo by mutableStateOf(AccountInfo())
        private set

    /** يتغير كل ما تغيّرت بيانات القائمة، فتعيد الشاشات قراءة ما تعرضه فقط. */
    var dataVersion by mutableIntStateOf(0)
        private set
    val categories = mutableStateMapOf<Kind, List<Category>>()
    val counts = mutableStateMapOf<Kind, Int>()

    var tab by mutableIntStateOf(0)
    val stack = mutableStateListOf<Screen>()

    var favorites by mutableStateOf(store.loadList("fav"))
        private set
    var later by mutableStateOf(store.loadList("later"))
        private set
    var watched by mutableStateOf(store.loadList("watched"))
        private set
    var recent by mutableStateOf(store.loadList("recent"))
        private set
    var history by mutableStateOf(store.loadList("history"))
        private set

    /** سجل المشاهدة الأخيرة (قنوات وأفلام ومسلسلات) ويُفصل حسب النوع. */
    fun historyOf(kind: Kind): List<Item> = history.filter { it.kind == kind }
    fun recordHistory(item: Item) {
        if (history.firstOrNull()?.key == item.key) return
        setList("history", (listOf(item) + history.filter { it.key != item.key }).take(80))
    }
    /** تسجيل قناة (من تبديل القنوات داخل المشغّل). */
    fun noteLive(key: String) {
        val id = key.removePrefix("LIVE:")
        viewModelScope.launch {
            val it = withContext(Dispatchers.IO) { db.byId(Kind.LIVE, id) }
            if (it != null) recordHistory(it)
        }
    }
    fun isFavKey(key: String) = favorites.any { it.key == key }
    fun toggleFavLive(key: String) {
        val id = key.removePrefix("LIVE:")
        viewModelScope.launch {
            val it = withContext(Dispatchers.IO) { db.byId(Kind.LIVE, id) }
            if (it != null) toggle("fav", it)
        }
    }
    fun clearHistory() {
        setList("history", emptyList())
        setList("recent", emptyList())
        setList("watched", emptyList())
        store.clearProgress()
    }

    // ---- ترجمات محفوظة ----
    fun savedSubs(key: String) = store.savedSubs(key)
    fun prefSub(key: String) = store.prefSub(key)
    fun setPrefSub(key: String, uri: String?) = store.setPrefSub(key, uri)
    fun addSub(key: String, label: String, uri: String) {
        val cur = store.savedSubs(key).filter { it.uri != uri }
        store.saveSubs(key, cur + SavedSub(label, uri))
        store.setPrefSub(key, uri)
    }
    fun clearSubs(key: String) {
        store.savedSubs(key).forEach { s ->
            if (s.uri.startsWith("file:")) runCatching { java.io.File(android.net.Uri.parse(s.uri).path.orEmpty()).delete() }
        }
        store.saveSubs(key, emptyList())
        store.setPrefSub(key, null)
    }
    var subSize by mutableIntStateOf(store.subSize)
    fun changeSubSize(v: Int) { subSize = v; store.subSize = v }

    // ---- دليل البرامج ----
    private val epgCache = HashMap<String, Pair<Long, List<EpgEntry>>>()
    private val epgSem = kotlinx.coroutines.sync.Semaphore(3)
    /** البرامج القادمة للقناة (تبدأ بالحالي). كاش 10 دقائق. */
    suspend fun epg(streamId: String, full: Boolean = false): List<EpgEntry> {
        val k = (if (full) "F" else "S") + streamId
        val nowMs = System.currentTimeMillis()
        synchronized(epgCache) { epgCache[k] }?.let { if (nowMs - it.first < 600_000) return it.second }
        val a = api ?: return emptyList()
        val list = epgSem.withPermit {
            runCatching { a.epg(streamId, full) }.getOrDefault(emptyList())
        }
        val nowS = nowMs / 1000
        val upcoming = list.filter { it.end > nowS }
        synchronized(epgCache) { epgCache[k] = nowMs to upcoming }
        return upcoming
    }

    fun tr(key: String) = Strings.get(lang, key)

    // ---------------- بدء التشغيل ----------------
    fun start() {
        if (phase != Phase.SPLASH) return
        viewModelScope.launch {
            delay(2000)
            val acc = savedAccount
            if (acc == null) phase = Phase.LOGIN else connect(acc.server, acc.user, acc.pass)
        }
    }

    fun connect(serverRaw: String, user: String, pass: String) {
        if (phase == Phase.LOADING) return
        val server = XtreamApi.normalizeServer(serverRaw)
        if (server.isEmpty() || user.isBlank() || pass.isBlank()) {
            error = tr("fill_all")
            phase = Phase.LOGIN
            return
        }
        error = null
        phase = Phase.LOADING
        viewModelScope.launch {
            try {
                val acc = Account(server, user.trim(), pass.trim())
                val a = XtreamApi(acc)
                val key = acc.server + "|" + acc.user
                val sameAccount = withContext(Dispatchers.IO) { db.meta("acc") == key }
                if (!sameAccount) withContext(Dispatchers.IO) { db.clearAll() }
                val hasData = sameAccount && withContext(Dispatchers.IO) { Kind.values().any { db.count(it) > 0 } }

                if (!hasData) {
                    accountInfo = a.login() // يرمي خطأ لو البيانات غلط
                } else {
                    launch { runCatching { accountInfo = a.login() } }
                }
                withContext(Dispatchers.IO) { db.setMeta("acc", key) }
                store.saveAccount(acc)
                savedAccount = acc
                api = a
                sectionState.clear()
                sectionCount.clear()
                for (k in Kind.values()) {
                    refreshKind(k)
                    if ((counts[k] ?: 0) > 0) sectionState[k] = LoadState.DONE
                }
                stack.clear()
                tab = 0
                phase = Phase.READY
            } catch (e: Throwable) {
                error = e.message ?: e.javaClass.simpleName
                phase = Phase.LOGIN
            }
        }
    }

    /** يقرأ من القاعدة عدد العناصر وأقسامها (صغيرة جدًا) ويحدّث الواجهة. */
    private suspend fun refreshKind(kind: Kind) {
        val c = withContext(Dispatchers.IO) { db.count(kind) }
        val cats = withContext(Dispatchers.IO) { db.categories(kind) }
        counts[kind] = c
        categories[kind] = cats
        dataVersion++
    }

    // ---------------- تحميل الأقسام (كل قسم لوحده ومتى تريد) ----------------
    val sectionState = mutableStateMapOf<Kind, LoadState>()
    val sectionCount = mutableStateMapOf<Kind, Int>()

    fun stateOf(k: Kind): LoadState = sectionState[k] ?: LoadState.IDLE
    fun countOf(k: Kind): Int = counts[k] ?: 0

    private fun tagOf(k: Kind) = when (k) { Kind.LIVE -> "live"; Kind.MOVIE -> "movie"; Kind.SERIES -> "series" }

    fun loadSection(kind: Kind) {
        val a = api ?: return
        if (stateOf(kind) == LoadState.LOADING) return
        sectionState[kind] = LoadState.LOADING
        sectionCount[kind] = 0
        viewModelScope.launch {
            val ok = runCatching { a.loadInto(kind, db) { sectionCount[kind] = it } }.isSuccess
            refreshKind(kind)
            if (ok && sectionState[kind] == LoadState.LOADING) {
                sectionState[kind] = LoadState.DONE
                store.catalogTime = System.currentTimeMillis()
            } else if (sectionState[kind] == LoadState.LOADING) {
                sectionState[kind] = LoadState.FAILED
            }
        }
    }

    fun cancelSection(kind: Kind) {
        api?.cancel(tagOf(kind))
        sectionState[kind] = LoadState.IDLE
    }

    fun clearSection(kind: Kind) {
        cancelSection(kind)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.clearKind(kind) }
            refreshKind(kind)
            sectionState[kind] = LoadState.IDLE
        }
    }

    // ---------------- قراءة صفحات من القاعدة ----------------
    suspend fun recentItems(kind: Kind, n: Int = 20): List<Item> = withContext(Dispatchers.IO) { db.recent(kind, n) }
    suspend fun topItems(kind: Kind, n: Int = 20): List<Item> = withContext(Dispatchers.IO) { db.top(kind, n) }
    suspend fun allItems(kind: Kind, limit: Int, offset: Int): List<Item> = withContext(Dispatchers.IO) { db.all(kind, limit, offset) }
    suspend fun inCategory(kind: Kind, cid: String, limit: Int, offset: Int): List<Item> =
        withContext(Dispatchers.IO) { db.inCategory(kind, cid, limit, offset) }

    suspend fun search(query: String): Triple<List<Item>, List<Item>, List<Item>> =
        withContext(Dispatchers.IO) {
            val q = normalizeForSearch(query)
            if (q.length < 2) return@withContext Triple(emptyList(), emptyList(), emptyList())
            Triple(db.search(Kind.LIVE, q, 40), db.search(Kind.MOVIE, q, 40), db.search(Kind.SERIES, q, 40))
        }

    /** قائمة قنوات للاختيار كصوت خارجي. */
    suspend fun liveChannels(): List<Item> = withContext(Dispatchers.IO) { db.all(Kind.LIVE, 60, 0) }

    fun logout() {
        stopMini()
        store.saveAccount(null)
        savedAccount = null
        api = null
        sectionState.clear()
        stack.clear()
        error = null
        phase = Phase.LOGIN
    }

    // ---------------- الإعدادات ----------------
    fun changeLang(l: String) {
        lang = l
        store.lang = l
    }

    fun changeVodLayout(v: String) {
        vodLayout = v
        store.vodLayout = v
    }

    /** تقييم IMDb (نص مثل 8.1) أو null. يُحفظ بالكاش. */
    suspend fun imdbRating(item: Item, year: String): String? = withContext(Dispatchers.IO) {
        store.imdb(item.key)?.let { return@withContext it.takeIf { v -> v.isNotEmpty() } }
        val r = runCatching {
            com.ayham.player.data.Imdb.fetch(XtreamApi.client, item.name, year, item.kind == Kind.SERIES)
        }.getOrNull()
        if (r != null) store.setImdb(item.key, r)
        r
    }

    // ---- التحديث من داخل التطبيق ----
    enum class UpdState { IDLE, CHECKING, AVAILABLE, DOWNLOADING, NEED_PERMISSION, ERROR, LATEST }
    var updState by mutableStateOf(UpdState.IDLE)
        private set
    var updInfo by mutableStateOf<com.ayham.player.data.UpdateInfo?>(null)
        private set
    var updProgress by androidx.compose.runtime.mutableFloatStateOf(0f)
        private set
    var updDismissed by mutableStateOf(false)
    private var updChecked = false

    fun checkUpdate(manual: Boolean) {
        if (!manual && updChecked) return
        updChecked = true
        updState = UpdState.CHECKING
        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) {
                runCatching { com.ayham.player.data.Updater.check(XtreamApi.client) }.getOrNull()
            }
            val cur = com.ayham.player.data.Updater.currentBuild(getApplication())
            if (info != null && info.build > cur) {
                updInfo = info
                updDismissed = false
                updState = UpdState.AVAILABLE
            } else {
                updState = if (info == null && manual) UpdState.ERROR else UpdState.LATEST
            }
        }
    }

    fun startUpdate() {
        val info = updInfo ?: return
        updState = UpdState.DOWNLOADING
        updProgress = 0f
        viewModelScope.launch {
            val app = getApplication<Application>()
            val file = java.io.File(app.cacheDir, "updates/AyhamPlayer.apk")
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    com.ayham.player.data.Updater.download(XtreamApi.client, info.url, file) { updProgress = it }
                }.isSuccess
            }
            updState = if (!ok) UpdState.ERROR
            else if (com.ayham.player.data.Updater.install(app, file)) UpdState.AVAILABLE
            else UpdState.NEED_PERMISSION
        }
    }

    // ---- الملف الشخصي ----
    var profileName by mutableStateOf(store.profileName)
        private set
    var profileAvatar by mutableIntStateOf(store.profileAvatar)
        private set
    /** يزيد عند تغيير الصورة حتى تتحدث الواجهة. */
    var photoVersion by mutableIntStateOf(0)
        private set
    fun photoFile() = java.io.File(getApplication<Application>().filesDir, "avatar.jpg")
    fun changeProfileName(v: String) { profileName = v; store.profileName = v }
    fun changeAvatar(i: Int) { profileAvatar = i; store.profileAvatar = i }
    /** يحفظ الصورة بحجم صغير ويعتمدها. */
    fun setPhoto(bytes: ByteArray): Boolean {
        return runCatching {
            val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            var sample = 1
            while (opts.outWidth / sample > 600 || opts.outHeight / sample > 600) sample *= 2
            val bmp = android.graphics.BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return false
            val side = minOf(bmp.width, bmp.height)
            val sq = android.graphics.Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            val scaled = android.graphics.Bitmap.createScaledBitmap(sq, 320, 320, true)
            photoFile().outputStream().use { scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it) }
            changeAvatar(-1)
            photoVersion++
            true
        }.getOrDefault(false)
    }

    private val simCache = HashMap<String, List<Item>>()
    /** عناصر مشابهة لعنصر شاهده المستخدم (لصفوف "لأنك شاهدت"). */
    /** توصيات مبنية على تصنيفاتك المفضلة (من سجل مشاهدتك) بعد استبعاد ما شاهدته. */
    suspend fun recommendedFor(kind: Kind, n: Int = 24): List<Item> = withContext(Dispatchers.IO) {
        val seen = (history + watched + favorites + later + recent).filter { it.kind == kind }
        if (seen.size < 2) return@withContext emptyList()
        val cats = seen.groupingBy { it.categoryId }.eachCount().filterKeys { it.isNotBlank() }
            .entries.sortedByDescending { it.value }.take(3).map { it.key }
        val seenKeys = (history + watched + favorites + later + recent).map { it.key }.toSet()
        runCatching { db.topInCats(kind, cats, n + seenKeys.size.coerceAtMost(80)) }.getOrDefault(emptyList())
            .filter { it.key !in seenKeys }.take(n)
    }

    /** رائج الآن (TMDB) مطابق مع ما هو متوفر عندك فعلًا. */
    suspend fun trending(kind: Kind, n: Int = 20): List<Item> = withContext(Dispatchers.IO) {
        if (!com.ayham.player.data.Tmdb.enabled) return@withContext emptyList()
        val titles = com.ayham.player.data.Tmdb.trending(store, if (kind == Kind.MOVIE) "movie" else "tv")
        val out = LinkedHashMap<String, Item>()
        for (t in titles) {
            if (out.size >= n) break
            for (cand in listOf(t.title, t.original).filter { it.isNotBlank() }.distinct()) {
                val q = com.ayham.player.ui.normalizeForSearch(cand)
                if (q.length < 4) continue
                val res = runCatching { db.search(kind, q, 12) }.getOrDefault(emptyList())
                val hit = res.firstOrNull { r ->
                    val y = Regex("(19|20)\\d{2}").find(r.name)?.value?.toIntOrNull()
                    y == null || t.year == 0 || kotlin.math.abs(y - t.year) <= 1
                }
                if (hit != null) { out.putIfAbsent(hit.key, hit); break }
            }
        }
        out.values.toList()
    }

    suspend fun similarTo(item: Item): List<Item> = withContext(Dispatchers.IO) {
        synchronized(simCache) { simCache[item.key] }?.let { return@withContext it }
        val r = runCatching { db.similar(item, 24) }.getOrDefault(emptyList())
        synchronized(simCache) { simCache[item.key] = r }
        r
    }

    /** بحث داخل نوع واحد. */
    suspend fun searchKind(kind: Kind, query: String, limit: Int = 80): List<Item> = withContext(Dispatchers.IO) {
        val q = normalizeForSearch(query)
        if (q.length < 2) emptyList() else db.search(kind, q, limit)
    }

    fun changeLiveLayout(v: String) {
        liveLayout = v
        store.liveLayout = v
    }

    fun changeMaxRes(v: Int) {
        maxRes = v
        store.maxRes = v
    }

    fun changeLiveFormat(f: String) {
        liveFormat = f
        store.liveFormat = f
    }

    // ---------------- المكتبة ----------------
    fun isIn(list: String, item: Item): Boolean = listFor(list).any { it.key == item.key }

    private fun listFor(list: String): List<Item> = when (list) {
        "fav" -> favorites
        "later" -> later
        "watched" -> watched
        "history" -> history
        else -> recent
    }

    fun toggle(list: String, item: Item) {
        val cur = listFor(list)
        val next = if (cur.any { it.key == item.key }) cur.filter { it.key != item.key } else listOf(item) + cur
        setList(list, next)
    }

    private fun setList(list: String, next: List<Item>) {
        when (list) {
            "fav" -> favorites = next
            "later" -> later = next
            "watched" -> watched = next
            "history" -> history = next
            else -> recent = next
        }
        store.saveList(list, next)
        markDirty()
    }

    fun progressOf(key: String): Pair<Long, Long>? = store.getProgress(key)

    fun saveProgress(item: Item?, key: String, pos: Long, dur: Long) {
        if (dur <= 0) return
        store.setProgress(key, pos, dur)
        markDirty()
        if (item == null || item.kind == Kind.LIVE) return
        val finished = pos.toDouble() / dur > 0.92
        if (finished) {
            if (item.kind == Kind.MOVIE && watched.none { it.key == item.key }) setList("watched", listOf(item) + watched)
            if (recent.any { it.key == item.key }) setList("recent", recent.filter { it.key != item.key })
            if (later.any { it.key == item.key }) setList("later", later.filter { it.key != item.key })
        } else if (pos > 30_000) {
            setList("recent", (listOf(item) + recent.filter { it.key != item.key }).take(20))
        }
    }

    // ---------------- التنقل ----------------
    fun openDetail(item: Item) {
        if (item.kind == Kind.LIVE) playLive(item, LiveSource(favs = listOf(item))) else stack.add(Screen.Detail(item))
    }

    fun pop(): Boolean {
        if (stack.isNotEmpty()) {
            stack.removeAt(stack.lastIndex)
            return true
        }
        if (miniItem != null) {
            stopMini()
            return true
        }
        if (tab != 0) {
            selectTab(0)
            return true
        }
        return false
    }

    // ---- الصوت الذكي (تعليق من قناة ثانية) ----
    fun extRecent(): List<Item> = store.loadList("ext_recent")
    fun noteExt(item: Item) {
        store.saveList("ext_recent", (listOf(item) + store.loadList("ext_recent").filter { it.key != item.key }).take(8))
    }
    var extAuto: Boolean
        get() = store.extAuto
        set(v) { store.extAuto = v }
    fun resetExtSettings() { store.saveList("ext_recent", emptyList()); store.extAuto = true }
    fun maxConnectionsInt(): Int = accountInfo.maxConnections.trim().toIntOrNull() ?: 0

    fun selectTab(i: Int) {
        if (i != 1) stopMini()
        tab = i
    }

    fun ensureLivePlayer(): ExoPlayer =
        livePlayer ?: PlayerFactory.create(getApplication(), maxRes, com.ayham.player.audio.ExtAudioSession.mainTap).also { livePlayer = it }

    /** تشغيل قناة بالشاشة المصغّرة داخل قائمة القنوات. */
    fun playMini(item: Item, source: LiveSource) {
        val url = urlForLive(item)
        if (url.isEmpty()) return
        recordHistory(item)
        val p = ensureLivePlayer()
        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.playWhenReady = true
        miniItem = item
        miniSource = source
    }

    fun stopMini() {
        livePlayer?.release()
        livePlayer = null
        miniItem = null
    }

    /** عند إغلاق المشغّل الكبير للقنوات: إذا نحن بقسم القنوات تبقى القناة بالشاشة المصغّرة. */
    fun onLiveClosed(key: String) {
        if (tab != 1) {
            stopMini()
            return
        }
        val id = key.removePrefix("LIVE:")
        viewModelScope.launch {
            val it = withContext(Dispatchers.IO) { db.byId(Kind.LIVE, id) }
            if (it == null) stopMini() else miniItem = it
        }
    }

    override fun onCleared() {
        livePlayer?.release()
        livePlayer = null
        db.close()
        super.onCleared()
    }

    fun playLive(item: Item, source: LiveSource) {
        recordHistory(item)
        miniSource = source
        val a = api ?: return
        viewModelScope.launch {
            val (list, idx) = withContext(Dispatchers.IO) {
                val favs = source.favs
                if (favs != null) {
                    val i = favs.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                    val from = (i - 100).coerceAtLeast(0)
                    val to = (i + 200).coerceAtMost(favs.size)
                    favs.subList(from, to) to (i - from)
                } else {
                    val rid = if (item.rid > 0) item.rid else db.byId(Kind.LIVE, item.id)?.rid ?: 0L
                    db.around(Kind.LIVE, source.cat, rid, 100, 200)
                }
            }
            if (list.isEmpty()) return@launch
            val q = list.map { QueueEntry(it.name, a.liveUrl(it.id, liveFormat), it.key, it.image, isLive = true) }
            stack.add(Screen.Player(PlayRequest(q, idx.coerceIn(0, q.lastIndex), null)))
        }
    }

    fun playMovie(item: Item) {
        val a = api ?: return
        recordHistory(item)
        val q = listOf(QueueEntry(item.name, a.movieUrl(item.id, item.ext.ifEmpty { "mp4" }), item.key, item.image))
        stack.add(Screen.Player(PlayRequest(q, 0, item)))
    }

    fun playEpisodes(series: Item, eps: List<Episode>, index: Int) {
        val a = api ?: return
        recordHistory(series)
        val q = eps.map {
            QueueEntry(
                "${series.name}  S${it.season}E${it.num}  ${it.title}",
                a.episodeUrl(it.id, it.ext),
                it.key,
                it.image.ifEmpty { series.image },
            )
        }
        stack.add(Screen.Player(PlayRequest(q, index, series)))
    }

    private val detailsCache = HashMap<String, Details>()
    private val seriesCache = HashMap<String, SeriesInfo>()

    suspend fun movieDetails(item: Item): Details {
        detailsCache[item.key]?.let { return it }
        val d = runCatching { api?.movieDetails(item.id) }.getOrNull() ?: Details()
        detailsCache[item.key] = d
        return d
    }

    suspend fun seriesInfo(item: Item): SeriesInfo {
        seriesCache[item.key]?.let { return it }
        val d = runCatching { api?.seriesInfo(item.id) }.getOrNull() ?: SeriesInfo(Details(), emptyMap())
        if (d.seasons.isNotEmpty()) seriesCache[item.key] = d
        return d
    }

    fun urlForLive(item: Item): String = api?.liveUrl(item.id, liveFormat) ?: ""
}
