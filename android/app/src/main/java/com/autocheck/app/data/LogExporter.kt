package com.autocheck.app.data

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Куда и под каким именем сохранён файл (для подтверждения пользователю). */
data class SavedFile(
    val place: String,
    val fileName: String,
    /** Выбранная папка оказалась недоступна, файл сохранён в системную Download. */
    val usedFallback: Boolean = false,
)

/**
 * Копирование, скачивание и отправка журнала (`.log`) и сохранённых ответов сервера (`.html`).
 * Не зависит от интерфейса: все функции принимают готовый текст.
 */
object LogExporter {
    /** Тип для сохранения `.log`: с `text/plain` часть систем дописывает к имени `.txt`. */
    const val MIME_LOG_SAVE = "application/octet-stream"
    const val MIME_LOG_SHARE = "text/plain"
    const val MIME_HTML = "text/html"

    private const val SHARE_DIR = "share"
    private val stampFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    private val lineFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // ───── Содержимое и имена файлов ─────

    /** Текст `.log`: записи в хронологическом порядке (старые сверху), по одной на строку. */
    fun formatLog(entries: List<LogEntry>): String = buildString {
        append("AutoCheck — журнал\n")
        append("Сформирован: ").append(LocalDateTime.now().format(lineFormat)).append('\n')
        append("Записей: ").append(entries.size).append("\n\n")
        entries.forEach { e ->
            append(e.fullTime().padEnd(19)).append("  ")
            append(e.level.name.padEnd(7)).append("  ")
            append(e.message)
            if (e.responseFile != null) append("  [есть HTML-ответ]")
            append('\n')
        }
    }

    fun logFileName(): String = "autocheck-log-${LocalDateTime.now().format(stampFormat)}.log"

    fun htmlFileName(entry: LogEntry): String {
        val moment = if (entry.timestamp != 0L) {
            Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
        } else {
            LocalDateTime.now()
        }
        return "autocheck-response-${entry.id}-${moment.format(stampFormat)}.html"
    }

    private fun LogEntry.fullTime(): String {
        if (timestamp == 0L) return time
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime().format(lineFormat)
    }

    // ───── Копирование ─────

    /** @return false, если система отказалась (чаще всего текст слишком большой для буфера обмена). */
    fun copy(context: Context, label: String, text: String): Boolean = runCatching {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }.isSuccess

    // ───── Скачивание ─────

    /** На Android 9 и ниже для записи в общую папку Download нужно разрешение; с Android 10 — нет. */
    fun needsStoragePermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED

    /**
     * Сохраняет файл в папку [treeUri] (выбрана в настройках журнала) или, если она не задана
     * либо недоступна, в системную Download. Выполнять не на главном потоке.
     *
     * @throws IOException если сохранить не удалось
     */
    fun save(context: Context, treeUri: String?, name: String, mime: String, text: String): SavedFile {
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (!treeUri.isNullOrBlank()) {
            val saved = runCatching { saveToTree(context, Uri.parse(treeUri), name, mime, bytes) }.getOrNull()
            if (saved != null) return saved
            return saveToDownload(context, name, mime, bytes).copy(usedFallback = true)
        }
        return saveToDownload(context, name, mime, bytes)
    }

    private fun saveToTree(context: Context, tree: Uri, name: String, mime: String, bytes: ByteArray): SavedFile {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val doc = DocumentsContract.createDocument(resolver, parent, mime, name)
            ?: throw IOException("Не удалось создать файл")
        try {
            resolver.openOutputStream(doc, "wt")?.use { it.write(bytes) }
                ?: throw IOException("Не удалось открыть файл для записи")
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, doc) }
            throw e
        }
        return SavedFile(folderLabel(context, tree.toString()), displayName(context, doc) ?: name)
    }

    private fun saveToDownload(context: Context, name: String, mime: String, bytes: ByteArray): SavedFile {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Не удалось создать файл в Download")
            try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: throw IOException("Не удалось открыть файл для записи")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                throw e
            }
            // Система может переименовать файл, если такое имя уже занято
            return SavedFile(DEFAULT_FOLDER, displayName(context, uri) ?: name)
        }

        if (needsStoragePermission(context)) throw IOException("Нет разрешения на запись в Download")
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Папка Download недоступна")
        val file = uniqueFile(dir, name)
        file.writeBytes(bytes)
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null)
        return SavedFile(DEFAULT_FOLDER, file.name)
    }

    private fun uniqueFile(dir: File, name: String): File {
        var file = File(dir, name)
        if (!file.exists()) return file
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        var n = 1
        while (file.exists()) file = File(dir, "$base ($n)$ext").also { n++ }
        return file
    }

    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull()

    const val DEFAULT_FOLDER = "Download"

    /** Название папки для скачивания: `Download` по умолчанию, иначе путь выбранной папки. */
    fun folderLabel(context: Context, treeUri: String?): String {
        if (treeUri.isNullOrBlank()) return DEFAULT_FOLDER
        return runCatching {
            val tree = Uri.parse(treeUri)
            val id = DocumentsContract.getTreeDocumentId(tree)
            val path = id.substringAfter(':', "")
            path.ifEmpty {
                displayName(context, DocumentsContract.buildDocumentUriUsingTree(tree, id)) ?: "Выбранная папка"
            }
        }.getOrDefault("Выбранная папка")
    }

    // ───── Поделиться ─────

    /**
     * Кладёт файл во временную папку кэша и готовит окно «Поделиться». Запись файла лучше делать
     * не на главном потоке, а сам [Intent] запускать уже на главном.
     */
    fun shareIntent(context: Context, name: String, mime: String, text: String): Intent {
        val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // старые отправленные файлы больше не нужны
        val file = File(dir, name).apply { writeText(text) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, name)
            clipData = ClipData.newRawUri(name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, null).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
