package com.ayham.player.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Ayham Smart Audio Control - Real Circular FIFO Delay Audio Processor
 * يقوم بتأخير الصوت بالملي ثانية بشكل حقيقي عبر طابور دائري في الذاكرة (Ring Buffer).
 * يمنع التقطيع والتشويه في بث IPTV المباشر.
 */
@OptIn(UnstableApi::class)
class DelayAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var delayMs: Long = 0L
        set(value) {
            val clamped = max(0L, min(value, 35_000L))
            if (field != clamped) {
                field = clamped
                updateTargetDelayBytes()
            }
        }

    private var bytesPerFrame: Int = 0
    private var targetDelayBytes: Int = 0

    // طابور دائري لحفظ العينات (حتى 35 ثانية كحد أقصى)
    private var ringBuffer = ByteArray(0)
    private var writePos = 0
    private var availableBytes = 0

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
            targetDelayBytes = 0
            return
        }
        val bytesPerSecond = inputAudioFormat.sampleRate.toLong() * bytesPerFrame
        val neededBytes = ((delayMs * bytesPerSecond) / 1000L).toInt()

        // مواءمة الحجم لحدود الإطار الصوتي (Frame alignment)
        targetDelayBytes = neededBytes - (neededBytes % bytesPerFrame)

        // تخصيص مساحة للـ Ring Buffer إذا لزم الأمر
        val maxCap = ((35_000L * bytesPerSecond) / 1000L).toInt()
        if (ringBuffer.size < maxCap) {
            val newBuf = ByteArray(maxCap)
            if (availableBytes > 0) {
                // نقل البيانات القديمة إن وجدت
                var r = (writePos - availableBytes + ringBuffer.size) % ringBuffer.size
                for (i in 0 until min(availableBytes, maxCap)) {
                    newBuf[i] = ringBuffer[r]
                    r = (r + 1) % ringBuffer.size
                }
                writePos = min(availableBytes, maxCap)
            } else {
                writePos = 0
            }
            ringBuffer = newBuf
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (targetDelayBytes <= 0) {
            // لا يوجد تأخير: تمرير البيانات مباشرة مع تفريغ أي مخزون قديم
            if (availableBytes > 0) {
                availableBytes = 0
                writePos = 0
            }
            val out = replaceOutputBuffer(remaining)
            out.put(inputBuffer)
            out.flip()
            return
        }

        // كتابة البيانات القادمة إلى الـ Ring Buffer
        val inBytes = ByteArray(remaining)
        inputBuffer.get(inBytes)

        val cap = ringBuffer.size
        if (cap == 0) return

        for (b in inBytes) {
            ringBuffer[writePos] = b
            writePos = (writePos + 1) % cap
            if (availableBytes < cap) {
                availableBytes++
            }
        }

        // نخرج الصوت فقط إذا تراكم لدينا ما يكفي من البيانات لتغطية التأخير المطلوب
        if (availableBytes > targetDelayBytes) {
            val toOutput = availableBytes - targetDelayBytes
            // مواءمة مع حدود الإطارات
            val alignedOutput = toOutput - (toOutput % bytesPerFrame)
            if (alignedOutput > 0) {
                val out = replaceOutputBuffer(alignedOutput)
                var readPos = (writePos - availableBytes + cap) % cap
                for (i in 0 until alignedOutput) {
                    out.put(ringBuffer[readPos])
                    readPos = (readPos + 1) % cap
                }
                availableBytes -= alignedOutput
                out.flip()
            }
        }
    }

    fun resetDelay() {
        delayMs = 0L
        targetDelayBytes = 0
        availableBytes = 0
        writePos = 0
    }

    override fun onReset() {
        ringBuffer = ByteArray(0)
        writePos = 0
        availableBytes = 0
        targetDelayBytes = 0
    }
}
