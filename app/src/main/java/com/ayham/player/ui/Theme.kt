package com.ayham.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import com.ayham.player.R

object C {
    val bg: Color get() = ThemeState.palette.bg
    val bg2: Color get() = ThemeState.palette.bg2
    val surface: Color get() = ThemeState.palette.surface
    val surfaceHi: Color get() = ThemeState.palette.surfaceHi
    val primary: Color get() = ThemeState.palette.primary
    val primaryLight: Color get() = ThemeState.palette.primaryLight
    val accent: Color get() = ThemeState.palette.accent
    val text: Color get() = ThemeState.palette.text
    val textDim: Color get() = ThemeState.palette.textDim
    val danger: Color get() = ThemeState.palette.danger
    val ok: Color get() = ThemeState.palette.ok
    val gold: Color get() = ThemeState.palette.gold ?: ThemeState.palette.primaryLight

    val glow: Brush get() = Brush.linearGradient(listOf(primary, accent))
}

val AppFont = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
)

val LocalLang = compositionLocalOf { "ar" }

@Composable
fun t(key: String): String = Strings.get(LocalLang.current, key)

@Composable
fun AyhamTheme(lang: String, content: @Composable () -> Unit) {
    val dir = if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.primary,
            background = C.bg,
            surface = C.surface,
            onBackground = C.text,
            onSurface = C.text,
        ),
    ) {
        CompositionLocalProvider(LocalLang provides lang, LocalLayoutDirection provides dir) {
            Box(Modifier.fillMaxSize().background(C.bg)) { content() }
        }
    }
}

/** نص بخط Cairo (الخط الرسمي للتطبيق). */
@Composable
fun RText(
    text: String,
    size: Int = 18,
    color: Color = C.text,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    lineHeight: Int = 0,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        style = TextStyle(
            fontFamily = AppFont,
            fontSize = size.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            lineHeight = if (lineHeight > 0) lineHeight.sp else (size * 1.5f).sp,
        ),
    )
}

/** نص عادي مقروء (لعناوين المحتوى). */
@Composable
fun BodyText(
    text: String,
    size: Int = 14,
    color: Color = C.text,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        style = TextStyle(
            fontFamily = AppFont,
            fontSize = size.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        ),
    )
}

/**
 * عنصر قابل للتركيز بالريموت: يكبر ويتوهج بالبنفسجي عند التركيز.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Focusable(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    focusScale: Float = 1.07f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable (focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val longFired = remember { booleanArrayOf(false) }
    val sc by animateFloatAsState(if (focused) focusScale else 1f, tween(90), label = "sc")
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .then(
                if (focused) Modifier.border(2.5.dp, C.glow, shape)
                else Modifier.border(1.dp, tc(0x22FFFFFF), shape)
            )
            .onFocusChanged { if (it.isFocused && !focused) UiSound.move(); focused = it.isFocused }
            .then(
                if (onLongClick == null) Modifier else Modifier.onPreviewKeyEvent { ev ->
                    // ضغطة مطوّلة على OK بالريموت
                    val code = ev.nativeKeyEvent.keyCode
                    val center = code == android.view.KeyEvent.KEYCODE_DPAD_CENTER || code == android.view.KeyEvent.KEYCODE_ENTER
                    if (!center) false
                    else if (ev.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                        if (ev.nativeKeyEvent.repeatCount >= 1) {
                            if (!longFired[0]) { longFired[0] = true; onLongClick() }
                            true
                        } else false
                    } else if (longFired[0]) { longFired[0] = false; true } else false
                },
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = onLongClick,
                onClick = { UiSound.click(); onClick() },
            ),
    ) {
        content(focused)
    }
}

/** زر نصي عصري. */
@Composable
fun GlowButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    height: Dp = 46.dp,
    onClick: () -> Unit,
) {
    Focusable(modifier = modifier, shape = RoundedCornerShape(24.dp), focusScale = 1.06f, onClick = onClick) { f ->
        Row(
            Modifier
                .then(
                    ThemeState.palette.stripe.let { st ->
                        if (primary && !f && st != null) Modifier.background(Brush.horizontalGradient(st), RoundedCornerShape(24.dp))
                        else Modifier.background(
                            when {
                                f -> C.surfaceHi
                                primary -> C.primary
                                else -> C.surface
                            },
                            RoundedCornerShape(24.dp),
                        )
                    },
                )
                .padding(horizontal = 22.dp)
                .height(height),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RText(label, 17, if (primary && !f) Color.White else C.text, bold = true, maxLines = 1)
        }
    }
}
