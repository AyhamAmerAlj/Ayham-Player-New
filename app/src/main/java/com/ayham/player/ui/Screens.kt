package com.ayham.player.ui

import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.LoadState
import com.ayham.player.R
import com.ayham.player.data.Details
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import com.ayham.player.data.LiveSource
import kotlinx.coroutines.delay

// ============================ Splash ============================
@Composable
fun SplashScreen() {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(500)) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painterResource(R.drawable.splash), null,
            Modifier.fillMaxSize().alpha(a.value),
            contentScale = ContentScale.Crop,
        )
    }
}

// ============================ Login ============================
@Composable
fun LoginScreen(vm: AppViewModel) {
    val saved = vm.savedAccount
    var server by rememberSaveable { mutableStateOf(saved?.server ?: "") }
    var user by rememberSaveable { mutableStateOf(saved?.user ?: "") }
    var pass by rememberSaveable { mutableStateOf(saved?.pass ?: "") }
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF2A1260), C.bg), radius = 1300f),
        ),
    ) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val tv = remember { isTvDevice(ctx) }
        val compact = isCompact()
        val form: @Composable (Modifier) -> Unit = { mod ->
            Column(
                mod.clip(RoundedCornerShape(24.dp)).background(Color(0x33000000)).padding(if (compact) 18.dp else 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RText(t("login_title"), 28, C.text, bold = true, modifier = Modifier.weight(1f))
                    Chip("العربية", vm.lang == "ar") { vm.changeLang("ar") }
                    Chip("English", vm.lang == "en") { vm.changeLang("en") }
                }
                AyhamField(server, { server = it }, t("server"), if (tv) Modifier.autoFocus() else Modifier)
                BodyText(t("server_hint"), 12, C.textDim)
                AyhamField(user, { user = it }, t("username"))
                AyhamField(pass, { pass = it }, t("password"), password = true, imeAction = ImeAction.Done) {
                    vm.connect(server, user, pass)
                }
                val err = vm.error
                if (err != null) BodyText(err, 14, C.danger)
                GlowButton(t("connect"), primary = true, modifier = Modifier.fillMaxWidth()) {
                    vm.connect(server, user, pass)
                }
            }
        }
        val logo: @Composable (Modifier) -> Unit = { mod ->
            Column(
                mod,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            ) {
                Image(
                    painterResource(R.drawable.ic_launcher), null,
                    Modifier.size(if (compact) 90.dp else 140.dp).clip(RoundedCornerShape(if (compact) 20.dp else 28.dp)),
                )
                // رمز QR فقط للتلفاز (تدخل البيانات من هاتفك)
                if (tv) QrLoginCard { s, u, p -> vm.connect(s, u, p) }
            }
        }
        if (compact) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            ) {
                logo(Modifier)
                form(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxSize().padding(40.dp), verticalAlignment = Alignment.CenterVertically) {
                logo(Modifier.weight(1f))
                form(Modifier.weight(1f))
            }
        }
    }
}

// ============================ Loading ============================
@Composable
fun LoadingScreen(vm: AppViewModel) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF2A1260), C.bg), radius = 1300f),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = C.accent, strokeWidth = 4.dp, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(20.dp))
            val key = "connecting"
            RText(t(key), 24, C.text)
        }
    }
}

// ============================ Main ============================
@Composable
fun MainScreen(vm: AppViewModel) {
    val compact = isCompact()
    val content: @Composable (Modifier) -> Unit = { mod ->
        Box(mod) {
            androidx.compose.runtime.key(vm.tab) {
                when (vm.tab) {
                    0 -> HomeTab(vm)
                    1 -> LiveTab(vm)
                    2 -> BrowseTab(vm, Kind.MOVIE)
                    3 -> BrowseTab(vm, Kind.SERIES)
                    4 -> SearchTab(vm)
                    5 -> LibraryTab(vm)
                    7 -> ItemsPage(vm, vm.recent, "continue_empty")
                    8 -> ItemsPage(vm, vm.history.distinctBy { it.key }, "history_empty")
                    9 -> SubsPage(vm)
                    else -> SettingsTab(vm)
                }
            }
        }
    }
    if (compact) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(C.bg2, C.bg))).statusBarsPadding()) {
            content(Modifier.weight(1f).fillMaxWidth())
            BottomNav(vm)
        }
    } else {
        val dir = androidx.compose.ui.platform.LocalLayoutDirection.current
        val sideReq = remember { androidx.compose.ui.focus.FocusRequester() }
        var sideFocused by remember { mutableStateOf(false) }
        // القنوات والأفلام والمسلسلات تُفتح بالشاشة الكاملة بدون الشريط الجانبي، والرجوع يعيد للرئيسية والشريط
        val full = vm.tab in 1..3
        var focusSideNext by remember { mutableStateOf(false) }
        androidx.activity.compose.BackHandler(enabled = vm.miniItem == null && (full || !sideFocused)) {
            if (full) {
                focusSideNext = true
                vm.selectTab(0)
            } else {
                runCatching { sideReq.requestFocus() }
            }
        }
        LaunchedEffect(vm.tab, focusSideNext) {
            if (focusSideNext && !full) {
                delay(120)
                runCatching { sideReq.requestFocus() }
                focusSideNext = false
            }
        }
        // تخفيف تكرار أزرار الاتجاهات عند الضغط المستمر حتى لا تتراكم حركات التمرير ويثقل التطبيق
        val lastKey = remember { longArrayOf(0L) }
        val throttle = Modifier.onPreviewKeyEvent { ev ->
            val code = ev.nativeKeyEvent.keyCode
            val dpad = code == android.view.KeyEvent.KEYCODE_DPAD_UP || code == android.view.KeyEvent.KEYCODE_DPAD_DOWN ||
                code == android.view.KeyEvent.KEYCODE_DPAD_LEFT || code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT
            if (dpad && ev.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && ev.nativeKeyEvent.repeatCount > 0) {
                val now = android.os.SystemClock.uptimeMillis()
                if (now - lastKey[0] < 130) true else { lastKey[0] = now; false }
            } else false
        }
        val bg = Brush.verticalGradient(listOf(Color.Black, Color(0xFF06020D)))
        if (full) {
            Box(Modifier.fillMaxSize().background(bg)) { content(Modifier.fillMaxSize().then(throttle)) }
        } else {
            CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                Row(Modifier.fillMaxSize().background(bg)) {
                    Sidebar(vm, sideReq) { sideFocused = it }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        TopBar(vm)
                        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides dir) {
                            content(Modifier.weight(1f).fillMaxWidth().then(throttle))
                        }
                    }
                }
            }
        }
    }
}

private val navEntries = listOf(
    NavEntry(Icons.Filled.Home, "home"),
    NavEntry(Icons.Filled.LiveTv, "live"),
    NavEntry(Icons.Filled.Movie, "movies"),
    NavEntry(Icons.Filled.Tv, "series"),
    NavEntry(Icons.Filled.Search, "search"),
    NavEntry(Icons.Filled.VideoLibrary, "library"),
    NavEntry(Icons.Filled.Settings, "settings"),
)

/** شريط تنقل سفلي للهاتف. */
@Composable
fun BottomNav(vm: AppViewModel) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF140A26)).navigationBarsPadding().padding(vertical = 4.dp),
    ) {
        navEntries.forEachIndexed { i, e ->
            val sel = vm.tab == i
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (sel) Color(0x448B5CF6) else Color.Transparent)
                    .clickable { vm.selectTab(i) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(e.icon, null, tint = if (sel) C.accent else C.textDim, modifier = Modifier.size(22.dp))
                RText(t(e.key), 10, if (sel) C.text else C.textDim, bold = sel, maxLines = 1)
            }
        }
    }
}

private data class NavEntry(val icon: ImageVector, val key: String)

@Composable
fun NavRail(vm: AppViewModel) {
    val entries = navEntries
    Column(
        Modifier.width(104.dp).fillMaxHeight()
            .background(Brush.verticalGradient(listOf(Color(0x552A1850), Color(0x330B0614))))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        entries.forEachIndexed { i, e ->
            val sel = vm.tab == i
            Focusable(
                Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), 1.05f,
                onClick = { vm.selectTab(i) },
            ) { f ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                f -> C.surfaceHi
                                sel -> Color(0x558B5CF6)
                                else -> Color.Transparent
                            },
                        )
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(e.icon, null, tint = if (sel || f) C.accent else C.textDim, modifier = Modifier.size(26.dp))
                    RText(t(e.key), 14, if (sel || f) C.text else C.textDim, bold = sel, maxLines = 1)
                }
            }
        }
    }
}

// ============================ Sections ============================
private fun loadKey(k: Kind) = when (k) { Kind.LIVE -> "load_live"; Kind.MOVIE -> "load_movies"; Kind.SERIES -> "load_series" }
private fun nameKey(k: Kind) = when (k) { Kind.LIVE -> "live"; Kind.MOVIE -> "movies"; Kind.SERIES -> "series" }

@Composable
fun SectionLoader(vm: AppViewModel, kind: Kind) {
    val st = vm.stateOf(kind)
    Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (st) {
                LoadState.LOADING -> {
                    CircularProgressIndicator(color = C.accent, strokeWidth = 4.dp, modifier = Modifier.size(50.dp))
                    RText(t(loadKey(kind)) + "...", 26, C.text, bold = true)
                    val n = vm.sectionCount[kind] ?: 0
                    if (n > 0) RText("${t("loaded_count")}  $n", 22, C.primaryLight)
                    GlowButton(t("cancel"), modifier = Modifier.autoFocus()) { vm.cancelSection(kind) }
                }
                LoadState.FAILED -> {
                    RText(t("load_failed"), 24, C.danger)
                    GlowButton(t("retry"), primary = true, modifier = Modifier.autoFocus()) { vm.loadSection(kind) }
                }
                else -> {
                    RText(t("section_not_loaded"), 24, C.textDim)
                    GlowButton(t(loadKey(kind)), primary = true, modifier = Modifier.autoFocus()) { vm.loadSection(kind) }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SetupCard(vm: AppViewModel) {
    val kinds = listOf(Kind.LIVE, Kind.MOVIE, Kind.SERIES)
    if (kinds.all { vm.stateOf(it) == LoadState.DONE }) return
    Column(
        Modifier.fillMaxWidth().padding(if (isCompact()) 14.dp else 28.dp).clip(RoundedCornerShape(20.dp))
            .background(Color(0x33000000)).padding(if (isCompact()) 16.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RText(t("setup_title"), if (isCompact()) 20 else 28, C.text, bold = true)
        BodyText(t("setup_hint"), 14, C.textDim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            kinds.forEachIndexed { i, k ->
                val st = vm.stateOf(k)
                if (st != LoadState.DONE) {
                    val label = when (st) {
                        LoadState.LOADING -> t(nameKey(k)) + "  " + (vm.sectionCount[k] ?: 0)
                        LoadState.FAILED -> t("retry") + "  " + t(nameKey(k))
                        else -> t(loadKey(k))
                    }
                    GlowButton(label, primary = st == LoadState.IDLE, modifier = if (i == 0) Modifier.autoFocus() else Modifier) {
                        if (st == LoadState.LOADING) vm.cancelSection(k) else vm.loadSection(k)
                    }
                }
            }
        }
    }
}

// ============================ Home ============================
@Composable
fun HeroBanner(vm: AppViewModel, item: Item) {
    var details by remember(item.key) { mutableStateOf<Details?>(null) }
    LaunchedEffect(item.key) {
        details = if (item.kind == Kind.MOVIE) vm.movieDetails(item) else null
    }
    val bg = details?.backdrop.orEmpty().ifEmpty { item.image }
    var imdb by remember(item.key) { mutableStateOf<String?>(null) }
    LaunchedEffect(item.key, details != null) {
        if (details != null) imdb = vm.imdbRating(item, details?.year.orEmpty())
    }
    val compact = isCompact()
    Box(Modifier.fillMaxWidth().height(if (compact) 230.dp else 310.dp)) {
        Img(bg, Modifier.fillMaxSize(), w = 600, h = 340)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color(0xF00B0614), Color(0x990B0614), Color(0x220B0614))),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(colorStops = arrayOf(0.6f to Color.Transparent, 1f to C.bg2)),
            ),
        )
        Column(
            if (compact) Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth()
            else Modifier.align(Alignment.CenterStart).padding(horizontal = 36.dp).width(560.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (compact) RText(item.name, 24, C.text, bold = true, maxLines = 2, lineHeight = 30)
            else RText(item.name, 40, C.text, bold = true, maxLines = 2, lineHeight = 48)
            val meta = listOfNotNull(
                details?.year?.takeIf { it.isNotBlank() },
                details?.genre?.takeIf { it.isNotBlank() },
                item.rating.takeIf { it.isNotBlank() && it != "0" }?.let { "★ $it" },
            ).joinToString("  •  ")
            imdb?.let { ImdbBadge(it) }
            if (meta.isNotEmpty()) BodyText(meta, 14, C.primaryLight, bold = true, maxLines = 1)
            val plot = details?.plot.orEmpty()
            if (plot.isNotBlank() && !compact) BodyText(plot, 14, C.textDim, maxLines = 3)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlowButton(t("play"), primary = true) {
                    if (item.kind == Kind.MOVIE) vm.playMovie(item) else vm.openDetail(item)
                }
                GlowButton(t("about")) { vm.openDetail(item) }
            }
        }
    }
}

// ============================ Live ============================
@Composable
fun LiveTab(vm: AppViewModel) {
    if (vm.stateOf(Kind.LIVE) != LoadState.DONE) {
        SectionLoader(vm, Kind.LIVE)
        return
    }
    var sel by rememberSaveable { mutableStateOf("all") }
    val cats = vm.categories[Kind.LIVE].orEmpty()
    val favLive = remember(vm.favorites) { vm.favorites.filter { it.kind == Kind.LIVE } }
    val recentLive = remember(vm.history) { vm.historyOf(Kind.LIVE) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val favAdded = t("fav_added")
    val favRemoved = t("fav_removed")
    val toggleFav: (Item) -> Unit = { ch ->
        val was = vm.isIn("fav", ch)
        vm.toggle("fav", ch)
        android.widget.Toast.makeText(ctx, if (was) favRemoved else favAdded, android.widget.Toast.LENGTH_SHORT).show()
    }
    var q by rememberSaveable { mutableStateOf("") }
    var found by remember { mutableStateOf<List<Item>>(emptyList()) }
    val searching = q.trim().length >= 2
    LaunchedEffect(q, vm.dataVersion) {
        delay(350)
        found = vm.searchKind(Kind.LIVE, q, 150)
    }
    val source = if (searching) LiveSource(favs = found)
        else if (sel == "fav") LiveSource(favs = favLive)
        else if (sel == "recent") LiveSource(favs = recentLive)
        else LiveSource(cat = if (sel == "all") null else sel)
    val pager = rememberPager(listOf(sel, vm.dataVersion, favLive.size, recentLive.size), 80) { offset, limit ->
        when (sel) {
            "fav" -> favLive.drop(offset).take(limit)
            "recent" -> recentLive.drop(offset).take(limit)
            "all" -> vm.allItems(Kind.LIVE, limit, offset)
            else -> vm.inCategory(Kind.LIVE, sel, limit, offset)
        }
    }
    val shownList = if (searching) found else pager.items
    val mini = vm.miniItem
    val pick: (Item) -> Unit = { ch ->
        // لو فيه قناة بالشاشة المصغّرة: اختيار قناة أخرى يبدّلها، واختيار نفس القناة يكبّرها
        if (mini != null && mini.id != ch.id) vm.playMini(ch, source) else vm.playLive(ch, source)
    }
    val compact = isCompact()
    val body: @Composable (Modifier) -> Unit = { mod -> Row(mod) {
        if (!compact) LazyColumn(
            Modifier.width(240.dp).fillMaxHeight(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { AyhamField(q, { q = it }, t("search_in_live"), imeAction = ImeAction.Search) }
            item { PanelItem(t("all"), sel == "all" && !searching, "${vm.countOf(Kind.LIVE)} ${t("channels")}") { sel = "all"; q = "" } }
            item { PanelItem(t("favorite_channels"), sel == "fav" && !searching, "${favLive.size}") { sel = "fav"; q = "" } }
            item { PanelItem(t("recent_live"), sel == "recent" && !searching, "${recentLive.size}") { sel = "recent"; q = "" } }
            items(cats, key = { it.id }) { c -> PanelItem(c.name, sel == c.id && !searching, "${c.count}") { sel = c.id; q = "" } }
        }
        if (vm.liveLayout == "list" || compact) {
            LazyColumn(
                Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(shownList, key = { _, it -> it.key }) { index, ch ->
                    if (!searching && index >= pager.items.size - 20) LaunchedEffect(pager.items.size) { pager.loadMore() }
                    ChannelRow(ch, mini?.id == ch.id, fav = vm.isIn("fav", ch), epg = rememberEpgNow(vm, ch.id), onLongClick = { toggleFav(ch) }) { pick(ch) }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (mini != null && !compact) 200.dp else 240.dp),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                gridItemsIndexed(shownList, key = { _, it -> it.key }) { index, ch ->
                    if (!searching && index >= pager.items.size - 20) LaunchedEffect(pager.items.size) { pager.loadMore() }
                    ChannelCard(ch, w = if (mini != null) 200.dp else 230.dp, fav = vm.isIn("fav", ch), epg = rememberEpgNow(vm, ch.id), onLongClick = { toggleFav(ch) }) { pick(ch) }
                }
            }
        }
        if (mini != null && !compact) MiniPlayer(vm, mini)
    } }
    if (compact) {
        Column(Modifier.fillMaxSize()) {
            AyhamField(q, { q = it }, t("search_in_live"), Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp), imeAction = ImeAction.Search)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Chip(t("all"), sel == "all") { sel = "all"; q = "" } }
                item { Chip(t("favorite_channels"), sel == "fav") { sel = "fav"; q = "" } }
                item { Chip(t("recent_live"), sel == "recent") { sel = "recent"; q = "" } }
                items(cats, key = { it.id }) { c -> Chip(c.name, sel == c.id) { sel = c.id; q = "" } }
            }
            if (mini != null) MiniPlayer(vm, mini)
            body(Modifier.weight(1f).fillMaxWidth())
        }
    } else {
        body(Modifier.fillMaxSize())
    }
}

// ============================ Movies / Series ============================
@Composable
fun BrowseTab(vm: AppViewModel, kind: Kind) {
    val isMovie = kind == Kind.MOVIE
    if (vm.stateOf(kind) != LoadState.DONE) {
        SectionLoader(vm, kind)
        return
    }
    val cats = vm.categories[kind].orEmpty()
    var sel by rememberSaveable(kind.name) { mutableStateOf<String?>(null) }
    var q by rememberSaveable(kind.name + "_q") { mutableStateOf("") }
    var found by remember(kind) { mutableStateOf<List<Item>>(emptyList()) }
    val searching = q.trim().length >= 2
    LaunchedEffect(q, vm.dataVersion) {
        delay(350)
        found = vm.searchKind(kind, q)
    }
    var recentItems by remember(kind, vm.dataVersion) { mutableStateOf<List<Item>>(emptyList()) }
    var topItems by remember(kind, vm.dataVersion) { mutableStateOf<List<Item>>(emptyList()) }
    LaunchedEffect(kind, vm.dataVersion) {
        recentItems = vm.recentItems(kind)
        topItems = if (isMovie) vm.topItems(kind) else emptyList()
    }
    val featured = remember(recentItems) { recentItems.take(8).randomOrNull() }
    val compact = isCompact()
    val hpad = if (compact) 14.dp else 28.dp
    val asList = vm.vodLayout == "list"
    val bases = remember(vm.history, kind) { vm.historyOf(kind).take(2) }
    var similar by remember(kind) { mutableStateOf<List<Pair<Item, List<Item>>>>(emptyList()) }
    LaunchedEffect(bases, vm.dataVersion) { similar = bases.map { it to vm.similarTo(it) } }
    val watchedHere = remember(vm.history, kind) { vm.historyOf(kind) }

    Column(Modifier.fillMaxSize()) {
        AyhamField(
            q, { q = it }, t("search_in_" + kind.name.lowercase()),
            Modifier.padding(start = hpad, end = hpad, top = 10.dp),
            imeAction = ImeAction.Search,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = hpad, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Chip(t("all"), sel == null) { sel = null; q = "" } }
            items(cats, key = { it.id }) { c -> Chip(c.name, sel == c.id) { sel = c.id; q = "" } }
        }
        val current = sel
        if (searching) {
            if (found.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    RText(t("no_results"), 22, C.textDim)
                }
            } else if (asList) {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = hpad, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(found, key = { it.key }) { item -> VodRow(item) { vm.openDetail(item) } }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (compact) 112.dp else 138.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = hpad, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(found.size, key = { found[it].key }) { i ->
                        PosterCard(found[i], w = if (compact) 112.dp else 128.dp, h = if (compact) 168.dp else 192.dp) { vm.openDetail(found[i]) }
                    }
                }
            }
        } else if (current == null && !asList) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 40.dp)) {
                similar.forEach { (base, list) ->
                    if (list.size >= 3) item(key = "sim_" + base.key) { ContentRow(t("because_watched") + " " + base.name, list) { vm.openDetail(it) } }
                }
                if (watchedHere.isNotEmpty()) item { ContentRow(t(if (isMovie) "recent_movies_w" else "recent_series_w"), watchedHere) { vm.openDetail(it) } }
                item { ContentRow(t(if (isMovie) "recent_movies" else "recent_series"), recentItems) { vm.openDetail(it) } }
                if (isMovie) item { ContentRow(t("top_movies"), topItems) { vm.openDetail(it) } }
                items(cats, key = { it.id }, contentType = { "catrow" }) { c ->
                    LazyCategoryRow(c.name, vm.dataVersion, { vm.inCategory(kind, c.id, 16, 0) }) { vm.openDetail(it) }
                }
            }
        } else {
            val pager = rememberPager(Triple(kind, current, vm.dataVersion), 60) { offset, limit ->
                if (current == null) vm.allItems(kind, limit, offset) else vm.inCategory(kind, current, limit, offset)
            }
            if (asList) {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = hpad, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(pager.items, key = { _, it -> it.key }) { index, item ->
                        if (index >= pager.items.size - 20) LaunchedEffect(pager.items.size) { pager.loadMore() }
                        VodRow(item) { vm.openDetail(item) }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (compact) 112.dp else 138.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = hpad, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    gridItemsIndexed(pager.items, key = { _, it -> it.key }) { index, item ->
                        if (index >= pager.items.size - 20) LaunchedEffect(pager.items.size) { pager.loadMore() }
                        PosterCard(item, w = if (compact) 112.dp else 128.dp, h = if (compact) 168.dp else 192.dp) { vm.openDetail(item) }
                    }
                }
            }
        }
    }
}

// ============================ Search ============================
@Composable
fun SearchTab(vm: AppViewModel) {
    var q by rememberSaveable { mutableStateOf("") }
    var res by remember { mutableStateOf(Triple(emptyList<Item>(), emptyList<Item>(), emptyList<Item>())) }
    LaunchedEffect(q) {
        delay(450)
        res = vm.search(q)
    }
    Column(Modifier.fillMaxSize()) {
        AyhamField(
            q, { q = it }, t("search_hint"),
            Modifier.padding(horizontal = if (isCompact()) 14.dp else 28.dp, vertical = if (isCompact()) 10.dp else 18.dp),
            imeAction = ImeAction.Search,
        )
        val empty = res.first.isEmpty() && res.second.isEmpty() && res.third.isEmpty()
        if (empty && q.trim().length >= 2) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                RText(t("no_results"), 24, C.textDim)
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
            item { ContentRow(t("live"), res.first) { vm.playLive(it, LiveSource(favs = res.first)) } }
            item { ContentRow(t("movies"), res.second) { vm.openDetail(it) } }
            item { ContentRow(t("series"), res.third) { vm.openDetail(it) } }
        }
    }
}

// ============================ Library ============================
@Composable
fun LibraryTab(vm: AppViewModel) {
    val none = vm.recent.isEmpty() && vm.favorites.isEmpty() && vm.later.isEmpty() && vm.watched.isEmpty() && vm.history.isEmpty()
    if (none) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            RText(t("empty_list"), 26, C.textDim)
        }
        return
    }
    val favLive = vm.favorites.filter { it.kind == Kind.LIVE }
    val favVod = vm.favorites.filter { it.kind != Kind.LIVE }
    val recentLive = vm.historyOf(Kind.LIVE)
    val playLive: (Item, List<Item>) -> Unit = { it, src -> vm.playLive(it, LiveSource(favs = src)) }
    LazyColumn(contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp)) {
        item { ContentRow(t("continue_watching"), vm.recent) { vm.openDetail(it) } }
        item { ContentRow(t("favorite_channels"), favLive) { playLive(it, favLive) } }
        item { ContentRow(t("recent_live"), recentLive) { playLive(it, recentLive) } }
        item { ContentRow(t("recent_movies_w"), vm.historyOf(Kind.MOVIE)) { vm.openDetail(it) } }
        item { ContentRow(t("recent_series_w"), vm.historyOf(Kind.SERIES)) { vm.openDetail(it) } }
        item { ContentRow(t("favorites"), favVod) { vm.openDetail(it) } }
        item { ContentRow(t("watch_later"), vm.later) { vm.openDetail(it) } }
        item { ContentRow(t("watched"), vm.watched) { vm.openDetail(it) } }
    }
}

// ============================ Settings ============================
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SettingsTab(vm: AppViewModel) {
    val t_hist = t("history_cleared")
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (isCompact()) 16.dp else 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        RText(t("settings"), if (isCompact()) 26 else 36, C.text, bold = true)

        // ---- الملف الشخصي ----
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val tvDevice = remember { isTvDevice(ctx) }
        var showQr by remember { mutableStateOf(false) }
        val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                if (bytes != null) vm.setPhoto(bytes)
            }
        }
        RText(t("profile"), 22, C.primaryLight, bold = true)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(vm, 72.dp)
            AyhamField(vm.profileName, { vm.changeProfileName(it) }, t("profile_name"), Modifier.weight(1f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            avatarPresets.indices.forEach { i ->
                Focusable(shape = CircleShape, focusScale = 1.12f, onClick = { vm.changeAvatar(i) }) { _ ->
                    Box(
                        Modifier.then(
                            if (vm.profileAvatar == i) Modifier.border(3.dp, Color.White, CircleShape) else Modifier,
                        ),
                    ) { PresetAvatar(i, 52.dp) }
                }
            }
            GlowButton(t("photo_from_phone")) {
                if (tvDevice) showQr = !showQr else photoPicker.launch(arrayOf("image/*"))
            }
        }
        if (showQr) PhotoQrCard(vm) { showQr = false }

        RText(t("language"), 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("العربية", vm.lang == "ar") { vm.changeLang("ar") }
            Chip("English", vm.lang == "en") { vm.changeLang("en") }
        }

        RText(t("live_layout"), 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(t("layout_grid"), vm.liveLayout == "grid") { vm.changeLiveLayout("grid") }
            Chip(t("layout_list"), vm.liveLayout == "list") { vm.changeLiveLayout("list") }
        }

        RText(t("vod_layout"), 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(t("layout_posters"), vm.vodLayout == "grid") { vm.changeVodLayout("grid") }
            Chip(t("layout_list"), vm.vodLayout == "list") { vm.changeVodLayout("list") }
        }

        RText(t("live_format"), 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("m3u8 (HLS)", vm.liveFormat == "m3u8") { vm.changeLiveFormat("m3u8") }
            Chip("ts", vm.liveFormat == "ts") { vm.changeLiveFormat("ts") }
        }

        RText(t("max_quality"), 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(t("q_4k"), vm.maxRes == 0) { vm.changeMaxRes(0) }
            Chip(t("q_1080"), vm.maxRes == 1080) { vm.changeMaxRes(1080) }
        }

        RText(t("sections"), 22, C.primaryLight, bold = true)
        listOf(Kind.LIVE, Kind.MOVIE, Kind.SERIES).forEach { k ->
            val st = vm.stateOf(k)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val status = when (st) {
                    LoadState.DONE -> "${t("loaded")} (${vm.countOf(k)})"
                    LoadState.LOADING -> "${t("loaded_count")} ${vm.sectionCount[k] ?: 0}"
                    else -> t("not_loaded")
                }
                BodyText("${t(nameKey(k))}: $status", 16, C.text, modifier = Modifier.width(260.dp))
                if (st == LoadState.LOADING) {
                    GlowButton(t("cancel")) { vm.cancelSection(k) }
                } else {
                    GlowButton(if (st == LoadState.DONE) t("update") else t(loadKey(k)), primary = st != LoadState.DONE) { vm.loadSection(k) }
                    if (st == LoadState.DONE) GlowButton(t("delete")) { vm.clearSection(k) }
                }
            }
        }

        RText(t("account"), 22, C.primaryLight, bold = true)
        val info = vm.accountInfo
        BodyText("${t("server")}: ${vm.savedAccount?.server.orEmpty()}", 15, C.textDim)
        BodyText("${t("username")}: ${vm.savedAccount?.user.orEmpty()}", 15, C.textDim)
        if (info.status.isNotEmpty()) BodyText("${t("status")}: ${info.status}", 15, C.textDim)
        if (info.expDate.isNotEmpty()) BodyText("${t("expires")}: ${info.expDate}", 15, C.textDim)
        if (info.maxConnections.isNotEmpty()) BodyText("${t("connections")}: ${info.maxConnections}", 15, C.textDim)

        if (com.ayham.player.data.LicenseManager.enabled) {
            RText(t("sync_title"), 22, C.primaryLight, bold = true)
            val syncScope = androidx.compose.runtime.rememberCoroutineScope()
            val okMsg = t("sync_done"); val failMsg = t("sync_fail"); val offMsg = t("sync_off")
            GlowButton(t("sync_now")) {
                syncScope.launch {
                    val ok = if (vm.sync.available) vm.sync.run() else null
                    android.widget.Toast.makeText(ctx, when (ok) { true -> okMsg; false -> failMsg; null -> offMsg }, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (com.ayham.player.data.LicenseManager.enabled) {
            val newsScope = androidx.compose.runtime.rememberCoroutineScope()
            GlowButton(t("whats_new")) {
                newsScope.launch { if (vm.newsList.isEmpty()) vm.loadNews(false); vm.openNewsAll() }
            }
        }
        RText(t("check_update"), 22, C.primaryLight, bold = true)
        val upd = vm.updState
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vm.updInfo != null && upd != AppViewModel.UpdState.CHECKING) {
                GlowButton(t("update_now"), primary = true) { vm.updDismissed = false; vm.startUpdate() }
            } else {
                GlowButton(t("check_update")) { vm.checkUpdate(true) }
            }
            GlowButton(t("clear_history")) {
                vm.clearHistory()
                android.widget.Toast.makeText(ctx, t_hist, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        when (upd) {
            AppViewModel.UpdState.CHECKING -> BodyText(t("checking"), 14, C.textDim)
            AppViewModel.UpdState.LATEST -> BodyText(t("up_to_date"), 14, C.ok)
            AppViewModel.UpdState.ERROR -> BodyText(t("update_failed"), 14, C.danger)
            AppViewModel.UpdState.DOWNLOADING -> BodyText(t("downloading") + " " + (vm.updProgress * 100).toInt() + "%", 14, C.text)
            AppViewModel.UpdState.NEED_PERMISSION -> BodyText(t("update_perm"), 14, C.accent)
            else -> {}
        }

        GlowButton(t("logout")) { vm.logout() }

        Spacer(Modifier.height(10.dp))
        if (com.ayham.player.data.Tmdb.enabled) BodyText(t("tmdb_credit"), 13, C.textDim)
        RText("${t("app")}  ${t("version")} ${com.ayham.player.data.Updater.currentBuild(ctx)}", 18, C.textDim)
    }
}
