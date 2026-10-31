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
    fun create(ctx: Context, maxRes: Int, tap: EnvelopeTap? = null, pcmOnly: Boolean = false, delayProcessor: com.ayham.player.audio.DelayAudioProcessor? = null): ExoPlayer {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("AyhamPlayer/1.0")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        val lc = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 30_000, 1_500, 3_000)
            .setTargetBufferBytes(48 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(20_000, false)
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
        val p = ExoPlayer.Builder(ctx, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(ctx).setDataSourceFactory(DefaultDataSource.Factory(ctx, http)))
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
