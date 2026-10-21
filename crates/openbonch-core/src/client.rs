use std::sync::Arc;
use reqwest::cookie::{CookieStore, Jar};
use reqwest::{Client, Url};
use crate::error::{BonchError, Result};

pub const LK_BASE_URL: &str = "https://lk.sut.ru/cabinet";
pub const PUBLIC_SCHEDULE_URL: &str = "https://www.sut.ru/studentu/raspisanie";

/// Проверка соответствия хоста официальным доменам учреждения.
/// Предотвращает любые обращения к недоверенным, внешним или зарубежным ресурсам.
pub fn validate_host(url_str: &str) -> Result<()> {
    let url = Url::parse(url_str)
        .map_err(|e| BonchError::Generic(format!("Некорректный URL: {}", e)))?;
    if let Some(host) = url.host_str() {
        if host == "lk.sut.ru" || host == "sut.ru" || host.ends_with(".sut.ru") {
            return Ok(());
        }
        return Err(BonchError::SecurityViolation(format!(
            "Обращение к внешнему хосту '{}' заблокировано политикой приватности Openbonch",
            host
        )));
    }
    Err(BonchError::SecurityViolation("Отсутствует хост в адресе".to_string()))
}

#[derive(Clone)]
pub struct BonchClient {
    pub(crate) http: Client,
    pub(crate) cookie_jar: Arc<Jar>,
}

impl BonchClient {
    pub fn new() -> Result<Self> {
        let cookie_jar = Arc::new(Jar::default());
        
        let http = Client::builder()
            .cookie_provider(cookie_jar.clone())
            .user_agent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
            .timeout(std::time::Duration::from_secs(15))
            .build()?;

        Ok(Self { http, cookie_jar })
    }

    /// Восстановление сессии напрямую через cookie `miden`
    pub fn with_miden_token(miden_token: &str) -> Result<Self> {
        let client = Self::new()?;
        client.set_miden_token(miden_token)?;
        Ok(client)
    }

    /// Установить cookie miden в jar
    pub fn set_miden_token(&self, token: &str) -> Result<()> {
        let url = Url::parse(LK_BASE_URL)
            .map_err(|e| BonchError::Generic(format!("Некорректный URL: {}", e)))?;
        
        let cookie_str = format!("miden={}; Domain=lk.sut.ru; Path=/", token.trim());
        self.cookie_jar.add_cookie_str(&cookie_str, &url);
        Ok(())
    }

    /// Получить текущую сессионную cookie `miden`
    pub fn get_miden_token(&self) -> Option<String> {
        let url = Url::parse(LK_BASE_URL).ok()?;
        let cookies = self.cookie_jar.cookies(&url)?;
        
        for part in cookies.to_str().unwrap_or("").split(';') {
            let trimmed = part.trim();
            if let Some(token) = trimmed.strip_prefix("miden=") {
                return Some(token.to_string());
            }
        }
        None
    }
}
