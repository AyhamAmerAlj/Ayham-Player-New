package com.ayham.player.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** الحقل النشط حالياً في الكيبورد الداخلي: معرّفه + دوال قراءة/كتابة قيمته + إجراء "تم". */
class KbTarget(
    val id: Any,
    val read: () -> String,
    val write: (String) -> Unit,
    val onDone: () -> Unit,
)

/** مضيف الكيبورد الداخلي: يحفظ الحقل النشط ويعرض اللوحة أسفل الشاشة. */
@Stable
class KeyboardHost {
    var target: KbTarget? by mutableStateOf(null)
    var last: KbTarget? = null
    /** هل الحقل النشط له التركيز؟ يُستخدم لإغلاق اللوحة عند الانتقال لعنصر آخر. */
    var fieldFocused: Boolean by mutableStateOf(false)
    /** هل أحد مفاتيح اللوحة له التركيز؟ */
    var keysFocused: Boolean by mutableStateOf(false)

    fun open(t: KbTarget) {
        last = t
        target = t
    }

    fun dismiss() {
        target = null
        keysFocused = false
    }

    fun done() {
        val t = target
        target = null
        keysFocused = false
        t?.onDone()
    }
}

/** null = لا يوجد مضيف (الحقول تستخدم الكيبورد العادي). */
val LocalKeyboardHost = compositionLocalOf<KeyboardHost?> { null }

private enum class KbLetters { AR, EN }

private val KbKeyHeight = 50.dp
private val KbKeyShape = RoundedCornerShape(10.dp)

/**
 * كيبورد داخلي خاص بالتطبيق (عربي / إنجليزي / رموز).
 * لا يستخدم كيبورد الجهاز، فلا يُرسل الضغطات لأي تطبيق كيبورد خارجي، وأسهل بالريموت.
 */
@Composable
fun InAppKeyboard(host: KeyboardHost, appLang: String, target: KbTarget) {
    var letters by remember { mutableStateOf(if (appLang == "ar") KbLetters.AR else KbLetters.EN) }
    var symbols by remember { mutableStateOf(false) }
    var shift by remember { mutableStateOf(false) }

    val isAr = letters == KbLetters.AR && !symbols
    val isEnLetters = letters == KbLetters.EN && !symbols
    val dir = if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr

    fun type(s: String) {
        val out = if (isEnLetters && shift) s.uppercase() else s
        target.write(target.read() + out)
        if (shift) shift = false
    }

    fun backspace() {
        val v = target.read()
        if (v.isNotEmpty()) target.write(v.dropLast(1))
    }

    fun row(s: String): List<String> = s.split(" ")

    val rows: List<List<String>> = when {
        symbols -> listOf(
            row("1 2 3 4 5 6 7 8 9 0"),
            row("@ # \$ % & * ( ) - +"),
            row("/ : ; \" ' ? ! _"),
        )
        letters == KbLetters.AR -> listOf(
            row("1 2 3 4 5 6 7 8 9 0"),
            row("ض ص ث ق ف غ ع ه خ ح ج د"),
            row("ش س ي ب ل ا ت ن م ك"),
            row("ظ ز ر و ة ى ء ؤ ئ ذ"),
        )
        else -> listOf(
            row("1 2 3 4 5 6 7 8 9 0"),
            row("q w e r t y u i o p"),
            row("a s d f g h j k l"),
            row("z x c v b n m"),
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides dir) {
        Column(
            Modifier
                .fillMaxWidth()
                .onFocusChanged { host.keysFocused = it.hasFocus }
                .background(C.bg2)
                .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 10.dp),
        ) {
            rows.forEachIndexed { index, keys ->
                Row(
                    Modifier.fillMaxWidth().height(KbKeyHeight),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (isEnLetters && index == rows.lastIndex) {
                        KbKey("⇧", Modifier.weight(1.5f), active = shift) { shift = !shift }
                    }
                    keys.forEach { ch ->
                        val label = if (isEnLetters && shift) ch.uppercase() else ch
                        KbKey(label, Modifier.weight(1f)) { type(ch) }
                    }
                }
            }

            // الصف السفلي بترتيب ثابت من اليمين: 123 ثم ⌫ (يمين) ثم . ومسافة ثم عربي/EN ثم تم
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    Modifier.fillMaxWidth().height(KbKeyHeight),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    KbKey(
                        label = if (symbols) (if (letters == KbLetters.AR) "أ ب ج" else "ABC") else "123",
                        modifier = Modifier.weight(1.4f),
                    ) { symbols = !symbols }
                    KbKey("⌫", Modifier.weight(1.4f)) { backspace() }
                    KbKey(".", Modifier.weight(1f)) { type(".") }
                    KbKey(
                        label = if (letters == KbLetters.AR) "مسافة" else "space",
                        modifier = Modifier.weight(4f),
                    ) { type(" ") }
                    KbKey(
                        label = if (letters == KbLetters.AR) "EN" else "عربي",
                        modifier = Modifier.weight(1.2f),
                    ) {
                        letters = if (letters == KbLetters.AR) KbLetters.EN else KbLetters.AR
                        symbols = false
                        shift = false
                    }
                    KbKey(
                        label = if (letters == KbLetters.AR) "تم" else "Done",
                        modifier = Modifier.weight(1.8f),
                        primary = true,
                    ) { host.done() }
                }
            }
        }
    }
}

/** مفتاح واحد: يكبر بالريموت عند التركيز، ويصغر قليلاً عند الضغط باللمس. */
@Composable
private fun KbKey(
    label: String,
    modifier: Modifier,
    primary: Boolean = false,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.92f
            focused -> 1.08f
            else -> 1f
        },
        animationSpec = tween(90),
        label = "kbKeyScale",
    )
    val bg = when {
        primary -> C.primary
        focused || active -> C.surfaceHi
        else -> C.surface
    }
    Box(
        modifier
            .fillMaxHeight()
            .padding(3.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(KbKeyShape)
            .background(bg)
            .then(if (focused) Modifier.border(2.dp, C.glow, KbKeyShape) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        RText(label, 18, if (primary) Color.White else C.text, bold = primary || focused, maxLines = 1)
    }
}
