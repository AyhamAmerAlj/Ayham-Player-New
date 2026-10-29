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

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // التلفاز: أفقي دائمًا. الهاتف: يتبع دوران الجهاز.
        requestedOrientation = if (com.ayham.player.ui.isTvDevice(this))
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        LicenseManager.init(this)
        lifecycleScope.launch { LicenseManager.start(); LicenseManager.heartbeat() }
        setContent {
            AyhamTheme(vm.lang) {
                Root(vm)
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) LicenseManager.release()
        super.onDestroy()
    }
}

@Composable
private fun Root(vm: AppViewModel) {
    when (LicenseManager.state) {
        LicState.CHECKING -> { SplashScreen(); return }
        LicState.NEED_CODE -> { LicenseScreen(); return }
        LicState.OK -> {}
    }
    LaunchedEffect(Unit) { vm.start() }
    when (vm.phase) {
        Phase.SPLASH -> SplashScreen()
        Phase.LOGIN -> LoginScreen(vm)
        Phase.LOADING -> LoadingScreen(vm)
        Phase.READY -> Ready(vm)
    }
}

@Composable
private fun Ready(vm: AppViewModel) {
    val holder = rememberSaveableStateHolder()
    BackHandler(enabled = vm.stack.isNotEmpty() || vm.tab != 0) { vm.pop() }

    val top = vm.stack.lastOrNull()
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.checkUpdate(false) }
    Box(Modifier.fillMaxSize()) {
        when (top) {
            null -> holder.SaveableStateProvider("main") { MainScreen(vm) }
            is Screen.Detail -> holder.SaveableStateProvider("detail-${vm.stack.size}-${top.item.key}") {
                DetailScreen(vm, top.item)
            }
            is Screen.Player -> PlayerScreen(vm, top.req) { vm.pop() }
        }
        if (top !is Screen.Player) com.ayham.player.ui.UpdateDialog(vm)
    }
}
