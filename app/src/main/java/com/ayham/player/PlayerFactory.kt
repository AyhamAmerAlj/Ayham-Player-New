@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ayham.player

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.ayham.player.audio.EnvelopeTap
import com.ayham.player.audio.TapAudioSink

/** مصنع واحد للمشغّل: نفس الإعدادات الخفيفة للمشغّل الكبير والمصغّر. */
object PlayerFactory {
    fun create(ctx: Context, maxRes: Int, tap: EnvelopeTap? = null, pcmOnly: Boolean = false, delayProcessor: com.ayham.player.audio.DelayAudioProcessor? = null, lite: Boolean = false, live: Boolean = false): ExoPlayer {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("AyhamPlayer/1.0")
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(30_000)
        // قناة التعليق (lite): مخزن أصغر حتى لا تزاحم القناة الرئيسية على الشبكة والذاكرة وتسبب تقطيعًا
        // البث المباشر: مخزن أكبر (ثبات أهم من القرب من البث) + OkHttp (اتصال أمتن مع إعادة محاولة تلقائية)
        val liveClient = okhttp3.OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(4, 5, java.util.concurrent.TimeUnit.MINUTES))
            .build()
        val liveHttp = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(liveClient).setUserAgent("AyhamPlayer/1.0")
        val lc = (if (live && !lite) DefaultLoadControl.Builder()
            .setBufferDurationsMs(45_000, 90_000, 4_000, 15_000)
            .setTargetBufferBytes(96 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(10_000, false)
        else if (lite) DefaultLoadControl.Builder()
            .setBufferDurationsMs(12_000, 24_000, 2_500, 6_000)
            .setTargetBufferBytes(16 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(15_000, false)
        else DefaultLoadControl.Builder()
            .setBufferDurationsMs(40_000, 80_000, 2_500, 10_000)
            .setTargetBufferBytes(140 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(20_000, false))
            .build()
        val renderers = object : DefaultRenderersFactory(ctx) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink? {
                val processors = mutableListOf<androidx.media3.common.audio.AudioProcessor>()
                if (delayProcessor != null) {
                    processors.add(delayProcessor)
                }
                
                if (tap == null && delayProcessor == null) return super.buildAudioSink(context, enableFloatOutput, enableAudioTrackPlaybackParams)
                
                val b = DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(false)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    
                if (processors.isNotEmpty()) {
                    b.setAudioProcessors(processors.toTypedArray())
                }
                
                // قناة التعليق: نفك الترميز دائمًا (بدون تمرير مباشر) حتى نقدر نحلّل الصوت
                if (pcmOnly) b.setAudioCapabilities(AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES)
                
                val builtSink = b.build()
                return if (tap != null) TapAudioSink(builtSink, tap) else builtSink
            }
        }.setEnableDecoderFallback(true)
            // فك الترميز بالعتاد أولًا دائمًا (البرمجي بطيء جدًا مع 4K/HEVC) + طابور كودك غير متزامن يقلّل إسقاط الإطارات
            .setMediaCodecSelector { mime, secure, tunneling ->
                androidx.media3.exoplayer.mediacodec.MediaCodecSelector.DEFAULT.getDecoderInfos(mime, secure, tunneling)
                    .sortedBy { if (it.softwareOnly || !it.hardwareAccelerated) 1 else 0 }
            }
            .forceEnableMediaCodecAsynchronousQueueing()
        // مجرى TS الخام من سيرفرات IPTV: أعلام تحسّن اكتشاف الإطارات وتقلّل التقطيع/التشويه
        val extractors = androidx.media3.extractor.DefaultExtractorsFactory()
            .setTsExtractorFlags(androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS)
            .setConstantBitrateSeekingEnabled(false)
        val dsf = if (live) DefaultDataSource.Factory(ctx, liveHttp) else DefaultDataSource.Factory(ctx, http)
        val p = ExoPlayer.Builder(ctx, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dsf, extractors).setLoadErrorHandlingPolicy(androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(8)))
            .setLoadControl(lc)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
        if (maxRes == 1080) {
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setMaxVideoSize(1920, 1080)
                .build()
        }
        return p
    }
}
