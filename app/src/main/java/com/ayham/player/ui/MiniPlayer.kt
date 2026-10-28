@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ayham.player.AppViewModel
import com.ayham.player.data.Item

/** شاشة مصغّرة تبقى ظاهرة بجانب قائمة القنوات أثناء التصفح. */
@Composable
fun MiniPlayer(vm: AppViewModel, item: Item) {
    val exo = vm.livePlayer ?: return
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_STOP) exo.pause()
            if (e == Lifecycle.Event.ON_START) exo.playWhenReady = true
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val shape = RoundedCornerShape(14.dp)
    val compact = isCompact()
    val video: @Composable (Modifier) -> Unit = { mod ->
        Focusable(mod.aspectRatio(16f / 9f), shape, 1.03f, onClick = { vm.playLive(item, vm.miniSource) }) { _ ->
            Box(Modifier.fillMaxSize().clip(shape).background(Color.Black)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { c ->
                        PlayerView(c).apply {
                            useController = false
                            player = exo
                            isFocusable = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    onRelease = { it.player = null },
                )
            }
        }
    }
    if (compact) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            video(Modifier.width(170.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                RText(item.name, 16, C.text, bold = true, maxLines = 2)
                GlowButton(t("close"), height = 38.dp) { vm.stopMini() }
            }
        }
    } else {
        Column(
            Modifier.width(380.dp).fillMaxHeight().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            video(Modifier.fillMaxWidth())
            RText(item.name, 20, C.text, bold = true, maxLines = 2)
            BodyText(t("mini_hint"), 13, C.textDim)
            GlowButton(t("close")) { vm.stopMini() }
        }
    }
}
