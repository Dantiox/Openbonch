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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.autocheck.app.data.LessonItem
import com.autocheck.app.data.SettingsStore
import com.autocheck.app.data.SutClient
import com.autocheck.app.data.WeekScheduleData
import com.autocheck.app.ui.theme.Ios
import kotlinx.coroutines.launch

// Образцовое расписание (на случай, если пользователь ещё не залогинился в lk.sut.ru)
private val DEMO_SCHEDULE = WeekScheduleData(
    weekNumber = "7",
    days = listOf(
        DayScheduleItem("Пн", "19.10", listOf(
            LessonItem("09:00 - 10:35", "Физика", "Смирнов А. В.", "320/1", "Лекция"),
            LessonItem("10:45 - 12:20", "Высшая математика", "Кузнецова Е. И.", "412/2", "Практические занятия")
        )),
        DayScheduleItem("Вт", "20.10", listOf(
            LessonItem("09:00 - 10:35", "Иностранный язык", "Иванова О. П.", "215/2", "Практические занятия"),
            LessonItem("10:45 - 12:20", "Информатика", "Шиян Андрей Анатольевич", "501/1", "Лекция")
        )),
        DayScheduleItem("Ср", "21.10", emptyList()),
        DayScheduleItem("Чт", "22.10", listOf(
            LessonItem("09:00 - 10:35", "Информатика", "Шиян Павел Анатольевич", "521/2", "Практические занятия"),
            LessonItem("10:45 - 12:20", "Информатика", "Шиян Андрей Анатольевич", "501/1", "Лекция"),
            LessonItem("13:00 - 14:35", "История России", "Руденко Егор", "356/1", "Практические занятия"),
            LessonItem("14:45 - 16:20", "История России", "Руденко Егор", "356/1", "Практические занятия")
        )),
        DayScheduleItem("Пт", "23.10", listOf(
            LessonItem("10:45 - 12:20", "Экология", "Морозов В. С.", "202/1", "Лекция"),
            LessonItem("13:00 - 14:35", "Физическая культура", "Кафедра ФК", "Спортзал", "Практические занятия")
        )),
        DayScheduleItem("Сб", "24.10", listOf(
            LessonItem("09:00 - 10:35", "Дискретная математика", "Ковалев Д. М.", "340/2", "Практические занятия")
        ))
    )
)

@Composable
fun ScheduleScreen(
    bottomPadding: Dp,
    onNavigateToMap: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rawScheduleData by AutoClickState.schedule.collectAsStateWithLifecycle()
    val scheduleData = rawScheduleData ?: DEMO_SCHEDULE

    var selectedViewTab by remember { mutableIntStateOf(0) } // 0: День, 1: Неделя, 2: Список
    var selectedDayIndex by remember { mutableIntStateOf(3) } // По умолчанию Четверг (индекс 3)
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshSchedule() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val store = SettingsStore(context)
                val account = store.loadAccounts().firstOrNull { it.isComplete }
                if (account == null) {
                    errorMessage = "Войдите в личный кабинет в разделе Настройки"
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
                errorMessage = e.message ?: "Ошибка загрузки расписания"
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
        // 1. Верхний заголовок: Группа и иконки действий (Скриншот 1:1)
        // ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ИБТС-2613",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "19 октября — 24 октября",
                    fontSize = 14.sp,
                    color = Ios.colors.secondaryLabel
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    Icons.Rounded.Groups,
                    contentDescription = "Мультигруппы",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    Icons.Rounded.FormatListBulleted,
                    contentDescription = "Заметки",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = "Календарь",
                    tint = Color(0xFFFF9800),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { refreshSchedule() }
                )
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
                        .background(if (isActive) Ios.colors.card else Color.Transparent)
                        .border(
                            1.dp,
                            if (isActive) Ios.colors.separator else Color.Transparent,
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
                        color = if (isActive) Ios.colors.label else Ios.colors.secondaryLabel
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ─────────────────────────────────────────────────────────────
        // 3. Карусель дней недели со столбиками пар (Скриншот 1:1)
        // ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.rounded.KeyboardArrowLeft,
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
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEachIndexed { index, day ->
                    val isSelected = selectedDayIndex == index
                    val dayLessonCount = day.lessons.size

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color(0xFFFF6D00) else Ios.colors.card)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFFFF6D00) else Ios.colors.separator,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { selectedDayIndex = index }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.weekday,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Ios.colors.label
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = day.date,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else Ios.colors.secondaryLabel
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Индикаторные полоски количества пар
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val bars = dayLessonCount.coerceIn(1, 4)
                            repeat(bars) {
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
            }

            Icon(
                Icons.AutoMirrored.rounded.KeyboardArrowRight,
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

        // ─────────────────────────────────────────────────────────────
        // 4. Сводка: «≡ Практик X • Лекций Y»
        // ─────────────────────────────────────────────────────────────
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

        // ─────────────────────────────────────────────────────────────
        // 5. Временная шкала (Timeline) со списком пар (Скриншот 1:1)
        // ─────────────────────────────────────────────────────────────
        if (lessons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "В этот день пар нет 🎉",
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
                    val pairNumber = index + 1
                    val isLecture = lesson.type.contains("лекци", ignoreCase = true)
                    val cardBg = if (isLecture) Color(0xFF0F1E2E) else Color(0xFF0F261E)
                    val badgeBg = if (isLecture) Color(0xFF1B3654) else Color(0xFF1B4332)
                    val badgeTextColor = if (isLecture) Color(0xFF70C1FF) else Color(0xFF74C69D)
                    val circleBg = if (isLecture) Color(0xFF64B5F6) else Color(0xFF52B788)

                    val times = lesson.time.split("-").map { it.trim() }
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
                                // Тип занятия и счетчик
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = lesson.type.ifBlank { if (isLecture) "Лекция" else "Практические занятия" },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextColor
                                        )
                                    }

                                    Text(
                                        text = "${index + 4} из 18",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Ios.colors.secondaryLabel
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = lesson.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Преподаватель (с шевроном >)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "👤 ${lesson.teacher.ifBlank { "Преподаватель не указан" }}",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        Icons.AutoMirrored.rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Ios.colors.secondaryLabel,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Аудитория (с шевроном >)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Navigation,
                                            contentDescription = null,
                                            tint = Color(0xFFFF6D00),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = lesson.auditorium.ifBlank { "521/2" },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.rounded.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Ios.colors.secondaryLabel,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Кнопка подтверждения присутствия (если доступна)
                                if (lesson.canCheckIn) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val store = SettingsStore(context)
                                                val account = store.loadAccounts().firstOrNull { it.isComplete } ?: return@launch
                                                val client = SutClient()
                                                if (account.type == com.autocheck.app.data.AccountType.TOKEN) {
                                                    client.useToken(account.token)
                                                } else {
                                                    client.login(account.login, account.password)
                                                }
                                                if (lesson.lessonId != null && lesson.checkInWeek != null) {
                                                    val ok = client.startLesson(lesson.lessonId, lesson.checkInWeek)
                                                    if (ok) {
                                                        AutoClickState.success("Занятие подтверждено: ${lesson.name}")
                                                        refreshSchedule()
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Подтвердить занятие", fontWeight = FontWeight.Bold)
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
