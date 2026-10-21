use thiserror::Error;

#[derive(Error, Debug)]
pub enum BonchError {
    #[error("Сетевая ошибка: {0}")]
    Network(#[from] reqwest::Error),

    #[error("Ошибка авторизации в ЛК: {0}")]
    AuthFailed(String),

    #[error("Сессия устарела или недействительна")]
    SessionExpired,

    #[error("Ошибка разбора данных (HTML/JSON): {0}")]
    ParseError(String),

    #[error("Внутренняя ошибка сервиса: {0}")]
    Generic(String),
}

pub type Result<T> = std::result::Result<T, BonchError>;
