package com.ayham.player.ui

import androidx.compose.foundation.background
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
    var details by remember(item.key) { mutableStateOf<Details?>(null) }
    var info by remember(item.key) { mutableStateOf<SeriesInfo?>(null) }
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

    var similarItems by remember(item.key) { mutableStateOf<List<Item>>(emptyList()) }
    LaunchedEffect(item.key) { similarItems = vm.similarTo(item).take(20) }
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
    Box(Modifier.fillMaxSize().background(C.bg)) {
        Img(bg, Modifier.fillMaxSize(), w = 960, h = 540)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Color(0xF20B0614), Color(0xE60B0614), Color(0xAA0B0614))),
            ),
        )

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
                        else RText(item.name, 38, C.text, bold = true, maxLines = 2, lineHeight = 46)
                        imdb?.let { ImdbBadge(it) }
                        val meta = listOfNotNull(
                            d?.year?.takeIf { it.isNotBlank() },
                            d?.duration?.takeIf { it.isNotBlank() },
                            (d?.rating?.takeIf { it.isNotBlank() && it != "0" } ?: item.rating.takeIf { it.isNotBlank() && it != "0" })
                                ?.let { "★ $it" },
                        ).joinToString("  •  ")
                        if (meta.isNotEmpty()) BodyText(meta, 14, C.primaryLight, bold = true)
                        val genre = d?.genre.orEmpty()
                        if (genre.isNotBlank()) BodyText("${t("genre")}: $genre", 13, C.textDim, maxLines = 1)
                        val plot = d?.plot.orEmpty()
                        if (plot.isNotBlank()) BodyText(plot, 14, C.text, maxLines = if (compact) 8 else 4)
                        val cast = d?.cast.orEmpty()
                        if (cast.isNotBlank()) BodyText("${t("cast")}: $cast", 12, C.textDim, maxLines = 2)
                        Spacer(Modifier.height(4.dp))
                        if (!compact) ActionButtons(vm, item, eps, curSeason)
                    }
                }
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            poster()
                            info(Modifier.weight(1f))
                        }
                        ActionButtons(vm, item, eps, curSeason)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                        poster()
                        info(Modifier.weight(1f))
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

            if (isSeries) {
                items(eps.size) { i ->
                    EpisodeRow(vm, item, eps, i)
                }
            }

            if (similarItems.size >= 3) {
                item { ContentRow(t("similar"), similarItems) { vm.openDetail(it) } }
            }
        }
    }
}

@Composable
private fun ActionButtons(vm: AppViewModel, item: Item, eps: List<Episode>, season: Int) {
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
            Modifier.fillMaxWidth().clip(shape).background(if (f) C.surfaceHi else Color(0xCC1C1033)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.width(if (isCompact()) 110.dp else 150.dp).height(if (isCompact()) 62.dp else 84.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x22FFFFFF))) {
                Img(ep.image.ifEmpty { series.image }, Modifier.fillMaxSize(), w = 320, h = 180)
                if (frac > 0f && !done) {
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x66000000))) {
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
