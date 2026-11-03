package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ayham.player.AppViewModel

/** نافذة إعلان من لوحة الإدارة في وسط الشاشة. تظهر مرة لكل إعلان جديد، وتُعاد من الإعدادات. */
@Composable
fun AnnPopup(vm: AppViewModel) {
    if (!vm.annShowNow) return
    val a = vm.ann ?: return
    val ar = vm.lang == "ar"
    val (ic, c1, c2) = when (a.kind) {
        "offer" -> Triple(Icons.Filled.CardGiftcard, Color(0xFFEDBB00), Color(0xFFD946EF))
        "maint" -> Triple(Icons.Filled.Build, Color(0xFFF97316), Color(0xFFEF4444))
        "new" -> Triple(Icons.Filled.AutoAwesome, Color(0xFF8B5CF6), Color(0xFFD946EF))
        else -> Triple(Icons.Filled.Info, Color(0xFF38BDF8), Color(0xFF8B5CF6))
    }
    val text = if (!ar && a.textEn.isNotBlank()) a.textEn else a.text
    val shape = RoundedCornerShape(26.dp)
    Dialog(onDismissRequest = { vm.dismissAnn() }) {
        Column(
            Modifier.fillMaxWidth(0.8f).clip(shape)
                .background(Brush.verticalGradient(listOf(c1.copy(alpha = 0.35f), tc(0xFF0B0616))))
                .border(1.5.dp, Brush.horizontalGradient(listOf(c1, c2)), shape)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Brush.linearGradient(listOf(c1, c2))), contentAlignment = Alignment.Center) {
                Icon(ic, null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            RText(if (ar) "إعلان" else "Announcement", 22, Color.White, bold = true)
            BodyText(text, 18, Color.White, maxLines = 6)
            Focusable(Modifier.autoFocus(), RoundedCornerShape(24.dp), 1.06f, onClick = { vm.dismissAnn() }) { f ->
                Box(Modifier.clip(RoundedCornerShape(24.dp)).background(if (f) c2 else c1).padding(horizontal = 28.dp, vertical = 10.dp)) {
                    RText(if (ar) "حسنًا" else "OK", 17, Color.White, bold = true)
                }
            }
        }
    }
}
