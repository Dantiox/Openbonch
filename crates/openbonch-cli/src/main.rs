use clap::{Parser, Subcommand};
use colored::*;
use directories::ProjectDirs;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use std::time::Duration;
use openbonch_core::{BonchClient, Result};

#[derive(Parser)]
#[command(name = "openbonch")]
#[command(author = "Dantiox")]
#[command(version = "0.1.0")]
#[command(about = "100% Free & Open-Source client and auto-checkin daemon for SPbSUT (Bonch) students", long_about = None)]
struct Cli {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
enum Commands {
    /// Вход в личный кабинет СПбГУТ (по email/паролю или токену miden)
    Login {
        #[arg(short, long)]
        email: Option<String>,

        #[arg(short, long)]
        password: Option<String>,

        /// Вход напрямую по сессионному cookie miden (без пароля)
        #[arg(short, long)]
        token: Option<String>,
    },

    /// Проверить статус текущей сессии
    Status,

    /// Показать расписание занятий
    Schedule {
        /// Показать только сегодняшние пары
        #[arg(short, long)]
        today: bool,
    },

    /// Отметиться на текущих доступных парах прямо сейчас
    Checkin,

    /// Запустить фоновый вотчер автоматической отметки на парах
    Watch {
        /// Интервал проверки в секундах (по умолчанию 60 сек)
        #[arg(short, long, default_value_t = 60)]
        interval: u64,
    },

    /// Выйти из учетной записи и удалить сохраненный токен
    Logout,
}

#[derive(Serialize, Deserialize)]
struct SavedSession {
    miden_token: String,
    saved_at: String,
}

fn get_config_path() -> PathBuf {
    if let Some(proj_dirs) = ProjectDirs::from("org", "openbonch", "openbonch") {
        let dir = proj_dirs.config_dir();
        fs::create_dir_all(dir).ok();
        dir.join("session.json")
    } else {
        PathBuf::from(".openbonch_session.json")
    }
}

fn load_saved_token() -> Option<String> {
    let path = get_config_path();
    if path.exists() {
        if let Ok(data) = fs::read_to_string(path) {
            if let Ok(session) = serde_json::from_str::<SavedSession>(&data) {
                return Some(session.miden_token);
            }
        }
    }
    None
}

fn save_token(token: &str) -> std::io::Result<()> {
    let path = get_config_path();
    let session = SavedSession {
        miden_token: token.to_string(),
        saved_at: chrono::Local::now().to_rfc3339(),
    };
    let data = serde_json::to_string_pretty(&session)?;
    fs::write(path, data)
}

fn get_authenticated_client() -> Result<BonchClient> {
    if let Some(token) = load_saved_token() {
        BonchClient::with_miden_token(&token)
    } else {
        eprintln!("{}", "Ошибка: Вы не авторизованы. Выполните: openbonch login".red());
        std::process::exit(1);
    }
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    let cli = Cli::parse();

    match cli.command {
        Commands::Login { email, password, token } => {
            let client = BonchClient::new()?;

            if let Some(t) = token {
                client.set_miden_token(&t)?;
                if client.is_authenticated().await? {
                    save_token(&t)?;
                    println!("{}", "✓ Успешный вход по токену miden!".green().bold());
                } else {
                    eprintln!("{}", "✗ Переданный токен miden недействителен или сессия истекла.".red());
                    std::process::exit(1);
                }
            } else {
                let user_email = match email {
                    Some(e) => e,
                    None => {
                        print!("Email / Логин от lk.sut.ru: ");
                        use std::io::Write;
                        std::io::stdout().flush()?;
                        let mut input = String::new();
                        std::io::stdin().read_line(&mut input)?;
                        input.trim().to_string()
                    }
                };

                let user_pass = match password {
                    Some(p) => p,
                    None => {
                        rpassword::prompt_password("Пароль от lk.sut.ru: ")?
                    }
                };

                println!("{}", "Авторизация в СПбГУТ...".cyan());
                match client.login(&user_email, &user_pass).await {
                    Ok(miden) => {
                        save_token(&miden)?;
                        println!("{}", "✓ Успешный вход! Токен сессии сохранен локально.".green().bold());
                    }
                    Err(e) => {
                        eprintln!("{} {}", "✗ Ошибка входа:".red().bold(), e);
                        std::process::exit(1);
                    }
                }
            }
        }

        Commands::Status => {
            let client = get_authenticated_client()?;
            print!("Проверка сессии... ");
            if client.is_authenticated().await? {
                println!("{}", "Активна ✓".green().bold());
                if let Ok(profile) = client.get_profile().await {
                    println!("Пользователь: {}", profile.name.cyan().bold());
                }
            } else {
                println!("{}", "Сессия истекла ✗ (требуется повторный вход)".red().bold());
            }
        }

        Commands::Schedule { today: _ } => {
            let client = get_authenticated_client()?;
            println!("{}", "Загрузка расписания из ЛК СПбГУТ...".cyan());
            let schedule = client.get_cabinet_schedule().await?;

            println!("{} {}", "Учебная неделя №".bold(), schedule.week_number.to_string().yellow().bold());
            println!("{}", "═══════════════════════════════════════════════════════".blue());

            for day in schedule.days {
                println!("\n📅 {}", day.weekday.bold().underline());
                if day.lessons.is_empty() {
                    println!("   {}", "Занятий нет".dimmed());
                    continue;
                }
                for lesson in day.lessons {
                    let mark_status = if lesson.can_check_in {
                        "[ДОСТУПНА ОТМЕТКА!]".green().bold()
                    } else {
                        "".normal()
                    };
                    println!("   🕒 {}  {} {}", lesson.time.yellow(), lesson.name, mark_status);
                }
            }
        }

        Commands::Checkin => {
            let client = get_authenticated_client()?;
            println!("{}", "Проверка доступных кнопок отметки на парах...".cyan());
            let results = client.auto_check_in_current().await?;

            if results.is_empty() {
                println!("{}", "В данный момент активных кнопок «Начать занятие» не обнаружено.".dimmed());
            } else {
                for res in results {
                    if res.success {
                        println!("{} {} ({})", "✓ УСПЕШНО ОТМЕЧЕН:".green().bold(), res.lesson_name, res.lesson_time);
                    } else {
                        println!("{} {} ({}) - {}", "✗ Ошибка отметки:".red().bold(), res.lesson_name, res.lesson_time, res.server_response);
                    }
                }
            }
        }

        Commands::Watch { interval } => {
            let client = get_authenticated_client()?;
            println!("{} {} {}", "⚡ Запущен демон автоотметки Openbonch (интервал:".green().bold(), interval, "сек)".green().bold());
            println!("{}", "Нажмите Ctrl+C для остановки.\n".dimmed());

            loop {
                let now = chrono::Local::now().format("%H:%M:%S").to_string();
                match client.auto_check_in_current().await {
                    Ok(results) => {
                        for res in results {
                            if res.success {
                                println!("[{}] {} {} ({})", now.dimmed(), "✓ ОТМЕЧЕН:".green().bold(), res.lesson_name, res.lesson_time);
                            }
                        }
                    }
                    Err(e) => {
                        eprintln!("[{}] {} {}", now.dimmed(), "Ошибка опроса:".red(), e);
                    }
                }
                tokio::time::sleep(Duration::from_secs(interval)).await;
            }
        }

        Commands::Logout => {
            let path = get_config_path();
            if path.exists() {
                fs::remove_file(path)?;
                println!("{}", "✓ Сессия удалена. Вы вышли из профиля.".green());
            } else {
                println!("{}", "Вы уже вышли.".dimmed());
            }
        }
    }

    Ok(())
}
