package com.autocheck.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autocheck.app.data.AutoClickState
import com.autocheck.app.data.DayScheduleItem
import com.autocheck.app.data.GroupScheduleProvider
import com.autocheck.app.data.LessonItem
import com.autocheck.app.data.SettingsStore
import com.autocheck.app.data.SutClient
import com.autocheck.app.data.WeekScheduleData
import com.autocheck.app.ui.theme.Ios
import kotlinx.coroutines.launch

@Composable
fun ScheduleScreen(
    bottomPadding: Dp,
    onNavigateToMap: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentGroup by AutoClickState.currentGroup.collectAsStateWithLifecycle()
    val rawScheduleData by AutoClickState.schedule.collectAsStateWithLifecycle()
    val scheduleData = rawScheduleData ?: remember(currentGroup) { GroupScheduleProvider.getScheduleForGroup(currentGroup) }

    var selectedViewTab by remember { mutableIntStateOf(0) } // 0: День, 1: Неделя, 2: Список
    var selectedDayIndex by remember { mutableIntStateOf(3) } // По умолчанию Четверг (индекс 3)
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var userNoteText by remember { mutableStateOf("Лабораторная по сетям: сдать отчет к пятнице.\nПодготовить доклад по защите ОС.") }

    fun refreshSchedule() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val store = SettingsStore(context)
                val account = store.loadAccounts().firstOrNull { it.isComplete }
                if (account == null) {
                    // Перезагружаем расписание для текущей выбранной группы
                    AutoClickState.updateSchedule(GroupScheduleProvider.getScheduleForGroup(currentGroup))
                    return@launch
                }
                val client = SutClient()
                if (account.type == com.autocheck.app.data.AccountType.TOKEN) {
                    client.useToken(account.token)
                } else {
                    client.login(account.login, account.password)
                }
                val data = client.fetchDetailedSchedule()
                AutoClickState.updateSchedule(data)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка синхронизации с личным кабинетом"
            } finally {
                isLoading = false
            }
        }
    }

    val days = scheduleData.days
    val currentDay = days.getOrNull(selectedDayIndex.coerceIn(0, (days.size - 1).coerceAtLeast(0)))
    val lessons = currentDay?.lessons ?: emptyList()

    val practicalCount = lessons.count { it.type.contains("практик", ignoreCase = true) || it.type.contains("лаб", ignoreCase = true) }
    val lectureCount = lessons.count { it.type.contains("лекци", ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
    ) {
        // ─────────────────────────────────────────────────────────────
        // 1. Верхний заголовок: Группа (кликабельна) и иконки действий
        // ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
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
                        text = currentGroup,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "▼",
                        fontSize = 14.sp,
                        color = Color(0xFFFF9800)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "19 октября — 24 октября • Неделя ${scheduleData.weekNumber}",
                    fontSize = 14.sp,
                    color = Ios.colors.secondaryLabel
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Мультигруппы
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Ios.colors.card)
                        .clickable { showGroupDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Groups,
                        contentDescription = "Сменить группу",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Заметки
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Ios.colors.card)
                        .clickable { showNotesDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.FormatListBulleted,
                        contentDescription = "Заметки",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Календарь / Синхронизация
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Ios.colors.card)
                        .clickable { refreshSchedule() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF9800),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = "Синхронизировать",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 2. Переключатель режима отображения: День | Неделя | Список
        // ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("День", "Неделя", "Список").forEachIndexed { index, title ->
                val isActive = selectedViewTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isActive) Color(0xFFFF6D00) else Ios.colors.card)
                        .border(
                            1.dp,
                            if (isActive) Color(0xFFFF6D00) else Ios.colors.separator,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedViewTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) Color.White else Ios.colors.label
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─────────────────────────────────────────────────────────────
        // РЕЖИМ 0: ДЕНЬ (Скриншот 1:1 с каруселью и таймлайном)
        // ─────────────────────────────────────────────────────────────
        if (selectedViewTab == 0) {
            // Карусель дней недели
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Назад",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            if (selectedDayIndex > 0) selectedDayIndex--
                        }
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEachIndexed { index, day ->
                        val isSelected = index == selectedDayIndex
                        val dayShort = when (day.weekday) {
                            "Понедельник" -> "Пн"
                            "Вторник" -> "Вт"
                            "Среда" -> "Ср"
                            "Четверг" -> "Чт"
                            "Пятница" -> "Пт"
                            "Суббота" -> "Сб"
                            else -> day.weekday.take(2)
                        }

                        val dayNumber = day.date.filter { it.isDigit() }.takeLast(2).ifBlank { "${19 + index}" }
                        val dayLessonCount = day.lessons.size

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) Color(0xFF1B4332)
                                    else Ios.colors.card
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF52B788) else Ios.colors.separator,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedDayIndex = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayShort,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF74C69D) else Ios.colors.secondaryLabel
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dayNumber,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Ios.colors.label
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (isSelected) Color.White
                                            else if (dayLessonCount == 0) Color.Transparent
                                            else Color(0xFF52B788)
                                        )
                                )
                            }
                        }
                    }
                }

                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Вперед",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            if (selectedDayIndex < days.size - 1) selectedDayIndex++
                        }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Сводка: «≡ Практик X • Лекций Y»
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "≡ Практик $practicalCount • Лекций $lectureCount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ios.colors.secondaryLabel
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Временная шкала (Timeline) со списком пар
            if (lessons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "В этот день занятий нет 🎉",
                        fontSize = 16.sp,
                        color = Ios.colors.secondaryLabel
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(lessons) { index, lesson ->
                        LessonRowTimeline(
                            index = index,
                            lesson = lesson,
                            onNavigateToMap = onNavigateToMap
                        )
                    }
                }
            }
        }
        // ─────────────────────────────────────────────────────────────
        // РЕЖИМ 1: НЕДЕЛЯ (Полное расписание на 6 дней)
        // ─────────────────────────────────────────────────────────────
        else if (selectedViewTab == 1) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(days) { day ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Ios.colors.card),
                        border = BorderStroke(1.dp, Ios.colors.separator)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${day.weekday} (${day.date})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                                Text(
                                    text = "${day.lessons.size} пар(ы)",
                                    fontSize = 12.sp,
                                    color = Ios.colors.secondaryLabel
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            if (day.lessons.isEmpty()) {
                                Text(
                                    text = "Выходной день",
                                    fontSize = 13.sp,
                                    color = Ios.colors.tertiaryLabel
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    day.lessons.forEachIndexed { i, l ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Ios.colors.fill)
                                                .clickable { onNavigateToMap(l.auditorium) }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${i + 1}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color(0xFFFF9800),
                                                modifier = Modifier.width(20.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = l.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Ios.colors.label,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${l.time} • ${l.teacher}",
                                                    fontSize = 12.sp,
                                                    color = Ios.colors.secondaryLabel,
                                                    maxLines = 1
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFF6D00).copy(alpha = 0.2f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = l.auditorium.ifBlank { "Кабинет" },
                                                    fontSize = 12.sp,
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
            }
        }
        // ─────────────────────────────────────────────────────────────
        // РЕЖИМ 2: СПИСОК (Хронологический список всех занятий)
        // ─────────────────────────────────────────────────────────────
        else {
            val allWeekLessons = days.flatMap { day -> day.lessons.map { day to it } }
            if (allWeekLessons.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Расписание пусто", color = Ios.colors.secondaryLabel)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allWeekLessons) { (day, lesson) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToMap(lesson.auditorium) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Ios.colors.card),
                            border = BorderStroke(1.dp, Ios.colors.separator)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(72.dp)) {
                                    Text(
                                        text = day.weekday.take(2),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFFF9800)
                                    )
                                    Text(
                                        text = lesson.time.split("—").firstOrNull()?.trim() ?: "",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Ios.colors.label
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = lesson.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Ios.colors.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = lesson.teacher,
                                        fontSize = 12.sp,
                                        color = Ios.colors.secondaryLabel,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1B4332))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = lesson.auditorium.ifBlank { "вуз" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF74C69D)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Диалог переключения группы
        // ─────────────────────────────────────────────────────────────
        if (showGroupDialog) {
            var customGroupText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showGroupDialog = false },
                title = { Text("Выбор учебной группы", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    ) {
                        Text(
                            text = "Выберите из частых групп или введите свой номер группы:",
                            fontSize = 13.sp,
                            color = Ios.colors.secondaryLabel,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Быстрые чипы популярных групп
                        Text(
                            text = "Популярные группы СПбГУТ:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
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
                                        .background(if (isSelected) Color(0xFFFF6D00) else Ios.colors.card)
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFFF6D00) else Ios.colors.separator,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            AutoClickState.setGroup(g)
                                            showGroupDialog = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
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

                        // Ввод произвольной группы
                        OutlinedTextField(
                            value = customGroupText,
                            onValueChange = { customGroupText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Например: ИКПИ-21, ИСТ-11...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF6D00),
                                unfocusedBorderColor = Ios.colors.separator
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customGroupText.isNotBlank()) {
                                AutoClickState.setGroup(customGroupText.trim())
                            }
                            showGroupDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00))
                    ) {
                        Text("Применить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGroupDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }

        // ─────────────────────────────────────────────────────────────
        // Диалог заметок студента
        // ─────────────────────────────────────────────────────────────
        if (showNotesDialog) {
            AlertDialog(
                onDismissRequest = { showNotesDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FormatListBulleted, contentDescription = null, tint = Color(0xFFFF9800))
                        Spacer(Modifier.width(8.dp))
                        Text("Заметки и задачи", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        OutlinedTextField(
                            value = userNoteText,
                            onValueChange = { userNoteText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF6D00),
                                unfocusedBorderColor = Ios.colors.separator
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showNotesDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00))
                    ) {
                        Text("Сохранить")
                    }
                }
            )
        }
    }
}

@Composable
private fun LessonRowTimeline(
    index: Int,
    lesson: LessonItem,
    onNavigateToMap: (String) -> Unit
) {
    val pairNumber = index + 1
    val isLecture = lesson.type.contains("лекци", ignoreCase = true)
    val cardBg = if (isLecture) Color(0xFF0F1E2E) else Color(0xFF0F261E)
    val badgeBg = if (isLecture) Color(0xFF1B3654) else Color(0xFF1B4332)
    val badgeTextColor = if (isLecture) Color(0xFF70C1FF) else Color(0xFF74C69D)
    val circleBg = if (isLecture) Color(0xFF64B5F6) else Color(0xFF52B788)

    val times = lesson.time.split("—", "-").map { it.trim() }
    val startTime = times.getOrNull(0) ?: "09:00"
    val endTime = times.getOrNull(1) ?: "10:35"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Левая шкала: Кружок с номером пары и временем начала/конца
        Column(
            modifier = Modifier.width(54.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(circleBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$pairNumber",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0E1015)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = startTime,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Ios.colors.label
            )

            // Вертикальная соединительная линия
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(34.dp)
                    .background(circleBg.copy(alpha = 0.5f))
            )

            Text(
                text = endTime,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Ios.colors.secondaryLabel
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Правая карточка пары
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable { onNavigateToMap(lesson.auditorium) },
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, badgeBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Тег типа занятия
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = lesson.type.ifBlank { if (isLecture) "Лекция" else "Практические занятия" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeTextColor
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Название предмета
                Text(
                    text = lesson.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Преподаватель
                if (lesson.teacher.isNotBlank()) {
                    Text(
                        text = "👤 ${lesson.teacher}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Нижняя панель: Аудитория с кнопкой «На карте»
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(badgeBg)
                            .clickable { onNavigateToMap(lesson.auditorium) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Navigation,
                                contentDescription = null,
                                tint = badgeTextColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lesson.auditorium.ifBlank { "Назначить" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNavigateToMap(lesson.auditorium) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "На карте",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF9800)
                        )
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
