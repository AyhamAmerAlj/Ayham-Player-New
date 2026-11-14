package com.ayham.player.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** نتيجة المقارنة: lagSec موجب = صوت القناة الخارجية متأخر عن صوت الفيديو الأصلي. */
class SyncEstimate(val lagSec: Double, val confidence: Double)

/**
 * مزامنة بصوت الجمهور (تعمل حتى لو لغتا التعليق مختلفتين).
 * الفكرة: كلام المعلّقين يختلف بين القناتين، لكن "أرضية" الصوت (هدير الجمهور) واحدة وتتغير بنفس اللحظات.
 * لذلك نأخذ من غلاف كل نطاق قيمة منخفضة (المئين 5) على نافذة نصف ثانية: هذه تتبع أصوات الجمهور
 * في فترات سكوت المعلق، ثم نقارن الأرضيتين بين القناتين.
 */
object SyncMath {
    private const val HOP = EnvelopeTap.HOP_S
    private const val MAX_LAG = 750        // ±15 ثانية
    private const val MIN_N = 1300         // 26 ثانية على الأقل
    private const val MAX_N = 2250         // 45 ثانية
    private const val HP = 150             // إزالة المستوى البطيء (3 ثوانٍ)
    private const val FW = 35              // نافذة الأرضية (0.7 ثانية)
    private const val DEC = 5              // بحث خشن كل 100ms ثم تدقيق كل 20ms

    private fun floorFeature(x: FloatArray): FloatArray {
        val n = x.size
        val out = FloatArray(n)
        val buf = FloatArray(FW)
        for (i in 0 until n) {
            var c = 0
            val a = max(0, i - FW / 2)
            val b = min(n, i + FW / 2 + 1)
            for (j in a until b) { val v = x[j]; if (!v.isNaN()) buf[c++] = v }
            if (c < FW / 3) { out[i] = Float.NaN; continue }
            java.util.Arrays.sort(buf, 0, c)
            out[i] = buf[(c * 0.05).toInt()]
        }
        val sm = FloatArray(n)
        for (i in 0 until n) {
            var s = 0f; var c = 0
            for (j in max(0, i - 1)..min(n - 1, i + 1)) { val v = out[j]; if (!v.isNaN()) { s += v; c++ } }
            sm[i] = if (c == 0) Float.NaN else s / c
        }
        return sm
    }

    /** يحذف المستوى البطيء ويُبقي التغيّرات (صيحات الجمهور، الصافرة). */
    private fun highPass(x: FloatArray, hp: Int): FloatArray {
        val n = x.size
        val pre = DoubleArray(n + 1)
        for (i in 0 until n) pre[i + 1] = pre[i] + (if (x[i].isNaN()) 0.0 else x[i].toDouble())
        val out = FloatArray(n)
        for (i in 0 until n) {
            if (x[i].isNaN()) { out[i] = Float.NaN; continue }
            val a = max(0, i - hp)
            val b = min(n, i + hp + 1)
            out[i] = (x[i] - (pre[b] - pre[a]) / (b - a)).toFloat()
        }
        return out
    }

    private fun decimate(x: FloatArray, n: Int): FloatArray {
        val m = n / DEC
        val out = FloatArray(m)
        for (i in 0 until m) {
            var s = 0f; var c = 0
            for (k in 0 until DEC) { val v = x[i * DEC + k]; if (!v.isNaN()) { s += v; c++ } }
            out[i] = if (c >= 3) s / c else Float.NaN
        }
        return out
    }

    /** ارتباط بيرسون عند إزاحة off (أي ext[i + off] مقابل main[i]). */
    private fun corrAt(m: FloatArray, e: FloatArray, n: Int, off: Int): Double {
        var sx = 0.0; var sy = 0.0; var sxx = 0.0; var syy = 0.0; var sxy = 0.0; var c = 0
        for (i in 0 until n) {
            val a = m[i]
            val b = e[i + off]
            if (a.isNaN() || b.isNaN()) continue
            sx += a; sy += b; sxx += a * a; syy += b * b; sxy += a * b; c++
        }
        if (c < n * 0.6) return Double.NaN
        val vx = sxx - sx * sx / c
        val vy = syy - sy * sy / c
        if (vx <= 1e-9 || vy <= 1e-9) return Double.NaN
        return (sxy - sx * sy / c) / sqrt(vx * vy)
    }

    private val WEIGHTS = doubleArrayOf(0.5, 1.0, 1.0)

    private fun combine(v: DoubleArray): Double {
        var sum = 0.0; var wt = 0.0
        for (b in 0..2) if (!v[b].isNaN()) { sum += v[b] * WEIGHTS[b]; wt += WEIGHTS[b] }
        return if (wt >= 1.5) sum / wt else Double.NaN
    }

    fun estimate(main: EnvelopeTap, ext: EnvelopeTap): SyncEstimate? {
        val rm = main.wallRange() ?: return null
        val re = ext.wallRange() ?: return null
        val t1 = min(rm.second, re.second)
        val t0min = max(rm.first, re.first)
        var n = min(MAX_N.toDouble(), (t1 - t0min) / HOP - 2).toInt()
        if (n < MIN_N) return null
        n = n / DEC * DEC
        val t0 = t1 - n * HOP
        val w = n + 2 * MAX_LAG

        val fm = Array(3) { FloatArray(0) }
        val fe = Array(3) { FloatArray(0) }
        for (b in 0..2) {
            val m = FloatArray(n); main.fill(t0, m, b)
            val e = FloatArray(w); ext.fill(t0 - MAX_LAG * HOP, e, b)
            fm[b] = highPass(floorFeature(m), HP)
            fe[b] = highPass(floorFeature(e), HP)
        }

        // بحث خشن
        val nc = n / DEC
        val cm = Array(3) { decimate(fm[it], n) }
        val ce = Array(3) { decimate(fe[it], w) }
        val coarseLags = 2 * MAX_LAG / DEC
        val comb = DoubleArray(coarseLags + 1) { Double.NaN }
        val per = Array(3) { DoubleArray(coarseLags + 1) { Double.NaN } }
        val tmp = DoubleArray(3)
        for (k in 0..coarseLags) {
            for (b in 0..2) { per[b][k] = corrAt(cm[b], ce[b], nc, k); tmp[b] = per[b][k] }
            comb[k] = combine(tmp)
        }
        fun argmax(r: DoubleArray): Int { var bi = -1; for (i in r.indices) if (!r[i].isNaN() && (bi < 0 || r[i] > r[bi])) bi = i; return bi }
        val best = argmax(comb)
        if (best < 0) return null
        var second = Double.NEGATIVE_INFINITY
        for (i in comb.indices) {
            if (abs(i - best) < 5 || comb[i].isNaN()) continue // أبعد من نصف ثانية عن القمة
            if (comb[i] > second) second = comb[i]
        }
        val margin = comb[best] - (if (second.isInfinite()) 0.0 else second)
        var conf = if (margin < 0.05) comb[best] * 0.5 else comb[best]
        val bl = argmax(per[1]); val bh = argmax(per[2])
        if (bl >= 0 && bh >= 0 && abs(bl - best) <= 1 && abs(bh - best) <= 1) conf = min(0.99, conf + 0.12)

        // تدقيق دقيق حول القمة الخشنة
        val center = best * DEC
        val lo = max(0, center - DEC - 2)
        val hi = min(2 * MAX_LAG, center + DEC + 2)
        val fine = DoubleArray(hi - lo + 1) { Double.NaN }
        for (off in lo..hi) {
            for (b in 0..2) tmp[b] = corrAt(fm[b], fe[b], n, off)
            fine[off - lo] = combine(tmp)
        }
        val fb = argmax(fine)
        if (fb < 0) return null
        var lag = (lo + fb - MAX_LAG).toDouble()
        if (fb > 0 && fb < fine.size - 1 && !fine[fb - 1].isNaN() && !fine[fb + 1].isNaN()) {
            val y0 = fine[fb - 1]; val y1 = fine[fb]; val y2 = fine[fb + 1]
            val den = y0 - 2 * y1 + y2
            if (den < -1e-9) lag += (0.5 * (y0 - y2) / den).coerceIn(-0.5, 0.5)
        }
        return SyncEstimate(lag * HOP, conf)
    }
}
