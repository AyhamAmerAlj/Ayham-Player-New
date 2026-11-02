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
    private var pool: SoundPool? = null
    private var idMove = 0
    private var idClick = 0
    private var ready = 0
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
            val fMove = File(dir, "move_v1.wav").also { if (!it.exists()) it.writeBytes(wav(tone(listOf(1318.5 to 1.0, 2637.0 to 0.18), 0.055, 0.30))) }
            val fClick = File(dir, "click_v1.wav").also {
                if (!it.exists()) it.writeBytes(wav(mix(tone(listOf(659.3 to 1.0, 1318.5 to 0.22), 0.16, 0.42), tone(listOf(987.8 to 1.0, 1975.6 to 0.2), 0.20, 0.38), 0.045)))
            }
            idMove = p.load(fMove.path, 1)
            idClick = p.load(fClick.path, 1)
            pool = p
        }
    }

    private fun gate(minGap: Long): Boolean {
        val n = android.os.SystemClock.uptimeMillis()
        if (n - last < minGap) return false
        last = n
        return true
    }

    fun move() { if (enabled && ready >= 2 && gate(55)) pool?.play(idMove, 0.5f, 0.5f, 1, 0, 1f) }
    fun click() { if (enabled && ready >= 2 && gate(90)) pool?.play(idClick, 0.7f, 0.7f, 1, 0, 1f) }

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
