package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.WhatsNew
import coil.imageLoader

/** لون متوسط لصورة (للإضاءة المحيطية). يرجع null إن تعذّر. */
suspend fun avgColor(ctx: android.content.Context, url: String): Color? = runCatching {
    val req = coil.request.ImageRequest.Builder(ctx).data(url).size(24, 24).allowHardware(false).build()
    val res = ctx.imageLoader.execute(req) as? coil.request.SuccessResult ?: return@runCatching null
    val bmp = (res.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return@runCatching null
    Color(android.graphics.Bitmap.createScaledBitmap(bmp, 1, 1, true).getPixel(0, 0))
}.getOrNull()

/** بانرات الرئيسية: تذكير انتهاء الاشتراك، وما الجديد في التحديث. */
@Composable
fun ExtraBanners(vm: AppViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpiryBanner(vm)
    }
}

@Composable
private fun ExpiryBanner(vm: AppViewModel) {
    val exp = vm.accountInfo.expDate
    if (exp.isBlank()) return
    val ms = runCatching { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(exp)?.time }.getOrNull() ?: return
    if (ms < 946_684_800_000L) return // تاريخ غير صالح أو اشتراك غير محدود
    val days = (ms - System.currentTimeMillis()) / 86_400_000L + 1
    if (days > 7) return
    val ar = vm.lang == "ar"
    val text = if (days <= 0L) {
        if (ar) "انتهى اشتراكك، جدّده عند المزوّد" else "Your subscription has expired"
    } else {
        if (ar) "ينتهي اشتراكك خلال $days يومًا" else "Your subscription ends in $days days"
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(tc(0xCC3B2A08)).border(1.dp, tc(0xFFEDBB00), shape).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RText(text, 15, Color.White, bold = true, maxLines = 2, modifier = Modifier.weight(1f))
    }
}

@Composable
fun WhatsNewPopup(vm: AppViewModel) {
    if (vm.whatsNewSeen == WhatsNew.id || vm.annShowNow) return
    val ar = vm.lang == "ar"
    val shape = RoundedCornerShape(26.dp)
    androidx.compose.ui.window.Dialog(onDismissRequest = { vm.dismissWhatsNew() }) {
        Column(
            Modifier.fillMaxWidth(0.85f).clip(shape).background(tc(0xFF0B0616)).border(1.5.dp, tc(0xFFA78BFA), shape).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RText(if (ar) "جديد في هذا التحديث" else "New in this update", 24, Color.White, bold = true)
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (if (ar) WhatsNew.ar else WhatsNew.en).forEach { BodyText("• $it", 16, C.text, maxLines = 4) }
            }
            GlowButton(if (ar) "حسناً" else "Got it") { vm.dismissWhatsNew() }
        }
    }
}

/** إحصائية المشاهدة للأسبوع الحالي: مجموع الوقت وأكثر قناة/فيلم مشاهدة. */
@Composable
fun WatchStatsCard(vm: AppViewModel) {
    val ar = vm.lang == "ar"
    val w = vm.store.watchWeek()
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(tc(0x551B0B3A)).border(1.dp, tc(0x558B5CF6), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RText(if (ar) "شاهدت هذا الأسبوع" else "Watched this week", 17, Color.White, bold = true)
        if (w.total < 60L) {
            BodyText(if (ar) "ابدأ المشاهدة وستظهر الإحصائية هنا" else "Start watching to see your stats here", 14, C.textDim)
        } else {
            val h = w.total / 3600
            val m = (w.total % 3600) / 60
            RText(if (ar) "$h س $m د" else "${h}h ${m}m", 26, Color.White, bold = true)
            if (w.topName.isNotBlank()) BodyText((if (ar) "الأكثر مشاهدة: " else "Most watched: ") + w.topName, 14, C.textDim, maxLines = 1)
        }
    }
}
