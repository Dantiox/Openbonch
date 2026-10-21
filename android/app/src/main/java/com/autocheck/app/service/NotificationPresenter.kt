package com.autocheck.app.service

import com.autocheck.app.data.EngineStatus
import com.autocheck.app.data.LogCategory
import com.autocheck.app.data.LogEntry
import com.autocheck.app.data.NotificationPrefs

/** Заголовок и текст уведомления. Сравнивается по значению: одинаковое содержимое повторно не публикуется. */
data class NotificationContent(val title: String, val text: String)

/** Решает, что написать в единственном уведомлении приложения. Не зависит от Android API. */
object NotificationPresenter {

    /**
     * Уведомление во время работы.
     *
     * @param baselineId записи журнала с id не больше этого значения относятся к прошлым запускам
     */
    fun running(
        status: EngineStatus,
        logs: List<LogEntry>,
        prefs: NotificationPrefs,
        baselineId: Long,
        intervalSec: Int,
    ): NotificationContent {
        val connecting = status == EngineStatus.CONNECTING
        val event = latestEvent(logs, prefs, baselineId)

        val title = if (connecting) "AutoCheck подключается" else "AutoCheck работает"
        val text = when {
            event != null -> "${event.time} · ${event.message}"
            connecting -> "Выполняется вход в личный кабинет"
            else -> "Проверка расписания каждые $intervalSec с"
        }
        return NotificationContent(title, text)
    }

    /** Уведомление после остановки (с кнопкой «Включить»). */
    fun stopped(status: EngineStatus, logs: List<LogEntry>): NotificationContent {
        if (status == EngineStatus.ERROR) {
            val reason = logs.lastOrNull { it.category == LogCategory.ERROR }?.message
            return NotificationContent(
                "AutoCheck остановлен из-за ошибки",
                reason ?: "Подробности смотрите в журнале",
            )
        }
        return NotificationContent("AutoCheck остановлен", "Проверка расписания не выполняется")
    }

    /**
     * Последнее событие, которое пользователь разрешил показывать.
     *
     * Ошибка или предупреждение перестают быть актуальными, как только после них прошла
     * успешная проверка или было начато занятие — даже если эти события в уведомлении отключены.
     */
    private fun latestEvent(logs: List<LogEntry>, prefs: NotificationPrefs, baselineId: Long): LogEntry? {
        var problemsResolved = false
        for (entry in logs.asReversed()) {
            if (entry.id <= baselineId) break
            when (entry.category) {
                LogCategory.PAUSE, LogCategory.RESPONSE -> Unit
                LogCategory.CHECK, LogCategory.LESSON -> {
                    if (prefs.shows(entry.category)) return entry
                    problemsResolved = true
                }

                LogCategory.ERROR, LogCategory.WARNING ->
                    if (!problemsResolved && prefs.shows(entry.category)) return entry

                LogCategory.SERVICE -> if (prefs.shows(entry.category)) return entry
            }
        }
        return null
    }
}
