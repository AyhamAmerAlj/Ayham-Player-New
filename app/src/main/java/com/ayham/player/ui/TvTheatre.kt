package com.ayham.player.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Details
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import kotlinx.coroutines.delay


/** «المسرح»: قسم الأفلام/المسلسلات للتلفاز. شريط أقسام جانبي، خلفية حيّة تتلوّن بلون البوستر المحدّد، ومعلومات العمل، وأرفف بوسترات كبيرة. */
@Composable
fun TvTheatre(vm: AppViewModel, kind: Kind) {
    val sH = LocalConfiguration.current.screenHeightDp
    val small = sH < 440
    val ar = vm.lang == "ar"
    val cats = vm.categories[kind].orEmpty()
    var sel by androidx.compose.runtime.saveable.rememberSaveable(kind.name + "_tvsel") { mutableStateOf("feed") }
    var q by androidx.compose.runtime.saveable.rememberSaveable(kind.name + "_tvq") { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var found by remember(kind) { mutableStateOf<List<Item>>(emptyList()) }
    val searching = q.trim().length >= 2
    LaunchedEffect(q, vm.dataVersion) {
        if (q.trim().length < 2) { found = emptyList(); return@LaunchedEffect }
        delay(350)
        found = vm.searchKind(kind, q)
    }
    val pager = rememberPager(listOf(kind, sel, vm.dataVersion), 60) { off, lim ->
        when (sel) {
            "recent", "feed" -> vm.recentItems(kind, off + lim).drop(off)
            "top" -> vm.topItems(kind, off + lim).drop(off)
            "all" -> vm.allItems(kind, lim, off)
            else -> vm.inCategory(kind, sel, lim, off)
        }
    }
    val list: List<Item> = if (searching) found else pager.items
    var contItems by remember(kind, vm.dataVersion) { mutableStateOf<List<Item>>(emptyList()) }
    var top10 by remember(kind, vm.dataVersion) { mutableStateOf<List<Item>>(emptyList()) }
    LaunchedEffect(kind, vm.dataVersion, vm.history.size) {
        contItems = vm.historyOf(kind).distinctBy { it.key }.take(20)
        top10 = runCatching { vm.topItems(kind, 10) }.getOrDefault(emptyList())
    }
    var focusItem by remember(sel, searching) { mutableStateOf<Item?>(null) }
    val item = focusItem ?: list.firstOrNull()

    var details by remember { mutableStateOf<Details?>(null) }
    var imdb by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(item?.key) {
        details = null; imdb = null
        val it = item ?: return@LaunchedEffect
        delay(300)
        val d = runCatching { if (it.kind == Kind.SERIES) vm.seriesInfo(it).details else vm.movieDetails(it) }.getOrNull()
        details = d
        if (d != null) imdb = runCatching { vm.imdbRating(it, d.year) }.getOrNull()
    }
    val arName = rememberArabicName(item)
    val kindLabel = t(if (kind == Kind.SERIES) "series" else "movies")

    val tint = rememberPosterColor(item?.image)
    Box(Modifier.fillMaxSize()) {
        CinemaBackdrop(details?.backdrop.orEmpty().ifEmpty { item?.image.orEmpty() })
        // وهج بلون البوستر (يتبدل بنعومة مع التركيز)
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(tint.copy(alpha = 0.34f), Color.Transparent), center = Offset(1500f, 120f), radius = 1300f)))

        Row(Modifier.fillMaxSize()) {
            // ---------- الشريط الجانبي للأقسام ----------
            val fixed = listOf("feed" to (if (ar) "الرئيسية" else "Home"), "recent" to t(if (kind == Kind.SERIES) "recent_series" else "recent_movies").substringBefore("(").trim(), "top" to (kindLabel + " ★"), "all" to t("all"))
            Column(
                Modifier.fillMaxHeight().width(if (small) 150.dp else 190.dp)
                    .background(Brush.horizontalGradient(listOf(tc(0xEE05020B), tc(0xAA05020B), Color.Transparent)))
                    .padding(top = if (small) 14.dp else 26.dp, start = 18.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RText(kindLabel, if (small) 20 else 26, Color.White, bold = true, maxLines = 1, modifier = Modifier.padding(bottom = 6.dp))
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    item(key = "_search") {
                        RailItem(t("search"), showSearch, icon = true) { showSearch = !showSearch; if (!showSearch) q = "" }
                    }
                    items(fixed.size, key = { "_" + fixed[it].first }) { i ->
                        val (k, label) = fixed[i]
                        RailItem(label, sel == k && !searching) { sel = k; q = ""; showSearch = false }
                    }
                    itemsIndexed(cats, key = { _, c -> c.id }) { _, c ->
                        RailItem(c.name, sel == c.id && !searching) { sel = c.id; q = ""; showSearch = false }
                    }
                }
            }

            // ---------- المحتوى ----------
            Column(Modifier.weight(1f).fillMaxHeight().padding(top = if (small) 18.dp else 30.dp, bottom = 10.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(end = 36.dp), contentAlignment = Alignment.BottomStart) {
                    if (item == null) {
                        RText(if (searching) t("no_results") else (if (ar) "لا توجد عناصر في هذا القسم" else "Nothing in this section"), 24, C.textDim, bold = true)
                    } else {
                        CinemaHeroText(vm, item, details, imdb, top10.indexOfFirst { it.key == item.key } + 1, small, arName)
                    }
                }
                if (showSearch) {
                    AyhamField(q, { q = it }, t("search_in_" + kind.name.lowercase()), Modifier.padding(end = 36.dp, top = 4.dp, bottom = 4.dp).fillMaxWidth(0.6f), imeAction = androidx.compose.ui.text.input.ImeAction.Search)
                }

                var initFocus by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) { delay(700); initFocus = false }
                val posterH = (sH * (if (small) 0.28f else 0.31f)).coerceIn(110f, 230f).dp
                val rowBlock = posterH + 62.dp
                val others = remember(cats, sel, searching) { if (searching) emptyList() else cats.filter { it.id != sel } }
                val selLabel = when (sel) {
                    "recent", "feed" -> t(if (kind == Kind.SERIES) "recent_series" else "recent_movies").substringBefore("(").trim()
                    "top" -> kindLabel + " ★"
                    "all" -> t("all")
                    else -> cats.firstOrNull { it.id == sel }?.name.orEmpty()
                }
                val hp = 8.dp
                LazyColumn(Modifier.fillMaxWidth().height(rowBlock * 1.22f)) {
                    val feed = sel == "feed" && !searching
                    if (feed && contItems.isNotEmpty()) item(key = "row_cont") {
                        CinemaRow(if (ar) "تابع المشاهدة" else "Continue watching", contItems, RowStyle.LAND, posterH, hp + 4.dp, first = initFocus, onFocus = { focusItem = it }, onOpen = { vm.openDetail(it) })
                    }
                    if (feed && top10.size >= 5) item(key = "row_top10") {
                        CinemaRow(if (ar) (if (kind == Kind.SERIES) "الأعلى 10 مسلسلات" else "الأعلى 10 أفلام") else (if (kind == Kind.SERIES) "Top 10 series" else "Top 10 movies"),
                            top10, RowStyle.RANK, posterH, hp + 4.dp, order = 1, first = initFocus && contItems.isEmpty(), onFocus = { focusItem = it }, onOpen = { vm.openDetail(it) })
                    }
                    item(key = "row_main") {
                        CinemaRow(if (searching) t("search") else if (feed) (if (ar) "أُضيف حديثًا" else "New arrivals") else selLabel, list, RowStyle.PORTRAIT, posterH, hp + 4.dp,
                            first = initFocus && !(feed && (contItems.isNotEmpty() || top10.size >= 5)), onFocus = { focusItem = it }, onIndex = { i -> if (!searching && i >= list.size - 12) pager.loadMore() }, onOpen = { vm.openDetail(it) })
                    }
                    items(others, key = { "row_" + it.id }) { c ->
                        var rowItems by remember(c.id, vm.dataVersion) { mutableStateOf<List<Item>?>(null) }
                        LaunchedEffect(c.id, vm.dataVersion) {
                            delay(200)
                            rowItems = runCatching { vm.inCategory(kind, c.id, 24, 0) }.getOrDefault(emptyList())
                        }
                        val ri = rowItems
                        if (ri == null) Box(Modifier.fillMaxWidth().height(rowBlock).padding(start = hp + 4.dp, top = 6.dp)) { RText(c.name, 17, C.textDim, bold = true, maxLines = 1) }
                        else if (ri.isNotEmpty()) CinemaRow(c.name, ri, RowStyle.PORTRAIT, posterH, hp + 4.dp, onFocus = { focusItem = it }, onOpen = { vm.openDetail(it) })
                    }
                }
            }
        }
    }
}

/** عنصر الشريط الجانبي: مؤشر لوني على اليمين/اليسار للقسم المحدد ووهج عند التركيز. */
@Composable
private fun RailItem(label: String, selected: Boolean, icon: Boolean = false, onClick: () -> Unit) {
    Focusable(shape = RoundedCornerShape(14.dp), focusScale = 1.04f, onClick = onClick) { f ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(if (f) tc(0xCC7C3AED) else if (selected) tc(0x557C3AED) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon) Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(16.dp))
            else Box(Modifier.size(width = 3.dp, height = 16.dp).clip(RoundedCornerShape(2.dp)).background(if (selected || f) Color.White else Color.Transparent))
            RText(label, 15, if (f || selected) Color.White else tc(0xFFC9BDE6), bold = selected || f, maxLines = 1)
        }
    }
}
