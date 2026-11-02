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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
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


/** صفحة الأفلام/المسلسلات للتلفاز بأسلوب الرئيسية: خلفية كبيرة تتبع التركيز مع معلومات العنوان، وتحتها الأقسام والبوسترات. */
@Composable
fun TvBrowse(vm: AppViewModel, kind: Kind) {
    val sH = LocalConfiguration.current.screenHeightDp
    val small = sH < 440
    val ar = vm.lang == "ar"
    val cats = vm.categories[kind].orEmpty()
    var sel by androidx.compose.runtime.saveable.rememberSaveable(kind.name + "_tvsel") { mutableStateOf("recent") }
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
            "recent" -> vm.recentItems(kind, off + lim).drop(off)
            "top" -> vm.topItems(kind, off + lim).drop(off)
            "all" -> vm.allItems(kind, lim, off)
            else -> vm.inCategory(kind, sel, lim, off)
        }
    }
    val list: List<Item> = if (searching) found else pager.items
    var selKey by remember(sel, searching) { mutableStateOf<String?>(null) }
    val item = list.firstOrNull { it.key == selKey } ?: list.firstOrNull()

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

    Box(Modifier.fillMaxSize()) {
        val bg = details?.backdrop.orEmpty().ifEmpty { item?.image.orEmpty() }
        Crossfade(targetState = bg, modifier = Modifier.fillMaxSize(), animationSpec = tween(500), label = "tvbrowseBg") { url ->
            val kb = remember(url) { androidx.compose.animation.core.Animatable(1f) }
            LaunchedEffect(url) { kb.animateTo(1.08f, tween(14_000, easing = androidx.compose.animation.core.LinearEasing)) }
            Img(url, Modifier.fillMaxSize().graphicsLayer { scaleX = kb.value; scaleY = kb.value }, w = 1280, h = 720)
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(tc(0x990B0614), tc(0x220B0614), tc(0x990B0614)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to tc(0xAA0B0614), 0.3f to tc(0x000B0614), 0.5f to tc(0xBB05020B), 1f to tc(0xFF05020B))))
        ThemeState.palette.glow?.let { g ->
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(g.copy(alpha = 0.30f), Color.Transparent), center = Offset(0f, 0f), radius = 1100f)))
        }

        Column(Modifier.fillMaxSize().padding(top = if (small) 18.dp else 30.dp, bottom = 10.dp)) {
            // ---------- التفاصيل ----------
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 44.dp),
                verticalArrangement = Arrangement.spacedBy(if (small) 4.dp else 8.dp, Alignment.Bottom),
            ) {
                if (item == null) {
                    RText(if (searching) t("no_results") else (if (ar) "لا توجد عناصر في هذا القسم" else "Nothing in this section"), 24, C.textDim, bold = true)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(tc(0xFF6D28D9)).padding(horizontal = 12.dp, vertical = 2.dp)) {
                            RText(kindLabel, 13, Color.White, bold = true, maxLines = 1)
                        }
                        imdb?.let { ImdbBadge(it) } ?: (details?.rating?.takeIf { it.isNotBlank() && it != "0" } ?: item.rating.takeIf { it.isNotBlank() && it != "0" })
                            ?.let { RText("★ $it", 16, tc(0xFFF5C518), bold = true, maxLines = 1) }
                    }
                    RText(arName ?: item.name, if (small) 28 else 44, Color.White, bold = true, maxLines = if (small) 1 else 2, lineHeight = if (small) 34 else 52,
                        modifier = Modifier.fillMaxWidth(0.62f))
                    if (!small && arName != null && arName != item.name) RText(item.name, 16, tc(0xFFBDB2D6), maxLines = 1)
                    val meta = listOfNotNull(
                        details?.year?.takeIf { it.isNotBlank() },
                        details?.genre?.takeIf { it.isNotBlank() }?.split(",", "/")?.take(2)?.joinToString(" · ") { it.trim() },
                        details?.duration?.takeIf { it.isNotBlank() },
                    ).joinToString("  •  ")
                    if (meta.isNotEmpty() && !small) BodyText(meta, 15, tc(0xFFD9CCF5), bold = true, maxLines = 1)
                    val plot = details?.plot.orEmpty()
                    if (plot.isNotBlank() && sH >= 500) BodyText(plot, 15, tc(0xFFE5DEF5), maxLines = 3, modifier = Modifier.fillMaxWidth(0.5f))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Focusable(shape = RoundedCornerShape(26.dp), focusScale = 1.07f, onClick = {
                            if (item.kind == Kind.MOVIE) vm.playMovie(item) else vm.openDetail(item)
                        }) { f ->
                            Row(
                                Modifier.clip(RoundedCornerShape(26.dp))
                                    .background(Brush.horizontalGradient(ThemeState.palette.stripe ?: if (f) listOf(tc(0xFFA855F7), tc(0xFF7C3AED)) else listOf(tc(0xFF8B1FD9), tc(0xFF6D28D9))))
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RText(t("watch_now"), 17, Color.White, bold = true, maxLines = 1)
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                        }
                        Focusable(shape = RoundedCornerShape(26.dp), focusScale = 1.07f, onClick = { vm.openDetail(item) }) { f ->
                            Box(Modifier.clip(RoundedCornerShape(26.dp)).background(if (f) tc(0x66FFFFFF) else tc(0x33FFFFFF)).padding(horizontal = 20.dp, vertical = 8.dp)) {
                                RText(t("details"), 16, Color.White, bold = true, maxLines = 1)
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
                    }
                }
            }

            if (showSearch) {
                AyhamField(q, { q = it }, t("search_in_" + kind.name.lowercase()), Modifier.padding(horizontal = 44.dp, vertical = 4.dp).fillMaxWidth(0.45f), imeAction = androidx.compose.ui.text.input.ImeAction.Search)
            }

            // ---------- الأقسام ----------
            val fixed = listOf("recent" to t(if (kind == Kind.SERIES) "recent_series" else "recent_movies").substringBefore("(").trim(), "top" to (kindLabel + " ★"), "all" to t("all"))
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
                items(fixed.size, key = { "_" + fixed[it].first }) { i ->
                    val (k, label) = fixed[i]
                    BrowseChip(label, sel == k && !searching) { sel = k; q = ""; showSearch = false }
                }
                itemsIndexed(cats, key = { _, c -> c.id }) { _, c ->
                    BrowseChip(c.name, sel == c.id && !searching) { sel = c.id; q = ""; showSearch = false }
                }
            }

            // ---------- البوسترات ----------
            val posterH = (sH * (if (small) 0.27f else 0.31f)).coerceIn(110f, 230f).dp
            LazyRow(
                Modifier.fillMaxWidth().height(posterH + 22.dp),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(list, key = { _, it -> it.key }) { i, it ->
                    if (!searching && i >= list.size - 12) LaunchedEffect(list.size) { pager.loadMore() }
                    PosterCard(
                        it, Modifier.onFocusChanged { f -> if (f.hasFocus) selKey = it.key }.then(if (i == 0) Modifier.autoFocus() else Modifier),
                        w = posterH * 0.68f, h = posterH,
                    ) { vm.openDetail(it) }
                }
            }
        }
    }
}

@Composable
private fun BrowseChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Focusable(shape = RoundedCornerShape(20.dp), focusScale = 1.06f, onClick = onClick) { f ->
        Box(
            Modifier.clip(RoundedCornerShape(20.dp))
                .background(if (f) C.primary else if (selected) tc(0x887C3AED) else tc(0x44000000))
                .padding(horizontal = 16.dp, vertical = 5.dp),
        ) { RText(label, 14, Color.White, bold = selected || f, maxLines = 1) }
    }
}
