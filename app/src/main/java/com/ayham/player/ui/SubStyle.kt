@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.ui.CaptionStyleCompat
import com.ayham.player.AppViewModel

/** أنماط الترجمة: 0 سينما (أبيض بظل ناعم)، 1 أصفر كلاسيكي، 2 خلفية شفافة. */
fun subCaptionStyle(i: Int, tf: android.graphics.Typeface? = null): CaptionStyleCompat = when (i) {
    1 -> CaptionStyleCompat(0xFFFFE066.toInt(), android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_OUTLINE, android.graphics.Color.BLACK, tf)
    2 -> CaptionStyleCompat(android.graphics.Color.WHITE, 0xB3000000.toInt(), android.graphics.Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_NONE, android.graphics.Color.BLACK, tf)
    else -> CaptionStyleCompat(android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW, android.graphics.Color.BLACK, tf)
}

/** معاينة حية لشكل الترجمة في الإعدادات، مع تغيير الحجم والنمط. */
@Composable
fun SubPreviewCard(vm: AppViewModel) {
    val ar = vm.lang == "ar"
    val sizeSp = when (vm.subSize) { 0 -> 18; 1 -> 24; else -> 32 }
    val fg = if (vm.subStyle == 1) Color(0xFFFFE066) else Color.White
    val bg = if (vm.subStyle == 2) Color(0xB3000000) else Color.Transparent
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(tc(0xFF2A1850), Color.Black))).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RText(if (ar) "شكل الترجمة" else "Subtitle look", 20, C.primaryLight, bold = true)
        Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF101014)), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.padding(bottom = 10.dp).clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 10.dp, vertical = 3.dp)) {
                RText(if (ar) "هكذا تظهر الترجمة على الشاشة" else "This is how subtitles look", sizeSp, fg, bold = true, maxLines = 1)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(t("size_s"), t("size_m"), t("size_l")).forEachIndexed { i, l -> Chip(l, vm.subSize == i) { vm.changeSubSize(i) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(if (ar) "سينما" else "Cinema", if (ar) "أصفر" else "Yellow", if (ar) "خلفية" else "Box").forEachIndexed { i, l -> Chip(l, vm.subStyle == i) { vm.changeSubStyle(i) } }
        }
    }
}
