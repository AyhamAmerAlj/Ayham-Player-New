package com.ayham.player.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** إعدادات الكيبورد الخاص بالتطبيق (اختياري، يُفعَّل من الإعدادات). */
object Kbd {
    private var sp: android.content.SharedPreferences? = null
    var enabled by mutableStateOf(false)
    var abc by mutableStateOf(false)      // ترتيب أبجدي بدل ترتيب الكيبورد العادي
    var sound by mutableStateOf(true)
    var recent by mutableStateOf<List<String>>(emptyList())

    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.getSharedPreferences("ayham_player", Context.MODE_PRIVATE)
        sp = p
        enabled = p.getBoolean("kbd_on", false)
        abc = p.getBoolean("kbd_abc", false)
        sound = p.getBoolean("kbd_snd", true)
        recent = (p.getString("kbd_recent", "") ?: "").split('\u0001').filter { it.isNotBlank() }
    }
    fun changeEnabled(v: Boolean) { enabled = v; sp?.edit()?.putBoolean("kbd_on", v)?.apply() }
    fun changeAbc(v: Boolean) { abc = v; sp?.edit()?.putBoolean("kbd_abc", v)?.apply() }
    fun changeSound(v: Boolean) { sound = v; sp?.edit()?.putBoolean("kbd_snd", v)?.apply() }
    fun remember(s: String) {
        val t = s.trim()
        if (t.length < 2) return
        recent = (listOf(t) + recent.filter { it != t }).take(8)
        sp?.edit()?.putString("kbd_recent", recent.joinToString("\u0001"))?.apply()
    }
}

private val EN_STD = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
private val EN_ABC = listOf("abcdefghi", "jklmnopqr", "stuvwxyz")
private val AR_STD = listOf("ضصثقفغعهخحجد", "شسيبلاتنمكط", "ئءؤرىةوزظذ")
private val AR_ABC = listOf("ابتثجحخدذر", "زسشصضطظعغف", "قكلمنهويةى")
private val AR_EXTRA = listOf("أ", "إ", "آ", "ؤ", "ئ", "ء", "لا")

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyTile(
    modifier: Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    accent: Boolean = false,
    soft: Boolean = false,
    sound: Int = 0,
    autoFocus: Boolean = false,
    onLong: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val target = when { pressed -> 0.88f; focused -> 1.12f; else -> 1f }
    val sc by animateFloatAsState(target, if (Perf.on) tween(60) else spring(dampingRatio = 0.6f, stiffness = 900f), label = "ks")
    val fr = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { kotlinx.coroutines.delay(90); runCatching { fr.requestFocus() } }
    val shape = RoundedCornerShape(11.dp)
    val bg = when {
        focused && accent -> Brush.verticalGradient(listOf(tc(0xFFC084FC), tc(0xFF8B5CF6)))
        accent -> Brush.verticalGradient(listOf(tc(0xFF8B5CF6), tc(0xFF6D28D9)))
        focused -> Brush.verticalGradient(listOf(tc(0xFF4A3A78), tc(0xFF3A2C63)))
        soft -> Brush.verticalGradient(listOf(tc(0xFF1B1430), tc(0xFF15102A)))
        else -> Brush.verticalGradient(listOf(tc(0xFF2A2046), tc(0xFF221A3B)))
    }
    Box(
        modifier
            .heightIn(min = 26.dp)
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(shape)
            .background(bg)
            .border(if (focused) 2.dp else 0.8.dp, if (focused) C.accent else tc(0x22FFFFFF), shape)
            .focusRequester(fr)
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(
                interactionSource = src, indication = null,
                onLongClick = onLong?.let { f -> { UiSound.key(2); f() } },
                onClick = { UiSound.key(sound); onClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) Icon(icon, null, tint = Color.White, modifier = Modifier.padding(6.dp))
        else if (label != null) RText(label, if (label.length > 2) 13 else 18, Color.White, bold = true, maxLines = 1)
    }
}

/** لوحة مفاتيح خاصة بالتطبيق: مصمّمة للريموت واللمس، بصوت نقرة ناعم. */
@Composable
fun AyhamKeyboard(
    initial: String, label: String, password: Boolean, search: Boolean,
    onChange: (String) -> Unit, onDone: () -> Unit, onClose: () -> Unit,
) {
    val lang = LocalLang.current
    var text by remember { mutableStateOf(initial) }
    var ar by remember { mutableStateOf(lang == "ar") }
    var symbols by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(false) }
    val compact = isCompact()
    val sHdp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    val keyH = (sHdp * 0.064f).coerceIn(30f, 46f).dp

    fun put(s: String) { text += s; onChange(text); if (shift) shift = false }
    fun del() { if (text.isNotEmpty()) { text = text.dropLast(1); onChange(text) } }
    fun delWord() {
        val t = text.trimEnd()
        text = if (t.contains(' ')) t.substringBeforeLast(' ') + " " else ""
        onChange(text)
    }
    fun finish() { Kbd.remember(text); onDone(); onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(Modifier.fillMaxSize().background(tc(0x99000000)), contentAlignment = Alignment.BottomCenter) {
                Column(
                    Modifier.widthIn(max = 820.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .background(Brush.verticalGradient(listOf(tc(0xF2120B24), tc(0xFA0B0616))))
                        .border(1.dp, tc(0x338B5CF6), RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // ---- الحقل ----
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(tc(0x33000000))
                            .border(1.dp, tc(0x558B5CF6), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.Search, null, tint = C.primaryLight, modifier = Modifier.padding(end = 2.dp))
                        Column(Modifier.weight(1f)) {
                            RText(label, 12, C.textDim, maxLines = 1)
                            CompositionLocalProvider(LocalLayoutDirection provides (if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr)) {
                                RText(
                                    (if (password) "•".repeat(text.length) else text) + "▏",
                                    20, Color.White, bold = true, maxLines = 1,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        KeyTile(Modifier.width(70.dp).height(32.dp), if (lang == "ar") "مسح" else "Clear", soft = true, sound = 2) { text = ""; onChange("") }
                    }
                    // ---- اقتراحات من آخر ما كتبته ----
                    val sugg = remember(text, Kbd.recent) {
                        Kbd.recent.filter { text.isBlank() || (it.startsWith(text, true) && !it.equals(text, true)) }.take(6)
                    }
                    if (!password && sugg.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sugg, key = { it }) { s ->
                                KeyTile(Modifier.height(30.dp), s, soft = true) { text = s; onChange(s) }
                            }
                        }
                    }
                    // ---- الأحرف ----
                    if (symbols) {
                        val rows = listOf("1234567890", "@#\$_&-+()/", "*\"':;!?%=", ".,،؟~^[]{}")
                        rows.forEach { r ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                r.forEach { c -> KeyTile(Modifier.weight(1f).height(keyH), c.toString()) { put(c.toString()) } }
                            }
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            "1234567890".forEach { c -> KeyTile(Modifier.weight(1f).height(keyH - 4.dp), c.toString(), soft = true) { put(c.toString()) } }
                        }
                        val rows = when {
                            ar && Kbd.abc -> AR_ABC
                            ar -> AR_STD
                            Kbd.abc -> EN_ABC
                            else -> EN_STD
                        }
                        rows.forEachIndexed { ri, r ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (ri == 2 && !ar) KeyTile(Modifier.weight(1.4f).height(keyH), "⇧", accent = shift, sound = 1) { shift = !shift }
                                r.forEachIndexed { ci, c ->
                                    val ch = if (!ar && shift) c.uppercaseChar().toString() else c.toString()
                                    KeyTile(Modifier.weight(1f).height(keyH), ch, autoFocus = ri == 1 && ci == 3) { put(ch) }
                                }
                                if (ri == 2) KeyTile(Modifier.weight(1.4f).height(keyH), "⌫", soft = true, sound = 2, onLong = { delWord() }) { del() }
                            }
                        }
                        if (ar) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AR_EXTRA.forEach { c -> KeyTile(Modifier.weight(1f).height(keyH - 4.dp), c, soft = true) { put(c) } }
                        }
                    }
                    // ---- الصف السفلي ----
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        KeyTile(Modifier.weight(1.3f).height(keyH), if (symbols) "ABC" else "123", soft = true, sound = 1) { symbols = !symbols }
                        KeyTile(Modifier.weight(1.3f).height(keyH), if (ar) "EN" else "ع", soft = true, sound = 1) { ar = !ar; symbols = false }
                        KeyTile(Modifier.weight(4f).height(keyH), if (lang == "ar") "مسافة" else "space", sound = 3) { put(" ") }
                        KeyTile(Modifier.weight(1.3f).height(keyH), ".com", soft = true) { put(".com") }
                        if (symbols) KeyTile(Modifier.weight(1f).height(keyH), "⌫", soft = true, sound = 2, onLong = { delWord() }) { del() }
                        KeyTile(Modifier.weight(2f).height(keyH), if (search) (if (lang == "ar") "بحث" else "Search") else (if (lang == "ar") "تم" else "Done"), accent = true, sound = 1) { finish() }
                    }
                }
            }
        }
    }
}

/** حقل يفتح كيبورد التطبيق عند الضغط (يُستخدم بدل حقل النظام لمّا يكون الكيبورد الخاص مفعّلًا). */
@Composable
fun KbdField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier,
    password: Boolean, icon: ImageVector?, search: Boolean, onDone: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Focusable(modifier.fillMaxWidth(), shape, 1.02f, onClick = { open = true }) { f ->
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape)
                .background(if (f) tc(0x33000000) else tc(0x11000000))
                .border(if (f) 2.dp else 1.dp, if (f) C.primary else tc(0x44FFFFFF), shape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) Icon(icon, null, tint = C.primaryLight, modifier = Modifier.height(24.dp))
            if (value.isBlank()) RText(label, 16, C.textDim, maxLines = 1, modifier = Modifier.weight(1f))
            else RText(if (password) "•".repeat(value.length) else value, 18, C.text, maxLines = 1, modifier = Modifier.weight(1f))
        }
    }
    if (open) AyhamKeyboard(value, label, password, search, onChange, onDone) { open = false }
}
