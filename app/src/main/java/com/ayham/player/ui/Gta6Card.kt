package com.ayham.player.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
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
 * بطاقة GTA VI ثابتة في أعلى الصفحة الرئيسية للتلفاز (على اليمين، مثل بطاقة برشلونة).
 * الصورة والعداد يظهران أو يختفيان من الإعدادات. الضغط عليها يفتح الصفحة الرسمية للعبة.
 */
@Composable
fun Gta6Home(vm: AppViewModel, small: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val ar = vm.lang == "ar"
    val count = gta6Count(ar, GTA6_RELEASE_MS - now)
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(18.dp)
    val posterW = if (small) 64.dp else 90.dp
    val posterH = posterW * 1.5f
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Focusable(shape = shape, focusScale = 1.04f, onClick = {
            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GTA6_PAGE))) }
        }) { f ->
            Row(
                Modifier.width(if (small) 260.dp else 310.dp).clip(shape)
                    .background(Brush.horizontalGradient(listOf(Color(0xEE0A0A24), Color(0xEE2B2070))))
                    .border(if (f) 2.dp else 1.dp, if (f) Color.White else Color(0x66FF8A5B), shape)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (vm.gtaPoster) {
                    Image(
                        painter = painterResource(R.drawable.gta6_poster),
                        contentDescription = null,
                        modifier = Modifier.width(posterW).height(posterH).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    RText("GTA VI", if (small) 18 else 22, Color.White, bold = true, maxLines = 1)
                    if (vm.gtaCountdown) RText(count, 14, Color(0xFFFF8A5B), bold = true, maxLines = 2)
                    BodyText("19 Nov 2026", 13, Color(0xFFE6E0F5), maxLines = 1)
                }
            }
        }
    }
}
