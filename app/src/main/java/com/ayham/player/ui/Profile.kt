package com.ayham.player.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel

val avatarPresets: List<Pair<Pair<Color, Color>, String>> = listOf(
    (Color(0xFF8B5CF6) to Color(0xFF4C1D95)) to "😀",
    (Color(0xFFEC4899) to Color(0xFF831843)) to "😎",
    (Color(0xFFF59E0B) to Color(0xFF92400E)) to "🦁",
    (Color(0xFF10B981) to Color(0xFF064E3B)) to "🦊",
    (Color(0xFF3B82F6) to Color(0xFF1E3A8A)) to "🚀",
    (Color(0xFFEF4444) to Color(0xFF7F1D1D)) to "🎬",
    (Color(0xFF14B8A6) to Color(0xFF134E4A)) to "🐼",
    (Color(0xFFA855F7) to Color(0xFF581C87)) to "👑",
)

/** دائرة الملف الشخصي: صورة المستخدم أو أفاتار جاهز. */
@Composable
fun ProfileAvatar(vm: AppViewModel, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val idx = vm.profileAvatar
    if (idx < 0) {
        val bmp = remember(vm.photoVersion) {
            runCatching { BitmapFactory.decodeFile(vm.photoFile().absolutePath)?.asImageBitmap() }.getOrNull()
        }
        if (bmp != null) {
            Image(
                bmp, null,
                modifier.size(size).clip(CircleShape).border(2.dp, C.primary, CircleShape),
                contentScale = ContentScale.Crop,
            )
            return
        }
    }
    PresetAvatar(if (idx < 0) 0 else idx, size, modifier)
}

@Composable
fun PresetAvatar(index: Int, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val (colors, emoji) = avatarPresets[index.coerceIn(0, avatarPresets.lastIndex)]
    Box(
        modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(colors.first, colors.second)))
            .border(2.dp, Color(0x66FFFFFF), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        RText(emoji, (size.value * 0.5f).toInt().coerceAtLeast(12), Color.White)
    }
}

/** رمز QR لإرسال صورة الملف الشخصي من الهاتف إلى الشاشة. */
@Composable
fun PhotoQrCard(vm: AppViewModel, onDone: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var url by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val savedMsg = t("photo_saved")
    androidx.compose.runtime.DisposableEffect(Unit) {
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        val srv = com.ayham.player.data.LoginServer(onUpload = { _, bytes ->
            main.post {
                if (vm.setPhoto(bytes)) {
                    android.widget.Toast.makeText(ctx, savedMsg, android.widget.Toast.LENGTH_SHORT).show()
                    onDone()
                }
            }
        }, photoMode = true)
        val ip = com.ayham.player.data.LoginServer.localIp()
        if (ip != null && srv.start()) url = "http://$ip:${srv.port}"
        onDispose { srv.stop() }
    }
    androidx.compose.foundation.layout.Row(
        Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(Color(0x33000000)).padding(16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val u = url
        if (u != null) {
            val bmp = remember(u) { qrBitmap(u) }
            Box(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp)) {
                Image(bmp.asImageBitmap(), null, Modifier.size(150.dp))
            }
            androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                RText(t("photo_qr_help"), 16, C.text)
                BodyText(u, 13, C.primaryLight)
                GlowButton(t("close")) { onDone() }
            }
        } else {
            RText(t("qr_fail"), 16, C.danger)
        }
    }
}
