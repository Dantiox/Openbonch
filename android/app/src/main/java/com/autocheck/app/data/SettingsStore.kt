package com.autocheck.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Замена `options.txt`: аккаунты и интервал хранятся в зашифрованных SharedPreferences. */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun load() = Credentials(
        accounts = loadAccounts(),
        timeoutSec = prefs.getInt(KEY_TIMEOUT, DEFAULT_TIMEOUT),
        route = runCatching { TrafficRoute.valueOf(prefs.getString(KEY_ROUTE, null).orEmpty()) }
            .getOrDefault(TrafficRoute.SYSTEM),
    )

    fun save(credentials: Credentials) = prefs.edit {
        putString(KEY_ACCOUNTS, accountsToJson(credentials.accounts))
        putInt(KEY_TIMEOUT, credentials.timeoutSec)
        putString(KEY_ROUTE, credentials.route.name)
    }

    /** Только список аккаунтов. Движок перечитывает его каждый цикл — аналог перезагрузки файла аккаунтов в Python. */
    fun loadAccounts(): List<Account> {
        val raw = prefs.getString(KEY_ACCOUNTS, null)
        if (raw != null) return runCatching { accountsFromJson(raw) }.getOrDefault(emptyList())

        // Миграция с версии с одним аккаунтом: логин и пароль лежали в отдельных ключах
        val login = prefs.getString(KEY_LEGACY_LOGIN, "").orEmpty()
        val password = prefs.getString(KEY_LEGACY_PASSWORD, "").orEmpty()
        if (login.isBlank() && password.isEmpty()) return emptyList()

        val migrated = listOf(Account(id = LEGACY_ACCOUNT_ID, login = login.trim(), password = password))
        prefs.edit {
            putString(KEY_ACCOUNTS, accountsToJson(migrated))
            remove(KEY_LEGACY_LOGIN)
            remove(KEY_LEGACY_PASSWORD)
        }
        return migrated
    }

    private fun accountsToJson(accounts: List<Account>): String {
        val array = JSONArray()
        accounts.forEach { a ->
            array.put(
                JSONObject()
                    .put("id", a.id)
                    .put("type", a.type.name)
                    .put("login", a.login)
                    .put("password", a.password)
                    .put("token", a.token)
            )
        }
        return array.toString()
    }

    private fun accountsFromJson(raw: String): List<Account> {
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Account(
                id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                type = runCatching { AccountType.valueOf(o.optString("type")) }.getOrDefault(AccountType.PASSWORD),
                login = o.optString("login"),
                password = o.optString("password"),
                token = o.optString("token"),
            )
        }
    }

    fun loadNotifications(): NotificationPrefs {
        val d = NotificationPrefs()
        return NotificationPrefs(
            lessonStarted = prefs.getBoolean(KEY_N_LESSON, d.lessonStarted),
            errors = prefs.getBoolean(KEY_N_ERRORS, d.errors),
            warnings = prefs.getBoolean(KEY_N_WARNINGS, d.warnings),
            checks = prefs.getBoolean(KEY_N_CHECKS, d.checks),
            service = prefs.getBoolean(KEY_N_SERVICE, d.service),
            controlWhenStopped = prefs.getBoolean(KEY_N_CONTROL, d.controlWhenStopped),
        )
    }

    fun saveNotifications(value: NotificationPrefs) = prefs.edit {
        putBoolean(KEY_N_LESSON, value.lessonStarted)
        putBoolean(KEY_N_ERRORS, value.errors)
        putBoolean(KEY_N_WARNINGS, value.warnings)
        putBoolean(KEY_N_CHECKS, value.checks)
        putBoolean(KEY_N_SERVICE, value.service)
        putBoolean(KEY_N_CONTROL, value.controlWhenStopped)
    }

    fun loadThemeMode(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null).orEmpty()) }
            .getOrDefault(ThemeMode.SYSTEM)

    fun saveThemeMode(mode: ThemeMode) = prefs.edit { putString(KEY_THEME, mode.name) }

    fun loadLogPrefs(): LogPrefs {
        val d = LogPrefs()
        val kinds = prefs.getString(KEY_LOG_KINDS, null)
            ?.split(',')
            ?.mapNotNull { runCatching { ResponseKind.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: d.kinds
        return LogPrefs(
            saveResponses = prefs.getBoolean(KEY_LOG_SAVE, d.saveResponses),
            kinds = kinds,
            maxEntries = prefs.getInt(KEY_LOG_MAX, d.maxEntries)
                .coerceIn(LogPrefs.MIN_ENTRIES, LogPrefs.MAX_ENTRIES),
            downloadDir = prefs.getString(KEY_LOG_DIR, null)?.takeIf { it.isNotBlank() },
        )
    }

    fun saveLogPrefs(value: LogPrefs) = prefs.edit {
        putBoolean(KEY_LOG_SAVE, value.saveResponses)
        putString(KEY_LOG_KINDS, value.kinds.joinToString(",") { it.name })
        putInt(KEY_LOG_MAX, value.maxEntries)
        if (value.downloadDir == null) remove(KEY_LOG_DIR) else putString(KEY_LOG_DIR, value.downloadDir)
    }

    /** Запускать ли проверку автоматически после перезагрузки устройства. По умолчанию выключено. */
    fun loadAutoStart(): Boolean = prefs.getBoolean(KEY_AUTO_START, false)

    fun saveAutoStart(enabled: Boolean) = prefs.edit { putBoolean(KEY_AUTO_START, enabled) }

    private companion object {
        const val FILE = "autocheck_secure_prefs"
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_LEGACY_LOGIN = "login"
        const val KEY_LEGACY_PASSWORD = "password"
        const val LEGACY_ACCOUNT_ID = "legacy"
        const val KEY_TIMEOUT = "timeout"
        const val KEY_ROUTE = "route"
        const val KEY_THEME = "theme_mode"
        const val KEY_AUTO_START = "auto_start_on_boot"
        const val KEY_LOG_SAVE = "log_save_responses"
        const val KEY_LOG_KINDS = "log_response_kinds"
        const val KEY_LOG_MAX = "log_max_entries"
        const val KEY_LOG_DIR = "log_download_dir"
        const val KEY_N_LESSON = "notify_lesson"
        const val KEY_N_ERRORS = "notify_errors"
        const val KEY_N_WARNINGS = "notify_warnings"
        const val KEY_N_CHECKS = "notify_checks"
        const val KEY_N_SERVICE = "notify_service"
        const val KEY_N_CONTROL = "notify_control"
        const val DEFAULT_TIMEOUT = 30

        fun createPrefs(context: Context): SharedPreferences {
            fun create(): SharedPreferences {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                return EncryptedSharedPreferences.create(
                    context,
                    FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }
            return try {
                create()
            } catch (e: Exception) {
                // Keystore мог сброситься (например, после восстановления устройства) — создаём хранилище заново
                context.deleteSharedPreferences(FILE)
                create()
            }
        }
    }
}
