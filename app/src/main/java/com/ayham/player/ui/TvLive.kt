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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.EpgEntry
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import com.ayham.player.data.LiveSource
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun hm(sec: Long) = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(sec * 1000))

/** شاشة القنوات للتلفاز بأسلوب سينمائي: شعار كبير وبرنامج الآن/التالي مع تقدم، أقسام ثابتة، وصفوف قنوات عند النزول. */
@Composable
fun TvLive(vm: AppViewModel) {
    val sH = LocalConfiguration.current.screenHeightDp
    val small = sH < 440
    val ar = vm.lang == "ar"
    val cats = vm.categories[Kind.LIVE].orEmpty()
    val favLive = remember(vm.favorites) { vm.favorites.filter { it.kind == Kind.LIVE } }
    val recentLive = remember(vm.history) { vm.historyOf(Kind.LIVE) }
    var sel by rememberSaveable { mutableStateOf("all") }
    var q by rememberSaveable { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var found by remember { mutableStateOf<List<Item>>(emptyList()) }
    val searching = q.trim().length >= 2
    LaunchedEffect(q, vm.dataVersion) {
        if (q.trim().length < 2) { found = emptyList(); return@LaunchedEffect }
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
    val list: List<Item> = if (searching) found else pager.items
    var focusItem by remember(sel, searching) { mutableStateOf<Item?>(null) }
    val item = focusItem ?: list.firstOrNull()
    val mini = vm.miniItem
    val pick: (Item) -> Unit = { ch ->
        if (mini != null && mini.id != ch.id) vm.playMini(ch, source) else vm.playLive(ch, source)
    }

    // البرنامج الحالي والتالي للقناة المحددة
    var epg by remember { mutableStateOf<List<EpgEntry>>(emptyList()) }
    var nowS by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(item?.id) {
        epg = emptyList()
        val it = item ?: return@LaunchedEffect
        delay(250)
        epg = runCatching { vm.epg(it.id) }.getOrDefault(emptyList())
        nowS = System.currentTimeMillis() / 1000
    }
    val cur = epg.firstOrNull { it.start <= nowS && it.end > nowS }
    val nxt = cur?.let { c -> epg.firstOrNull { it.start >= c.end } }

    val L_live = if (ar) "مباشر" else "LIVE"
    val allL = t("all"); val favL = t("favorite_channels"); val recL = t("recent_live")
    val catName = cats.firstOrNull { it.id == sel }?.name.orEmpty()
    val selLabel = when (sel) { "all" -> allL; "fav" -> favL; "recent" -> recL; else -> catName }

    Box(Modifier.fillMaxSize().background(Color(0xFF05020B))) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(tc(0x558B5CF6), Color.Transparent), center = Offset(1500f, 300f), radius = 1300f)))
        ThemeState.palette.glow?.let { g ->
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(g.copy(alpha = 0.28f), Color.Transparent), center = Offset(0f, 0f), radius = 1100f)))
        }
        // الشعار الكبير يتبدل بتلاشٍ ناعم
        Crossfade(targetState = item?.image.orEmpty(), modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 70.dp).size(if (small) 150.dp else 230.dp), animationSpec = tween(400), label = "liveLogo") { url ->
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).background(tc(0x22FFFFFF)).border(1.dp, tc(0x338B5CF6), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
                Img(url, Modifier.fillMaxSize().padding(18.dp), ContentScale.Fit, 300, 300)
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.45f to tc(0x6605020B), 1f to tc(0xFF05020B))))
        if (mini != null) Box(Modifier.align(Alignment.TopStart).padding(start = 44.dp, top = 24.dp)) { MiniPlayer(vm, mini) }

        Column(Modifier.fillMaxSize().padding(top = if (small) 18.dp else 30.dp, bottom = 10.dp)) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 44.dp),
                verticalArrangement = Arrangement.spacedBy(if (small) 4.dp else 8.dp, Alignment.Bottom),
            ) {
                if (item == null) {
                    RText(if (searching) t("no_results") else (if (ar) "لا توجد قنوات في هذا القسم" else "No channels here"), 24, C.textDim, bold = true)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(C.danger))
                        RText(L_live, 14, C.danger, bold = true)
                        if (selLabel.isNotBlank()) BodyText(selLabel, 14, C.textDim, maxLines = 1)
                    }
                    RText(item.name, if (small) 28 else 44, Color.White, bold = true, maxLines = 2, lineHeight = if (small) 34 else 52, modifier = Modifier.fillMaxWidth(0.6f))
                    if (cur != null) {
                        RText((if (ar) "الآن: " else "Now: ") + cur.title, if (small) 16 else 19, tc(0xFFE5DEF5), bold = true, maxLines = 1, modifier = Modifier.fillMaxWidth(0.6f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(0.45f)) {
                            BodyText(hm(cur.start), 13, C.textDim)
                            val frac = ((nowS - cur.start).toFloat() / (cur.end - cur.start).coerceAtLeast(1)).coerceIn(0f, 1f)
                            Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(tc(0x33FFFFFF))) {
                                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Brush.horizontalGradient(listOf(C.primary, C.accent))))
                            }
                            BodyText(hm(cur.end), 13, C.textDim)
                        }
                        if (nxt != null && !small) BodyText((if (ar) "التالي: " else "Next: ") + nxt.title + "  (" + hm(nxt.start) + ")", 15, C.textDim, maxLines = 1, modifier = Modifier.fillMaxWidth(0.6f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Focusable(shape = RoundedCornerShape(26.dp), focusScale = 1.07f, onClick = { pick(item) }) { f ->
                            Row(
                                Modifier.clip(RoundedCornerShape(26.dp))
                                    .background(Brush.horizontalGradient(ThemeState.palette.stripe ?: if (f) listOf(tc(0xFFA855F7), tc(0xFF7C3AED)) else listOf(tc(0xFF8B1FD9), tc(0xFF6D28D9))))
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RText(t("watch_now"), 17, Color.White, bold = true, maxLines = 1)
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                        val inFav = vm.isIn("fav", item)
                        Focusable(shape = RoundedCornerShape(26.dp), focusScale = 1.07f, onClick = { vm.toggle("fav", item) }) { f ->
                            Row(
                                Modifier.clip(RoundedCornerShape(26.dp)).background(if (f) tc(0x558B5CF6) else tc(0x33FFFFFF)).padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Filled.Favorite, null, tint = if (inFav) tc(0xFFD946EF) else Color.White, modifier = Modifier.size(18.dp))
                                RText(t("my_list"), 15, Color.White, bold = true, maxLines = 1)
                            }
                        }
                        Focusable(shape = RoundedCornerShape(26.dp), focusScale = 1.07f, onClick = { vm.openMulti(item) }) { f ->
                            Row(
                                Modifier.clip(RoundedCornerShape(26.dp)).background(if (f) tc(0x558B5CF6) else tc(0x33FFFFFF)).padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Filled.GridView, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                RText(if (ar) "عرض متعدد" else "Multi-view", 15, Color.White, bold = true, maxLines = 1)
                            }
                        }
                    }
                }
            }

            if (showSearch) {
                AyhamField(q, { q = it }, t("search_in_live"), Modifier.padding(horizontal = 44.dp, vertical = 4.dp).fillMaxWidth(0.45f), imeAction = androidx.compose.ui.text.input.ImeAction.Search)
            }

            // ---------- الأقسام (ثابتة) ----------
            LazyRow(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "_search") {
                    Focusable(shape = RoundedCornerShape(20.dp), focusScale = 1.06f, onClick = { showSearch = !showSearch; if (!showSearch) q = "" }) { f ->
                        Row(
                            Modifier.clip(RoundedCornerShape(20.dp)).background(if (f) C.primary else if (showSearch) tc(0x887C3AED) else tc(0x44000000)).padding(horizontal = 14.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            RText(t("search"), 14, Color.White, bold = f || showSearch, maxLines = 1)
                        }
                    }
                }
                item(key = "_fav") { LiveChip(favL, sel == "fav" && !searching) { sel = "fav"; q = ""; showSearch = false } }
                item(key = "_recent") { LiveChip(recL, sel == "recent" && !searching) { sel = "recent"; q = ""; showSearch = false } }
                item(key = "_all") { LiveChip(allL, sel == "all" && !searching) { sel = "all"; q = ""; showSearch = false } }
                itemsIndexed(cats, key = { _, c -> c.id }) { _, c -> LiveChip(c.name, sel == c.id && !searching) { sel = c.id; q = ""; showSearch = false } }
            }

            // ---------- القنوات: الصف الأول للقسم المختار، وبالنزول بقية التصنيفات ----------
            var initFocus by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) { delay(700); initFocus = false }
            val cardH = (sH * (if (small) 0.19f else 0.21f)).coerceIn(80f, 150f).dp
            val rowBlock = cardH + 22.dp + 30.dp
            val others = remember(cats, sel, searching) { if (searching) emptyList() else cats.filter { it.id != sel } }
            LazyColumn(Modifier.fillMaxWidth().height(rowBlock * 1.28f)) {
                item(key = "row_main") {
                    LiveRow(if (searching) t("search") else selLabel, list, cardH, vm, onFocus = { focusItem = it }, onOpen = pick, first = initFocus, playingId = mini?.id) { i ->
                        if (!searching && i >= list.size - 12) pager.loadMore()
                    }
                }
                items(others, key = { "row_" + it.id }) { c ->
                    var rowItems by remember(c.id, vm.dataVersion) { mutableStateOf<List<Item>?>(null) }
                    LaunchedEffect(c.id, vm.dataVersion) {
                        delay(200)
                        rowItems = runCatching { vm.inCategory(Kind.LIVE, c.id, 30, 0) }.getOrDefault(emptyList())
                    }
                    val ri = rowItems
                    if (ri == null) Box(Modifier.fillMaxWidth().height(rowBlock).padding(start = 44.dp, top = 6.dp)) { RText(c.name, 17, C.textDim, bold = true, maxLines = 1) }
                    else if (ri.isNotEmpty()) LiveRow(c.name, ri, cardH, vm, onFocus = { focusItem = it }, onOpen = pick, playingId = mini?.id)
                }
            }
        }
    }
}

@Composable
private fun LiveChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Focusable(shape = RoundedCornerShape(20.dp), focusScale = 1.06f, onClick = onClick) { f ->
        Box(
            Modifier.clip(RoundedCornerShape(20.dp)).background(if (f) C.primary else if (selected) tc(0x887C3AED) else tc(0x44000000)).padding(horizontal = 16.dp, vertical = 5.dp),
        ) { RText(label, 14, Color.White, bold = selected || f, maxLines = 1) }
    }
}

@Composable
private fun LiveRow(
    title: String, items: List<Item>, cardH: Dp, vm: AppViewModel,
    onFocus: (Item) -> Unit, onOpen: (Item) -> Unit, first: Boolean = false, playingId: String? = null, onIndex: (Int) -> Unit = {},
) {
    Column(Modifier.fillMaxWidth()) {
        RText(title, 17, C.text, bold = true, maxLines = 1, modifier = Modifier.padding(start = 44.dp, top = 6.dp, end = 44.dp))
        LazyRow(
            Modifier.fillMaxWidth().height(cardH + 22.dp),
            contentPadding = PaddingValues(horizontal = 40.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.key }) { i, ch ->
                if (first && i >= items.size - 12) LaunchedEffect(items.size) { onIndex(i) }
                val shape = RoundedCornerShape(14.dp)
                val fav = vm.isIn("fav", ch)
                Focusable(
                    Modifier.width(cardH * 1.35f).height(cardH).onFocusChanged { f -> if (f.hasFocus) onFocus(ch) }.then(if (first && i == 0) Modifier.autoFocus() else Modifier),
                    shape, 1.06f, onLongClick = { vm.toggle("fav", ch) }, onClick = { onOpen(ch) },
                ) { f ->
                    Column(Modifier.fillMaxSize().clip(shape).background(if (f) C.surfaceHi else C.surface).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (ch.image.isBlank()) RText(ch.name.take(1), 26, C.primaryLight, bold = true)
                            Img(ch.image, Modifier.fillMaxSize().padding(2.dp), ContentScale.Fit, 160, 160)
                            if (fav) Icon(Icons.Filled.Favorite, null, tint = C.accent, modifier = Modifier.align(Alignment.TopEnd).size(14.dp))
                            if (playingId == ch.id) Box(Modifier.align(Alignment.TopStart).size(9.dp).clip(CircleShape).background(C.danger))
                        }
                        BodyText(ch.name, 12, if (f) Color.White else C.text, bold = f, maxLines = 1)
                    }
                }
            }
        }
    }
}
