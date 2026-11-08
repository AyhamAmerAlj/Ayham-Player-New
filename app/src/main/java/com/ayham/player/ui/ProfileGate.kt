@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ayham.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.nativeKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.LicenseManager
import com.ayham.player.data.Profile
import com.ayham.player.data.ProfileUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** يعرض الشاشة المناسبة من شاشات الملفات (اختيار / إدارة / تعديل). */
@Composable
fun ProfileFlow(vm: AppViewModel) {
    when (vm.profileNav.lastOrNull() ?: ProfileUi.PICK) {
        ProfileUi.PICK -> ProfileGate(vm)
        ProfileUi.MANAGE -> ProfileManage(vm)
        ProfileUi.EDIT -> ProfileEditor(vm)
    }
}

// ============================ لوحة الرمز (PIN) ============================
@Composable
private fun PadKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val s = if (isCompact()) 64.dp else 78.dp
    Focusable(modifier.size(s), CircleShape, 1.12f, onClick = onClick) { f ->
        Box(Modifier.fillMaxSize().clip(CircleShape).background(if (f) C.surfaceHi else tc(0x33FFFFFF)), contentAlignment = Alignment.Center) {
            RText(label, 28, Color.White, bold = true)
        }
    }
}

@Composable
fun PinPad(title: String, subtitle: String, accent: Color, error: Boolean, onComplete: (String) -> Unit, onCancel: () -> Unit, extra: (@Composable () -> Unit)? = null) {
    var code by remember { mutableStateOf("") }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (error) {
            repeat(3) { shake.animateTo(16f, tween(45)); shake.animateTo(-16f, tween(45)) }
            shake.animateTo(0f, tween(45))
        }
    }
    fun add(d: String) {
        if (code.length >= 4) return
        val n = code + d
        code = n
        if (n.length == 4) onComplete(n)
    }
    BackHandler { onCancel() }
    Column(
        Modifier.fillMaxSize()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val kc = ev.nativeKeyEvent.keyCode
                when {
                    kc in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9 -> { add((kc - android.view.KeyEvent.KEYCODE_0).toString()); true }
                    kc in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9 -> { add((kc - android.view.KeyEvent.KEYCODE_NUMPAD_0).toString()); true }
                    kc == android.view.KeyEvent.KEYCODE_DEL -> { code = code.dropLast(1); true }
                    else -> false
                }
            }
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        RText("🔒", 34, Color.White)
        RText(title, 26, Color.White, bold = true, maxLines = 2)
        RText(subtitle, 16, if (error) C.danger else C.textDim, maxLines = 2)
        Row(Modifier.graphicsLayer { translationX = shake.value }, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.size(18.dp).clip(CircleShape)
                        .background(if (i < code.length) (if (error) C.danger else accent) else tc(0x44FFFFFF)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
        rows.forEachIndexed { ri, r ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                r.forEachIndexed { ci, d -> PadKey(d, if (ri == 1 && ci == 1) Modifier.autoFocus() else Modifier) { add(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PadKey("⌫") { code = code.dropLast(1) }
            PadKey("0") { add("0") }
            PadKey("✕") { onCancel() }
        }
        extra?.invoke()
    }
}

/** يطلب رمز ملف معيّن: محاولات محدودة، ونسيت الرمز يتم بكود التفعيل. */
@Composable
fun PinGate(vm: AppViewModel, target: Profile, title: String, onOk: () -> Unit, onCancel: () -> Unit) {
    val ar = vm.lang == "ar"
    var attempt by remember { mutableIntStateOf(0) }
    var err by remember { mutableStateOf(false) }
    var fails by remember { mutableIntStateOf(0) }
    var lockedUntil by remember { mutableLongStateOf(0L) }
    var forgot by remember { mutableStateOf(false) }
    if (forgot) {
        ForgotPin(vm, onOk = { vm.clearPin(target.id); onOk() }, onCancel = { forgot = false })
        return
    }
    androidx.compose.runtime.key(attempt) {
        val locked = android.os.SystemClock.elapsedRealtime() < lockedUntil
        PinPad(
            title,
            when {
                locked -> if (ar) "محاولات كثيرة، انتظر 30 ثانية" else "Too many attempts, wait 30 seconds"
                err -> if (ar) "الرمز غير صحيح" else "Wrong code"
                else -> if (ar) "أدخل الرمز المكوّن من 4 أرقام" else "Enter your 4-digit code"
            },
            ProfileLook.c1(target), err,
            onComplete = { code ->
                val now = android.os.SystemClock.elapsedRealtime()
                if (now < lockedUntil) { err = true; attempt++ }
                else if (vm.pinOk(target, code)) onOk()
                else {
                    fails++
                    err = true
                    if (fails >= 5) { lockedUntil = now + 30_000L; fails = 0 }
                    attempt++
                }
            },
            onCancel = onCancel,
            extra = { Chip(if (ar) "نسيت الرمز؟" else "Forgot code?", false) { forgot = true } },
        )
    }
}

@Composable
private fun ForgotPin(vm: AppViewModel, onOk: () -> Unit, onCancel: () -> Unit) {
    val ar = vm.lang == "ar"
    var code by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    BackHandler { onCancel() }
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        RText(if (ar) "استعادة الرمز" else "Recover code", 26, Color.White, bold = true)
        RText(if (ar) "اكتب كود التفعيل الخاص باشتراكك لإزالة الرمز" else "Enter your activation code to remove the code", 16, C.textDim, maxLines = 3)
        AyhamField(code, { code = it; bad = false }, if (ar) "كود التفعيل" else "Activation code", Modifier.widthIn(max = 420.dp).fillMaxWidth())
        if (bad) RText(if (ar) "الكود غير صحيح" else "Wrong code", 16, C.danger)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlowButton(if (ar) "تأكيد" else "Confirm", primary = true) {
                val saved = LicenseManager.savedCode()
                val ok = !LicenseManager.enabled || (saved != null && code.trim().equals(saved.trim(), ignoreCase = true))
                if (ok) onOk() else bad = true
            }
            GlowButton(if (ar) "إلغاء" else "Cancel") { onCancel() }
        }
    }
}

/** إنشاء رمز جديد: إدخال مرتين للتأكيد. */
@Composable
fun PinSetup(vm: AppViewModel, accent: Color, onSet: (String) -> Unit, onCancel: () -> Unit) {
    val ar = vm.lang == "ar"
    var first by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    var err by remember { mutableStateOf(false) }
    androidx.compose.runtime.key(attempt, first) {
        PinPad(
            if (first == null) (if (ar) "ارسم رمزاً جديداً" else "Create a code") else (if (ar) "أعد كتابة الرمز للتأكيد" else "Repeat the code"),
            if (err) (if (ar) "الرمزان غير متطابقين، ابدأ من جديد" else "Codes differ, start again") else (if (ar) "4 أرقام" else "4 digits"),
            accent, err,
            onComplete = { c ->
                val f = first
                if (f == null) { err = false; first = c }
                else if (f == c) onSet(c)
                else { err = true; first = null; attempt++ }
            },
            onCancel = onCancel,
        )
    }
}

// ============================ شاشة اختيار الملف ============================
@Composable
fun ProfileGate(vm: AppViewModel) {
    val ar = vm.lang == "ar"
    val compact = isCompact()
    var pinFor by remember { mutableStateOf<Profile?>(null) }
    var guard by remember { mutableStateOf<(() -> Unit)?>(null) }
    var focusColor by remember { mutableStateOf(C.primary) }
    val glowCol by animateColorAsState(focusColor, tween(600), label = "gate-glow")
    val pulse by rememberInfiniteTransition(label = "gate-pulse").animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pa",
    )
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(700)) }
    BackHandler(enabled = vm.profileReady && pinFor == null && guard == null) { vm.profileBack() }

    fun choose(p: Profile) {
        if (vm.profileReady && vm.activeProfile?.id == p.id && p.type != "GUEST") { vm.profileBack(); return }
        if (p.pin.isNotEmpty()) pinFor = p else vm.enterProfile(p)
    }
    fun needMain(action: () -> Unit) {
        val m = vm.mainProfile()
        if (m == null || m.pin.isEmpty() || vm.manageUnlocked) action() else guard = action
    }

    val shown = vm.profiles.filter { !it.hidden }
    val cardSize = if (compact) 92.dp else 150.dp

    Box(
        Modifier.fillMaxSize().background(Color.Black).drawBehind {
            drawRect(
                Brush.radialGradient(
                    listOf(glowCol.copy(alpha = 0.42f * pulse), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.28f), radius = size.width * 0.75f,
                ),
            )
        },
    ) {
        val pf = pinFor
        val g = guard
        when {
            pf != null -> PinGate(vm, pf, (if (ar) "ملف " else "Profile ") + pf.name, onOk = { pinFor = null; vm.enterProfile(pf) }, onCancel = { pinFor = null })
            g != null -> PinGate(vm, vm.mainProfile() ?: pf ?: shown.first(), if (ar) "رمز الملف الرئيسي" else "Main profile code",
                onOk = { vm.manageUnlocked = true; guard = null; g() }, onCancel = { guard = null })
            else -> Column(
                Modifier.fillMaxSize().graphicsLayer { alpha = appear.value }.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 18.dp else 30.dp, Alignment.CenterVertically),
            ) {
                RText("AYHAM PLAYER", 14, C.accent, bold = true)
                RText(if (ar) "من يشاهد الآن؟" else "Who's watching?", if (compact) 28 else 44, Color.White, bold = true)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 30.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 18.dp else 28.dp),
                ) {
                    val firstId = (shown.firstOrNull { it.id == vm.activeProfile?.id } ?: shown.firstOrNull())?.id
                    shown.forEachIndexed { i, p ->
                        ProfileCard(vm, p, i, p.id == firstId, cardSize, onFocus = { focusColor = it }) { choose(p) }
                    }
                    ProfileCard(vm, vm.guestProfile(ar), shown.size, false, cardSize, onFocus = { focusColor = it }) { choose(vm.guestProfile(ar)) }
                    if (vm.profiles.size < 8) AddCard(vm, shown.size + 1, cardSize) { needMain { vm.openEditor(null) } }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    GlowButton(if (ar) "إدارة الملفات" else "Manage profiles") { needMain { vm.openManage() } }
                    if (vm.profileReady) GlowButton(if (ar) "رجوع" else "Back") { vm.profileBack() }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(vm: AppViewModel, p: Profile, idx: Int, first: Boolean, size: androidx.compose.ui.unit.Dp, onFocus: (Color) -> Unit, onClick: () -> Unit) {
    val ar = vm.lang == "ar"
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(120L + idx * 90L); a.animateTo(1f, tween(520)) }
    val accent = ProfileLook.c1(p)
    Focusable(
        Modifier.graphicsLayer { alpha = a.value; val s = 0.86f + 0.14f * a.value; scaleX = s; scaleY = s; translationY = (1f - a.value) * 36f }
            .then(if (first) Modifier.autoFocus() else Modifier)
            .onFocusChanged { if (it.isFocused) onFocus(accent) },
        RoundedCornerShape(size * 0.26f), 1.1f, onClick = onClick,
    ) { f ->
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileAvatarView(vm, p, size, glow = f)
            RText(p.name, if (isCompact()) 16 else 20, if (f) Color.White else C.textDim, bold = f, maxLines = 1, modifier = Modifier.widthIn(max = size + 20.dp))
            val badge = when {
                p.type == "MAIN" -> if (ar) "★ رئيسي" else "★ Main"
                p.type == "KIDS" -> if (ar) "🧸 أطفال" else "🧸 Kids"
                p.type == "GUEST" -> if (ar) "ضيف" else "Guest"
                else -> ""
            } + (if (p.pin.isNotEmpty()) " 🔒" else "")
            if (badge.isNotBlank()) RText(badge.trim(), 13, accent, bold = true, maxLines = 1)
            else if (p.tagline.isNotBlank()) RText(p.tagline, 13, C.textDim, maxLines = 1, modifier = Modifier.widthIn(max = size + 20.dp))
        }
    }
}

@Composable
private fun AddCard(vm: AppViewModel, idx: Int, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val ar = vm.lang == "ar"
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(120L + idx * 90L); a.animateTo(1f, tween(520)) }
    Focusable(
        Modifier.graphicsLayer { alpha = a.value; val s = 0.86f + 0.14f * a.value; scaleX = s; scaleY = s; translationY = (1f - a.value) * 36f },
        RoundedCornerShape(size * 0.26f), 1.1f, onClick = onClick,
    ) { f ->
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(size).clip(RoundedCornerShape(size * 0.22f)).background(if (f) tc(0x44FFFFFF) else tc(0x1AFFFFFF))
                    .border(2.dp, tc(0x44FFFFFF), RoundedCornerShape(size * 0.22f)),
                contentAlignment = Alignment.Center,
            ) { RText("+", (size.value * 0.45f).toInt(), if (f) Color.White else C.textDim, bold = true) }
            RText(if (ar) "إضافة ملف" else "Add profile", if (isCompact()) 16 else 20, if (f) Color.White else C.textDim, bold = f, maxLines = 1)
        }
    }
}

// ============================ الترحيب السينمائي ============================
@Composable
fun WelcomeOverlay(vm: AppViewModel, p: Profile, onDone: () -> Unit) {
    val ar = vm.lang == "ar"
    val alpha = remember { Animatable(1f) }
    val pop = remember { Animatable(0.55f) }
    val rise = remember { Animatable(0f) }
    val pulse by rememberInfiniteTransition(label = "wl").animateFloat(0.7f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "wlp")
    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch { pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 180f)) }
            launch { rise.animateTo(1f, tween(900)) }
        }
        delay(1100)
        alpha.animateTo(0f, tween(650))
        onDone()
    }
    val a = ProfileLook.c1(p)
    Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value; val s = 1f + (1f - alpha.value) * 0.06f; scaleX = s; scaleY = s }) {
        ProfileBackdrop(p, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
                ProfileAvatarView(vm, p, if (isCompact()) 120.dp else 180.dp, glow = true)
            }
            Column(Modifier.graphicsLayer { this.alpha = rise.value; translationY = (1f - rise.value) * 28f }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RText(if (ar) "مرحباً" else "Welcome", 20, a.copy(alpha = pulse), bold = true)
                RText(p.name, if (isCompact()) 34 else 52, Color.White, bold = true, maxLines = 1)
                if (p.tagline.isNotBlank()) RText(p.tagline, 18, C.textDim, maxLines = 1)
            }
        }
    }
}
