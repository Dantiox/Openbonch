use serde::{Deserialize, Serialize};
use crate::client::{BonchClient, LK_BASE_URL};
use crate::error::{BonchError, Result};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CheckinResult {
    pub lesson_name: String,
    pub lesson_time: String,
    pub rasp_id: String,
    pub week: String,
    pub success: bool,
    pub server_response: String,
}

impl BonchClient {
    /// Отметиться на конкретном занятии по его ID и номеру недели
    pub async fn check_in(&self, rasp_id: &str, week: &str) -> Result<String> {
        let check_url = format!(
            "{}/project/cabinet/forms/raspisanie.php?open=1&rasp={}&week={}",
            LK_BASE_URL, rasp_id, week
        );

        let res = self.http.post(&check_url).send().await?;
        let body = res.text().await?;

        if body.contains("У Вас нет прав доступа") {
            return Err(BonchError::SessionExpired);
        }

        Ok(body)
    }

    /// Проверить текущее расписание и автоматически отметиться на всех доступных парах прямо сейчас
    pub async fn auto_check_in_current(&self) -> Result<Vec<CheckinResult>> {
        let schedule = self.get_cabinet_schedule().await?;
        let mut results = Vec::new();

        for day in schedule.days {
            for lesson in day.lessons {
                if lesson.can_check_in {
                    if let Some((rasp_id, week)) = &lesson.check_in_params {
                        match self.check_in(rasp_id, week).await {
                            Ok(resp) => {
                                results.push(CheckinResult {
                                    lesson_name: lesson.name.clone(),
                                    lesson_time: lesson.time.clone(),
                                    rasp_id: rasp_id.clone(),
                                    week: week.clone(),
                                    success: true,
                                    server_response: resp,
                                });
                            }
                            Err(e) => {
                                results.push(CheckinResult {
                                    lesson_name: lesson.name.clone(),
                                    lesson_time: lesson.time.clone(),
                                    rasp_id: rasp_id.clone(),
                                    week: week.clone(),
                                    success: false,
                                    server_response: e.to_string(),
                                });
                            }
                        }
                    }
                }
            }
        }

        Ok(results)
    }
}
