package com.autocheck.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.autocheck.app.data.AutoClickState
import com.autocheck.app.data.SettingsStore

/**
 * Запускает проверку после перезагрузки устройства, если включён «Автозапуск» в настройках
 * («Работа в фоне») и добавлен хотя бы один аккаунт.
 *
 * Используется только BOOT_COMPLETED (после разблокировки): зашифрованное хранилище настроек
 * до первой разблокировки устройства недоступно.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_QUICKBOOT_POWERON) return

        try {
            val store = SettingsStore(context)
            if (!store.loadAutoStart()) return
            if (!store.load().isComplete) return
            AutoClickService.start(context.applicationContext)
        } catch (e: Exception) {
            AutoClickState.error("Не удалось выполнить автозапуск: ${e.message}")
        }
    }

    private companion object {
        /** Быстрая загрузка на части прошивок (HTC, Xiaomi и др.) вместо обычного BOOT_COMPLETED. */
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
