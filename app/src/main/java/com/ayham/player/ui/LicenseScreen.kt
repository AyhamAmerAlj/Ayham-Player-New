package com.ayham.player.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import com.ayham.player.R
import com.ayham.player.data.LicenseManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** شاشة إدخال كود التفعيل. */
@Composable
fun LicenseScreen() {
    var code by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val tv = remember { isTvDevice(ctx) }
    LaunchedEffect(Unit) { while (true) { delay(15_000); if (LicenseManager.hasSaved) LicenseManager.retry() } }
    val go: () -> Unit = { scope.launch { LicenseManager.activate(code) } }
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF2A1260), C.bg), radius = 1300f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth(0.9f).padding(vertical = 16.dp)
                .then(if (tv) Modifier.fillMaxWidth(0.5f) else Modifier)
                .verticalScroll(rememberScrollState()).imePadding()
                .clip(RoundedCornerShape(24.dp)).background(Color(0x33000000)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(painterResource(R.drawable.ic_launcher), null, Modifier.size(84.dp).clip(RoundedCornerShape(20.dp)))
            RText("تفعيل Ayham Player", 26, C.text, bold = true)
            BodyText("اكتب كود التفعيل الذي استلمته. للشراء تواصل عبر واتساب: 07828244399", 14, C.textDim)
            AyhamField(code, { code = it.uppercase() }, "كود التفعيل", if (tv) Modifier.autoFocus() else Modifier, imeAction = ImeAction.Done) { go() }
            val err = LicenseManager.error
            if (err != null) BodyText(err, 14, C.danger)
            GlowButton(if (LicenseManager.busy) "جارٍ التحقق…" else "تفعيل", primary = true, modifier = Modifier.fillMaxWidth()) { if (!LicenseManager.busy) go() }
        }
    }
}
