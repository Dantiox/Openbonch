package com.autocheck.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autocheck.app.data.EngineStatus
import com.autocheck.app.data.LogEntry
import com.autocheck.app.data.LogLevel
import com.autocheck.app.data.Stats
import com.autocheck.app.data.displayTime
import com.autocheck.app.ui.components.Banner
import com.autocheck.app.ui.components.CardShape
import com.autocheck.app.ui.components.GroupHeader
import com.autocheck.app.ui.components.GroupedCard
import com.autocheck.app.ui.components.InsetDivider
import com.autocheck.app.ui.components.LargeTitle
import com.autocheck.app.ui.components.LinkText
import com.autocheck.app.ui.components.iosClickable
import com.autocheck.app.ui.components.pressHighlight
import com.autocheck.app.ui.theme.Brand
import com.autocheck.app.ui.theme.Ios
import com.autocheck.app.ui.theme.MonoStyle
import com.autocheck.app.ui.theme.color

/** Цвета героя-карточки для каждого состояния: только плоские заливки, без градиентов. */
private data class HeroStyle(
    val bg: Color,
    val title: Color,
    val subtitle: Color,
    val button: Color,
    val buttonIcon: Color,
)

@Composable
private fun heroStyle(status: EngineStatus): HeroStyle {
    val c = Ios.colors
    return when (status) {
        // Остановлено: белая/чёрная карточка, оранжевая кнопка запуска
        EngineStatus.STOPPED -> HeroStyle(
            bg = c.card,
            title = c.label,
            subtitle = c.secondaryLabel,
            button = c.orange,
            buttonIcon = Color.White,
        )

        // Подключение: голубая карточка, белая кнопка с голубым значком
        EngineStatus.CONNECTING -> HeroStyle(
            bg = c.accent,
            title = Color.White,
            subtitle = Color.White.copy(alpha = 0.88f),
            button = Color.White,
            buttonIcon = c.accent,
        )

        // Работа: оранжевая карточка, белая кнопка с оранжевым значком
        EngineStatus.RUNNING -> HeroStyle(
            bg = c.orange,
            title = Color.White,
            subtitle = Color.White.copy(alpha = 0.88f),
            button = Color.White,
            buttonIcon = c.orange,
        )

        // Ошибка: карточка остаётся нейтральной, о сбое говорит красный заголовок; кнопка снова оранжевая
        EngineStatus.ERROR -> HeroStyle(
            bg = c.card,
            title = c.danger,
            subtitle = c.secondaryLabel,
            button = c.orange,
            buttonIcon = Color.White,
        )
    }
}

private fun EngineStatus.title() = when (this) {
    EngineStatus.STOPPED -> "Остановлено"
    EngineStatus.CONNECTING -> "Подключение…"
    EngineStatus.RUNNING -> "Работает"
    EngineStatus.ERROR -> "Ошибка"
}

@Composable
fun HomeScreen(
    status: EngineStatus,
    stats: Stats,
    recentLogs: List<LogEntry>,
    timeoutSec: Int,
    hasCredentials: Boolean,
    bottomInset: Dp,
    resetSignal: Int,
    onToggle: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scroll = rememberScrollState()
    // Повторное нажатие на вкладку «Главная» — прокрутка в начало
    LaunchedEffect(resetSignal) { if (resetSignal > 0) scroll.animateScrollTo(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LargeTitle("AutoCheck")

        Hero(
            status = status,
            subtitle = when (status) {
                EngineStatus.STOPPED -> "Нажмите, чтобы запустить автопосещение"
                EngineStatus.CONNECTING -> "Выполняется вход в личный кабинет"
                EngineStatus.RUNNING -> "Проверка расписания каждые $timeoutSec с"
                EngineStatus.ERROR -> "Подробности в журнале"
            },
            enabled = hasCredentials || status != EngineStatus.STOPPED,
            onToggle = onToggle,
        )

        if (!hasCredentials) {
            Banner(
                text = "Добавьте аккаунт от lk.sut.ru в настройках",
                onClick = onOpenSettings,
            )
        }

        StatsCard(stats)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GroupHeader("Последние события")
                LinkText("Весь журнал", onClick = onOpenLog)
            }
            if (recentLogs.isEmpty()) {
                GroupedCard {
                    Text(
                        "Здесь появятся события после запуска",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ios.colors.secondaryLabel,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                    )
                }
            } else {
                GroupedCard {
                    recentLogs.forEachIndexed { index, entry ->
                        if (index > 0) InsetDivider(startInset = 38.dp)
                        LogRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(
    status: EngineStatus,
    subtitle: String,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val target = heroStyle(status)
    val spec = tween<Color>(400)
    val bg by animateColorAsState(target.bg, spec, label = "bg")
    val titleColor by animateColorAsState(target.title, spec, label = "title")
    val subtitleColor by animateColorAsState(target.subtitle, spec, label = "subtitle")
    val buttonColor by animateColorAsState(target.button, spec, label = "button")
    val buttonIcon by animateColorAsState(target.buttonIcon, spec, label = "buttonIcon")

    val active = status == EngineStatus.RUNNING || status == EngineStatus.CONNECTING

    // Кольцо-пульс вокруг кнопки, пока приложение работает
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "scale",
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.35f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(bg)
            .padding(vertical = 32.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                if (active) {
                    Box(
                        Modifier
                            .size(116.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                alpha = ringAlpha
                            }
                            .background(Color.White, CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .iosClickable(enabled = enabled, pressedScale = 0.94f, onClick = onToggle)
                        .alpha(if (enabled) 1f else 0.4f)
                        .size(116.dp)
                        .clip(CircleShape)
                        .background(buttonColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (active) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = if (active) "Остановить" else "Запустить",
                        tint = buttonIcon,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                status.title(),
                style = MaterialTheme.typography.headlineMedium,
                color = titleColor,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
            )
        }
    }
}

/** Статистика: одна карточка с тремя колонками. Все значения одного цвета — основного текста. */
@Composable
private fun StatsCard(stats: Stats) {
    val c = Ios.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(c.card)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stat("Проверок", stats.cycles.toString(), Modifier.weight(1f))
        StatDivider()
        Stat("Начато занятий", stats.started.toString(), Modifier.weight(1f))
        StatDivider()
        Stat("Последняя", stats.lastCheck ?: "—", Modifier.weight(1f))
    }
}

@Composable
private fun StatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Ios.colors.separator)
    )
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Ios.colors.label,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = Ios.colors.secondaryLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Строка журнала: цветная точка уровня, сообщение и время. Используется на главной и в «Журнале».
 *
 * Если у записи есть сохранённый ответ сервера и передан [onOpenResponse], строку можно нажать,
 * чтобы открыть этот ответ.
 */
@Composable
fun LogRow(
    entry: LogEntry,
    modifier: Modifier = Modifier,
    onOpenResponse: ((LogEntry) -> Unit)? = null,
) {
    val c = Ios.colors
    val openable = onOpenResponse != null && entry.responseFile != null
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (openable) {
                    Modifier
                        .pressHighlight(interaction)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "Открыть ответ сервера",
                            onClick = { onOpenResponse?.invoke(entry) },
                        )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(10.dp)
                // Записи с сохранённым HTML-ответом помечены жёлто-оранжевой точкой, остальные — цветом уровня
                .background(if (entry.responseFile != null) Brand.Amber else entry.level.color(), CircleShape)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                entry.message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (entry.level == LogLevel.ERROR) c.danger else c.label,
                fontWeight = when (entry.level) {
                    LogLevel.SUCCESS, LogLevel.ERROR -> FontWeight.Medium
                    else -> null
                },
            )
            Text(entry.displayTime(), style = MonoStyle, color = c.secondaryLabel)
        }
        if (openable) {
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("HTML", style = MaterialTheme.typography.labelMedium, color = c.accentText)
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = c.tertiaryLabel,
                )
            }
        }
    }
}
