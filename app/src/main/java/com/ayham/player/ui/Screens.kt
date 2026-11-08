package com.ayham.player.ui

import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import com.ayham.player.AppViewModel
import com.ayham.player.LoadState
import com.ayham.player.R
import com.ayham.player.data.Details
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import com.ayham.player.data.LiveSource
import kotlinx.coroutines.delay

// ============================ Splash ============================
@Composable
fun SplashScreen() {
    // افتتاحية سينمائية: حلقة تُرسم ويظهر فيها زر التشغيل، يتحول إلى نقطة مضيئة، ثم ينفجر منها شعار Ayham ويظهر الاسم حرفاً حرفاً.
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(2600, easing = androidx.compose.animation.core.LinearEasing)) }
    LaunchedEffect(Unit) {
        if (!UiSound.splashPlayed) {
            var w = 0
            while (!UiSound.isReady() && w < 20) { delay(50); w++ }
            UiSound.splashPlayed = true
            UiSound.splash()
        }
    }
    val inf = androidx.compose.animation.core.rememberInfiniteTransition(label = "sp")
    val pulse by inf.animateFloat(
        0.85f, 1.15f,
        androidx.compose.animation.core.infiniteRepeatable(tween(1600, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "pulse",
    )
    val violet = Color(0xFF8B5CF6)
    val magenta = Color(0xFFD946EF)
    val compact = isCompact()
    val box = if (compact) 150.dp else 200.dp
    val fs = if (compact) 30 else 42
    fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
    fun ease(x: Float) = 1f - (1f - x) * (1f - x) * (1f - x)
    Box(Modifier.fillMaxSize().background(Color(0xFF030106)), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val t = p.value
            val g = 0.34f * ease(seg(t, 0.05f, 0.7f)) * (if (t >= 0.95f) pulse else 1f)
            drawRect(Brush.radialGradient(listOf(violet.copy(alpha = g), Color.Transparent), center = center, radius = size.maxDimension * 0.45f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(box), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val t = p.value
                    val c = center
                    val r = size.minDimension * 0.40f
                    val draw = ease(seg(t, 0.05f, 0.26f))
                    val morph = ease(seg(t, 0.42f, 0.60f))
                    val rr = r * (1f + 0.45f * morph)
                    if (morph < 1f && draw > 0f) {
                        drawArc(
                            Brush.sweepGradient(listOf(violet, magenta, violet), center = c),
                            startAngle = -90f, sweepAngle = 360f * draw, useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(c.x - rr, c.y - rr),
                            size = androidx.compose.ui.geometry.Size(rr * 2, rr * 2),
                            alpha = 1f - morph,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        )
                    }
                    // زر التشغيل ثم يتقلص إلى نقطة
                    val tri = seg(t, 0.14f, 0.26f) * (1f - seg(t, 0.32f, 0.40f))
                    if (tri > 0f) {
                        val s = r * 0.5f * (1f - 0.3f * seg(t, 0.32f, 0.40f))
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(c.x - s * 0.5f, c.y - s * 0.65f); lineTo(c.x - s * 0.5f, c.y + s * 0.65f); lineTo(c.x + s * 0.75f, c.y); close()
                        }
                        drawPath(path, Color.White.copy(alpha = tri))
                    }
                    val dot = seg(t, 0.32f, 0.42f) * (1f - seg(t, 0.46f, 0.58f))
                    if (dot > 0f) {
                        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = dot), magenta.copy(alpha = 0.7f * dot), Color.Transparent), center = c, radius = r * 0.35f), r * 0.35f, c)
                    }
                }
                Image(
                    painterResource(R.drawable.ic_launcher), null,
                    Modifier.size(box * 0.78f).graphicsLayer {
                        val x = seg(p.value, 0.46f, 0.70f)
                        val back = 1f + 2.2f * Math.pow((x - 1f).toDouble(), 3.0).toFloat() + 1.2f * Math.pow((x - 1f).toDouble(), 2.0).toFloat()
                        val sc = 0.15f + 0.85f * back
                        scaleX = sc; scaleY = sc
                        alpha = (x * 1.6f).coerceIn(0f, 1f)
                    }.clip(RoundedCornerShape(26.dp)),
                )
            }
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                // الاسم كنص واحد (أخف بكثير من حرف لكل عنصر) مع ظهور وتكبير ناعم
                val name = remember {
                    androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(androidx.compose.ui.text.SpanStyle(color = Color.White)) { append("AYHAM ") }
                        withStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFD946EF))) { append("PLAYER") }
                    }
                }
                androidx.compose.foundation.text.BasicText(
                    name,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = AppFont, fontSize = (fs + 6).sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 4.sp,
                    ),
                    modifier = Modifier.graphicsLayer {
                        val x = seg(p.value, 0.58f, 0.80f)
                        alpha = x
                        val sc = 0.88f + 0.12f * ease(x)
                        scaleX = sc; scaleY = sc
                        translationY = (1f - ease(x)) * 14.dp.toPx()
                    },
                )
            }
        }
    }
}

// ============================ Login ============================
@Composable
private fun LoginSteps(step: Int, ar: Boolean) {
    val labels = if (ar) listOf("السيرفر", "المستخدم", "كلمة المرور") else listOf("Server", "User", "Password")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { n, l ->
            val done = n < step
            val on = n == step
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(
                    when { done -> tc(0xFF22C55E); on -> C.primary; else -> tc(0x22FFFFFF) },
                ),
                contentAlignment = Alignment.Center,
            ) { RText(if (done) "✓" else "${n + 1}", 14, if (done) tc(0xFF06210F) else Color.White, bold = true) }
            BodyText(l, 13, if (on || done) C.text else C.textDim)
            if (n < labels.lastIndex) Box(Modifier.weight(1f).height(2.dp).background(tc(0x22FFFFFF)))
        }
    }
}

@Composable
fun LoginScreen(vm: AppViewModel) {
    val saved = vm.savedAccount
    var server by rememberSaveable { mutableStateOf(saved?.server ?: "") }
    var user by rememberSaveable { mutableStateOf(saved?.user ?: "") }
    var pass by rememberSaveable { mutableStateOf(saved?.pass ?: "") }
    val ar = vm.lang == "ar"
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(tc(0xFF3B1A86), C.bg), radius = 1500f),
        ),
    ) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val tv = remember { isTvDevice(ctx) }
        val compact = isCompact()
        val step = if (server.isBlank()) 0 else if (user.isBlank()) 1 else if (pass.isBlank()) 2 else 3
        val form: @Composable (Modifier) -> Unit = { mod ->
            Column(
                mod.clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(tc(0x1AFFFFFF), tc(0x0AFFFFFF))))
                    .border(1.dp, tc(0x55C4A1FF), RoundedCornerShape(28.dp))
                    .padding(if (compact) 18.dp else 30.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RText(t("login_title"), 30, C.text, bold = true, modifier = Modifier.weight(1f))
                    Chip("العربية", vm.lang == "ar") { vm.changeLang("ar") }
                    Chip("English", vm.lang == "en") { vm.changeLang("en") }
                }
                LoginSteps(step.coerceAtMost(2), ar)
                AyhamField(server, { server = it }, t("server"), if (tv) Modifier.autoFocus() else Modifier, icon = androidx.compose.material.icons.Icons.Filled.Language)
                BodyText(t("server_hint"), 12, C.textDim)
                AyhamField(user, { user = it }, t("username"), icon = androidx.compose.material.icons.Icons.Filled.Person)
                AyhamField(pass, { pass = it }, t("password"), password = true, imeAction = ImeAction.Done, icon = androidx.compose.material.icons.Icons.Filled.Lock) {
                    vm.connect(server, user, pass)
                }
                val err = vm.error
                if (err != null) BodyText(err, 14, C.danger)
                GlowButton(t("connect"), primary = true, height = 54.dp, modifier = Modifier.fillMaxWidth()) {
                    vm.connect(server, user, pass)
                }
            }
        }
        val logo: @Composable (Modifier) -> Unit = { mod ->
            Column(
                mod,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            ) {
                Image(
                    painterResource(R.drawable.ic_launcher), null,
                    Modifier.size(if (compact) 96.dp else 170.dp),
                )
                RText("Ayham Player", if (compact) 24 else 40, C.text, bold = true)
                BodyText(if (ar) "كل ما تحب مشاهدته في مكان واحد" else "Everything you love to watch, in one place", if (compact) 13 else 17, C.textDim)
                ThemeState.palette.tagline?.let { RText(it, if (compact) 16 else 22, C.gold, bold = true) }
                ThemeState.palette.taglineSub?.let { BodyText(it, if (compact) 11 else 14, C.textDim) }
                // رمز QR فقط للتلفاز (تدخل البيانات من هاتفك)
                if (tv) { Spacer(Modifier.height(10.dp)); QrLoginCard { s, u, p -> vm.connect(s, u, p) } }
            }
        }
        if (compact) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            ) {
                logo(Modifier)
                form(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxSize().padding(40.dp), verticalAlignment = Alignment.CenterVertically) {
                logo(Modifier.weight(1f))
                form(Modifier.weight(1f))
            }
        }
    }
}

// ============================ Loading ============================
@Composable
fun LoadingScreen(vm: AppViewModel) {
    // بدون كلمات: شعار نابض فقط حتى يبدو الدخول فوريًا
    val inf = androidx.compose.animation.core.rememberInfiniteTransition(label = "ld")
    val sc by inf.animateFloat(
        0.94f, 1.06f,
        androidx.compose.animation.core.infiniteRepeatable(tween(700), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(tc(0xFF3B1A86), C.bg), radius = 1300f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.ic_launcher), null, Modifier.size(120.dp).graphicsLayer { scaleX = sc; scaleY = sc })
            ThemeState.palette.tagline?.let { RText(it, 18, C.gold, bold = true) }
        }
        StripeBar(Modifier.align(Alignment.TopCenter))
    }
}

// ============================ Main ============================
@Composable
fun MainScreen(vm: AppViewModel) {
    val compact = isCompact()
    val content: @Composable (Modifier) -> Unit = { mod ->
        Box(mod) {
            androidx.compose.runtime.key(vm.tab) {
                val fade = remember { androidx.compose.animation.core.Animatable(0f) }
                LaunchedEffect(Unit) { fade.animateTo(1f, androidx.compose.animation.core.tween(240)) }
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = fade.value; translationY = (1f - fade.value) * 18f }) {
                when (vm.tab) {
                    0 -> HomeTab(vm)
                    1 -> LiveTab(vm)
                    2 -> BrowseTab(vm, Kind.MOVIE)
                    3 -> BrowseTab(vm, Kind.SERIES)
                    4 -> SearchTab(vm)
                    5 -> LibraryTab(vm)
                    7 -> ItemsPage(vm, vm.recent, "continue_empty")
                    8 -> ItemsPage(vm, vm.history.distinctBy { it.key }, "history_empty")
                    9 -> SubsPage(vm)
                    else -> SettingsTab(vm)
                }
                }
            }
        }
    }
    if (compact) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(C.bg2, C.bg))).statusBarsPadding()) {
            content(Modifier.weight(1f).fillMaxWidth())
            BottomNav(vm)
        }
    } else {
        val dir = androidx.compose.ui.platform.LocalLayoutDirection.current
        val sideReq = remember { androidx.compose.ui.focus.FocusRequester() }
        // القنوات والأفلام والمسلسلات تُفتح بالشاشة الكاملة بدون الشريط الجانبي، والرجوع يعيد للرئيسية والشريط
        val full = vm.tab in 1..3
        var focusSideNext by remember { mutableStateOf(false) }
        AnnPopup(vm)
        WhatsNewPopup(vm)
        TonightDialog(vm)
        androidx.activity.compose.BackHandler(enabled = vm.miniItem == null && (full || vm.tab != 0)) {
            focusSideNext = true
            vm.selectTab(0)
        }
        LaunchedEffect(vm.tab, focusSideNext) {
            if (focusSideNext && !full) {
                delay(160)
                runCatching { sideReq.requestFocus() }
                focusSideNext = false
            }
        }
        // تخفيف تكرار أزرار الاتجاهات عند الضغط المستمر حتى لا تتراكم حركات التمرير ويثقل التطبيق
        val lastKey = remember { longArrayOf(0L) }
        val throttle = Modifier.onPreviewKeyEvent { ev ->
            val code = ev.nativeKeyEvent.keyCode
            val dpad = code == android.view.KeyEvent.KEYCODE_DPAD_UP || code == android.view.KeyEvent.KEYCODE_DPAD_DOWN ||
                code == android.view.KeyEvent.KEYCODE_DPAD_LEFT || code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT
            if (dpad && ev.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && ev.nativeKeyEvent.repeatCount > 0) {
                val now = android.os.SystemClock.uptimeMillis()
                if (now - lastKey[0] < 130) true else { lastKey[0] = now; false }
            } else false
        }
        val bg = Brush.verticalGradient(listOf(Color.Black, tc(0xFF06020D)))
        if (full) {
            Box(Modifier.fillMaxSize().background(bg)) { content(Modifier.fillMaxSize().then(throttle)); StripeBar(Modifier.align(Alignment.TopCenter)) }
        } else {
            Box(Modifier.fillMaxSize().background(bg)) {
                content(Modifier.fillMaxSize().then(if (vm.tab == 0) Modifier else Modifier.padding(top = 88.dp)).then(throttle))
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
                    Box(Modifier.align(Alignment.TopCenter)) { TvTopNav(vm, sideReq) }
                }
                StripeBar(Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

private val navEntries = listOf(
    NavEntry(Icons.Filled.Home, "home"),
    NavEntry(Icons.Filled.LiveTv, "live"),
    NavEntry(Icons.Filled.Movie, "movies"),
    NavEntry(Icons.Filled.Tv, "series"),
    NavEntry(Icons.Filled.Search, "search"),
    NavEntry(Icons.Filled.VideoLibrary, "library"),
    NavEntry(Icons.Filled.Settings, "settings", 6),
)

/** شريط تنقل سفلي للهاتف. */
@Composable
fun BottomNav(vm: AppViewModel) {
    Row(
        Modifier.fillMaxWidth().background(tc(0xFF140A26)).navigationBarsPadding().padding(vertical = 4.dp),
    ) {
        navEntries.forEachIndexed { i0, e ->
            val i = if (e.tab >= 0) e.tab else i0
            val sel = vm.tab == i
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (sel) tc(0x448B5CF6) else Color.Transparent)
                    .clickable { vm.selectTab(i) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(e.icon, null, tint = if (sel) C.accent else C.textDim, modifier = Modifier.size(22.dp))
                RText(t(e.key), 10, if (sel) C.text else C.textDim, bold = sel, maxLines = 1)
            }
        }
    }
}

private data class NavEntry(val icon: ImageVector, val key: String, val tab: Int = -1)

@Composable
fun NavRail(vm: AppViewModel) {
    val entries = navEntries
    Column(
        Modifier.width(104.dp).fillMaxHeight()
            .background(Brush.verticalGradient(listOf(tc(0x552A1850), tc(0x330B0614))))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        entries.forEachIndexed { i0, e ->
            val i = if (e.tab >= 0) e.tab else i0
            val sel = vm.tab == i
            Focusable(
                Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), 1.05f,
                onClick = { vm.selectTab(i) },
            ) { f ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(
                            when {
                                f -> C.surfaceHi
                                sel -> tc(0x558B5CF6)
                                else -> Color.Transparent
                            },
                        )
                        .padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(e.icon, null, tint = if (sel || f) C.accent else C.textDim, modifier = Modifier.size(26.dp))
                    RText(t(e.key), 14, if (sel || f) C.text else C.textDim, bold = sel, maxLines = 1)
                }
            }
        }
    }
}

// ============================ Sections ============================
private fun loadKey(k: Kind) = when (k) { Kind.LIVE -> "load_live"; Kind.MOVIE -> "load_movies"; Kind.SERIES -> "load_series" }
private fun nameKey(k: Kind) = when (k) { Kind.LIVE -> "live"; Kind.MOVIE -> "movies"; Kind.SERIES -> "series" }

@Composable
fun SectionLoader(vm: AppViewModel, kind: Kind) {
    val st = vm.stateOf(kind)
    Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (st) {
                LoadState.LOADING -> {
                    val inf = androidx.compose.animation.core.rememberInfiniteTransition(label = "loadGlow")
                    val gA by inf.animateFloat(0.2f, 0.75f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1300), androidx.compose.animation.core.RepeatMode.Reverse), label = "gA")
                    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(120.dp).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(C.primary.copy(alpha = gA), androidx.compose.ui.graphics.Color.Transparent)), androidx.compose.foundation.shape.CircleShape))
                        CircularProgressIndicator(color = C.accent, strokeWidth = 4.dp, modifier = Modifier.size(50.dp))
                    }
                    RText(t(loadKey(kind)) + "...", 26, C.text, bold = true)
                    val n = vm.sectionCount[kind] ?: 0
                    if (n > 0) RText("${t("loaded_count")}  $n", 22, C.primaryLight)
                    GlowButton(t("cancel"), modifier = Modifier.autoFocus()) { vm.cancelSection(kind) }
                }
                LoadState.FAILED -> {
                    RText(t("load_failed"), 24, C.danger)
                    GlowButton(t("retry"), primary = true, modifier = Modifier.autoFocus()) { vm.loadSection(kind) }
                }
                else -> {
                    RText(t("section_not_loaded"), 24, C.textDim)
                    GlowButton(t(loadKey(kind)), primary = true, modifier = Modifier.autoFocus()) { vm.loadSection(kind) }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SetupCard(vm: AppViewModel) {
    val kinds = listOf(Kind.LIVE, Kind.MOVIE, Kind.SERIES)
    if (kinds.all { vm.stateOf(it) == LoadState.DONE }) return
    Column(
        Modifier.fillMaxWidth().padding(if (isCompact()) 14.dp else 28.dp).clip(RoundedCornerShape(20.dp))
            .background(tc(0x33000000)).padding(if (isCompact()) 16.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RText(t("setup_title"), if (isCompact()) 20 else 28, C.text, bold = true)
        BodyText(t("setup_hint"), 14, C.textDim)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            kinds.forEachIndexed { i, k ->
                val st = vm.stateOf(k)
                if (st != LoadState.DONE) {
                    val label = when (st) {
                        LoadState.LOADING -> t(nameKey(k)) + "  " + (vm.sectionCount[k] ?: 0)
                        LoadState.FAILED -> t("retry") + "  " + t(nameKey(k))
                        else -> t(loadKey(k))
                    }
                    GlowButton(label, primary = st == LoadState.IDLE, modifier = if (i == 0) Modifier.autoFocus() else Modifier) {
                        if (st == LoadState.LOADING) vm.cancelSection(k) else vm.loadSection(k)
                    }
                }
            }
        }
    }
}

// ============================ Home ============================
@Composable
fun HeroBanner(vm: AppViewModel, item: Item) {
    var details by remember(item.key) { mutableStateOf<Details?>(null) }
    LaunchedEffect(item.key) {
        details = if (item.kind == Kind.MOVIE) vm.movieDetails(item) else null
    }
    val bg = details?.backdrop.orEmpty().ifEmpty { item.image }
    var imdb by remember(item.key) { mutableStateOf<String?>(null) }
    LaunchedEffect(item.key, details != null) {
        if (details != null) imdb = vm.imdbRating(item, details?.year.orEmpty())
    }
    val compact = isCompact()
    Box(Modifier.fillMaxWidth().height(if (compact) 230.dp else 310.dp)) {
        Img(bg, Modifier.fillMaxSize(), w = 600, h = 340)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(tc(0xF00B0614), tc(0x990B0614), tc(0x220B0614))),
            ),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(colorStops = arrayOf(0.6f to Color.Transparent, 1f to C.bg2)),
            ),
        )
        Column(
            if (compact) Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth()
            else Modifier.align(Alignment.CenterStart).padding(horizontal = 36.dp).width(560.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (compact) RText(item.name, 24, C.text, bold = true, maxLines = 2, lineHeight = 30)
            else RText(item.name, 40, C.text, bold = true, maxLines = 2, lineHeight = 48)
            val meta = listOfNotNull(
                details?.year?.takeIf { it.isNotBlank() },
                details?.genre?.takeIf { it.isNotBlank() },
                item.rating.takeIf { it.isNotBlank() && it != "0" }?.let { "★ $it" },
            ).joinToString("  •  ")
            imdb?.let { ImdbBadge(it) }
            if (meta.isNotEmpty()) BodyText(meta, 14, C.primaryLight, bold = true, maxLines = 1)
            val plot = details?.plot.orEmpty()
            if (plot.isNotBlank() && !compact) BodyText(plot, 14, C.textDim, maxLines = 3)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlowButton(t("play"), primary = true) {
                    if (item.kind == Kind.MOVIE) vm.playMovie(item) else vm.openDetail(item)
                }
                GlowButton(t("about")) { vm.openDetail(item) }
            }
        }
    }
}

// ============================ Live ============================
@Composable
fun LiveTab(vm: AppViewModel) {
    if (vm.stateOf(Kind.LIVE) != LoadState.DONE) {
        SectionLoader(vm, Kind.LIVE)
        return
    }
    if (!isCompact()) { TvLive(vm); return }
    var sel by rememberSaveable { mutableStateOf("all") }
    val cats = vm.categories[Kind.LIVE].orEmpty()
    val favLive = remember(vm.favorites) { vm.favorites.filter { it.kind == Kind.LIVE } }
    val recentLive = remember(vm.history) { vm.historyOf(Kind.LIVE) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val favAdded = t("fav_added")
    val favRemoved = t("fav_removed")
    val toggleFav: (Item) -> Unit = { ch ->
        val was = vm.isIn("fav", ch)
        vm.toggle("fav", ch)
        android.widget.Toast.makeText(ctx, if (was) favRemoved else favAdded, android.widget.Toast.LENGTH_SHORT).show()
    }
    var q by rememberSaveable { mutableStateOf("") }
    var found by remember { mutableStateOf<List<Item>>(emptyList()) }
    val searching = q.trim().length >= 2
    LaunchedEffect(q, vm.dataVersion) {
        delay(350)
        found = vm.searchKind(Kind.LIVE, q, 150)
    }
    val source = if (searching) LiveSource(favs = found)
        else if (sel == "fav") LiveSource(favs = favLive)
        else if (sel == "recent") LiveSource(favs = recentLive)
        else LiveSource(cat = if (sel == "all") null else sel)
    val pager = rememberPager(listOf(sel, vm.dataVersion, favLive.size, recentLive.size), 80) { offset, limit ->
        when (sel) {
            "fav" -> favLive.drop(offset).take(limit)
            "recent" -> recentLive.drop(offset).take(limit)
            "all" -> vm.allItems(Kind.LIVE, limit, offset)
            else -> vm.inCategory(Kind.LIVE, sel, limit, offset)
        }
    }
    val shownList = if (searching) found else pager.items
    val mini = vm.miniItem
    val pick: (Item) -> Unit = { ch ->
        // لو فيه قناة بالشاشة المصغّرة: اختيار قناة أخرى يبدّلها، واختيار نفس القناة يكبّرها
        if (mini != null && mini.id != ch.id) vm.playMini(ch, source) else vm.playLive(ch, source)
    }
    val compact = isCompact()
    val body: @Composable (Modifier) -> Unit = { mod -> Row(mod) {
        if (!compact) LazyColumn(
            Modifier.width(240.dp).fillMaxHeight(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { AyhamField(q, { q = it }, t("search_in_live"), imeAction = ImeAction.Search) }
            item { PanelItem(t("all"), sel == "all" && !searching, "${vm.countOf(Kind.LIVE)} ${t("channels")}") { sel = "all"; q = "" } }
            item { PanelItem(t("favorite_channels"), sel == "fav" && !searching, "${favLive.size}") { sel = "fav"; q = "" } }
            item { PanelItem(t("recent_live"), sel == "recent" && !searching, "${recentLive.size}") { sel = "recent"; q = "" } }
            items(cats, key = { it.id }) { c -> PanelItem(c.name, sel == c.id && !searching, "${c.count}") { sel = c.id; q = "" } }
        }
        if (vm.liveLayout == "list" || compact) {
            LazyColumn(
                Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(shownList, key = { _, it -> it.key }) { index, ch ->
                    if (!searching && index >= pager.items.size - 20) LaunchedEffect(pager.items.size) { pager.loadMore() }
                    ChannelRow(ch, mini?.id == ch.id, fav = vm.isIn("fav", ch), epg = rememberEpgNow(vm, ch.id), onLongClick = { toggleFav(ch) }) { pick(ch) }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (mini != null && !compact) 200.dp else 240.dp),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                gridItemsIndexed(shownList, key = { _, it -> it.key }) { index, ch ->
                    if (!searching && index >= pager.items.size - 20) LaunchedEffect(pager.items.size) { pager.loadMore() }
                    ChannelCard(ch, w = if (mini != null) 200.dp else 230.dp, fav = vm.isIn("fav", ch), epg = rememberEpgNow(vm, ch.id), onLongClick = { toggleFav(ch) }) { pick(ch) }
                }
            }
        }
        if (mini != null && !compact) MiniPlayer(vm, mini)
    } }
    if (compact) {
        Column(Modifier.fillMaxSize()) {
            AyhamField(q, { q = it }, t("search_in_live"), Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp), imeAction = ImeAction.Search)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Chip(t("all"), sel == "all") { sel = "all"; q = "" } }
                item { Chip(t("favorite_channels"), sel == "fav") { sel = "fav"; q = "" } }
                item { Chip(t("recent_live"), sel == "recent") { sel = "recent"; q = "" } }
                items(cats, key = { it.id }) { c -> Chip(c.name, sel == c.id) { sel = c.id; q = "" } }
            }
            if (mini != null) MiniPlayer(vm, mini)
            body(Modifier.weight(1f).fillMaxWidth())
        }
    } else {
        body(Modifier.fillMaxSize())
    }
}

// ============================ Movies / Series ============================
@Composable
fun BrowseTab(vm: AppViewModel, kind: Kind) {
    val isMovie = kind == Kind.MOVIE
    if (vm.stateOf(kind) != LoadState.DONE) {
        SectionLoader(vm, kind)
        return
    }
    // التلفاز: نفس أسلوب الرئيسية (خلفية كبيرة تتبع التركيز)
    if (!isCompact()) { TvBrowse(vm, kind); return }
    val cats = vm.categories[kind].orEmpty()
    var sel by rememberSaveable(kind.name) { mutableStateOf<String?>(null) }
    var q by rememberSaveable(kind.name + "_q") { mutableStateOf("") }
    var found by remember(kind) { mutableStateOf<List<Item>>(emptyList()) }
    val searching = q.trim().length >= 2
    LaunchedEffect(q, vm.dataVersion) {
        delay(350)
        found = vm.searchKind(kind, q)
    }
    var recentItems by remember(kind, vm.dataVersion) { mutableStateOf<List<Item>>(emptyList()) }
    var topItems by remember(kind, vm.dataVersion) { mutableStateOf<List<Item>>(emptyList()) }
    LaunchedEffect(kind, vm.dataVersion) {
        recentItems = vm.recentItems(kind)
        topItems = if (isMovie) vm.topItems(kind) else emptyList()
    }
    val featured = remember(recentItems) { recentItems.take(8).randomOrNull() }
    val compact = isCompact()
    val hpad = if (compact) 14.dp else 28.dp
    val asList = vm.vodLayout == "list"
    val bases = remember(vm.history, kind) { vm.historyOf(kind).take(2) }
    var similar by remember(kind) { mutableStateOf<List<Pair<Item, List<Item>>>>(emptyList()) }
    LaunchedEffect(bases, vm.dataVersion) { similar = bases.map { it to vm.similarTo(it) } }
    val watchedHere = remember(vm.history, kind) { vm.historyOf(kind) }

    Column(Modifier.fillMaxSize()) {
        AyhamField(
            q, { q = it }, t("search_in_" + kind.name.lowercase()),
            Modifier.padding(start = hpad, end = hpad, top = 10.dp),
            imeAction = ImeAction.Search,
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = hpad, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Chip(t("all"), sel == null) { sel = null; q = "" } }
            items(cats, key = { it.id }) { c -> Chip(c.name, sel == c.id) { sel = c.id; q = "" } }
        }
        val current = sel
        if (searching) {
            if (found.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    RText(t("no_results"), 22, C.textDim)
                }
            } else if (asList) {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = hpad, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(found, key = { it.key }) { item -> VodRow(item) { vm.openDetail(item) } }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (compact) 112.dp else 138.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = hpad, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(found.size, key = { found[it].key }) { i ->
                        PosterCard(found[i], w = if (compact) 112.dp else 128.dp, h = if (compact) 168.dp else 192.dp) { vm.openDetail(found[i]) }
                    }
                }
            }
        } else if (current == null && !asList) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 40.dp)) {
                similar.forEach { (base, list) ->
                    if (list.size >= 3) item(key = "sim_" + base.key) { ContentRow(t("because_watched") + " " + base.name, list) { vm.openDetail(it) } }
                }
                if (watchedHere.isNotEmpty()) item { ContentRow(t(if (isMovie) "recent_movies_w" else "recent_series_w"), watchedHere) { vm.openDetail(it) } }
                item { ContentRow(t(if (isMovie) "recent_movies" else "recent_series"), recentItems) { vm.openDetail(it) } }
                if (isMovie) item { ContentRow(t("top_movies"), topItems) { vm.openDetail(it) } }
                items(cats, key = { it.id }, contentType = { "catrow" }) { c ->
                    LazyCategoryRow(c.name, vm.dataVersion, { vm.inCategory(kind, c.id, 16, 0) }) { vm.openDetail(it) }
                }
            }
        } else {
            val pager = rememberPager(Triple(kind, current, vm.dataVersion), 60) { offset, limit ->
                if (current == null) vm.allItems(kind, limit, offset) else vm.inCategory(kind, current, limit, offset)
            }
            if (asList) {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = hpad, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(pager.items, key = { _, it -> it.key }) { index, item ->
                        if (index >= pager.items.size - 20) LaunchedEffect(pager.items.size) { pager.loadMore() }
                        VodRow(item) { vm.openDetail(item) }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (compact) 112.dp else 138.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = hpad, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    gridItemsIndexed(pager.items, key = { _, it -> it.key }) { index, item ->
                        if (index >= pager.items.size - 20) LaunchedEffect(pager.items.size) { pager.loadMore() }
                        PosterCard(item, w = if (compact) 112.dp else 128.dp, h = if (compact) 168.dp else 192.dp) { vm.openDetail(item) }
                    }
                }
            }
        }
    }
}

// ============================ Search ============================
@Composable
fun SearchTab(vm: AppViewModel) {
    val ar = vm.lang == "ar"
    var q by rememberSaveable { mutableStateOf("") }
    var res by remember { mutableStateOf(Triple(emptyList<Item>(), emptyList<Item>(), emptyList<Item>())) }
    var typeF by rememberSaveable { mutableIntStateOf(0) }
    var ratingF by rememberSaveable { mutableIntStateOf(0) }
    var recentQ by remember { mutableStateOf(vm.store.recentQueries()) }
    var smart by remember { mutableStateOf<List<Item>>(emptyList()) }
    LaunchedEffect(q) {
        smart = emptyList()
        if (q.trim().length < 6) return@LaunchedEffect
        delay(800)
        smart = vm.smartSearch(q)
    }
    LaunchedEffect(q) {
        delay(450)
        res = vm.search(q)
    }
    LaunchedEffect(q) {
        delay(1800)
        if (q.trim().length >= 3) { vm.store.addQuery(q); recentQ = vm.store.recentQueries() }
    }
    val searching = q.trim().length >= 2
    val live = if (typeF == 0 || typeF == 1) res.first else emptyList()
    val movies = (if (typeF == 0 || typeF == 2) res.second else emptyList()).filter { ratingF == 0 || (it.rating.toFloatOrNull() ?: 0f) >= ratingF }
    val series = (if (typeF == 0 || typeF == 3) res.third else emptyList()).filter { ratingF == 0 || (it.rating.toFloatOrNull() ?: 0f) >= ratingF }
    val open: (Item) -> Unit = { it -> if (it.kind == Kind.LIVE) vm.playLive(it, LiveSource(favs = live)) else vm.openDetail(it) }
    val mostWatched = remember(vm.history) {
        vm.history.groupingBy { it.key }.eachCount().entries.sortedByDescending { it.value }.take(10)
            .mapNotNull { e -> vm.history.firstOrNull { it.key == e.key } }
    }
    val hpad = if (isCompact()) 14.dp else 28.dp
    Column(Modifier.fillMaxSize()) {
        AyhamField(
            q, { q = it }, t("search_hint"),
            Modifier.padding(horizontal = hpad, vertical = if (isCompact()) 10.dp else 18.dp),
            imeAction = ImeAction.Search,
        )
        if (searching) {
            // فلاتر سريعة
            val types = listOf(t("all"), t("live"), t("movies"), t("series"))
            LazyRow(contentPadding = PaddingValues(horizontal = hpad), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(types.size) { i -> Chip(types[i], typeF == i) { typeF = i } }
                item { Chip(if (ar) "كل التقييمات" else "Any rating", ratingF == 0) { ratingF = 0 } }
                item { Chip("7+", ratingF == 7) { ratingF = 7 } }
                item { Chip("8+", ratingF == 8) { ratingF = 8 } }
            }
        }
        val empty = live.isEmpty() && movies.isEmpty() && series.isEmpty() && smart.isEmpty()
        if (empty && searching) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                RText(t("no_results"), 24, C.textDim)
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
            if (!searching) {
                if (recentQ.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = hpad, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RText(if (ar) "آخر ما بحثت عنه" else "Recent searches", 18, C.text, bold = true)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(recentQ.size) { i -> Chip(recentQ[i], false) { q = recentQ[i] } }
                                item { Chip(if (ar) "مسح" else "Clear", false) { vm.store.clearQueries(); recentQ = emptyList() } }
                            }
                        }
                    }
                }
                if (mostWatched.isNotEmpty()) {
                    item { ContentRow(if (ar) "الأكثر مشاهدة" else "Most watched", mostWatched) { open(it) } }
                }
            } else {
                // أفضل نتيجة بشكل كبير
                val best = series.firstOrNull() ?: movies.firstOrNull() ?: live.firstOrNull()
                if (best != null) {
                    item {
                        val shape = RoundedCornerShape(18.dp)
                        Focusable(Modifier.padding(horizontal = hpad, vertical = 10.dp).fillMaxWidth(), shape, 1.02f, onClick = { open(best) }) { f ->
                            Row(
                                Modifier.fillMaxWidth().clip(shape).background(if (f) C.surfaceHi else C.surface).padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Img(best.image, Modifier.width(110.dp).height(160.dp).clip(RoundedCornerShape(12.dp)), w = 220, h = 320)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    RText(if (ar) "أفضل نتيجة" else "Top result", 14, C.accent, bold = true)
                                    RText(best.name, 26, Color.White, bold = true, maxLines = 2)
                                    val kindL = when (best.kind) { Kind.LIVE -> t("live"); Kind.MOVIE -> t("movies"); else -> t("series") }
                                    BodyText(kindL + (best.rating.takeIf { it.isNotBlank() && it != "0" }?.let { "  ·  ★ $it" } ?: ""), 15, C.textDim)
                                }
                            }
                        }
                    }
                }
            }
            if (searching && smart.isNotEmpty()) item { ContentRow(if (ar) "اقتراحات ذكية من مكتبتك" else "Smart picks from your library", smart) { open(it) } }
            item { ContentRow(t("live"), live) { vm.playLive(it, LiveSource(favs = live)) } }
            item { ContentRow(t("movies"), movies) { vm.openDetail(it) } }
            item { ContentRow(t("series"), series) { vm.openDetail(it) } }
        }
    }
}

// ============================ Library ============================
@Composable
fun LibraryTab(vm: AppViewModel) {
    val none = vm.recent.isEmpty() && vm.favorites.isEmpty() && vm.later.isEmpty() && vm.watched.isEmpty() && vm.history.isEmpty()
    if (none) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            RText(t("empty_list"), 26, C.textDim)
        }
        return
    }
    val favLive = vm.favorites.filter { it.kind == Kind.LIVE }
    val favVod = vm.favorites.filter { it.kind != Kind.LIVE }
    val recentLive = vm.historyOf(Kind.LIVE)
    val playLive: (Item, List<Item>) -> Unit = { it, src -> vm.playLive(it, LiveSource(favs = src)) }
    LazyColumn(contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp)) {
        item { ContentRow(t("continue_watching"), vm.recent) { vm.openDetail(it) } }
        item { ContentRow(t("favorite_channels"), favLive) { playLive(it, favLive) } }
        item { ContentRow(t("recent_live"), recentLive) { playLive(it, recentLive) } }
        item { ContentRow(t("recent_movies_w"), vm.historyOf(Kind.MOVIE)) { vm.openDetail(it) } }
        item { ContentRow(t("recent_series_w"), vm.historyOf(Kind.SERIES)) { vm.openDetail(it) } }
        item { ContentRow(t("favorites"), favVod) { vm.openDetail(it) } }
        item { ContentRow(t("watch_later"), vm.later) { vm.openDetail(it) } }
        item { ContentRow(t("watched"), vm.watched) { vm.openDetail(it) } }
    }
}

// ============================ Settings ============================
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun SettingsTab(vm: AppViewModel) {
    val t_hist = t("history_cleared")
    var cat by remember { mutableIntStateOf(0) }
    val ar = vm.lang == "ar"
    val cats = listOf(
        if (ar) "الملف الشخصي" else "Profile",
        if (ar) "المظهر والعرض" else "Look & layout",
        if (ar) "التشغيل والجودة" else "Playback & quality",
        if (ar) "المحتوى" else "Content",
        if (ar) "الحساب والتطبيق" else "Account & app",
    )
    val shortScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 560
    val padAll = if (isCompact()) 12.dp else if (shortScreen) 16.dp else 32.dp
    Row(Modifier.fillMaxSize().padding(start = padAll, end = padAll, top = if (shortScreen && !isCompact()) 4.dp else padAll, bottom = padAll), horizontalArrangement = Arrangement.spacedBy(if (shortScreen) 16.dp else 24.dp)) {
      Column(Modifier.width(if (isCompact()) 150.dp else if (shortScreen) 230.dp else 270.dp).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(if (shortScreen) 4.dp else 8.dp)) {
        RText(t("settings"), if (isCompact()) 24 else if (shortScreen) 26 else 34, C.text, bold = true)
        if (!shortScreen) BodyText(if (ar) "اختر قسمًا لعرض إعداداته" else "Pick a section", 13, C.textDim)
        Spacer(Modifier.height(if (shortScreen) 2.dp else 6.dp))
        cats.forEachIndexed { n, label ->
            PanelItem(label, selected = cat == n, modifier = if (n == cat) Modifier.autoFocus() else Modifier) { cat = n }
        }
      }
      Column(
        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())
            .clip(RoundedCornerShape(20.dp)).background(tc(0x661C1033)).padding(if (isCompact()) 14.dp else 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {

        if (cat == 0) WatchStatsCard(vm)
        if (cat == 0) {
            var open by remember { mutableStateOf(false) }
            GlowButton(if (ar) "سجل التحديثات" else "Update history") { open = !open }
            if (open) com.ayham.player.data.releaseHistory.forEach { r ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    RText((if (ar) "إصدار " else "Build ") + r.id, 18, C.accent, bold = true)
                    (if (ar) r.ar else r.en).forEach { BodyText("• $it", 14, C.text, maxLines = 3) }
                }
            }
        }
        if (cat == 0 && vm.ann != null) GlowButton(if (ar) "عرض الإعلان" else "Show announcement") { vm.openAnn() }

        // ---- الملف الشخصي ----
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val tvDevice = remember { isTvDevice(ctx) }
        var showQr by remember { mutableStateOf(false) }
        val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        ) { uri: android.net.Uri? ->
            if (uri != null) {
                val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                if (bytes != null) vm.setPhoto(bytes)
            }
        }
        if (cat == 0) {
        RText(t("profile"), 22, C.primaryLight, bold = true)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(vm, 72.dp)
            AyhamField(vm.profileName, { vm.changeProfileName(it) }, t("profile_name"), Modifier.weight(1f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            avatarPresets.indices.forEach { i ->
                Focusable(shape = CircleShape, focusScale = 1.12f, onClick = { vm.changeAvatar(i) }) { _ ->
                    Box(
                        Modifier.then(
                            if (vm.profileAvatar == i) Modifier.border(3.dp, Color.White, CircleShape) else Modifier,
                        ),
                    ) { PresetAvatar(i, 52.dp) }
                }
            }
            GlowButton(t("photo_from_phone")) {
                if (tvDevice) showQr = !showQr else photoPicker.launch(arrayOf("image/*"))
            }
        }
        if (showQr) PhotoQrCard(vm) { showQr = false }

        }
        if (cat == 1) {
        RText(t("language"), 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("العربية", vm.lang == "ar") { vm.changeLang("ar") }
            Chip("English", vm.lang == "en") { vm.changeLang("en") }
        }

        RText(if (ar) "الثيم" else "Theme", 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Ayham Original", ThemeState.id == Themes.ORIGINAL) { vm.changeTheme(Themes.ORIGINAL) }
            Chip("FC Barcelona", ThemeState.id == Themes.BARCELONA) { vm.changeTheme(Themes.BARCELONA) }
            Chip("Dark Mode", ThemeState.id == Themes.DARK) { vm.changeTheme(Themes.DARK) }
            Chip("Light Mode", ThemeState.id == Themes.LIGHT) { vm.changeTheme(Themes.LIGHT) }
            Chip("Ayham", ThemeState.id == Themes.AYHAM) { vm.changeTheme(Themes.AYHAM) }
            Chip("Royal White", ThemeState.id == Themes.ROYAL) { vm.changeTheme(Themes.ROYAL) }
        }
        ThemeState.palette.tagline?.let { tl ->
            RText(tl, 20, C.gold, bold = true)
            ThemeState.palette.taglineSub?.let { BodyText(it, 15, C.textDim) }
        }

        RText(if (ar) "مباراة برشلونة القادمة" else "Next Barcelona match", 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(if (ar) "إظهار في الرئيسية" else "Show on Home", vm.matchCard || ThemeState.id == Themes.BARCELONA) { vm.changeMatchCard(!vm.matchCard) }
        }
        if (ThemeState.id == Themes.BARCELONA) BodyText(if (ar) "تظهر دائمًا مع ثيم برشلونة" else "Always shown with the Barcelona theme", 13, C.textDim)

        RText(t("live_layout"), 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(t("layout_grid"), vm.liveLayout == "grid") { vm.changeLiveLayout("grid") }
            Chip(t("layout_list"), vm.liveLayout == "list") { vm.changeLiveLayout("list") }
        }

        RText(if (ar) "تفاصيل الساعة (الرئيسية)" else "Clock details (Home)", 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip((if (ar) "الطقس: " else "Weather: ") + (if (vm.clockWeather) (if (ar) "ظاهر" else "On") else (if (ar) "مخفي" else "Off")), vm.clockWeather) { vm.changeClockWeather(!vm.clockWeather) }
            Chip((if (ar) "المنطقة: " else "Region: ") + (if (vm.clockRegion) (if (ar) "ظاهرة" else "On") else (if (ar) "مخفية" else "Off")), vm.clockRegion) { vm.changeClockRegion(!vm.clockRegion) }
        }
        if (vm.clockWeather || vm.clockRegion) {
            BodyText(if (ar) "اختر مدينتك:" else "Choose your city:", 14, C.textDim)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                com.ayham.player.data.Weather.cities.forEachIndexed { i, c ->
                    Chip(if (ar) c.ar else c.en, vm.clockCity == i) { vm.changeClockCity(i) }
                }
            }
        }

        RText(if (ar) "أصوات التنقل" else "Navigation sounds", 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(if (ar) "تشغيل" else "On", vm.uiSounds) { vm.changeUiSounds(true) }
            Chip(if (ar) "إيقاف" else "Off", !vm.uiSounds) { vm.changeUiSounds(false) }
        }
        if (vm.uiSounds) FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(if (ar) "الافتراضي" else "Default", vm.uiSoundPack == 0) { vm.changeUiSoundPack(0) }
            Chip(if (ar) "غَيم" else "Ghaym (clouds)", vm.uiSoundPack == 1) { vm.changeUiSoundPack(1) }
            Chip(if (ar) "مطر" else "Rain", vm.uiSoundPack == 2) { vm.changeUiSoundPack(2) }
        }

        RText(t("vod_layout"), 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(t("layout_posters"), vm.vodLayout == "grid") { vm.changeVodLayout("grid") }
            Chip(t("layout_list"), vm.vodLayout == "list") { vm.changeVodLayout("list") }
        }

        }
        if (cat == 2) {
            Chip((if (vm.lang == "ar") "شاشة التوقف السينمائية: " else "Cinematic screensaver: ") + (if (vm.saverMin == 0) (if (vm.lang == "ar") "مغلقة" else "Off") else (if (vm.lang == "ar") "بعد ${vm.saverMin} د" else "after ${vm.saverMin} min")), vm.saverMin != 0) {
                vm.changeSaverMin(when (vm.saverMin) { 0 -> 1; 1 -> 2; 2 -> 5; else -> 0 })
            }
            Chip((if (vm.lang == "ar") "تشغيل آخر قناة عند فتح التطبيق: " else "Resume last channel on launch: ") + (if (vm.resumeLive) (if (vm.lang == "ar") "مفعّل" else "On") else (if (vm.lang == "ar") "مغلق" else "Off")), vm.resumeLive) { vm.changeResumeLive(!vm.resumeLive) }
            Chip((if (vm.lang == "ar") "تقديم وتأخير القنوات المباشرة: " else "Live rewind/forward: ") + (if (vm.liveShift) (if (vm.lang == "ar") "مفعّل" else "On") else (if (vm.lang == "ar") "مغلق" else "Off")), vm.liveShift) { vm.changeLiveShift(!vm.liveShift) }
            SubPreviewCard(vm)
        RText(t("live_format"), 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("m3u8 (HLS)", vm.liveFormat == "m3u8") { vm.changeLiveFormat("m3u8") }
            Chip("ts", vm.liveFormat == "ts") { vm.changeLiveFormat("ts") }
        }

        RText(t("max_quality"), 22, C.primaryLight, bold = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(t("q_4k"), vm.maxRes == 0) { vm.changeMaxRes(0) }
            Chip(t("q_1080"), vm.maxRes == 1080) { vm.changeMaxRes(1080) }
        }

        }
        if (cat == 3) {
        RText(t("sections"), 22, C.primaryLight, bold = true)
        listOf(Kind.LIVE, Kind.MOVIE, Kind.SERIES).forEach { k ->
            val st = vm.stateOf(k)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val status = when (st) {
                    LoadState.DONE -> "${t("loaded")} (${vm.countOf(k)})"
                    LoadState.LOADING -> "${t("loaded_count")} ${vm.sectionCount[k] ?: 0}"
                    else -> t("not_loaded")
                }
                BodyText("${t(nameKey(k))}: $status", 16, C.text, modifier = Modifier.width(260.dp))
                if (st == LoadState.LOADING) {
                    GlowButton(t("cancel")) { vm.cancelSection(k) }
                } else {
                    GlowButton(if (st == LoadState.DONE) t("update") else t(loadKey(k)), primary = st != LoadState.DONE) { vm.loadSection(k) }
                    if (st == LoadState.DONE) GlowButton(t("delete")) { vm.clearSection(k) }
                }
            }
        }

        }
        if (cat == 4) {
        RText(t("account"), 22, C.primaryLight, bold = true)
        val info = vm.accountInfo
        BodyText("${t("server")}: ${vm.savedAccount?.server.orEmpty()}", 15, C.textDim)
        BodyText("${t("username")}: ${vm.savedAccount?.user.orEmpty()}", 15, C.textDim)
        if (info.status.isNotEmpty()) BodyText("${t("status")}: ${info.status}", 15, C.textDim)
        if (info.expDate.isNotEmpty()) BodyText("${t("expires")}: ${info.expDate}", 15, C.textDim)
        if (info.maxConnections.isNotEmpty()) BodyText("${t("connections")}: ${info.maxConnections}", 15, C.textDim)

        if (com.ayham.player.data.LicenseManager.enabled) {
            RText(t("sync_title"), 22, C.primaryLight, bold = true)
            val syncScope = androidx.compose.runtime.rememberCoroutineScope()
            val okMsg = t("sync_done"); val failMsg = t("sync_fail"); val offMsg = t("sync_off")
            GlowButton(t("sync_now")) {
                syncScope.launch {
                    val ok = if (vm.sync.available) vm.sync.run() else null
                    android.widget.Toast.makeText(ctx, when (ok) { true -> okMsg; false -> failMsg; null -> offMsg }, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (com.ayham.player.data.LicenseManager.enabled) {
            val newsScope = androidx.compose.runtime.rememberCoroutineScope()
            GlowButton(t("whats_new")) {
                newsScope.launch { if (vm.newsList.isEmpty()) vm.loadNews(false); vm.openNewsAll() }
            }
        }
        RText(t("check_update"), 22, C.primaryLight, bold = true)
        val upd = vm.updState
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vm.updInfo != null && upd != AppViewModel.UpdState.CHECKING) {
                GlowButton(t("update_now"), primary = true) { vm.updDismissed = false; vm.startUpdate() }
            } else {
                GlowButton(t("check_update")) { vm.checkUpdate(true) }
            }
            GlowButton(t("clear_history")) {
                vm.clearHistory()
                android.widget.Toast.makeText(ctx, t_hist, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        when (upd) {
            AppViewModel.UpdState.CHECKING -> BodyText(t("checking"), 14, C.textDim)
            AppViewModel.UpdState.LATEST -> BodyText(t("up_to_date"), 14, C.ok)
            AppViewModel.UpdState.ERROR -> BodyText(t("update_failed"), 14, C.danger)
            AppViewModel.UpdState.DOWNLOADING -> BodyText(t("downloading") + " " + (vm.updProgress * 100).toInt() + "%", 14, C.text)
            AppViewModel.UpdState.NEED_PERMISSION -> BodyText(t("update_perm"), 14, C.accent)
            else -> {}
        }

        GlowButton(t("logout")) { vm.logout() }

        Spacer(Modifier.height(10.dp))
        if (com.ayham.player.data.Tmdb.enabled) BodyText(t("tmdb_credit"), 13, C.textDim)
        RText("${t("app")}  ${t("version")} ${com.ayham.player.data.Updater.currentBuild(ctx)}", 18, C.textDim)
        }
      }
    }
}

