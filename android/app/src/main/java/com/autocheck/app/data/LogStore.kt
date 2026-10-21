package com.autocheck.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Журнал на диске, в приватной папке приложения (`filesDir/log`): список записей — один JSON-файл,
 * сохранённые ответы сервера — отдельные HTML-файлы. Переживает перезапуск приложения и устройства.
 */
class LogStore(context: Context) {
    private val dir = File(context.filesDir, "log").apply { mkdirs() }
    private val responsesDir = File(dir, "responses").apply { mkdirs() }
    private val entriesFile = File(dir, "entries.json")

    // ───── Записи ─────

    fun loadEntries(): List<LogEntry> {
        if (!entriesFile.exists()) return emptyList()
        return try {
            val array = JSONArray(entriesFile.readText())
            (0 until array.length()).mapNotNull { i ->
                runCatching { fromJson(array.getJSONObject(i)) }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList() // повреждённый файл не должен мешать запуску
        }
    }

    /** Пишет во временный файл и переименовывает: если процесс убьют посреди записи, старый файл останется целым. */
    fun saveEntries(entries: List<LogEntry>) {
        runCatching {
            val array = JSONArray()
            entries.forEach { array.put(toJson(it)) }
            val tmp = File(dir, "entries.json.tmp")
            tmp.writeText(array.toString())
            if (!tmp.renameTo(entriesFile)) {
                entriesFile.delete()
                tmp.renameTo(entriesFile)
            }
        }
    }

    // ───── Ответы сервера ─────

    /** @return false, если записать файл не удалось. */
    fun writeResponse(name: String, body: String): Boolean = runCatching {
        File(responsesDir, safe(name)).writeText(body.take(MAX_RESPONSE_CHARS))
    }.isSuccess

    fun readResponse(name: String): String? = runCatching {
        File(responsesDir, safe(name)).takeIf { it.isFile }?.readText()
    }.getOrNull()

    fun deleteResponses(names: Collection<String>) {
        names.forEach { runCatching { File(responsesDir, safe(it)).delete() } }
    }

    /** Удаляет файлы, на которые не ссылается ни одна запись (например, после обрезки журнала). */
    fun deleteResponsesExcept(keep: Set<String>) {
        responsesDir.listFiles()?.forEach { if (it.name !in keep) it.delete() }
    }

    /** Имя приходит из сохранённого JSON: оставляем только последний сегмент, чтобы нельзя было выйти из папки. */
    private fun safe(name: String): String = File(name).name

    private fun toJson(e: LogEntry) = JSONObject()
        .put("id", e.id)
        .put("time", e.time)
        .put("level", e.level.name)
        .put("message", e.message)
        .put("category", e.category.name)
        .put("timestamp", e.timestamp)
        .apply { e.responseFile?.let { put("responseFile", it) } }

    private fun fromJson(o: JSONObject) = LogEntry(
        id = o.getLong("id"),
        time = o.getString("time"),
        level = LogLevel.valueOf(o.getString("level")),
        message = o.getString("message"),
        category = runCatching { LogCategory.valueOf(o.getString("category")) }.getOrDefault(LogCategory.SERVICE),
        timestamp = o.optLong("timestamp", 0L),
        responseFile = o.optString("responseFile").ifEmpty { null },
    )

    private companion object {
        /** Страховка от гигантских ответов: больше в файл не пишем. */
        const val MAX_RESPONSE_CHARS = 1_000_000
    }
}
