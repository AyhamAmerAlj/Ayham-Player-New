package com.ayham.player.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * ثيم واحد للتطبيق.
 * - الحقول الأساسية هي ألوان الواجهة الرئيسية (تُقرأ من C).
 * - remap: ألوان الصفحات الثابتة (البنفسجي الأصلي) -> لونها في هذا الثيم. المفتاح RRGGBB.
 *   الثيم الأصلي خريطته فارغة، فيبقى كل شيء كما هو بالضبط.
 */
class Palette(
    val id: String,
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val surfaceHi: Color,
    val primary: Color,
    val primaryLight: Color,
    val accent: Color,
    val text: Color,
    val textDim: Color,
    val danger: Color,
    val ok: Color,
    val remap: Map<Long, Long> = emptyMap(),
    /** عبارة تظهر في الشاشة الافتتاحية والقائمة الجانبية والإعدادات (ليست في الأصلي). */
    val tagline: String? = null,
    val taglineSub: String? = null,
    /** ألوان الشريط/الأزرار الرئيسية المتدرجة (blaugrana). null = الأصلي بدون تغيير. */
    val stripe: List<Color>? = null,
    /** لون ذهبي للتمييز (التقييم، العبارة). */
    val gold: Color? = null,
    /** لون توهج خفيف في زوايا الشاشة الرئيسية. */
    val glow: Color? = null,
)

/** كل الثيمات. لإضافة ثيم جديد: Palette جديدة + خريطة remap + إضافتها في all. */
object Themes {
    const val ORIGINAL = "original"
    const val BARCELONA = "barcelona"

    val original = Palette(
        id = ORIGINAL,
        bg = Color(0xFF0B0614),
        bg2 = Color(0xFF140A26),
        surface = Color(0xFF1C1033),
        surfaceHi = Color(0xFF2A1850),
        primary = Color(0xFF8B5CF6),
        primaryLight = if (Season.halloween) Color(0xFFFFB36B) else Color(0xFFC4A1FF),
        accent = if (Season.halloween) Color(0xFFFF8A1F) else Color(0xFFE879F9),
        text = Color(0xFFF5F0FF),
        textDim = Color(0xFFA99BC4),
        danger = Color(0xFFFF6B8B),
        ok = Color(0xFF5EEAD4),
    )

    /** خريطة ألوان برشلونة: كحلي وأسود للخلفيات، أزرق برشلونة للأساسي، خمري للثانوي. */
    private val barcelonaRemap: Map<Long, Long> = mapOf(
        // خلفيات كحلية داكنة وسوداء
        0x05020BL to 0x02050CL, 0x06020DL to 0x02050DL, 0x080414L to 0x030A18L,
        0x0A0418L to 0x040B1CL, 0x0A0518L to 0x050C1EL, 0x0B0614L to 0x050B1AL,
        0x0B0616L to 0x050B1AL, 0x0E0820L to 0x0A1428L, 0x0F0720L to 0x0A1428L,
        0x120A24L to 0x0C1A36L, 0x120A26L to 0x0C1A36L, 0x140A26L to 0x0D1A34L,
        0x140A2BL to 0x0D1A36L, 0x1A0B3AL to 0x102A52L, 0x1B0D3DL to 0x12305EL,
        0x1C1033L to 0x0E2243L, 0x2A1260L to 0x0E2A66L, 0x2A1850L to 0x163362L,
        0x3B1A7AL to 0x1B4078L, 0x3B1A86L to 0x1E4C94L,
        // أزرق برشلونة (الأساسي) وتدرجاته
        0x5B21B6L to 0x003A7AL, 0x6D28D9L to 0x00407FL, 0x7C3AEDL to 0x0A4F9AL,
        0x8B5CF6L to 0x004D98L, 0x8B1FD9L to 0x1F5FB0L, 0x9F67FFL to 0x3D86DCL,
        0xA855F7L to 0x2F7AD0L, 0xA78BFAL to 0x6AA6F0L, 0xC4A1FFL to 0x9CC7FFL,
        // أحمر خمري (الثانوي) وتدرجاته للتحديد والإضاءة
        0xD946EFL to 0xA50044L, 0xE879F9L to 0xD63A6EL,
        // نصوص: أبيض ورمادي فاتح
        0xA99BC4L to 0xA6B5CFL, 0xBDB2D6L to 0xB7C6DEL, 0xD9CCF5L to 0xD5E2F5L,
        0xE5DEF5L to 0xE6EEF9L, 0xF5F0FFL to 0xF4F8FFL,
        // تقييم النجوم: ذهبي برشلونة
        0xF5C518L to 0xEDBB00L,
    )

    val barcelona = Palette(
        id = BARCELONA,
        bg = Color(0xFF050B1A),
        bg2 = Color(0xFF0D1A34),
        surface = Color(0xFF0E2243),
        surfaceHi = Color(0xFF163362),
        primary = Color(0xFF004D98),
        primaryLight = Color(0xFF9CC7FF),
        accent = Color(0xFFB0124F),
        text = Color(0xFFFFFFFF),
        textDim = Color(0xFFC5CCD8),
        danger = Color(0xFFFF6B8B),
        ok = Color(0xFF5EEAD4),
        remap = barcelonaRemap,
        tagline = "Més que un club",
        taglineSub = "Est. 29 Nov 1899  ·  VisçaBarça",
        stripe = listOf(Color(0xFF004D98), Color(0xFFA50044)),
        gold = Color(0xFFEDBB00),
        glow = Color(0xFFA50044),
    )

    val all: List<Palette> = listOf(original, barcelona)

    fun byId(id: String): Palette = all.firstOrNull { it.id == id } ?: original
}

/** الثيم الحالي. تغييره يعيد رسم كل الشاشات فورًا. */
object ThemeState {
    var palette: Palette by mutableStateOf(Themes.original)
        private set

    val id: String get() = palette.id

    fun select(id: String) {
        palette = Themes.byId(id)
    }
}

/**
 * لون ثابت من الكود يتحوّل حسب الثيم الحالي.
 * في الثيم الأصلي يرجع اللون نفسه بدون أي تغيير.
 */
fun tc(argb: Long): Color {
    val remap = ThemeState.palette.remap
    if (remap.isEmpty()) return Color(argb)
    val to = remap[argb and 0xFFFFFFL] ?: return Color(argb)
    return Color((argb and 0xFF000000L) or to)
}
