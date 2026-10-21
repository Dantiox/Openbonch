package com.autocheck.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autocheck.app.data.LogEntry
import com.autocheck.app.ui.components.ExportBar
import com.autocheck.app.ui.components.IconTile
import com.autocheck.app.ui.components.InsetDivider
import com.autocheck.app.ui.components.LargeTitle
import com.autocheck.app.ui.components.RoundIconButton
import com.autocheck.app.ui.theme.Ios

private val GroupRadius = 26.dp

/**
 * Журнал: новые записи сверху, записи сгруппированы в одну скруглённую карточку.
 * Над списком — панель «Копировать / Скачать / Поделиться» для всего журнала (.log).
 */
@Composable
fun LogScreen(
    logs: List<LogEntry>,
    bottomInset: Dp,
    resetSignal: Int,
    onClear: () -> Unit,
    onOpenResponse: (LogEntry) -> Unit,
    onCopy: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
) {
    val c = Ios.colors
    val listState = rememberLazyListState()
    // Повторное нажатие на вкладку «Журнал» — к самым новым записям
    LaunchedEffect(resetSignal) { if (resetSignal > 0) listState.animateScrollToItem(0) }

    Column(Modifier.fillMaxSize()) {
        LargeTitle(
            text = "Журнал",
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            trailing = {
                RoundIconButton(
                    icon = Icons.Rounded.DeleteSweep,
                    contentDescription = "Очистить журнал",
                    onClick = onClear,
                    enabled = logs.isNotEmpty(),
                )
            },
        )

        // Весь журнал целиком (.log): копировать, сохранить в Download или отправить
        ExportBar(
            onCopy = onCopy,
            onDownload = onDownload,
            onShare = onShare,
            enabled = logs.isNotEmpty(),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
        )

        if (logs.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomInset),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(Icons.Rounded.History, size = 56.dp)
                    Text(
                        "Записей пока нет",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.secondaryLabel,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = bottomInset),
            ) {
                itemsIndexed(logs, key = { _, entry -> entry.id }) { index, entry ->
                    val first = index == 0
                    val last = index == logs.lastIndex
                    // Скругляем только верх первой и низ последней записи — получается одна карточка
                    val shape = RoundedCornerShape(
                        topStart = if (first) GroupRadius else 0.dp,
                        topEnd = if (first) GroupRadius else 0.dp,
                        bottomStart = if (last) GroupRadius else 0.dp,
                        bottomEnd = if (last) GroupRadius else 0.dp,
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(c.card)
                    ) {
                        if (!first) InsetDivider(startInset = 38.dp)
                        LogRow(entry, onOpenResponse = onOpenResponse)
                    }
                }
            }
        }
    }
}
