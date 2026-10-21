package com.autocheck.app.data

data class LessonItem(
    val time: String,
    val name: String,
    val teacher: String = "",
    val auditorium: String = "",
    val type: String = "",
    val lessonId: String? = null,
    val canCheckIn: Boolean = false,
    val checkInWeek: String? = null,
)

data class DayScheduleItem(
    val weekday: String,
    val date: String = "",
    val lessons: List<LessonItem> = emptyList(),
)

data class WeekScheduleData(
    val weekNumber: String = "1",
    val days: List<DayScheduleItem> = emptyList(),
)
