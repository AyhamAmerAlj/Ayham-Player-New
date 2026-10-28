@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ayham.player.audio

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ayham.player.PlayerFactory
import com.ayham.player.data.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

/**
 * جلسة الصوت الخارجي (تعليق من قناة ثانية فوق فيديو القناة الأساسية).
 *
 * - تراقب اتصال القناة الصوتية وتعيده تلقائيًا (مع مهلة متزايدة وحد أعلى للمحاولات).
 * - الصوت الأصلي يُكتم فقط وقت ما يكون الصوت الخارجي شغّال فعلًا، ويرجع تلقائيًا إذا انقطع.
 * - مزامنة تلقائية: تقارن غلاف طاقة الصوتين وتحسب الفرق، وتصحّحه بإبطاء المتقدّم منهما قليلًا
 *   (بدون قفزات، ويصلح للبث TS اللي ما يقبل seek).
 */
class ExtAudioSession(
    private val ctx: Context,
    private val main: ExoPlayer,
    private val maxRes: Int,
    private val ar: Boolean,
    private val urlOf: (Item) -> String,
    private val maxConnections: Int,
) {
    companion object {
        /** مراقب صوت الفيديو الأساسي (المشغّل المشترك للقنوات). */
        val mainTap = EnvelopeTap()
        private const val TAG = "ExtAudio"
        private const val MAX_ATTEMPTS = 6
    }

    private enum class Phase { MEASURING, CORRECTING }

    var player by mutableStateOf<ExoPlayer?>(null); private set
    var name by mutableStateOf<String?>(null); private set
    var status by mutableStateOf(""); private set
    var delayMs by mutableLongStateOf(0L); private set
    var suspended by mutableStateOf(false); private set
    var autoSync by mutableStateOf(true)
    /** صورة مصغّرة لقناة التعليق (للمقارنة اليدوية). الإيقاف يوقف فك ترميز الفيديو فقط، والصوت يبقى. */
    var pip by mutableStateOf(false); private set
    /** حجم الصورة المصغّرة: 0 صغير، 1 متوسط، 2 كبير. */
    var pipSize by androidx.compose.runtime.mutableIntStateOf(1); private set
    /** مستوى الصوت الأصلي أثناء عمل الصوت الخارجي (الافتراضي 0 = مكتوم). */
    var mainVolume by mutableFloatStateOf(0f); private set
    var manualMs by mutableLongStateOf(0L); private set
    var volume by mutableFloatStateOf(1f)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val extTap = EnvelopeTap()
    private var current: Item? = null
    private var attempts = 0
    private var playing = true
    private var reconnecting = false
    private var released = false
    private var phase = Phase.MEASURING
    private var correctingUntil = 0L
    private var prevLag: Double? = null
    private var computing = false
    private var lastPos = -1L
    private var lastProgressAt = 0L
    private var bufferingSince = 0L
    private var healthySince = 0L
    private var unhealthySince = 0L
    private var mutedMain = false
    private var loops: Job? = null
    private var slowed: ExoPlayer? = null

    private fun s(a: String, e: String) = if (ar) a else e
    private fun now() = System.currentTimeMillis()

    private val mainListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            // إعادة تحميل أو تقطيع بالفيديو الأساسي: القياسات القديمة ما تصلح
            if (state == Player.STATE_BUFFERING || state == Player.STATE_IDLE) resetMeasure()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = resetMeasure()
    }

    private val extListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.w(TAG, "ext error: " + error.errorCodeName)
            scheduleReconnect(error.errorCodeName)
        }
    }

    // ------------------------------------------------------------------ تشغيل/إيقاف
    fun start(item: Item) {
        if (released) return
        suspended = false
        current = item
        name = item.name
        attempts = 0
        delayMs = 0
        manualMs = 0
        if (maxConnections in 1..1) {
            status = s(
                "تنبيه: اشتراكك يسمح باتصال واحد فقط. تشغيل قناتين معًا قد يقطعه السيرفر بعد قليل.",
                "Warning: your plan allows only 1 connection. Two streams at once may get cut.",
            )
        }
        build()
        runCatching { main.removeListener(mainListener) }
        main.addListener(mainListener)
        startLoops()
    }

    private fun build() {
        val item = current ?: return
        val url = urlOf(item)
        if (url.isEmpty()) return
        releasePlayer()
        extTap.clear()
        mainTap.clear()
        extTap.enabled = autoSync
        mainTap.enabled = autoSync
        val p = PlayerFactory.create(ctx, maxRes, extTap, pcmOnly = true)
        p.trackSelectionParameters = pipParams(p)
        p.volume = volume
        p.addListener(extListener)
        p.setMediaItem(MediaItem.fromUri(url))
        p.prepare()
        p.playWhenReady = playing
        player = p
        lastPos = -1L
        lastProgressAt = now()
        bufferingSince = 0L
        phase = Phase.MEASURING
        prevLag = null
        reconnecting = false
    }

    private fun pipParams(p: ExoPlayer) = p.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !pip)
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        .setMaxVideoSize(640, 360)
        .build()

    fun changePip(on: Boolean) {
        pip = on
        player?.let { it.trackSelectionParameters = pipParams(it) }
    }

    fun cyclePipSize(d: Int) { pipSize = (pipSize + d).coerceIn(0, 2) }

    fun changeMainVolume(v: Float) {
        mainVolume = v.coerceIn(0f, 1f)
        if (mutedMain) main.volume = mainVolume
    }

    private fun releasePlayer() {
        player?.let {
            runCatching { it.removeListener(extListener) }
            runCatching { it.release() }
        }
        player = null
        if (slowed != null && slowed !== main) slowed = null
    }

    fun setPlaying(p: Boolean) {
        playing = p
        player?.playWhenReady = p
    }

    fun pause() {
        player?.pause()
    }

    fun setVolumeLevel(v: Float) {
        volume = v.coerceIn(0f, 1f)
        player?.volume = volume
    }

    /** رجوع مؤقت للصوت الأصلي مع الاحتفاظ بالقناة المختارة. */
    fun useOriginal() {
        suspended = true
        stopLoops()
        resetSpeeds()
        releasePlayer()
        applyMainMute(false)
        status = s("تم الرجوع للصوت الأصلي", "Back to original audio")
    }

    fun resume() {
        val c = current ?: return
        if (suspended) start(c)
    }

    /** إيقاف المسار الخارجي نهائيًا (وننسى القناة). */
    fun stop() {
        stopLoops()
        resetSpeeds()
        releasePlayer()
        runCatching { main.removeListener(mainListener) }
        extTap.enabled = false
        mainTap.enabled = false
        mainTap.clear()
        applyMainMute(false)
        current = null
        name = null
        pip = false
        status = ""
        delayMs = 0
        suspended = false
    }

    fun release() {
        stop()
        released = true
        scope.cancel()
    }

    fun channel(): Item? = current

    // ------------------------------------------------------------------ تقديم/تأخير البث (DVR)
    private var offMoment = false

    /** الفيديو تحرّك داخل نافذة البث بمقدار delta: نحرّك الصوت الخارجي نفس المقدار، وإلا ما نخلّيه يشتغل بلحظة غلط. */
    fun followMain(delta: Long) {
        val p = player ?: return
        resetSpeeds()
        resetMeasure()
        val tl = p.currentTimeline
        if (p.isCurrentMediaItemSeekable && !tl.isEmpty) {
            val w = tl.getWindow(p.currentMediaItemIndex, androidx.media3.common.Timeline.Window())
            val maxPos = if (w.durationMs == C.TIME_UNSET) Long.MAX_VALUE else (w.durationMs - 1500L).coerceAtLeast(0L)
            p.seekTo((p.currentPosition + delta).coerceIn(0L, maxPos))
            offMoment = false
            status = s("تم نقل الصوت الخارجي مع الفيديو، جارٍ إعادة المزامنة…", "External audio moved with the video, re-syncing…")
        } else {
            offMoment = true
            p.volume = 0f
            applyMainMute(false)
            status = s(
                "القناة الصوتية لا تدعم التنقل الزمني، رجع الصوت الأصلي إلى أن ترجع للمباشر.",
                "The audio channel can't time-shift; original audio is on until you return to live.",
            )
        }
    }

    fun goLive() {
        val p = player
        resetSpeeds()
        resetMeasure()
        offMoment = false
        if (p != null) {
            p.volume = volume
            p.seekToDefaultPosition()
            status = s("رجعت للبث المباشر، جارٍ إعادة المزامنة…", "Back to live, re-syncing…")
        }
    }

    // ------------------------------------------------------------------ مزامنة
    fun setAuto(on: Boolean) {
        autoSync = on
        extTap.enabled = on && player != null
        mainTap.enabled = on && player != null
        resetSpeeds()
        resetMeasure()
        if (!on) status = s("المزامنة التلقائية متوقفة", "Auto sync is off")
    }

    fun resyncNow() {
        resetSpeeds()
        resetMeasure()
        status = s("جارٍ تحليل الصوت…", "Analyzing audio…")
    }

    private fun resetMeasure() {
        phase = Phase.MEASURING
        prevLag = null
        extTap.clear()
        mainTap.clear()
    }

    private fun resetSpeeds() {
        runCatching { main.setPlaybackSpeed(1f) }
        runCatching { player?.setPlaybackSpeed(1f) }
        slowed = null
        if (phase == Phase.CORRECTING) phase = Phase.MEASURING
    }

    /** إزاحة يدوية (لأدوات التشخيص): موجب = تأخير الصوت الخارجي. */
    fun nudge(ms: Long): Boolean {
        val p = player ?: return false
        if (!p.isCurrentMediaItemSeekable) return false
        p.seekTo((p.currentPosition - ms).coerceAtLeast(0L))
        manualMs += ms
        delayMs = manualMs
        // الضبط اليدوي يوقف التصحيح التلقائي حتى ما يتعارضان
        if (autoSync) setAuto(false)
        status = s("ضبط يدوي: ${if (manualMs >= 0) "+" else ""}${"%.1f".format(manualMs / 1000.0)} ث", "Manual: ${if (manualMs >= 0) "+" else ""}${"%.1f".format(manualMs / 1000.0)}s")
        return true
    }

    private fun startCorrection(lag: Double) {
        val target = if (lag > 0) main else player ?: return
        val rate = if (abs(lag) > 3.0) 0.90f else 0.95f
        val secs = min(abs(lag) / (1.0 - rate), 70.0)
        target.setPlaybackSpeed(rate)
        slowed = target
        phase = Phase.CORRECTING
        correctingUntil = now() + (secs * 1000).toLong()
        Log.i(TAG, "correcting lag=" + "%.2f".format(lag) + "s rate=" + rate + " for " + secs.toInt() + "s")
    }

    private fun syncTick() {
        val p = player ?: return
        if (!autoSync || reconnecting) return
        if (phase == Phase.CORRECTING) {
            val left = ((correctingUntil - now()) / 1000).toInt()
            if (left > 0) {
                status = s("جارٍ تصحيح المزامنة… (باقي $left ث)", "Correcting sync… (${left}s left)")
            } else {
                resetSpeeds()
                resetMeasure()
            }
            return
        }
        if (!mainTap.pcm || !extTap.pcm) {
            status = s(
                "تحليل الصوت غير متاح (الصوت الأصلي بصيغة تمرير مباشر). استخدم الضبط اليدوي.",
                "Audio analysis unavailable (original audio is passthrough). Use manual sync.",
            )
            return
        }
        if (!p.isPlaying) return
        val have = min(mainTap.spanSec(), extTap.spanSec())
        if (have < 28.0) {
            status = s("جارٍ تحليل الصوت… ${(have / 28.0 * 100).toInt().coerceAtMost(99)}%", "Analyzing audio… ${(have / 28.0 * 100).toInt().coerceAtMost(99)}%")
            return
        }
        if (computing) return
        computing = true
        scope.launch {
            val est = withContext(Dispatchers.Default) { runCatching { SyncMath.estimate(mainTap, extTap) }.getOrNull() }
            computing = false
            if (released || player == null) return@launch
            onEstimate(est)
        }
    }

    private fun onEstimate(est: SyncEstimate?) {
        if (est == null || est.confidence < 0.35) {
            prevLag = null
            status = s(
                "ثقة منخفضة: تعذّر مطابقة الصوتين (قد لا تنقل القناتان نفس الحدث). المزامنة لم تتغير.",
                "Low confidence: couldn't match the two audios (channels may show different events). Sync unchanged.",
            )
            return
        }
        val lag = est.lagSec
        val pl = prevLag
        prevLag = lag
        if (pl == null || abs(lag - pl) > 0.25) {
            status = s("جارٍ التأكد من القياس…", "Verifying measurement…")
            return
        }
        delayMs = (lag * 1000).toLong()
        val pct = (est.confidence * 100).toInt().coerceAtMost(99)
        when {
            abs(lag) <= 0.18 -> status = s("تمت المزامنة (فرق ${(abs(lag) * 1000).toInt()}ms، ثقة $pct%)", "Synced (diff ${(abs(lag) * 1000).toInt()}ms, confidence $pct%)")
            abs(lag) > 14.0 -> status = s("الفرق كبير جدًا (${"%.1f".format(abs(lag))}ث). جرّب قناة ثانية أو إعادة التشغيل.", "Difference too large (${"%.1f".format(abs(lag))}s). Try another channel.")
            else -> startCorrection(lag)
        }
    }

    // ------------------------------------------------------------------ مراقبة الاتصال
    private fun scheduleReconnect(reason: String) {
        if (reconnecting || released || suspended) return
        reconnecting = true
        attempts++
        if (attempts > MAX_ATTEMPTS) {
            Log.w(TAG, "giving up after $MAX_ATTEMPTS attempts ($reason)")
            stopLoops()
            resetSpeeds()
            releasePlayer()
            applyMainMute(false)
            suspended = true
            reconnecting = false
            status = s(
                "تعذّر استعادة الصوت الخارجي. رجعت للصوت الأصلي. اختر «استئناف» للمحاولة من جديد.",
                "Couldn't restore the external audio. Back on original audio. Choose Resume to retry.",
            )
            return
        }
        status = s("جارٍ استعادة الاتصال… (محاولة $attempts من $MAX_ATTEMPTS)", "Reconnecting… (attempt $attempts of $MAX_ATTEMPTS)")
        scope.launch {
            delay(min(1000L shl (attempts - 1), 15_000L))
            if (released || suspended || current == null) { reconnecting = false; return@launch }
            resetSpeeds()
            build()
        }
    }

    private fun applyMainMute(mute: Boolean) {
        if (mute == mutedMain) return
        mutedMain = mute
        main.volume = if (mute) mainVolume else 1f
    }

    private fun watchdog() {
        val p = player ?: return
        val t = now()
        if (reconnecting) return
        val st = p.playbackState
        val ok = st == Player.STATE_READY && p.isPlaying
        // الصوت الأصلي: مكتوم فقط وقت ما الخارجي سليم، ويرجع لو تعطّل أكثر من 3 ثوان
        if (ok) {
            unhealthySince = 0L
            if (healthySince == 0L) healthySince = t
            if (t - healthySince >= 1000 && !offMoment) applyMainMute(true)
            if (t - healthySince > 60_000) attempts = 0
        } else {
            healthySince = 0L
            if (unhealthySince == 0L) unhealthySince = t
            if (playing && t - unhealthySince >= 3000) applyMainMute(false)
        }
        when {
            st == Player.STATE_ENDED || (st == Player.STATE_IDLE && p.playerError == null) -> scheduleReconnect("idle/ended")
            st == Player.STATE_BUFFERING -> {
                if (bufferingSince == 0L) bufferingSince = t
                if (t - bufferingSince > 15_000) { bufferingSince = 0L; scheduleReconnect("buffering too long") }
            }
            else -> {
                bufferingSince = 0L
                if (p.isPlaying) {
                    val pos = p.currentPosition
                    if (pos != lastPos) { lastPos = pos; lastProgressAt = t }
                    else if (t - lastProgressAt > 6000) { lastProgressAt = t; scheduleReconnect("position stalled") }
                } else lastProgressAt = t
            }
        }
        if (status.isEmpty() && ok) status = s("القناة الصوتية تعمل", "Audio channel is playing")
    }

    private fun startLoops() {
        stopLoops()
        loops = scope.launch {
            var n = 0
            while (true) {
                delay(1000)
                watchdog()
                if (n++ % 4 == 3) syncTick()
            }
        }
    }

    private fun stopLoops() {
        loops?.cancel()
        loops = null
    }
}
