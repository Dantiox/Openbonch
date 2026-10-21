use serde::{Deserialize, Serialize};
use regex::Regex;
use scraper::{Html, Selector};
use crate::client::{BonchClient, LK_BASE_URL};
use crate::error::{BonchError, Result};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Lesson {
    pub id: Option<String>,
    pub time: String,
    pub name: String,
    pub lesson_type: String,
    pub teacher: String,
    pub auditorium: String,
    pub can_check_in: bool,
    pub check_in_params: Option<(String, String)>, // (rasp_id, week)
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DaySchedule {
    pub weekday: String,
    pub date: String,
    pub lessons: Vec<Lesson>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WeekSchedule {
    pub week_number: u32,
    pub days: Vec<DaySchedule>,
}

impl BonchClient {
    /// Получить расписание из личного кабинета СПбГУТ
    pub async fn get_cabinet_schedule(&self) -> Result<WeekSchedule> {
        let schedule_url = format!("{}/project/cabinet/forms/raspisanie.php", LK_BASE_URL);
        let res = self.http.post(&schedule_url).send().await?;
        let html_content = res.text().await?;

        if html_content.contains("У Вас нет прав доступа") {
            return Err(BonchError::SessionExpired);
        }

        Self::parse_cabinet_schedule(&html_content)
    }

    /// Парсер HTML-страницы расписания личного кабинета
    pub fn parse_cabinet_schedule(html: &str) -> Result<WeekSchedule> {
        let document = Html::parse_document(html);

        // 1. Извлечение номера недели
        let week_re = Regex::new(r"№\s*(\d+)").unwrap();
        let week_number = week_re.captures(html)
            .and_then(|c| c.get(1))
            .and_then(|m| m.as_str().parse::<u32>().ok())
            .unwrap_or(1);

        // Регулярка для кнопки отметки: open_zan(970285, 27)
        let open_zan_re = Regex::new(r"open_zan\(\s*(\d+)\s*,\s*(\d+)\s*\)").unwrap();

        let mut days = Vec::new();
        let table_sel = Selector::parse("table.simple-little-table, table").unwrap();
        let tr_sel = Selector::parse("tr").unwrap();
        let td_sel = Selector::parse("td, th").unwrap();

        for table in document.select(&table_sel) {
            let mut current_day_name = String::new();
            let mut current_lessons = Vec::new();

            for tr in table.select(&tr_sel) {
                let cells: Vec<_> = tr.select(&td_sel).collect();
                if cells.is_empty() {
                    continue;
                }

                // Проверяем, является ли строка заголовком дня
                let first_text = cells[0].text().collect::<String>().trim().to_string();
                if cells.len() == 1 || first_text.contains("Понедельник") || first_text.contains("Вторник") 
                    || first_text.contains("Среда") || first_text.contains("Четверг") 
                    || first_text.contains("Пятница") || first_text.contains("Суббота") {
                    if !current_day_name.is_empty() && !current_lessons.is_empty() {
                        days.push(DaySchedule {
                            weekday: current_day_name.clone(),
                            date: String::new(),
                            lessons: current_lessons.clone(),
                        });
                        current_lessons.clear();
                    }
                    current_day_name = first_text;
                    continue;
                }

                // Строка занятия
                if cells.len() >= 3 {
                    let time = cells[0].text().collect::<String>().trim().to_string();
                    let desc = cells[1].text().collect::<String>().trim().to_string();
                    let raw_cell_html = cells[1].html();

                    let mut can_check_in = false;
                    let mut check_in_params = None;

                    if let Some(caps) = open_zan_re.captures(&raw_cell_html) {
                        can_check_in = true;
                        let rasp = caps.get(1).map_or("", |m| m.as_str()).to_string();
                        let week = caps.get(2).map_or("", |m| m.as_str()).to_string();
                        check_in_params = Some((rasp, week));
                    }

                    // Если кнопка есть в третьей или четвертой ячейке
                    if !can_check_in && cells.len() >= 3 {
                        let btn_html = cells[cells.len() - 1].html();
                        if let Some(caps) = open_zan_re.captures(&btn_html) {
                            can_check_in = true;
                            let rasp = caps.get(1).map_or("", |m| m.as_str()).to_string();
                            let week = caps.get(2).map_or("", |m| m.as_str()).to_string();
                            check_in_params = Some((rasp, week));
                        }
                    }

                    current_lessons.push(Lesson {
                        id: check_in_params.as_ref().map(|p| p.0.clone()),
                        time,
                        name: desc.clone(),
                        lesson_type: String::new(),
                        teacher: String::new(),
                        auditorium: String::new(),
                        can_check_in,
                        check_in_params,
                    });
                }
            }

            if !current_day_name.is_empty() && !current_lessons.is_empty() {
                days.push(DaySchedule {
                    weekday: current_day_name,
                    date: String::new(),
                    lessons: current_lessons,
                });
            }
        }

        Ok(WeekSchedule {
            week_number,
            days,
        })
    }
}
