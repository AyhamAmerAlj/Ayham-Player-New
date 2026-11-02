package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel

/** بانر إعلان من لوحة الإدارة، بلون وأيقونة حسب النوع، ويمكن إغلاقه (لا يعود لنفس الإعلان). */
@Composable
fun AnnBanner(vm: AppViewModel, modifier: Modifier = Modifier) {
    val a = vm.annVisible ?: return
    val ar = vm.lang == "ar"
    val (ic, c1, c2) = when (a.kind) {
        "offer" -> Triple(Icons.Filled.CardGiftcard, Color(0xFFEDBB00), Color(0xFFD946EF))
        "maint" -> Triple(Icons.Filled.Build, Color(0xFFF97316), Color(0xFFEF4444))
        "new" -> Triple(Icons.Filled.AutoAwesome, Color(0xFF8B5CF6), Color(0xFFD946EF))
        else -> Triple(Icons.Filled.Info, Color(0xFF38BDF8), Color(0xFF8B5CF6))
    }
    val text = if (!ar && a.textEn.isNotBlank()) a.textEn else a.text
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.clip(shape).background(Brush.horizontalGradient(listOf(c1.copy(alpha = 0.28f), c2.copy(alpha = 0.28f), tc(0xDD0B0616))))
            .border(1.2.dp, Brush.horizontalGradient(listOf(c1, c2)), shape).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c1, c2))), contentAlignment = Alignment.Center) {
            Icon(ic, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        BodyText(text, 15, Color.White, bold = true, maxLines = 2, modifier = Modifier.weight(1f, fill = false))
        Focusable(shape = CircleShape, focusScale = 1.15f, onClick = { vm.dismissAnn() }) { f ->
            Box(Modifier.size(28.dp).clip(CircleShape).background(if (f) tc(0x88FFFFFF) else tc(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
