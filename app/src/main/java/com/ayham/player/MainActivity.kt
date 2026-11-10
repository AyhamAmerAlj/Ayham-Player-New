package com.ayham.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.LaunchedEffect
import com.ayham.player.data.LicState
import com.ayham.player.data.LicenseManager
import kotlinx.coroutines.launch
import com.ayham.player.ui.LicenseScreen
import com.ayham.player.ui.AyhamTheme
import com.ayham.player.ui.DetailScreen
import com.ayham.player.ui.LoadingScreen
import com.ayham.player.ui.LoginScreen
import com.ayham.player.ui.MainScreen
import com.ayham.player.ui.PlayerScreen
import com.ayham.player.ui.SplashScreen
import com.ayham.player.ui.ThemeState
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // التلفاز: أفقي دائمًا. الهاتف: يتبع دوران الجهاز.
        requestedOrientation = if (com.ayham.player.ui.isTvDevice(this))
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        // الثيم المحفوظ يُطبّق قبل أول رسم للواجهة
        com.ayham.player.ui.AppIcon.load(com.ayham.player.data.Store(this).appIcon)
        ThemeState.select(com.ayham.player.data.Store(this).appTheme)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(ThemeState.palette.bg.toArgb()))
        LicenseManager.init(this)
        lifecycleScope.launch { LicenseManager.start(); LicenseManager.heartbeat() }
        setContent {
            AyhamTheme(vm.lang) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.ayham.player.ui.LocalProgress provides { k -> vm.progressOf(k)?.let { (p, d) -> if (d > 0) (p.toFloat() / d).coerceIn(0f, 1f) else null } },
                ) { Root(vm) }
            }
        }
    }

    // أي ضغطة أو لمسة تصفّر عدّاد الخمول، وتغلق شاشة التوقف لو كانت ظاهرة
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (vm.saverOn) { if (event.action == android.view.KeyEvent.ACTION_DOWN) vm.userActive(); return true }
        vm.lastActive = android.os.SystemClock.uptimeMillis()
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        if (vm.saverOn) { if (ev.action == android.view.MotionEvent.ACTION_DOWN) vm.userActive(); return true }
        vm.lastActive = android.os.SystemClock.uptimeMillis()
        return super.dispatchTouchEvent(ev)
    }

    override fun onStop() {
        vm.flushSync()
        super.onStop()
    }

    override fun onDestroy() {
        if (isFinishing) LicenseManager.release()
        super.onDestroy()
    }
}

@Composable
private fun Root(vm: AppViewModel) {
    Box(Modifier.fillMaxSize()) {
        RootInner(vm)
        // ترحيب سينمائي بعد اختيار الملف ثم ينسحب إلى الرئيسية
        vm.welcome?.let { w -> com.ayham.player.ui.WelcomeOverlay(vm, w) { vm.welcome = null } }
    }
}

@Composable
private fun RootInner(vm: AppViewModel) {
    val lic = LicenseManager.state
    if (lic == LicState.NEED_CODE) { LicenseScreen(); return }
    LaunchedEffect(lic) { if (lic == LicState.OK) vm.start() }
    // موضع استدعاء واحد للافتتاحية حتى لا تُعاد الحركة عند تبدّل الحالات
    if (lic == LicState.CHECKING || vm.phase == Phase.SPLASH || (vm.phase == Phase.LOADING && vm.bootSplash)) {
        SplashScreen()
        return
    }
    // شاشة اختيار الملف الشخصي (عند التشغيل أو التبديل أو الإدارة)
    if (!vm.profileReady || vm.profileNav.isNotEmpty()) {
        com.ayham.player.ui.ProfileFlow(vm)
        return
    }
    when (vm.phase) {
        Phase.LOGIN -> LoginScreen(vm)
        Phase.LOADING -> LoadingScreen(vm)
        Phase.READY -> Ready(vm)
        else -> {}
    }
}

@Composable
private fun Ready(vm: AppViewModel) {
    val holder = rememberSaveableStateHolder()
    // زر الرجوع: يرجع خطوة، ولا يخرج من التطبيق إلا من الرئيسية
    BackHandler(enabled = vm.stack.isNotEmpty() || vm.tab != 0) { vm.pop() }

    val top = vm.stack.lastOrNull()
    // الانتقال لشاشة أو تبويب آخر يغلق الكيبورد حتى لا يبقى فوق الشاشة الجديدة
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.checkUpdate(false) }
    // ما الجديد؟: جلب مرة واحدة بعد الفتح بخيط خلفي (لا يؤثر على السرعة)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        vm.loadNews(true)
    }
    // إعلان الرئيسية: عند الفتح ثم كل 15 دقيقة (طلب خفيف واحد)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        while (true) {
            vm.loadAnn()
            kotlinx.coroutines.delay(15 * 60 * 1000L)
        }
    }
    // مزامنة بيانات المشاهدة عند الفتح ثم كل 3 دقائق
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            vm.syncOnce()
            kotlinx.coroutines.delay(3 * 60 * 1000L)
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { kotlinx.coroutines.delay(1500); vm.maybeResumeLive() }
    // الدخول أو الخروج من شاشة (مشغّل/تفاصيل) يصفّر عدّاد الخمول حتى لا تظهر شاشة التوقف فور الرجوع
    androidx.compose.runtime.LaunchedEffect(vm.stack.size) { vm.lastActive = android.os.SystemClock.uptimeMillis() }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4000)
            val idle = android.os.SystemClock.uptimeMillis() - vm.lastActive
            if (vm.saverMin > 0 && !vm.saverOn && vm.stack.isEmpty() && !vm.multiOpen && vm.miniItem == null && idle > vm.saverMin * 60_000L) vm.saverOn = true
        }
    }
    Box(Modifier.fillMaxSize()) {
        when (top) {
            null -> if (vm.multiOpen) com.ayham.player.ui.MultiView(vm) else holder.SaveableStateProvider("main") { MainScreen(vm) }
            is Screen.Detail -> holder.SaveableStateProvider("detail-${vm.stack.size}-${top.item.key}") {
                DetailScreen(vm, top.item)
            }
            is Screen.Player -> PlayerScreen(vm, top.req) { vm.pop() }
        }
        if (vm.saverOn) com.ayham.player.ui.Screensaver(vm)
        if (top !is Screen.Player) com.ayham.player.ui.UpdateDialog(vm)
        if (top !is Screen.Player && (vm.updInfo == null || vm.updDismissed)) com.ayham.player.ui.NewsDialog(vm)
    }
}
