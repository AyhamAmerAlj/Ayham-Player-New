@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ayham.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Profile

/** لوحة إدارة الملفات: إضافة، تعديل، حذف، ترتيب، نسخ إعدادات، تبديل سريع، رئيسي وافتراضي. */
@Composable
fun ProfileManage(vm: AppViewModel) {
    val ar = vm.lang == "ar"
    val compact = isCompact()
    var del by remember { mutableStateOf<Profile?>(null) }
    var copyFrom by remember { mutableStateOf<Profile?>(null) }
    var pinFor by remember { mutableStateOf<Profile?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = del == null && copyFrom == null && pinFor == null) { vm.profileBack() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ProfileBackdrop(vm.activeProfile ?: vm.profiles.first(), Modifier.fillMaxSize().alpha(0.45f))
        val pf = pinFor
        val dl = del
        val cf = copyFrom
        when {
            pf != null -> PinGate(vm, pf, (if (ar) "ملف " else "Profile ") + pf.name, onOk = { pinFor = null; vm.enterProfile(pf) }, onCancel = { pinFor = null })
            dl != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                BackHandler { del = null }
                ProfileAvatarView(vm, dl, 96.dp)
                RText(if (ar) "حذف «${dl.name}» نهائياً؟" else "Delete \"${dl.name}\" permanently?", 26, Color.White, bold = true, maxLines = 2)
                RText(if (ar) "سيُمسح سجل مشاهدته ومفضلته وإعداداته ولا يمكن التراجع." else "Its history, favorites and settings will be erased. This can't be undone.", 16, C.textDim, maxLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlowButton(if (ar) "إلغاء" else "Cancel", Modifier.autoFocus()) { del = null }
                    GlowButton(if (ar) "نعم، احذف" else "Yes, delete", primary = true) { vm.deleteProfile(dl.id); del = null; note = if (ar) "تم الحذف" else "Deleted" }
                }
            }
            cf != null -> Column(
                Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            ) {
                BackHandler { copyFrom = null }
                RText(if (ar) "نسخ إعدادات «${cf.name}» إلى:" else "Copy \"${cf.name}\" settings to:", 24, Color.White, bold = true, maxLines = 2)
                BodyText(if (ar) "تُنسخ الإعدادات فقط (الثيم والمشغل والترجمة والواجهة)، لا المشاهدة ولا المفضلة." else "Settings only (theme, player, subtitles, UI). Not history or favorites.", 14, C.textDim)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.profiles.filter { it.id != cf.id }.forEach { t ->
                        Chip(t.name, false) { vm.copyProfileSettings(cf.id, t.id); copyFrom = null; note = if (ar) "تم نسخ الإعدادات إلى ${t.name}" else "Copied to ${t.name}" }
                    }
                }
                GlowButton(if (ar) "إلغاء" else "Cancel") { copyFrom = null }
            }
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (compact) 14.dp else 48.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RText(if (ar) "إدارة الملفات الشخصية" else "Manage profiles", if (compact) 26 else 34, Color.White, bold = true)
                note?.let { RText(it, 16, C.accent, bold = true) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlowButton(if (ar) "+ إضافة ملف" else "+ Add profile", Modifier.autoFocus(), primary = true) {
                        if (vm.profiles.size < 8) vm.openEditor(null) else note = if (ar) "الحد الأقصى 8 ملفات" else "Max 8 profiles"
                    }
                    GlowButton(if (ar) "رجوع" else "Back") { vm.profileBack() }
                }
                vm.profiles.forEachIndexed { i, p ->
                    Column(
                        Modifier.fillMaxWidth().widthIn(max = 900.dp).clip(RoundedCornerShape(20.dp)).background(tc(0x26FFFFFF)).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatarView(vm, p, 64.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                RText(p.name, 22, Color.White, bold = true, maxLines = 1)
                                val tags = listOfNotNull(
                                    if (p.type == "MAIN") (if (ar) "★ رئيسي" else "★ Main") else null,
                                    if (p.type == "KIDS") (if (ar) "🧸 أطفال ${p.kidsLevel}+" else "🧸 Kids ${p.kidsLevel}+") else null,
                                    if (vm.defaultProfileId() == p.id) (if (ar) "افتراضي" else "Default") else null,
                                    if (p.pin.isNotEmpty()) "🔒" else null,
                                    if (p.hidden) (if (ar) "مخفي" else "Hidden") else null,
                                    if (vm.activeProfile?.id == p.id) (if (ar) "الحالي" else "Current") else null,
                                )
                                if (tags.isNotEmpty()) BodyText(tags.joinToString("  ·  "), 14, ProfileLook.c1(p))
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (vm.activeProfile?.id != p.id) Chip(if (ar) "تبديل" else "Switch", false) { if (p.pin.isNotEmpty()) pinFor = p else vm.enterProfile(p) }
                            Chip(if (ar) "تعديل" else "Edit", false) { vm.openEditor(p) }
                            if (i > 0) Chip("↑", false) { vm.moveProfile(p.id, -1) }
                            if (i < vm.profiles.lastIndex) Chip("↓", false) { vm.moveProfile(p.id, 1) }
                            Chip(if (vm.defaultProfileId() == p.id) (if (ar) "★ افتراضي عند التشغيل" else "★ Default at launch") else (if (ar) "جعله افتراضياً" else "Set as default"), vm.defaultProfileId() == p.id) { vm.setDefaultProfile(p.id) }
                            if (p.type == "PERSONAL") Chip(if (ar) "جعله الرئيسي" else "Make main", false) { vm.setMainProfile(p.id); note = if (ar) "${p.name} هو الرئيسي الآن" else "${p.name} is now main" }
                            if (vm.profiles.size > 1) Chip(if (ar) "نسخ إعداداته إلى…" else "Copy settings to…", false) { copyFrom = p }
                            if (p.type != "MAIN" && vm.activeProfile?.id != p.id) Chip(if (ar) "حذف" else "Delete", false) { del = p }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
