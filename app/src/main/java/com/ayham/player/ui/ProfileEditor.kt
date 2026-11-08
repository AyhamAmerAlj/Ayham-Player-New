@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ayham.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ayham.player.AppViewModel
import com.ayham.player.data.Pins
import com.ayham.player.data.Profile

@Composable
private fun SectionTitle(text: String) = RText(text, 20, C.primaryLight, bold = true)

/** دائرة/مربع اختيار بلون (للأنماط والألوان والخلفيات). */
@Composable
private fun Swatch(brush: Brush, selected: Boolean, w: androidx.compose.ui.unit.Dp = 46.dp, h: androidx.compose.ui.unit.Dp = 46.dp, circle: Boolean = true, onClick: () -> Unit) {
    val shape = if (circle) CircleShape else RoundedCornerShape(12.dp)
    Focusable(shape = shape, focusScale = 1.15f, onClick = onClick) { _ ->
        Box(
            Modifier.size(w, h).clip(shape).background(brush)
                .then(if (selected) Modifier.border(3.dp, Color.White, shape) else Modifier),
        )
    }
}

/** محرّر الملف الشخصي: كل الخيارات مع معاينة مباشرة قبل الحفظ. */
@Composable
fun ProfileEditor(vm: AppViewModel) {
    val ar = vm.lang == "ar"
    val compact = isCompact()
    val ctx = LocalContext.current
    val tv = remember { isTvDevice(ctx) }
    val original = remember { vm.editTarget }
    var d by remember { mutableStateOf(original ?: Profile(id = vm.newProfileId(), name = "", type = "PERSONAL", avatar = 5, style = 1, bg = 1, frame = 1)) }
    var photo by remember { mutableStateOf<ByteArray?>(null) }
    var showQr by remember { mutableStateOf(false) }
    var pinMode by remember { mutableIntStateOf(0) } // 0 لا شيء، 1 إنشاء رمز، 2 تحقق ثم تغيير، 3 تحقق ثم إزالة
    var msg by remember { mutableStateOf<String?>(null) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes != null) { photo = bytes; d = d.copy(avatar = -1) }
        }
    }
    BackHandler(enabled = pinMode == 0) { vm.profileBack() }

    fun save() {
        val name = d.name.trim()
        if (name.isBlank()) { msg = if (ar) "اكتب اسم الملف أولاً" else "Enter a profile name"; return }
        vm.saveProfile(d.copy(name = name), photo)
        vm.profileBack()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ProfileBackdrop(d, Modifier.fillMaxSize().alpha(0.7f))
        when (pinMode) {
            1 -> PinSetup(vm, ProfileLook.c1(d), onSet = { c -> d = d.copy(pin = Pins.hash(d.id, c)); pinMode = 0 }, onCancel = { pinMode = 0 })
            2, 3 -> PinGate(vm, d, if (ar) "رمز هذا الملف" else "This profile's code",
                onOk = { if (pinMode == 2) pinMode = 1 else { d = d.copy(pin = ""); pinMode = 0 } }, onCancel = { pinMode = 0 })
            else -> {
                val preview: @Composable (Modifier) -> Unit = { m ->
                    Column(m, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                        RText(if (ar) "معاينة مباشرة" else "Live preview", 14, C.accent, bold = true)
                        ProfileAvatarView(vm, d, if (compact) 110.dp else 170.dp, glow = true, photoBytes = photo)
                        RText(d.name.ifBlank { if (ar) "اسم الملف" else "Profile name" }, if (compact) 24 else 34, Color.White, bold = true, maxLines = 1)
                        if (d.tagline.isNotBlank()) RText(d.tagline, 16, C.textDim, maxLines = 1)
                        val st = ProfileLook.style(d)
                        RText((if (ar) st.ar else st.en) + (if (d.pin.isNotEmpty()) "  🔒" else "") + (if (d.type == "KIDS") "  🧸" else ""), 14, ProfileLook.c1(d), bold = true, maxLines = 1)
                    }
                }
                val form: @Composable (Modifier) -> Unit = { m ->
                    Column(m.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        RText(if (original == null) (if (ar) "ملف شخصي جديد" else "New profile") else (if (ar) "تعديل الملف" else "Edit profile"), 28, Color.White, bold = true)

                        SectionTitle(if (ar) "الاسم واللقب" else "Name & tagline")
                        AyhamField(d.name, { d = d.copy(name = it.take(14)) }, if (ar) "اسم الملف" else "Profile name", Modifier.fillMaxWidth())
                        AyhamField(d.tagline, { d = d.copy(tagline = it.take(24)) }, if (ar) "لقب أو وصف قصير (اختياري)" else "Short tagline (optional)", Modifier.fillMaxWidth())

                        if (d.type != "MAIN") {
                            SectionTitle(if (ar) "نوع الملف" else "Profile type")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip(if (ar) "شخصي" else "Personal", d.type == "PERSONAL") { d = d.copy(type = "PERSONAL") }
                                Chip(if (ar) "🧸 أطفال" else "🧸 Kids", d.type == "KIDS") { d = d.copy(type = "KIDS", hidden = false) }
                            }
                            if (d.type == "KIDS") {
                                SectionTitle(if (ar) "تقييد المحتوى حسب العمر" else "Age restriction")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Chip(if (ar) "7+ أطفال وكرتون فقط" else "7+ kids & cartoons only", d.kidsLevel <= 7) { d = d.copy(kidsLevel = 7) }
                                    Chip(if (ar) "12+ حجب الكبار والرعب" else "12+ no adult/horror", d.kidsLevel == 12) { d = d.copy(kidsLevel = 12) }
                                    Chip(if (ar) "16+ حجب فئات الكبار" else "16+ no adult categories", d.kidsLevel >= 16) { d = d.copy(kidsLevel = 16) }
                                }
                                BodyText(if (ar) "التقييد يعتمد على أسماء أقسام قائمتك (مثل أطفال وكرتون وأفلام الكبار). وملف الأطفال بدون إعدادات." else "Restriction uses your playlist's category names. Kids profiles have no settings.", 13, C.textDim)
                            }
                        }

                        SectionTitle(if (ar) "النمط" else "Style")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileLook.styles.forEachIndexed { i, s ->
                                Chip((if (ar) s.ar else s.en), d.style == i && d.color < 0) { d = d.copy(style = i, color = -1, bg = s.bg) }
                            }
                        }

                        SectionTitle(if (ar) "الصورة" else "Avatar")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProfileLook.emojis.forEachIndexed { i, e ->
                                Focusable(shape = CircleShape, focusScale = 1.15f, onClick = { d = d.copy(avatar = i) }) { _ ->
                                    Box(
                                        Modifier.size(48.dp).clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(ProfileLook.c1(d), ProfileLook.c2(d))))
                                            .then(if (d.avatar == i) Modifier.border(3.dp, Color.White, CircleShape) else Modifier),
                                        contentAlignment = Alignment.Center,
                                    ) { RText(e, 24, Color.White) }
                                }
                            }
                        }
                        GlowButton(if (ar) "📷 صورة من جهازك" else "📷 Photo from device") {
                            if (tv) showQr = !showQr else picker.launch(arrayOf("image/*"))
                        }
                        if (showQr) PhotoQrCard(vm, onPhoto = { photo = it; d = d.copy(avatar = -1) }) { showQr = false }

                        SectionTitle(if (ar) "اللون" else "Color")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Chip(if (ar) "حسب النمط" else "Style color", d.color < 0) { d = d.copy(color = -1) }
                            ProfileLook.palette.forEachIndexed { i, c ->
                                Swatch(Brush.linearGradient(listOf(c, c)), d.color == i) { d = d.copy(color = i) }
                            }
                        }

                        SectionTitle(if (ar) "الخلفية" else "Background")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProfileLook.bgs.forEachIndexed { i, b ->
                                Swatch(Brush.linearGradient(listOf(b.c, b.a, b.b)), d.bg == i, 72.dp, 44.dp, circle = false) { d = d.copy(bg = i) }
                            }
                        }

                        SectionTitle(if (ar) "إطار الصورة" else "Avatar frame")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProfileLook.frames.forEachIndexed { i, f -> Chip(if (ar) f.first else f.second, d.frame == i) { d = d.copy(frame = i) } }
                        }

                        SectionTitle(if (ar) "الحماية والخصوصية" else "Privacy")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (d.pin.isEmpty()) {
                                Chip(if (ar) "🔒 إضافة رمز (4 أرقام)" else "🔒 Add 4-digit code", false) { pinMode = 1 }
                            } else {
                                Chip(if (ar) "🔒 تغيير الرمز" else "🔒 Change code", false) { pinMode = 2 }
                                Chip(if (ar) "إزالة الرمز" else "Remove code", false) { pinMode = 3 }
                            }
                            if (d.type != "MAIN" && d.type != "KIDS") {
                                Chip(if (ar) "إخفاء من شاشة الاختيار: " + (if (d.hidden) "نعم" else "لا") else "Hide from picker: " + (if (d.hidden) "Yes" else "No"), d.hidden) { d = d.copy(hidden = !d.hidden) }
                            }
                        }
                        if (d.hidden) BodyText(if (ar) "الملف المخفي يظهر فقط من (إدارة الملفات)." else "Hidden profiles appear only in Manage profiles.", 13, C.textDim)

                        msg?.let { RText(it, 16, C.danger, bold = true) }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlowButton(if (ar) "حفظ" else "Save", primary = true) { save() }
                            GlowButton(if (ar) "إلغاء" else "Cancel") { vm.profileBack() }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
                if (compact) {
                    Column(Modifier.fillMaxSize()) {
                        preview(Modifier.fillMaxWidth().padding(top = 12.dp))
                        form(Modifier.weight(1f).fillMaxWidth())
                    }
                } else {
                    Row(Modifier.fillMaxSize()) {
                        preview(Modifier.weight(0.38f).fillMaxSize())
                        form(Modifier.weight(0.62f).fillMaxSize())
                    }
                }
            }
        }
    }
}
