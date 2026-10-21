pub mod error;
pub mod client;
pub mod auth;
pub mod schedule;
pub mod attendance;
pub mod profile;

pub use client::BonchClient;
pub use error::{BonchError, Result};
pub use schedule::{Lesson, DaySchedule, WeekSchedule};
pub use attendance::CheckinResult;
pub use profile::StudentProfile;
