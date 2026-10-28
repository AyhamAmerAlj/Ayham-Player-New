package com.ayham.player.ui

import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.ayham.player.data.LoginServer
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

internal fun qrBitmap(text: String, size: Int = 400): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
    val px = IntArray(size * size)
    for (y in 0 until size) for (x in 0 until size) {
        px[y * size + x] = if (m.get(x, y)) AColor.BLACK else AColor.WHITE
    }
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
}

/** رمز QR يظهر مباشرة في صفحة الدخول: امسحه بالهاتف وأرسل بيانات الاشتراك للشاشة. */
@Composable
fun QrLoginCard(onCreds: (String, String, String) -> Unit) {
    val creds by rememberUpdatedState(onCreds)
    var url by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val main = Handler(Looper.getMainLooper())
        val srv = LoginServer(onCreds = { s, u, p -> main.post { creds(s, u, p) } })
        val ip = LoginServer.localIp()
        if (ip != null && srv.start()) url = "http://$ip:${srv.port}" else failed = true
        onDispose { srv.stop() }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val u = url
        if (u != null) {
            val bmp = remember(u) { qrBitmap(u) }
            Box(Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).padding(10.dp)) {
                Image(bmp.asImageBitmap(), null, Modifier.size(170.dp))
            }
            RText(t("qr_scan"), 18, C.text, bold = true)
            BodyText(u, 13, C.primaryLight)
        } else if (failed) {
            RText(t("qr_fail"), 16, C.danger)
        }
    }
}
