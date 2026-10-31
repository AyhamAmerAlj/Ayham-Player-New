package com.ayham.player.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalLayoutDirection
import com.ayham.player.AppViewModel
import com.ayham.player.R
import com.ayham.player.data.Details
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import com.ayham.player.data.LiveSource
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val purpleGlow = Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)))
private val panelBorder = Color(0x558B5CF6)

// ============================ الشريط العلوي + الشريط الجانبي (التلفاز) ============================
private data class SideItem(val tab: Int, val key: String, val icon: ImageVector)

private val sideItems = listOf(
    SideItem(0, "home", Icons.Filled.Home),
    SideItem(2, "movies", Icons.Filled.Movie),
    SideItem(3, "series", Icons.Filled.Tv),
    SideItem(1, "live", Icons.Filled.LiveTv),
    SideItem(5, "nav_fav", Icons.Filled.Favorite),
    SideItem(7, "side_continue", Icons.Filled.PlayCircle),
    SideItem(8, "side_history", Icons.Filled.History),
    SideItem(9, "side_subs", Icons.Filled.Subtitles),
    SideItem(4, "search", Icons.Filled.Search),
    SideItem(6, "settings", Icons.Filled.Settings),
)

@Composable
fun Sidebar(vm: AppViewModel, selectedFocus: androidx.compose.ui.focus.FocusRequester, onSideFocus: (Boolean) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val ver = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty() }
    Column(
        Modifier.width(188.dp).fillMaxHeight()
            .onFocusChanged { onSideFocus(it.hasFocus) }
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0616), Color(0xFF05020B))))
            .border(1.dp, Color(0x228B5CF6))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(Modifier.padding(start = 6.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_launcher), null, Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(8.dp))
            Column {
                RText("Ayham", 16, Color.White, bold = true, maxLines = 1)
                RText("Player", 16, C.primaryLight, bold = true, maxLines = 1)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            sideItems.forEach { s ->
                val sel = vm.tab == s.tab
                val shape = RoundedCornerShape(10.dp)
                Focusable(Modifier.fillMaxWidth().then(if (sel) Modifier.focusRequester(selectedFocus) else Modifier), shape, 1.03f, onClick = { vm.selectTab(s.tab) }) { f ->
                    Row(
                        Modifier.fillMaxWidth().height(40.dp).clip(shape)
                            .background(
                                when {
                                    f -> Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)))
                                    sel -> Brush.horizontalGradient(listOf(Color(0xAA7C3AED), Color(0x553B1A7A)))
                                    else -> Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                },
                            )
                            .then(if (sel) Modifier.border(1.dp, C.primary, shape) else Modifier)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(s.icon, null, tint = if (sel || f) Color.White else C.primaryLight, modifier = Modifier.size(20.dp))
                        RText(t(s.key), 14, if (sel || f) Color.White else C.text, bold = sel, maxLines = 1)
                    }
                }
            }
        }
        RText(t("tagline"), 11, C.textDim, maxLines = 2, modifier = Modifier.padding(start = 6.dp))
        if (ver.isNotEmpty()) RText("v$ver", 11, C.textDim, maxLines = 1, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
    }
}

@Composable
fun TopBar(vm: AppViewModel) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(20_000)
        }
    }
    val time = remember(now / 60_000) { SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(now)) }
    Row(
        Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // حقل البحث العلوي: يفتح صفحة البحث (فوق الشاشة، لا يؤثر على حركة الريموت)
        val shape = RoundedCornerShape(24.dp)
        Focusable(Modifier.weight(1f).widthIn(max = 640.dp), shape, 1.02f, onClick = { vm.selectTab(4) }) { f ->
            Row(
                Modifier.fillMaxWidth().height(42.dp).clip(shape)
                    .background(Color(0xFF120A24))
                    .border(1.5.dp, if (f) C.primary else panelBorder, shape)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Search, null, tint = C.primaryLight, modifier = Modifier.size(20.dp))
                RText(t("search_top"), 14, C.textDim, maxLines = 1)
            }
        }
        Spacer(Modifier.weight(0.2f))
        Focusable(shape = RoundedCornerShape(24.dp), focusScale = 1.05f, onClick = { vm.selectTab(6) }) { f ->
            Row(
                Modifier.clip(RoundedCornerShape(24.dp)).background(if (f) Color(0x558B5CF6) else Color.Transparent).padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileAvatar(vm, 34.dp)
                RText(vm.profileName.ifBlank { "Ayham" }, 14, Color.White, bold = true, maxLines = 1)
            }
        }
        Spacer(Modifier.width(14.dp))
        RText(time, 16, Color.White, bold = true, maxLines = 1)
    }
}

@Composable
private fun TopIcon(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Focusable(shape = CircleShape, focusScale = 1.1f, onClick = onClick) { f ->
        Box(
            Modifier.size(38.dp).clip(CircleShape)
                .background(if (f) C.primary else if (selected) Color(0x668B5CF6) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

// ============================ البانر المميز ============================
@Composable
fun FeaturedBanner(vm: AppViewModel, items: List<Item>) {
    if (items.isEmpty()) return
    val compact = isCompact()
    val screenH = LocalConfiguration.current.screenHeightDp
    val h = if (compact) 210.dp else (screenH * 0.42f).dp
    val pad = if (compact) 14.dp else 36.dp
    var idx by remember { mutableIntStateOf(0) }
    var held by remember { mutableStateOf(false) }
    LaunchedEffect(idx, held, items.size) {
        if (!held && items.size > 1) {
            delay(8000)
            idx = (idx + 1) % items.size
        }
    }
    val item = items[idx.coerceIn(0, items.lastIndex)]
    var details by remember(item.key) { mutableStateOf<Details?>(null) }
    var imdb by remember(item.key) { mutableStateOf<String?>(null) }
    var loaded by remember(item.key) { mutableStateOf(false) }
    LaunchedEffect(item.key) {
        val d = runCatching { if (item.kind == Kind.SERIES) vm.seriesInfo(item).details else vm.movieDetails(item) }.getOrNull()
        details = d
        if (d != null) imdb = vm.imdbRating(item, d.year)
        loaded = true
    }
    val shape = RoundedCornerShape(22.dp)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = pad).height(h).clip(shape)
                .background(Color(0xFF0E0820)).border(1.5.dp, panelBorder, shape),
        ) {
            val bg = details?.backdrop.orEmpty().ifEmpty { item.image }
            Img(bg, Modifier.fillMaxSize(), w = 960, h = 540)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(listOf(Color(0xF8080414), Color(0xDD080414), Color(0x88080414), Color(0x11080414))),
                ),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(colorStops = arrayOf(0.6f to Color.Transparent, 1f to Color(0xEE080414))),
                ),
            )
            Column(
                Modifier.align(Alignment.CenterStart).padding(start = if (compact) 16.dp else 32.dp).fillMaxWidth(if (compact) 0.9f else 0.52f),
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
            ) {
                // شارات النوع والجودة
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF6D28D9)).padding(horizontal = 12.dp, vertical = 3.dp)) {
                        RText(t(if (item.kind == Kind.SERIES) "series" else "movies"), 13, Color.White, bold = true)
                    }
                    QualityPill("4K UHD", Color(0xFF8B5CF6))
                    val r = imdb
                    if (r != null) {
                        ImdbBadge(r)
                    } else {
                        val localR = (details?.rating?.takeIf { it.isNotBlank() && it != "0" } ?: item.rating.takeIf { it.isNotBlank() && it != "0" })
                        if (localR != null) {
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0x33F5C518)).border(0.8.dp, Color(0x88F5C518), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                RText("★ $localR", 13, Color(0xFFF5C518), bold = true)
                            }
                        }
                    }
                }
                RText(item.name, if (compact) 24 else 40, Color.White, bold = true, maxLines = 2, lineHeight = if (compact) 30 else 48)
                val meta = listOfNotNull(
                    details?.year?.takeIf { it.isNotBlank() },
                    details?.genre?.takeIf { it.isNotBlank() }?.split(",", "/")?.firstOrNull()?.trim(),
                    details?.duration?.takeIf { it.isNotBlank() },
                ).joinToString("  •  ")
                if (meta.isNotEmpty()) BodyText(meta, 14, C.primaryLight, bold = true, maxLines = 1)
                val plot = details?.plot.orEmpty()
                if (plot.isNotBlank()) {
                    if (!compact) BodyText(plot, 16, Color(0xFFE5DEF5), maxLines = 3)
                } else if (loaded && !compact) {
                    BodyText(t("no_ar_desc"), 15, C.textDim, maxLines = 1)
                }
                Row(
                    Modifier.onFocusChanged { held = it.hasFocus },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Focusable(shape = RoundedCornerShape(26.dp), focusScale = 1.07f, onClick = {
                        if (item.kind == Kind.MOVIE) vm.playMovie(item) else vm.openDetail(item)
                    }) { f ->
                        Row(
                            Modifier.clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        if (f) listOf(Color(0xFF9F67FF), Color(0xFF7C3AED)) else listOf(Color(0xFF7C3AED), Color(0xFF5B21B6)),
                                    ),
                                )
                                .border(1.dp, Color(0x88C4A1FF), RoundedCornerShape(26.dp))
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RText(t("watch_now"), 18, Color.White, bold = true, maxLines = 1)
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                    Focusable(shape = RoundedCornerShape(26.dp), focusScale = 1.07f, onClick = { vm.openDetail(item) }) { f ->
                        Box(
                            Modifier.clip(RoundedCornerShape(26.dp))
                                .background(if (f) Color(0x66FFFFFF) else Color(0x33FFFFFF))
                                .padding(horizontal = 22.dp, vertical = 10.dp),
                        ) { RText(t("details"), 18, Color.White, bold = true, maxLines = 1) }
                    }
                    val inFav = vm.isIn("fav", item)
                    Focusable(shape = RoundedCornerShape(26.dp), focusScale = 1.07f, onClick = { vm.toggle("fav", item) }) { f ->
                        Row(
                            Modifier.clip(RoundedCornerShape(26.dp))
                                .background(if (f) Color(0x558B5CF6) else if (inFav) Color(0x338B5CF6) else Color(0x22FFFFFF))
                                .border(1.dp, if (inFav) Color(0xFFA78BFA) else Color(0x44FFFFFF), RoundedCornerShape(26.dp))
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Filled.Favorite, null, tint = if (inFav) Color(0xFFD946EF) else Color.White, modifier = Modifier.size(18.dp))
                            RText(t("my_list"), 16, Color.White, bold = true, maxLines = 1)
                        }
                    }
                }
            }
            if (items.size > 1) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(start = if (compact) 16.dp else 32.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items.indices.forEach { i ->
                        Box(
                            Modifier.height(5.dp).width(if (i == idx) 30.dp else 22.dp).clip(RoundedCornerShape(3.dp))
                                .background(if (i == idx) Color.White else Color(0x66FFFFFF)),
                        )
                    }
                }
            }
        }
    }
}

// ============================ بلاطات الأقسام ============================
@Composable
private fun HomeTile(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val compact = isCompact()
    Focusable(modifier = modifier, shape = shape, focusScale = 1.05f, onClick = onClick) { f ->
        Box(
            Modifier.fillMaxWidth().height(if (compact) 64.dp else (LocalConfiguration.current.screenHeightDp * 0.125f).dp)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        if (f) listOf(Color(0xFF3B1A7A), Color(0xFF1A0B3A)) else listOf(Color(0xFF1B0D3D), Color(0xFF0A0518)),
                    ),
                )
                .border(1.dp, if (f) C.primary else panelBorder, shape),
        ) {
            Icon(
                icon, null, tint = C.primary,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(if (compact) 40.dp else 78.dp).alpha(0.28f),
            )
            Icon(icon, null, tint = C.primaryLight, modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 12.dp).size(if (compact) 20.dp else 28.dp))
            Row(
                Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RText(label, if (compact) 14 else 20, Color.White, bold = true, maxLines = 1)
                Icon(Icons.Filled.ChevronRight, null, tint = C.primary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun HomeTiles(vm: AppViewModel) {
    val pad = if (isCompact()) 14.dp else 36.dp
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(Modifier.fillMaxWidth().padding(horizontal = pad), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            HomeTile(t("movies"), Icons.Filled.Movie, Modifier.weight(1f)) { vm.selectTab(2) }
            HomeTile(t("series"), Icons.Filled.Tv, Modifier.weight(1f)) { vm.selectTab(3) }
            HomeTile(t("live"), Icons.Filled.LiveTv, Modifier.weight(1f)) { vm.selectTab(1) }
            HomeTile(t("my_list"), Icons.Filled.Favorite, Modifier.weight(1f)) { vm.selectTab(5) }
            HomeTile(t("settings"), Icons.Filled.Settings, Modifier.weight(1f)) { vm.selectTab(6) }
        }
    }
}

// ============================ صف مربعات (الأعلى تقييمًا) ============================
@Composable
fun SquareRow(title: String, items: List<Item>, onItem: (Item) -> Unit) {
    if (items.isEmpty()) return
    val compact = isCompact()
    val pad = if (compact) 14.dp else 36.dp
    val h = if (compact) 130.dp else (LocalConfiguration.current.screenHeightDp * 0.30f).dp
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = pad, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = C.primary, modifier = Modifier.size(26.dp))
            RText(title, if (compact) 18 else 24, Color.White, bold = true, maxLines = 1)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = pad, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.key }) { idx, item ->
                val shape = RoundedCornerShape(14.dp)
                Focusable(Modifier.width(h * 1.12f).height(h), shape, 1.07f, onClick = { onItem(item) }) { f ->
                    Box(
                        Modifier.fillMaxSize().clip(shape).background(Color(0xFF140A2B))
                            .then(if (f) Modifier.border(2.5.dp, Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFD946EF))), shape) else Modifier.border(1.dp, Color(0x228B5CF6), shape))
                    ) {
                        Img(item.image, Modifier.fillMaxSize(), w = 300, h = 300)

                        // شارة ترتيب Top 10 على الزاوية
                        Box(
                            Modifier.align(Alignment.TopStart).padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            RText("#${idx + 1}", 11, Color.White, bold = true)
                        }

                        // شارة التقييم
                        val r = item.rating.trim().takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
                        if (r != null) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xDD0B0616))
                                    .border(0.8.dp, Color(0x55F5C518), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    RText("★", 11, Color(0xFFF5C518), bold = true)
                                    RText(r, 11, Color.White, bold = true)
                                }
                            }
                        }

                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(colorStops = arrayOf(0.55f to Color.Transparent, 1f to Color(0xEE05020B))),
                            ),
                        )
                        BodyText(
                            item.name, 12, if (f) Color.White else C.text, bold = true, maxLines = 1,
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

// ============================ الشاشة الرئيسية ============================
@Composable
fun HomeTab(vm: AppViewModel) {
    var recentMovies by remember { mutableStateOf<List<Item>>(emptyList()) }
    var recentSeries by remember { mutableStateOf<List<Item>>(emptyList()) }
    var topMovies by remember { mutableStateOf<List<Item>>(emptyList()) }
    var topSeries by remember { mutableStateOf<List<Item>>(emptyList()) }
    LaunchedEffect(vm.dataVersion) {
        recentMovies = vm.recentItems(Kind.MOVIE)
        recentSeries = vm.recentItems(Kind.SERIES)
        topMovies = vm.topItems(Kind.MOVIE)
        topSeries = vm.topItems(Kind.SERIES)
    }
    var trendMovies by remember { mutableStateOf<List<Item>>(emptyList()) }
    var trendSeries by remember { mutableStateOf<List<Item>>(emptyList()) }
    LaunchedEffect(vm.dataVersion) {
        trendMovies = vm.trending(Kind.MOVIE)
        trendSeries = vm.trending(Kind.SERIES)
    }
    var recMovies by remember { mutableStateOf<List<Item>>(emptyList()) }
    var recSeries by remember { mutableStateOf<List<Item>>(emptyList()) }
    LaunchedEffect(vm.dataVersion, vm.history.size, vm.watched.size, vm.favorites.size) {
        recMovies = vm.recommendedFor(Kind.MOVIE)
        recSeries = vm.recommendedFor(Kind.SERIES)
    }
    var horrorMovies by remember { mutableStateOf<List<Item>>(emptyList()) }
    var horrorSeries by remember { mutableStateOf<List<Item>>(emptyList()) }
    LaunchedEffect(vm.dataVersion) {
        if (Season.halloween) { horrorMovies = vm.horror(Kind.MOVIE); horrorSeries = vm.horror(Kind.SERIES) }
    }
    val featured = remember(recentMovies, recentSeries) {
        (recentMovies.take(2) + recentSeries.take(2)).filter { it.image.isNotBlank() }
    }
    val sugMovies = remember(topMovies) { topMovies.shuffled().take(20) }
    val sugSeries = remember(topSeries) { topSeries.shuffled().take(20) }
    val bases = remember(vm.history) {
        vm.history.filter { it.kind != Kind.LIVE }.distinctBy { it.key }.take(3)
    }
    var similar by remember { mutableStateOf<List<Pair<Item, List<Item>>>>(emptyList()) }
    LaunchedEffect(bases, vm.dataVersion) { similar = bases.map { it to vm.similarTo(it) } }
    val open: (Item) -> Unit = { vm.openDetail(it) }
    val favLive = vm.favorites.filter { it.kind == Kind.LIVE }
    val recentLive = vm.historyOf(Kind.LIVE)
    val playLive: (Item, List<Item>) -> Unit = { it, src -> vm.playLive(it, LiveSource(favs = src)) }

    LazyColumn(contentPadding = PaddingValues(top = 2.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (Season.halloween) item { HalloweenBanner() }
        item { SetupCard(vm) }
        item { FeaturedBanner(vm, featured) }
        item { HomeTiles(vm) }
        if (Season.halloween && horrorMovies.size >= 3) item { FocusRow(t("hall_movies"), horrorMovies, vm, open) }
        if (Season.halloween && horrorSeries.size >= 3) item { FocusRow(t("hall_series"), horrorSeries, vm, open) }
        item { SquareRow(t("top_rated"), topMovies, open) }
        if (vm.recent.isNotEmpty()) item { FocusRow(t("continue_watching"), vm.recent, vm, open) }
        if (recMovies.size >= 4) item { FocusRow(t("rec_movies"), recMovies, vm, open) }
        if (recSeries.size >= 4) item { FocusRow(t("rec_series"), recSeries, vm, open) }
        if (trendMovies.size >= 3) item { FocusRow(t("trend_movies"), trendMovies, vm, open) }
        if (trendSeries.size >= 3) item { FocusRow(t("trend_series"), trendSeries, vm, open) }
        similar.forEach { (base, list) ->
            if (list.size >= 3) item(key = "sim_" + base.key) { FocusRow(t("because_watched") + " " + base.name, list, vm, open) }
        }
        if (favLive.isNotEmpty()) item { ContentRow(t("favorite_channels"), favLive) { playLive(it, favLive) } }
        if (recentLive.isNotEmpty()) item { ContentRow(t("recent_live"), recentLive) { playLive(it, recentLive) } }
        vm.historyOf(Kind.MOVIE).takeIf { it.isNotEmpty() }?.let { l -> item { FocusRow(t("recent_movies_w"), l, vm, open) } }
        vm.historyOf(Kind.SERIES).takeIf { it.isNotEmpty() }?.let { l -> item { FocusRow(t("recent_series_w"), l, vm, open) } }
        if (vm.later.isNotEmpty()) item { FocusRow(t("watch_later"), vm.later, vm, open) }
        item { FocusRow(t("recent_movies"), recentMovies, vm, open) }
        item { FocusRow(t("recent_series"), recentSeries, vm, open) }
        item { FocusRow(t("suggested_movies"), sugMovies, vm, open) }
        item { FocusRow(t("suggested_series"), sugSeries, vm, open) }
    }
}

// ============================ صفحات الشريط الجانبي ============================
@Composable
fun ItemsPage(vm: AppViewModel, items: List<Item>, emptyKey: String) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { RText(t(emptyKey), 22, C.textDim) }
        return
    }
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(138.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(items.size, key = { items[it].key }) { i ->
            val it = items[i]
            PosterCard(it, w = 128.dp, h = 192.dp) {
                if (it.kind == Kind.LIVE) vm.playLive(it, LiveSource(favs = items.filter { x -> x.kind == Kind.LIVE })) else vm.openDetail(it)
            }
        }
    }
}

/** الأعمال التي لها ترجمات محفوظة (من المشاهدات والمفضلة والأخيرة). */
@Composable
fun SubsPage(vm: AppViewModel) {
    val withSubs = remember(vm.history, vm.favorites, vm.recent, vm.later, vm.watched) {
        (vm.history + vm.favorites + vm.recent + vm.later + vm.watched)
            .filter { it.kind != Kind.LIVE }.distinctBy { it.key }
            .filter { vm.savedSubs(it.key).isNotEmpty() }
    }
    if (withSubs.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) { RText(t("subs_empty"), 20, C.textDim) }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(withSubs, key = { it.key }) { it ->
            VodRow(it) { vm.openDetail(it) }
            BodyText(vm.savedSubs(it.key).size.toString() + " " + t("subs_saved"), 13, C.textDim, modifier = Modifier.padding(start = 8.dp))
        }
    }
}


/** شريط موسم الهالوين (يظهر 3 أيام فقط). */
@Composable
fun HalloweenBanner() {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp)
            .background(Color(0xFF1A0D06), shape).border(2.dp, C.accent, shape).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RText("🎃", 34, Color.White)
        androidx.compose.foundation.layout.Column {
            RText(t("hall_title"), 22, C.accent, bold = true)
            BodyText(t("hall_sub"), 14, C.textDim)
        }
    }
}
