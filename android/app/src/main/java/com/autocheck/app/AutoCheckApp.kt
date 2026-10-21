package com.autocheck.app

import android.app.Application
import com.autocheck.app.data.AutoClickState

class AutoCheckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Журнал восстанавливается до запуска Activity, сервиса или приёмника загрузки
        AutoClickState.init(this)
    }
}
