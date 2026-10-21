use serde::{Deserialize, Serialize};
use scraper::{Html, Selector};
use crate::client::{BonchClient, LK_BASE_URL};
use crate::error::{BonchError, Result};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct StudentProfile {
    pub name: String,
    pub group: String,
    pub faculty: String,
}

impl BonchClient {
    /// Получить базовые данные профиля студента
    pub async fn get_profile(&self) -> Result<StudentProfile> {
        let profile_url = format!("{}/", LK_BASE_URL);
        let res = self.http.get(&profile_url).send().await?;
        let html_content = res.text().await?;

        if html_content.contains("У Вас нет прав доступа") {
            return Err(BonchError::SessionExpired);
        }

        let document = Html::parse_document(&html_content);
        let name_sel = Selector::parse(".user-name, #user_name, .fio, h4, h3").unwrap();
        
        let mut name = String::new();
        for el in document.select(&name_sel) {
            let text = el.text().collect::<String>().trim().to_string();
            if text.split_whitespace().count() >= 2 && !text.contains("Личный кабинет") {
                name = text;
                break;
            }
        }

        Ok(StudentProfile {
            name: if name.is_empty() { "Студент СПбГУТ".to_string() } else { name },
            group: String::new(),
            faculty: String::new(),
        })
    }
}
