package com.autocheck.app.data

object GroupScheduleProvider {
    val POPULAR_GROUPS: List<String> = listOf(
        "ИБТС-2613",
        "ИКВТ-31",
        "ИКВТ-32",
        "ИКТ-2611",
        "ИКТ-2612",
        "ИКТ-2613",
        "ИКТБ-2561",
        "ИКПИ-21",
        "ИСТ-11"
    )

    fun getScheduleForGroup(group: String): WeekScheduleData {
        return when (group.uppercase().trim()) {
            "ИБТС-2613" -> WeekScheduleData(
                weekNumber = "7",
                days = listOf(
                    DayScheduleItem(
                        weekday = "Понедельник",
                        date = "19 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "09:00 — 10:35",
                                name = "Криптографические методы защиты информации",
                                teacher = "Шиян Павел Анатольевич",
                                auditorium = "521/2",
                                type = "Практические занятия • пара 1",
                                canCheckIn = true
                            ),
                            LessonItem(
                                time = "10:45 — 12:20",
                                name = "Теория информационной безопасности",
                                teacher = "Карлов Андрей Владимирович",
                                auditorium = "501/1",
                                type = "Лекция • пара 2"
                            ),
                            LessonItem(
                                time = "13:00 — 14:35",
                                name = "Безопасность операционных систем",
                                teacher = "Смирнов Дмитрий Юрьевич",
                                auditorium = "332/1",
                                type = "Лабораторная работа • пара 3"
                            )
                        )
                    ),
                    DayScheduleItem(
                        weekday = "Вторник",
                        date = "20 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "10:45 — 12:20",
                                name = "Беспроводные сенсорные сети",
                                teacher = "Кучерявый Андрей Евгеньевич",
                                auditorium = "200/1",
                                type = "Лекция • пара 2"
                            ),
                            LessonItem(
                                time = "13:00 — 14:35",
                                name = "Информационные технологии",
                                teacher = "Красов Андрей Владимирович",
                                auditorium = "521/2",
                                type = "Практические занятия • пара 3"
                            )
                        )
                    ),
                    DayScheduleItem(
                        weekday = "Среда",
                        date = "21 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "09:00 — 10:35",
                                name = "Информатика и программирование",
                                teacher = "Шиян Павел Анатольевич",
                                auditorium = "521/2",
                                type = "Практические занятия • пара 1",
                                canCheckIn = true
                            ),
                            LessonItem(
                                time = "10:45 — 12:20",
                                name = "Защита программного обеспечения",
                                teacher = "Ушаков Игорь Александрович",
                                auditorium = "105/2",
                                type = "Лабораторная работа • пара 2"
                            )
                        )
                    ),
                    DayScheduleItem(
                        weekday = "Четверг",
                        date = "22 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "09:00 — 10:35",
                                name = "Информатика",
                                teacher = "Шиян Павел Анатольевич",
                                auditorium = "521/2",
                                type = "Практические занятия • пара 1",
                                canCheckIn = true
                            ),
                            LessonItem(
                                time = "10:45 — 12:20",
                                name = "Техническая защита информации",
                                teacher = "Абрамов Сергей Романович",
                                auditorium = "501/1",
                                type = "Лекция • пара 2"
                            )
                        )
                    ),
                    DayScheduleItem(
                        weekday = "Пятница",
                        date = "23 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "10:45 — 12:20",
                                name = "Сети передачи данных",
                                teacher = "Владыко Андрей Геннадьевич",
                                auditorium = "301/1",
                                type = "Лекция • пара 2"
                            ),
                            LessonItem(
                                time = "13:00 — 14:35",
                                name = "Криптографические протоколы",
                                teacher = "Шиян Павел Анатольевич",
                                auditorium = "521/2",
                                type = "Практические занятия • пара 3"
                            )
                        )
                    ),
                    DayScheduleItem(
                        weekday = "Суббота",
                        date = "24 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "09:00 — 10:35",
                                name = "Военная подготовка (ВУЦ)",
                                teacher = "Полковник Мартынов В.С.",
                                auditorium = "ВУЦ-101",
                                type = "Практические занятия • пара 1"
                            )
                        )
                    )
                )
            )

            "ИКВТ-31" -> WeekScheduleData(
                weekNumber = "7",
                days = listOf(
                    DayScheduleItem(
                        weekday = "Понедельник",
                        date = "19 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "09:00 — 10:35",
                                name = "Архитектура вычислительных систем",
                                teacher = "Волошин Виктор Олегович",
                                auditorium = "332/1",
                                type = "Лекция • пара 1"
                            ),
                            LessonItem(
                                time = "10:45 — 12:20",
                                name = "Микропроцессорные системы",
                                teacher = "Иванов Петр Сергеевич",
                                auditorium = "304/2",
                                type = "Лабораторная работа • пара 2"
                            )
                        )
                    ),
                    DayScheduleItem(
                        weekday = "Вторник",
                        date = "20 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "13:00 — 14:35",
                                name = "Базы данных и экспертные системы",
                                teacher = "Ковалева Анна Михайловна",
                                auditorium = "108/2",
                                type = "Практические занятия • пара 3"
                            )
                        )
                    ),
                    DayScheduleItem(
                        weekday = "Среда",
                        date = "21 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "10:45 — 12:20",
                                name = "Параллельные вычисления",
                                teacher = "Смирнов Алексей Викторович",
                                auditorium = "130/1",
                                type = "Лекция • пара 2"
                            )
                        )
                    ),
                    DayScheduleItem(
                        weekday = "Четверг",
                        date = "22 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "09:00 — 10:35",
                                name = "Схемотехника ЭВМ",
                                teacher = "Платонов Игорь Борисович",
                                auditorium = "332/1",
                                type = "Практические занятия • пара 1"
                            )
                        )
                    ),
                    DayScheduleItem(
                        weekday = "Пятница",
                        date = "23 окт.",
                        lessons = listOf(
                            LessonItem(
                                time = "10:45 — 12:20",
                                name = "Компьютерная графика",
                                teacher = "Кузнецов Денис Олегович",
                                auditorium = "100/1",
                                type = "Лабораторная работа • пара 2"
                            )
                        )
                    )
                )
            )

            else -> {
                val groupHash = kotlin.math.abs(group.hashCode())
                val aud1 = if (groupHash % 2 == 0) "105/2" else "301/1"
                val aud2 = if (groupHash % 3 == 0) "521/2" else "332/1"
                val aud3 = if (groupHash % 5 == 0) "130/1" else "200/1"
                WeekScheduleData(
                    weekNumber = "7",
                    days = listOf(
                        DayScheduleItem(
                            weekday = "Понедельник",
                            date = "19 окт.",
                            lessons = listOf(
                                LessonItem("09:00 — 10:35", "Высшая математика", "Проф. Морозов В.А.", aud1, "Лекция • пара 1"),
                                LessonItem("10:45 — 12:20", "Физика телекоммуникаций", "Доц. Лебедев Н.К.", aud2, "Практические занятия • пара 2")
                            )
                        ),
                        DayScheduleItem(
                            weekday = "Вторник",
                            date = "20 окт.",
                            lessons = listOf(
                                LessonItem("10:45 — 12:20", "Основы теории цепей", "Доц. Громов А.П.", aud3, "Лабораторная работа • пара 2")
                            )
                        ),
                        DayScheduleItem(
                            weekday = "Среда",
                            date = "21 окт.",
                            lessons = listOf(
                                LessonItem("09:00 — 10:35", "Программирование на C++", "Ст. преп. Белов С.В.", aud1, "Лабораторная работа • пара 1"),
                                LessonItem("13:00 — 14:35", "Инженерная графика", "Доц. Николаев Д.И.", aud2, "Практические занятия • пара 3")
                            )
                        ),
                        DayScheduleItem(
                            weekday = "Четверг",
                            date = "22 окт.",
                            lessons = listOf(
                                LessonItem("09:00 — 10:35", "Сетевые протоколы", "Проф. Воронов К.М.", aud2, "Лекция • пара 1", canCheckIn = true),
                                LessonItem("10:45 — 12:20", "Электроника и схемотехника", "Доц. Петрова Е.В.", aud3, "Практические занятия • пара 2")
                            )
                        ),
                        DayScheduleItem(
                            weekday = "Пятница",
                            date = "23 окт.",
                            lessons = listOf(
                                LessonItem("10:45 — 12:20", "Иностранный язык в сфере ИКТ", "Ст. преп. Миллер К.А.", aud1, "Практические занятия • пара 2")
                            )
                        )
                    )
                )
            }
        }
    }
}
