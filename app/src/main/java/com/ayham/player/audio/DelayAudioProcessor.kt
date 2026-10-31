package com.ayham.player.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Ayham Smart Audio Control - Delay Processor
 * يتحكم بتأخير الصوت بالملي ثانية مباشرة عبر تخزين عينات الـ PCM مؤقتًا
 * يمنع انقطاع الصوت أو الحاجة للـ Seek في بث IPTV المباشر.
 */
@OptIn(UnstableApi::class)
class DelayAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var delayMs: Long = 0L
        set(value) {
            val clamped = max(0L, min(value, 30_000L))
            if (field != clamped) {
                field = clamped
                updateTargetDelayBytes()
            }
        }

    private var targetDelayBytes: Long = 0L
    private var bytesPerFrame: Int = 0
    private var delayBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var queuedDelayBytes: Long = 0L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        bytesPerFrame = inputAudioFormat.bytesPerFrame
        updateTargetDelayBytes()
        return inputAudioFormat
    }

    private fun updateTargetDelayBytes() {
        if (inputAudioFormat == AudioProcessor.AudioFormat.NOT_SET || bytesPerFrame == 0) {
            targetDelayBytes = 0L
            return
        }
        val bytesPerSecond = inputAudioFormat.sampleRate.toLong() * bytesPerFrame
        targetDelayBytes = (delayMs * bytesPerSecond) / 1000L
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (delayMs <= 0L) {
            // لا يوجد تأخير: تفريغ أي صمت قديم وتمرير الصوت مباشرة
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            queuedDelayBytes = 0L
            return
        }

        // إذا كان حجم التأخير المطلوب أكبر مما خزنّاه حتى الآن، نحجز العينات لإنشاء التأخير
        if (queuedDelayBytes < targetDelayBytes) {
            val needed = (targetDelayBytes - queuedDelayBytes).coerceAtMost(remaining.toLong()).toInt()
            queuedDelayBytes += needed
            // نقرأ من inputBuffer بدون كتابته إلى الـ Output لتأخير البداية
            val currentPos = inputBuffer.position()
            inputBuffer.position(currentPos + needed)

            val leftAfterDelay = inputBuffer.remaining()
            if (leftAfterDelay > 0) {
                val out = replaceOutputBuffer(leftAfterDelay)
                out.put(inputBuffer)
                out.flip()
            }
        } else {
            // وصلنا للتأخير المستهدف: نمرر الدفق بشكل طبيعي ومتزامن
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
        }
    }

    fun resetDelay() {
        delayMs = 0L
        queuedDelayBytes = 0L
    }

    override fun onReset() {
        delayBuffer = AudioProcessor.EMPTY_BUFFER
        queuedDelayBytes = 0L
        targetDelayBytes = 0L
    }
}
