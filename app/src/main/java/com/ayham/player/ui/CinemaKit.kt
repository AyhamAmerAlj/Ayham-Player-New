package com.ayham.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Details
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import kotlinx.coroutines.delay

/** مكوّنات الواجهة السينمائية الجديدة (النسخة الأصلية فقط). */
enum class RowStyle { PORTRAIT, LAND, RANK }

/** عنوان صف: شريط لوني صغير + عنوان بخط عريض. */
@Composable
fun CinemaTitle(title: String, hpad: Dp) {
    Row(Modifier.padding(start = hpad, end = hpad, top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(3.dp)).background(Brush.verticalGradient(listOf(tc(0xFFD946EF), tc(0xFF7C3AED)))))
        RText(title, 20, Color.White, bold = true, maxLines = 1)
    }
}

/** بطاقة «تابع المشاهدة» الأفقية: بوستر صغير + العنوان + شريط التقدم. */
@Composable
fun LandCard(item: Item, h: Dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val prog = LocalProgress.current(item.key)
    Focusable(modifier.width(h * 2.3f).height(h), shape, 1.08f, onClick = onClick) { f ->
        Row(
            Modifier.fillMaxSize().clip(shape).background(if (f) tc(0xFF2A1E4A) else tc(0xFF150E27))
                .border(if (f) 2.dp else 1.dp, if (f) C.accent else tc(0x22FFFFFF), shape),
        ) {
            Box(Modifier.width(h * 0.68f).fillMaxHeight()) {
                Img(item.image, Modifier.fillMaxSize(), w = 300, h = 450)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Transparent, tc(0x66150E27)))))
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(34.dp).background(tc(0x88000000), RoundedCornerShape(50)).padding(4.dp))
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BodyText(item.name, 14, Color.White, bold = true, maxLines = 3)
                    BodyText(t(if (item.kind == Kind.SERIES) "series" else "movies"), 11, C.textDim, maxLines = 1)
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (prog != null && prog > 0.02f) {
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(tc(0x33FFFFFF))) {
                            Box(Modifier.fillMaxWidth(prog.coerceIn(0f, 1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(tc(0xFF8B5CF6), tc(0xFFD946EF)))))
                        }
                    }
                    BodyText(if (LocalLang.current == "ar") "متابعة" else "Resume", 12, C.accent, bold = true, maxLines = 1)
                }
            }
        }
    }
}

/** صف سينمائي: يدخل بحركة تلاشٍ وصعود، وبوسترات تكبر عند التركيز. */
@Composable
fun CinemaRow(
    title: String, items: List<Item>, style: RowStyle, posterH: Dp, hpad: Dp,
    order: Int = 0, first: Boolean = false,
    onFocus: (Item) -> Unit = {}, onIndex: (Int) -> Unit = {}, onOpen: (Item) -> Unit,
) {
    if (items.isEmpty()) return
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(70L * order.coerceAtMost(5)); a.animateTo(1f, tween(420)) }
    Column(Modifier.fillMaxWidth().graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 36f }) {
        CinemaTitle(title, hpad)
        val rowH = if (style == RowStyle.LAND) posterH * 0.78f else posterH
        LazyRow(
            Modifier.fillMaxWidth().height(rowH + 26.dp),
            contentPadding = PaddingValues(horizontal = hpad - 4.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (style == RowStyle.RANK) 4.dp else 14.dp),
        ) {
            itemsIndexed(if (style == RowStyle.RANK) items.take(10) else items, key = { _, it -> style.name + it.key }) { i, it ->
                if (style != RowStyle.RANK && i >= items.size - 12) LaunchedEffect(items.size) { onIndex(i) }
                val fm = Modifier.onFocusChanged { f -> if (f.hasFocus) onFocus(it) }.then(if (first && i == 0) Modifier.autoFocus() else Modifier)
                when (style) {
                    RowStyle.LAND -> LandCard(it, rowH, fm) { onOpen(it) }
                    RowStyle.PORTRAIT -> PosterCard(it, fm, w = posterH * 0.68f, h = posterH, focusScale = 1.12f) { onOpen(it) }
                    RowStyle.RANK -> Row(verticalAlignment = Alignment.Bottom) {
                        androidx.compose.material3.Text(
                            text = "${i + 1}",
                            modifier = Modifier.offset(x = if (i == 9) 30.dp else 22.dp, y = 8.dp),
                            style = TextStyle(
                                fontFamily = AppFont, fontSize = (posterH.value * 0.78f).sp, fontWeight = FontWeight.Black,
                                color = tc(0x99C9B8F5), drawStyle = Stroke(width = 6f),
                            ),
                            maxLines = 1,
                        )
                        PosterCard(it, fm, w = posterH * 0.68f, h = posterH, focusScale = 1.12f) { onOpen(it) }
                    }
                }
            }
        }
    }
}

/** خلفية سينمائية: صورة تتبدل بتلاشٍ ناعم مع تقريب بطيء، وتدرجات تُبرز النص. */
@Composable
fun CinemaBackdrop(url: String) {
    Box(Modifier.fillMaxSize()) {
        androidx.compose.animation.Crossfade(targetState = url, modifier = Modifier.fillMaxSize(), animationSpec = tween(650), label = "cbg") { u ->
            val z = remember { Animatable(1f) }
            LaunchedEffect(u) { z.snapTo(1f); delay(1800); if (!Perf.on) z.animateTo(1.08f, tween(18000, easing = androidx.compose.animation.core.LinearEasing)) }
            Img(u, Modifier.fillMaxSize().graphicsLayer { scaleX = z.value; scaleY = z.value }, w = 1280, h = 720)
        }
        // تدرجات: جهة النص أغمق، وأسفل الشاشة يذوب في الأسود
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(tc(0xE60B0614), tc(0x880B0614), tc(0x110B0614), tc(0x440B0614)))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to tc(0xAA0B0614), 0.28f to tc(0x000B0614), 0.52f to tc(0xCC05020B), 0.78f to tc(0xFA05020B), 1f to tc(0xFF05020B))))
        ThemeState.palette.glow?.let { g ->
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(g.copy(alpha = 0.28f), Color.Transparent), center = Offset(0f, 0f), radius = 1100f)))
        }
    }
}

@Composable
private fun Pill(text: String) {
    Box(Modifier.clip(RoundedCornerShape(50)).border(1.dp, tc(0x55FFFFFF), RoundedCornerShape(50)).background(tc(0x33000000)).padding(horizontal = 12.dp, vertical = 3.dp)) {
        BodyText(text, 13, tc(0xFFEFE8FF), bold = true, maxLines = 1)
    }
}

/** كتلة التفاصيل في البانر الكبير: شارات، عنوان بظل، معلومات، قصة، وأزرار. تتبدل بتلاشٍ وانزلاق. */
@Composable
fun CinemaHeroText(
    vm: AppViewModel, item: Item, details: Details?, imdb: String?, rank: Int, small: Boolean, ar: String?, showTonight: Boolean = false,
    firstAction: Modifier = Modifier,
) {
    val sH = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val arabic = vm.lang == "ar"
    AnimatedContent(
        targetState = item,
        transitionSpec = { (fadeIn(tween(380)) + slideInVertically(tween(380)) { it / 12 }) togetherWith fadeOut(tween(160)) },
        label = "chero",
    ) { it0 ->
        val d = if (it0.key == item.key) details else null
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (small) 4.dp else 9.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Brush.horizontalGradient(listOf(tc(0xFF7C3AED), tc(0xFFD946EF)))).padding(horizontal = 12.dp, vertical = 2.dp)) {
                    RText(t(if (it0.kind == Kind.SERIES) "series" else "movies"), 13, Color.White, bold = true, maxLines = 1)
                }
                if (rank > 0) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Brush.horizontalGradient(listOf(tc(0xFFF59E0B), tc(0xFFEF4444)))).padding(horizontal = 10.dp, vertical = 2.dp)) {
                    RText(if (arabic) "🔥 رقم $rank اليوم" else "🔥 #$rank today", 13, Color.White, bold = true, maxLines = 1)
                }
                (if (it0.key == item.key) imdb else null)?.let { ImdbBadge(it) }
                    ?: (d?.rating?.takeIf { it.isNotBlank() && it != "0" } ?: it0.rating.takeIf { it.isNotBlank() && it != "0" })?.let { RText("★ $it", 16, tc(0xFFF5C518), bold = true, maxLines = 1) }
            }
            androidx.compose.material3.Text(
                text = ar ?: it0.name, maxLines = if (small) 1 else 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.62f),
                style = TextStyle(
                    fontFamily = AppFont, fontSize = if (small) 30.sp else 44.sp, fontWeight = FontWeight.Black, color = Color.White,
                    lineHeight = if (small) 36.sp else 52.sp, shadow = Shadow(Color.Black, Offset(0f, 6f), 22f),
                ),
            )
            if (!small) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    d?.year?.takeIf { it.isNotBlank() }?.let { Pill(it) }
                    d?.genre?.takeIf { it.isNotBlank() }?.split(",", "/")?.map { it.trim() }?.filter { it.isNotBlank() }?.take(3)?.forEach { Pill(it) }
                    d?.duration?.takeIf { it.isNotBlank() }?.let { Pill(it) }
                }
                val plot = d?.plot.orEmpty()
                if (plot.isNotBlank() && sH >= 500) BodyText(plot, 15, tc(0xFFE5DEF5), maxLines = 3, modifier = Modifier.fillMaxWidth(0.48f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Focusable(firstAction, RoundedCornerShape(28.dp), 1.08f, onClick = { if (it0.kind == Kind.MOVIE) vm.playMovie(it0) else vm.openDetail(it0) }) { f ->
                    Row(
                        Modifier.clip(RoundedCornerShape(28.dp))
                            .background(if (f) Color.White else tc(0xFFF1EAFF)).padding(horizontal = 26.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = tc(0xFF1A0B33), modifier = Modifier.size(24.dp))
                        RText(t("watch_now"), 17, tc(0xFF1A0B33), bold = true, maxLines = 1)
                    }
                }
                Focusable(Modifier, RoundedCornerShape(28.dp), 1.08f, onClick = { vm.openDetail(it0) }) { f ->
                    Row(
                        Modifier.clip(RoundedCornerShape(28.dp)).background(if (f) tc(0x88FFFFFF) else tc(0x55FFFFFF)).padding(horizontal = 20.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        RText(t("details"), 16, Color.White, bold = true, maxLines = 1)
                    }
                }
                val inFav = vm.isIn("fav", it0)
                Focusable(Modifier, RoundedCornerShape(28.dp), 1.08f, onClick = { vm.toggle("fav", it0) }) { f ->
                    Row(
                        Modifier.clip(RoundedCornerShape(28.dp)).background(if (f) tc(0x88FFFFFF) else tc(0x55FFFFFF)).padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(if (inFav) Icons.Filled.Check else Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        RText(t("my_list"), 15, Color.White, bold = true, maxLines = 1)
                    }
                }
                if (showTonight) Focusable(Modifier, RoundedCornerShape(28.dp), 1.08f, onClick = { vm.showTonight = true }) { f ->
                    Row(
                        Modifier.clip(RoundedCornerShape(28.dp)).background(if (f) tc(0x88FFFFFF) else tc(0x55FFFFFF)).padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RText("🎬", 15, Color.White)
                        RText(if (arabic) "الليلة" else "Tonight", 15, Color.White, bold = true, maxLines = 1)
                    }
                }
            }
        }
    }
}
