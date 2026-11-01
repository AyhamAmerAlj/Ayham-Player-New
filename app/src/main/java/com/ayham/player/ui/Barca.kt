package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Matches
import com.ayham.player.data.NextMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** شريط قميص برشلونة (أزرق/خمري) بخط ذهبي رفيع. يظهر فقط في ثيم برشلونة. */
@Composable
fun StripeBar(modifier: Modifier = Modifier) {
    val st = ThemeState.palette.stripe ?: return
    val bands = 24
    val stops = ArrayList<Pair<Float, Color>>()
    for (i in 0 until bands) {
        val c = st[i % st.size]
        stops.add(i / bands.toFloat() to c)
        stops.add((i + 1) / bands.toFloat() to c)
    }
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(Brush.horizontalGradient(colorStops = stops.toTypedArray())))
        ThemeState.palette.gold?.let { Box(Modifier.fillMaxWidth().height(1.dp).background(it)) }
    }
}

/** بطاقة المباراة القادمة لبرشلونة مع عدّ تنازلي. الضغط عليها يفتح القنوات المباشرة للبحث عن الناقلة. */
@Composable
fun NextMatchCard(vm: AppViewModel, small: Boolean) {
    var match by remember { mutableStateOf<NextMatch?>(null) }
    LaunchedEffect(Unit) { match = withContext(Dispatchers.IO) { runCatching { Matches.next() }.getOrNull() } }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    val m = match ?: return
    val ar = vm.lang == "ar"
    val diff = m.epochMs - now
    val count = when {
        diff <= -2 * 3600_000L -> return
        diff <= 0 -> if (ar) "جارية الآن" else "Live now"
        else -> {
            val mins = diff / 60_000
            val d = mins / 1440; val h = (mins % 1440) / 60; val mm = mins % 60
            if (ar) (if (d > 0) "بعد $d يوم و$h س" else if (h > 0) "بعد $h س و$mm د" else "بعد $mm د")
            else (if (d > 0) "In ${d}d ${h}h" else if (h > 0) "In ${h}h ${mm}m" else "In ${mm}m")
        }
    }
    val day = remember(m.epochMs, ar) { SimpleDateFormat("EEEE", if (ar) Locale("ar") else Locale.ENGLISH).format(Date(m.epochMs)) }
    val dt = remember(m.epochMs) { SimpleDateFormat("d/M  ·  hh:mm a", Locale.ENGLISH).format(Date(m.epochMs)) }
    val shape = RoundedCornerShape(18.dp)
    Focusable(Modifier.widthIn(max = 300.dp), shape, 1.05f, onClick = { vm.selectTab(1) }) { f ->
        Column(
            Modifier.clip(shape).background(Brush.verticalGradient(listOf(tc(0xDD0C1A36), tc(0xEE050B1A))))
                .border(1.2.dp, if (f) C.gold else tc(0x55EDBB00), shape)
                .padding(horizontal = 16.dp, vertical = if (small) 8.dp else 12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
        ) {
            RText(if (ar) "المباراة القادمة" else "Next match", 13, C.gold, bold = true, maxLines = 1)
            RText(m.home + "  vs  " + m.away, if (small) 15 else 17, Color.White, bold = true, maxLines = 2)
            if (!small && m.league.isNotBlank()) BodyText(m.league, 13, C.primaryLight, maxLines = 1)
            BodyText("$day  $dt", 13, C.textDim, maxLines = 1)
            RText(count, 16, C.gold, bold = true, maxLines = 1)
            if (!small) BodyText(if (ar) "اضغط للبحث عن القناة الناقلة" else "Press to find the channel", 12, C.textDim, maxLines = 1)
        }
    }
}
