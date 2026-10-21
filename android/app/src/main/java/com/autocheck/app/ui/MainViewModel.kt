package com.autocheck.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.autocheck.app.data.Account
import com.autocheck.app.data.AccountType
import com.autocheck.app.data.AutoClickState
import com.autocheck.app.data.Credentials
import com.autocheck.app.data.LogPrefs
import com.autocheck.app.data.NotificationPrefs
import com.autocheck.app.data.SettingsStore
import com.autocheck.app.data.ThemeMode
import com.autocheck.app.data.TrafficRoute
import com.autocheck.app.service.AutoClickService

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val initial = store.load()

    var saved by mutableStateOf(initial)
        private set

    /**
     * Редактируемый список аккаунтов. Каждое изменение сразу сохраняется в хранилище,
     * при этом сам список в поле ввода остаётся как есть (с пробелами и пустыми карточками),
     * чтобы не мешать набору текста.
     */
    var accounts by mutableStateOf(initial.accounts)
        private set

    var timeout by mutableIntStateOf(initial.timeoutSec)
        private set

    var route by mutableStateOf(initial.route)
        private set

    /** Настройки уведомлений применяются и сохраняются сразу. */
    var notifications by mutableStateOf(store.loadNotifications())
        private set

    /** Настройки журнала (ответы сервера, лимит записей) применяются и сохраняются сразу. */
    var logPrefs by mutableStateOf(store.loadLogPrefs())
        private set

    /** Тема применяется и сохраняется сразу. */
    var themeMode by mutableStateOf(store.loadThemeMode())
        private set

    /** Запускать проверку автоматически после перезагрузки устройства. */
    var autoStart by mutableStateOf(store.loadAutoStart())
        private set

    init {
        AutoClickState.setNotificationPrefs(notifications)
    }

    /** Аккаунты в том виде, в котором они сохраняются: без лишних пробелов и пустых записей. */
    private fun cleanedAccounts() = accounts.map { it.normalized() }.filterNot { it.isBlank }

    val canStart: Boolean
        get() = accounts.any { it.normalized().isComplete }

    fun addAccount(type: AccountType) {
        accounts = accounts + Account(type = type)
        persist()
    }

    fun updateAccount(id: String, transform: (Account) -> Account) {
        accounts = accounts.map { if (it.id == id) transform(it) else it }
        persist()
    }

    fun removeAccount(id: String) {
        accounts = accounts.filterNot { it.id == id }
        persist()
    }

    fun updateTimeout(seconds: Int) {
        if (seconds == timeout) return
        timeout = seconds
        persist()
    }

    fun updateRoute(value: TrafficRoute) {
        if (value == route) return
        route = value
        persist()
    }

    /** Записывает аккаунты, интервал и маршрут в зашифрованное хранилище. */
    private fun persist() {
        saved = Credentials(cleanedAccounts(), timeout, route)
        store.save(saved)
    }

    fun updateNotifications(transform: (NotificationPrefs) -> NotificationPrefs) {
        notifications = transform(notifications)
        store.saveNotifications(notifications)
        AutoClickState.setNotificationPrefs(notifications)
    }

    fun updateLogPrefs(transform: (LogPrefs) -> LogPrefs) {
        val updated = transform(logPrefs)
        if (updated == logPrefs) return
        logPrefs = updated
        store.saveLogPrefs(updated)
        AutoClickState.setLogPrefs(updated)
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        store.saveThemeMode(mode)
    }

    fun updateAutoStart(enabled: Boolean) {
        autoStart = enabled
        store.saveAutoStart(enabled)
    }

    fun start() {
        AutoClickService.start(getApplication<Application>())
    }

    fun stop() = AutoClickService.stop(getApplication<Application>())

    fun clearLogs() = AutoClickState.clearLogs()
}
