package com.ayham.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Details
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import kotlinx.coroutines.delay

private class HomeRow(val key: String, val label: String, val items: List<Item>, val style: RowStyle)

/**
 * الرئيسية السينمائية للتلفاز: بانر كبير بتفاصيل العمل المحدد في الأعلى، وتحته صفوف
 * (تابع المشاهدة، أفضل 10، جديد، مقترح...) تدخل بحركة ناعمة، والتركيز على أي بوستر يغيّر البانر.
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
    val small = sH < 560
    val T_continue_watching = t("continue_watching")
    val T_hall_movies = t("hall_movies")
    val T_hall_series = t("hall_series")
    val T_movies = t("movies")
    val T_my_list = t("my_list")
    val T_rec_movies = t("rec_movies")
    val T_rec_series = t("rec_series")
    val T_recent_movies = t("recent_movies")
    val T_recent_series = t("recent_series")
    val T_series = t("series")
    val T_trend_movies = t("trend_movies")
    val T_trend_series = t("trend_series")
    val T_watch_later = t("watch_later")
    val rows = remember(vm.recent, vm.favorites, vm.later, recMovies, recSeries, newMovies, newSeries, trendMovies, trendSeries, topMovies, topSeries, horrorMovies, horrorSeries) {
        val l = ArrayList<HomeRow>()
        fun add(k: String, label: String, items: List<Item>, style: RowStyle = RowStyle.PORTRAIT, min: Int = 1) {
            val d = items.filter { it.kind != Kind.LIVE }.distinctBy { it.key }
            if (d.size >= min) l.add(HomeRow(k, label, d, style))
        }
        add("cont", T_continue_watching, vm.recent, RowStyle.LAND)
        add("hm", T_hall_movies, horrorMovies, min = 3)
        add("hs", T_hall_series, horrorSeries, min = 3)
        val top = (topMovies.take(5) + topSeries.take(5)).sortedByDescending { it.rating.toFloatOrNull() ?: 0f }
        add("top10", if (vm.lang == "ar") "أفضل 10 اليوم" else "Top 10 today", top, RowStyle.RANK, 5)
        add("nm", T_movies + " · " + T_recent_movies.substringBefore("(").trim(), newMovies)
        add("ns", T_series + " · " + T_recent_series.substringBefore("(").trim(), newSeries)
        add("rm", T_rec_movies.substringBefore("(").trim(), recMovies, min = 4)
        add("rs", T_rec_series.substringBefore("(").trim(), recSeries, min = 4)
        add("tm", T_trend_movies, trendMovies, min = 3)
        add("ts", T_trend_series, trendSeries, min = 3)
        add("topm", T_movies + " ★", topMovies, min = 3)
        add("tops", T_series + " ★", topSeries, min = 3)
        add("fav", T_my_list, vm.favorites)
        add("later", T_watch_later, vm.later)
        l
    }

    if (rows.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(top = 96.dp).verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) { SetupCard(vm) }
        return
    }

    val first = rows.first().items.first()
    var item by remember { mutableStateOf(first) }
    var details by remember { mutableStateOf<Details?>(null) }
    var imdb by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(item.key) {
        delay(320)
        details = null; imdb = null
        val d = runCatching { if (item.kind == Kind.SERIES) vm.seriesInfo(item).details else vm.movieDetails(item) }.getOrNull()
        details = d
        if (d != null) imdb = runCatching { vm.imdbRating(item, d.year) }.getOrNull()
    }
    val ar = rememberArabicName(item)
    val rank = remember(item.key, topMovies, topSeries) {
        val i = (topMovies.take(10)).indexOfFirst { it.key == item.key }
        if (i in 0..9) i + 1 else 0
    }
    val posterH = (sH * (if (small) 0.25f else 0.27f)).coerceIn(105f, 210f).dp
    val rowsH = posterH + 66.dp + 34.dp

    Box(Modifier.fillMaxSize()) {
        CinemaBackdrop(details?.backdrop.orEmpty().ifEmpty { item.image })
        if (ThemeState.id == Themes.BARCELONA || vm.matchCard) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = if (small) 80.dp else 96.dp, end = 44.dp)) { NextMatchCard(vm, small) }
        }
        Column(Modifier.fillMaxSize().padding(top = if (small) 74.dp else 86.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 44.dp), contentAlignment = Alignment.BottomStart) {
                CinemaHeroText(vm, item, details, imdb, rank, small, ar, showTonight = true)
            }
            LazyColumn(Modifier.fillMaxWidth().height(rowsH), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                itemsIndexed(rows, key = { _, r -> r.key }) { i, r ->
                    CinemaRow(
                        r.label, r.items, r.style, posterH, 44.dp, order = i, first = i == 0,
                        onFocus = { item = it }, onOpen = { vm.openDetail(it) },
                    )
                }
            }
        }
    }
}
