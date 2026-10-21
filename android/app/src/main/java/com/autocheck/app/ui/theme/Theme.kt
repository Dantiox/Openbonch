package com.autocheck.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.autocheck.app.data.LogLevel
import com.autocheck.app.data.ThemeMode

/**
 * Палитра в духе iOS 27: чистая и плоская, без градиентов, теней и бликов. Правило 60–30–10:
 *  • 60 % — нейтральный: белый (светлая тема) / чёрный (тёмная тема) — фон, карточки, текст;
 *  • 30 % — оранжевый: значки разделов, главная карточка, кнопки действий;
 *  • 10 % — голубой: интерактивные состояния — переключатели, слайдер, ссылки, выбранная вкладка, галочки.
 * На одном логическом уровне всегда один цвет: все значки разделов оранжевые, все ссылки голубые.
 */
object Brand {
    val Orange = Color(0xFFFF8A00)
    val Sky = Color(0xFF14B1FF)

    /** Жёлто-оранжевый индикатор записей журнала с сохранённым HTML-ответом сервера. */
    val Amber = Color(0xFFFFC107)
}

/** Семантические цвета интерфейса. Получать через [Ios.colors]. */
@Immutable
data class IosColors(
    val isDark: Boolean,
    /** Фон экрана. */
    val background: Color,
    /** Фон карточек и сгруппированных списков. */
    val card: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    /** Заливка вторичных элементов: дорожка слайдера, нажатая строка. */
    val fill: Color,
    /** Дорожка слайдера и выключенный переключатель. */
    val track: Color,
    /** Плавающие элементы (панель вкладок, круглые кнопки): сплошная заливка. */
    val bar: Color,
    /** Тонкая кромка плавающих элементов вместо тени. */
    val edge: Color,
    /** Акцентный голубой для заливок и элементов управления. */
    val accent: Color,
    /** Голубой для текста и значков: чуть темнее в светлой теме, чтобы читался на белом. */
    val accentText: Color,
    val orange: Color,
    val success: Color,
    val danger: Color,
)

private val LightIos = IosColors(
    isDark = false,
    background = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF6C6C70),
    tertiaryLabel = Color(0xFFAEAEB2),
    separator = Color(0xFFE5E5EA),
    fill = Color(0xFFE9E9EE),
    track = Color(0xFFD9D9DE),
    bar = Color(0xFFFFFFFF),
    edge = Color(0x1A000000),
    accent = Brand.Sky,
    accentText = Color(0xFF0A8FE6),
    orange = Brand.Orange,
    success = Color(0xFF1FA55A),
    danger = Color(0xFFE0303F),
)

private val DarkIos = IosColors(
    isDark = true,
    background = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFF98989F),
    tertiaryLabel = Color(0xFF636366),
    separator = Color(0xFF38383A),
    fill = Color(0xFF2C2C2E),
    track = Color(0xFF3A3A3C),
    bar = Color(0xFF1C1C1E),
    edge = Color(0x1AFFFFFF),
    accent = Color(0xFF2DB9FF),
    accentText = Color(0xFF4FC6FF),
    orange = Color(0xFFFF9A1F),
    success = Color(0xFF34D27B),
    danger = Color(0xFFFF6B77),
)

private val LocalIosColors = staticCompositionLocalOf { LightIos }

/** Доступ к цветам текущей темы: `Ios.colors.card`, `Ios.colors.accent` и т. д. */
object Ios {
    val colors: IosColors
        @Composable
        @ReadOnlyComposable
        get() = LocalIosColors.current
}

/** Шрифтовая шкала iOS (Large Title 34, Title 28/22/20, Headline 17, Body 17, Subheadline 15, Footnote 13, Caption 11). */
private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium),
)

val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)

/** Тёмная ли тема сейчас на самом деле: учитывает и выбор пользователя, и настройку системы. */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** Нужна ли тёмная тема при выбранном режиме. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private fun IosColors.toMaterial() =
    (if (isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accent.copy(alpha = 0.18f),
        onPrimaryContainer = accentText,
        secondary = orange,
        onSecondary = Color.White,
        secondaryContainer = orange.copy(alpha = 0.18f),
        onSecondaryContainer = orange,
        background = background,
        onBackground = label,
        surface = background,
        onSurface = label,
        surfaceVariant = fill,
        onSurfaceVariant = secondaryLabel,
        surfaceContainer = card,
        surfaceContainerHigh = fill,
        outline = separator,
        outlineVariant = separator,
        error = danger,
    )

@Composable
fun AutoCheckTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkIos else LightIos
    CompositionLocalProvider(
        LocalIosColors provides colors,
        LocalDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = AppTypography,
            content = content,
        )
    }
}

/** Цвет точки уровня лога: голубой — инфо, зелёный — успех, оранжевый — предупреждение, красный — ошибка. */
@Composable
fun LogLevel.color(): Color {
    val c = Ios.colors
    return when (this) {
        LogLevel.INFO -> c.accent
        LogLevel.SUCCESS -> c.success
        LogLevel.WARNING -> c.orange
        LogLevel.ERROR -> c.danger
    }
}
