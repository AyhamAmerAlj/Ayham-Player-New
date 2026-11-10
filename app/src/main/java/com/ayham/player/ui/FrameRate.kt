package com.ayham.player.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

/**
 * اختيار معدل تحديث الشاشة الحقيقي (30 / 60 / 120 هرتز) لكل شاشة:
 * التصفح (الرئيسية والأفلام والمسلسلات…) والمشغّل. 0 = تلقائي (نظام الجهاز).
 * يُطبَّق عبر أوضاع العرض التي يدعمها الجهاز فعلاً، ولا يخترع ما لا يدعمه.
 */
object FrameRate {
    val choices = listOf(0, 30, 60, 120)
    var ui by mutableStateOf(0); private set
    var player by mutableStateOf(0); private set

    fun load(c: Context) {
        val sp = c.getSharedPreferences("ayham_player", Context.MODE_PRIVATE)
        ui = sp.getInt("fps_ui", 0); player = sp.getInt("fps_player", 0)
    }

    fun setUi(a: Activity, hz: Int) {
        ui = hz; a.getSharedPreferences("ayham_player", Context.MODE_PRIVATE).edit().putInt("fps_ui", hz).apply(); apply(a, hz)
    }

    fun setPlayer(a: Activity, hz: Int) {
        player = hz; a.getSharedPreferences("ayham_player", Context.MODE_PRIVATE).edit().putInt("fps_player", hz).apply()
    }

    /** الترددات المدعومة فعلياً بدقة الشاشة الحالية. */
    fun supported(a: Activity): List<Int> {
        val d = displayOf(a) ?: return emptyList()
        val cur = d.mode
        return d.supportedModes.filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .map { Math.round(it.refreshRate) }.distinct().sorted()
    }

    /** أقرب تردد مدعوم لما طُلب (يفضّل مضاعفاً صحيحاً: 30 على شاشة 60، وهكذا). */
    fun resolve(a: Activity, hz: Int): Int? {
        val s = supported(a); if (hz == 0 || s.isEmpty()) return null
        return s.firstOrNull { it == hz } ?: s.firstOrNull { it % hz == 0 } ?: s.minByOrNull { abs(it - hz) }
    }

    fun apply(a: Activity, hz: Int) {
        runCatching {
            val lp = a.window.attributes
            val d = displayOf(a)
            val target = resolve(a, hz)
            if (hz == 0 || target == null || d == null) {
                lp.preferredDisplayModeId = 0; lp.preferredRefreshRate = 0f
            } else {
                val cur = d.mode
                val m = d.supportedModes.firstOrNull {
                    it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight && Math.round(it.refreshRate) == target
                }
                lp.preferredDisplayModeId = m?.modeId ?: 0
                lp.preferredRefreshRate = target.toFloat()
            }
            a.window.attributes = lp
        }
    }

    @Suppress("DEPRECATION")
    private fun displayOf(a: Activity) =
        if (Build.VERSION.SDK_INT >= 30) a.display else a.windowManager.defaultDisplay

    fun label(hz: Int, ar: Boolean) = if (hz == 0) (if (ar) "تلقائي" else "Auto") else "$hz FPS"
}
