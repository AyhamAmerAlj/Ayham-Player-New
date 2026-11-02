package com.ayham.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Weather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** تفاصيل اختيارية بجانب الساعة: الطقس والمنطقة (بخط التطبيق). لا تعرض شيئًا لو الخيارين مطفأين أو فشل الطقس. */
@Composable
fun ClockExtras(vm: AppViewModel) {
    var w by remember { mutableStateOf<Weather.W?>(null) }
    val wOn = vm.clockWeather
    val city = Weather.cities[vm.clockCity.coerceIn(0, Weather.cities.lastIndex)]
    LaunchedEffect(wOn, vm.clockCity) {
        w = null
        if (!wOn) return@LaunchedEffect
        while (true) {
            withContext(Dispatchers.IO) { Weather.fetch(vm.clockCity) }?.let { w = it }
            delay(30 * 60_000L)
        }
    }
    if (vm.clockRegion) RText((if (vm.lang == "ar") city.ar else city.en).substringBefore(" ("), 15, C.textDim, bold = true, maxLines = 1)
    val cur = w
    if (wOn && cur != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val (ic, col) = when {
                cur.code >= 95 -> Icons.Filled.Thunderstorm to Color(0xFFFACC15)
                cur.code in 71..77 || cur.code in 85..86 -> Icons.Filled.AcUnit to Color(0xFF7DD3FC)
                cur.code in 51..67 || cur.code in 80..82 -> Icons.Filled.WaterDrop to Color(0xFF60A5FA)
                cur.code in 45..48 -> Icons.Filled.Cloud to Color(0xFF9CA3AF)
                cur.code == 3 -> Icons.Filled.Cloud to Color(0xFFCBD5E1)
                cur.code in 1..2 -> Icons.Filled.WbCloudy to Color(0xFFE2E8F0)
                !cur.day -> Icons.Filled.NightsStay to Color(0xFFC4B5FD)
                else -> Icons.Filled.WbSunny to Color(0xFFFBBF24)
            }
            Icon(ic, null, tint = col, modifier = Modifier.size(20.dp))
            RText("${cur.temp}°", 15, Color.White, bold = true, maxLines = 1)
        }
    }
}
