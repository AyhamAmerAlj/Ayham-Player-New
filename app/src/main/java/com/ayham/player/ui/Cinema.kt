package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ayham.player.AppViewModel
import com.ayham.player.data.Item
import kotlinx.coroutines.launch

/** "الليلة في السينما": اقتراح فيلم حسب المزاج من مكتبتك. */
@Composable
fun TonightDialog(vm: AppViewModel) {
    if (!vm.showTonight) return
    val ar = vm.lang == "ar"
    val scope = rememberCoroutineScope()
    var mood by remember { mutableStateOf("") }
    var pick by remember { mutableStateOf<Item?>(null) }
    var loading by remember { mutableStateOf(false) }
    var none by remember { mutableStateOf(false) }
    val seen = remember { HashSet<String>() }
    fun roll(m: String) {
        mood = m; loading = true; none = false
        scope.launch {
            val it = vm.tonight(m, seen)
            pick = it; loading = false; none = it == null
            it?.let { x -> seen.add(x.key) }
        }
    }
    val moods = listOf("comedy" to (if (ar) "ضحك" else "Comedy"), "thriller" to (if (ar) "إثارة" else "Thriller"), "romance" to (if (ar) "رومانسي" else "Romance"),
        "family" to (if (ar) "عائلي" else "Family"), "action" to (if (ar) "أكشن" else "Action"), "surprise" to (if (ar) "فاجئني" else "Surprise me"))
    Dialog(onDismissRequest = { vm.showTonight = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(26.dp)
        Column(
            Modifier.fillMaxWidth(if (isCompact()) 0.94f else 0.62f).clip(shape).background(Brush.verticalGradient(listOf(tc(0xFF1A0B33), tc(0xFF05020B))))
                .border(1.5.dp, Brush.horizontalGradient(listOf(C.primary, C.accent)), shape).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RText(if (ar) "🎬 الليلة في السينما" else "🎬 Tonight at the cinema", 26, Color.White, bold = true)
            BodyText(if (ar) "اختر مزاجك وأختار لك فيلماً من مكتبتك" else "Pick a mood and get a movie from your library", 14, C.textDim)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(moods) { (id, label) -> Chip(label, mood == id) { roll(id) } }
            }
            if (loading) RText(if (ar) "جارٍ الاختيار..." else "Picking...", 16, C.accent, bold = true)
            if (none && !loading) BodyText(if (ar) "لم أجد فيلماً مناسباً بهذا المزاج في مكتبتك، جرّب مزاجاً آخر" else "No match in your library for this mood, try another", 14, C.textDim)
            pick?.takeIf { !loading }?.let { m ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(tc(0x22FFFFFF)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Img(m.image, Modifier.width(100.dp).height(150.dp).clip(RoundedCornerShape(12.dp)), w = 200, h = 300)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RText(m.name, 22, Color.White, bold = true, maxLines = 2)
                        m.rating.takeIf { it.isNotBlank() && it != "0" }?.let { RText("★ $it", 16, tc(0xFFF5C518), bold = true) }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlowButton(if (ar) "ابدأ العرض" else "Start", primary = true) { vm.showTonight = false; vm.playMovie(m) }
                            GlowButton(if (ar) "اقتراح آخر" else "Another") { roll(mood) }
                        }
                        GlowButton(if (ar) "التفاصيل" else "Details") { vm.showTonight = false; vm.openDetail(m) }
                    }
                }
            }
            GlowButton(if (ar) "إغلاق" else "Close") { vm.showTonight = false }
        }
    }
}

/** أعمال ممثل موجودة في مكتبتك. */
@Composable
fun ActorDialog(vm: AppViewModel, name: String, onDismiss: () -> Unit) {
    val ar = vm.lang == "ar"
    var works by remember(name) { mutableStateOf<List<Item>?>(null) }
    LaunchedEffect(name) { works = vm.actorWorks(name) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth(0.94f).clip(shape).background(tc(0xFF0B0616)).border(1.5.dp, Brush.horizontalGradient(listOf(C.primary, C.accent)), shape).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RText((if (ar) "أعمال " else "Works of ") + name, 22, Color.White, bold = true, maxLines = 2)
            val w = works
            when {
                w == null -> RText(if (ar) "جارٍ البحث في مكتبتك..." else "Searching your library...", 16, C.accent, bold = true)
                w.isEmpty() -> BodyText(if (ar) "لا توجد أعمال لهذا الاسم في مكتبتك" else "No works for this name in your library", 15, C.textDim)
                else -> ContentRow(if (ar) "الموجود عندك" else "In your library", w) { onDismiss(); vm.openDetail(it) }
            }
            GlowButton(if (ar) "إغلاق" else "Close", Modifier.autoFocus()) { onDismiss() }
        }
    }
}
