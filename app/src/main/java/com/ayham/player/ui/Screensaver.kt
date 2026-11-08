package com.ayham.player.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Imdb
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import kotlinx.coroutines.delay

/**
 * شاشة التوقف السينمائية: تظهر لو التطبيق ساكن في الرئيسية. بوسترات أفلامك ومسلسلاتك تتبدّل ببطء مع
 * حركة تقريب خفيفة (Ken Burns) وشريطين سينمائيين وساعة. أي زر أو لمسة ترجعك فوراً.
 */
@Composable
fun Screensaver(vm: AppViewModel) {
    val items by produceState(emptyList<Item>()) { value = vm.saverItems() }
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(items) {
        if (items.size > 1) while (true) { delay(9000); idx = (idx + 1) % items.size }
    }
    val clock by produceState("") {
        while (true) { value = java.text.SimpleDateFormat("h:mm", java.util.Locale.US).format(java.util.Date()); delay(15_000) }
    }
    val ar = vm.lang == "ar"
    val compact = isCompact()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (items.isNotEmpty()) {
            Crossfade(targetState = idx, animationSpec = tween(1600), label = "saver") { i ->
                val cur = items[i % items.size]
                val sc = remember { Animatable(0f) }
                LaunchedEffect(Unit) { sc.animateTo(1f, tween(11_000, easing = LinearEasing)) }
                Box(Modifier.fillMaxSize()) {
                    Img(cur.image, Modifier.fillMaxSize().blur(36.dp).alpha(0.45f).graphicsLayer { val s = 1.15f + 0.1f * sc.value; scaleX = s; scaleY = s }, w = 400, h = 600)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent, Color(0xCC000000)))))
                    val poster: @Composable (Modifier) -> Unit = { m ->
                        Img(cur.image, m.clip(RoundedCornerShape(16.dp)).graphicsLayer { val s = 1f + 0.06f * sc.value; scaleX = s; scaleY = s }, w = 500, h = 750)
                    }
                    val texts: @Composable () -> Unit = {
                        Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start) {
                            RText(if (cur.kind == Kind.SERIES) (if (ar) "مسلسل" else "SERIES") else (if (ar) "فيلم" else "MOVIE"), 18, C.accent, bold = true)
                            RText(Imdb.clean(cur.name), if (compact) 30 else 46, Color.White, bold = true, maxLines = 3)
                            cur.rating.takeIf { it.isNotBlank() && it != "0" }?.let { RText("★ $it", 22, C.primaryLight, bold = true) }
                        }
                    }
                    if (compact) {
                        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)) {
                            poster(Modifier.fillMaxWidth(0.62f).aspectRatio(2f / 3f))
                            texts()
                        }
                    } else {
                        Row(Modifier.fillMaxSize().padding(horizontal = 90.dp), horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            poster(Modifier.fillMaxHeight(0.66f).aspectRatio(2f / 3f))
                            texts()
                        }
                    }
                }
            }
        }
        // شريطا السينما
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().fillMaxHeight(0.07f).background(Color.Black))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.07f).background(Color.Black))
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.align(Alignment.BottomStart).padding(start = 36.dp, bottom = if (compact) 48.dp else 40.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RText(clock, if (compact) 40 else 54, Color.White, bold = true)
                RText("AYHAM PLAYER", 14, C.accent, bold = true, modifier = Modifier.padding(bottom = 10.dp))
            }
        }
        RText(if (ar) "اضغط أي زر للعودة" else "Press any key to return", 14, C.textDim, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 36.dp, bottom = if (compact) 52.dp else 48.dp))
    }
}
