package com.autocheck.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.autocheck.app.data.AutoClickState
import com.autocheck.app.data.LogEntry
import com.autocheck.app.data.LogExporter
import com.autocheck.app.data.displayTime
import com.autocheck.app.ui.components.CardShape
import com.autocheck.app.ui.components.ExportBar
import com.autocheck.app.ui.components.Exporter
import com.autocheck.app.ui.components.IosButton
import com.autocheck.app.ui.components.IosButtonStyle
import com.autocheck.app.ui.components.RoundIconButton
import com.autocheck.app.ui.theme.Ios
import com.autocheck.app.ui.theme.MonoStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Сохранённый ответ сервера на весь экран: «Страница» (как её нарисовал бы браузер) или «Код» (исходный HTML).
 * Закрывается кнопкой «Назад» и системным жестом.
 */
@Composable
fun ResponseViewerScreen(entry: LogEntry, exporter: Exporter, onClose: () -> Unit) {
    val c = Ios.colors
    BackHandler(onBack = onClose)

    var html by remember(entry.id) { mutableStateOf<String?>(null) }
    var loaded by remember(entry.id) { mutableStateOf(false) }
    var showSource by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(entry.id) {
        html = withContext(Dispatchers.IO) { AutoClickState.readResponse(entry.responseFile.orEmpty()) }
        loaded = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
            RoundIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Назад",
                onClick = onClose,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text("Ответ сервера", style = MaterialTheme.typography.titleMedium, color = c.label)
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(entry.message, style = MaterialTheme.typography.bodyMedium, color = c.label)
            Text(entry.displayTime(), style = MonoStyle, color = c.secondaryLabel)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Выбранная «Страница» — оранжевая, выбранный «Код» — синий
            ModeButton("Страница", Icons.Rounded.Visibility, c.orange, selected = !showSource, Modifier.weight(1f)) {
                showSource = false
            }
            ModeButton("Код", Icons.Rounded.Code, c.accentText, selected = showSource, Modifier.weight(1f)) {
                showSource = true
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(CardShape)
                .background(if (showSource || !loaded) c.card else androidx.compose.ui.graphics.Color.White),
        ) {
            val text = html
            when {
                !loaded -> Unit
                text == null -> Message("Файл ответа не найден. Возможно, он был удалён.")
                text.isBlank() -> Message("Сервер вернул пустой ответ.")
                showSource -> SelectionContainer {
                    Text(
                        text,
                        style = MonoStyle,
                        color = c.label,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                    )
                }

                else -> HtmlView(text)
            }
        }

        // Этот HTML-файл целиком — одинаково в режимах «Страница» и «Код»
        val fileText = html
        val canExport = loaded && !fileText.isNullOrBlank()
        ExportBar(
            onCopy = { fileText?.let { exporter.copy("HTML-ответ сервера", it) } },
            onDownload = { fileText?.let { exporter.download(LogExporter.htmlFileName(entry), LogExporter.MIME_HTML, it) } },
            onShare = { fileText?.let { exporter.share(LogExporter.htmlFileName(entry), LogExporter.MIME_HTML, it) } },
            enabled = canExport,
        )
    }
}

@Composable
private fun ModeButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    IosButton(
        text = text,
        icon = icon,
        onClick = onClick,
        modifier = modifier,
        style = if (selected) IosButtonStyle.Tinted else IosButtonStyle.Neutral,
        tint = tint,
        height = 40.dp,
    )
}

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Ios.colors.secondaryLabel, textAlign = TextAlign.Center)
    }
}

/**
 * Показывает сохранённый HTML. Это чужая страница, поэтому WebView ограничен: без JavaScript,
 * без доступа к файлам и без сети (стили и картинки с сервера не подгружаются, зато запросы не
 * уходят в обход выбранного маршрута трафика), переходы по ссылкам отключены.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HtmlView(html: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.blockNetworkLoads = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                setBackgroundColor(AndroidColor.WHITE)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = true
                }
            }
        },
        update = { view ->
            // update вызывается при каждой перекомпоновке: страницу перезагружаем только если текст изменился
            if (view.tag != html) {
                view.tag = html
                view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        },
        onRelease = { it.destroy() },
    )
}
