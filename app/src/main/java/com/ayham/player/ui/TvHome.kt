package com.ayham.player.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
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

private class HomeCat(val key: String, val label: String, val items: List<Item>)

/**
 * الشاشة الرئيسية للتلفاز (أفلام ومسلسلات فقط): صفحة واحدة لا تتمرر، كل أجزائها ضمن الشاشة.
 * خلفية سينمائية تتبدل مع العنوان المحدد، تحتها أقسام (شرائح) وصف بوسترات.
 * الانتقال بين الشرائح يبدّل الصف فورًا، والتركيز على أي بوستر يحدّث الخلفية والتفاصيل.
 */
@Composable
fun TvHome(
    vm: AppViewModel,
    recMovies: List<Item>, recSeries: List<Item>,
    newMovies: List<Item>, newSeries: List<Item>,
    trendMovies: List<Item>, trendSeries: List<Item>,
    topMovies: List<Item>, topSeries: List<Item>,
    horrorMovies: List<Item>, horrorSeries: List<Item>,
) {
    val sH = LocalConfiguration.current.screenHeightDp
    val small = sH < 440
    val L0 = t("continue_watching")
    val L1 = t("hall_movies")
    val L2 = t("hall_series")
    val L3 = t("movies")
    val L4 = t("my_list")
    val L5 = t("rec_movies").substringBefore("(").trim()
    val L6 = t("rec_series").substringBefore("(").trim()
    val L7 = t("recent_movies").substringBefore("(").trim()
    val L8 = t("recent_series").substringBefore("(").trim()
    val L9 = t("series")
    val L10 = t("trend_movies")
    val L11 = t("trend_series")
    val L12 = t("watch_later")
    val cats = remember(vm.recent, vm.favorites, vm.later, recMovies, recSeries, newMovies, newSeries, trendMovies, trendSeries, topMovies, topSeries, horrorMovies, horrorSeries) {
        val l = ArrayList<HomeCat>()
        fun add(k: String, label: String, items: List<Item>, min: Int = 1) {
            val d = items.filter { it.kind != Kind.LIVE }.distinctBy { it.key }
            if (d.size >= min) l.add(HomeCat(k, label, d))
        }
        add("cont", L0, vm.recent)
        add("hm", L1, horrorMovies, 3)
        add("hs", L2, horrorSeries, 3)
        add("nm", L3 + " · " + L7, newMovies)
        add("ns", L9 + " · " + L8, newSeries)
        add("rm", L5, recMovies, 4)
        add("rs", L6, recSeries, 4)
        add("tm", L10, trendMovies, 3)
        add("ts", L11, trendSeries, 3)
        add("topm", L3 + " ★", topMovies, 3)
        add("tops", L9 + " ★", topSeries, 3)
        add("fav", L4, vm.favorites)
        add("later", L12, vm.later)
        l
    }

    if (cats.none { it.items.isNotEmpty() }) {
        Box(Modifier.fillMaxSize().padding(top = 96.dp).verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) { SetupCard(vm) }
        return
    }

    var catIdx by remember { mutableIntStateOf(0) }
    val cat = cats[catIdx.coerceIn(0, cats.lastIndex)]
    var selKey by remember(cat.key) { mutableStateOf<String?>(null) }
    // قسم GTA ليس فيه عناصر، فيبقى البانر على أول قسم فيه عناصر
    val heroCat = if (cat.items.isNotEmpty()) cat else cats.first { it.items.isNotEmpty() }
    val item = heroCat.items.firstOrNull { it.key == selKey } ?: heroCat.items.first()

    var details by remember { mutableStateOf<Details?>(null) }
    var imdb by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(item.key) {
        delay(300)
        details = null; imdb = null
        val d = runCatching { if (item.kind == Kind.SERIES) vm.seriesInfo(item).details else vm.movieDetails(item) }.getOrNull()
        details = d
        if (d != null) imdb = runCatching { vm.imdbRating(item, d.year) }.getOrNull()
    }
    val ar = rememberArabicName(item)

    Box(Modifier.fillMaxSize()) {
        val bg = details?.backdrop.orEmpty().ifEmpty { item.image }
        Crossfade(targetState = bg, modifier = Modifier.fillMaxSize(), animationSpec = tween(500), label = "tvhomeBg") { url ->
            // بعد 2.5 ث من الوقوف على العمل تبدأ الخلفية بتقريب بطيء (مثل مقدمة العرض)
            val z = remember { androidx.compose.animation.core.Animatable(1f) }
            LaunchedEffect(url) { z.snapTo(1f); delay(2500); z.animateTo(1.1f, tween(16000, easing = androidx.compose.animation.core.LinearEasing)) }
            Img(url, Modifier.fillMaxSize().graphicsLayer { scaleX = z.value; scaleY = z.value }, w = 1280, h = 720)
        }
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(tc(0x990B0614), tc(0x220B0614), tc(0x990B0614)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to tc(0xAA0B0614), 0.3f to tc(0x000B0614), 0.5f to tc(0xBB05020B), 1f to tc(0xFF05020B))))

        // توهج خفيف بألوان الثيم في زاويتين (برشلونة فقط)
        ThemeState.palette.glow?.let { g ->
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(g.copy(alpha = 0.30f), Color.Transparent), center = Offset(0f, 0f), radius = 1100f)))
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(C.primary.copy(alpha = 0.30f), Color.Transparent), center = Offset(2600f, 1500f), radius = 1300f)))
        }
        if (ThemeState.id == Themes.BARCELONA || vm.matchCard) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = if (small) 80.dp else 96.dp, end = 44.dp)) { NextMatchCard(vm, small) }
        }
        Column(Modifier.fillMaxSize().padding(top = if (small) 76.dp else 88.dp, bottom = 10.dp)) {
            // ---------- التفاصيل ----------
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 44.dp),
                verticalArrangement = Arrangement.spacedBy(if (small) 4.dp else 8.dp, Alignment.Bottom),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(tc(0xFF6D28D9)).padding(horizontal = 12.dp, vertical = 2.dp)) {
                        RText(t(if (item.kind == Kind.SERIES) "series" else "movies"), 13, Color.White, bold = true, maxLines = 1)
                    }
                    imdb?.let { ImdbBadge(it) } ?: (details?.rating?.takeIf { it.isNotBlank() && it != "0" } ?: item.rating.takeIf { it.isNotBlank() && it != "0" })
                        ?.let { RText("★ $it", 16, tc(0xFFF5C518), bold = true, maxLines = 1) }
                }
                RText(ar ?: item.name, if (small) 28 else 42, Color.White, bold = true, maxLines = if (small) 1 else 2, lineHeight = if (small) 34 else 50,
                    modifier = Modifier.fillMaxWidth(0.62f))
                val meta = listOfNotNull(
                    details?.year?.takeIf { it.isNotBlank() },
                    details?.genre?.takeIf { it.isNotBlank() }?.split(",", "/")?.take(2)?.joinToString(" · ") { it.trim() },
                    details?.duration?.takeIf { it.isNotBlank() },
                ).joinToString("  •  ")
                if (meta.isNotEmpty() && !small) BodyText(meta, 15, tc(0xFFD9CCF5), bold = true, maxLines = 1)
                val plot = details?.plot.orEmpty()
                if (plot.isNotBlank() && sH >= 500) BodyText(plot, 15, tc(0xFFE5DEF5), maxLines = 2, modifier = Modifier.fillMaxWidth(0.5f))
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
                    Focusable(shape = RoundedCornerShape(26.dp), focusScale = 1.07f, onClick = { vm.showTonight = true }) { f ->
                        Row(
                            Modifier.clip(RoundedCornerShape(26.dp)).background(if (f) tc(0x558B5CF6) else tc(0x33FFFFFF)).padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            RText("🎬", 15, Color.White)
                            RText(if (vm.lang == "ar") "الليلة" else "Tonight", 15, Color.White, bold = true, maxLines = 1)
                        }
                    }
                }
            }

            // ---------- الأقسام (الانتقال بينها يبدّل الصف فورًا) ----------
            LazyRow(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(cats, key = { _, c -> c.key }) { i, c ->
                    val sel = i == catIdx
                    Focusable(Modifier.onFocusChanged { if (it.hasFocus) catIdx = i }, RoundedCornerShape(20.dp), 1.06f, onClick = { catIdx = i }) { f ->
                        Box(
                            Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (f) C.primary else if (sel) tc(0x887C3AED) else tc(0x44000000))
                                .padding(horizontal = 16.dp, vertical = 5.dp),
                        ) { RText(c.label, 14, Color.White, bold = sel || f, maxLines = 1) }
                    }
                }
            }

            // ---------- البوسترات ----------
            val posterH = (sH * (if (small) 0.27f else 0.31f)).coerceIn(110f, 230f).dp
            LazyRow(
                Modifier.fillMaxWidth().height(posterH + 22.dp),
                contentPadding = PaddingValues(horizontal = 40.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(cat.items, key = { _, it -> it.key }) { _, it ->
                    PosterCard(
                        it, Modifier.onFocusChanged { f -> if (f.hasFocus) selKey = it.key },
                        w = posterH * 0.68f, h = posterH,
                    ) { vm.openDetail(it) }
                }
            }
        }
    }
}

