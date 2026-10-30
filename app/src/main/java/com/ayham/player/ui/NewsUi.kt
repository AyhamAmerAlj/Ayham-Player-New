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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.NewsEntry

/** نافذة "ما الجديد؟": تظهر مرة واحدة لكل تحديث، وتُفتح كذلك من الإعدادات لعرض السجل. */
@Composable
fun NewsDialog(vm: AppViewModel) {
    val list = vm.newsShow ?: return
    if (list.isEmpty()) return
    val en = vm.lang == "en"
    BackHandler { vm.dismissNews() }
    Box(
        Modifier.fillMaxSize().background(Color(0xD9000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth(0.92f).heightIn(max = 640.dp)
                .clip(shape).background(Color(0xFF120A26)).border(1.5.dp, C.primary, shape).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            RText(t("whats_new"), 28, Color.White, bold = true)
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                list.forEach { e -> NewsBlock(e, en) }
            }
            GlowButton(t("lets_go"), primary = true, modifier = Modifier.fillMaxWidth().autoFocus()) { vm.dismissNews() }
        }
    }
}

@Composable
private fun NewsBlock(e: NewsEntry, en: Boolean) {
    val title = if (en) e.titleEn.ifBlank { e.title } else e.title
    val desc = if (en) e.descEn.ifBlank { e.desc } else e.desc
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BodyText("${t("version")} ${e.version}  •  ${e.date}", 14, C.primaryLight, bold = true)
        RText(title, 22, Color.White, bold = true)
        if (desc.isNotBlank()) BodyText(desc, 15, C.textDim)
        e.items.forEach { f ->
            val ft = if (en) f.titleEn.ifBlank { f.title } else f.title
            val fd = if (en) f.descEn.ifBlank { f.desc } else f.desc
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1C1033)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(C.surfaceHi), contentAlignment = Alignment.Center) {
                    RText(f.icon.ifBlank { "✨" }, 22, Color.White)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (ft.isNotBlank()) BodyText(ft, 16, Color.White, bold = true)
                    if (fd.isNotBlank()) BodyText(fd, 13, C.textDim)
                    if (f.image.isNotBlank()) Img(
                        f.image, Modifier.fillMaxWidth().heightIn(max = 160.dp).clip(RoundedCornerShape(12.dp)),
                        ContentScale.Crop, w = 600, h = 320,
                    )
                }
            }
        }
    }
}
