package com.ayham.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel

/** نافذة التحديث: تظهر لما ينزل إصدار جديد، وتنزّل وتثبّت من داخل التطبيق. */
@Composable
fun UpdateDialog(vm: AppViewModel) {
    val info = vm.updInfo ?: return
    if (vm.updDismissed) return
    val st = vm.updState
    if (st != AppViewModel.UpdState.AVAILABLE && st != AppViewModel.UpdState.DOWNLOADING &&
        st != AppViewModel.UpdState.NEED_PERMISSION && st != AppViewModel.UpdState.ERROR
    ) return
    val downloading = st == AppViewModel.UpdState.DOWNLOADING
    BackHandler(enabled = !downloading) { vm.updDismissed = true }
    Box(
        Modifier.fillMaxSize().background(Color(0xCC000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(22.dp)
        Column(
            Modifier.widthIn(max = 520.dp).fillMaxWidth(0.9f).clip(shape).background(Color(0xFF120A26))
                .border(1.5.dp, C.primary, shape).padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RText(t("update_available"), 24, Color.White, bold = true)
            BodyText("${t("version")}: ${info.build}", 15, C.primaryLight, bold = true)
            if (info.notes.isNotBlank()) BodyText(info.notes.take(300), 13, C.textDim, maxLines = 5)
            when {
                downloading -> {
                    BodyText(t("downloading") + "  " + (vm.updProgress * 100).toInt() + "%", 16, C.text)
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color(0x33FFFFFF))) {
                        Box(Modifier.fillMaxWidth(vm.updProgress.coerceIn(0.02f, 1f)).fillMaxHeight().background(C.primary))
                    }
                }
                else -> {
                    if (st == AppViewModel.UpdState.NEED_PERMISSION) BodyText(t("update_perm"), 14, C.accent)
                    if (st == AppViewModel.UpdState.ERROR) BodyText(t("update_failed"), 14, C.danger)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlowButton(t("update_now"), primary = true, modifier = Modifier.autoFocus()) { vm.startUpdate() }
                        GlowButton(t("later")) { vm.updDismissed = true }
                    }
                }
            }
        }
    }
}
