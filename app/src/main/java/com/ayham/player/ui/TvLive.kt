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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
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

private fun qual(n: String): String? {
    val u = n.uppercase(Locale.ROOT)
    return when {
        "4K" in u || "UHD" in u -> "4K"
        "FHD" in u || "1080" in u -> "FHD"
        Regex("\\bHD\\b").containsMatchIn(u) || "720" in u -> "HD"
        Regex("\\bSD\\b").containsMatchIn(u) -> "SD"
        else -> null
    }
}

/** شاشة القنوات للتلفاز: قائمة تصنيفات جانبية + لوحة معلومات + شبكة قنوات. الكتابة برقم القناة تقفز إليها. */
@Composable
fun TvLive(vm: AppViewModel) {
    val sH = LocalConfiguration.current.screenHeightDp
    val small = sH < 440
    val ar = vm.lang == "ar"
    val cats = vm.categories[Kind.LIVE].orEmpty()
    val favLive = remember(vm.favorites) { vm.favorites.filter { it.kind == Kind.LIVE } }
    val recentLive = remember(vm.history) { vm.historyOf(Kind.LIVE) }
    var sel by remember { mutableStateOf(vm.liveSel) }
    LaunchedEffect(sel) { if (vm.liveSel != sel) { vm.liveSel = sel; vm.liveFocusKey = null } }
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

    // الانتقال برقم القناة (ترتيبها في القائمة الحالية)
    var typed by remember { mutableStateOf("") }
    LaunchedEffect(typed) {
        if (typed.isEmpty()) return@LaunchedEffect
        delay(1400)
        val n = typed.toIntOrNull()
        typed = ""
        if (n != null && n in 1..list.size) pick(list[n - 1])
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
    val sideW = if (small) 170.dp else 230.dp
    val cardW = if (small) 130.dp else 170.dp
    val cardH = if (small) 96.dp else 120.dp

    Box(
        Modifier.fillMaxSize().background(Color(0xFF05020B)).onPreviewKeyEvent { e ->
            if (e.type == KeyEventType.KeyDown) {
                val kc = e.key.nativeKeyCode
                val d = when (kc) { in 7..16 -> kc - 7; in 144..153 -> kc - 144; else -> -1 }
                if (d >= 0 && !showSearch && typed.length < 4) { typed += d.toString(); true } else false
            } else false
        },
    ) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(tc(0x558B5CF6), Color.Transparent), center = Offset(1500f, 200f), radius = 1300f)))
        ThemeState.palette.glow?.let { g ->
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(g.copy(alpha = 0.25f), Color.Transparent), center = Offset(0f, 0f), radius = 1100f)))
        }
        Row(Modifier.fillMaxSize().padding(top = if (small) 14.dp else 24.dp)) {
            // ---------- القائمة الجانبية ----------
            LazyColumn(
                Modifier.width(sideW).fillMaxHeight().background(tc(0x66000000)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item(key = "_search") { SideChip(t("search"), showSearch, Icons.Filled.Search) { showSearch = !showSearch; if (!showSearch) q = "" } }
                item(key = "_fav") { SideChip(favL, sel == "fav" && !searching, Icons.Filled.Favorite) { sel = "fav"; q = ""; showSearch = false } }
                item(key = "_recent") { SideChip(recL, sel == "recent" && !searching, Icons.Filled.History) { sel = "recent"; q = ""; showSearch = false } }
                item(key = "_all") { SideChip(allL, sel == "all" && !searching, Icons.Filled.GridView) { sel = "all"; q = ""; showSearch = false } }
                itemsIndexed(cats, key = { _, c -> c.id }) { _, c -> SideChip(c.name, sel == c.id && !searching, null) { sel = c.id; q = ""; showSearch = false } }
            }

            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 22.dp, end = 30.dp)) {
                // ---------- لوحة المعلومات ----------
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Crossfade(targetState = item?.image.orEmpty(), modifier = Modifier.size(if (small) 84.dp else 124.dp), animationSpec = tween(300), label = "liveLogo") { url ->
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)).background(tc(0x22FFFFFF)).border(1.dp, tc(0x558B5CF6), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                            Img(url, Modifier.fillMaxSize().padding(12.dp), ContentScale.Fit, 240, 240)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(if (small) 3.dp else 6.dp)) {
                        if (item == null) {
                            RText(if (searching) t("no_results") else (if (ar) "لا توجد قنوات في هذا القسم" else "No channels here"), 22, C.textDim, bold = true)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(C.danger).padding(horizontal = 8.dp, vertical = 2.dp)) { RText(L_live, 12, Color.White, bold = true) }
                                qual(item.name)?.let { Box(Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, tc(0xFFA78BFA), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { RText(it, 12, tc(0xFFC4B5FD), bold = true) } }
                                if (selLabel.isNotBlank()) BodyText(selLabel, 14, C.textDim, maxLines = 1)
                            }
                            RText(item.name, if (small) 24 else 34, Color.White, bold = true, maxLines = 1)
                            if (cur != null) {
                                RText((if (ar) "الآن: " else "Now: ") + cur.title, if (small) 14 else 17, tc(0xFFE5DEF5), bold = true, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(0.7f)) {
                                    BodyText(hm(cur.start), 13, C.textDim)
                                    val frac = ((nowS - cur.start).toFloat() / (cur.end - cur.start).coerceAtLeast(1)).coerceIn(0f, 1f)
                                    Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(tc(0x33FFFFFF))) {
                                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Brush.horizontalGradient(listOf(C.primary, C.accent))))
                                    }
                                    BodyText(hm(cur.end), 13, C.textDim)
                                }
                                if (nxt != null && !small) BodyText((if (ar) "التالي: " else "Next: ") + nxt.title + "  (" + hm(nxt.start) + ")", 14, C.textDim, maxLines = 1)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Focusable(shape = RoundedCornerShape(24.dp), focusScale = 1.06f, onClick = { pick(item) }) { f ->
                                    Row(
                                        Modifier.clip(RoundedCornerShape(24.dp))
                                            .background(Brush.horizontalGradient(ThemeState.palette.stripe ?: if (f) listOf(tc(0xFFA855F7), tc(0xFF7C3AED)) else listOf(tc(0xFF8B1FD9), tc(0xFF6D28D9))))
                                            .padding(horizontal = 22.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        RText(t("watch_now"), 16, Color.White, bold = true, maxLines = 1)
                                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                val inFav = vm.isIn("fav", item)
                                Focusable(shape = RoundedCornerShape(24.dp), focusScale = 1.06f, onClick = { vm.toggle("fav", item) }) { f ->
                                    Row(Modifier.clip(RoundedCornerShape(24.dp)).background(if (f) tc(0x558B5CF6) else tc(0x33FFFFFF)).padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Filled.Favorite, null, tint = if (inFav) tc(0xFFD946EF) else Color.White, modifier = Modifier.size(17.dp))
                                        RText(t("my_list"), 14, Color.White, bold = true, maxLines = 1)
                                    }
                                }
                                Focusable(shape = RoundedCornerShape(24.dp), focusScale = 1.06f, onClick = { vm.openMulti(item) }) { f ->
                                    Row(Modifier.clip(RoundedCornerShape(24.dp)).background(if (f) tc(0x558B5CF6) else tc(0x33FFFFFF)).padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Filled.GridView, null, tint = Color.White, modifier = Modifier.size(17.dp))
                                        RText(if (ar) "عرض متعدد" else "Multi-view", 14, Color.White, bold = true, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    if (mini != null) CinemaMini(vm, mini, if (small) 190.dp else 290.dp)
                }

                if (showSearch) {
                    AyhamField(q, { q = it }, t("search_in_live"), Modifier.padding(bottom = 8.dp).fillMaxWidth(0.5f), imeAction = androidx.compose.ui.text.input.ImeAction.Search)
                }

                // ---------- شبكة القنوات ----------
                var restoreKey by remember { mutableStateOf(vm.liveFocusKey) }
                var initFocus by remember { mutableStateOf(restoreKey == null) }
                LaunchedEffect(Unit) { delay(700); initFocus = false }
                val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                var tries by remember { mutableStateOf(0) }
                LaunchedEffect(list.size, restoreKey) {
                    val rk = restoreKey ?: return@LaunchedEffect
                    val idx = list.indexOfFirst { it.key == rk }
                    if (idx >= 0) gridState.scrollToItem(idx)
                    else if (!searching && tries < 12) { tries++; pager.loadMore() }
                    else restoreKey = null
                }
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(cardW),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    gridItemsIndexed(list, key = { _, it -> it.key }) { i, ch ->
                        if (!searching && i >= list.size - 16) LaunchedEffect(list.size) { pager.loadMore() }
                        val shape = RoundedCornerShape(14.dp)
                        val fav = vm.isIn("fav", ch)
                        val ql = qual(ch.name)
                        Focusable(
                            Modifier.width(cardW).height(cardH).onFocusChanged { f -> if (f.hasFocus) { focusItem = ch; vm.liveFocusKey = ch.key } }.then(if ((initFocus && i == 0 && !showSearch) || (restoreKey == ch.key)) Modifier.autoFocus().also { LaunchedEffect(Unit) { delay(300); restoreKey = null } } else Modifier),
                            shape, 1.05f, onLongClick = { vm.toggle("fav", ch) }, onClick = { pick(ch) },
                        ) { f ->
                            Column(
                                Modifier.fillMaxSize().clip(shape).background(if (f) C.surfaceHi else C.surface)
                                    .border(if (f) 2.dp else 0.dp, if (f) tc(0xFFA78BFA) else Color.Transparent, shape).padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    if (ch.image.isBlank()) RText(ch.name.take(1), 26, C.primaryLight, bold = true)
                                    Img(ch.image, Modifier.fillMaxSize().padding(2.dp), ContentScale.Fit, 160, 160)
                                    Box(Modifier.align(Alignment.TopStart).clip(RoundedCornerShape(4.dp)).background(C.danger).padding(horizontal = 4.dp)) { RText("LIVE", 8, Color.White, bold = true) }
                                    if (ql != null) Box(Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(4.dp)).background(tc(0xCC6D28D9)).padding(horizontal = 4.dp)) { RText(ql, 8, Color.White, bold = true) }
                                    if (fav) Icon(Icons.Filled.Favorite, null, tint = C.accent, modifier = Modifier.align(Alignment.BottomEnd).size(13.dp))
                                    if (mini?.id == ch.id) Box(Modifier.align(Alignment.BottomStart).size(9.dp).clip(CircleShape).background(C.danger))
                                }
                                BodyText(ch.name, 12, if (f) Color.White else C.text, bold = f, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        if (typed.isNotEmpty()) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = 30.dp).clip(RoundedCornerShape(16.dp)).background(tc(0xDD1B0B3A)).border(1.dp, tc(0xFFA78BFA), RoundedCornerShape(16.dp)).padding(horizontal = 26.dp, vertical = 8.dp)) {
                RText(typed, 38, Color.White, bold = true)
            }
        }
    }
}

@Composable
private fun SideChip(label: String, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit) {
    Focusable(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), focusScale = 1.03f, onClick = onClick) { f ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(if (f) C.primary else if (selected) tc(0x887C3AED) else tc(0x22FFFFFF)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
            RText(label, 14, Color.White, bold = selected || f, maxLines = 1)
        }
    }
}
