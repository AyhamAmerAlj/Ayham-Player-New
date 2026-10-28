package com.ayham.player.ui

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** هل الجهاز تلفاز؟ (الباقي يُعامل كهاتف/تابلت باللمس) */
fun isTvDevice(ctx: Context): Boolean {
    val um = ctx.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return um?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        ctx.packageManager.hasSystemFeature("android.software.leanback")
}

/** شاشة ضيقة (هاتف بالطول أو هاتف صغير): نستعمل شريط تنقل سفلي وقوائم أفقية. */
@Composable
fun isCompact(): Boolean = LocalConfiguration.current.screenWidthDp < 640
