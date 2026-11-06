package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import com.ayham.player.data.RatingsData

private data class RCard(val name: String, val color: Color, val value: String?, val sub: String, val url: String?)

private fun votes(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}

/** تقييمات متعددة: كل مصدر مستقل، ويظهر "غير متوفر" عند غياب بياناته. */
@Composable
fun RatingsPanel(vm: AppViewModel, item: Item, year: String, imdbLocal: String?) {
    val ar = vm.lang == "ar"
    val ctx = LocalContext.current
    var data by remember(item.key) { mutableStateOf<RatingsData?>(null) }
    var done by remember(item.key) { mutableStateOf(false) }
    LaunchedEffect(item.key, year) {
        if (year.isEmpty() && item.kind == Kind.SERIES) { /* ننتظر التفاصيل */ }
        data = vm.ratingsOf(item, year)
        done = true
    }
    if (!done) return
    val d = data
    val na = if (ar) "غير متوفر" else "N/A"
    val critics = if (ar) "نقاد" else "Critics"
    val users = if (ar) "جمهور" else "Users"
    val tv = item.kind == Kind.SERIES
    val imdbId = d?.imdbId.orEmpty()
    val imdbVal = imdbLocal ?: d?.imdb?.first?.let { String.format("%.1f", it) }
    val cards = listOf(
        RCard("IMDb", Color(0xFFF5C518), imdbVal?.let { "$it/10" }, listOfNotNull(users, d?.imdb?.second?.takeIf { it > 0 }?.let { votes(it) }).joinToString(" · "),
            imdbId.takeIf { it.isNotEmpty() }?.let { "https://www.imdb.com/title/$it/" }),
        RCard("Rotten Tomatoes", Color(0xFFFA320A), d?.rt?.let { "$it%" }, critics, null),
        RCard("Rotten Tomatoes", Color(0xFFFA320A), d?.rta?.let { "$it%" }, users, null),
        RCard("Metacritic", Color(0xFF66CC33), d?.mc?.let { "$it/100" }, critics, null),
        RCard("TMDB", Color(0xFF01B4E4), d?.tmdb?.let { String.format("%.1f/10", it.first) }, listOfNotNull(users, d?.tmdb?.second?.let { votes(it) }).joinToString(" · "),
            d?.tmdbId?.takeIf { it > 0 }?.let { "https://www.themoviedb.org/${if (tv) "tv" else "movie"}/$it" }),
        RCard("Trakt", Color(0xFFED1C24), d?.trakt?.let { String.format("%.1f/10", it.first) }, listOfNotNull(users, d?.trakt?.second?.let { votes(it) }).joinToString(" · "),
            imdbId.takeIf { it.isNotEmpty() }?.let { "https://trakt.tv/search/imdb/$it" }),
        RCard("Letterboxd", Color(0xFF00E054), d?.lb?.let { String.format("%.1f/5", it.first) }, listOfNotNull(users, d?.lb?.second?.takeIf { it > 0 }?.let { votes(it) }).joinToString(" · "), null),
    ).filter { it.value != null || it.name !in listOf("Letterboxd") && !(it.name == "Rotten Tomatoes" && it.sub == users) }
    if (cards.none { it.value != null }) return
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RText(if (ar) "التقييمات" else "Ratings", 20, C.text, bold = true, modifier = Modifier.padding(horizontal = if (isCompact()) 16.dp else 44.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = if (isCompact()) 16.dp else 44.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cards) { c ->
                val shape = RoundedCornerShape(14.dp)
                Focusable(Modifier.width(150.dp), shape, 1.05f, onClick = {
                    c.url?.let { u -> runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(u)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                }) { f ->
                    Column(
                        Modifier.clip(shape).background(if (f) C.surfaceHi else C.surface).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.Start,
                    ) {
                        RText(c.name, 14, c.color, bold = true, maxLines = 1)
                        RText(c.value ?: na, if (c.value != null) 26 else 16, if (c.value != null) Color.White else C.textDim, bold = true, maxLines = 1)
                        BodyText(if (c.value != null) c.sub else "", 12, C.textDim, maxLines = 1)
                    }
                }
            }
        }
    }
}
