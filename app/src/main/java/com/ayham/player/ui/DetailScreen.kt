package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Details
import com.ayham.player.data.Episode
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import com.ayham.player.data.SeriesInfo

@Composable
fun DetailScreen(vm: AppViewModel, item: Item) {
    val isSeries = item.kind == Kind.SERIES
    var details by remember(item.key) { mutableStateOf<Details?>(vm.cachedDetails(item)) }
    var info by remember(item.key) { mutableStateOf<SeriesInfo?>(vm.cachedSeries(item)) }
    var season by remember(item.key) { mutableIntStateOf(-1) }

    LaunchedEffect(item.key) {
        if (isSeries) {
            val si = vm.seriesInfo(item)
            info = si
            details = si.details
        } else {
            details = vm.movieDetails(item)
        }
    }

    var versions by remember(item.key) { mutableStateOf<List<Item>>(emptyList()) }
    var showVersions by remember(item.key) { mutableStateOf(false) }
    LaunchedEffect(item.key) { versions = vm.versionsOf(item) }

    var similarItems by remember(item.key) { mutableStateOf<List<Item>>(emptyList()) }
    LaunchedEffect(item.key) { similarItems = vm.similarTo(item).take(20) }
    LaunchedEffect(item.key, details != null) {
        if (details == null) return@LaunchedEffect
        val rec = vm.recommendFor(item, details?.year.orEmpty())
        if (rec.isNotEmpty()) similarItems = (rec + similarItems).distinctBy { it.key }.filter { it.key != item.key }.take(24)
    }
    var franchise by remember(item.key) { mutableStateOf<List<Pair<Int, Item>>>(emptyList()) }
    LaunchedEffect(item.key) { franchise = vm.franchiseOf(item) }
    var imdb by remember(item.key) { mutableStateOf<String?>(null) }
    val yearKey = details?.year.orEmpty()
    LaunchedEffect(item.key, details != null, yearKey) {
        if (details != null) imdb = vm.imdbRating(item, yearKey)
    }

    val seasons = info?.seasons ?: emptyMap()
    val curSeason = if (seasons.containsKey(season)) season else (seasons.keys.firstOrNull() ?: 0)
    val eps: List<Episode> = seasons[curSeason].orEmpty()
    val d = details
    val bg = d?.backdrop.orEmpty().ifEmpty { item.image }

    val compact = isCompact()
    var actorSel by remember { mutableStateOf<String?>(null) }
    actorSel?.let { ActorDialog(vm, it) { actorSel = null } }
    Box(Modifier.fillMaxSize().background(C.bg)) {
        val kb = androidx.compose.animation.core.rememberInfiniteTransition(label = "kb")
        val kz by kb.animateFloat(
            1f, 1.09f,
            androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(22_000, easing = androidx.compose.animation.core.LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
            label = "kz",
        )
        if (compact) Img(bg, Modifier.fillMaxSize().graphicsLayer { scaleX = kz; scaleY = kz }, w = 960, h = 540)
        else Box(Modifier.fillMaxWidth().fillMaxHeight(0.68f).clipToBounds()) { Img(bg, Modifier.fillMaxSize().graphicsLayer { scaleX = kz; scaleY = kz }, w = 1280, h = 720) }
        if (compact) Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(tc(0xF20B0614), tc(0xE60B0614), tc(0xAA0B0614))),
            ),
        ) else {
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(tc(0x800B0614), Color.Transparent, tc(0x800B0614)))))
            Box(Modifier.fillMaxWidth().fillMaxHeight(0.68f).background(Brush.verticalGradient(0.30f to Color.Transparent, 0.95f to C.bg)))
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = if (compact) 14.dp else 40.dp, vertical = if (compact) 14.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val poster: @Composable () -> Unit = {
                    Box(
                        Modifier.width(if (compact) 120.dp else 190.dp).height(if (compact) 180.dp else 285.dp)
                            .clip(RoundedCornerShape(14.dp)).background(C.surface),
                    ) {
                        Img(item.image, Modifier.fillMaxSize())
                    }
                }
                val info: @Composable (Modifier) -> Unit = { mod ->
                    Column(mod, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (compact) RText(item.name, 24, C.text, bold = true, maxLines = 3, lineHeight = 30)
                        else RText(item.name, 46, Color.White, bold = true, maxLines = 2, lineHeight = 54)
                        if (!compact) {
                            val arL = vm.lang == "ar"
                            val total = seasons.values.sumOf { it.size }
                            val parts = listOfNotNull(
                                d?.genre?.split(",", "،", "/")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() },
                                d?.year?.takeIf { it.isNotBlank() },
                                if (isSeries && seasons.isNotEmpty()) "${seasons.size} " + (if (arL) "موسم" else "seasons") else null,
                                if (isSeries && total > 0) "$total " + (if (arL) "حلقة" else "episodes") else d?.duration?.takeIf { it.isNotBlank() },
                            )
                            val plot0 = d?.plot.orEmpty()
                            if (plot0.isNotBlank()) BodyText(plot0, 16, C.text, maxLines = 2)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                parts.forEachIndexed { i, x ->
                                    if (i > 0) Box(Modifier.width(1.dp).height(16.dp).background(tc(0x55FFFFFF)))
                                    BodyText(x, 15, C.text, bold = true)
                                }
                                if (parts.isNotEmpty() && imdb != null) Box(Modifier.width(1.dp).height(16.dp).background(tc(0x55FFFFFF)))
                                imdb?.let { ImdbBadge(it) }
                            }
                        } else imdb?.let { ImdbBadge(it) }
                        val meta = if (!compact) "" else listOfNotNull(
                            d?.year?.takeIf { it.isNotBlank() },
                            d?.duration?.takeIf { it.isNotBlank() },
                            (d?.rating?.takeIf { it.isNotBlank() && it != "0" } ?: item.rating.takeIf { it.isNotBlank() && it != "0" })
                                ?.let { "★ $it" },
                        ).joinToString("  •  ")
                        if (meta.isNotEmpty()) BodyText(meta, 14, C.primaryLight, bold = true)
                        val genre = d?.genre.orEmpty()
                        if (genre.isNotBlank() && compact) BodyText("${t("genre")}: $genre", 13, C.textDim, maxLines = 1)
                        val plot = d?.plot.orEmpty()
                        if (plot.isNotBlank() && compact) BodyText(plot, 15, C.text, maxLines = 8)
                        val cast = d?.cast.orEmpty()
                        if (cast.isNotBlank() && compact) BodyText("${t("cast")}: $cast", 12, C.textDim, maxLines = 2)
                        Spacer(Modifier.height(4.dp))
                        if (!compact) ActionButtons(vm, item, eps, curSeason, versions) { showVersions = true }
                    }
                }
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            poster()
                            info(Modifier.weight(1f))
                        }
                        ActionButtons(vm, item, eps, curSeason, versions) { showVersions = true }
                    }
                } else {
                    // تلفاز: خلفية عريضة بأعلى الشاشة، وبوستر صغير بجانب العنوان والتفاصيل والأزرار
                    val heroH = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.72f).dp
                    Row(
                        Modifier.fillMaxWidth().height(heroH),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        info(Modifier.weight(1f))
                        poster()
                    }
                }
            }

            if (isSeries && seasons.size > 1) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(seasons.keys.toList()) { s ->
                            Chip("${t("season")} $s", s == curSeason) { season = s }
                        }
                    }
                }
            }

            if (isSeries && compact) {
                items(eps.size) { i ->
                    EpisodeRow(vm, item, eps, i)
                }
            } else if (isSeries) {
                item { RText((if (vm.lang == "ar") "الموسم " else "Season ") + curSeason + " - " + eps.size + (if (vm.lang == "ar") " حلقة" else " episodes"), 20, C.text, bold = true) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                        items(eps.size) { i -> EpisodeCard(vm, item, eps, i) }
                    }
                }
            }

            item { RatingsPanel(vm, item, details?.year.orEmpty(), imdb) }
            item {
                val names = d?.cast.orEmpty().split(",", "،").map { it.trim() }.filter { it.length >= 3 }.take(8)
                if (names.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RText(if (vm.lang == "ar") "الممثلون (اضغط لعرض أعماله)" else "Cast (tap for their works)", 16, C.textDim, bold = true)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(names) { n -> Chip(n, false) { actorSel = n } } }
                }
            }

            if (franchise.size >= 2) {
                item { ContentRow(if (vm.lang == "ar") "أجزاء السلسلة" else "The collection", franchise.map { it.second }) { vm.openDetail(it) } }
            }
            if (similarItems.size >= 3) {
                item { ContentRow(t("similar"), similarItems) { vm.openDetail(it) } }
            }
        }
        if (showVersions && versions.size > 1) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showVersions = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(Modifier.fillMaxSize().background(tc(0xCC000000)), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.fillMaxWidth(if (compact) 0.94f else 0.55f).fillMaxHeight(0.88f).clip(RoundedCornerShape(22.dp))
                            .background(tc(0xFF120A24)).padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RText(com.ayham.player.data.Versions.core(item.name).replaceFirstChar { it.uppercase() }, 24, C.text, bold = true, maxLines = 1)
                        BodyText(if (vm.lang == "ar") "اختر النسخة (الجودة / اللغة)" else "Choose a version (quality / language)", 14, C.textDim)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(versions.size) { n ->
                                val v = versions[n]
                                PanelItem(
                                    v.name,
                                    selected = v.key == item.key,
                                    sub = com.ayham.player.data.Versions.tag(v.name).ifBlank { null },
                                    modifier = if (v.key == item.key) Modifier.autoFocus() else Modifier,
                                ) {
                                    showVersions = false
                                    if (v.key != item.key) { vm.pop(); vm.openDetail(v) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(vm: AppViewModel, item: Item, eps: List<Episode>, season: Int, versions: List<Item>, onVersions: () -> Unit) {
    val isSeries = item.kind == Kind.SERIES
    val progress = if (isSeries) null else vm.progressOf(item.key)
    val canResume = progress != null && progress.first > 10_000 && progress.first < progress.second * 0.92

    // أول حلقة غير مكتملة في الموسم الحالي
    var startIdx = 0
    if (isSeries) {
        val partial = eps.indexOfFirst {
            val p = vm.progressOf(it.key)
            p != null && p.first > 10_000 && p.first < p.second * 0.92
        }
        startIdx = if (partial >= 0) partial else {
            val firstUnwatched = eps.indexOfFirst {
                val p = vm.progressOf(it.key)
                p == null || p.first < p.second * 0.92
            }
            if (firstUnwatched >= 0) firstUnwatched else 0
        }
    }
    val hasPlayable = !isSeries || eps.isNotEmpty()

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (hasPlayable) {
            item {
                GlowButton(
                    if (canResume || (isSeries && startIdx > 0)) t("resume") else t("play"),
                    primary = true,
                    modifier = Modifier.autoFocus(),
                ) {
                    if (isSeries) vm.playEpisodes(item, eps, startIdx) else vm.playMovie(item)
                }
            }
        }
        if (versions.size > 1) item {
            GlowButton((if (vm.lang == "ar") "اختيار النسخة" else "Versions") + " (${versions.size})") { onVersions() }
        }
        item {
            val on = vm.isIn("fav", item)
            GlowButton(if (on) t("remove_fav") else t("add_fav")) { vm.toggle("fav", item) }
        }
        item {
            val on = vm.isIn("later", item)
            GlowButton(if (on) t("remove_later") else t("add_later")) { vm.toggle("later", item) }
        }
        item {
            val on = vm.isIn("watched", item)
            GlowButton(if (on) t("unmark_watched") else t("mark_watched")) { vm.toggle("watched", item) }
        }
    }
}

@Composable
private fun EpisodeRow(vm: AppViewModel, series: Item, eps: List<Episode>, i: Int) {
    val ep = eps[i]
    val shape = RoundedCornerShape(12.dp)
    val prog = vm.progressOf(ep.key)
    val frac = if (prog != null && prog.second > 0) (prog.first.toFloat() / prog.second).coerceIn(0f, 1f) else 0f
    val done = frac > 0.92f
    Focusable(Modifier.fillMaxWidth(), shape, 1.02f, onClick = { vm.playEpisodes(series, eps, i) }) { f ->
        Row(
            Modifier.fillMaxWidth().clip(shape).background(if (f) C.surfaceHi else tc(0xCC1C1033)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.width(if (isCompact()) 110.dp else 150.dp).height(if (isCompact()) 62.dp else 84.dp).clip(RoundedCornerShape(8.dp)).background(tc(0x22FFFFFF))) {
                Img(ep.image.ifEmpty { series.image }, Modifier.fillMaxSize(), w = 320, h = 180)
                if (frac > 0f && !done) {
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(tc(0x66000000))) {
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(C.accent))
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                BodyText("${t("episode")} ${ep.num}  •  ${ep.title}", 15, C.text, bold = true, maxLines = 1)
                if (ep.plot.isNotBlank()) BodyText(ep.plot, 12, C.textDim, maxLines = 2)
                if (ep.duration.isNotBlank()) BodyText(ep.duration, 12, C.primaryLight, maxLines = 1)
            }
            if (done) Icon(Icons.Filled.CheckCircle, null, tint = C.ok, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
private fun EpisodeCard(vm: AppViewModel, series: Item, eps: List<Episode>, i: Int) {
    val ep = eps[i]
    val shape = RoundedCornerShape(14.dp)
    val prog = vm.progressOf(ep.key)
    val frac = if (prog != null && prog.second > 0) (prog.first.toFloat() / prog.second).coerceIn(0f, 1f) else 0f
    val done = frac > 0.92f
    Column(Modifier.width(270.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Focusable(Modifier.fillMaxWidth(), shape, 1.05f, onClick = { vm.playEpisodes(series, eps, i) }) { f ->
            Box(
                Modifier.fillMaxWidth().height(152.dp).clip(shape).background(C.surface)
                    .then(if (f) Modifier.border(2.dp, C.accent, shape) else Modifier),
            ) {
                Img(ep.image.ifEmpty { series.image }, Modifier.fillMaxSize(), w = 400, h = 225)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to tc(0x00000000), 1f to tc(0xCC000000))))
                BodyText("${t("episode")} ${ep.num}", 13, Color.White, bold = true, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp, 10.dp, 10.dp, 12.dp))
                if (done) Icon(Icons.Filled.CheckCircle, null, tint = C.ok, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp))
                if (frac > 0f && !done) {
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(tc(0x66000000))) {
                        Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(C.accent))
                    }
                }
            }
        }
        BodyText(ep.title.ifBlank { "${t("episode")} ${ep.num}" }, 14, C.text, bold = true, maxLines = 1)
        if (ep.duration.isNotBlank()) BodyText(ep.duration, 12, C.primaryLight, maxLines = 1)
    }
}
