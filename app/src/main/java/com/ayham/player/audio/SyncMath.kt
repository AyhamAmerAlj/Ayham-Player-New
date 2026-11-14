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

    /** ارتباط كل إزاحة (NaN حيث لا توجد بيانات كافية). */
    private fun correlate(mh: FloatArray, eh: FloatArray, n: Int): DoubleArray {
        val rs = DoubleArray(2 * MAX_LAG + 1) { Double.NaN }
        for (off in 0..2 * MAX_LAG) {
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
            rs[off] = (sxy - sx * sy / c) / sqrt(vx * vy)
        }
        return rs
    }

    private fun argmax(r: DoubleArray): Int {
        var best = -1
        for (i in r.indices) if (!r[i].isNaN() && (best < 0 || r[i] > r[best])) best = i
        return best
    }

    /**
     * مزامنة بصوت الجمهور: نقارن ثلاثة نطاقات (هدير منخفض، همس عالٍ، والكل) بين صوت الفيديو وصوت التعليق.
     * المعلّقان مختلفان فكلامهما لا يتطابق، بينما صيحات الجمهور والصافرة تحدث في نفس اللحظة، فيظهر الفرق الحقيقي.
     */
    fun estimate(main: EnvelopeTap, ext: EnvelopeTap): SyncEstimate? {
        val rm = main.wallRange() ?: return null
        val re = ext.wallRange() ?: return null
        val t1 = min(rm.second, re.second)
        val t0min = max(rm.first, re.first)
        val n = min(MAX_N.toDouble(), (t1 - t0min) / HOP - 2).toInt()
        if (n < MIN_N) return null
        val t0 = t1 - n * HOP
        val w = n + 2 * MAX_LAG
        val weights = doubleArrayOf(0.5, 1.0, 1.0)
        val per = ArrayList<DoubleArray>()
        val ws = ArrayList<Double>()
        for (b in 0..2) {
            val m = FloatArray(n); main.fill(t0, m, b)
            val e = FloatArray(w); ext.fill(t0 - MAX_LAG * HOP, e, b)
            per.add(correlate(highPass(m), highPass(e), n))
            ws.add(weights[b])
        }
        val comb = DoubleArray(2 * MAX_LAG + 1) { i ->
            var sum = 0.0; var wt = 0.0
            for (b in 0..2) { val v = per[b][i]; if (!v.isNaN()) { sum += v * ws[b]; wt += ws[b] } }
            if (wt >= 1.5) sum / wt else Double.NaN
        }
        val best = argmax(comb)
        if (best < 0) return null
        var second = Double.NEGATIVE_INFINITY
        for (i in comb.indices) {
            if (abs(i - best) < 25 || comb[i].isNaN()) continue
            if (comb[i] > second) second = comb[i]
        }
        val margin = comb[best] - (if (second.isInfinite()) 0.0 else second)
        var conf = if (margin < 0.05) comb[best] * 0.5 else comb[best]
        // اتفاق نطاقي الجمهور (المنخفض والعالي) على نفس اللحظة يرفع الثقة
        val bl = argmax(per[1]); val bh = argmax(per[2])
        if (bl >= 0 && bh >= 0 && abs(bl - best) <= 6 && abs(bh - best) <= 6) conf = min(0.99, conf + 0.12)
        // تحسين الدقة بين العينات (قمة قطع مكافئ)
        var lag = (best - MAX_LAG).toDouble()
        if (best > 0 && best < comb.size - 1 && !comb[best - 1].isNaN() && !comb[best + 1].isNaN()) {
            val y0 = comb[best - 1]; val y1 = comb[best]; val y2 = comb[best + 1]
            val den = y0 - 2 * y1 + y2
            if (den < -1e-9) lag += (0.5 * (y0 - y2) / den).coerceIn(-0.5, 0.5)
        }
        return SyncEstimate(lag * HOP, conf)
    }
}
