package com.autocheck.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autocheck.app.ui.AppRoot
import com.autocheck.app.ui.MainViewModel
import com.autocheck.app.ui.theme.AutoCheckTheme
import com.autocheck.app.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val dark = vm.themeMode.isDark()

            // Значки в строке состояния и панели навигации должны соответствовать выбранной теме,
            // а не теме системы: иначе при ручном выборе они могут слиться с фоном.
            // Обе панели прозрачные: содержимое уходит под плавающую стеклянную панель вкладок.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose { }
            }

            AutoCheckTheme(darkTheme = dark) {
                AppRoot(vm)
            }
        }
    }
}
