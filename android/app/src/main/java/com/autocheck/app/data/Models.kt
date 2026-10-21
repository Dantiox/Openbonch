package com.autocheck.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class LogLevel { INFO, SUCCESS, WARNING, ERROR }

/**
 * Тип события. По нему определяется, показывать ли запись в уведомлении
 * (см. [NotificationPrefs]). В журнале записи видны всегда.
 */
enum class LogCategory {
    /** Вход, смена сети, запуск и остановка. */
    SERVICE,

    /** Результат успешной проверки расписания. */
    CHECK,

    /** Занятие начато. */
    LESSON,

    /** Предупреждение, например истёкшая сессия. */
    WARNING,

    /** Ошибка подключения или авторизации. */
    ERROR,

    /** Пауза между проверками. В уведомлении не показывается никогда. */
    PAUSE,

    /** Сохранённый ответ сервера (HTML-файл, открывается из журнала). В уведомлении не показывается никогда. */
    RESPONSE,
}

data class LogEntry(
    val id: Long,
    val time: String,
    val level: LogLevel,
    val message: String,
    val category: LogCategory = LogCategory.SERVICE,
    /** Момент записи (мс). Нужен, чтобы после перезапуска отличать вчерашние записи от сегодняшних. */
    val timestamp: Long = 0L,
    /** Имя файла с сохранённым ответом сервера; `null` — у записи нет ответа. */
    val responseFile: String? = null,
)

/** Время записи; для записей не за сегодня добавляется дата. */
fun LogEntry.displayTime(): String {
    if (timestamp == 0L) return time
    val day = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    return if (day == LocalDate.now()) time else "${day.format(DATE_FORMAT)} $time"
}

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM")

/** Какие ответы сервера можно сохранять в журнал как HTML-файлы. */
enum class ResponseKind(val title: String, val description: String) {
    LOGIN("Вход", "Ответ сервера на вход по логину и паролю."),
    SCHEDULE(
        "Расписание",
        "Страница расписания. Загружается при каждой проверке, поэтому быстро заполняет журнал.",
    ),
    CLICK("Нажатие кнопки", "Ответ сервера на нажатие кнопки занятия."),
    ERROR(
        "Ошибка",
        "Ответы с ошибкой: HTTP-код не 2xx или сообщение об истёкшей сессии (нет прав доступа).",
    ),
}

/** Настройки журнала. Применяются сразу и сохраняются на устройстве. */
data class LogPrefs(
    /** Сохранять ли ответы сервера как HTML-файлы. По умолчанию выключено. */
    val saveResponses: Boolean = false,
    /** Какие типы ответов сохранять (если [saveResponses] включено). */
    val kinds: Set<ResponseKind> = setOf(ResponseKind.LOGIN, ResponseKind.CLICK, ResponseKind.ERROR),
    /** Максимум записей в журнале: старые удаляются вместе с их HTML-файлами. */
    val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    /** Папка для кнопки «Скачать» (адрес дерева документов Android); `null` — системная Download. */
    val downloadDir: String? = null,
) {
    companion object {
        const val DEFAULT_MAX_ENTRIES = 300
        const val MIN_ENTRIES = 50
        const val MAX_ENTRIES = 1000
        const val ENTRIES_STEP = 50
    }
}

enum class EngineStatus { STOPPED, CONNECTING, RUNNING, ERROR }

/** Как запросы приложения выходят в сеть. Первый вариант используется по умолчанию. */
enum class TrafficRoute(val title: String, val description: String) {
    SYSTEM(
        "Как в системе",
        "Маршрут выбирает Android: если VPN включён, запросы идут через него.",
    ),
    DIRECT(
        "Напрямую, без VPN",
        "Через Wi‑Fi, а если его нет — через мобильную сеть, не используя VPN. " +
            "Если VPN выключен, это обычное соединение.",
    ),
    WIFI("Только Wi‑Fi, без VPN", "Запросы идут только через Wi‑Fi или Ethernet, не используя VPN."),
    CELLULAR(
        "Только мобильная сеть, без VPN",
        "Запросы идут только через мобильный интернет, не используя VPN.",
    ),
}

/** Тема оформления приложения. */
enum class ThemeMode(val title: String, val description: String) {
    SYSTEM("Как в системе", "Тема меняется вместе с настройкой Android."),
    LIGHT("Светлая", "Светлое оформление независимо от настроек Android."),
    DARK("Тёмная", "Тёмное оформление независимо от настроек Android."),
}

/** Как аккаунт входит в личный кабинет. */
enum class AccountType(val title: String) {
    /** Логин и пароль: приложение входит само и при истечении сессии входит заново. */
    PASSWORD("Логин и пароль"),

    /** Готовый токен: значение cookie `miden` из браузера. Повторный вход невозможен. */
    TOKEN("Токен"),
}

/** Один аккаунт личного кабинета. [id] нужен, чтобы отличать аккаунты и хранить сессию каждого отдельно. */
data class Account(
    val id: String = UUID.randomUUID().toString(),
    val type: AccountType = AccountType.PASSWORD,
    val login: String = "",
    val password: String = "",
    val token: String = "",
) {
    val isComplete: Boolean
        get() = when (type) {
            AccountType.PASSWORD -> login.isNotBlank() && password.isNotBlank()
            AccountType.TOKEN -> token.isNotBlank()
        }

    /** Ничего не введено: такой аккаунт при сохранении отбрасывается. */
    val isBlank: Boolean
        get() = login.isBlank() && password.isEmpty() && token.isBlank()

    /** Подпись для журнала и списка: логин либо последние символы токена. */
    val label: String
        get() = when (type) {
            AccountType.PASSWORD -> login.ifBlank { "без логина" }
            AccountType.TOKEN -> "токен ...${token.takeLast(4)}"
        }

    /** Убирает лишние пробелы и поля, не относящиеся к выбранному способу входа. */
    fun normalized(): Account = when (type) {
        AccountType.PASSWORD -> copy(login = login.trim(), token = "")
        AccountType.TOKEN -> copy(login = "", password = "", token = token.trim())
    }
}

data class Credentials(
    val accounts: List<Account> = emptyList(),
    val timeoutSec: Int = 30,
    val route: TrafficRoute = TrafficRoute.SYSTEM,
) {
    /** Есть хотя бы один аккаунт, с которым можно работать. */
    val isComplete get() = accounts.any { it.isComplete }
}

/**
 * Какие события показывать в уведомлении. Само уведомление одно и обновляется на месте:
 * настройки определяют только его текст.
 */
data class NotificationPrefs(
    val lessonStarted: Boolean = true,
    val errors: Boolean = true,
    val warnings: Boolean = false,
    val checks: Boolean = false,
    val service: Boolean = false,
    /** После остановки оставить уведомление с кнопкой «Включить». */
    val controlWhenStopped: Boolean = true,
) {
    fun shows(category: LogCategory): Boolean = when (category) {
        LogCategory.LESSON -> lessonStarted
        LogCategory.ERROR -> errors
        LogCategory.WARNING -> warnings
        LogCategory.CHECK -> checks
        LogCategory.SERVICE -> service
        LogCategory.PAUSE, LogCategory.RESPONSE -> false
    }

    /** Сколько типов событий включено (для краткого описания в настройках). */
    val enabledEventTypes: Int
        get() = listOf(lessonStarted, errors, warnings, checks, service).count { it }

    companion object {
        const val EVENT_TYPES = 5
    }
}

data class Stats(
    val cycles: Int = 0,
    val started: Int = 0,
    val lastCheck: String? = null,
)
