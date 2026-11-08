package com.ayham.player.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Profile

class PStyle(val ar: String, val en: String, val c1: Color, val c2: Color, val bg: Int)
class PBg(val ar: String, val en: String, val a: Color, val b: Color, val c: Color)

/** هوية الملفات الشخصية البصرية: أنماط، ألوان، خلفيات، إطارات، ومكتبة صور سينمائية. */
object ProfileLook {
    // أول ثمانية = الأفاتارات القديمة (نفس الترتيب) حتى يبقى اختيار المستخدمين القدامى كما هو
    val emojis = listOf(
        "😀", "😎", "🦁", "🦊", "🚀", "🎬", "🐼", "👑",
        "🎥", "🍿", "🎭", "🎞️", "📽️", "🎟️", "🦸", "🦹",
        "🧙", "🧛", "🥷", "🤠", "👽", "🤖", "👻", "🐉",
        "🦅", "🐺", "🐯", "🦄", "🐙", "🦇", "🕷️", "🧸",
        "🐱", "🐶", "🐰", "🐸", "🦖", "🪐", "🌙", "⭐",
        "🔥", "💎", "🏆", "🎮", "🎧", "⚽", "🏎️", "🌹",
    )
    val styles = listOf(
        PStyle("سينمائي أسود", "Cinematic Black", Color(0xFFE5E7EB), Color(0xFF374151), 3),
        PStyle("بنفسجي ملكي", "Royal Purple", Color(0xFF8B5CF6), Color(0xFF4C1D95), 1),
        PStyle("أزرق منتصف الليل", "Midnight Blue", Color(0xFF3B82F6), Color(0xFF1E3A8A), 2),
        PStyle("أحمر داكن", "Dark Red", Color(0xFFEF4444), Color(0xFF7F1D1D), 0),
        PStyle("أخضر زمردي", "Emerald Green", Color(0xFF10B981), Color(0xFF064E3B), 4),
        PStyle("ذهبي فاخر", "Gold Luxury", Color(0xFFF5C451), Color(0xFF92400E), 5),
        PStyle("نيون سايبر", "Neon Cyber", Color(0xFF22D3EE), Color(0xFFD946EF), 6),
        PStyle("بسيط داكن", "Minimal Dark", Color(0xFF9CA3AF), Color(0xFF1F2937), 3),
    )
    val palette = listOf(
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF5C451), Color(0xFF84CC16),
        Color(0xFF10B981), Color(0xFF14B8A6), Color(0xFF22D3EE), Color(0xFF3B82F6), Color(0xFF6366F1), Color(0xFFE5E7EB),
    )
    val bgs = listOf(
        PBg("ستارة حمراء", "Red Curtain", Color(0xFF2B0A12), Color(0xFF12040A), Color(0xFFB91C1C)),
        PBg("شفق بنفسجي", "Violet Dusk", Color(0xFF1B0F3A), Color(0xFF0A0618), Color(0xFF7C3AED)),
        PBg("منتصف الليل", "Midnight", Color(0xFF0B1330), Color(0xFF050914), Color(0xFF2563EB)),
        PBg("نوار", "Noir", Color(0xFF16161A), Color(0xFF050506), Color(0xFF71717A)),
        PBg("غابة", "Forest", Color(0xFF07231A), Color(0xFF030D09), Color(0xFF059669)),
        PBg("غبار ذهبي", "Gold Dust", Color(0xFF2A1D06), Color(0xFF0E0902), Color(0xFFD97706)),
        PBg("سايبر", "Cyber", Color(0xFF0A1830), Color(0xFF1A0626), Color(0xFF06B6D4)),
        PBg("غروب", "Sunset", Color(0xFF2A0F1F), Color(0xFF0B0510), Color(0xFFF97316)),
    )
    val frames = listOf("بدون" to "None", "حلقة" to "Ring", "مزدوج" to "Double", "ذهبي" to "Gold", "نيون" to "Neon", "تاج" to "Crown")

    fun style(p: Profile) = styles[p.style.coerceIn(0, styles.lastIndex)]
    fun c1(p: Profile): Color = palette.getOrNull(p.color) ?: style(p).c1
    fun c2(p: Profile): Color = if (p.color in palette.indices) lerp(c1(p), Color.Black, 0.55f) else style(p).c2
    fun bg(p: Profile) = bgs[p.bg.coerceIn(0, bgs.lastIndex)]
    fun emoji(i: Int) = emojis[i.coerceIn(0, emojis.lastIndex)]
}

/** خلفية سينمائية للملف (تدرج داكن مع وهج بلون الخلفية المختارة). */
@Composable
fun ProfileBackdrop(p: Profile, modifier: Modifier = Modifier) {
    val b = ProfileLook.bg(p)
    Box(modifier.background(Brush.verticalGradient(listOf(b.a, b.b)))) {
        Box(Modifier.matchParentSize().background(Brush.radialGradient(listOf(b.c.copy(alpha = 0.38f), Color.Transparent))))
    }
}

/** صورة الملف: إيموجي على تدرج بلون الملف أو صورة من الجهاز، مع الإطار المختار. */
@Composable
fun ProfileAvatarView(vm: AppViewModel, p: Profile, size: Dp, modifier: Modifier = Modifier, glow: Boolean = false, photoBytes: ByteArray? = null) {
    val big = size >= 96.dp
    val shape: Shape = if (big) RoundedCornerShape(size * 0.22f) else CircleShape
    val a = ProfileLook.c1(p)
    val b = ProfileLook.c2(p)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (glow || p.frame == 4) {
            Box(
                Modifier.matchParentSize().graphicsLayer { scaleX = 1.45f; scaleY = 1.45f }
                    .background(Brush.radialGradient(listOf(a.copy(alpha = if (glow) 0.55f else 0.35f), Color.Transparent)), CircleShape),
            )
        }
        val frameMod = when (p.frame) {
            1, 4, 5 -> Modifier.border(3.dp, a, shape)
            2 -> Modifier.border(2.dp, a, shape).padding(5.dp).border(2.dp, Color.White.copy(alpha = 0.55f), shape)
            3 -> Modifier.border(4.dp, Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFFFDE68A))), shape)
            else -> Modifier
        }
        Box(
            Modifier.fillMaxSize().then(frameMod).clip(shape).background(Brush.linearGradient(listOf(a, b))),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = if (p.avatar < 0) remember(vm.photoVersion, p.id, photoBytes) {
                runCatching {
                    if (photoBytes != null) BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)?.asImageBitmap()
                    else BitmapFactory.decodeFile(vm.photoFileOf(p.id).absolutePath)?.asImageBitmap()
                }.getOrNull()
            } else null
            if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else RText(ProfileLook.emoji(if (p.avatar < 0) 0 else p.avatar), (size.value * 0.5f).toInt().coerceAtLeast(12), Color.White)
        }
        if (p.frame == 5) {
            RText("👑", (size.value * 0.28f).toInt().coerceAtLeast(12), Color.White, modifier = Modifier.align(Alignment.TopCenter).offset(y = -(size * 0.2f)))
        }
    }
}
