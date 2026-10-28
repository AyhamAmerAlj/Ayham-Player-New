@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ayham.player.audio

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * يسجّل "غلاف الطاقة" الصوتية (RMS كل 20 مللي ثانية) للصوت اللي يمرّ على المشغّل.
 * لا يخزّن الصوت نفسه، فقط رقم واحد لكل 20ms، ولا يعمل شيئًا إلا إذا فُعّل.
 * يُستعمل لمقارنة صوتي القناتين وحساب فرق التوقيت بينهما.
 */
class EnvelopeTap {
    companion object {
        const val HOP_S = 0.02
        const val CAP = 4000 // 80 ثانية
    }

    @Volatile var enabled = false
    @Volatile var pcm = true

    private val lock = Any()
    private val env = FloatArray(CAP)
    private var hops = 0L
    private var rate = 48000
    private var channels = 2
    private var framesPerHop = 960
    private var acc = 0.0
    private var accN = 0
    private val anchors = DoubleArray(96)
    private var anchorN = 0
    private var anchorPos = 0

    fun clear() = synchronized(lock) {
        hops = 0; acc = 0.0; accN = 0; anchorN = 0; anchorPos = 0
    }

    fun setFormat(sampleRate: Int, channelCount: Int, pcm16: Boolean) = synchronized(lock) {
        pcm = pcm16
        if (sampleRate > 0) rate = sampleRate
        if (channelCount > 0) channels = channelCount
        framesPerHop = (rate * HOP_S).roundToInt().coerceAtLeast(1)
        acc = 0.0; accN = 0
    }

    /** كم ثانية من الصوت مسجّلة (بحد أقصى 80). */
    fun spanSec(): Double = synchronized(lock) { minOf(hops, CAP.toLong()) * HOP_S }

    fun feed(src: ByteBuffer) {
        if (!enabled || !pcm) return
        synchronized(lock) {
            val b = src.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            val ch = channels
            val need = 2 * ch
            while (b.remaining() >= need) {
                var s = 0
                for (c in 0 until ch) s += b.getShort().toInt()
                val v = (s / ch) / 32768.0
                acc += v * v
                accN++
                if (accN >= framesPerHop) {
                    env[(hops % CAP).toInt()] = ln(1.0 + sqrt(acc / accN) * 60.0).toFloat()
                    hops++
                    acc = 0.0
                    accN = 0
                }
            }
            val now = System.nanoTime() / 1e9
            anchors[anchorPos] = now - hops * HOP_S
            anchorPos = (anchorPos + 1) % anchors.size
            if (anchorN < anchors.size) anchorN++
        }
    }

    private fun anchorLocked(): Double? {
        if (anchorN < 8) return null
        val a = anchors.copyOf(anchorN)
        a.sort()
        return a[anchorN / 2]
    }

    /** آخر لحظة (بالساعة الحقيقية) لها بيانات، وأقدم لحظة. */
    fun wallRange(): Pair<Double, Double>? = synchronized(lock) {
        val a = anchorLocked() ?: return null
        if (hops < 50) return null
        val first = maxOf(0L, hops - CAP)
        (first * HOP_S + a) to ((hops - 1) * HOP_S + a)
    }

    /** يملأ out بقيم الغلاف عند اللحظات t0 + i*HOP، وNaN حيث لا توجد بيانات. */
    fun fill(t0: Double, out: FloatArray) = synchronized(lock) {
        val a = anchorLocked()
        val first = maxOf(0L, hops - CAP)
        for (i in out.indices) {
            if (a == null) { out[i] = Float.NaN; continue }
            val k = ((t0 + i * HOP_S - a) / HOP_S).roundToInt().toLong()
            out[i] = if (k in first until hops) env[(k % CAP).toInt()] else Float.NaN
        }
    }
}

/** يغلّف مخرج الصوت ويمرّر نسخة من البيانات للـ tap بدون ما يغيّر التشغيل. */
class TapAudioSink(sink: AudioSink, private val tap: EnvelopeTap) : ForwardingAudioSink(sink) {
    override fun configure(inputFormat: Format, specifiedBufferSize: Int, outputChannels: IntArray?) {
        val pcm16 = inputFormat.sampleMimeType == MimeTypes.AUDIO_RAW && inputFormat.pcmEncoding == C.ENCODING_PCM_16BIT
        tap.setFormat(inputFormat.sampleRate, inputFormat.channelCount, pcm16)
        super.configure(inputFormat, specifiedBufferSize, outputChannels)
    }

    override fun handleBuffer(buffer: ByteBuffer, presentationTimeUs: Long, encodedAccessUnitCount: Int): Boolean {
        if (!tap.enabled) return super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        val p0 = buffer.position()
        val ok = super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        val p1 = buffer.position()
        if (p1 > p0) {
            val d = buffer.duplicate()
            d.position(p0)
            d.limit(p1)
            tap.feed(d)
        }
        return ok
    }
}
