package com.autocheck.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autocheck.app.ui.theme.Ios

/** Форма сгруппированных карточек и списков. */
val CardShape = RoundedCornerShape(26.dp)

// ───────────────────────── Плавающие элементы ─────────────────────────

/**
 * Плавающий элемент (панель вкладок, круглые кнопки): сплошная заливка и тонкая кромка.
 * Без теней, градиентов и бликов — плоский чистый вид.
 */
@Composable
fun Modifier.floating(shape: Shape): Modifier {
    val c = Ios.colors
    return this
        .clip(shape)
        .background(c.bar)
        .border(width = 0.5.dp, color = c.edge, shape = shape)
}

// ───────────────────────── Нажатия ─────────────────────────

/** Нажатие в стиле iOS: без ряби, элемент слегка уменьшается. Ставить первым в цепочке модификаторов. */
@Composable
fun Modifier.iosClickable(
    enabled: Boolean = true,
    role: Role? = Role.Button,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

/** Подсветка строки списка при нажатии (вместо ряби). Используется вместе с [interaction] строки. */
@Composable
fun Modifier.pressHighlight(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    // Невидимое состояние — тот же цвет с нулевой прозрачностью, а не Color.Transparent (чёрный с alpha 0):
    // иначе при переходе цвет на мгновение уходит в тёмный, и строка «затемняется» дважды — при нажатии и отпускании
    val fill = Ios.colors.fill
    val bg by animateColorAsState(
        targetValue = if (pressed) fill else fill.copy(alpha = 0f),
        animationSpec = tween(120),
        label = "row-press",
    )
    return this.background(bg)
}

// ───────────────────────── Заголовки и группы ─────────────────────────

/** Большой заголовок экрана (Large Title). */
@Composable
fun LargeTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.displaySmall,
            color = Ios.colors.label,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        if (trailing != null) trailing()
    }
}

/** Сгруппированная карточка со скруглёнными углами: контейнер для строк списка. */
@Composable
fun GroupedCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Ios.colors.card),
        content = content,
    )
}

@Composable
fun GroupHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Ios.colors.secondaryLabel,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 2.dp),
    )
}

@Composable
fun GroupFooter(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = Ios.colors.secondaryLabel,
        modifier = modifier.padding(horizontal = 16.dp),
    )
}

/** Тонкий разделитель строк; [startInset] — отступ слева (под значок). */
@Composable
fun InsetDivider(startInset: Dp = 16.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startInset),
        thickness = Dp.Hairline,
        color = Ios.colors.separator,
    )
}

// ───────────────────────── Значки и кнопки ─────────────────────────

/** Скруглённая оранжевая плитка с белым значком, как в системных настройках iOS. Один цвет для всех плиток. */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Ios.colors.orange),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.62f),
        )
    }
}

/** Круглая плоская кнопка со значком в панели заголовка («Назад», «Очистить»). Значок голубой, как у всех кнопок панели. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Ios.colors
    Box(
        modifier = modifier
            .iosClickable(enabled = enabled, onClick = onClick)
            .size(44.dp)
            .floating(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) c.accentText else c.tertiaryLabel,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Вид кнопки [IosButton]. */
enum class IosButtonStyle {
    /** Серая заливка, тёмный текст и серый кружок со значком. */
    Neutral,

    /** Сплошная заливка цветом, белый текст и белый кружок с цветным значком. */
    Filled,

    /** Лёгкая заливка цветом, цветной текст и цветной кружок с белым значком. */
    Tinted,
}

private class ButtonColors(val container: Color, val content: Color, val badge: Color, val glyph: Color)

/**
 * Компактная кнопка-капсула в стиле iOS: значок в кружке и подпись. По умолчанию занимает ровно
 * столько места, сколько нужно содержимому; для кнопки на всю ширину или в `Row` с весом
 * передайте [modifier] (`fillMaxWidth()`, `weight(1f)`) — содержимое останется по центру.
 *
 * @param tint цвет для [IosButtonStyle.Filled] и [IosButtonStyle.Tinted]; по умолчанию голубой.
 * @param height высота; меньше 40 dp включает уменьшенный вариант (для кнопок рядом с заголовком).
 */
@Composable
fun IosButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IosButtonStyle = IosButtonStyle.Neutral,
    tint: Color? = null,
    height: Dp = 44.dp,
) {
    val c = Ios.colors
    val base = tint ?: if (style == IosButtonStyle.Filled) c.accent else c.accentText
    val colors = when (style) {
        IosButtonStyle.Neutral -> ButtonColors(c.fill, c.label, c.tertiaryLabel, Color.White)
        IosButtonStyle.Filled -> ButtonColors(base, Color.White, Color.White, base)
        IosButtonStyle.Tinted ->
            ButtonColors(base.copy(alpha = if (c.isDark) 0.20f else 0.12f), base, base, Color.White)
    }
    val compact = height < 40.dp
    val badgeSize = if (compact) 20.dp else 26.dp

    Row(
        modifier = modifier
            .iosClickable(onClick = onClick)
            .height(height)
            .clip(CircleShape)
            .background(colors.container)
            .padding(start = if (compact) 7.dp else 9.dp, end = if (compact) 14.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .clip(CircleShape)
                .background(colors.badge),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.glyph,
                modifier = Modifier.size(badgeSize * 0.64f),
            )
        }
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
            color = colors.content,
            maxLines = 1,
        )
    }
}

/** Текстовая ссылка голубого цвета («Весь журнал», «Открыть»). */
@Composable
fun LinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Ios.colors.accentText,
        modifier = modifier
            .iosClickable(role = Role.Button, pressedScale = 0.94f, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    )
}

/** Плашка-подсказка на белой/чёрной карточке с оранжевым значком. С [actionLabel] справа показывается текст, без него — шеврон. */
@Composable
fun Banner(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
) {
    val c = Ios.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .iosClickable(role = Role.Button, pressedScale = 0.98f, onClick = onClick)
            .clip(RoundedCornerShape(22.dp))
            .background(c.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(Icons.Rounded.Error)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = c.label,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = c.accentText,
            )
        } else {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = c.tertiaryLabel,
            )
        }
    }
}

// ───────────────────────── Элементы управления ─────────────────────────

/** Переключатель iOS (51×31). Состоянием управляет строка-родитель (toggleable). */
@Composable
fun IosSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val track by animateColorAsState(
        targetValue = if (checked) c.accent else c.track,
        animationSpec = tween(220),
        label = "switch-track",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "switch-thumb",
    )
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(track)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(27.dp)
                .background(Color.White, CircleShape)
                .border(0.5.dp, thumbEdge(), CircleShape)
        )
    }
}

/** Слайдер iOS: тонкая дорожка и белый круглый бегунок. Шаг задаёт вызывающий код. */
@Composable
fun IosSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    val density = LocalDensity.current
    val thumb = 28.dp
    var widthPx by remember { mutableFloatStateOf(0f) }
    val currentOnChange by rememberUpdatedState(onValueChange)

    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val widthDp = with(density) { widthPx.toDp() }
    val travel = (widthDp - thumb).coerceAtLeast(0.dp)

    fun valueAt(x: Float): Float {
        val thumbPx = with(density) { thumb.toPx() }
        val travelPx = (widthPx - thumbPx).coerceAtLeast(1f)
        val f = ((x - thumbPx / 2f) / travelPx).coerceIn(0f, 1f)
        return valueRange.start + f * span
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(valueRange) {
                detectTapGestures { offset -> currentOnChange(valueAt(offset.x)) }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentOnChange(valueAt(change.position.x))
                }
            }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                setProgress { target ->
                    currentOnChange(target.coerceIn(valueRange.start, valueRange.endInclusive))
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Дорожка
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(c.track)
        )
        // Заполненная часть: до центра бегунка
        Box(
            Modifier
                .width(thumb / 2 + travel * fraction)
                .height(6.dp)
                .clip(CircleShape)
                .background(c.accent)
        )
        // Бегунок
        Box(
            Modifier
                .offset(x = travel * fraction)
                .size(thumb)
                .background(Color.White, CircleShape)
                .border(0.5.dp, thumbEdge(), CircleShape)
        )
    }
}

/** Тонкая кромка белого бегунка: в светлой теме заменяет тень, в тёмной не нужна. */
@Composable
private fun thumbEdge(): Color = if (Ios.colors.isDark) Color.Transparent else Color(0x33000000)
