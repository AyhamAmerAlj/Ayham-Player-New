package com.ayham.player.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** نتيجة المقارنة: lagSec موجب = صوت القناة الخارجية متأخر عن صوت الفيديو الأصلي. */
class SyncEstimate(val lagSec: Double, val confidence: Double)

object SyncMath {
    private const val HOP = EnvelopeTap.HOP_S
    private const val MAX_LAG = 750        // ±15 ثانية
    private const val MIN_N = 1300         // 26 ثانية على الأقل
    private const val MAX_N = 2250         // 45 ثانية
    private const val HP = 50              // مرشّح تمرير عالٍ (ثانية واحدة)

    /** نمرّر الغلاف على مرشّح يحذف المستوى الثابت ويُبقي الأحداث (صيحات، صافرة، كلام). */
    private fun highPass(x: FloatArray): FloatArray {
        val n = x.size
        val pre = DoubleArray(n + 1)
        for (i in 0 until n) pre[i + 1] = pre[i] + (if (x[i].isNaN()) 0.0 else x[i].toDouble())
        val out = FloatArray(n)
        for (i in 0 until n) {
            if (x[i].isNaN()) { out[i] = Float.NaN; continue }
            val a = max(0, i - HP)
            val b = min(n, i + HP + 1)
            out[i] = (x[i] - (pre[b] - pre[a]) / (b - a)).toFloat()
        }
        return out
    }

    fun estimate(main: EnvelopeTap, ext: EnvelopeTap): SyncEstimate? {
        val rm = main.wallRange() ?: return null
        val re = ext.wallRange() ?: return null
        val t1 = min(rm.second, re.second)
        val t0min = max(rm.first, re.first)
        val n = min(MAX_N.toDouble(), (t1 - t0min) / HOP - 2).toInt()
        if (n < MIN_N) return null
        val t0 = t1 - n * HOP
        val m = FloatArray(n)
        main.fill(t0, m)
        val w = n + 2 * MAX_LAG
        val e = FloatArray(w)
        ext.fill(t0 - MAX_LAG * HOP, e)
        val mh = highPass(m)
        val eh = highPass(e)

        val rs = DoubleArray(2 * MAX_LAG + 1) { Double.NaN }
        for (lagIdx in 0..2 * MAX_LAG) {
            val off = lagIdx // e index = i + lagIdx  (lag = lagIdx - MAX_LAG)
            var sx = 0.0; var sy = 0.0; var sxx = 0.0; var syy = 0.0; var sxy = 0.0; var c = 0
            for (i in 0 until n) {
                val a = mh[i]
                val b = eh[i + off]
                if (a.isNaN() || b.isNaN()) continue
                sx += a; sy += b; sxx += a * a; syy += b * b; sxy += a * b; c++
            }
            if (c < n * 0.6) continue
            val vx = sxx - sx * sx / c
            val vy = syy - sy * sy / c
            if (vx <= 1e-9 || vy <= 1e-9) continue
            rs[lagIdx] = (sxy - sx * sy / c) / sqrt(vx * vy)
        }
        var best = -1
        for (i in rs.indices) if (!rs[i].isNaN() && (best < 0 || rs[i] > rs[best])) best = i
        if (best < 0) return null
        var second = Double.NEGATIVE_INFINITY
        for (i in rs.indices) {
            if (abs(i - best) < 25 || rs[i].isNaN()) continue // بعيد 0.5 ثانية عن القمة
            if (rs[i] > second) second = rs[i]
        }
        val margin = rs[best] - (if (second.isInfinite()) 0.0 else second)
        // ثقة = الارتباط الأعلى، بشرط تميّز القمة عن غيرها
        val conf = if (margin < 0.06) rs[best] * 0.5 else rs[best]
        return SyncEstimate((best - MAX_LAG) * HOP, conf)
    }
}
