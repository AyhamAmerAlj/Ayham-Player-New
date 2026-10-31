@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ayham.player.ui

import android.net.Uri
import android.view.KeyEvent as AKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C as MC
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.ayham.player.AppViewModel
import com.ayham.player.PlayerFactory
import com.ayham.player.data.Item
import com.ayham.player.data.PlayRequest
import kotlinx.coroutines.delay
import java.util.Locale

private enum class Panel { MENU, SUBS, SUBURL, SUBPHONE, TRYSUB, AUDIO, EXT, SYNC, EPG }

private fun hm(sec: Long): String = java.text.SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date(sec * 1000))

private fun fmt(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
    else String.format(Locale.US, "%d:%02d", m, sec)
}

private fun subMime(url: String): String {
    val clean = url.substringBefore('?').lowercase(Locale.US)
    return when {
        clean.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        clean.endsWith(".ass") || clean.endsWith(".ssa") -> MimeTypes.TEXT_SSA
        clean.endsWith(".ttml") || clean.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

@Composable
fun PlayerScreen(vm: AppViewModel, req: PlayRequest, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val queue = req.queue
    val exit by rememberUpdatedState(onExit)

    // نصوص نحتاجها داخل callbacks
    val errText = t("error_play")
    val notSeekable = t("not_seekable")
    val aspectLabels = listOf(t("fit"), t("fill"), t("zoom"))
    val toastAspect = t("toast_aspect")
    val subApplied = t("sub_applied")
    val subBad = t("sub_bad")
    val audioBad = t("audio_bad")
    val favAddedT = t("fav_added")
    val favRemovedT = t("fav_removed")
    val subClearedT = t("sub_cleared")
    val audioUnsupported = t("audio_unsupported")
    val liveEdgeT = t("live_edge_limit")
    val skipIntroT = t("skip_intro")
    val introSavedT = t("intro_saved")

    var index by remember { mutableIntStateOf(req.index.coerceIn(0, queue.lastIndex)) }
    var isPlaying by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(true) }
    var tracks by remember { mutableStateOf(Tracks.EMPTY) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var controls by remember { mutableStateOf(true) }
    val lastTouch = remember { longArrayOf(0L) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var toastId by remember { mutableIntStateOf(0) }
    var aspect by remember { mutableIntStateOf(0) }
    var speed by remember { mutableFloatStateOf(1f) }
    // ---- تخطي المقدمة (مثل نتفلكس) ----
    val isSeries = req.item?.kind == com.ayham.player.data.Kind.SERIES
    var introPos by remember { mutableLongStateOf(0L) }
    var introTick by remember { mutableIntStateOf(0) }
    var subUrl by remember { mutableStateOf("") }
    var subsLoaded by remember { mutableStateOf(false) }
    var barVisible by remember { mutableStateOf(false) }
    var epgList by remember { mutableStateOf<List<com.ayham.player.data.EpgEntry>>(emptyList()) }
    var epgFull by remember { mutableStateOf<List<com.ayham.player.data.EpgEntry>>(emptyList()) }
    val okSwallow = remember { booleanArrayOf(false) }
    val silentTries = remember { intArrayOf(0) }
    val barFocus = remember { FocusRequester() }
    var pv by remember { mutableStateOf<PlayerView?>(null) }
    var audioOverridden by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }

    // الصوت الخارجي
    var syncAudio by remember { mutableStateOf(true) }

    val liveSession = remember { req.queue.firstOrNull()?.isLive == true }
    // القنوات تستعمل مشغّل مشترك (حتى تبقى بالشاشة المصغّرة)، والأفلام لها مشغّل خاص
    val exo = remember { if (liveSession) vm.ensureLivePlayer() else PlayerFactory.create(ctx, vm.maxRes) }
    val extSession = remember {
        com.ayham.player.audio.ExtAudioSession(
            ctx, exo, vm.maxRes, vm.lang == "ar", { vm.urlForLive(it) }, vm.maxConnectionsInt(),
        ).also { it.autoSync = vm.extAuto }
    }
    val extName = extSession.name
    val extVolume = extSession.volume
    val offsetMs = extSession.delayMs
    val rootFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val tv = remember { isTvDevice(ctx) }
    val compact = isCompact()
    val activity = remember(ctx) { ctx as? android.app.Activity }
    val audioMgr = remember { ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    var volF by remember { mutableFloatStateOf(0f) }
    var dragLeft by remember { mutableStateOf(false) }

    // الهاتف: شاشة كاملة أفقية أثناء التشغيل، ونرجع للوضع الطبيعي عند الخروج
    DisposableEffect(Unit) {
        val win = activity?.window
        val ctl = win?.let { androidx.core.view.WindowCompat.getInsetsController(it, it.decorView) }
        if (!tv) {
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ctl?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctl?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (!tv) {
                activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                ctl?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                win?.let { w -> val lp = w.attributes; lp.screenBrightness = -1f; w.attributes = lp }
            }
        }
    }

    var pendingSub by remember { mutableStateOf<java.io.File?>(null) }
    var pendingName by remember { mutableStateOf("") }
    var pendingBad by remember { mutableStateOf(false) }
    // ملف ترجمة من ذاكرة الهاتف
    val pickSub = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val name = runCatching {
                ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && i >= 0) c.getString(i) else null
                }
            }.getOrNull() ?: "sub.srt"
            val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            val f = bytes?.let { com.ayham.player.data.SubTools.fromUpload(java.io.File(ctx.filesDir, "subs/" + queue[index].key.hashCode().toUInt()), name, it, clean = false) }
            if (f != null) { pendingName = name; pendingSub = f } else pendingBad = true
        }
    }

    fun touch() { lastTouch[0] = android.os.SystemClock.uptimeMillis() }

    fun showToast(msg: String) {
        toast = msg
        toastId++
    }

    fun loadMedia(startMs: Long, withSubs: Boolean = true) {
        val key = queue[index].key
        val b = MediaItem.Builder().setUri(queue[index].url)
        val saved = if (withSubs && !queue[index].isLive) vm.savedSubs(key) else emptyList()
        subsLoaded = saved.isNotEmpty()
        if (saved.isNotEmpty()) {
            val pref = vm.prefSub(key)
            b.setSubtitleConfigurations(
                saved.map { sv ->
                    MediaItem.SubtitleConfiguration.Builder(Uri.parse(sv.uri))
                        .setMimeType(subMime(sv.uri))
                        .setLabel(sv.label)
                        .setSelectionFlags(if (sv.uri == pref) MC.SELECTION_FLAG_DEFAULT else 0)
                        .build()
                },
            )
            if (pref != null) {
                exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(MC.TRACK_TYPE_TEXT, false).build()
            }
        }
        exo.setMediaItem(b.build(), if (startMs > 0) startMs else MC.TIME_UNSET)
        exo.prepare()
        exo.playWhenReady = true
    }

    fun retry() {
        errorMsg = null
        exo.seekToDefaultPosition()
        exo.prepare()
        exo.playWhenReady = true
    }

    fun speedLabel(v: Float) = (if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()) + "x"
    fun cycleSpeed() {
        val all = listOf(1f, 1.25f, 1.5f, 2f, 0.5f, 0.75f)
        speed = all[(all.indexOf(speed) + 1) % all.size]
        exo.setPlaybackSpeed(speed)
        showToast(speedLabel(speed))
    }
    /** تخطي المقدمة: تقديم 85 ثانية (المزوّد ما يعطي توقيت المقدمة، فالتخطي يدوي). */
    fun skipIntro() {
        val dur = if (exo.duration == MC.TIME_UNSET) Long.MAX_VALUE else exo.duration
        exo.seekTo((exo.currentPosition + 85_000L).coerceIn(0L, dur))
        showToast("+85")
    }

    /** لو الزر يظهر الآن يرجع موضع الانتقال، وإلا null. المقدمة المضبوطة للمسلسل تتبع كل حلقاته. */
    fun introTarget(): Long? {
        if (!isSeries || queue[index].isLive || introTick < 0) return null
        val p = introPos
        val prof = req.item?.key?.let { vm.store.introProfile(it) }
        val st = prof?.first ?: 0L
        val en = prof?.second ?: 0L
        return if (en > st && en > 0L) {
            if (p >= st && p < en - 1500L) en else null
        } else {
            if (p >= st && p < st + 180_000L) p + 85_000L else null
        }
    }
    fun doSkip() {
        val tgt = introTarget()
        if (tgt != null) {
            val dur = if (exo.duration == MC.TIME_UNSET) Long.MAX_VALUE else exo.duration
            exo.seekTo(tgt.coerceIn(0L, dur))
            showToast(skipIntroT)
        } else skipIntro()
    }

    fun seekBy(ms: Long) {
        val live = queue[index].isLive
        if (live && !exo.isCurrentMediaItemSeekable) {
            showToast(notSeekable)
            return
        }
        if (live) {
            // تقديم/تأخير داخل نافذة البث المباشر فقط، والصوت الخارجي يتبع نفس الحركة
            val tl = exo.currentTimeline
            if (tl.isEmpty) return
            val w = tl.getWindow(exo.currentMediaItemIndex, androidx.media3.common.Timeline.Window())
            if (w.durationMs == MC.TIME_UNSET) { showToast(notSeekable); return }
            val pos = exo.currentPosition
            val target = (pos + ms).coerceIn(0L, (w.durationMs - 2000L).coerceAtLeast(0L))
            val delta = target - pos
            if (kotlin.math.abs(delta) < 500L) { showToast(liveEdgeT); return }
            exo.seekTo(target)
            extSession.followMain(delta)
            showToast(if (delta > 0) "+${delta / 1000}" else "${delta / 1000}")
            return
        }
        val dur = if (exo.duration == MC.TIME_UNSET) Long.MAX_VALUE else exo.duration
        val target = (exo.currentPosition + ms).coerceIn(0L, dur)
        exo.seekTo(target)
        showToast(if (ms > 0) "+${ms / 1000}" else "${ms / 1000}")
    }

    fun go(delta: Int) {
        val n = index + delta
        if (n in queue.indices) {
            index = n
            if (queue[n].isLive) showToast(queue[n].title)
        }
    }


    fun togglePlay() {
        if (exo.isPlaying || exo.playWhenReady) exo.pause() else exo.play()
    }

    fun startExt(item: Item) {
        extSession.start(item)
        vm.noteExt(item)
    }

    fun stopExt() {
        extSession.stop()
    }

    /** ضبط يدوي (تشخيص): delayMs موجب = تأخير الصوت الخارجي، سالب = تقديمه. */
    fun shift(delayMs: Long) {
        if (extSession.player == null) return
        if (!extSession.nudge(if (syncAudio) delayMs else -delayMs)) showToast(notSeekable)
    }

    fun closePanel() {
        panel = null
        controls = true
        touch()
    }

    // تطبيق الصوتيات المحفوظة للقناة تلقائيًا (بدون ما تفتح اللوحة كل مرة)
    LaunchedEffect(index) {
        if (!queue[index].isLive) return@LaunchedEffect
        val pref = vm.store.audioPref(queue[index].key) ?: return@LaunchedEffect
        delay(3500)
        runCatching {
            if (extSession.player != null) return@runCatching
            val d = pref[2].toLongOrNull() ?: 0L
            if (pref[0] == "s") {
                extSession.start(com.ayham.player.data.Item(com.ayham.player.data.Kind.LIVE, queue[index].key.substringAfter(':'), queue[index].title), true, pref[1].ifBlank { null })
                if (d != 0L) extSession.requestAdjust(-d)
            } else if (pref[3].isNotBlank()) {
                extSession.start(com.ayham.player.data.Item(com.ayham.player.data.Kind.LIVE, pref[3], pref[4]))
                var w = 0
                while (w < 60 && extSession.player?.playbackState != androidx.media3.common.Player.STATE_READY) { delay(250); w++ }
                if (d != 0L) extSession.nudge(d)
            }
        }
    }

    fun addSub(label: String, uri: String) {
        vm.addSub(queue[index].key, label, uri)
        loadMedia(exo.currentPosition)
        showToast(subApplied)
        closePanel()
    }

    fun applySub(f: java.io.File, name: String) = addSub(name.substringBeforeLast('.').take(40).ifBlank { "Subtitle" }, Uri.fromFile(f).toString())

    fun subDir() = java.io.File(ctx.filesDir, "subs/" + queue[index].key.hashCode().toUInt())

    LaunchedEffect(pendingSub, pendingBad) {
        pendingSub?.let { applySub(it, pendingName); pendingSub = null }
        if (pendingBad) { showToast(subBad); pendingBad = false }
    }

    fun handleKey(code: Int, repeat: Int): Boolean {
        when (code) {
            AKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { togglePlay(); return true }
            AKeyEvent.KEYCODE_MEDIA_PLAY -> { exo.play(); return true }
            AKeyEvent.KEYCODE_MEDIA_PAUSE -> { exo.pause(); return true }
            AKeyEvent.KEYCODE_MEDIA_REWIND -> { seekBy(-10_000); return true }
            AKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { seekBy(10_000); return true }
            AKeyEvent.KEYCODE_MEDIA_NEXT, AKeyEvent.KEYCODE_CHANNEL_UP -> { go(1); return true }
            AKeyEvent.KEYCODE_MEDIA_PREVIOUS, AKeyEvent.KEYCODE_CHANNEL_DOWN -> { go(-1); return true }
            AKeyEvent.KEYCODE_MENU -> { panel = Panel.MENU; controls = false; return true }
        }
        if (errorMsg != null && (code == AKeyEvent.KEYCODE_DPAD_CENTER || code == AKeyEvent.KEYCODE_ENTER)) {
            retry()
            return true
        }
        if (panel != null || controls) return false
        val live = queue[index].isLive
        if (!live && introTarget() != null &&
            (code == AKeyEvent.KEYCODE_DPAD_CENTER || code == AKeyEvent.KEYCODE_ENTER || code == AKeyEvent.KEYCODE_NUMPAD_ENTER)) {
            okSwallow[0] = true
            doSkip()
            return true
        }
        val ok = code == AKeyEvent.KEYCODE_DPAD_CENTER || code == AKeyEvent.KEYCODE_ENTER || code == AKeyEvent.KEYCODE_NUMPAD_ENTER
        if (live) {
            // القنوات: يمين/يسار = تبديل القناة، OK = إظهار أزرار التحكم (إيقاف، تقديم، تأخير)
            return when {
                code == AKeyEvent.KEYCODE_DPAD_LEFT -> { go(-1); true }
                code == AKeyEvent.KEYCODE_DPAD_RIGHT -> { go(1); true }
                code == AKeyEvent.KEYCODE_DPAD_UP || code == AKeyEvent.KEYCODE_DPAD_DOWN -> { controls = true; true }
                ok -> { okSwallow[0] = true; controls = true; true }
                else -> false
            }
        }
        // الأفلام: يمين/يسار = تقديم/رجوع (يتسارع مع الضغط المستمر)
        val step = when {
            repeat >= 15 -> 60_000L
            repeat >= 5 -> 30_000L
            else -> 10_000L
        }
        return when {
            code == AKeyEvent.KEYCODE_DPAD_LEFT -> { seekBy(-step); true }
            code == AKeyEvent.KEYCODE_DPAD_RIGHT -> { seekBy(step); true }
            code == AKeyEvent.KEYCODE_DPAD_UP || code == AKeyEvent.KEYCODE_DPAD_DOWN || ok -> {
                if (ok) okSwallow[0] = true
                controls = true
                true
            }
            else -> false
        }
    }

    // ---------- مستمع المشغل ----------
    DisposableEffect(exo) {
        isPlaying = exo.isPlaying
        buffering = exo.playbackState == Player.STATE_BUFFERING
        tracks = exo.currentTracks
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                extSession.setPlaying(playing)
            }

            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    silentTries[0] = 0
                    errorMsg = null
                    retryCount = 0
                }
                if (state == Player.STATE_ENDED && !queue[index].isLive) {
                    val d = exo.duration.coerceAtLeast(0L)
                    vm.saveProgress(req.item, queue[index].key, d, d)
                    if (index + 1 < queue.size) index++ else exit()
                }
            }

            override fun onTracksChanged(t: Tracks) {
                tracks = t
            }

            override fun onPlayerError(error: PlaybackException) {
                val pos = exo.currentPosition
                if (subsLoaded) {
                    // ملف ترجمة فشل: نكمل الفيلم بدون الترجمات المضافة بدل ما يتوقف
                    subsLoaded = false
                    loadMedia(pos, withSubs = false)
                    showToast(subBad)
                    return
                }
                if (audioOverridden) {
                    // مسار الصوت غير مدعوم: نرجع للمسار الافتراضي ونكمل
                    audioOverridden = false
                    exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                        .clearOverridesOfType(MC.TRACK_TYPE_AUDIO)
                        .setTrackTypeDisabled(MC.TRACK_TYPE_AUDIO, false)
                        .build()
                    exo.prepare()
                    exo.playWhenReady = true
                    showToast(audioBad)
                    return
                }
                // أخطاء الصوت/الفك: نرجع للمسار الافتراضي بصمت ونكمل بدون ما تظهر رسالة
                val audioish = error.errorCode in listOf(
                    PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
                    PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                )
                if (silentTries[0] < 3) {
                    silentTries[0]++
                    if (audioish) {
                        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                            .clearOverridesOfType(MC.TRACK_TYPE_AUDIO)
                            .setTrackTypeDisabled(MC.TRACK_TYPE_AUDIO, false)
                            .build()
                    }
                    exo.prepare()
                    exo.playWhenReady = true
                    return
                }
                errorMsg = "$errText (${error.errorCodeName})"
            }
        }
        exo.addListener(l)
        onDispose { exo.removeListener(l) }
    }

    // ---------- تحميل المقطع عند تغيير القناة/الحلقة ----------
    LaunchedEffect(index) {
        errorMsg = null
        retryCount = 0
        val e = queue[index]
        var start = 0L
        if (!e.isLive) {
            val p = vm.progressOf(e.key)
            if (p != null && p.first > 10_000 && p.first < p.second * 0.92) start = p.first
        }
        val cur = exo.currentMediaItem?.localConfiguration?.uri?.toString()
        val st = exo.playbackState
        if (e.isLive && cur == e.url && st != Player.STATE_IDLE && st != Player.STATE_ENDED) {
            exo.playWhenReady = true // نفس القناة شغّالة بالشاشة المصغّرة، نكمل بدون إعادة تحميل
        } else {
            loadMedia(start)
        }
        if (e.isLive) vm.noteLive(e.key)
    }

    // دليل البرامج للقناة الحالية
    LaunchedEffect(index) {
        epgList = emptyList()
        val e = queue[index]
        if (e.isLive) epgList = vm.epg(e.key.removePrefix("LIVE:")).take(3)
    }

    // إعادة محاولة تلقائية للقنوات المباشرة
    LaunchedEffect(errorMsg) {
        if (errorMsg != null && queue[index].isLive && retryCount < 6) {
            delay(3000)
            retryCount++
            retry()
        }
    }

    // موضع التشغيل لزر تخطي المقدمة
    LaunchedEffect(exo, index, isSeries) {
        if (!isSeries) return@LaunchedEffect
        while (true) {
            introPos = exo.currentPosition
            delay(500)
        }
    }

    // ---------- موضع التشغيل وحفظ التقدم ----------
    LaunchedEffect(exo) {
        while (true) {
            delay(10_000)
            val d = exo.duration
            if (d > 0 && d != MC.TIME_UNSET && !queue[index].isLive) {
                vm.saveProgress(req.item, queue[index].key, exo.currentPosition, d)
            }
        }
    }

    // إخفاء الأزرار تلقائيًا
    LaunchedEffect(controls, panel, isPlaying) {
        if (controls && panel == null && isPlaying) {
            touch()
            while (true) {
                delay(500)
                if (android.os.SystemClock.uptimeMillis() - lastTouch[0] > 5000) {
                    controls = false
                    break
                }
            }
        }
    }

    // نقل التركيز
    LaunchedEffect(controls, panel, barVisible) {
        delay(70)
        if (panel == null) {
            runCatching {
                if (!controls) rootFocus.requestFocus()
                else if (barVisible && !queue[index].isLive) barFocus.requestFocus()
                else playFocus.requestFocus()
            }
        }
    }

    LaunchedEffect(toastId) {
        if (toast != null) {
            delay(1300)
            toast = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val e = queue[index]
            if (!e.isLive && exo.duration > 0 && exo.currentPosition > 0) {
                vm.saveProgress(req.item, e.key, exo.currentPosition, exo.duration)
            }
            extSession.release()
            if (liveSession) {
                exo.volume = 1f
                vm.onLiveClosed(e.key)
            } else {
                exo.release()
            }
        }
    }

    // نوقف التشغيل لو خرجنا من التطبيق (حتى ما يظل الصوت شغّال)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                exo.pause()
                extSession.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    BackHandler {
        // رجوع مرة وحدة: لو فيه لوحة مفتوحة نغلقها، غير هيج نطلع من المشغّل مباشرة
        if (panel != null) closePanel() else exit()
    }

    // ======================= الواجهة =======================
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { ev ->
                val code = ev.nativeKeyEvent.keyCode
                if ((code == AKeyEvent.KEYCODE_BACK || code == AKeyEvent.KEYCODE_ESCAPE) && panel != Panel.TRYSUB) {
                    // رجوع من أول ضغطة مهما كان العنصر المحدد (حتى أثناء التقديم)
                    if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.repeatCount == 0) {
                        if (panel != null) closePanel() else exit()
                    }
                    true
                } else if (ev.type != KeyEventType.KeyDown) {
                    val isOk = code == AKeyEvent.KEYCODE_DPAD_CENTER || code == AKeyEvent.KEYCODE_ENTER || code == AKeyEvent.KEYCODE_NUMPAD_ENTER
                    if (isOk && okSwallow[0]) { okSwallow[0] = false; true } else false
                } else {
                    touch()
                    handleKey(code, ev.nativeKeyEvent.repeatCount)
                }
            }
            .focusRequester(rootFocus)
            .focusable(),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                PlayerView(c).apply {
                    useController = false
                    player = exo
                    keepScreenOn = true
                    isFocusable = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    subtitleView?.apply {
                        setFractionalTextSize(0.055f)
                        setStyle(
                            CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                android.graphics.Color.BLACK,
                                null,
                            ),
                        )
                    }
                }
            },
            onRelease = { it.player = null },
            update = {
                it.subtitleView?.setFractionalTextSize(when (vm.subSize) { 0 -> 0.042f; 1 -> 0.055f; else -> 0.075f })
                it.resizeMode = when (aspect) {
                    0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
        )

        // طبقة اللمس: نقرة = إظهار/إخفاء التحكم، نقرتين = تقديم/رجوع (أو تبديل قناة)، سحب عمودي = سطوع/صوت
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            touch()
                            when {
                                errorMsg != null -> retry()
                                panel != null -> if (panel != Panel.TRYSUB && panel != Panel.AUDIO) closePanel()
                                else -> controls = !controls
                            }
                        },
                        onDoubleTap = { off ->
                            if (panel != null) return@detectTapGestures
                            val w = size.width
                            val live = queue[index].isLive
                            when {
                                off.x < w * 0.35f -> if (live) go(-1) else seekBy(-10_000)
                                off.x > w * 0.65f -> if (live) go(1) else seekBy(10_000)
                                else -> togglePlay()
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { off ->
                            dragLeft = off.x < size.width / 2f
                            volF = audioMgr.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat()
                        },
                        onVerticalDrag = { _, dy ->
                            if (panel != null) return@detectVerticalDragGestures
                            val delta = -dy / size.height * 1.5f
                            if (dragLeft) {
                                val w = activity?.window
                                if (w != null) {
                                    val lp = w.attributes
                                    val cur = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                    lp.screenBrightness = (cur + delta).coerceIn(0.02f, 1f)
                                    w.attributes = lp
                                    showToast("☀ " + (lp.screenBrightness * 100).toInt() + "%")
                                }
                            } else {
                                val max = audioMgr.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                                volF = (volF + delta * max).coerceIn(0f, max.toFloat())
                                audioMgr.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, volF.toInt(), 0)
                                showToast("🔊 " + (volF / max * 100).toInt() + "%")
                            }
                        },
                    )
                },
        )

        if (buffering && errorMsg == null) {
            CircularProgressIndicator(
                color = C.accent, strokeWidth = 4.dp,
                modifier = Modifier.align(Alignment.Center).size(52.dp),
            )
        }

        if (errorMsg != null) {
            Column(
                Modifier.align(Alignment.Center).clip(RoundedCornerShape(16.dp)).background(Color(0xCC000000)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RText(errorMsg.orEmpty(), 22, C.danger, bold = true)
                Spacer(Modifier.height(6.dp))
                RText(t("retry") + "  (OK)", 18, C.text)
            }
        }

        val tmsg = toast
        if (tmsg != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 130.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Color(0xCC1C1033))
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            ) { RText(tmsg, 22, C.text, bold = true) }
        }

        if (extName != null && !controls) {
            Box(
                Modifier.align(Alignment.TopStart).padding(18.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color(0x99000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) { RText("🎧 " + extName.orEmpty(), 15, C.primaryLight, maxLines = 1) }
        }

        // صورة مصغّرة لقناة التعليق (يسار الشاشة): تكبير/تصغير/إزالة من الأزرار تحتها أو من الإعدادات
        if (extSession.pip && extSession.player != null) {
            val pw = when (extSession.pipSize) { 0 -> 190; 1 -> 290; else -> 430 }
            val pwDp = (if (compact) pw * 0.55f else pw.toFloat()).dp
            Column(
                Modifier.align(Alignment.CenterStart).padding(start = if (compact) 8.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.width(pwDp).aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)).background(Color.Black)
                        .border(2.dp, C.primary, RoundedCornerShape(12.dp)),
                ) {
                    AndroidView(
                        factory = { c ->
                            (android.view.LayoutInflater.from(c).inflate(com.ayham.player.R.layout.pip_view, null) as PlayerView)
                        },
                        update = { it.player = extSession.player },
                        modifier = Modifier.fillMaxSize(),
                    )
                    BodyText(
                        extName.orEmpty(), 11, Color.White, maxLines = 1,
                        modifier = Modifier.align(Alignment.BottomStart).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
                if (controls && panel == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconBtn(Icons.Filled.Remove, size = 40.dp) { extSession.cyclePipSize(-1); touch() }
                        IconBtn(Icons.Filled.Add, size = 40.dp) { extSession.cyclePipSize(1); touch() }
                        IconBtn(Icons.Filled.Close, size = 40.dp) { extSession.changePip(false); touch() }
                    }
                }
            }
        }

        // ---------------- أزرار التحكم (دائمًا يسار-يمين) ----------------
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            AnimatedVisibility(visible = controls, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxWidth().height(110.dp).align(Alignment.TopCenter)
                            .background(Brush.verticalGradient(listOf(Color(0xDD000000), Color.Transparent))),
                    )
                    Box(
                        Modifier.fillMaxWidth().height(230.dp).align(Alignment.BottomCenter)
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE000000)))),
                    )
                    Row(
                        Modifier.align(Alignment.TopStart).padding(if (compact) 14.dp else 28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        if (!tv) IconBtn(Icons.Filled.ArrowBack, size = 44.dp) { exit() }
                        BodyText(queue[index].title, 20, C.text, bold = true, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                        if (queue[index].isLive) {
                            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(C.danger).padding(horizontal = 10.dp, vertical = 3.dp)) {
                                BodyText(t("live_badge"), 12, Color.White, bold = true)
                            }
                        }
                    }
                    if (queue[index].isLive && epgList.isNotEmpty()) {
                        Column(
                            Modifier.align(Alignment.TopStart).padding(start = if (compact) 18.dp else 28.dp, top = if (compact) 70.dp else 92.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            val nowS = System.currentTimeMillis() / 1000
                            val cur = epgList.firstOrNull { it.start <= nowS && it.end > nowS } ?: epgList.first()
                            BodyText(t("epg_now") + ": " + cur.title + "  (" + hm(cur.start) + " - " + hm(cur.end) + ")", 15, C.text, maxLines = 1)
                            epgList.firstOrNull { it.start >= cur.end }?.let { nx ->
                                BodyText(t("epg_next") + ": " + nx.title + "  (" + hm(nx.start) + ")", 14, C.textDim, maxLines = 1)
                            }
                        }
                    }
                    if (queue[index].isLive) {
                        var tech by remember { mutableStateOf<List<String>>(emptyList()) }
                        LaunchedEffect(index) {
                            while (true) {
                                tech = runCatching { techChips(exo) }.getOrDefault(emptyList())
                                delay(2000)
                            }
                        }
                        if (tech.isNotEmpty()) Row(
                            Modifier.align(Alignment.TopStart).padding(start = if (compact) 18.dp else 28.dp, top = if (epgList.isNotEmpty()) (if (compact) 118.dp else 150.dp) else (if (compact) 70.dp else 92.dp)),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tech.forEach { c ->
                                Box(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x66FFFFFF).copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    BodyText(c, 12, C.text, bold = true)
                                }
                            }
                        }
                    }
                    Column(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = if (compact) 18.dp else 36.dp, vertical = if (compact) 10.dp else 22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (queue[index].isLive) LiveShiftRow(exo) {
                            exo.seekToDefaultPosition()
                            extSession.goLive()
                            touch()
                        }
                        if (!queue[index].isLive) ProgressRow(exo, barFocus, onVisible = { barVisible = it }, onToggle = { togglePlay() }) { touch() }
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        ) {
                            if (queue.size > 1) IconBtn(Icons.Filled.SkipPrevious) { go(-1); touch() }
                            IconBtn(Icons.Filled.Replay10) { seekBy(-10_000); touch() }
                            IconBtn(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                size = 64.dp,
                                modifier = Modifier.focusRequester(playFocus),
                            ) { togglePlay(); touch() }
                            IconBtn(Icons.Filled.Forward10) { seekBy(10_000); touch() }
                            if (queue.size > 1) IconBtn(Icons.Filled.SkipNext) { go(1); touch() }
                            Spacer(Modifier.width(26.dp))
                            if (!queue[index].isLive) IconBtn(Icons.Filled.Subtitles) { panel = Panel.SUBS; controls = false }
                            if (!queue[index].isLive) IconBtn(Icons.Filled.Speed, active = speed != 1f) { cycleSpeed(); touch() }
                            if (!queue[index].isLive && queue.size > 1) IconBtn(Icons.Filled.FastForward) { doSkip(); touch() }
                            if (queue[index].isLive) {
                                val fav = vm.isFavKey(queue[index].key)
                                IconBtn(if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, active = fav) {
                                    vm.toggleFavLive(queue[index].key)
                                    showToast(if (fav) favRemovedT else favAddedT)
                                    touch()
                                }
                            }
                            if (queue[index].isLive && extName != null) {
                                IconBtn(Icons.Filled.PictureInPictureAlt, active = extSession.pip) { extSession.changePip(!extSession.pip); touch() }
                            }
                            IconBtn(Icons.Filled.Audiotrack, active = extName != null) { panel = Panel.AUDIO; controls = false }
                            IconBtn(Icons.Filled.Tune) { panel = Panel.MENU; controls = false }
                        }
                    }
                }
            }
        }

        val introGo = if (!controls && panel == null) introTarget() else null
        if (introGo != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 90.dp)
                    .background(Color(0xCC140A26), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .border(2.dp, Color.White, androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .clickable { doSkip(); touch() }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) { RText(skipIntroT, 20, Color.White, bold = true) }
        }

        if (panel == Panel.TRYSUB) {
            TrySubWeb(
                onFile = { name, bytes ->
                    val f = com.ayham.player.data.SubTools.fromUpload(subDir(), name, bytes, clean = false)
                    if (f != null) applySub(f, name) else showToast(subBad)
                },
                onClose = { panel = Panel.SUBS },
            )
        }

        // ---------------- اللوحات الجانبية ----------------
        val cur = panel
        if (cur != null && cur != Panel.TRYSUB) {
            Column(
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().widthIn(max = if (cur == Panel.AUDIO || cur == Panel.SYNC) 440.dp else 430.dp)
                    .fillMaxWidth(if (compact) 0.88f else 1f)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .background(if (cur == Panel.AUDIO) androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xB30A0418), Color(0xE60F0720)))
                                else androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xF2140A26), Color(0xF2140A26))))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (cur) {
                    Panel.MENU -> {
                        RText(t("settings"), 26, C.text, bold = true)
                        if (!queue[index].isLive) PanelItem(t("subtitles"), modifier = Modifier.autoFocus()) { panel = Panel.SUBS }
                        PanelItem(t("audio_tracks"), modifier = if (queue[index].isLive) Modifier.autoFocus() else Modifier) { panel = Panel.AUDIO }
                        if (!queue[index].isLive) {
                            PanelItem(t("speed"), sub = speedLabel(speed)) { cycleSpeed() }
                            PanelItem(t("skip_intro"), sub = t("skip_intro_sub")) { doSkip(); closePanel() }
                            if (isSeries) {
                                val key = req.item?.key.orEmpty()
                                PanelItem(t("intro_start")) {
                                    val en = vm.store.introProfile(key)?.second ?: 0L
                                    vm.store.setIntro(key, exo.currentPosition, if (en > exo.currentPosition) en else 0L)
                                    introTick++; showToast(introSavedT); closePanel()
                                }
                                PanelItem(t("intro_end")) {
                                    val st = vm.store.introProfile(key)?.first ?: 0L
                                    vm.store.setIntro(key, if (st < exo.currentPosition) st else 0L, exo.currentPosition)
                                    introTick++; showToast(introSavedT); closePanel()
                                }
                                if (vm.store.introProfile(key) != null) PanelItem(t("intro_reset")) {
                                    vm.store.setIntro(key, 0L, 0L); introTick++; closePanel()
                                }
                            }
                        }
                        PanelItem(t("smart_audio"), selected = extName != null, sub = extName) { panel = Panel.EXT }
                        if (extName != null) PanelItem(t("sync")) { panel = Panel.SYNC }
                        PanelItem(t("aspect"), sub = aspectLabels[aspect]) {
                            aspect = (aspect + 1) % 3
                            showToast(toastAspect + ": " + aspectLabels[aspect])
                        }
                        if (queue[index].isLive) {
                            PanelItem(t("epg")) {
                                epgFull = emptyList()
                                panel = Panel.EPG
                            }
                        } else {
                            val sizes = listOf(t("size_s"), t("size_m"), t("size_l"))
                            PanelItem(t("sub_size"), sub = sizes[vm.subSize.coerceIn(0, 2)]) {
                                vm.changeSubSize((vm.subSize + 1) % 3)
                            }
                        }
                        PanelItem(t("close")) { closePanel() }
                    }

                    Panel.SUBS -> {
                        RText(t("subtitles"), 26, C.text, bold = true)
                        val textTracks = tracks.groups.filter { it.type == MC.TRACK_TYPE_TEXT }
                        val anySelected = textTracks.any { g -> (0 until g.length).any { g.isTrackSelected(it) } }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            item {
                                PanelItem(t("off"), selected = !anySelected, modifier = Modifier.autoFocus()) {
                                    vm.setPrefSub(queue[index].key, null)
                                    exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                                        .setTrackTypeDisabled(MC.TRACK_TYPE_TEXT, true).build()
                                }
                            }
                            val flatText = textTracks.flatMap { g -> (0 until g.length).map { g to it } }
                            items(flatText.size) { n ->
                                val (g, i) = flatText[n]
                                PanelItem(
                                    trackLabel(g.getTrackFormat(i), i, vm.lang),
                                    selected = g.isTrackSelected(i),
                                    sub = t("sub_embedded"),
                                ) {
                                    val lbl = g.getTrackFormat(i).label
                                    val mk = queue[index].key
                                    vm.setPrefSub(mk, vm.savedSubs(mk).firstOrNull { sv -> sv.label == lbl }?.uri)
                                    exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                                        .setTrackTypeDisabled(MC.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
                                        .build()
                                }
                            }
                            if (vm.savedSubs(queue[index].key).isNotEmpty()) {
                                item {
                                    PanelItem(t("sub_clear_saved")) {
                                        vm.clearSubs(queue[index].key)
                                        loadMedia(exo.currentPosition, withSubs = false)
                                        showToast(subClearedT)
                                    }
                                }
                            }
                            if (tv) item { PanelItem(t("sub_phone"), sub = t("sub_phone_help")) { panel = Panel.SUBPHONE } }
                            else item { PanelItem(t("sub_file"), sub = "srt / vtt / zip") { pickSub.launch(arrayOf("*/*")) } }
                            item { PanelItem(t("sub_trysub"), sub = "subdl.com") { panel = Panel.TRYSUB } }
                            item { PanelItem(t("sub_url")) { panel = Panel.SUBURL } }
                            item { PanelItem(t("back")) { panel = Panel.MENU } }
                        }
                    }

                    Panel.SUBURL -> {
                        RText(t("sub_url"), 24, C.text, bold = true)
                        AyhamField(
                            subUrl, { subUrl = it }, "https://.../subtitle.srt",
                            Modifier.autoFocus(), imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                        ) {
                            if (subUrl.isNotBlank()) addSub(subUrl.trim().substringBefore('?').substringAfterLast('/').take(30).ifBlank { "URL" }, subUrl.trim())
                        }
                        GlowButton(t("sub_load"), primary = true) {
                            if (subUrl.isNotBlank()) addSub(subUrl.trim().substringBefore('?').substringAfterLast('/').take(30).ifBlank { "URL" }, subUrl.trim())
                        }
                        GlowButton(t("back")) { panel = Panel.SUBS }
                    }

                    Panel.SUBPHONE -> {
                        var url by remember { mutableStateOf<String?>(null) }
                        DisposableEffect(Unit) {
                            val main = android.os.Handler(android.os.Looper.getMainLooper())
                            val srv = com.ayham.player.data.LoginServer(onUpload = { name, bytes ->
                                main.post {
                                    val f = com.ayham.player.data.SubTools.fromUpload(subDir(), name, bytes, clean = false)
                                    if (f != null) applySub(f, name) else showToast(subBad)
                                }
                            })
                            val ip = com.ayham.player.data.LoginServer.localIp()
                            if (ip != null && srv.start()) url = "http://$ip:${srv.port}"
                            onDispose { srv.stop() }
                        }
                        RText(t("sub_phone"), 26, C.text, bold = true)
                        RText(t("sub_phone_help"), 16, C.textDim)
                        val u = url
                        if (u != null) {
                            val bmp = remember(u) { qrBitmap(u) }
                            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(10.dp).align(Alignment.CenterHorizontally)) {
                                androidx.compose.foundation.Image(bmp.asImageBitmap(), null, Modifier.size(220.dp))
                            }
                            BodyText(u, 14, C.primaryLight, modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            RText(t("qr_fail"), 16, C.danger)
                        }
                        GlowButton(t("back"), modifier = Modifier.autoFocus()) { panel = Panel.SUBS }
                    }

                    Panel.TRYSUB -> {
                        // يُعرض بملء الشاشة فوق المشغّل (انظر أسفل الملف)
                    }

                    Panel.EPG -> {
                        RText(t("epg"), 26, C.text, bold = true)
                        val id = queue[index].key.removePrefix("LIVE:")
                        LaunchedEffect(id) { epgFull = vm.epg(id, full = true) }
                        val list = if (epgFull.isNotEmpty()) epgFull else epgList
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            if (list.isEmpty()) item { RText(t("epg_none"), 16, C.textDim) }
                            items(list.size) { n ->
                                val e = list[n]
                                PanelItem(e.title, sub = hm(e.start) + " - " + hm(e.end), modifier = if (n == 0) Modifier.autoFocus() else Modifier) { }
                            }
                            item { PanelItem(t("back"), modifier = if (list.isEmpty()) Modifier.autoFocus() else Modifier) { panel = Panel.MENU } }
                        }
                    }

                    Panel.AUDIO -> {
                        val ar = vm.lang == "ar"
                        val audio = tracks.groups.filter { it.type == MC.TRACK_TYPE_AUDIO }
                        val live = queue[index].isLive
                        var tab by remember { mutableIntStateOf(0) }
                        var q by remember { mutableStateOf("") }
                        var results by remember { mutableStateOf<List<Item>>(emptyList()) }
                        val recent = remember { vm.extRecent() }
                        val searching = q.trim().length >= 2
                        LaunchedEffect(q) {
                            delay(300)
                            results = if (q.trim().length >= 2) vm.search(q).first else emptyList()
                        }
                        val pager = rememberPager(Unit, 60) { off, lim -> vm.allItems(com.ayham.player.data.Kind.LIVE, lim, off) }
                        val chosen = extSession.channel()?.key
                        // التأخير: موجب = التعليق متأخر عن الصورة، سالب = الصورة متأخرة عن التعليق
                        val commentary = if (extSession.sameStream) -extSession.picDelayMs else extSession.manualMs
                        val extActive = extSession.player != null
                        val lastAdj = remember { longArrayOf(0L) }
                        fun ensureSame() {
                            if (!extSession.sameStream && extSession.player == null) {
                                val sel = audio.flatMap { g -> (0 until g.length).filter { g.isTrackSelected(it) }.map { g.getTrackFormat(it) } }.firstOrNull()
                                extSession.start(com.ayham.player.data.Item(com.ayham.player.data.Kind.LIVE, queue[index].key.substringAfter(':'), queue[index].title), true, sel?.let { it.id ?: it.label })
                            }
                        }
                        fun adj(ms: Long) {
                            val nw = System.currentTimeMillis()
                            if (nw - lastAdj[0] < 220L) return
                            lastAdj[0] = nw
                            if (kotlin.math.abs(commentary + ms) > 30_000L) { showToast(if (ar) "وصلت للحد الأقصى 30 ثانية" else "Max 30 seconds"); return }
                            val ok = runCatching {
                                ensureSame()
                                if (extSession.sameStream) extSession.requestAdjust(-ms) else extSession.nudge(ms)
                            }.getOrDefault(false)
                            if (!ok) showToast(notSeekable)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.Audiotrack, null, tint = C.accent, modifier = Modifier.size(28.dp))
                            Column {
                                RText(if (ar) "Ayham Smart Audio" else "Ayham Smart Audio", 23, C.text, bold = true)
                                BodyText(if (ar) "المسارات والمزامنة للبث المباشر" else "Tracks and sync for Live TV", 13, C.primaryLight)
                            }
                        }
                        if (live && extActive) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                GlowButton(
                                    (if (ar) "المزامنة التلقائية: " else "Auto sync: ") +
                                        (if (extSession.autoSync) (if (ar) "مفعّلة" else "On") else (if (ar) "متوقفة" else "Off")),
                                    primary = extSession.autoSync, height = 54.dp,
                                    modifier = Modifier.weight(1f).autoFocus(),
                                ) {
                                    extSession.setAuto(!extSession.autoSync)
                                    vm.extAuto = extSession.autoSync
                                }
                                GlowButton(if (ar) "زامن الآن" else "Sync now", height = 54.dp, modifier = Modifier.weight(1f)) {
                                    extSession.resyncNow()
                                }
                            }
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            if (live) {
                                item {
                                    val txt = when {
                                        commentary > 0 -> (if (ar) "التعليق متأخر " else "Commentary late ") + String.format(Locale.US, "%.0f", commentary / 1000f) + (if (ar) " ث" else " s")
                                        commentary < 0 -> (if (ar) "الصورة متأخرة " else "Picture late ") + String.format(Locale.US, "%.0f", -commentary / 1000f) + (if (ar) " ث" else " s")
                                        else -> if (ar) "التأخير: 0 ث" else "Delay: 0 s"
                                    }
                                    val saved = vm.store.audioPref(queue[index].key)
                                    Column(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.surface.copy(alpha = 0.9f))
                                            .border(1.dp, if (commentary != 0L) C.accent else C.surfaceHi, RoundedCornerShape(14.dp)).padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        RText(txt, 20, if (commentary != 0L) C.accent else C.text, bold = true)
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            GlowButton("− 1.0 ث", primary = false, height = 54.dp, modifier = Modifier.weight(1f).autoFocus()) { adj(-1000L) }
                                            GlowButton("+ 1.0 ث", primary = true, height = 54.dp, modifier = Modifier.weight(1f)) { adj(1000L) }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            GlowButton("− 0.5 ث", primary = false, height = 54.dp, modifier = Modifier.weight(1f)) { adj(-500L) }
                                            GlowButton("+ 0.5 ث", primary = false, height = 54.dp, modifier = Modifier.weight(1f)) { adj(500L) }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            GlowButton("− 100 ms", primary = false, height = 54.dp, modifier = Modifier.weight(1f)) { adj(-100L) }
                                            GlowButton("+ 100 ms", primary = false, height = 54.dp, modifier = Modifier.weight(1f)) { adj(100L) }
                                        }
                                        GlowButton(
                                            if (ar) "إعادة ضبط المزامنة · 0 ms" else "Reset sync · 0 ms",
                                            primary = false, height = 54.dp, modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            runCatching {
                                                if (extSession.sameStream) extSession.requestAdjust(extSession.picDelayMs)
                                                else extSession.nudge(-extSession.manualMs)
                                            }
                                        }
                                        BodyText(if (ar) "− يؤخر الصوت الخارجي | + يؤخر الصورة" else "− delays external audio | + delays video", 13, C.textDim)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            GlowButton(if (ar) "الصوت الأصلي" else "Original", modifier = Modifier.weight(1f)) { runCatching { stopExt() } }
                                            GlowButton(if (ar) "إغلاق" else "Close", modifier = Modifier.weight(1f)) { closePanel() }
                                        }
                                        GlowButton(
                                            if (saved != null) (if (ar) "محفوظ ✓ (حدّث)" else "Saved ✓ (update)") else (if (ar) "حفظ التأخير لهذه القناة" else "Save for this channel"),
                                            primary = saved == null, modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            val v = if (extSession.sameStream) "s\t" + (extSession.wantTrack ?: "") + "\t" + commentary + "\t\t"
                                            else if (extActive) "e\t\t" + commentary + "\t" + (extSession.channel()?.id ?: "") + "\t" + (extSession.channel()?.name ?: "")
                                            else null
                                            if (v == null) showToast(if (ar) "اضبط التأخير أو اختر تعليقًا أولًا" else "Set a delay or pick commentary first")
                                            else { vm.store.setAudioPref(queue[index].key, v); showToast(if (ar) "تم حفظ التأخير لهذه القناة" else "Saved for this channel") }
                                        }
                                        if (saved != null) GlowButton(if (ar) "مسح المحفوظ" else "Clear saved", modifier = Modifier.fillMaxWidth()) {
                                            vm.store.setAudioPref(queue[index].key, null); showToast(if (ar) "تم المسح" else "Cleared")
                                        }
                                        if (extActive && extSession.status.isNotBlank()) BodyText(extSession.status, 11, C.textDim)
                                    }
                                }
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Chip(if (ar) "مسارات البث" else "Stream tracks", tab == 0) { tab = 0 }
                                    Chip(if (ar) "قنوات التعليق" else "Commentary channels", tab == 1) { tab = 1 }
                                }
                            }
                            if (tab == 0) {
                                val flatAudio = audio.flatMap { g -> (0 until g.length).map { g to it } }
                                if (flatAudio.isEmpty()) item { BodyText(if (ar) "لا توجد مسارات صوت في هذا البث" else "No audio tracks in this stream", 14, C.textDim) }
                                items(flatAudio.size) { n ->
                                    val (g, i) = flatAudio[n]
                                    val supported = g.isTrackSupported(i)
                                    val key = g.getTrackFormat(i).let { it.id ?: it.label ?: "" }
                                    PanelItem(
                                        trackLabel(g.getTrackFormat(i), i, vm.lang),
                                        selected = if (extSession.sameStream) key == extSession.wantTrack else (!extActive && g.isTrackSelected(i)),
                                        sub = if (supported) null else audioUnsupported,
                                    ) {
                                        if (!supported) return@PanelItem
                                        if (extSession.sameStream) { extSession.selectTrack(key); return@PanelItem }
                                        if (extActive) stopExt()
                                        audioOverridden = true
                                        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
                                            .setTrackTypeDisabled(MC.TRACK_TYPE_AUDIO, false)
                                            .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, i))
                                            .build()
                                    }
                                }
                            } else {
                                item {
                                    AyhamField(q, { q = it }, t("search_hint"), Modifier, imeAction = androidx.compose.ui.text.input.ImeAction.Search)
                                }
                                if (extName != null && !extSession.sameStream) {
                                    item { PanelItem(t("ext_audio_remove")) { stopExt() } }
                                    item { PanelItem(t("sync")) { panel = Panel.SYNC } }
                                }
                                if (searching) {
                                    items(results, key = { "s" + it.key }) { ch ->
                                        PanelItem(ch.name, selected = ch.key == chosen && !extSession.sameStream) { startExt(ch) }
                                    }
                                } else {
                                    if (recent.isNotEmpty()) {
                                        item { RText(t("ext_recent"), 16, C.primaryLight, bold = true) }
                                        items(recent, key = { "r" + it.key }) { ch ->
                                            PanelItem(ch.name, selected = ch.key == chosen && !extSession.sameStream) { startExt(ch) }
                                        }
                                        item { RText(t("ext_all"), 16, C.primaryLight, bold = true) }
                                    }
                                    itemsIndexed(pager.items, key = { _, it -> "a" + it.key }) { idx, ch ->
                                        if (idx >= pager.items.size - 15) LaunchedEffect(pager.items.size) { pager.loadMore() }
                                        PanelItem(ch.name, selected = ch.key == chosen && !extSession.sameStream) { startExt(ch) }
                                    }
                                }
                            }
                            if (!live) item { PanelItem(t("back"), modifier = Modifier.autoFocus()) { closePanel() } }
                        }
                    }

                    Panel.EXT -> {
                        RText(t("ext_audio_pick"), 24, C.text, bold = true)
                        var q by remember { mutableStateOf("") }
                        var results by remember { mutableStateOf<List<Item>>(emptyList()) }
                        val recent = remember { vm.extRecent() }
                        val searching = q.trim().length >= 2
                        LaunchedEffect(q) {
                            delay(300)
                            results = if (q.trim().length >= 2) vm.search(q).first else emptyList()
                        }
                        val pager = rememberPager(Unit, 60) { off, lim -> vm.allItems(com.ayham.player.data.Kind.LIVE, lim, off) }
                        val chosen = extSession.channel()?.key
                        AyhamField(
                            q, { q = it }, t("search_hint"), Modifier.autoFocus(),
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            if (extName != null) {
                                item {
                                    PanelItem(t("ext_audio_remove")) {
                                        stopExt()
                                        closePanel()
                                    }
                                }
                            }
                            if (searching) {
                                items(results, key = { "s" + it.key }) { ch ->
                                    PanelItem(ch.name, selected = ch.key == chosen) { startExt(ch); panel = Panel.SYNC }
                                }
                            } else {
                                if (recent.isNotEmpty()) {
                                    item { RText(t("ext_recent"), 16, C.primaryLight, bold = true) }
                                    items(recent, key = { "r" + it.key }) { ch ->
                                        PanelItem(ch.name, selected = ch.key == chosen) { startExt(ch); panel = Panel.SYNC }
                                    }
                                    item { RText(t("ext_all"), 16, C.primaryLight, bold = true) }
                                }
                                itemsIndexed(pager.items, key = { _, it -> "a" + it.key }) { idx, ch ->
                                    if (idx >= pager.items.size - 15) LaunchedEffect(pager.items.size) { pager.loadMore() }
                                    PanelItem(ch.name, selected = ch.key == chosen) { startExt(ch); panel = Panel.SYNC }
                                }
                            }
                            item { PanelItem(t("back")) { panel = Panel.MENU } }
                        }
                    }

                    Panel.SYNC -> {
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RText(t("smart_settings"), 26, C.text, bold = true)
                            if (extName != null) BodyText("🎧 $extName", 15, C.primaryLight, maxLines = 2)
                            else BodyText(t("ext_none"), 14, C.textDim)
                            if (extSession.status.isNotBlank()) BodyText(extSession.status, 14, C.text)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip(t("auto_sync") + ": " + t(if (extSession.autoSync) "on" else "off"), extSession.autoSync) {
                                    extSession.setAuto(!extSession.autoSync)
                                    vm.extAuto = extSession.autoSync
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlowButton(t("resync_now"), primary = true, modifier = Modifier.autoFocus()) { extSession.resyncNow() }
                                GlowButton(t("change_channel")) { panel = Panel.EXT }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (extSession.suspended) GlowButton(t("resume_ext")) { extSession.resume() }
                                else if (extName != null) GlowButton(t("back_original")) { extSession.useOriginal() }
                                GlowButton(t("ext_audio_remove")) { stopExt(); closePanel() }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip(t("pip_show") + ": " + t(if (extSession.pip) "on" else "off"), extSession.pip) {
                                    extSession.changePip(!extSession.pip)
                                }
                            }
                            if (extSession.pip) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GlowButton(t("pip_smaller")) { extSession.cyclePipSize(-1) }
                                    GlowButton(t("pip_bigger")) { extSession.cyclePipSize(1) }
                                }
                            }
                            RText(t("vol") + ": " + (extVolume * 100).toInt() + "%", 18, C.textDim)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlowButton("-") { extSession.setVolumeLevel(extSession.volume - 0.1f) }
                                GlowButton("+") { extSession.setVolumeLevel(extSession.volume + 0.1f) }
                            }
                            RText(t("main_vol") + ": " + (extSession.mainVolume * 100).toInt() + "%", 18, C.textDim)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlowButton("-") { extSession.changeMainVolume(extSession.mainVolume - 0.1f) }
                                GlowButton("+") { extSession.changeMainVolume(extSession.mainVolume + 0.1f) }
                            }
                            RText(t("manual_tools"), 16, C.textDim, bold = true)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip(t("target_audio"), syncAudio) { syncAudio = true }
                                Chip(t("target_video"), !syncAudio) { syncAudio = false }
                            }
                            RText("${t("delay_audio")} (${t("sec")})", 16, C.textDim)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(100L to "0.1", 500L to "0.5", 1000L to "1", 5000L to "5").forEach { (ms, label) ->
                                    GlowButton(label) { shift(ms) }
                                }
                            }
                            RText("${t("advance_audio")} (${t("sec")})", 16, C.textDim)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(100L to "0.1", 500L to "0.5", 1000L to "1", 5000L to "5").forEach { (ms, label) ->
                                    GlowButton(label) { shift(-ms) }
                                }
                            }
                            BodyText(
                                t("manual_offset") + ": " + (if (extSession.manualMs >= 0) "+" else "") + String.format(Locale.US, "%.1f", extSession.manualMs / 1000f) + t("sec"),
                                14, C.text,
                            )
                            if (extSession.autoSync && extName != null && offsetMs != 0L) {
                                BodyText(
                                    t("last_measured") + ": " + (if (offsetMs >= 0) "+" else "") + String.format(Locale.US, "%.2f", offsetMs / 1000f) + t("sec"),
                                    14, C.text,
                                )
                            }
                            GlowButton(t("reset_feature")) { stopExt(); vm.resetExtSettings(); extSession.autoSync = true }
                            GlowButton(t("close"), primary = true) { closePanel() }
                        }
                    }
                }
            }
        }
    }
}

private fun trackLabel(f: Format, i: Int, lang: String): String {
    val language = f.language?.let {
        Locale.forLanguageTag(it).getDisplayLanguage(Locale(lang))
    }?.takeIf { it.isNotBlank() }
    return listOfNotNull(f.label, language).distinct().joinToString(" - ").ifEmpty { "Track ${i + 1}" }
}

/**
 * شريط التقدم: له حالته الخاصة فلا يعيد رسم المشغّل. وتقدر تروح له (فوق) وتحرّك يمين/يسار
 * لتقدّم أو ترجّع بكيفك، والضغط المستمر يسرّع الحركة، والتنفيذ بعد توقفك لحظة (أو بزر OK).
 */
@Composable
private fun ProgressRow(exo: ExoPlayer, barFocus: FocusRequester, onVisible: (Boolean) -> Unit, onToggle: () -> Unit, onTouch: () -> Unit) {
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var scrub by remember { mutableStateOf<Long?>(null) }
    var focused by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(exo) {
        while (true) {
            if (scrub == null) pos = exo.currentPosition
            dur = if (exo.duration == MC.TIME_UNSET) 0L else exo.duration
            delay(1000)
        }
    }
    LaunchedEffect(dur > 0) { onVisible(dur > 0) }
    if (dur <= 0) return
    val shown = scrub ?: pos
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BodyText(fmt(shown), 14, if (scrub != null) C.accent else C.text, bold = scrub != null)
        var barW by remember { mutableStateOf(1) }
        fun posAt(x: Float): Long = (x / barW.coerceAtLeast(1) * dur).toLong().coerceIn(0L, dur)
        Box(
            Modifier.weight(1f).height(34.dp)
                .onSizeChanged { barW = it.width }
                .pointerInput(dur) {
                    detectTapGestures { off ->
                        exo.seekTo(posAt(off.x))
                        onTouch()
                    }
                }
                .pointerInput(dur) {
                    detectHorizontalDragGestures(
                        onDragStart = { off -> scrub = posAt(off.x); onTouch() },
                        onDragEnd = { scrub?.let { exo.seekTo(it) }; scrub = null },
                        onDragCancel = { scrub = null },
                    ) { change, _ ->
                        scrub = posAt(change.position.x)
                        onTouch()
                    }
                }
                .focusRequester(barFocus)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val code = ev.nativeKeyEvent.keyCode
                    when (code) {
                        AKeyEvent.KEYCODE_DPAD_LEFT, AKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            val rc = ev.nativeKeyEvent.repeatCount
                            val step = when { rc >= 15 -> 60_000L; rc >= 5 -> 30_000L; else -> 10_000L }
                            val dir = if (code == AKeyEvent.KEYCODE_DPAD_LEFT) -1 else 1
                            val base = scrub ?: exo.currentPosition
                            scrub = (base + dir * step).coerceIn(0L, dur)
                            onTouch()
                            job?.cancel()
                            job = scope.launch {
                                delay(700)
                                scrub?.let { exo.seekTo(it) }
                                scrub = null
                            }
                            true
                        }
                        AKeyEvent.KEYCODE_DPAD_CENTER, AKeyEvent.KEYCODE_ENTER -> {
                            // OK على الشريط: لو فيه تقديم معلّق ننفذه، وغير هيج إيقاف/تشغيل
                            if (ev.nativeKeyEvent.repeatCount == 0) {
                                job?.cancel()
                                val target = scrub
                                if (target != null) {
                                    exo.seekTo(target)
                                    scrub = null
                                } else {
                                    onToggle()
                                }
                                onTouch()
                            }
                            true
                        }
                        else -> false
                    }
                }
                .focusable(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier.fillMaxWidth().height(if (focused) 16.dp else 8.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0x44FFFFFF)),
            ) {
                Box(
                    Modifier.fillMaxWidth((shown.toFloat() / dur).coerceIn(0f, 1f))
                        .fillMaxHeight().background(C.glow),
                )
            }
        }
        BodyText(fmt(dur), 14, C.text)
    }
}

/** موقع SubDL داخل التطبيق: حمّل الترجمة منه وتنطبق على الفيلم مباشرة. */
@Composable
private fun TrySubWeb(onFile: (String, ByteArray) -> Unit, onClose: () -> Unit) {
    val main = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var web by remember { mutableStateOf<android.webkit.WebView?>(null) }
    val file by rememberUpdatedState(onFile)
    Box(Modifier.fillMaxSize().background(Color(0xFF0B0614))) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                android.webkit.WebView(c).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    webViewClient = android.webkit.WebViewClient()
                    setDownloadListener { url, ua, disposition, mime, _ ->
                        val cookie = android.webkit.CookieManager.getInstance().getCookie(url).orEmpty()
                        val name = android.webkit.URLUtil.guessFileName(url, disposition, mime)
                        Thread {
                            runCatching {
                                val req = okhttp3.Request.Builder().url(url)
                                    .header("User-Agent", ua)
                                    .header("Cookie", cookie)
                                    .header("Referer", "https://subdl.com/")
                                    .build()
                                com.ayham.player.data.XtreamApi.client.newCall(req).execute().use { r ->
                                    val bytes = r.body?.bytes()
                                    if (r.isSuccessful && bytes != null) main.post { file(name, bytes) }
                                }
                            }
                        }.start()
                    }
                    loadUrl("https://subdl.com/")
                    requestFocus()
                    web = this
                }
            },
            onRelease = { it.destroy() },
        )
        Row(
            Modifier.align(Alignment.TopEnd).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GlowButton(t("close"), primary = true) { onClose() }
        }
    }
    BackHandler {
        val w = web
        if (w != null && w.canGoBack()) w.goBack() else onClose()
    }
}


/**
 * شريط البث المباشر (DVR): يظهر فقط لما المصدر يسمح فعلًا بالتنقل الزمني (HLS بنافذة قابلة للتقديم).
 * يبيّن موضع المشاهدة، ونقطة المباشر، والمتاح للرجوع، وزر العودة للمباشر.
 */
@Composable
private fun LiveShiftRow(exo: ExoPlayer, onBackToLive: () -> Unit) {
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var live by remember { mutableLongStateOf(0L) }
    LaunchedEffect(exo) {
        while (true) {
            val tl = exo.currentTimeline
            if (!tl.isEmpty && exo.isCurrentMediaItemSeekable) {
                val w = tl.getWindow(exo.currentMediaItemIndex, androidx.media3.common.Timeline.Window())
                if (w.durationMs != MC.TIME_UNSET && w.durationMs > 15_000L) {
                    dur = w.durationMs
                    live = if (w.defaultPositionMs != MC.TIME_UNSET) w.defaultPositionMs else w.durationMs
                    pos = exo.currentPosition
                } else dur = 0L
            } else dur = 0L
            delay(500)
        }
    }
    if (dur <= 0L) return
    val behind = (live - pos).coerceAtLeast(0L)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BodyText(t("rewind_avail") + ": " + fmt(pos), 14, C.text)
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x44FFFFFF))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((pos.toFloat() / dur).coerceIn(0f, 1f)).background(C.primary))
            val lf = (live.toFloat() / dur).coerceIn(0.01f, 0.99f)
            Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(lf))
                Box(Modifier.fillMaxHeight().width(3.dp).background(C.danger))
                Spacer(Modifier.weight(1f - lf))
            }
        }
        if (behind > 4_000L) {
            BodyText(t("live_behind") + " " + fmt(behind), 14, C.accent, bold = true)
            GlowButton(t("back_to_live"), primary = true) { onBackToLive() }
        } else {
            BodyText(t("at_live"), 14, C.danger, bold = true)
        }
    }
}

private fun techChips(exo: ExoPlayer): List<String> {
    val out = ArrayList<String>()
    exo.videoFormat?.let { v ->
        if (v.height > 0) out += "${v.height}p"
        if (v.frameRate > 0f) out += "${Math.round(v.frameRate)} fps"
        val br = if (v.bitrate > 0) v.bitrate else v.averageBitrate
        if (br > 0) out += String.format(Locale.US, "%.1f Mbps", br / 1_000_000f)
        out += when {
            v.sampleMimeType?.contains("hevc") == true -> "HEVC"
            v.sampleMimeType?.contains("avc") == true -> "H.264"
            v.sampleMimeType?.contains("av01") == true -> "AV1"
            v.sampleMimeType?.contains("mpeg2") == true -> "MPEG-2"
            else -> ""
        }
    }
    exo.audioFormat?.let { a ->
        out += when {
            a.sampleMimeType?.contains("mp4a") == true -> "AAC"
            a.sampleMimeType?.contains("eac3") == true -> "E-AC3"
            a.sampleMimeType?.contains("ac3") == true -> "AC3"
            a.sampleMimeType?.contains("mpeg") == true -> "MP3"
            else -> ""
        }
        out += when (a.channelCount) { 1 -> "Mono"; 2 -> "Stereo"; 6 -> "5.1"; 8 -> "7.1"; else -> if (a.channelCount > 0) "${a.channelCount}ch" else "" }
    }
    return out.filter { it.isNotBlank() }
}
