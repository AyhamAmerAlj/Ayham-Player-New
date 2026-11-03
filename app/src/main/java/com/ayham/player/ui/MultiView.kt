package com.ayham.player.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.EpgEntry
import com.ayham.player.data.Item
import com.ayham.player.data.Kind
import com.ayham.player.data.LiveSource
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * عرض متعدد: حتى 4 قنوات معًا. الصوت لخانة واحدة فقط، والباقي مكتومة بجودة منخفضة لتخفيف الحمل.
 * كل خانة تستهلك اتصالًا من اشتراك المشترك، لذلك عدد الخانات يتبع حدّ اتصالات الحساب.
 */
@Composable
fun MultiView(vm: AppViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val ar = vm.lang == "ar"
    val maxConn = vm.maxConnectionsInt()
    val cap = if (maxConn in 1..3) maxConn.coerceAtLeast(1) else 4
    var slots by remember { mutableIntStateOf(minOf(2, cap)) }
    val n = minOf(slots, cap)
    val cells = remember { androidx.compose.runtime.mutableStateListOf<Item?>(*arrayOfNulls<Item>(4)) }
    val players = remember { arrayOfNulls<ExoPlayer>(4) }
    var audio by remember { mutableIntStateOf(0) }
    var picking by remember { mutableStateOf<Int?>(null) }
    var note by remember { mutableStateOf<String?>(null) }

    fun applyAudio() {
        for (i in 0 until 4) players[i]?.let { p ->
            p.volume = if (i == audio) 1f else 0f
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setMaxVideoSize(if (i == audio) 1280 else 854, if (i == audio) 720 else 480).build()
        }
    }
    fun assign(i: Int, ch: Item) {
        players[i]?.release()
        val url = vm.urlForLive(ch)
        if (url.isEmpty()) return
        val p = com.ayham.player.PlayerFactory.create(ctx, 720)
        p.setMediaItem(MediaItem.fromUri(url)); p.prepare(); p.playWhenReady = true
        players[i] = p
        cells[i] = ch
        applyAudio()
    }
    LaunchedEffect(Unit) {
        vm.stopMini()
        vm.multiStart?.let { assign(0, it); if (cap > 1) picking = 1 }
        if (maxConn == 1) note = if (ar) "اشتراكك يسمح باتصال واحد فقط، فلا يمكن عرض أكثر من قناة." else "Your plan allows a single connection."
    }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) players.forEach { it?.pause() }
            if (e == androidx.lifecycle.Lifecycle.Event.ON_START) players.forEach { it?.playWhenReady = true }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); for (i in 0 until 4) { players[i]?.release(); players[i] = null } }
    }
    BackHandler(enabled = picking == null) { vm.closeMulti() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RText(if (ar) "عرض متعدد" else "Multi-view", 18, Color.White, bold = true, maxLines = 1)
                BodyText(
                    note ?: if (ar) "OK: صوت ثم ملء الشاشة  ·  ضغطة مطوّلة: تغيير القناة  ·  رجوع: خروج (كل خانة تستهلك اتصالًا)" else "OK: audio, then fullscreen · Long press: change · Back: exit (each cell uses a connection)",
                    12, if (note != null) C.danger else C.textDim, maxLines = 1,
                )
                if (slots < cap) Focusable(shape = RoundedCornerShape(20.dp), focusScale = 1.05f, onClick = { slots = slots + 1; picking = slots - 1 }) { f ->
                    Row(Modifier.clip(RoundedCornerShape(20.dp)).background(if (f) tc(0x558B5CF6) else tc(0x33FFFFFF)).padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        RText(if (ar) "إضافة قناة" else "Add channel", 13, Color.White, bold = true, maxLines = 1)
                    }
                }
            }
            val rows = if (n <= 2) 1 else 2
            val perRow = if (n <= 2) n else 2
            for (r in 0 until rows) Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0 until perRow) {
                    val i = r * perRow + c
                    if (i >= n) { Box(Modifier.weight(1f).fillMaxHeight()); continue }
                    val ch = cells[i]
                    val shape = RoundedCornerShape(14.dp)
                    Focusable(
                        Modifier.weight(1f).fillMaxHeight().then(if (i == 0) Modifier.autoFocus() else Modifier),
                        shape, 1.01f,
                        onLongClick = { picking = i },
                        onClick = {
                            if (ch == null) picking = i
                            else if (audio != i) { audio = i; applyAudio() }
                            else {
                                val list = cells.filterNotNull()
                                vm.closeMulti()
                                vm.playLive(ch, LiveSource(favs = list))
                            }
                        },
                    ) { f ->
                        Box(Modifier.fillMaxSize().clip(shape).background(Color(0xFF0B0614)).border(if (audio == i && ch != null) 2.dp else 0.dp, C.accent, shape)) {
                            val p = players[i]
                            if (ch != null && p != null) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { c2 -> PlayerView(c2).apply { useController = false; player = p; isFocusable = false; setShutterBackgroundColor(android.graphics.Color.BLACK) } },
                                    update = { it.player = p },
                                )
                                Row(
                                    Modifier.align(Alignment.BottomStart).fillMaxWidth().background(tc(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (audio == i) Icon(Icons.Filled.VolumeUp, null, tint = C.accent, modifier = Modifier.size(16.dp))
                                    BodyText(ch.name, 13, Color.White, bold = f || audio == i, maxLines = 1)
                                }
                            } else {
                                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Add, null, tint = if (f) C.accent else C.textDim, modifier = Modifier.size(36.dp))
                                    BodyText(if (ar) "اختر قناة" else "Pick a channel", 14, if (f) Color.White else C.textDim)
                                }
                            }
                        }
                    }
                }
            }
        }
        val pk = picking
        if (pk != null) ChannelPicker(vm, ar, onClose = { picking = null }) { ch -> assign(pk, ch); picking = null }
    }
}

@Composable
private fun ChannelPicker(vm: AppViewModel, ar: Boolean, onClose: () -> Unit, onPick: (Item) -> Unit) {
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<List<Item>>(emptyList()) }
    val base = remember(vm.favorites, vm.history) { (vm.favorites.filter { it.kind == Kind.LIVE } + vm.historyOf(Kind.LIVE)).distinctBy { it.key } }
    LaunchedEffect(q) {
        if (q.trim().length < 2) { res = base.ifEmpty { vm.allItems(Kind.LIVE, 60, 0) }; return@LaunchedEffect }
        delay(350)
        res = vm.searchKind(Kind.LIVE, q, 60)
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.85f).clip(RoundedCornerShape(20.dp)).background(tc(0xFF140A26)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RText(if (ar) "اختر قناة" else "Pick a channel", 20, Color.White, bold = true)
            AyhamField(q, { q = it }, t("search_in_live"), Modifier.fillMaxWidth(), imeAction = androidx.compose.ui.text.input.ImeAction.Search)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(res, key = { it.key }) { ch -> ChannelRow(ch, false, fav = vm.isIn("fav", ch)) { onPick(ch) } }
            }
        }
    }
}
