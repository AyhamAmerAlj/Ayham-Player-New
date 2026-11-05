package com.ayham.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorMatrix as CM
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Store

/** قيم الصورة: كلها بالمدى -50..50 (والصبغة -30..30)، والصفر = بدون تغيير. */
data class VVals(val b: Int = 0, val c: Int = 0, val s: Int = 0, val h: Int = 0, val t: Int = 0) {
    val neutral get() = b == 0 && c == 0 && s == 0 && h == 0 && t == 0
    fun ser() = "$b,$c,$s,$h,$t"
    companion object {
        fun parse(x: String?): VVals? = x?.split(",")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 5 }?.let { VVals(it[0], it[1], it[2], it[3], it[4]) }
    }
}

data class VMode(val id: String, val ar: String, val en: String, val descAr: String, val descEn: String, val icon: String)

object Vision {
    val builtin = listOf(
        VMode("std", "عادي", "Standard", "الصورة الأصلية بدون أي تعديل", "Original picture, no changes", "◐"),
        VMode("cinema", "سينما", "Cinema", "ألوان دافئة وتباين عميق للأفلام", "Warm tones and deep contrast for movies", "🎬"),
        VMode("series", "مسلسلات", "Series", "صورة متوازنة مريحة للمشاهدة الطويلة", "Balanced, comfortable for long viewing", "📺"),
        VMode("sports", "رياضة", "Sports", "ألوان حيّة وسطوع أعلى", "Vivid colors and higher brightness", "🏅"),
        VMode("football", "كرة القدم", "Football", "عشب أنضر وألوان أقمصة أوضح", "Richer turf and clearer kits", "⚽"),
        VMode("hdr", "HDR", "HDR", "يحافظ على HDR الأصلي بلا معالجة", "Keeps native HDR untouched", "✨"),
        VMode("custom", "مخصص", "Custom", "إعداداتك الخاصة", "Your own settings", "🎛"),
    )
    fun default(id: String) = when (id) {
        "cinema" -> VVals(b = -5, c = 10, s = -5, t = 10)
        "series" -> VVals(c = 5, s = 5, t = 5)
        "sports" -> VVals(b = 3, c = 12, s = 20, t = -5)
        "football" -> VVals(b = 2, c = 10, s = 25)
        else -> VVals()
    }

    /** تأثيرات Media3 الرسمية: سطوع، تباين، تشبّع/صبغة، حرارة اللون. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun effects(v: VVals): List<androidx.media3.common.Effect> {
        val l = ArrayList<androidx.media3.common.Effect>()
        if (v.b != 0) l.add(androidx.media3.effect.Brightness(v.b * 0.006f))
        if (v.c != 0) l.add(androidx.media3.effect.Contrast(v.c / 100f))
        if (v.s != 0 || v.h != 0) l.add(androidx.media3.effect.HslAdjustment.Builder().adjustHue(v.h.toFloat()).adjustSaturation(v.s * 1.2f).build())
        if (v.t != 0) l.add(androidx.media3.effect.RgbAdjustment.Builder().setRedScale(1f + v.t / 250f).setBlueScale(1f - v.t / 250f).build())
        return l
    }

    /** مصفوفة لون حقيقية تُطبَّق على طبقة الفيديو بالمعالج الرسومي (بدون معالجة إطارات بالمعالج). */
    fun matrix(v: VVals): android.graphics.ColorMatrix {
        val m = android.graphics.ColorMatrix()
        m.postConcat(android.graphics.ColorMatrix().apply { setSaturation(1f + v.s / 100f) })
        if (v.h != 0) {
            val r = Math.toRadians(v.h.toDouble()); val c = Math.cos(r).toFloat(); val s = Math.sin(r).toFloat()
            m.postConcat(android.graphics.ColorMatrix(floatArrayOf(
                0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
                0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
                0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )))
        }
        val k = 1f + v.c / 100f
        val off = (1f - k) * 127.5f + v.b * 1.5f
        val rs = 1f + v.t / 250f; val bs = 1f - v.t / 250f
        m.postConcat(android.graphics.ColorMatrix(floatArrayOf(
            k * rs, 0f, 0f, 0f, off,
            0f, k, 0f, 0f, off,
            0f, 0f, k * bs, 0f, off,
            0f, 0f, 0f, 1f, 0f,
        )))
        return m
    }
}

/** تفضيلات Ayham Vision: التعديل مستقل لكل وضع ويُحفظ تلقائياً. */
class VisionPrefs(private val st: Store) {
    var auto by mutableStateOf(st.raw("v_auto") == "1")
        private set
    var mode by mutableStateOf(st.raw("v_mode") ?: "std")
        private set
    private val vals = mutableStateMapOf<String, VVals>()
    val customs = androidx.compose.runtime.mutableStateListOf<Pair<String, String>>() // id -> الاسم

    init {
        Vision.builtin.forEach { vals[it.id] = VVals.parse(st.raw("v_ov_" + it.id)) ?: Vision.default(it.id) }
        (st.raw("v_customs") ?: "").lines().filter { it.contains("|") }.forEach {
            val p = it.split("|", limit = 2)
            customs.add(p[0] to p[1]); vals[p[0]] = VVals.parse(st.raw("v_ov_" + p[0])) ?: VVals()
        }
    }

    fun toggleAuto(v: Boolean) { auto = v; st.putRaw("v_auto", if (v) "1" else "0") }
    fun select(id: String) { mode = id; st.putRaw("v_mode", id) }
    fun vals(id: String): VVals = vals[id] ?: VVals()
    fun set(id: String, v: VVals) { vals[id] = v; st.putRaw("v_ov_$id", v.ser()) }
    fun reset(id: String) = set(id, Vision.default(id))
    fun name(id: String, ar: Boolean): String = Vision.builtin.firstOrNull { it.id == id }?.let { if (ar) it.ar else it.en }
        ?: customs.firstOrNull { it.first == id }?.second ?: id
    fun isCustom(id: String) = customs.any { it.first == id }
    fun addCustom(name: String, from: VVals): String {
        val id = "c" + System.currentTimeMillis()
        customs.add(id to name.trim().take(24).replace("|", " ").replace("\n", " "))
        st.putRaw("v_customs", customs.joinToString("\n") { it.first + "|" + it.second })
        set(id, from)
        return id
    }
    fun deleteCustom(id: String) {
        customs.removeAll { it.first == id }; vals.remove(id)
        st.putRaw("v_customs", customs.joinToString("\n") { it.first + "|" + it.second })
        if (mode == id) select("std")
    }

    /** الوضع التلقائي حسب نوع المحتوى. */
    fun autoModeFor(live: Boolean, series: Boolean, title: String): String {
        if (live) {
            val n = title.lowercase()
            return when {
                Regex("(كرة|مباراة|football|soccer|\\bfc\\b|liga|premier|champions|دوري|كأس)").containsMatchIn(n) -> "football"
                Regex("(sport|bein|ssc|رياض|arena|espn|dazn|nba|ufc)").containsMatchIn(n) -> "sports"
                else -> "std"
            }
        }
        return if (series) "series" else "cinema"
    }
}

/** لوحة Ayham Vision داخل المشغّل. */
@Composable
fun VisionPanel(vm: AppViewModel, active: String, isHdr: Boolean, onPick: (String) -> Unit, onBack: () -> Unit) {
    val ar = vm.lang == "ar"
    val v = vm.vision
    var newName by remember { mutableStateOf("") }
    val cur = v.vals(active)
    val editable = active != "std" && active != "hdr"
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { RText("Ayham Vision", 26, C.text, bold = true) }
        if (isHdr) item { BodyText(if (ar) "هذا المحتوى HDR: تُحفظ الصورة الأصلية ولا تُطبَّق الفلاتر" else "HDR content: original picture is kept, filters are off", 13, C.accent) }
        item { PanelItem(if (ar) "التفعيل التلقائي حسب المحتوى" else "Auto by content", sub = if (v.auto) (if (ar) "مفعّل" else "On") else (if (ar) "يدوي" else "Manual"), modifier = Modifier.autoFocus()) { v.toggleAuto(!v.auto) } }
        items(Vision.builtin.size) { i ->
            val m = Vision.builtin[i]
            PanelItem((if (m.id == active) "● " else "") + m.icon + "  " + (if (ar) m.ar else m.en), selected = m.id == active, sub = if (ar) m.descAr else m.descEn) { onPick(m.id) }
        }
        items(v.customs.size) { i ->
            val (id, nm) = v.customs[i]
            PanelItem((if (id == active) "● " else "") + "🎛  " + nm, selected = id == active, sub = if (ar) "وضع أنشأته" else "Your mode") { onPick(id) }
        }
        if (editable && !isHdr) {
            item { RText((if (ar) "تعديل: " else "Edit: ") + v.name(active, ar), 18, C.primaryLight, bold = true) }
            val rows = listOf(
                Triple(if (ar) "السطوع" else "Brightness", cur.b, 50) to { x: Int -> cur.copy(b = x) },
                Triple(if (ar) "التباين" else "Contrast", cur.c, 50) to { x: Int -> cur.copy(c = x) },
                Triple(if (ar) "التشبّع" else "Saturation", cur.s, 50) to { x: Int -> cur.copy(s = x) },
                Triple(if (ar) "الصبغة" else "Hue", cur.h, 30) to { x: Int -> cur.copy(h = x) },
                Triple(if (ar) "حرارة اللون" else "Temperature", cur.t, 50) to { x: Int -> cur.copy(t = x) },
            )
            items(rows.size) { i ->
                val (meta, mk) = rows[i]
                val (label, value, lim) = meta
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RText(label, 16, C.text, modifier = Modifier.weight(1f))
                    Chip("−", false) { v.set(active, mk((value - 5).coerceAtLeast(-lim))) }
                    RText((if (value > 0) "+" else "") + value, 16, C.accent, bold = true)
                    Chip("+", false) { v.set(active, mk((value + 5).coerceAtMost(lim))) }
                }
            }
            item { PanelItem(if (ar) "إعادة ضبط هذا الوضع" else "Reset this mode") { v.reset(active) } }
            if (v.isCustom(active)) item { PanelItem(if (ar) "حذف هذا الوضع" else "Delete this mode") { v.deleteCustom(active); onPick("std") } }
            item { AyhamField(newName, { newName = it }, if (ar) "اسم وضع جديد" else "New mode name", Modifier.fillMaxWidth()) }
            item {
                PanelItem(if (ar) "حفظ الإعدادات الحالية كوضع جديد" else "Save current as new mode") {
                    if (newName.isNotBlank()) { onPick(v.addCustom(newName, cur)); newName = "" }
                }
            }
        }
        item { BodyText(if (ar) "يؤثر على صورة المشغّل فقط وليس على إعدادات التلفاز. الحدّة وتقليل الضوضاء ووضوح الحركة غير مدعومة في المحرك الحالي." else "Affects the player picture only, not TV settings. Sharpness, noise reduction and motion clarity are not supported by the current engine.", 12, C.textDim) }
        item { PanelItem(t("back")) { onBack() } }
    }
}
