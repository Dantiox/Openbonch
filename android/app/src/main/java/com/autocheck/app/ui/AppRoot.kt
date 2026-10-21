package com.autocheck.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autocheck.app.data.AutoClickState
import com.autocheck.app.data.EngineStatus
import com.autocheck.app.data.LogEntry
import com.autocheck.app.data.LogCategory
import com.autocheck.app.data.LogExporter
import com.autocheck.app.ui.components.ToastHost
import com.autocheck.app.ui.components.ToastState
import com.autocheck.app.ui.components.rememberExporter
import com.autocheck.app.ui.components.floating
import com.autocheck.app.ui.screens.HomeScreen
import com.autocheck.app.ui.screens.LogScreen
import com.autocheck.app.ui.screens.ResponseViewerScreen
import com.autocheck.app.ui.screens.SettingsScreen
import com.autocheck.app.ui.theme.Ios

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Главная", Icons.Rounded.Home),
    Log("Журнал", Icons.Rounded.History),
    Settings("Настройки", Icons.Rounded.Settings),
}

/** Место под плавающую панель вкладок (без системной навигации): сама панель + отступы. */
private val TabBarSpace: Dp = 88.dp

@Composable
fun AppRoot(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    // Запись журнала, ответ сервера которой сейчас открыт на весь экран
    var viewingId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Растёт при каждом повторном нажатии на уже открытую вкладку: экран возвращается в начало
    var reselect by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val status by AutoClickState.status.collectAsStateWithLifecycle()
    val stats by AutoClickState.stats.collectAsStateWithLifecycle()
    val logs by AutoClickState.logs.collectAsStateWithLifecycle()

    // Подтверждения («Скопировано», «Сохранено») и выполнение копирования/скачивания/отправки для журнала и ответов
    val toast = remember { ToastState() }
    val exporter = rememberExporter(vm.logPrefs.downloadDir, toast)

    // Разрешение на уведомления нужно на Android 13+; сервис стартует в любом случае
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.start() }

    val onToggle: () -> Unit = {
        if (status == EngineStatus.RUNNING || status == EngineStatus.CONNECTING) {
            vm.stop()
        } else if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.start()
        }
    }

    // Ответ сервера открыт на весь экран. Если запись уже удалена из журнала, он закрывается
    val viewing = viewingId?.let { id -> logs.firstOrNull { it.id == id && it.responseFile != null } }
    // Во время анимации закрытия запись уже не «открыта», но экран ещё на виду — помним последнюю
    var lastViewed by remember { mutableStateOf<LogEntry?>(null) }
    if (viewing != null) lastViewed = viewing
    // 0 — виден журнал, 1 — ответ сервера открыт; журнал под ним уходит влево, как страница настроек
    val shift by animateFloatAsState(
        targetValue = if (viewing != null) 1f else 0f,
        animationSpec = tween(320),
        label = "viewer-shift",
    )

    val c = Ios.colors
    // Содержимое прокручивается под плавающей панелью, поэтому экранам нужен нижний отступ
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + TabBarSpace

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = c.background,
        contentColor = c.label,
    ) {
        Box(Modifier.fillMaxSize()) {
            Crossfade(
                targetState = tab,
                label = "tabs",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -size.width / 4f * shift
                        alpha = 1f - shift
                    }
                    .statusBarsPadding(),
            ) { current ->
                when (current) {
                    Tab.Home -> HomeScreen(
                        status = status,
                        stats = stats,
                        recentLogs = logs.filter { it.category != LogCategory.RESPONSE }.takeLast(4).reversed(),
                        timeoutSec = vm.saved.timeoutSec,
                        hasCredentials = vm.canStart,
                        bottomInset = bottomInset,
                        resetSignal = reselect,
                        onToggle = onToggle,
                        onOpenLog = { tab = Tab.Log },
                        onOpenSettings = { tab = Tab.Settings },
                    )

                    Tab.Log -> LogScreen(
                        logs = logs.asReversed(),
                        bottomInset = bottomInset,
                        resetSignal = reselect,
                        onClear = vm::clearLogs,
                        onOpenResponse = { viewingId = it.id },
                        onCopy = { exporter.copy("Весь журнал", LogExporter.formatLog(logs)) },
                        onDownload = {
                            exporter.download(LogExporter.logFileName(), LogExporter.MIME_LOG_SAVE, LogExporter.formatLog(logs))
                        },
                        onShare = {
                            exporter.share(LogExporter.logFileName(), LogExporter.MIME_LOG_SHARE, LogExporter.formatLog(logs))
                        },
                    )

                    Tab.Settings -> SettingsScreen(
                        vm = vm,
                        bottomInset = bottomInset,
                        resetSignal = reselect,
                    )
                }
            }

            FloatingTabBar(
                selected = tab,
                onSelect = { if (it == tab) reselect++ else tab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            // Ответ сервера поверх всего, включая панель вкладок. Выезжает справа и уезжает обратно,
            // с той же анимацией, что и переход в раздел настроек
            AnimatedVisibility(
                visible = viewing != null,
                modifier = Modifier.fillMaxSize(),
                enter = slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320)),
                exit = slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(200)),
            ) {
                lastViewed?.let { entry ->
                    ResponseViewerScreen(entry = entry, exporter = exporter, onClose = { viewingId = null })
                }
            }

            // Поверх всего, включая экран ответа сервера
            ToastHost(
                state = toast,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp, start = 20.dp, end = 20.dp),
            )
        }
    }
}

/** Плавающая панель вкладок: плоская капсула со сплошной заливкой и тонкой кромкой. */
@Composable
private fun FloatingTabBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .floating(CircleShape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Tab.entries.forEach { item ->
                TabItem(
                    tab = item,
                    selected = item == selected,
                    onClick = { onSelect(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val tint by animateColorAsState(
        targetValue = if (selected) c.accentText else c.secondaryLabel,
        animationSpec = tween(200),
        label = "tab-tint",
    )
    val highlight by animateColorAsState(
        targetValue = if (selected) c.accent.copy(alpha = if (c.isDark) 0.22f else 0.14f) else Color.Transparent,
        animationSpec = tween(200),
        label = "tab-highlight",
    )

    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(highlight)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
        )
    }
}
