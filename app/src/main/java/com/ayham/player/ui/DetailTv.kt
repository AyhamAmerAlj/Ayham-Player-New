package com.ayham.player.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import coil.request.ImageRequest
import com.ayham.player.AppViewModel
import com.ayham.player.data.Details
import com.ayham.player.data.Episode
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import com.ayham.player.data.SeriesInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** يستخرج اللون الغالب من البوستر (يميل للألوان الزاهية) ويتحرك بنعومة عند تغيّره. */
@Composable
fun rememberPosterColor(url: String?, fallback: Color = C.primary): Color {
    val ctx = LocalContext.current
    var c by remember(url) { mutableStateOf(fallback) }
    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        val r = withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(ctx).data(url).size(48, 72).allowHardware(false).build()
                val bmp = (ctx.imageLoader.execute(req).drawable as? BitmapDrawable)?.bitmap ?: return@runCatching null
                dominantColor(bmp)
            }.getOrNull()
        }
        if (r != null) c = r
    }
    return animateColorAsState(c, tween(800), label = "posterColor").value
}

private fun dominantColor(bmp: android.graphics.Bitmap): Color? {
    val w = bmp.width; val h = bmp.height
    if (w <= 0 || h <= 0) return null
    val px = IntArray(w * h)
    bmp.getPixels(px, 0, w, 0, 0, w, h)
    val hsv = FloatArray(3)
    var sx = 0.0; var sy = 0.0; var tw = 0.0
    for (p in px) {
        android.graphics.Color.colorToHSV(p, hsv)
        val wt = (hsv[1] * hsv[1] * hsv[2]).toDouble() + 0.0005
        val rad = Math.toRadians(hsv[0].toDouble())
        sx += Math.cos(rad) * wt; sy += Math.sin(rad) * wt; tw += wt
    }
    if (tw < 1.0) return null // بوستر رمادي: نبقى على لون الثيم
    val hue = ((Math.toDegrees(Math.atan2(sy, sx)) + 360) % 360).toFloat()
    var satSum = 0.0; var n = 0
    for (p in px) { android.graphics.Color.colorToHSV(p, hsv); if (hsv[1] > 0.25f && hsv[2] > 0.2f) { satSum += hsv[1]; n++ } }
    val sat = if (n > 0) (satSum / n).toFloat() else 0.6f
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat.coerceIn(0.55f, 0.9f), 0.88f)))
}

@Composable
private fun TvBtn(
    icon: ImageVector, label: String, accent: Color, primary: Boolean = false,
    modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Focusable(modifier, shape, 1.08f, onClick = onClick) { f ->
        Row(
            Modifier.animateContentSize(tween(180)).height(46.dp).clip(shape)
                .background(
                    when {
                        primary && f -> Brush.horizontalGradient(listOf(Color.White, Color.White))
                        primary -> Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.78f)))
                        f -> Brush.horizontalGradient(listOf(tc(0x66FFFFFF), tc(0x55FFFFFF)))
                        else -> Brush.horizontalGradient(listOf(tc(0x44000000), tc(0x44000000)))
                    },
                )
                .border(if (f) 2.dp else 1.dp, if (f) Color.White else if (primary) Color.Transparent else tc(0x44FFFFFF), shape)
                .padding(horizontal = if (primary) 22.dp else 13.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, null, tint = if (primary && f) tc(0xFF16092E) else Color.White, modifier = Modifier.size(if (primary) 26.dp else 22.dp))
            if (primary || f) RText(label, 16, if (primary && f) tc(0xFF16092E) else Color.White, bold = true, maxLines = 1)
        }
    }
}

@Composable
private fun InfoPill(text: String, accent: Color) {
    Box(Modifier.clip(RoundedCornerShape(50)).border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(50)).background(tc(0x33000000)).padding(horizontal = 12.dp, vertical = 3.dp)) {
        BodyText(text, 13, tc(0xFFF1EAFF), bold = true, maxLines = 1)
    }
}

/** تصميم صفحة الفيلم/المسلسل للشاشات: هيرو ثابت (بوستر كامل + معلومات) وتحته قوائم تتمرر، وكل الألوان من البوستر. */
@Composable
fun TvDetailBody(
    vm: AppViewModel, item: Item, d: Details?, imdb: String?, info: SeriesInfo?,
    seasons: Map<Int, List<Episode>>, curSeason: Int, eps: List<Episode>, versions: List<Item>,
    similarItems: List<Item>, franchise: List<Pair<Int, Item>>,
    onSeason: (Int) -> Unit, onVersions: () -> Unit, onActor: (String) -> Unit,
) {
    val isSeries = item.kind == Kind.SERIES
    val arL = vm.lang == "ar"
    val sH = LocalConfiguration.current.screenHeightDp
    val accent = rememberPosterColor(item.image)
    val base = tc(0xFF07040E)
    val bg = d?.backdrop.orEmpty().ifEmpty { item.image }
    val heroH = (sH * 0.58f).dp
    val kz = rememberInfiniteTransition(label = "kb").animateFloat(
        1f, 1.07f, infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse), label = "kz",
    ).value

    Box(Modifier.fillMaxSize().background(base)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.8f).clipToBounds()) {
            Img(bg, Modifier.fillMaxSize().graphicsLayer { scaleX = kz; scaleY = kz }, w = 1280, h = 720)
        }
        // طبقات اللون: ظلام من جهة النص، توهج من لون البوستر، وذوبان نحو الأسفل
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(tc(0xCC07040E), tc(0x6607040E), tc(0xCC07040E)))))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(alpha = 0.38f), Color.Transparent), center = Offset(1500f, 300f), radius = 1300f)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to tc(0x6607040E), 0.3f to Color.Transparent, 0.62f to base.copy(alpha = 0.92f), 1f to base)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to accent.copy(alpha = 0.16f))))

        Column(Modifier.fillMaxSize().padding(top = 26.dp)) {
            // ---------------- الهيرو (ثابت لا يتمرر) ----------------
            Row(
                Modifier.fillMaxWidth().height(heroH).padding(horizontal = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp), verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.Bottom)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.7f)))).padding(horizontal = 12.dp, vertical = 2.dp)) {
                            RText(t(if (isSeries) "series" else "movies"), 13, Color.White, bold = true, maxLines = 1)
                        }
                        imdb?.let { ImdbBadge(it) }
                        d?.year?.takeIf { it.isNotBlank() }?.let { InfoPill(it, accent) }
                        d?.genre?.split(",", "،", "/")?.map { it.trim() }?.filter { it.isNotBlank() }?.take(2)?.forEach { InfoPill(it, accent) }
                        if (isSeries && seasons.isNotEmpty()) InfoPill("${seasons.size} " + (if (arL) "موسم" else "seasons") + " · ${seasons.values.sumOf { it.size }} " + (if (arL) "حلقة" else "ep"), accent)
                        else d?.duration?.takeIf { it.isNotBlank() }?.let { InfoPill(it, accent) }
                    }
                    androidx.compose.material3.Text(
                        text = item.name, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            fontFamily = AppFont, fontSize = if (sH < 560) 32.sp else 42.sp, fontWeight = FontWeight.Black, color = Color.White,
                            lineHeight = if (sH < 560) 38.sp else 50.sp, shadow = Shadow(Color.Black, Offset(0f, 6f), 22f),
                        ),
                    )
                    val plot = d?.plot.orEmpty()
                    if (plot.isNotBlank()) BodyText(plot, 15, tc(0xFFE9E2F8), maxLines = if (sH < 560) 2 else 3, modifier = Modifier.fillMaxWidth(0.92f))
                    // أزرار الإجراء: الأول بلون البوستر، والباقي أيقونات تتوسع بالاسم عند التركيز
                    val progress = if (isSeries) null else vm.progressOf(item.key)
                    val canResume = progress != null && progress.first > 10_000 && progress.first < progress.second * 0.92
                    var startIdx = 0
                    if (isSeries) {
                        val partial = eps.indexOfFirst { val p = vm.progressOf(it.key); p != null && p.first > 10_000 && p.first < p.second * 0.92 }
                        startIdx = if (partial >= 0) partial else eps.indexOfFirst { val p = vm.progressOf(it.key); p == null || p.first < p.second * 0.92 }.coerceAtLeast(0)
                    }
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!isSeries || eps.isNotEmpty()) TvBtn(
                            Icons.Filled.PlayArrow, if (canResume || (isSeries && startIdx > 0)) t("resume") else t("play"), accent, primary = true,
                            modifier = Modifier.autoFocus(),
                        ) { if (isSeries) vm.playEpisodes(item, eps, startIdx) else vm.playMovie(item) }
                        if (versions.size > 1) TvBtn(Icons.Filled.Layers, (if (arL) "النسخ" else "Versions") + " (${versions.size})", accent) { onVersions() }
                        val fav = vm.isIn("fav", item)
                        TvBtn(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (fav) t("remove_fav") else t("add_fav"), accent) { vm.toggle("fav", item) }
                        val later = vm.isIn("later", item)
                        TvBtn(if (later) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, if (later) t("remove_later") else t("add_later"), accent) { vm.toggle("later", item) }
                        val w = vm.isIn("watched", item)
                        TvBtn(if (w) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (w) t("unmark_watched") else t("mark_watched"), accent) { vm.toggle("watched", item) }
                    }
                }
                // البوستر كاملًا بنسبة 2:3 مع توهج بلون البوستر
                val pH = heroH - 12.dp
                val pShape = RoundedCornerShape(18.dp)
                Box(
                    Modifier.padding(bottom = 6.dp).width(pH * 2f / 3f).height(pH)
                        .shadow(28.dp, pShape, ambientColor = accent, spotColor = accent)
                        .clip(pShape).background(C.surface)
                        .border(1.5.dp, Brush.verticalGradient(listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.2f))), pShape),
                ) { Img(item.image, Modifier.fillMaxSize(), w = 420, h = 630) }
            }

            // ---------------- التفاصيل السفلية (تتمرر) ----------------
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 40.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isSeries && seasons.size > 1) item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(seasons.keys.toList()) { s -> Chip("${t("season")} $s", s == curSeason) { onSeason(s) } }
                    }
                }
                if (isSeries) {
                    item { RText((if (arL) "الموسم " else "Season ") + curSeason + " - " + eps.size + (if (arL) " حلقة" else " episodes"), 20, C.text, bold = true) }
                    item { LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 6.dp)) { items(eps.size) { i -> EpisodeCard(vm, item, eps, i) } } }
                }
                item { RatingsPanel(vm, item, d?.year.orEmpty(), imdb) }
                item {
                    val names = d?.cast.orEmpty().split(",", "،").map { it.trim() }.filter { it.length >= 3 }.take(8)
                    if (names.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RText(if (arL) "الممثلون (اضغط لعرض أعماله)" else "Cast (tap for their works)", 16, C.textDim, bold = true)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(names) { n -> Chip(n, false) { onActor(n) } } }
                    }
                }
                if (franchise.size >= 2) item { ContentRow(if (arL) "أجزاء السلسلة" else "The collection", franchise.map { it.second }) { vm.openDetail(it) } }
                if (similarItems.size >= 3) item { ContentRow(t("similar"), similarItems) { vm.openDetail(it) } }
            }
        }
    }
}
