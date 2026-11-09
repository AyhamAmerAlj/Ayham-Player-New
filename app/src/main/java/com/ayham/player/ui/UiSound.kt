package com.ayham.player.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * أصوات تنقل ناعمة (شبه ماريمبا/زجاج) تُولَّد برمجيًا عند أول تشغيل، بدون ملفات صوت جاهزة.
 * move: نقرة خفيفة جدًا عند تحريك التركيز، click: نغمتان صاعدتان عند الاختيار.
 */
object UiSound {
    @Volatile var enabled = true
    @Volatile var splashPlayed = false
    private var pool: SoundPool? = null
    private val idMove = IntArray(3)
    private val idClick = IntArray(3)
    @Volatile var pack = 0
    private var ready = 0
    private var idSplash = 0
    private const val TOTAL = 8
    private var idCinema = 0
    private var last = 0L

    fun init(ctx: Context) {
        if (pool != null) return
        runCatching {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            val p = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attrs).build()
            p.setOnLoadCompleteListener { _, _, st -> if (st == 0) ready++ }
            val dir = File(ctx.cacheDir, "uisnd").apply { mkdirs() }
            for (k in 0..2) {
                val (m, c) = packs(k)
                val fm = File(dir, "move_p${k}_v2.wav").also { if (!it.exists()) it.writeBytes(wav(m)) }
                val fc = File(dir, "click_p${k}_v2.wav").also { if (!it.exists()) it.writeBytes(wav(c)) }
                idMove[k] = p.load(fm.path, 1)
                idClick[k] = p.load(fc.path, 1)
            }
            val fs = File(dir, "splash_v1.wav").also { if (!it.exists()) it.writeBytes(wav(splashSound())) }
            idSplash = p.load(fs.path, 1)
            val fc2 = File(dir, "cinema_v1.wav").also { if (!it.exists()) it.writeBytes(wav(cinemaSound())) }
            idCinema = p.load(fc2.path, 1)
            pool = p
        }
    }

    private fun gate(minGap: Long): Boolean {
        val n = android.os.SystemClock.uptimeMillis()
        if (n - last < minGap) return false
        last = n
        return true
    }

    fun isReady() = ready >= TOTAL
    /** نغمة الافتتاحية: حفيف حرير ناعم ثم رنين هادئ عند ظهور الشعار. */
    fun splash() { if (enabled && ready >= TOTAL) pool?.play(idSplash, 0.6f, 0.6f, 1, 0, 1f) }

    /** افتتاحية السينما: نبضة عميقة تتصاعد مع حفيف ستارة، ثم وتر دافئ. */
    fun cinema() { if (enabled && ready >= TOTAL) pool?.play(idCinema, 0.7f, 0.7f, 1, 0, 1f) }

    private fun cinemaSound(): ShortArray {
        val n = (SR * 2.2).toInt()
        val out = DoubleArray(n)
        val rnd = java.util.Random(23L)
        var lp = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            // نبضة عميقة تتصاعد ثم تهدأ
            val swell = sin(PI * (t / 1.6).coerceAtMost(1.0)).let { it * it }
            val low = (sin(2 * PI * 55.0 * t) + 0.6 * sin(2 * PI * 82.5 * t)) * swell * 0.38
            // حفيف قماش: ضجيج مصفّى يفتح خلال أول 1.3 ث
            val x = (t / 1.3).coerceIn(0.0, 1.0)
            lp += (0.03 + 0.2 * sin(PI * x)) * ((rnd.nextDouble() * 2 - 1) - lp)
            val whoosh = lp * sin(PI * x) * 0.5
            out[i] = low + whoosh
        }
        // وتر دافئ عند 1.1 ث
        val st = (SR * 1.1).toInt()
        for (i in st until n) {
            val t = (i - st).toDouble() / SR
            val env = (t / 0.03).coerceAtMost(1.0) * exp(-t * 2.6)
            val v = sin(2 * PI * 261.6 * t) + 0.8 * sin(2 * PI * 329.6 * t) + 0.7 * sin(2 * PI * 392.0 * t) + 0.3 * sin(2 * PI * 523.25 * t)
            out[i] += v / 2.8 * env * 0.4
        }
        return ShortArray(n) { (out[it].coerceIn(-1.0, 1.0) * 32767).toInt().toShort() }
    }

    private fun splashSound(): ShortArray {
        val n = (SR * 2.4).toInt()
        val out = DoubleArray(n)
        val rnd = java.util.Random(11L)
        var lp = 0.0
        // حفيف: ضجيج مصفّى يفتح ثم يغلق (0 - 0.9 ث)
        for (i in 0 until (SR * 0.9).toInt()) {
            val t = i.toDouble() / SR
            val x = t / 0.9
            val env = sin(PI * x).let { it * it }
            val k = 0.04 + 0.22 * sin(PI * x)
            lp += k * ((rnd.nextDouble() * 2 - 1) - lp)
            out[i] += lp * env * 0.55
        }
        // رنين ناعم (ثلاث نغمات متناسقة) عند 1.05 ث
        val start = (SR * 1.05).toInt()
        for (i in start until n) {
            val t = (i - start).toDouble() / SR
            val env = (t / 0.015).coerceAtMost(1.0) * exp(-t * 3.2)
            val v = sin(2 * PI * 523.25 * t) + 0.7 * sin(2 * PI * 784.0 * t) + 0.45 * sin(2 * PI * 1046.5 * t) + 0.12 * sin(2 * PI * 2093.0 * t) * exp(-t * 6)
            out[i] += v / 2.3 * env * 0.42
        }
        return ShortArray(n) { (out[it].coerceIn(-1.0, 1.0) * 32767).toInt().toShort() }
    }

    fun move() { if (enabled && ready >= TOTAL && gate(55)) pool?.play(idMove[pack.coerceIn(0, 2)], 0.5f, 0.5f, 1, 0, 1f) }
    fun click() { if (enabled && ready >= TOTAL && gate(90)) pool?.play(idClick[pack.coerceIn(0, 2)], 0.7f, 0.7f, 1, 0, 1f) }

    /** الحزم: 0 الافتراضي (ماريمبا/زجاج)، 1 غَيم (زجاج + قطرة ماء)، 2 مطر (قطرات خفيفة). */
    private fun packs(k: Int): Pair<ShortArray, ShortArray> = when (k) {
        1 -> bloop(1050.0, 1500.0, 0.085, 0.30) to mix(bloop(880.0, 1320.0, 0.14, 0.40), bloop(1250.0, 1750.0, 0.17, 0.36), 0.07)
        2 -> drop(0.035, 2600.0, 0.26) to mix(drop(0.09, 2100.0, 0.42), drop(0.11, 2900.0, 0.36), 0.075)
        else -> tone(listOf(1318.5 to 1.0, 2637.0 to 0.18), 0.055, 0.30) to
            mix(tone(listOf(659.3 to 1.0, 1318.5 to 0.22), 0.16, 0.42), tone(listOf(987.8 to 1.0, 1975.6 to 0.2), 0.20, 0.38), 0.045)
    }

    /** قطرة ماء زجاجية: نغمة يرتفع ترددها بسرعة + رنين زجاج خفيف (جزئي غير توافقي). */
    private fun bloop(f0: Double, f1: Double, sec: Double, amp: Double): ShortArray {
        val n = (SR * sec).toInt()
        val out = ShortArray(n)
        var ph = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val f = f1 + (f0 - f1) * exp(-t * 38.0)
            ph += 2 * PI * f / SR
            val env = (t / 0.003).coerceAtMost(1.0) * exp(-t * (6.0 / sec))
            val v = sin(ph) + 0.22 * sin(ph * 2.76) * exp(-t * 30.0) + 0.1 * sin(ph * 5.4) * exp(-t * 55.0)
            out[i] = (v / 1.32 * env * amp * 32767).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    /** قطرة مطر ناعمة: نقرة ضجيج مصفّى سريعة الخفوت + نغمة عالية خافتة. */
    private fun drop(sec: Double, f: Double, amp: Double): ShortArray {
        val n = (SR * sec).toInt()
        val out = ShortArray(n)
        val rnd = java.util.Random(7L + (f * 10).toLong())
        var lp = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val env = (t / 0.0015).coerceAtMost(1.0) * exp(-t * (7.0 / sec))
            lp += 0.35 * ((rnd.nextDouble() * 2 - 1) - lp) // مرشح تمرير منخفض بسيط يليّن الضجيج
            val v = 0.55 * lp * 2.2 + 0.45 * sin(2 * PI * f * t) * exp(-t * 60.0)
            out[i] = (v * env * amp * 32767).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    // ---- توليد الصوت ----
    private const val SR = 44100

    private fun tone(parts: List<Pair<Double, Double>>, sec: Double, amp: Double): ShortArray {
        val n = (SR * sec).toInt()
        val out = ShortArray(n)
        val norm = parts.sumOf { it.second }
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val atk = (t / 0.004).coerceAtMost(1.0)
            val env = atk * exp(-t * (5.5 / sec))
            var v = 0.0
            for ((f, a) in parts) v += a * sin(2 * PI * f * t)
            out[i] = (v / norm * env * amp * 32767).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    /** يدمج نغمة ثانية بعد تأخير (ثوانٍ). */
    private fun mix(a: ShortArray, b: ShortArray, delaySec: Double): ShortArray {
        val off = (SR * delaySec).toInt()
        val out = ShortArray(maxOf(a.size, b.size + off))
        for (i in a.indices) out[i] = a[i]
        for (i in b.indices) out[i + off] = (out[i + off] + b[i]).coerceIn(-32767, 32767).toShort()
        return out
    }

    private fun wav(pcm: ShortArray): ByteArray {
        val bo = ByteArrayOutputStream()
        fun i32(v: Int) { bo.write(v and 255); bo.write(v shr 8 and 255); bo.write(v shr 16 and 255); bo.write(v shr 24 and 255) }
        fun i16(v: Int) { bo.write(v and 255); bo.write(v shr 8 and 255) }
        bo.write("RIFF".toByteArray()); i32(36 + pcm.size * 2); bo.write("WAVEfmt ".toByteArray())
        i32(16); i16(1); i16(1); i32(SR); i32(SR * 2); i16(2); i16(16)
        bo.write("data".toByteArray()); i32(pcm.size * 2)
        for (s in pcm) i16(s.toInt())
        return bo.toByteArray()
    }
}
