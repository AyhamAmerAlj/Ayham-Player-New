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
    const val DARK = "dark"
    const val LIGHT = "light"
    const val AYHAM = "ayham"

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

    /** Dark Mode: أسود محايد بدل البنفسجي، والتمييز بالرمادي الفضي. */
    private val darkRemap: Map<Long, Long> = mapOf(
        0x05020BL to 0x050505L, 0x06020DL to 0x050505L, 0x080414L to 0x08080AL, 0x0A0418L to 0x08080AL,
        0x0A0518L to 0x0A0A0CL, 0x0B0614L to 0x0A0A0CL, 0x0B0616L to 0x0A0A0CL, 0x0E0820L to 0x0F0F13L,
        0x0F0720L to 0x0F0F13L, 0x120A24L to 0x121216L, 0x120A26L to 0x121216L, 0x140A26L to 0x141418L,
        0x140A2BL to 0x141418L, 0x1A0B3AL to 0x1A1A20L, 0x1B0D3DL to 0x1B1B22L, 0x1C1033L to 0x18181EL,
        0x2A1260L to 0x24242EL, 0x2A1850L to 0x24242EL, 0x3B1A7AL to 0x2E2E3AL, 0x3B1A86L to 0x2E2E3AL,
        0x8B5CF6L to 0x475569L, 0x6D28D9L to 0x334155L, 0x7C3AEDL to 0x3F4A5CL, 0xA855F7L to 0x526071L,
        0x8B1FD9L to 0x3F4A5CL, 0x9F67FFL to 0x64748BL, 0xA78BFAL to 0xB6C2D0L, 0xC4A1FFL to 0xCBD5E1L,
        0xD946EFL to 0x94A3B8L, 0xE879F9L to 0x94A3B8L,
    )

    /** Light Mode: خلفية فاتحة ونصوص داكنة، والبنفسجي يبقى للتمييز بدرجات أغمق. */
    private val lightRemap: Map<Long, Long> = mapOf(
        0x05020BL to 0xF7F7FBL, 0x06020DL to 0xF7F7FBL, 0x080414L to 0xF3F3F8L, 0x0A0418L to 0xF3F3F8L,
        0x0A0518L to 0xF3F3F8L, 0x0B0614L to 0xF7F7FBL, 0x0B0616L to 0xF7F7FBL, 0x0E0820L to 0xEEEEF5L,
        0x0F0720L to 0xEEEEF5L, 0x120A24L to 0xEAEAF2L, 0x120A26L to 0xEAEAF2L, 0x140A26L to 0xE8E8F0L,
        0x140A2BL to 0xE8E8F0L, 0x1A0B3AL to 0xE2E2EEL, 0x1B0D3DL to 0xE2E2EEL, 0x1C1033L to 0xFFFFFFL,
        0x2A1260L to 0xDCDCE8L, 0x2A1850L to 0xE6E6F0L, 0x3B1A7AL to 0xD4D4E4L, 0x3B1A86L to 0xD4D4E4L,
        0xF5F0FFL to 0x111827L, 0xE5DEF5L to 0x1F2937L, 0xD9CCF5L to 0x1F2937L, 0xBDB2D6L to 0x374151L,
        0xA99BC4L to 0x4B5563L, 0x8B5CF6L to 0x7C3AEDL, 0x9F67FFL to 0x7C3AEDL, 0xA78BFAL to 0x6D28D9L,
        0xC4A1FFL to 0x5B21B6L, 0xE879F9L to 0xC026D3L, 0xD946EFL to 0xC026D3L,
    )

    val dark = Palette(
        id = DARK,
        bg = Color(0xFF050505), bg2 = Color(0xFF0F0F13), surface = Color(0xFF18181E), surfaceHi = Color(0xFF24242E),
        primary = Color(0xFF475569), primaryLight = Color(0xFFCBD5E1), accent = Color(0xFF94A3B8),
        text = Color(0xFFF5F5F7), textDim = Color(0xFF9CA3AF), danger = Color(0xFFFF6B8B), ok = Color(0xFF5EEAD4),
        remap = darkRemap,
    )

    val light = Palette(
        id = LIGHT,
        bg = Color(0xFFF7F7FB), bg2 = Color(0xFFEEEEF5), surface = Color(0xFFFFFFFF), surfaceHi = Color(0xFFE6E6F0),
        primary = Color(0xFF7C3AED), primaryLight = Color(0xFF5B21B6), accent = Color(0xFFC026D3),
        text = Color(0xFF111827), textDim = Color(0xFF4B5563), danger = Color(0xFFDC2626), ok = Color(0xFF059669),
        remap = lightRemap,
    )

    /** Ayham: ألوان غروب (وردي وبرتقالي على خلفية بنفسجية داكنة) من تصميم Ayham نفسه. */
    private val ayhamRemap: Map<Long, Long> = mapOf(
        0x05020BL to 0x0E0616L, 0x0B0614L to 0x140A1FL, 0x0A0518L to 0x140A1FL, 0x0E0820L to 0x1A0D28L,
        0x120A26L to 0x1E0F2EL, 0x140A26L to 0x1E0F2EL, 0x1C1033L to 0x2A1538L, 0x2A1260L to 0x3A1A4AL,
        0x2A1850L to 0x3D1F4DL, 0x3B1A7AL to 0x4A2360L,
        0x8B5CF6L to 0xE0508AL, 0x6D28D9L to 0xB83A6EL, 0x7C3AEDL to 0xC8457AL, 0xA855F7L to 0xE8649AL,
        0x8B1FD9L to 0xB83A6EL, 0x9F67FFL to 0xF07A8EL, 0xA78BFAL to 0xFFB38AL, 0xC4A1FFL to 0xFFC9A8L,
        0xD946EFL to 0xFF9F5AL, 0xE879F9L to 0xFF9F5AL,
    )

    val ayham = Palette(
        id = AYHAM,
        bg = Color(0xFF140A1F), bg2 = Color(0xFF1E0F2E), surface = Color(0xFF2A1538), surfaceHi = Color(0xFF3D1F4D),
        primary = Color(0xFFE0508A), primaryLight = Color(0xFFFFB38A), accent = Color(0xFFFF9F5A),
        text = Color(0xFFFFF4EE), textDim = Color(0xFFC9A8B8), danger = Color(0xFFFF6B8B), ok = Color(0xFF5EEAD4),
        remap = ayhamRemap,
    )

    val all: List<Palette> = listOf(original, barcelona, dark, light, ayham)

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
