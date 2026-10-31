package com.ayham.player.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Details
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import kotlinx.coroutines.delay

/**
 * صف بأسلوب الشاشة الرئيسية: بوسترات طولية، والعنصر المحدد يتمدد لبطاقة عريضة،
 * وتحت الصف تفاصيله (نوع، سنة، تصنيف، تقييم، وصف).
 */
@Composable
fun FocusRow(title: String, items: List<Item>, vm: AppViewModel, onItem: (Item) -> Unit) {
    if (items.isEmpty()) return
    val compact = isCompact()
    val screenH = LocalConfiguration.current.screenHeightDp
    val rowH = if (compact) 190.dp else (screenH * 0.36f).dp
    val posterW = rowH * 0.66f
    val wideW = rowH * 1.78f
    val pad = if (compact) 14.dp else 32.dp
    val listState = rememberLazyListState()
    var focusIdx by remember { mutableIntStateOf(-1) }
    val focusItem = items.getOrNull(focusIdx)

    var details by remember { mutableStateOf<Details?>(null) }
    var imdb by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(focusItem?.key) {
        details = null
        imdb = null
        val it = focusItem ?: return@LaunchedEffect
        delay(250)
        val d = runCatching {
            if (it.kind == Kind.SERIES) vm.seriesInfo(it).details else vm.movieDetails(it)
        }.getOrNull()
        details = d
        if (d != null) imdb = vm.imdbRating(it, d.year)
    }
    LaunchedEffect(focusIdx) {
        if (focusIdx >= 0) runCatching { listState.scrollToItem(focusIdx) }
    }

    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        RText(title, if (compact) 19 else 26, C.text, bold = true, modifier = Modifier.padding(horizontal = pad, vertical = 6.dp), maxLines = 1)
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().height(rowH + 12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = pad, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.key }) { i, item ->
                val expanded = !compact && focusIdx == i
                val w by animateDpAsState(if (expanded) wideW else posterW, tween(200), label = "w")
                val shape = RoundedCornerShape(14.dp)
                Box(
                    Modifier.width(w).height(rowH).clip(shape).background(C.surface)
                        .then(if (expanded) Modifier.border(2.5.dp, Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFD946EF))), shape) else Modifier)
                        .onFocusChanged { f ->
                            if (f.isFocused) focusIdx = i else if (focusIdx == i) focusIdx = -1
                        }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onItem(item) },
                ) {
                    val bg = if (expanded) details?.backdrop.orEmpty().ifEmpty { item.image } else item.image
                    if (item.image.isBlank() && bg.isBlank()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RText(item.name.take(1), 44, C.surfaceHi, bold = true)
                        }
                    }
                    Img(bg, Modifier.fillMaxSize(), w = if (expanded) 640 else 240, h = if (expanded) 360 else 360)

                    // شارة التقييم على البوستر غير الممتد
                    val rating = item.rating.trim().takeIf { it.isNotBlank() && it != "0" && it != "0.0" }
                    if (!expanded && rating != null) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xDD0B0616))
                                .border(0.8.dp, Color(0x55F5C518), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                RText("★", 11, Color(0xFFF5C518), bold = true)
                                RText(rating, 11, Color.White, bold = true)
                            }
                        }
                    }

                    // شريط تقدم استكمال المشاهدة عند توفره
                    val prog = vm.progressOf(item.key)
                    if (prog != null && prog.second > 0L) {
                        val fraction = (prog.first.toFloat() / prog.second.toFloat()).coerceIn(0.04f, 1f)
                        Box(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp)
                                .background(Color(0x44FFFFFF))
                        ) {
                            Box(
                                Modifier.fillMaxHeight().fillMaxWidth(fraction)
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFFD946EF))))
                            )
                        }
                    }

                    if (expanded) {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(colorStops = arrayOf(0.45f to Color.Transparent, 1f to Color(0xEE0B0616))),
                            ),
                        )
                        RText(
                            item.name, 30, Color.White, bold = true, maxLines = 2, lineHeight = 36,
                            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
                        )
                    }
                }
            }
        }
        if (!compact) {
            Column(
                Modifier.fillMaxWidth().height(96.dp).padding(horizontal = pad, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val it = focusItem
                if (it != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(C.primary).padding(horizontal = 12.dp, vertical = 3.dp)) {
                            RText(t(if (it.kind == Kind.SERIES) "badge_series" else "badge_movie"), 15, Color.White, bold = true)
                        }
                        val d = details
                        d?.year?.takeIf { y -> y.isNotBlank() }?.let { y -> BodyText(y, 16, C.text, bold = true) }
                        d?.genre?.takeIf { g -> g.isNotBlank() }?.let { g ->
                            BodyText(g.split(",", "/", "·").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }.take(3).joinToString(" · "), 16, C.text, bold = true, maxLines = 1)
                        }
                        val r = imdb
                        if (r != null) ImdbBadge(r)
                        else (d?.rating?.takeIf { x -> x.isNotBlank() && x != "0" } ?: it.rating.takeIf { x -> x.isNotBlank() && x != "0" })?.let { x ->
                            RText("★ $x", 16, Color(0xFFF5C518), bold = true)
                        }
                    }
                    val plot = details?.plot.orEmpty()
                    if (plot.isNotBlank()) BodyText(plot, 15, C.textDim, maxLines = 2)
                }
            }
        }
    }
}
