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
    /** تحويل الألوان البنفسجية الثابتة في الشاشات حسب سطوعها (بدل خريطة صريحة). */
    val recolor: ((Long) -> Long)? = null,
)

/** كل الثيمات. لإضافة ثيم جديد: Palette جديدة + خريطة remap + إضافتها في all. */
object Themes {
    const val ORIGINAL = "original"
    const val BARCELONA = "barcelona"
    const val GTA6 = "gta6"
    const val GOT = "got"

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

    /** ثيم مستوحى من ملصق GTA VI: ليل كحلي، وردي، برتقالي غروب، كريمي. */
    val gta6 = Palette(
        id = GTA6,
        bg = Color(0xFF0A0A24),
        bg2 = Color(0xFF140F38),
        surface = Color(0xFF1A1450),
        surfaceHi = Color(0xFF2B2070),
        primary = Color(0xFFE0457F),
        primaryLight = Color(0xFFFFB3C7),
        accent = Color(0xFFFF8A5B),
        text = Color(0xFFFFF4EA),
        textDim = Color(0xFFB9B0D6),
        danger = Color(0xFFFF6B8B),
        ok = Color(0xFF5EEAD4),
        recolor = ::viceRecolor,
        tagline = "Welcome to Vice City",
        taglineSub = "GTA VI  ·  19 Nov 2026",
        stripe = listOf(Color(0xFF3ED6C8), Color(0xFFFF5FA2), Color(0xFFFF8A5B)),
        gold = Color(0xFFFFB36B),
        glow = Color(0xFFC2407F),
    )

    /** ثيم Game of Thrones: فولاذي وأسود مع لمسات ذهبية، مستوحى من شعار آل ستارك وبوسترات المسلسل. */
    val got = Palette(
        id = GOT,
        bg = Color(0xFF07090C),
        bg2 = Color(0xFF12161B),
        surface = Color(0xFF12161B),
        surfaceHi = Color(0xFF1E252D),
        primary = Color(0xFF3E4C5A),
        primaryLight = Color(0xFFC9D3DD),
        accent = Color(0xFFC9A227),
        text = Color(0xFFF2F4F7),
        textDim = Color(0xFF9AA5B1),
        danger = Color(0xFFFF6B8B),
        ok = Color(0xFF5EEAD4),
        recolor = ::gotRecolor,
        tagline = "Winter is Coming",
        taglineSub = "Game of Thrones  ·  House Stark",
        stripe = listOf(Color(0xFF3E4C5A), Color(0xFFC9A227)),
        gold = Color(0xFFC9A227),
        glow = Color(0xFF3E4C5A),
    )

    val all: List<Palette> = listOf(original, barcelona, gta6, got)

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
    val p = ThemeState.palette
    val remap = p.remap
    if (remap.isEmpty()) {
        val rc = p.recolor ?: return Color(argb)
        return Color(rc(argb))
    }
    val to = remap[argb and 0xFFFFFFL] ?: return Color(argb)
    return Color((argb and 0xFF000000L) or to)
}

/** تدرج غروب فيسي سيتي حسب السطوع: من الليل الكحلي إلى الوردي والبرتقالي والكريمي. */
private val viceStops: List<Pair<Float, Long>> = listOf(
    0.00f to 0xFF0A0A24L, 0.20f to 0xFF1E1A4FL, 0.40f to 0xFF5A2A7AL,
    0.60f to 0xFFC2407FL, 0.80f to 0xFFFF8A6BL, 1.00f to 0xFFFFF1E0L,
)

/** يحوّل الألوان البنفسجية/الوردية (الثابتة في الكود) إلى تدرج الغروب، ويترك الباقي كما هو. */
private fun viceRecolor(argb: Long): Long {
    val r = ((argb shr 16) and 0xFFL) / 255f
    val g = ((argb shr 8) and 0xFFL) / 255f
    val b = (argb and 0xFFL) / 255f
    val mx = maxOf(r, maxOf(g, b))
    val mn = minOf(r, minOf(g, b))
    val d = mx - mn
    if (d < 0.02f) return argb
    val l = (mx + mn) / 2f
    val s = d / (1f - kotlin.math.abs(2f * l - 1f))
    if (s < 0.35f) return argb
    var h = when (mx) {
        r -> (g - b) / d
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } * 60f
    if (h < 0f) h += 360f
    if (h < 220f || h > 335f) return argb
    var i = 0
    while (i < viceStops.size - 2 && l > viceStops[i + 1].first) i++
    val (t0, c0) = viceStops[i]
    val (t1, c1) = viceStops[i + 1]
    val f = ((l - t0) / (t1 - t0)).coerceIn(0f, 1f)
    fun mix(shift: Int): Long {
        val a = ((c0 shr shift) and 0xFFL).toFloat()
        val c = ((c1 shr shift) and 0xFFL).toFloat()
        return (a + (c - a) * f).toLong().coerceIn(0L, 255L)
    }
    return (argb and 0xFF000000L) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
}

/** تدرج فولاذي وأسود: الألوان البنفسجية الثابتة في الكود تتحول إليه حسب السطوع. */
private val gotStops: List<Pair<Float, Long>> = listOf(
    0.00f to 0xFF07090CL, 0.25f to 0xFF1A2129L, 0.50f to 0xFF3E4C5AL,
    0.75f to 0xFF8FA0B0L, 1.00f to 0xFFE8EEF4L,
)

/** يحوّل الألوان البنفسجية/الوردية إلى تدرج فولاذي للثيم، ويترك الباقي كما هو. */
private fun gotRecolor(argb: Long): Long {
    val r = ((argb shr 16) and 0xFFL) / 255f
    val g = ((argb shr 8) and 0xFFL) / 255f
    val b = (argb and 0xFFL) / 255f
    val mx = maxOf(r, maxOf(g, b))
    val mn = minOf(r, minOf(g, b))
    val d = mx - mn
    if (d < 0.02f) return argb
    val l = (mx + mn) / 2f
    val s = d / (1f - kotlin.math.abs(2f * l - 1f))
    if (s < 0.35f) return argb
    var h = when (mx) {
        r -> (g - b) / d
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } * 60f
    if (h < 0f) h += 360f
    if (h < 220f || h > 335f) return argb
    var i = 0
    while (i < gotStops.size - 2 && l > gotStops[i + 1].first) i++
    val (t0, c0) = gotStops[i]
    val (t1, c1) = gotStops[i + 1]
    val f = ((l - t0) / (t1 - t0)).coerceIn(0f, 1f)
    fun mix(shift: Int): Long {
        val a = ((c0 shr shift) and 0xFFL).toFloat()
        val c = ((c1 shr shift) and 0xFFL).toFloat()
        return (a + (c - a) * f).toLong().coerceIn(0L, 255L)
    }
    return (argb and 0xFF000000L) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
}
