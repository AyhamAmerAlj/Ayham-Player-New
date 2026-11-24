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
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.imageLoader
import coil.request.ImageRequest
import com.ayham.player.AppViewModel
import com.ayham.player.data.Details
import com.ayham.player.data.Episode
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import com.ayham.player.data.SeriesInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
                    var showPlot by remember(item.key) { mutableStateOf(false) }
                    if (plot.isNotBlank()) {
                        var more by remember(plot) { mutableStateOf(false) }
                        Focusable(Modifier.fillMaxWidth(0.94f), RoundedCornerShape(12.dp), 1.0f, onClick = { showPlot = true }) { f ->
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(if (f) tc(0x44000000) else Color.Transparent)
                                    .border(1.5.dp, if (f) accent else Color.Transparent, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                androidx.compose.material3.Text(
                                    plot, maxLines = if (sH < 560) 2 else 3, overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { more = it.hasVisualOverflow },
                                    style = TextStyle(fontFamily = AppFont, fontSize = 15.sp, color = tc(0xFFE9E2F8), lineHeight = 22.sp),
                                )
                                if (more || f) RText((if (arL) "اقرأ القصة كاملة  ▾" else "Read the full story  ▾"), 13, accent, bold = true, maxLines = 1)
                            }
                        }
                    }
                    if (showPlot) PlotDialog(item.name, plot, accent) { showPlot = false }
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
                if (isSeries && eps.isNotEmpty()) item { TvEpisodeStudio(vm, item, seasons, curSeason, eps, accent, onSeason) }
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


@Composable
private fun PlotDialog(title: String, plot: String, accent: Color, onClose: () -> Unit) {
    val scroll = rememberScrollState()
    val fr = remember { FocusRequester() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(tc(0xDD05020B)), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(alpha = 0.30f), Color.Transparent), radius = 1100f)))
            val shape = RoundedCornerShape(26.dp)
            Column(
                Modifier.fillMaxWidth(0.62f).fillMaxHeight(0.78f).clip(shape)
                    .background(Brush.verticalGradient(listOf(tc(0xF2160A2C), tc(0xF2090414))))
                    .border(1.5.dp, Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.15f))), shape)
                    .padding(horizontal = 30.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(5.dp).height(26.dp).clip(RoundedCornerShape(3.dp)).background(accent))
                    RText(title, 22, Color.White, bold = true, maxLines = 2, modifier = Modifier.weight(1f))
                }
                Box(
                    Modifier.weight(1f).fillMaxWidth().focusRequester(fr).focusable()
                        .onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionDown || e.key == Key.DirectionUp)) {
                                scope.launch { scroll.animateScrollTo((scroll.value + (if (e.key == Key.DirectionDown) 220 else -220)).coerceAtLeast(0)) }; true
                            } else false
                        }
                        .verticalScroll(scroll),
                ) {
                    androidx.compose.material3.Text(plot, style = TextStyle(fontFamily = AppFont, fontSize = 18.sp, color = tc(0xFFEDE7FA), lineHeight = 30.sp))
                }
                RText("▲▼  تمرير      OK / رجوع  إغلاق", 12, C.textDim, maxLines = 1)
            }
        }
    }
}

/** استوديو الحلقات: شريط مواسم بأرقام كبيرة (يتبدل الموسم بمجرد التركيز) وتحته بكرة حلقات سينمائية. */
@Composable
private fun TvEpisodeStudio(
    vm: AppViewModel, series: Item, seasons: Map<Int, List<Episode>>, curSeason: Int, eps: List<Episode>,
    accent: Color, onSeason: (Int) -> Unit,
) {
    val arL = vm.lang == "ar"
    val nHist = vm.history.size + vm.watched.size
    fun fracOf(e: Episode): Float { val p = vm.progressOf(e.key); return if (p != null && p.second > 0) (p.first.toFloat() / p.second).coerceIn(0f, 1f) else 0f }
    val doneBySeason = remember(seasons, nHist) { seasons.mapValues { (_, l) -> l.count { fracOf(it) > 0.92f } } }
    val nextIdx = remember(eps, nHist) {
        val partial = eps.indexOfFirst { val f = fracOf(it); f > 0.01f && f <= 0.92f }
        if (partial >= 0) partial else eps.indexOfFirst { fracOf(it) <= 0.92f }.let { if (it < 0) 0 else it }
    }
    val done = doneBySeason[curSeason] ?: 0
    val listState = rememberLazyListState()
    var focusIdx by remember(curSeason, nextIdx) { mutableStateOf(nextIdx) }
    LaunchedEffect(curSeason, nextIdx) { runCatching { listState.scrollToItem((nextIdx - 1).coerceAtLeast(0)) } }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ---------- المواسم: أقراص رفيعة مع تقدّم ----------
        if (seasons.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp)) {
                items(seasons.keys.toList()) { sn ->
                    val sel = sn == curSeason
                    val total = seasons[sn]?.size ?: 0
                    val dn = doneBySeason[sn] ?: 0
                    val shape = RoundedCornerShape(50)
                    Focusable(Modifier.onFocusChanged { if (it.hasFocus && !sel) onSeason(sn) }, shape, 1.06f, onClick = { onSeason(sn) }) { f ->
                        Row(
                            Modifier.clip(shape)
                                .background(if (sel) accent.copy(alpha = 0.9f) else tc(0x55000000))
                                .border(if (f) 2.dp else 1.dp, if (f) Color.White else if (sel) accent else tc(0x33FFFFFF), shape)
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RText((if (arL) "الموسم " else "Season ") + sn, 15, if (sel || f) Color.White else tc(0xFFC9BDE6), bold = true, maxLines = 1)
                            RText(if (dn >= total && total > 0) "✓" else "$total", 12, if (sel) Color.White else C.textDim, bold = true, maxLines = 1)
                        }
                    }
                }
            }
        }

        // ---------- الكادر الرئيسي: الحلقة المحددة بحجم كبير مع تفاصيلها ----------
        val cur = eps.getOrNull(focusIdx.coerceIn(0, (eps.size - 1).coerceAtLeast(0)))
        if (cur != null) {
            androidx.compose.animation.AnimatedContent(
                targetState = cur,
                transitionSpec = { androidx.compose.animation.fadeIn(tween(260)) togetherWith androidx.compose.animation.fadeOut(tween(120)) },
                label = "epSpot",
            ) { e ->
                val frac = fracOf(e)
                val shape = RoundedCornerShape(18.dp)
                Row(Modifier.fillMaxWidth().height(176.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Box(Modifier.width(312.dp).fillMaxHeight().clip(shape).background(C.surface).border(1.dp, accent.copy(alpha = 0.5f), shape)) {
                        Img(e.image.ifEmpty { series.image }, Modifier.fillMaxSize(), w = 640, h = 360)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to tc(0xDD000000))))
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(54.dp).background(accent.copy(alpha = 0.88f), RoundedCornerShape(50)).padding(10.dp))
                        if (frac > 0.01f) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(5.dp).background(tc(0x66000000))) {
                            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(accent))
                        }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            RText("S$curSeason · E${e.num}", 14, accent, bold = true, maxLines = 1)
                            if (e.duration.isNotBlank()) RText(e.duration, 13, C.textDim, maxLines = 1)
                            if (frac > 0.92f) RText(if (arL) "✓ تمت المشاهدة" else "✓ Watched", 13, C.ok, bold = true, maxLines = 1)
                            else if (eps.indexOf(e) == nextIdx) RText(if (frac > 0.01f) (if (arL) "▶ أكمل المشاهدة" else "▶ Resume") else (if (arL) "▶ التالية" else "▶ Up next"), 13, Color.White, bold = true, maxLines = 1)
                        }
                        androidx.compose.material3.Text(
                            e.title.ifBlank { "${t("episode")} ${e.num}" }, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = TextStyle(fontFamily = AppFont, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, lineHeight = 32.sp,
                                shadow = androidx.compose.ui.graphics.Shadow(Color.Black, Offset(0f, 4f), 14f)),
                        )
                        if (e.plot.isNotBlank()) BodyText(e.plot, 14, tc(0xFFE5DEF5), maxLines = 3, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }

        // ---------- شريط الفيلم: لقطات صغيرة بثقوب فيلم ----------
        Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RText("$done / ${eps.size} " + (if (arL) "تمت مشاهدتها" else "watched"), 13, accent, bold = true)
            Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(tc(0x33FFFFFF))) {
                Box(Modifier.fillMaxWidth(if (eps.isEmpty()) 0f else done.toFloat() / eps.size).fillMaxHeight().background(Brush.horizontalGradient(listOf(accent, Color.White))))
            }
        }
        val holes = tc(0x33FFFFFF)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(tc(0xCC05020B))
                .drawBehind {
                    val step = 22.dp.toPx(); val w = 9.dp.toPx(); val h = 5.dp.toPx()
                    var x = 6.dp.toPx()
                    while (x < size.width) {
                        drawRoundRect(holes, Offset(x, 4.dp.toPx()), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(2f, 2f))
                        drawRoundRect(holes, Offset(x, size.height - 4.dp.toPx() - h), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(2f, 2f))
                        x += step
                    }
                },
        ) {
            LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp)) {
                itemsIndexed(eps, key = { _, e -> e.key }) { i, ep ->
                    val frac = fracOf(ep)
                    val isDone = frac > 0.92f
                    val shape = RoundedCornerShape(8.dp)
                    Focusable(Modifier.onFocusChanged { if (it.hasFocus) focusIdx = i }, shape, 1.1f, onClick = { vm.playEpisodes(series, eps, i) }) { f ->
                        Box(
                            Modifier.width(152.dp).height(86.dp).clip(shape).background(C.surface)
                                .border(if (f) 2.5.dp else 1.dp, if (f) Color.White else if (i == nextIdx) accent else tc(0x22FFFFFF), shape),
                        ) {
                            Img(ep.image.ifEmpty { series.image }, Modifier.fillMaxSize(), w = 300, h = 170)
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to tc(0xCC000000))))
                            androidx.compose.material3.Text(
                                "${ep.num}", Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 2.dp),
                                style = TextStyle(fontFamily = AppFont, fontSize = 26.sp, fontWeight = FontWeight.Black, color = if (f) Color.White else accent.copy(alpha = 0.9f)),
                            )
                            if (isDone) Icon(Icons.Filled.CheckCircle, null, tint = C.ok, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp))
                            if (frac > 0.01f && !isDone) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(tc(0x66000000))) {
                                Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(accent))
                            }
                        }
                    }
                }
            }
        }
    }
}
