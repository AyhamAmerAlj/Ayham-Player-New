package com.ayham.player.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.R
import kotlinx.coroutines.delay

/** إطلاق GTA VI: 19 نوفمبر 2026 الساعة 05:00 UTC (حسب صفحة PlayStation الرسمية). */
const val GTA6_RELEASE_MS = 1795064400000L

/** بطاقة الملصق مع العد التنازلي لإطلاق GTA VI. تظهر فقط مع ثيم GTA VI. */
@Composable
fun Gta6Card(vm: AppViewModel, small: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val ar = vm.lang == "ar"
    val diff = GTA6_RELEASE_MS - now
    val mins = (diff / 60_000).coerceAtLeast(0L)
    val d = mins / 1440; val h = (mins % 1440) / 60; val m = mins % 60
    val count = when {
        diff <= 0 -> if (ar) "متوفرة الآن" else "Out now"
        d > 0 -> if (ar) "باقي $d يوم و$h ساعة" else "$d days $h hours to go"
        else -> if (ar) "باقي $h ساعة و$m دقيقة" else "$h hours $m min to go"
    }
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier.width(if (small) 150.dp else 190.dp).height(if (small) 210.dp else 270.dp)
            .clip(shape).border(1.2.dp, Color(0x66FF8A5B), shape),
    ) {
        Image(
            painter = painterResource(R.drawable.gta6_poster),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, 0.5f to Color.Transparent, 1f to Color(0xEE0A0A24)),
            ),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            RText("GTA VI", if (small) 18 else 22, Color.White, bold = true, maxLines = 1)
            RText(count, if (small) 12 else 14, Color(0xFFFF8A5B), bold = true, maxLines = 1)
            BodyText("19 Nov 2026", if (small) 11 else 13, Color(0xFFE6E0F5), maxLines = 1)
        }
    }
}
