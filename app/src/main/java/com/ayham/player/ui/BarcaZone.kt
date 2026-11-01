package com.ayham.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.BCoach
import com.ayham.player.data.BEvent
import com.ayham.player.data.BFixture
import com.ayham.player.data.BLineup
import com.ayham.player.data.BMatch
import com.ayham.player.data.BPlayerStat
import com.ayham.player.data.BSquadPlayer
import com.ayham.player.data.BStanding
import com.ayham.player.data.BarcaApi
import com.ayham.player.data.BarcaStatus
import com.ayham.player.data.parseCoach
import com.ayham.player.data.parseFixtures
import com.ayham.player.data.parseMatch
import com.ayham.player.data.parsePlayerStats
import com.ayham.player.data.parseSquad
import com.ayham.player.data.parseStandings
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ألوان برشلونة الرسمية ثابتة داخل هذا القسم (مع هوية Ayham الداكنة)
private object BZ {
    val blue = Color(0xFF004D98)
    val garnet = Color(0xFFA50044)
    val gold = Color(0xFFEDBB00)
    val bg1 = Color(0xFF050B1A)
    val bg2 = Color(0xFF0A0616)
    val card = Color(0xCC0C1A36)
    val cardHi = Color(0xEE12306A)
    val dim = Color(0xFFB7C6DE)
    val win = Color(0xFF22C55E)
    val draw = Color(0xFFF59E0B)
    val loss = Color(0xFFEF4444)
    val purple = Color(0xFF8B5CF6)
    val stripe = Brush.horizontalGradient(listOf(Color(0xFF004D98), Color(0xFFA50044)))
}

// ---------------- تحميل البيانات ----------------
private sealed class BState<out T> {
    object Loading : BState<Nothing>()
    data class Ok<T>(val v: T, val stale: Boolean) : BState<T>()
    data class Err(val code: String, val msg: String) : BState<Nothing>()
}

@Composable
private fun <T> useBarca(
    ep: String, extra: Map<String, Any?> = emptyMap(), refreshMs: Long = 0L, enabled: Boolean = true, parse: (JsonElement) -> T,
): Pair<BState<T>, () -> Unit> {
    var state by remember(ep, extra) { mutableStateOf<BState<T>>(BState.Loading) }
    var tick by remember(ep, extra) { mutableIntStateOf(0) }
    LaunchedEffect(ep, extra, tick, enabled) {
        if (!enabled) return@LaunchedEffect
        while (true) {
            val r = withContext(Dispatchers.IO) { BarcaApi.call(ep, extra) }
            val next: BState<T> = if (r.ok && r.data != null) {
                runCatching { BState.Ok(parse(r.data), r.stale) as BState<T> }.getOrElse { BState.Err("parse", "") }
            } else BState.Err(r.err, r.msg)
            // فشل تحديث لاحق لا يمسح بيانات سليمة سبق عرضها
            state = if (next is BState.Err && state is BState.Ok<*>) (state as BState.Ok<T>).copy(stale = true) else next
            if (refreshMs <= 0) break
            delay(refreshMs)
        }
    }
    return state to { tick++ }
}

private fun fmtDate(ms: Long, ar: Boolean) = SimpleDateFormat("EEEE d MMMM", if (ar) Locale("ar") else Locale.ENGLISH).format(Date(ms))
private fun fmtTime(ms: Long) = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(ms))

// ---------------- مكوّنات صغيرة ----------------
@Composable
private fun Crest(url: String, size: Dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) Img(url, Modifier.size(size), scale = ContentScale.Fit, w = 160, h = 160)
    }
}

@Composable
private fun BzCard(modifier: Modifier = Modifier, hi: Boolean = false, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.then(modifier).clip(shape).background(if (hi) BZ.cardHi else BZ.card).border(1.dp, Color(0x33FFFFFF), shape).padding(16.dp)) { content() }
}

@Composable
private fun BzFocus(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable (Boolean) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Focusable(modifier, shape, 1.02f, onClick = onClick) { f ->
        Box(Modifier.clip(shape).background(if (f) BZ.cardHi else BZ.card).border(1.2.dp, if (f) BZ.gold else Color(0x33FFFFFF), shape).padding(16.dp)) { content(f) }
    }
}

@Composable
private fun BzChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Focusable(Modifier, RoundedCornerShape(22.dp), 1.05f, onClick = onClick) { f ->
        Box(
            Modifier.clip(RoundedCornerShape(22.dp))
                .background(if (f) BZ.blue else if (selected) Color(0xFF0A3C7A) else Color(0x33000000))
                .then(if (selected) Modifier.border(1.dp, BZ.gold, RoundedCornerShape(22.dp)) else Modifier)
                .padding(horizontal = 16.dp, vertical = 7.dp),
        ) { RText(label, 15, Color.White, bold = selected || f, maxLines = 1) }
    }
}

@Composable
private fun BzLoading() {
    Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BZ.gold, strokeWidth = 3.dp) }
}

@Composable
private fun BzError(code: String, msg: String, ar: Boolean, retry: () -> Unit) {
    val text = when (code) {
        "no_license" -> if (ar) "فعّل التطبيق بكود التفعيل لاستخدام هذا القسم." else "Activate the app with your code to use this section."
        "not_configured" -> if (ar) "القسم غير مفعّل بعد من جهة الخادم (مفتاح البيانات غير مضبوط)." else "This section is not enabled on the server yet."
        "plan" -> (if (ar) "خطة مزوّد البيانات الحالية لا تغطي هذا الموسم: " else "The data plan does not cover this season: ") + msg
        "quota" -> if (ar) "بلغنا الحد اليومي لطلبات البيانات. جرّب لاحقًا." else "Daily data limit reached. Try again later."
        "empty" -> if (ar) "لا توجد بيانات لهذا الموسم حاليًا." else "No data for this season yet."
        "bad_id" -> if (ar) "هذه المباراة غير متاحة." else "Match not available."
        "invalid", "revoked", "devices_full" -> if (ar) "تعذّر التحقق من التفعيل." else "Activation check failed."
        else -> if (ar) "تعذّر جلب البيانات. تحقق من الإنترنت وحاول مجددًا." else "Could not load data. Check your connection and retry."
    }
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RText("⚠", 34, BZ.gold)
            BodyText(text, 17, Color.White, modifier = Modifier.fillMaxWidth(0.8f))
            Focusable(Modifier, RoundedCornerShape(24.dp), 1.06f, onClick = retry) { f ->
                Box(Modifier.clip(RoundedCornerShape(24.dp)).background(if (f) BZ.blue else Color(0xFF0A3C7A)).padding(horizontal = 26.dp, vertical = 10.dp)) {
                    RText(if (ar) "إعادة المحاولة" else "Retry", 16, Color.White, bold = true)
                }
            }
        }
    }
}

@Composable
private fun <T> BzGate(s: BState<T>, ar: Boolean, retry: () -> Unit, content: @Composable (T) -> Unit) {
    when (s) {
        is BState.Loading -> BzLoading()
        is BState.Err -> BzError(s.code, s.msg, ar, retry)
        is BState.Ok -> content(s.v)
    }
}

// ---------------- الشاشة الرئيسية للقسم ----------------
@Composable
fun BarcaZone(vm: AppViewModel) {
    val ar = vm.lang == "ar"
    val compact = isCompact()
    var sub by rememberSaveable { mutableIntStateOf(0) }
    var openId by remember { mutableStateOf<Int?>(null) }
    BackHandler(enabled = openId != null) { openId = null }
    val (fxState, fxRetry) = useBarca("fixtures", refreshMs = 90_000L) { parseFixtures(it) }
    val tabs = listOf(if (ar) "الرئيسية" else "Home", if (ar) "المباريات" else "Matches", if (ar) "الفريق" else "Squad", if (ar) "الدوري" else "League")
    val crest = (fxState as? BState.Ok)?.v?.firstOrNull()?.let { if (it.barcaHome) it.h.logo else it.a.logo }.orEmpty()
    val stale = (fxState as? BState.Ok)?.stale == true

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BZ.bg1, BZ.bg2)))) {
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(BZ.garnet.copy(alpha = 0.28f), Color.Transparent), center = Offset(0f, 0f), radius = 1100f)))
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(BZ.blue.copy(alpha = 0.35f), Color.Transparent), center = Offset(2600f, 1500f), radius = 1400f)))
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = if (compact) 14.dp else 36.dp, vertical = if (compact) 8.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Crest(crest, if (compact) 40.dp else 54.dp)
                Column(Modifier.width(if (compact) 120.dp else 190.dp)) {
                    RText("BARÇA ZONE", if (compact) 17 else 24, Color.White, bold = true, maxLines = 1)
                    Box(Modifier.width(70.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(BZ.stripe))
                    if (stale) BodyText(if (ar) "بيانات محفوظة" else "Cached data", 11, BZ.gold, maxLines = 1)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    itemsIndexed(tabs) { i, l -> BzChip(l, sub == i && openId == null) { sub = i; openId = null } }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val id = openId
                if (id != null) {
                    MatchCenter(id, ar, compact) { openId = null }
                } else Crossfade(targetState = sub, animationSpec = tween(220), label = "bz") { t ->
                    when (t) {
                        0 -> BzGate(fxState, ar, fxRetry) { HomeSub(it, ar, compact) { openId = it } }
                        1 -> BzGate(fxState, ar, fxRetry) { MatchesSub(it, ar, compact) { openId = it } }
                        2 -> TeamSub(ar, compact)
                        else -> LeagueSub(ar, compact)
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(3.dp).background(BZ.stripe))
    }
}

// ---------------- الرئيسية ----------------
@Composable
private fun CountdownRow(target: Long, ar: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(target) {
        while (true) {
            now = System.currentTimeMillis()
            delay(if (target - now < 3_600_000L) 1_000L else 20_000L)
        }
    }
    val diff = (target - now).coerceAtLeast(0L) / 1000
    val parts = listOf(
        (diff / 86400) to (if (ar) "يوم" else "Days"), ((diff % 86400) / 3600) to (if (ar) "ساعة" else "Hrs"),
        ((diff % 3600) / 60) to (if (ar) "دقيقة" else "Min"), (diff % 60) to (if (ar) "ثانية" else "Sec"),
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            parts.forEach { (v, l) ->
                Column(
                    Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x55000000)).border(1.dp, Color(0x33EDBB00), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp).width(54.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RText(v.toString().padStart(2, '0'), 24, BZ.gold, bold = true, maxLines = 1)
                    BodyText(l, 11, BZ.dim, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Scoreline(f: BFixture, big: Boolean) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (big) 20.dp else 12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(if (big) 130.dp else 90.dp)) {
                Crest(f.h.logo, if (big) 64.dp else 44.dp)
                BodyText(f.h.name, if (big) 14 else 12, Color.White, bold = true, maxLines = 1)
            }
            val mid = if (f.gh != null && f.ga != null) "${f.gh} - ${f.ga}" else "vs"
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RText(mid, if (big) 40 else 28, Color.White, bold = true, maxLines = 1)
                f.pen?.let { BodyText("(${it.first}-${it.second} pens)", 12, BZ.dim) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(if (big) 130.dp else 90.dp)) {
                Crest(f.a.logo, if (big) 64.dp else 44.dp)
                BodyText(f.a.name, if (big) 14 else 12, Color.White, bold = true, maxLines = 1)
            }
        }
    }
}

@Composable
private fun HomeSub(fx: List<BFixture>, ar: Boolean, compact: Boolean, open: (Int) -> Unit) {
    val nowMs = System.currentTimeMillis()
    // مباراة جارية أو على وشك البدء/انتهت للتو: نسأل الخادم عن تفاصيلها الحية
    val windowFx = fx.firstOrNull { !it.finished && nowMs > it.ms - 15 * 60_000L && nowMs < it.ms + 4 * 3_600_000L }
    val (liveState, _) = useBarca("match", mapOf("id" to (windowFx?.id ?: 0)), refreshMs = 120_000L, enabled = windowFx != null) { parseMatch(it) }
    val liveFx: BFixture? = (liveState as? BState.Ok)?.v?.fx ?: windowFx
    val showLive = liveFx != null && (liveFx.live || (!liveFx.finished && liveFx.gh != null))
    val next = fx.firstOrNull { it.upcoming && it.ms > nowMs - 3_600_000L && it.id != liveFx?.id }
    val last = (listOfNotNull(liveFx?.takeIf { it.finished }) + fx.filter { it.finished }).maxByOrNull { it.ts }
    val (stState, _) = useBarca("standings", refreshMs = 0L) { parseStandings(it) }
    val me = (stState as? BState.Ok)?.v?.firstOrNull { it.id == BarcaApi.TEAM_ID }
    val pad = if (compact) 12.dp else 36.dp

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = pad, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showLive && liveFx != null) item(key = "live") {
            BzFocus(Modifier.fillMaxWidth(), onClick = { open(liveFx.id) }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(BZ.loss))
                        RText((if (ar) "مباشر" else "LIVE") + (liveFx.el?.let { "  $it'" } ?: ""), 16, BZ.loss, bold = true, maxLines = 1)
                        BodyText(BarcaStatus.label(liveFx.st, ar), 14, BZ.dim, maxLines = 1)
                    }
                    Scoreline(liveFx, true)
                    BodyText(liveFx.league + (if (liveFx.round.isNotBlank()) " · " + liveFx.round else ""), 13, BZ.dim, maxLines = 1)
                }
            }
        }
        if (next != null) item(key = "next") {
            BzFocus(Modifier.fillMaxWidth(), onClick = { open(next.id) }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RText(if (ar) "المباراة القادمة" else "NEXT MATCH", 14, BZ.gold, bold = true)
                    Scoreline(next, true)
                    BodyText(next.league + (if (next.round.isNotBlank()) " · " + next.round else ""), 13, BZ.dim, maxLines = 1)
                    BodyText(fmtDate(next.ms, ar) + "  ·  " + fmtTime(next.ms) + (if (next.venue.isNotBlank()) "  ·  " + next.venue else ""), 14, Color.White, maxLines = 2)
                    CountdownRow(next.ms, ar)
                }
            }
        }
        if (last != null) item(key = "last") {
            BzFocus(Modifier.fillMaxWidth(), onClick = { open(last.id) }) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rc = when (last.result) { 1 -> BZ.win; -1 -> BZ.loss; 0 -> BZ.draw; else -> BZ.dim }
                    val rt = when (last.result) { 1 -> if (ar) "فوز" else "WIN"; -1 -> if (ar) "خسارة" else "LOSS"; 0 -> if (ar) "تعادل" else "DRAW"; else -> "" }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        RText(if (ar) "آخر نتيجة" else "LAST RESULT", 14, BZ.gold, bold = true)
                        if (rt.isNotEmpty()) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(rc.copy(alpha = 0.25f)).border(1.dp, rc, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 1.dp)) { RText(rt, 13, rc, bold = true) }
                    }
                    Scoreline(last, false)
                    BodyText(last.league + " · " + fmtDate(last.ms, ar), 13, BZ.dim, maxLines = 1)
                }
            }
        }
        if (me != null) item(key = "table") {
            BzCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        RText(if (ar) "ترتيب الدوري الإسباني" else "LA LIGA POSITION", 13, BZ.gold, bold = true)
                        RText("#${me.rank}", 34, Color.White, bold = true)
                    }
                    StatBox(me.pts.toString(), if (ar) "نقطة" else "PTS")
                    StatBox("${me.w}-${me.d}-${me.l}", if (ar) "ف-ت-خ" else "W-D-L")
                    StatBox((if (me.gd > 0) "+" else "") + me.gd, if (ar) "الفارق" else "GD")
                }
            }
        }
        if (next == null && last == null && !showLive) item(key = "none") {
            BzCard(Modifier.fillMaxWidth()) { BodyText(if (ar) "لا توجد مباريات في هذا الموسم حاليًا." else "No matches this season yet.", 16, Color.White) }
        }
    }
}

@Composable
private fun StatBox(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RText(v, 24, Color.White, bold = true, maxLines = 1)
        BodyText(l, 12, BZ.dim, maxLines = 1)
    }
}

// ---------------- المباريات ----------------
@Composable
private fun FixtureRow(f: BFixture, ar: Boolean, onClick: () -> Unit) {
    BzFocus(Modifier.fillMaxWidth(), onClick = onClick) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Crest(f.h.logo, 26.dp); BodyText(f.h.name, 14, Color.White, bold = f.barcaHome, maxLines = 1)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Crest(f.a.logo, 26.dp); BodyText(f.a.name, 14, Color.White, bold = !f.barcaHome, maxLines = 1)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(110.dp)) {
                    if (f.gh != null && f.ga != null) {
                        val rc = when (f.result) { 1 -> BZ.win; -1 -> BZ.loss; 0 -> BZ.draw; else -> Color.White }
                        RText("${f.gh} - ${f.ga}", 22, if (f.finished) rc else Color.White, bold = true, maxLines = 1)
                        BodyText(if (f.live) (f.el?.let { "$it'" } ?: BarcaStatus.label(f.st, ar)) else BarcaStatus.label(f.st, ar), 11, if (f.live) BZ.loss else BZ.dim, maxLines = 1)
                    } else {
                        RText(fmtTime(f.ms), 18, BZ.gold, bold = true, maxLines = 1)
                        BodyText(BarcaStatus.label(f.st, ar), 11, BZ.dim, maxLines = 1)
                    }
                }
                Column(Modifier.width(if (isCompact()) 90.dp else 170.dp), horizontalAlignment = Alignment.End) {
                    BodyText(fmtDate(f.ms, ar).substringAfter(' '), 12, Color.White, maxLines = 1)
                    BodyText(f.league, 11, BZ.dim, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MatchesSub(fx: List<BFixture>, ar: Boolean, compact: Boolean, open: (Int) -> Unit) {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 القادمة 1 النتائج
    val nowMs = System.currentTimeMillis()
    val list = remember(fx, mode) {
        if (mode == 0) fx.filter { !it.finished }.sortedBy { it.ts } else fx.filter { it.finished }.sortedByDescending { it.ts }
    }
    val pad = if (compact) 12.dp else 36.dp
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = pad, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BzChip(if (ar) "القادمة" else "Upcoming", mode == 0) { mode = 0 }
            BzChip(if (ar) "النتائج" else "Results", mode == 1) { mode = 1 }
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BodyText(if (mode == 0) (if (ar) "لا توجد مباريات قادمة." else "No upcoming matches.") else (if (ar) "لا توجد نتائج بعد." else "No results yet."), 17, BZ.dim)
            }
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = pad, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.id }) { f -> FixtureRow(f, ar) { open(f.id) } }
        }
    }
}

// ---------------- مركز المباراة ----------------
@Composable
private fun MatchCenter(id: Int, ar: Boolean, compact: Boolean, close: () -> Unit) {
    val (st, retry) = useBarca("match", mapOf("id" to id), refreshMs = 60_000L) { parseMatch(it) }
    var sec by rememberSaveable { mutableIntStateOf(0) }
    val pad = if (compact) 12.dp else 36.dp
    BzGate(st, ar, retry) { m ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = pad, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "head") {
                BzCard(Modifier.fillMaxWidth(), hi = true) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (m.fx.live) Box(Modifier.size(10.dp).clip(CircleShape).background(BZ.loss))
                            RText(BarcaStatus.label(m.fx.st, ar) + (if (m.fx.live) m.fx.el?.let { "  $it'" }.orEmpty() else ""), 15, if (m.fx.live) BZ.loss else BZ.gold, bold = true, maxLines = 1)
                        }
                        Scoreline(m.fx, true)
                        BodyText(m.fx.league + (if (m.fx.round.isNotBlank()) " · " + m.fx.round else ""), 13, BZ.dim, maxLines = 1)
                        BodyText(fmtDate(m.fx.ms, ar) + " · " + fmtTime(m.fx.ms) + (if (m.fx.venue.isNotBlank()) " · " + m.fx.venue else ""), 13, Color.White, maxLines = 2)
                    }
                }
            }
            item(key = "chips") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BzChip(if (ar) "الملخص" else "Summary", sec == 0) { sec = 0 }
                    BzChip(if (ar) "الإحصائيات" else "Stats", sec == 1) { sec = 1 }
                    BzChip(if (ar) "التشكيلة" else "Lineups", sec == 2) { sec = 2 }
                    BzChip(if (ar) "رجوع" else "Back", false, close)
                }
            }
            when (sec) {
                0 -> {
                    val evs = m.events.sortedWith(compareBy({ it.minute }, { it.extra ?: 0 }))
                    if (evs.isEmpty()) item(key = "noev") { BodyText(if (ar) "لا توجد أحداث بعد." else "No events yet.", 15, BZ.dim) }
                    else items(evs.size, key = { "ev$it" }) { i -> EventRow(evs[i], m.fx, ar) }
                }
                1 -> item(key = "stats") { StatsBlock(m, ar) }
                else -> {
                    if (m.lineups.isEmpty()) item(key = "nolu") { BodyText(if (ar) "التشكيلة غير متوفرة بعد (تظهر قبل المباراة بقليل)." else "Lineups not available yet.", 15, BZ.dim) }
                    else items(m.lineups.sortedByDescending { it.teamId == BarcaApi.TEAM_ID }, key = { "lu" + it.teamId }) { l -> LineupBlock(l, m.fx, ar) }
                }
            }
        }
    }
}

@Composable
private fun EventRow(e: BEvent, f: BFixture, ar: Boolean) {
    val icon = when {
        e.type.equals("Goal", true) -> if (e.detail.contains("Missed", true)) "✖" else if (e.detail.contains("Own", true)) "⚽(OG)" else "⚽"
        e.type.equals("Card", true) -> if (e.detail.contains("Red", true)) "🟥" else "🟨"
        e.type.equals("subst", true) -> "🔄"
        e.type.equals("Var", true) -> "VAR"
        else -> "•"
    }
    val mine = e.teamId == BarcaApi.TEAM_ID
    val teamName = if (e.teamId == f.h.id) f.h.name else f.a.name
    val txt = if (e.type.equals("subst", true)) (if (ar) "دخول: " else "On: ") + e.assist + (if (e.player.isNotBlank()) (if (ar) "  خروج: " else "  Off: ") + e.player else "")
    else e.player + (if (e.assist.isNotBlank() && e.type.equals("Goal", true)) (if (ar) "  (صناعة: " else "  (assist: ") + e.assist + ")" else "")
    BzCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(54.dp)) { RText("${e.minute}'" + (e.extra?.let { "+$it" } ?: ""), 16, BZ.gold, bold = true, maxLines = 1) }
            RText(icon, 18, Color.White, maxLines = 1)
            Column(Modifier.weight(1f)) {
                BodyText(txt, 15, Color.White, bold = mine, maxLines = 2)
                BodyText(teamName + (if (e.detail.isNotBlank() && !e.type.equals("subst", true)) " · " + e.detail else ""), 11, BZ.dim, maxLines = 1)
            }
        }
    }
}

private fun statNum(s: String): Double = s.trim().removeSuffix("%").toDoubleOrNull() ?: 0.0

@Composable
private fun StatsBlock(m: BMatch, ar: Boolean) {
    val hs = m.stats[m.fx.h.id].orEmpty(); val aw = m.stats[m.fx.a.id].orEmpty()
    if (hs.isEmpty() && aw.isEmpty()) { BodyText(if (ar) "الإحصائيات غير متوفرة بعد." else "Statistics not available yet.", 15, BZ.dim); return }
    val types = (hs.map { it.first } + aw.map { it.first }).distinct()
    BzCard(Modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                types.forEach { t ->
                    val a = hs.firstOrNull { it.first == t }?.second.orEmpty().ifBlank { "0" }
                    val b = aw.firstOrNull { it.first == t }?.second.orEmpty().ifBlank { "0" }
                    val na = statNum(a); val nb = statNum(b); val tot = (na + nb).takeIf { it > 0 } ?: 1.0
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            RText(a, 15, Color.White, bold = true, maxLines = 1); BodyText(t, 13, BZ.dim, maxLines = 1); RText(b, 15, Color.White, bold = true, maxLines = 1)
                        }
                        Row(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x33FFFFFF))) {
                            val wa = (na / tot).toFloat().coerceIn(0.001f, 0.999f)
                            Box(Modifier.weight(wa).fillMaxHeight().background(if (m.fx.h.id == BarcaApi.TEAM_ID) BZ.blue else BZ.garnet))
                            Box(Modifier.weight(1f - wa).fillMaxHeight().background(if (m.fx.a.id == BarcaApi.TEAM_ID) BZ.blue else BZ.garnet))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineupBlock(l: BLineup, f: BFixture, ar: Boolean) {
    val name = if (l.teamId == f.h.id) f.h.name else f.a.name
    BzCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RText(name + (if (l.formation.isNotBlank()) "  ·  " + l.formation else ""), 17, BZ.gold, bold = true, maxLines = 1)
            if (l.coach.isNotBlank()) BodyText((if (ar) "المدرب: " else "Coach: ") + l.coach, 13, BZ.dim, maxLines = 1)
            BodyText(if (ar) "التشكيلة الأساسية" else "Starting XI", 13, Color.White, bold = true)
            l.xi.forEach { p -> BodyText("${p.number ?: "-"}   ${p.name}   ${p.pos}", 14, Color.White, maxLines = 1) }
            if (l.subs.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                BodyText(if (ar) "الاحتياط" else "Substitutes", 13, Color.White, bold = true)
                l.subs.forEach { p -> BodyText("${p.number ?: "-"}   ${p.name}   ${p.pos}", 13, BZ.dim, maxLines = 1) }
            }
        }
    }
}

// ---------------- الفريق ----------------
private fun posLabel(p: String, ar: Boolean) = if (ar) when (p) { "Goalkeeper" -> "حراس المرمى"; "Defender" -> "المدافعون"; "Midfielder" -> "خط الوسط"; "Attacker" -> "المهاجمون"; else -> p }
else when (p) { "Goalkeeper" -> "Goalkeepers"; "Defender" -> "Defenders"; "Midfielder" -> "Midfielders"; "Attacker" -> "Forwards"; else -> p }

@Composable
private fun TeamSub(ar: Boolean, compact: Boolean) {
    val (sq, sqRetry) = useBarca("squad") { parseSquad(it) }
    val (coach, _) = useBarca("coach") { parseCoach(it) }
    val (p1, _) = useBarca("players", mapOf("page" to 1)) { parsePlayerStats(it) }
    val (p2, _) = useBarca("players", mapOf("page" to 2)) { parsePlayerStats(it) }
    val stats: Map<Int, BPlayerStat> = remember(p1, p2) {
        ((p1 as? BState.Ok)?.v.orEmpty() + (p2 as? BState.Ok)?.v.orEmpty()).associateBy { it.id }
    }
    val pad = if (compact) 12.dp else 36.dp
    BzGate(sq, ar, sqRetry) { players ->
        val order = listOf("Goalkeeper", "Defender", "Midfielder", "Attacker")
        val groups = order.map { it to players.filter { p -> p.pos == it } }.filter { it.second.isNotEmpty() } +
            listOf("" to players.filter { it.pos !in order }).filter { it.second.isNotEmpty() }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = pad, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (coach as? BState.Ok)?.v?.let { c -> item(key = "coach") { CoachCard(c, ar) } }
            groups.forEach { (pos, list) ->
                item(key = "h$pos") { RText(posLabel(pos, ar), 18, BZ.gold, bold = true, modifier = Modifier.padding(top = 8.dp)) }
                items(list, key = { "p" + it.id }) { p -> PlayerRow(p, stats[p.id], ar) }
            }
        }
    }
}

@Composable
private fun CoachCard(c: BCoach, ar: Boolean) {
    BzCard(Modifier.fillMaxWidth(), hi = true) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0x33FFFFFF))) { if (c.photo.isNotBlank()) Img(c.photo, Modifier.fillMaxSize(), w = 160, h = 160) }
            Column {
                BodyText(if (ar) "المدرب" else "HEAD COACH", 12, BZ.gold, bold = true)
                RText(c.name, 20, Color.White, bold = true, maxLines = 1)
                BodyText(listOfNotNull(c.nat.takeIf { it.isNotBlank() }, c.age?.let { (if (ar) "$it سنة" else "$it yrs") }).joinToString(" · "), 13, BZ.dim, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PlayerRow(p: BSquadPlayer, st: BPlayerStat?, ar: Boolean) {
    BzFocus(Modifier.fillMaxWidth(), onClick = {}) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0x33FFFFFF))) { if (p.photo.isNotBlank()) Img(p.photo, Modifier.fillMaxSize(), w = 140, h = 140) }
            Box(Modifier.width(34.dp)) { RText(p.number?.toString() ?: "-", 22, BZ.gold, bold = true, maxLines = 1) }
            Column(Modifier.weight(1f)) {
                RText(p.name, 17, Color.White, bold = true, maxLines = 1)
                BodyText(listOfNotNull(p.age?.let { if (ar) "$it سنة" else "$it yrs" }, st?.rating?.let { "★ $it" }).joinToString("  ·  "), 12, BZ.dim, maxLines = 1)
            }
            if (st != null && st.apps > 0) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBox(st.apps.toString(), if (ar) "مباراة" else "Apps")
                        StatBox(st.goals.toString(), if (ar) "هدف" else "Goals")
                        StatBox(st.ast.toString(), if (ar) "صناعة" else "Ast")
                        if (!isCompact()) { StatBox(st.yc.toString(), "🟨"); StatBox(st.rc.toString(), "🟥") }
                    }
                }
            }
        }
    }
}

// ---------------- الدوري ----------------
@Composable
private fun LeagueSub(ar: Boolean, compact: Boolean) {
    val (st, retry) = useBarca("standings", refreshMs = 300_000L) { parseStandings(it) }
    val pad = if (compact) 12.dp else 36.dp
    BzGate(st, ar, retry) { rows ->
        val listState = rememberLazyListState()
        LaunchedEffect(rows.size) { val i = rows.indexOfFirst { it.id == BarcaApi.TEAM_ID }; if (i > 6) runCatching { listState.scrollToItem(i - 3) } }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(horizontal = pad, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item(key = "hd") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(34.dp)) { BodyText("#", 12, BZ.dim) }
                        Box(Modifier.weight(1f)) { BodyText(if (ar) "الفريق" else "Team", 12, BZ.dim) }
                        listOf(if (ar) "لعب" else "P", if (ar) "ف" else "W", if (ar) "ت" else "D", if (ar) "خ" else "L", if (ar) "+/-" else "GD").forEach { Box(Modifier.width(if (compact) 28.dp else 40.dp)) { BodyText(it, 12, BZ.dim) } }
                        Box(Modifier.width(if (compact) 36.dp else 46.dp)) { BodyText(if (ar) "نقاط" else "Pts", 12, BZ.gold, bold = true) }
                    }
                }
                items(rows, key = { "r" + it.id }) { r ->
                    val me = r.id == BarcaApi.TEAM_ID
                    val shape = RoundedCornerShape(12.dp)
                    val zone = when {
                        r.desc.contains("Champions", true) -> BZ.blue
                        r.desc.contains("Europa", true) || r.desc.contains("Conference", true) -> BZ.purple
                        r.desc.contains("Relegation", true) -> BZ.loss
                        else -> Color.Transparent
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(shape).background(if (me) Color(0xFF0E3A78) else Color(0x66000000))
                            .then(if (me) Modifier.border(1.2.dp, BZ.gold, shape) else Modifier).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(4.dp).height(26.dp).clip(RoundedCornerShape(2.dp)).background(zone))
                        Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) { RText(r.rank.toString(), 14, Color.White, bold = me, maxLines = 1) }
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Crest(r.logo, 24.dp); BodyText(r.name, 14, Color.White, bold = me, maxLines = 1)
                        }
                        listOf(r.p, r.w, r.d, r.l, r.gd).forEach { Box(Modifier.width(if (compact) 28.dp else 40.dp)) { BodyText(it.toString(), 13, Color.White, maxLines = 1) } }
                        Box(Modifier.width(if (compact) 36.dp else 46.dp)) { RText(r.pts.toString(), 15, BZ.gold, bold = true, maxLines = 1) }
                    }
                }
            }
        }
    }
}
