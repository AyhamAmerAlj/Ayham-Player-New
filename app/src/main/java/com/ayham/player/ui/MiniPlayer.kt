@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ayham.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
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

/** معاينة سينمائية مصغّرة لشاشة القنوات للتلفاز: بحجم ثابت (لا تأخذ مساحة القنوات) مع توهج بنفسجي وظهور ناعم. */
@Composable
fun CinemaMini(vm: AppViewModel, item: Item, w: androidx.compose.ui.unit.Dp) {
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
    val shape = RoundedCornerShape(18.dp)
    val vis = androidx.compose.runtime.remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
    androidx.compose.animation.AnimatedVisibility(
        visibleState = vis,
        enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(450)) + androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(450), initialScale = 0.85f),
    ) {
        Column(Modifier.width(w), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Focusable(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).androidx_shadow(shape),
                shape, 1.04f, onClick = { vm.playLive(item, vm.miniSource) },
            ) { f ->
                Box(
                    Modifier.fillMaxSize().clip(shape).background(Color.Black)
                        .androidx_border(if (f) 2.dp else 1.dp, if (f) tc(0xFFC4B5FD) else tc(0x778B5CF6), shape),
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { c ->
                            PlayerView(c).apply {
                                useController = false
                                player = exo
                                isFocusable = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                setShutterBackgroundColor(android.graphics.Color.BLACK)
                            }
                        },
                        onRelease = { it.player = null },
                    )
                    Box(
                        Modifier.fillMaxWidth().align(androidx.compose.ui.Alignment.BottomCenter)
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD05020B))))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) { RText(item.name, 13, Color.White, bold = true, maxLines = 1) }
                    Box(
                        Modifier.align(androidx.compose.ui.Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(5.dp)).background(C.danger).padding(horizontal = 6.dp),
                    ) { RText("LIVE", 9, Color.White, bold = true) }
                }
            }
            Focusable(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), 1.04f, onClick = { vm.stopMini() }) { f ->
                androidx.compose.foundation.layout.Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (f) tc(0x558B5CF6) else tc(0x33FFFFFF)).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.width(14.dp))
                    RText(" " + t("close"), 13, Color.White, bold = true, maxLines = 1)
                }
            }
        }
    }
}

private fun Modifier.androidx_shadow(shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.shadow(18.dp, shape, clip = false, ambientColor = Color(0xFF8B5CF6), spotColor = Color(0xFF8B5CF6))

private fun Modifier.androidx_border(w: androidx.compose.ui.unit.Dp, c: Color, shape: androidx.compose.ui.graphics.Shape): Modifier =
    this.border(w, c, shape)
