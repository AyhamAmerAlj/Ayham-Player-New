package com.ayham.player.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.R
import kotlinx.coroutines.delay

/** إطلاق GTA VI: 19 نوفمبر 2026 الساعة 05:00 UTC (حسب صفحة PlayStation الرسمية). */
const val GTA6_RELEASE_MS = 1795064400000L

private const val GTA6_PAGE = "https://www.playstation.com/en-us/games/grand-theft-auto-vi/"

/** نص العداد: الأيام والساعات والدقائق المتبقية، أو "متوفرة الآن" بعد الإطلاق. */
fun gta6Count(ar: Boolean, diff: Long): String {
    if (diff <= 0) return if (ar) "متوفرة الآن" else "Out now"
    val mins = diff / 60_000
    val d = mins / 1440; val h = (mins % 1440) / 60; val m = mins % 60
    return when {
        d > 0 -> if (ar) "باقي $d يوم و$h س" else "$d d $h h left"
        else -> if (ar) "باقي $h س و$m د" else "$h h $m min left"
    }
}

/**
 * بوستر GTA VI داخل صف الأقسام على التلفاز، بنفس حجم بوسترات الصف.
 * يُختار بالريموت، والضغط عليه يفتح الصفحة الرسمية للعبة.
 */
@Composable
fun Gta6Poster(vm: AppViewModel, w: Dp, h: Dp) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val ar = vm.lang == "ar"
    val count = gta6Count(ar, GTA6_RELEASE_MS - now)
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(14.dp)
    Focusable(shape = shape, focusScale = 1.05f, onClick = {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GTA6_PAGE))) }
    }) { f ->
        Box(
            Modifier.width(w).height(h).clip(shape)
                .border(if (f) 2.dp else 1.dp, if (f) Color.White else Color(0x66FF8A5B), shape),
        ) {
            Image(
                painter = painterResource(R.drawable.gta6_poster),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color(0xEE0A0A24)),
                ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                RText("GTA VI", 18, Color.White, bold = true, maxLines = 1)
                RText(count, 13, Color(0xFFFF8A5B), bold = true, maxLines = 2)
                BodyText("19 Nov 2026", 12, Color(0xFFE6E0F5), maxLines = 1)
            }
        }
    }
}
