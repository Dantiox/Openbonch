package com.autocheck.app.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autocheck.app.data.LogExporter
import com.autocheck.app.ui.theme.Ios
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ───────────────────────── Всплывающее подтверждение ─────────────────────────

class ToastData(val id: Int, val title: String, val detail: String?, val success: Boolean)

/** Состояние всплывающей плашки («Скопировано», «Сохранено»). Показывает [ToastHost]. */
@Stable
class ToastState {
    var current by mutableStateOf<ToastData?>(null)
        internal set
    private var seq = 0

    fun show(title: String, detail: String? = null, success: Boolean = true) {
        current = ToastData(++seq, title, detail, success)
    }
}

/** Плашка в стиле iOS: плоская скруглённая капсула сверху экрана, исчезает сама через пару секунд. */
@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val data = state.current
    // Во время анимации исчезновения плашка ещё рисуется, хотя current уже null — помним последнее содержимое
    var shown by remember { mutableStateOf<ToastData?>(null) }
    if (data != null) shown = data

    LaunchedEffect(data?.id) {
        if (data != null) {
            delay(if (data.detail != null) 3200L else 2000L)
            if (state.current?.id == data.id) state.current = null
        }
    }

    AnimatedVisibility(
        visible = data != null,
        modifier = modifier,
        enter = slideInVertically(tween(280)) { -it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(180)),
    ) {
        val toast = shown
        if (toast != null) {
            Row(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .floating(RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (toast.success) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                    contentDescription = null,
                    tint = if (toast.success) c.success else c.danger,
                    modifier = Modifier.size(24.dp),
                )
                Column(Modifier.weight(1f, fill = false)) {
                    Text(toast.title, style = MaterialTheme.typography.titleSmall, color = c.label)
                    if (toast.detail != null) {
                        Text(
                            toast.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.secondaryLabel,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────── Панель действий ─────────────────────────

/**
 * Плоская капсула с тремя действиями — «Копировать», «Скачать», «Поделиться» — в стиле панели вкладок:
 * значок над подписью, голубой цвет. Недоступная панель серая.
 */
@Composable
fun ExportBar(
    onCopy: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .floating(CircleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ExportAction("Копировать", Icons.Rounded.ContentCopy, enabled, onCopy, Modifier.weight(1f))
        ExportAction("Скачать", Icons.Rounded.Download, enabled, onDownload, Modifier.weight(1f))
        ExportAction("Поделиться", Icons.Rounded.IosShare, enabled, onShare, Modifier.weight(1f))
    }
}

@Composable
private fun ExportAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val c = Ios.colors
    val tint = if (enabled) c.accentText else c.tertiaryLabel
    Column(
        modifier = modifier
            .iosClickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

// ───────────────────────── Выполнение действий ─────────────────────────

/** Выполняет копирование, скачивание и отправку и сообщает о результате через [ToastState]. */
@Stable
class Exporter internal constructor(
    private val context: android.content.Context,
    private val scope: CoroutineScope,
    private val toast: ToastState,
    private val folder: () -> String?,
    private val ensureStorageAccess: (onGranted: () -> Unit) -> Unit,
) {
    /** [what] — что скопировано, для подписи под «Скопировано». */
    fun copy(what: String, text: String) {
        if (LogExporter.copy(context, what, text)) {
            toast.show("Скопировано", what)
        } else {
            toast.show(
                "Не удалось скопировать",
                "Слишком много текста для буфера обмена. Используйте «Скачать» или «Поделиться».",
                success = false,
            )
        }
    }

    fun download(name: String, mime: String, text: String) {
        // Адрес папки читаем сразу, чтобы смена настройки во время записи не подменила цель
        val target = folder()
        val save = {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { LogExporter.save(context, target, name, mime, text) }
                }
                result.onSuccess { saved ->
                    if (saved.usedFallback) {
                        toast.show(
                            "Сохранено в ${saved.place}",
                            "Выбранная папка недоступна. ${saved.fileName}",
                        )
                    } else {
                        toast.show("Сохранено", "${saved.place}/${saved.fileName}")
                    }
                }.onFailure {
                    toast.show("Не удалось сохранить", it.message ?: "Проверьте папку в настройках журнала.", success = false)
                }
            }
            Unit
        }
        // Без своей папки на Android 9 и ниже нужно разрешение; с выбранной папкой оно не требуется
        if (target == null) ensureStorageAccess(save) else save()
    }

    fun share(name: String, mime: String, text: String) {
        scope.launch {
            val prepared = withContext(Dispatchers.IO) {
                runCatching { LogExporter.shareIntent(context, name, mime, text) }
            }
            prepared
                .mapCatching { context.startActivity(it) }
                .onFailure {
                    val detail = if (it is ActivityNotFoundException) "Нет приложения для отправки." else it.message
                    toast.show("Не удалось поделиться", detail, success = false)
                }
        }
    }
}

@Composable
fun rememberExporter(downloadDir: String?, toast: ToastState): Exporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentDir by rememberUpdatedState(downloadDir)

    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pending
        pending = null
        if (granted) {
            action?.invoke()
        } else {
            toast.show("Нет доступа к памяти", "Разрешите запись, чтобы сохранять файлы в Download.", success = false)
        }
    }

    return remember(context, scope, toast) {
        Exporter(
            context = context,
            scope = scope,
            toast = toast,
            folder = { currentDir },
            ensureStorageAccess = { onGranted ->
                if (LogExporter.needsStoragePermission(context)) {
                    pending = onGranted
                    permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    onGranted()
                }
            },
        )
    }
}
