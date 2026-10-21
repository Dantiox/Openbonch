package com.autocheck.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

/**
 * Общее состояние: сервис пишет, UI читает. Аналог класса `Log` из Python-версии.
 *
 * Журнал хранится на устройстве ([LogStore]) и после [init] восстанавливается при запуске процесса.
 */
object AutoClickState {
    private const val SAVE_INTERVAL_MS = 1000L
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val counter = AtomicLong()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var store: LogStore? = null

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _status = MutableStateFlow(EngineStatus.STOPPED)
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    private val _stats = MutableStateFlow(Stats())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    /** Настройки уведомлений: UI меняет, сервис применяет сразу, без перезапуска. */
    private val _notificationPrefs = MutableStateFlow(NotificationPrefs())
    val notificationPrefs: StateFlow<NotificationPrefs> = _notificationPrefs.asStateFlow()

    /** Настройки журнала: что сохранять и сколько хранить. Действуют сразу. */
    private val _logPrefs = MutableStateFlow(LogPrefs())

    /**
     * Восстанавливает журнал с диска и запускает его автосохранение. Вызывается один раз из
     * `Application.onCreate`, поэтому журнал доступен и UI, и сервису, и приёмнику загрузки.
     */
    @OptIn(FlowPreview::class)
    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        val s = LogStore(context.applicationContext)
        store = s

        val prefs = runCatching { SettingsStore(context.applicationContext).loadLogPrefs() }
            .getOrDefault(LogPrefs())
        _logPrefs.value = prefs

        val loaded = s.loadEntries()
        // Идентификаторы продолжаются с последнего: они служат ключами списка и именами файлов ответов
        counter.set(loaded.maxOfOrNull { it.id } ?: 0L)
        val kept = loaded.takeLast(prefs.maxEntries)
        _logs.value = kept
        s.deleteResponsesExcept(kept.mapNotNullTo(HashSet()) { it.responseFile })

        // Запись на диск не чаще раза в секунду: журнал может пополняться пачками
        ioScope.launch { _logs.sample(SAVE_INTERVAL_MS).collect { s.saveEntries(it) } }
    }

    private fun now(): String = LocalTime.now().format(timeFormat)

    fun log(level: LogLevel, message: String, category: LogCategory) {
        append(LogEntry(counter.incrementAndGet(), now(), level, message, category, System.currentTimeMillis()))
    }

    /**
     * Сохраняет ответ сервера как HTML-файл и добавляет в журнал запись со ссылкой на него —
     * но только если сохранение включено и выбран тип [kind].
     *
     * @param kind тип ответа (для ошибок — [ResponseKind.ERROR])
     * @param source какой запрос получил ответ; нужен для подписи, если [kind] — ошибка
     * @param tag подпись аккаунта, если их несколько
     */
    fun recordResponse(kind: ResponseKind, source: ResponseKind, httpCode: Int, tag: String?, body: String) {
        val prefs = _logPrefs.value
        val s = store ?: return
        if (!prefs.saveResponses || kind !in prefs.kinds) return

        val id = counter.incrementAndGet()
        val file = "$id.html"
        if (!s.writeResponse(file, body)) return

        val what = if (kind == source) kind.title else "${kind.title}: ${source.title.lowercase()}"
        val message = "Ответ сервера: $what (HTTP $httpCode)"
        append(
            LogEntry(
                id = id,
                time = now(),
                level = if (kind == ResponseKind.ERROR) LogLevel.WARNING else LogLevel.INFO,
                message = if (tag == null) message else "$tag: $message",
                category = LogCategory.RESPONSE,
                timestamp = System.currentTimeMillis(),
                responseFile = file,
            )
        )
    }

    /** Содержимое сохранённого ответа; `null`, если файла нет. */
    fun readResponse(file: String): String? = store?.readResponse(file)

    @Synchronized
    private fun append(entry: LogEntry) {
        _logs.value = _logs.value + entry
        trimToLimit()
    }

    /** Отбрасывает самые старые записи сверх лимита вместе с их HTML-файлами. */
    private fun trimToLimit() {
        val max = _logPrefs.value.maxEntries
        val current = _logs.value
        if (current.size <= max) return
        val dropCount = current.size - max
        _logs.value = current.drop(dropCount)
        deleteResponseFiles(current.take(dropCount))
    }

    private fun deleteResponseFiles(entries: List<LogEntry>) {
        val files = entries.mapNotNull { it.responseFile }
        if (files.isEmpty()) return
        ioScope.launch { store?.deleteResponses(files) }
    }

    fun info(message: String, category: LogCategory = LogCategory.SERVICE) =
        log(LogLevel.INFO, message, category)

    fun success(message: String, category: LogCategory = LogCategory.SERVICE) =
        log(LogLevel.SUCCESS, message, category)

    fun warning(message: String) = log(LogLevel.WARNING, message, LogCategory.WARNING)
    fun error(message: String) = log(LogLevel.ERROR, message, LogCategory.ERROR)

    fun setStatus(status: EngineStatus) {
        _status.value = status
    }

    fun setNotificationPrefs(prefs: NotificationPrefs) {
        _notificationPrefs.value = prefs
    }

    /** Применяет настройки журнала; если лимит стал меньше, лишние старые записи удаляются сразу. */
    @Synchronized
    fun setLogPrefs(prefs: LogPrefs) {
        _logPrefs.value = prefs
        trimToLimit()
    }

    fun onCycleFinished() {
        _stats.update { it.copy(cycles = it.cycles + 1, lastCheck = now()) }
    }

    fun onLessonStarted() {
        _stats.update { it.copy(started = it.started + 1) }
    }

    @Synchronized
    fun clearLogs() {
        val old = _logs.value
        _logs.value = emptyList()
        deleteResponseFiles(old)
    }

    fun resetForNewRun() {
        _stats.value = Stats()
        _status.value = EngineStatus.CONNECTING
    }
}
