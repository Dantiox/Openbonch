use crate::client::{BonchClient, LK_BASE_URL};
use crate::error::{BonchError, Result};

impl BonchClient {
    /// Авторизация по логину (email) и паролю от личного кабинета СПбГУТ
    pub async fn login(&self, email: &str, password: &str) -> Result<String> {
        // Шаг 1. Инициализирующий запрос сессии
        let init_url = format!("{}/?login=no", LK_BASE_URL);
        self.http.get(&init_url).send().await?;

        // Шаг 2. Отправка учетных данных
        let auth_url = format!(
            "{}/lib/autentificationok.php?users={}&parole={}",
            LK_BASE_URL,
            urlencoding::encode(email),
            urlencoding::encode(password)
        );

        let res = self.http.post(&auth_url).send().await?;
        let body = res.text().await?;
        let trimmed_body = body.trim();

        // Сервер Бонча отвечает '1' или '\n1' при успехе
        if !trimmed_body.contains('1') || trimmed_body == "0" || trimmed_body == "-1" {
            let msg = if trimmed_body.is_empty() {
                "Неверный логин или пароль".to_string()
            } else {
                format!("Ответ сервера: {}", trimmed_body)
            };
            return Err(BonchError::AuthFailed(msg));
        }

        // Шаг 3. Подтверждающий запрос и фиксация сессии
        let confirm_url = format!("{}/?login=yes", LK_BASE_URL);
        self.http.get(&confirm_url).send().await?;

        // Шаг 4. Проверка и получение токена miden
        let token = self.get_miden_token()
            .ok_or_else(|| BonchError::AuthFailed("Не удалось получить сессионный токен miden".to_string()))?;

        Ok(token)
    }

    /// Проверка валидности текущей сессии
    pub async fn is_authenticated(&self) -> Result<bool> {
        let check_url = format!("{}/", LK_BASE_URL);
        let res = self.http.get(&check_url).send().await?;
        let body = res.text().await?;

        if body.contains("У Вас нет прав доступа") || body.contains("autentificationok.php") {
            return Ok(false);
        }

        if body.contains("Выход") || body.contains("logout=yes") || body.contains("project/cabinet/forms") {
            return Ok(true);
        }

        Ok(false)
    }
}

// Простой хелпер для urlencoding без добавления тяжелых зависимостей
mod urlencoding {
    pub fn encode(data: &str) -> String {
        let mut encoded = String::new();
        for b in data.bytes() {
            match b {
                b'a'..=b'z' | b'A'..=b'Z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                    encoded.push(b as char);
                }
                _ => {
                    encoded.push_str(&format!("%{:02X}", b));
                }
            }
        }
        encoded
    }
}
