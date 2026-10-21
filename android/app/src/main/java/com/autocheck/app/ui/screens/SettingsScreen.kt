package com.autocheck.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings as SettingsIcon
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.autocheck.app.data.Account
import com.autocheck.app.data.AccountType
import com.autocheck.app.data.LogExporter
import com.autocheck.app.data.LogPrefs
import com.autocheck.app.data.NotificationPrefs
import com.autocheck.app.data.ResponseKind
import com.autocheck.app.data.ThemeMode
import com.autocheck.app.data.TrafficRoute
import com.autocheck.app.service.AutoStartSettings
import com.autocheck.app.ui.MainViewModel
import com.autocheck.app.ui.components.Banner
import com.autocheck.app.ui.components.GroupFooter
import com.autocheck.app.ui.components.GroupHeader
import com.autocheck.app.ui.components.GroupedCard
import com.autocheck.app.ui.components.IconTile
import com.autocheck.app.ui.components.InsetDivider
import com.autocheck.app.ui.components.IosButton
import com.autocheck.app.ui.components.IosButtonStyle
import com.autocheck.app.ui.components.IosSlider
import com.autocheck.app.ui.components.IosSwitch
import com.autocheck.app.ui.components.LargeTitle
import com.autocheck.app.ui.components.RoundIconButton
import com.autocheck.app.ui.components.iosClickable
import com.autocheck.app.ui.components.pressHighlight
import com.autocheck.app.ui.theme.Brand
import com.autocheck.app.ui.theme.Ios
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val CHECK_ATTEMPTS = 10
private const val CHECK_INTERVAL_MS = 500L

/** Нижний отступ прокручиваемых страниц: место под плавающую панель вкладок. */
private val LocalContentBottom = compositionLocalOf { 0.dp }

/** Разделы настроек. Главная страница настроек — список этих разделов. */
private enum class SettingsPage(val title: String, val icon: ImageVector) {
    Account("Аккаунты", Icons.Rounded.Person),
    Connection("Подключение", Icons.Rounded.Wifi),
    Notifications("Уведомления", Icons.Rounded.Notifications),
    Log("Журнал", Icons.Rounded.History),
    Appearance("Оформление", Icons.Rounded.Palette),
    Background("Работа в фоне", Icons.Rounded.BatteryChargingFull),
}

@Composable
fun SettingsScreen(vm: MainViewModel, bottomInset: Dp, resetSignal: Int) {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var ignoringBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var notificationsAllowed by remember { mutableStateOf(areNotificationsAllowed(context)) }

    // Возврат из системных настроек: фиксируем момент, когда приложение снова на экране
    var resumeCount by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeCount++
        onPauseOrDispose { }
    }

    // Часть прошивок применяет изменение (например, «Нет ограничений») уже после возврата в приложение,
    // поэтому одной проверки в onResume мало: перепроверяем несколько секунд подряд
    LaunchedEffect(resumeCount) {
        repeat(CHECK_ATTEMPTS) {
            ignoringBattery = isIgnoringBatteryOptimizations(context)
            notificationsAllowed = areNotificationsAllowed(context)
            delay(CHECK_INTERVAL_MS)
        }
    }

    // Повторное нажатие на вкладку «Настройки»: из раздела — к списку разделов, а на списке — в его начало
    val hubScroll = rememberScrollState()
    LaunchedEffect(resetSignal) {
        if (resetSignal == 0) return@LaunchedEffect
        if (page != null) page = null else hubScroll.animateScrollTo(0)
    }

    // Системная кнопка «Назад» возвращает к списку разделов
    BackHandler(enabled = page != null) { page = null }

    CompositionLocalProvider(LocalContentBottom provides bottomInset) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    if (initialState == null && targetState != null) {
                        // Открываем раздел: страница выезжает справа, как в навигации iOS
                        (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(320)) { -it / 4 } + fadeOut(tween(200)))
                    } else {
                        // Возвращаемся к списку
                        (slideInHorizontally(tween(320)) { -it / 4 } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(200)))
                    }
                },
                label = "settings-page",
                modifier = Modifier.fillMaxSize(),
            ) { current ->
                when (current) {
                    null -> SettingsHub(
                        vm = vm,
                        ignoringBattery = ignoringBattery,
                        scrollState = hubScroll,
                        onOpen = { page = it },
                    )

                    SettingsPage.Account -> AccountPage(vm, onBack = { page = null })
                    SettingsPage.Connection -> ConnectionPage(vm, onBack = { page = null })
                    SettingsPage.Notifications -> NotificationsPage(
                        vm = vm,
                        notificationsAllowed = notificationsAllowed,
                        onBack = { page = null },
                    )

                    SettingsPage.Log -> LogPage(vm, onBack = { page = null })

                    SettingsPage.Appearance -> AppearancePage(vm, onBack = { page = null })

                    SettingsPage.Background -> BackgroundPage(
                        vm = vm,
                        ignoringBattery = ignoringBattery,
                        onBack = { page = null },
                    )
                }
            }
        }
    }
}

// ───────────────────────── Главная страница настроек ─────────────────────────

@Composable
private fun SettingsHub(
    vm: MainViewModel,
    ignoringBattery: Boolean,
    scrollState: ScrollState,
    onOpen: (SettingsPage) -> Unit,
) {
    PageScaffold(scrollState) {
        LargeTitle("Настройки")

        GroupedCard {
            SettingsPage.entries.forEachIndexed { index, item ->
                if (index > 0) InsetDivider(startInset = 62.dp)
                NavRow(
                    icon = item.icon,
                    title = item.title,
                    summary = summaryOf(item, vm, ignoringBattery),
                    onClick = { onOpen(item) },
                )
            }
        }
    }
}

private fun summaryOf(page: SettingsPage, vm: MainViewModel, ignoringBattery: Boolean): String =
    when (page) {
        SettingsPage.Account -> {
            val ready = vm.accounts.map { it.normalized() }.filter { it.isComplete }
            when (ready.size) {
                0 -> "Аккаунты не добавлены"
                1 -> ready.first().label
                else -> "Аккаунтов: ${ready.size}"
            }
        }
        SettingsPage.Connection -> "${vm.route.title} · проверка каждые ${vm.timeout} с"
        SettingsPage.Notifications ->
            "Типов событий в уведомлении: ${vm.notifications.enabledEventTypes} из ${NotificationPrefs.EVENT_TYPES}"

        SettingsPage.Log -> {
            val prefs = vm.logPrefs
            val responses = if (prefs.saveResponses) {
                "ответы сервера: ${prefs.kinds.size} из ${ResponseKind.entries.size}"
            } else {
                "ответы сервера не сохраняются"
            }
            "$responses · до ${prefs.maxEntries} записей"
        }

        SettingsPage.Appearance -> vm.themeMode.title

        SettingsPage.Background -> {
            val battery = if (ignoringBattery) "Оптимизация батареи отключена" else "Оптимизация батареи включена"
            "$battery · автозапуск ${if (vm.autoStart) "вкл" else "выкл"}"
        }
    }

// ───────────────────────── Разделы ─────────────────────────

@Composable
private fun AccountPage(vm: MainViewModel, onBack: () -> Unit) {
    PageScaffold {
        PageHeader("Аккаунты", onBack)

        if (vm.accounts.isEmpty()) {
            GroupedCard {
                Text(
                    "Аккаунтов пока нет. Добавьте логин и пароль или токен.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ios.colors.secondaryLabel,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                )
            }
        }

        vm.accounts.forEachIndexed { index, account ->
            key(account.id) { AccountSection(vm, account, number = index + 1) }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("Добавить аккаунт")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IosButton(
                    text = "С паролем",
                    icon = Icons.Rounded.Add,
                    onClick = { vm.addAccount(AccountType.PASSWORD) },
                    style = IosButtonStyle.Tinted,
                    tint = Ios.colors.orange,
                    modifier = Modifier.weight(1f),
                )
                IosButton(
                    text = "С токеном",
                    icon = Icons.Rounded.Add,
                    onClick = { vm.addAccount(AccountType.TOKEN) },
                    style = IosButtonStyle.Tinted,
                    modifier = Modifier.weight(1f),
                )
            }
            GroupFooter(
                "Данные хранятся на устройстве в зашифрованном виде. " +
                    "Изменения сохраняются автоматически и применяются при следующей проверке."
            )
        }
    }
}

/** Карточка одного аккаунта: поля для выбранного способа входа и кнопка удаления. */
@Composable
private fun AccountSection(vm: MainViewModel, account: Account, number: Int) {
    var reveal by remember { mutableStateOf(false) }
    val c = Ios.colors

    val eye: @Composable () -> Unit = {
        Box(
            Modifier
                .iosClickable(pressedScale = 0.88f) { reveal = !reveal }
                .size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (reveal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = if (reveal) "Скрыть" else "Показать",
                tint = c.secondaryLabel,
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GroupHeader("$number · ${account.type.title}")
            IosButton(
                text = "Удалить",
                icon = Icons.Rounded.Delete,
                onClick = { vm.removeAccount(account.id) },
                style = IosButtonStyle.Tinted,
                tint = c.danger,
                height = 32.dp,
            )
        }
        GroupedCard {
            when (account.type) {
                AccountType.PASSWORD -> {
                    FieldRow(
                        label = "Логин",
                        placeholder = "Введите логин",
                        value = account.login,
                        onValueChange = { v -> vm.updateAccount(account.id) { it.copy(login = v) } },
                        keyboardType = KeyboardType.Email,
                    )
                    InsetDivider()
                    FieldRow(
                        label = "Пароль",
                        placeholder = "Введите пароль",
                        value = account.password,
                        onValueChange = { v -> vm.updateAccount(account.id) { it.copy(password = v) } },
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = eye,
                    )
                }

                AccountType.TOKEN -> FieldRow(
                    label = "Токен",
                    placeholder = "Значение cookie miden",
                    value = account.token,
                    onValueChange = { v -> vm.updateAccount(account.id) { it.copy(token = v) } },
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = eye,
                )
            }
        }
    }
}

@Composable
private fun ConnectionPage(vm: MainViewModel, onBack: () -> Unit) {
    val c = Ios.colors

    PageScaffold {
        PageHeader("Подключение", onBack)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("Маршрут трафика")
            GroupedCard {
                TrafficRoute.entries.forEachIndexed { index, option ->
                    if (index > 0) InsetDivider()
                    OptionRow(
                        selected = vm.route == option,
                        title = option.title,
                        description = option.description,
                        onClick = { vm.updateRoute(option) },
                    )
                }
            }
            GroupFooter(
                "Режимы «без VPN» не сработают, если в настройках VPN включена блокировка соединений без VPN. " +
                    "Если выбранной сети нет, запрос не отправляется и будет повторён при следующей проверке. " +
                    "Изменения сохраняются автоматически и вступают в силу при следующем запуске проверки."
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("Интервал проверки")
            GroupedCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Каждые", style = MaterialTheme.typography.bodyLarge, color = c.secondaryLabel)
                        Text("${vm.timeout} сек", style = MaterialTheme.typography.titleMedium, color = c.accentText)
                    }
                    IosSlider(
                        value = vm.timeout.toFloat(),
                        onValueChange = { vm.updateTimeout((it / 5).roundToInt() * 5) },
                        valueRange = 5f..300f,
                    )
                }
            }
            GroupFooter(
                "Как часто приложение проверяет расписание. Чем меньше значение, тем быстрее срабатывает " +
                    "автопосещение и тем выше расход батареи. Изменение сохраняется автоматически " +
                    "и вступает в силу при следующем запуске проверки."
            )
        }
    }
}

@Composable
private fun NotificationsPage(vm: MainViewModel, notificationsAllowed: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = vm.notifications

    PageScaffold {
        PageHeader("Уведомления", onBack)

        if (!notificationsAllowed) {
            Banner(
                text = "Уведомления отключены в настройках Android",
                actionLabel = "Открыть",
                onClick = { openNotificationSettings(context) },
            )
        }

        GroupFooter(
            "Приложение показывает одно уведомление. Оно обновляется на месте, новые не создаются. " +
                "Ниже выберите, какие события отображаются в его тексте."
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("События в уведомлении")
            GroupedCard {
                SwitchRow(
                    title = "Занятие начато",
                    description = "Когда приложение успешно начало занятие.",
                    checked = prefs.lessonStarted,
                    onCheckedChange = { on -> vm.updateNotifications { it.copy(lessonStarted = on) } },
                )
                InsetDivider()
                SwitchRow(
                    title = "Ошибки",
                    description = "Сбои подключения, неверный логин или пароль.",
                    checked = prefs.errors,
                    onCheckedChange = { on -> vm.updateNotifications { it.copy(errors = on) } },
                )
                InsetDivider()
                SwitchRow(
                    title = "Предупреждения",
                    description = "Например, истёкшая сессия, из-за которой нужен повторный вход.",
                    checked = prefs.warnings,
                    onCheckedChange = { on -> vm.updateNotifications { it.copy(warnings = on) } },
                )
                InsetDivider()
                SwitchRow(
                    title = "Результат каждой проверки",
                    description = "Например, «Нет активных занятий». Уведомление будет обновляться при каждой проверке.",
                    checked = prefs.checks,
                    onCheckedChange = { on -> vm.updateNotifications { it.copy(checks = on) } },
                )
                InsetDivider()
                SwitchRow(
                    title = "Служебные сообщения",
                    description = "Вход в кабинет, смена сети, запуск и остановка.",
                    checked = prefs.service,
                    onCheckedChange = { on -> vm.updateNotifications { it.copy(service = on) } },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("Управление")
            GroupedCard {
                SwitchRow(
                    title = "Кнопка «Включить» после остановки",
                    description = "После остановки уведомление остаётся в шторке, и проверку можно снова " +
                        "запустить прямо из него. Уведомление можно смахнуть.",
                    checked = prefs.controlWhenStopped,
                    onCheckedChange = { on -> vm.updateNotifications { it.copy(controlWhenStopped = on) } },
                )
            }
            GroupFooter("Изменения применяются сразу. Пока приложение работает, в уведомлении есть кнопка «Остановить».")
        }
    }
}

@Composable
private fun LogPage(vm: MainViewModel, onBack: () -> Unit) {
    val c = Ios.colors
    val prefs = vm.logPrefs
    val context = LocalContext.current

    val folderLabel = remember(prefs.downloadDir) { LogExporter.folderLabel(context, prefs.downloadDir) }
    val folderFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            // Право на папку нужно сохранить, иначе оно пропадёт после перезапуска приложения
            runCatching { context.contentResolver.takePersistableUriPermission(uri, folderFlags) }
            releaseFolder(context, prefs.downloadDir, except = uri.toString())
            vm.updateLogPrefs { it.copy(downloadDir = uri.toString()) }
        }
    }

    PageScaffold {
        PageHeader("Журнал", onBack)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("Ответы сервера")
            GroupedCard {
                SwitchRow(
                    title = "Сохранять ответы сервера",
                    description = "Ответ на каждый выбранный запрос сохраняется как HTML-файл. " +
                        "Его можно открыть из журнала.",
                    checked = prefs.saveResponses,
                    onCheckedChange = { on -> vm.updateLogPrefs { it.copy(saveResponses = on) } },
                )
            }
            GroupFooter(
                "Ответы могут содержать личные данные, например расписание. " +
                    "Файлы хранятся только в закрытой памяти приложения на этом устройстве."
            )
        }

        if (prefs.saveResponses) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GroupHeader("Какие ответы сохранять")
                GroupedCard {
                    ResponseKind.entries.forEachIndexed { index, kind ->
                        if (index > 0) InsetDivider()
                        SwitchRow(
                            title = kind.title,
                            description = kind.description,
                            checked = kind in prefs.kinds,
                            onCheckedChange = { on ->
                                vm.updateLogPrefs { it.copy(kinds = if (on) it.kinds + kind else it.kinds - kind) }
                            },
                        )
                    }
                }
                GroupFooter(
                    "Каждый сохранённый ответ — отдельная запись в журнале. " +
                        "Нажмите на неё, чтобы открыть ответ как страницу или посмотреть исходный код."
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("Скачивание")
            GroupedCard {
                NavRow(
                    icon = Icons.Rounded.Folder,
                    title = "Папка для файлов",
                    summary = folderLabel,
                    onClick = { folderPicker.launch(null) },
                )
                if (prefs.downloadDir != null) {
                    InsetDivider(startInset = 62.dp)
                    ActionRow(
                        title = "Вернуть папку Download",
                        onClick = {
                            releaseFolder(context, prefs.downloadDir, except = null)
                            vm.updateLogPrefs { it.copy(downloadDir = null) }
                        },
                    )
                }
            }
            GroupFooter(
                "Сюда кнопка «Скачать» сохраняет весь журнал (.log) и отдельные ответы сервера (.html). " +
                    "По умолчанию это системная папка Download. Если выбранная папка станет недоступна, " +
                    "файл сохранится в Download."
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("Размер журнала")
            GroupedCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Максимум записей", style = MaterialTheme.typography.bodyLarge, color = c.secondaryLabel)
                        Text("${prefs.maxEntries}", style = MaterialTheme.typography.titleMedium, color = c.accentText)
                    }
                    IosSlider(
                        value = prefs.maxEntries.toFloat(),
                        onValueChange = { v ->
                            val step = LogPrefs.ENTRIES_STEP
                            val rounded = ((v / step).roundToInt() * step)
                                .coerceIn(LogPrefs.MIN_ENTRIES, LogPrefs.MAX_ENTRIES)
                            vm.updateLogPrefs { it.copy(maxEntries = rounded) }
                        },
                        valueRange = LogPrefs.MIN_ENTRIES.toFloat()..LogPrefs.MAX_ENTRIES.toFloat(),
                    )
                }
            }
            GroupFooter(
                "Журнал хранится в памяти устройства и не стирается при перезапуске. Когда записей становится " +
                    "больше лимита, самые старые удаляются вместе с их HTML-файлами. Если уменьшить лимит, " +
                    "лишние старые записи удаляются сразу."
            )
        }
    }
}

@Composable
private fun AppearancePage(vm: MainViewModel, onBack: () -> Unit) {
    PageScaffold {
        PageHeader("Оформление", onBack)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("Тема")
            GroupedCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ThemeMode.entries.forEach { option ->
                        ThemeSwatch(
                            mode = option,
                            selected = vm.themeMode == option,
                            onClick = { vm.updateThemeMode(option) },
                        )
                    }
                }
            }
            GroupFooter("${vm.themeMode.description} Изменение применяется сразу.")
        }
    }
}

@Composable
private fun BackgroundPage(vm: MainViewModel, ignoringBattery: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val c = Ios.colors

    PageScaffold {
        PageHeader("Работа в фоне", onBack)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("Оптимизация батареи")
            if (ignoringBattery) {
                GroupedCard {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.success)
                        Text(
                            "Оптимизация батареи отключена",
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.label,
                        )
                    }
                }
            } else {
                IosButton(
                    text = "Отключить оптимизацию батареи",
                    icon = Icons.Rounded.BatteryChargingFull,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                .setData(Uri.parse("package:${context.packageName}"))
                        )
                    },
                    style = IosButtonStyle.Tinted,
                    tint = c.orange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GroupFooter(
                "Android может ограничивать работу приложений при выключенном экране. " +
                    "Исключите AutoCheck из оптимизации батареи, чтобы проверки не прерывались."
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GroupHeader("Автозапуск")
            GroupedCard {
                SwitchRow(
                    title = "Запускать после перезагрузки",
                    description = "После включения или перезагрузки телефона AutoCheck сам начнёт проверку, " +
                        "открывать приложение не нужно.",
                    checked = vm.autoStart,
                    onCheckedChange = vm::updateAutoStart,
                )
            }
            if (AutoStartSettings.isVendorRestricted()) {
                IosButton(
                    text = "Открыть настройки автозапуска",
                    icon = Icons.Rounded.SettingsIcon,
                    onClick = { AutoStartSettings.open(context) },
                    style = IosButtonStyle.Tinted,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            GroupFooter(
                "Работает, если добавлен хотя бы один аккаунт. " +
                    if (AutoStartSettings.isVendorRestricted()) {
                        "Ваша прошивка может блокировать автозапуск: откройте настройки выше и разрешите " +
                            "AutoCheck запускаться автоматически (на Samsung — не отправляйте приложение в «спящие»)."
                    } else {
                        "Дополнительных разрешений на этом устройстве, как правило, не нужно."
                    }
            )
        }
    }
}

// ───────────────────────── Общие элементы ─────────────────────────

/** Прокручиваемая страница с едиными отступами. */
@Composable
private fun PageScaffold(
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = LocalContentBottom.current),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        content = content,
    )
}

/** Верхняя панель вложенной страницы: круглая кнопка «Назад» и заголовок по центру. */
@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        RoundIconButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "Назад",
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Ios.colors.label,
        )
    }
}

/** Строка формы: подпись слева, поле ввода справа. */
@Composable
private fun FieldRow(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Ios.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .padding(start = 16.dp, end = if (trailing != null) 8.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = c.label,
            modifier = Modifier.width(76.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.label),
            cursorBrush = SolidColor(c.accent),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.tertiaryLabel,
                        )
                    }
                    inner()
                }
            },
        )
        if (trailing != null) trailing()
    }
}

/** Строка выбора: название, пояснение и галочка у выбранного варианта. */
@Composable
private fun OptionRow(selected: Boolean, title: String, description: String, onClick: () -> Unit) {
    val c = Ios.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressHighlight(interaction)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.label)
            Text(description, style = MaterialTheme.typography.bodySmall, color = c.secondaryLabel)
        }
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = c.accentText)
            }
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    val c = Ios.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressHighlight(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.label)
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = c.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = c.tertiaryLabel,
        )
    }
}

/** Строка-действие: голубой текст по центру, как «Сбросить» в системных настройках iOS. */
@Composable
private fun ActionRow(title: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .pressHighlight(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = Ios.colors.accentText)
    }
}

/** Отпускает сохранённое право на прежнюю папку скачивания, чтобы не копить неиспользуемые права. */
private fun releaseFolder(context: Context, old: String?, except: String?) {
    if (old == null || old == except) return
    runCatching {
        context.contentResolver.releasePersistableUriPermission(
            Uri.parse(old),
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = Ios.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressHighlight(interaction)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.label)
            Text(description, style = MaterialTheme.typography.bodySmall, color = c.secondaryLabel)
        }
        IosSwitch(checked = checked)
    }
}

// ───────────────────────── Превью тем ─────────────────────────

/** Миниатюра темы: «Светлая», «Тёмная» или половина на половину для «Как в системе». */
@Composable
private fun ThemeSwatch(mode: ThemeMode, selected: Boolean, onClick: () -> Unit) {
    val c = Ios.colors
    val shape = RoundedCornerShape(18.dp)
    val ring by animateColorAsState(
        targetValue = if (selected) c.accent else c.separator,
        animationSpec = tween(180),
        label = "swatch-ring",
    )

    Column(
        modifier = Modifier
            .width(96.dp)
            .iosClickable(role = Role.RadioButton, pressedScale = 0.96f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(width = 80.dp, height = 112.dp)
                .clip(shape)
                .drawBehind {
                    when (mode) {
                        ThemeMode.LIGHT -> drawMiniScreen(dark = false, from = 0f, to = size.width)
                        ThemeMode.DARK -> drawMiniScreen(dark = true, from = 0f, to = size.width)
                        ThemeMode.SYSTEM -> {
                            drawMiniScreen(dark = false, from = 0f, to = size.width / 2f)
                            drawMiniScreen(dark = true, from = size.width / 2f, to = size.width)
                        }
                    }
                }
                .border(if (selected) 3.dp else 1.dp, ring, shape)
        )
        Text(
            text = mode.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) c.accentText else c.secondaryLabel,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** Рисует упрощённый экран приложения в светлой или тёмной теме в пределах [from]..[to] по горизонтали. */
private fun DrawScope.drawMiniScreen(dark: Boolean, from: Float, to: Float) {
    val bg = if (dark) Color(0xFF000000) else Color(0xFFF2F2F7)
    val card = if (dark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val line = if (dark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    val w = size.width
    val h = size.height

    clipRect(left = from, top = 0f, right = to, bottom = h) {
        drawRect(bg)
        // Главная карточка — оранжевая
        drawRoundRect(
            color = Brand.Orange,
            topLeft = Offset(w * 0.10f, h * 0.12f),
            size = Size(w * 0.80f, h * 0.30f),
            cornerRadius = CornerRadius(10.dp.toPx()),
        )
        // Белая/чёрная карточка со строками
        drawRoundRect(
            color = card,
            topLeft = Offset(w * 0.10f, h * 0.48f),
            size = Size(w * 0.80f, h * 0.26f),
            cornerRadius = CornerRadius(9.dp.toPx()),
        )
        drawRoundRect(
            color = line,
            topLeft = Offset(w * 0.18f, h * 0.55f),
            size = Size(w * 0.50f, 4.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        drawRoundRect(
            color = line,
            topLeft = Offset(w * 0.18f, h * 0.63f),
            size = Size(w * 0.36f, 4.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        // Панель вкладок с голубым акцентом
        drawRoundRect(
            color = card,
            topLeft = Offset(w * 0.14f, h * 0.84f),
            size = Size(w * 0.72f, h * 0.10f),
            cornerRadius = CornerRadius(h * 0.05f),
        )
        drawRoundRect(
            color = Brand.Sky,
            topLeft = Offset(w * 0.18f, h * 0.855f),
            size = Size(w * 0.22f, h * 0.07f),
            cornerRadius = CornerRadius(h * 0.035f),
        )
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

private fun areNotificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

private fun openNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )
}
