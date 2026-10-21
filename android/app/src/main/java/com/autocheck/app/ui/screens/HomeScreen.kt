package com.autocheck.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autocheck.app.data.AutoClickState
import com.autocheck.app.data.EngineStatus
import com.autocheck.app.data.LogEntry
import com.autocheck.app.data.LogLevel
import com.autocheck.app.data.Stats
import com.autocheck.app.data.displayTime
import com.autocheck.app.ui.components.Banner
import com.autocheck.app.ui.components.CardShape
import com.autocheck.app.ui.components.GroupHeader
import com.autocheck.app.ui.components.GroupedCard
import com.autocheck.app.ui.components.InsetDivider
import com.autocheck.app.ui.components.LargeTitle
import com.autocheck.app.ui.components.LinkText
import com.autocheck.app.ui.components.iosClickable
import com.autocheck.app.ui.components.pressHighlight
import com.autocheck.app.ui.theme.Brand
import com.autocheck.app.ui.theme.Ios
import com.autocheck.app.ui.theme.MonoStyle
import com.autocheck.app.ui.theme.color

private data class HeroStyle(
    val bg: Color,
    val title: Color,
    val subtitle: Color,
    val button: Color,
    val buttonIcon: Color,
)

@Composable
private fun heroStyle(status: EngineStatus): HeroStyle {
    val c = Ios.colors
    return when (status) {
        EngineStatus.STOPPED -> HeroStyle(
            bg = c.card,
            title = c.label,
            subtitle = c.secondaryLabel,
            button = c.orange,
            buttonIcon = Color.White,
        )
        EngineStatus.CONNECTING -> HeroStyle(
            bg = c.accent,
            title = Color.White,
            subtitle = Color.White.copy(alpha = 0.88f),
            button = Color.White,
            buttonIcon = c.accent,
        )
        EngineStatus.RUNNING -> HeroStyle(
            bg = c.orange,
            title = Color.White,
            subtitle = Color.White.copy(alpha = 0.88f),
            button = Color.White,
            buttonIcon = c.orange,
        )
        EngineStatus.ERROR -> HeroStyle(
            bg = c.card,
            title = c.danger,
            subtitle = c.secondaryLabel,
            button = c.orange,
            buttonIcon = Color.White,
        )
    }
}

private fun EngineStatus.title() = when (this) {
    EngineStatus.STOPPED -> "Остановлено"
    EngineStatus.CONNECTING -> "Подключение…"
    EngineStatus.RUNNING -> "Работает"
    EngineStatus.ERROR -> "Ошибка"
}

@Composable
fun HomeScreen(
    status: EngineStatus,
    stats: Stats,
    recentLogs: List<LogEntry>,
    timeoutSec: Int,
    hasCredentials: Boolean,
    bottomInset: Dp,
    resetSignal: Int,
    onToggle: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToMap: (String) -> Unit = {},
) {
    val scroll = rememberScrollState()
    val currentGroup by AutoClickState.currentGroup.collectAsStateWithLifecycle()
    val scheduleData by AutoClickState.schedule.collectAsStateWithLifecycle()
    var searchInput by remember { mutableStateOf("") }

    var showGroupDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showSubjectsDialog by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }
    var unreadNotifications by remember { mutableStateOf(70) }

    LaunchedEffect(resetSignal) { if (resetSignal > 0) scroll.animateScrollTo(0) }

    // Извлечение первой/ближайшей пары из расписания
    val todayLessons = scheduleData?.days?.firstOrNull()?.lessons ?: emptyList()
    val nextLesson = todayLessons.firstOrNull()

    val nextLessonName = nextLesson?.name ?: "Криптографические методы защиты информации"
    val nextLessonAud = nextLesson?.auditorium?.ifBlank { "521/2" } ?: "521/2"
    val nextLessonTeacher = nextLesson?.teacher?.ifBlank { "Шиян Павел Анатольевич" } ?: "Шиян Павел Анатольевич"
    val nextLessonType = nextLesson?.type?.ifBlank { "Практические занятия • пара 1" } ?: "Практические занятия • пара 1"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(bottom = bottomInset)
    ) {
        // ─────────────────────────────────────────────────────────────
        // 1. Верхний градиентный заголовок (Скриншот 1:1)
        // ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE65100),
                            Color(0xFFBF360C).copy(alpha = 0.85f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showGroupDialog = true }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Четверг, 22 октября • $currentGroup",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.95f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.Groups,
                                contentDescription = "Сменить группу",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Привет! 👋",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Колокольчик с бейджем уведомлений
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showNotificationsDialog = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Notifications,
                                    contentDescription = "Уведомления",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            if (unreadNotifications > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE53935))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$unreadNotifications",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Аватар / Личный кабинет
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF5350))
                                .clickable { onOpenSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.School,
                                contentDescription = "Профиль",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Поисковая капсула
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSearchDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Группа, преподаватель, аудитория...",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ─────────────────────────────────────────────────────────────
            // 2. Большая карточка ближайшей пары (Скриншот 1:1)
            // ─────────────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F261E)),
                border = BorderStroke(1.dp, Color(0xFF1B4332))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Тег типа занятия и таймер
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1B4332))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = nextLessonType,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF74C69D)
                            )
                        }

                        Text(
                            text = "через 8 ч 33 мин",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF74C69D)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = nextLessonName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Линия времени
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "09:00",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(Color(0xFF1B4332))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "10:35",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Чипы: Аудитория и Преподаватель
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF16382C))
                                .clickable { onNavigateToMap(nextLessonAud) }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Navigation,
                                    contentDescription = null,
                                    tint = Color(0xFF52B788),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = nextLessonAud,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF16382C))
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "👤 $nextLessonTeacher",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Большая яркая оранжевая кнопка «Проложить маршрут»
                    Button(
                        onClick = { onNavigateToMap(nextLessonAud) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00))
                    ) {
                        Icon(
                            Icons.Rounded.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Проложить маршрут",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Статус ожидания
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF16382C).copy(alpha = 0.7f))
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF74C69D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ждём преподавателя",
                                color = Color(0xFFD8F3DC),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 3. Раздел «Быстрый доступ» (Скриншот 1:1)
            // ─────────────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Быстрый доступ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Карточка «Личный кабинет»
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSettings() },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Ios.colors.card),
                    border = BorderStroke(1.dp, Ios.colors.separator)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Личный кабинет",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ios.colors.label
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF6D00)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 4 плитки быстрого доступа
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Мультигруппы
                    QuickTile(
                        title = "Мультигруппы",
                        icon = Icons.Rounded.Groups,
                        iconTint = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    ) { showGroupDialog = true }

                    // Аудитории (Переход в карту)
                    QuickTile(
                        title = "Аудитории",
                        icon = Icons.Rounded.MeetingRoom,
                        iconTint = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    ) { onNavigateToMap("") }

                    // Предметы
                    QuickTile(
                        title = "Предметы",
                        icon = Icons.Rounded.MenuBook,
                        iconTint = Color(0xFFA78BFA),
                        modifier = Modifier.weight(1f)
                    ) { showSubjectsDialog = true }

                    // Избранное
                    QuickTile(
                        title = "Избранное",
                        icon = Icons.Rounded.Star,
                        iconTint = Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    ) { showFavoritesDialog = true }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 4. Карточка фоновой службы синхронизации (Hero Engine)
            // ─────────────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Автоматизация",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Hero(
                    status = status,
                    subtitle = when (status) {
                        EngineStatus.STOPPED -> "Нажмите, чтобы включить автоматизацию"
                        EngineStatus.CONNECTING -> "Выполняется вход в личный кабинет"
                        EngineStatus.RUNNING -> "Проверка расписания каждые $timeoutSec с"
                        EngineStatus.ERROR -> "Подробности в журнале"
                    },
                    enabled = hasCredentials || status != EngineStatus.STOPPED,
                    onToggle = onToggle,
                )

                if (!hasCredentials) {
                    Banner(
                        text = "Добавьте аккаунт от lk.sut.ru в настройках",
                        onClick = onOpenSettings,
                    )
                }

                StatsCard(stats)
            }

            // ─────────────────────────────────────────────────────────────
            // 5. Последние события журнала
            // ─────────────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    GroupHeader("Последние события")
                    LinkText("Весь журнал", onClick = onOpenLog)
                }
                if (recentLogs.isEmpty()) {
                    GroupedCard {
                        Text(
                            "Здесь появятся события после запуска",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ios.colors.secondaryLabel,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                        )
                    }
                } else {
                    GroupedCard {
                        recentLogs.forEachIndexed { index, entry ->
                            if (index > 0) InsetDivider(startInset = 38.dp)
                            LogRow(entry)
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ДИАЛОГ 1: Выбор учебной группы (Мультигруппы)
    // ─────────────────────────────────────────────────────────────
    if (showGroupDialog) {
        var customGroupText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Выбор учебной группы", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Text(
                        text = "Текущая группа: $currentGroup",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Популярные группы СПбГУТ:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ios.colors.secondaryLabel
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        GroupScheduleProvider.POPULAR_GROUPS.forEach { g ->
                            val isSelected = g.equals(currentGroup, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) Color(0xFFFF9800)
                                        else Ios.colors.card
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFFFF9800) else Ios.colors.separator,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        AutoClickState.setGroup(g)
                                        showGroupDialog = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = g,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Ios.colors.label
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Или введите произвольную группу:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ios.colors.secondaryLabel
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customGroupText,
                        onValueChange = { customGroupText = it },
                        placeholder = { Text("Например: ИКВТ-31, ИСТ-11") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customGroupText.isNotBlank()) {
                            AutoClickState.setGroup(customGroupText)
                        }
                        showGroupDialog = false
                    }
                ) {
                    Text("Применить", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDialog = false }) {
                    Text("Отмена", color = Ios.colors.secondaryLabel)
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────
    // ДИАЛОГ 2: Уведомления ЛК Бонч (Колокольчик)
    // ─────────────────────────────────────────────────────────────
    if (showNotificationsDialog) {
        val notifications = listOf(
            Triple(
                "Автоматическая отметка",
                "Отметка посещаемости успешно выставлена на занятие «$nextLessonName» (ауд. $nextLessonAud).",
                "10 мин назад"
            ),
            Triple(
                "Расписание обновлено",
                "Расписание для группы $currentGroup синхронизировано с базой расписания СПбГУТ.",
                "1 час назад"
            ),
            Triple(
                "Деканат СПбГУТ",
                "Начался прием документов на материальную помощь и повышенную государственную стипендию.",
                "Вчера"
            ),
            Triple(
                "Навигатор кампуса",
                "Доступна векторная карта коридоров 1–7 этажей и переходов между корпусами.",
                "2 дня назад"
            ),
            Triple(
                "Сессия авторизации",
                "Связь с личным кабинетом lk.sut.ru активна.",
                "3 дня назад"
            )
        )

        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = Color(0xFFFF6D00),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Уведомления ЛК", fontWeight = FontWeight.Bold)
                    }
                    if (unreadNotifications > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE53935))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$unreadNotifications новых",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications) { (title, body, time) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Ios.colors.card),
                            border = BorderStroke(1.dp, Ios.colors.separator)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800)
                                    )
                                    Text(
                                        text = time,
                                        fontSize = 11.sp,
                                        color = Ios.colors.tertiaryLabel
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = body,
                                    fontSize = 12.sp,
                                    color = Ios.colors.label,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        unreadNotifications = 0
                        showNotificationsDialog = false
                    }
                ) {
                    Icon(
                        Icons.Rounded.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Прочитать все", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Закрыть", color = Ios.colors.secondaryLabel)
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────
    // ДИАЛОГ 3: Быстрый поиск (Аудитории, преподаватели, группы)
    // ─────────────────────────────────────────────────────────────
    if (showSearchDialog) {
        var query by remember { mutableStateOf("") }

        val allRooms = listOf(
            "521/2" to "Лаборатория информационной безопасности • 2к, 5 эт.",
            "501/1" to "Поточная лекционная аудитория • 1к, 5 эт.",
            "334/1" to "Кафедра ИБТС • 1к, 3 эт.",
            "200/1" to "Актовый зал СПбГУТ • 1к, 2 эт.",
            "101/1" to "Студенческая столовая • 1к, 1 эт.",
            "312/1" to "Семинарская аудитория • 1к, 3 эт.",
            "601/1" to "Лекционная аудитория • 1к, 6 эт.",
            "Библиотека" to "Читальный зал • 1к, 2 эт.",
            "Гардероб" to "Главный холл входа • 1к, 1 эт."
        )

        val allTeachers = listOf(
            "Шиян Павел Анатольевич" to "521/2",
            "Карлов Андрей Владимирович" to "501/1",
            "Кучерявый Андрей Евгеньевич" to "200/1",
            "Смирнов Дмитрий Юрьевич" to "332/1",
            "Волков Денис Игоревич" to "312/1"
        )

        val filteredRooms = remember(query) {
            if (query.isBlank()) allRooms
            else allRooms.filter { it.first.contains(query, ignoreCase = true) || it.second.contains(query, ignoreCase = true) }
        }

        val filteredGroups = remember(query) {
            if (query.isBlank()) GroupScheduleProvider.POPULAR_GROUPS
            else GroupScheduleProvider.POPULAR_GROUPS.filter { it.contains(query, ignoreCase = true) }
        }

        val filteredTeachers = remember(query) {
            if (query.isBlank()) allTeachers
            else allTeachers.filter { it.first.contains(query, ignoreCase = true) }
        }

        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = {
                Text("Быстрый поиск СПбГУТ", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Аудитория, группа или преподаватель...") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFFFF9800))
                        },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Очистить",
                                    tint = Ios.colors.secondaryLabel,
                                    modifier = Modifier.clickable { query = "" }
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Блок найденных аудиторий
                        if (filteredRooms.isNotEmpty()) {
                            item {
                                Text(
                                    text = "АУДИТОРИИ И МЕСТА",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800),
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                )
                            }
                            items(filteredRooms) { (room, desc) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Ios.colors.card)
                                        .clickable {
                                            showSearchDialog = false
                                            onNavigateToMap(room)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = room, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = desc, fontSize = 11.sp, color = Ios.colors.secondaryLabel)
                                    }
                                    Text(
                                        text = "Карта",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                        }

                        // Блок найденных групп
                        if (filteredGroups.isNotEmpty()) {
                            item {
                                Text(
                                    text = "УЧЕБНЫЕ ГРУППЫ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                            items(filteredGroups) { grp ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Ios.colors.card)
                                        .clickable {
                                            AutoClickState.setGroup(grp)
                                            showSearchDialog = false
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Groups,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = grp, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text(
                                        text = "Выбрать",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }

                        // Блок найденных преподавателей
                        if (filteredTeachers.isNotEmpty()) {
                            item {
                                Text(
                                    text = "ПРЕПОДАВАТЕЛИ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA78BFA),
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                            items(filteredTeachers) { (teacher, aud) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Ios.colors.card)
                                        .clickable {
                                            showSearchDialog = false
                                            onNavigateToMap(aud)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = Color(0xFFA78BFA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = teacher, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "Кабинет $aud", fontSize = 11.sp, color = Ios.colors.secondaryLabel)
                                    }
                                    Text(
                                        text = "Найти",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text("Закрыть", color = Color(0xFFFF9800))
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────
    // ДИАЛОГ 4: Предметы группы (Быстрый доступ)
    // ─────────────────────────────────────────────────────────────
    if (showSubjectsDialog) {
        val distinctLessons = remember(scheduleData) {
            val list = scheduleData?.days?.flatMap { it.lessons } ?: emptyList()
            list.distinctBy { it.name }
        }

        AlertDialog(
            onDismissRequest = { showSubjectsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Дисциплины: $currentGroup", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (distinctLessons.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет данных о дисциплинах", color = Ios.colors.secondaryLabel)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(distinctLessons) { lesson ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Ios.colors.card),
                                border = BorderStroke(1.dp, Ios.colors.separator)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = lesson.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Ios.colors.label
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "👤 ${lesson.teacher.ifBlank { "Преподаватель кафедры" }}",
                                        fontSize = 12.sp,
                                        color = Ios.colors.secondaryLabel
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = lesson.type,
                                            fontSize = 11.sp,
                                            color = Color(0xFFA78BFA)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFF6D00).copy(alpha = 0.2f))
                                                .clickable {
                                                    showSubjectsDialog = false
                                                    onNavigateToMap(lesson.auditorium)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Rounded.Navigation,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFF6D00),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Ауд. ${lesson.auditorium}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF6D00)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubjectsDialog = false }) {
                    Text("Закрыть", color = Color(0xFFFF9800))
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────
    // ДИАЛОГ 5: Избранные места и аудитории (Быстрый доступ)
    // ─────────────────────────────────────────────────────────────
    if (showFavoritesDialog) {
        val favorites = listOf(
            Triple("521/2", "Лаборатория защиты информации (2к, 5 эт.)", Icons.Rounded.MeetingRoom),
            Triple("501/1", "Главная поточная лекционная (1к, 5 эт.)", Icons.Rounded.MeetingRoom),
            Triple("334/1", "Кафедра ИБТС (1к, 3 эт.)", Icons.Rounded.School),
            Triple("200/1", "Актовый зал СПбГУТ (1к, 2 эт.)", Icons.Rounded.MeetingRoom),
            Triple("101/1", "Студенческая столовая (1к, 1 эт.)", Icons.Rounded.Bookmark),
            Triple("Библиотека", "Читальный зал и абонемент (1к, 2 эт.)", Icons.Rounded.MenuBook),
            Triple("Гардероб", "Центральный гардероб входа (1к, 1 эт.)", Icons.Rounded.Bookmark),
            Triple("Переход", "Крытый надземный мост между 1к и 2к (3 эт.)", Icons.Rounded.Navigation)
        )

        AlertDialog(
            onDismissRequest = { showFavoritesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Избранные места СПбГУТ", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favorites) { (name, desc, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Ios.colors.card)
                                .clickable {
                                    showFavoritesDialog = false
                                    onNavigateToMap(name)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFBBF24).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ios.colors.label
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = Ios.colors.secondaryLabel
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF9800))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Маршрут",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFavoritesDialog = false }) {
                    Text("Закрыть", color = Color(0xFFFF9800))
                }
            }
        )
    }
}

@Composable
private fun QuickTile(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Ios.colors.card),
        border = BorderStroke(1.dp, Ios.colors.separator)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Ios.colors.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Hero(
    status: EngineStatus,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val target = heroStyle(status)
    val spec = tween<Color>(400)
    val bg by animateColorAsState(target.bg, spec, label = "bg")
    val titleColor by animateColorAsState(target.title, spec, label = "title")
    val subtitleColor by animateColorAsState(target.subtitle, spec, label = "subtitle")
    val buttonColor by animateColorAsState(target.button, spec, label = "button")
    val buttonIcon by animateColorAsState(target.buttonIcon, spec, label = "buttonIcon")

    val active = status == EngineStatus.RUNNING || status == EngineStatus.CONNECTING

    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "scale",
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.35f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .padding(vertical = 24.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = status.title(),
                    style = MaterialTheme.typography.titleLarge,
                    color = titleColor,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                )
            }

            Spacer(Modifier.width(16.dp))

            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (active) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = ringAlpha
                            }
                            .clip(CircleShape)
                            .background(buttonColor),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(buttonColor)
                        .iosClickable(
                            enabled = enabled,
                            role = Role.Button,
                            pressedScale = 0.92f,
                            onClick = onToggle,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (active) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = if (active) "Остановить" else "Запустить",
                        tint = buttonIcon,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(stats: Stats) {
    GroupedCard {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem(label = "Проверок", value = stats.cycles.toString())
            StatItem(label = "Начато занятий", value = stats.started.toString(), accent = true)
            StatItem(
                label = "Последняя",
                value = stats.lastCheck ?: "—",
            )
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, accent: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (accent) Ios.colors.orange else Ios.colors.label,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Ios.colors.secondaryLabel,
        )
    }
}

@Composable
fun LogRow(
    entry: LogEntry,
    modifier: Modifier = Modifier,
    onOpenResponse: ((LogEntry) -> Unit)? = null,
) {
    val c = Ios.colors
    val openable = onOpenResponse != null && entry.responseFile != null
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (openable) {
                    Modifier
                        .pressHighlight(interaction)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "Открыть ответ сервера",
                            onClick = { onOpenResponse?.invoke(entry) },
                        )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(10.dp)
                .background(if (entry.responseFile != null) Brand.Amber else entry.level.color(), CircleShape)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                entry.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (entry.level == LogLevel.ERROR) c.danger else c.label,
                fontWeight = when (entry.level) {
                    LogLevel.SUCCESS, LogLevel.ERROR -> FontWeight.Medium
                    else -> null
                },
            )
            Text(entry.displayTime(), style = MonoStyle, color = c.secondaryLabel)
        }
        if (openable) {
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("HTML", style = MaterialTheme.typography.labelMedium, color = c.accentText)
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = c.tertiaryLabel,
                )
            }
        }
    }
}
