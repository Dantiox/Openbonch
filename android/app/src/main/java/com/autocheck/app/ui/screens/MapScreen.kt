package com.autocheck.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

data class CampusRoom(
    val id: String,
    val title: String,
    val floor: Int,
    val building: String,
    val about: String,
    val x: Float,
    val y: Float,
)

private fun loadRooms(context: Context): List<CampusRoom> {
    return try {
        val jsonStr = context.assets.open("maps/rooms.json").bufferedReader().use { it.readText() }
        val array = JSONArray(jsonStr)
        val list = ArrayList<CampusRoom>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                CampusRoom(
                    id = obj.optString("id"),
                    title = obj.optString("title"),
                    floor = obj.optInt("floor", 1),
                    building = obj.optString("building", "1"),
                    about = obj.optString("about"),
                    x = obj.optDouble("x", 0.0).toFloat(),
                    y = obj.optDouble("y", 0.0).toFloat(),
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun loadSvg(context: Context, floor: Int): String {
    return try {
        val raw = context.assets.open("maps/${floor}k.svg").bufferedReader().use { it.readText() }
        raw.replace(Regex("<\\?xml[^\\?]+\\?>"), "")
            .replace(Regex("<!--[\\s\\S]*?-->"), "")
            .trim()
    } catch (_: Exception) {
        ""
    }
}

private fun buildMapHtml(svgContent: String, isDark: Boolean, query: String?): String {
    val bgColor = if (isDark) "#121418" else "#F1F5F9"
    val wallColor = if (isDark) "#222630" else "#FFFFFF"
    val wallStroke = if (isDark) "#4B5563" else "#94A3B8"
    val roomColor = if (isDark) "#2E3646" else "#F8FAFC"
    val roomStroke = if (isDark) "#64748B" else "#64748B"
    val textColor = if (isDark) "#F8FAFC" else "#0F172A"
    val courtyardColor = if (isDark) "#181A20" else "#E2E8F0"
    val corridorColor = if (isDark) "#252B3B" else "#EFF6FF"

    val sanitizedQuery = query?.replace("'", "\\'")?.replace("\"", "\\\"") ?: ""

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=1800, initial-scale=0.22, minimum-scale=0.15, maximum-scale=3.5, user-scalable=yes">
<style>
* { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
html, body {
    margin: 0;
    padding: 0;
    width: 1800px;
    height: 1800px;
    background-color: $bgColor;
    user-select: none;
    -webkit-user-select: none;
    overflow: visible;
}
svg {
    width: 1800px;
    height: 1800px;
    display: block;
}
/* Стены и контуры здания */
svg path[fill="#FDFDFD"], svg polygon[fill="#FDFDFD"] {
    fill: $wallColor !important;
    stroke: $wallStroke !important;
    stroke-width: 2px !important;
}
/* Внутренние дворы */
svg path[fill="#F9FCF7"], svg polygon[fill="#F9FCF7"] {
    fill: $courtyardColor !important;
}
/* Коридоры */
svg .st1 {
    fill: $corridorColor !important;
    stroke: $wallStroke !important;
    stroke-width: 1.5px !important;
}
/* Аудитории и кабинеты */
svg .st2 {
    fill: $roomColor !important;
    stroke: $roomStroke !important;
    stroke-width: 1.5px !important;
    cursor: pointer;
    transition: fill 0.15s, stroke 0.15s;
}
svg .st2:active {
    fill: #3B82F6 !important;
    stroke: #60A5FA !important;
}
/* Номера аудиторий */
svg text, svg .st3, svg .st4 {
    fill: $textColor !important;
    font-family: -apple-system, BlinkMacSystemFont, Roboto, "Segoe UI", sans-serif !important;
    font-size: 19px !important;
    font-weight: 800 !important;
    pointer-events: none;
}
/* Линии переходов */
svg .st5 {
    stroke: #F59E0B !important;
    stroke-width: 2px !important;
}
/* Подсветка найденной аудитории */
.room-highlight {
    fill: #FF6D00 !important;
    stroke: #FFE082 !important;
    stroke-width: 6px !important;
    filter: drop-shadow(0 0 18px rgba(255, 109, 0, 1)) !important;
}
.room-selected {
    fill: #2563EB !important;
    stroke: #93C5FD !important;
    stroke-width: 5px !important;
    filter: drop-shadow(0 0 16px rgba(37, 99, 235, 0.95)) !important;
}
</style>
</head>
<body>
$svgContent

<script>
document.addEventListener('click', function(e) {
    var target = e.target;
    while (target && target.tagName !== 'svg' && target !== document.body) {
        var id = target.getAttribute('id');
        if (id && (id.indexOf('k1-') === 0 || id.indexOf('k2-') === 0)) {
            var prev = document.querySelectorAll('.room-selected');
            for (var i = 0; i < prev.length; i++) prev[i].classList.remove('room-selected');
            target.classList.add('room-selected');
            if (window.AndroidBridge && window.AndroidBridge.onRoomClicked) {
                window.AndroidBridge.onRoomClicked(id);
            }
            return;
        }
        target = target.parentElement;
    }
});

function highlight(query) {
    if (!query) return;
    var q = query.trim().toLowerCase();
    
    var prev = document.querySelectorAll('.room-highlight');
    for (var i = 0; i < prev.length; i++) prev[i].classList.remove('room-highlight');

    var found = document.getElementById(q) || document.getElementById('k1-' + q) || document.getElementById('k2-' + q);
    if (!found) {
        var texts = document.querySelectorAll('text');
        for (var j = 0; j < texts.length; j++) {
            var val = (texts[j].textContent || '').trim().toLowerCase();
            if (val === q || val.indexOf(q) !== -1) {
                found = texts[j];
                break;
            }
        }
    }

    if (found) {
        found.classList.add('room-highlight');
        try {
            found.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
        } catch(e) {}
    }
}

function focusBuilding(bldg) {
    if (bldg === 1) {
        window.scrollTo({ left: 800, top: 400, behavior: 'smooth' });
    } else if (bldg === 2) {
        window.scrollTo({ left: 50, top: 400, behavior: 'smooth' });
    } else {
        window.scrollTo({ left: 350, top: 350, behavior: 'smooth' });
    }
}

window.onload = function() {
    var q = "$sanitizedQuery";
    if (q) {
        setTimeout(function() { highlight(q); }, 150);
    }
};
</script>
</body>
</html>
""".trimIndent()
}

class MapBridge(
    private val onRoomSelected: (String) -> Unit
) {
    @JavascriptInterface
    fun onRoomClicked(roomId: String) {
        onRoomSelected(roomId)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(
    bottomPadding: Dp,
    highlightRoom: String? = null
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val allRooms = remember { loadRooms(context) }

    var selectedFloor by remember { mutableIntStateOf(1) }
    var selectedBuildingTab by remember { mutableIntStateOf(0) } // 0: Все, 1: Корпус 1, 2: Корпус 2
    var searchQuery by remember { mutableStateOf(highlightRoom ?: "") }
    var selectedRoom by remember { mutableStateOf<CampusRoom?>(null) }
    var showSuggestions by remember { mutableStateOf(false) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Обработка перехода из расписания
    LaunchedEffect(highlightRoom) {
        if (!highlightRoom.isNullOrBlank()) {
            searchQuery = highlightRoom
            val match = allRooms.firstOrNull { 
                it.title.equals(highlightRoom, ignoreCase = true) ||
                it.id.equals(highlightRoom, ignoreCase = true) ||
                it.about.contains(highlightRoom, ignoreCase = true)
            }
            if (match != null) {
                selectedFloor = match.floor
                selectedRoom = match
                selectedBuildingTab = if (match.building == "2") 2 else 1
            } else {
                val digits = highlightRoom.filter { it.isDigit() }
                if (digits.length >= 3) {
                    val floorCandidate = digits.first().digitToIntOrNull()
                    if (floorCandidate != null && floorCandidate in 1..7) {
                        selectedFloor = floorCandidate
                    }
                }
            }
        }
    }

    // Загрузка карты при смене этажа или темы
    LaunchedEffect(selectedFloor, isDark, webViewInstance) {
        val wv = webViewInstance
        if (wv != null) {
            val svg = loadSvg(context, selectedFloor)
            if (svg.isNotEmpty()) {
                val html = buildMapHtml(svg, isDark, searchQuery)
                wv.loadDataWithBaseURL("https://lk.sut.ru/", html, "text/html", "UTF-8", null)
            }
        }
    }

    // Фокус на корпусе
    LaunchedEffect(selectedBuildingTab, webViewInstance) {
        val wv = webViewInstance
        if (wv != null) {
            wv.evaluateJavascript("focusBuilding($selectedBuildingTab);", null)
        }
    }

    // Поиск совпадений для автодополнения
    val matchingRooms = remember(searchQuery, allRooms) {
        if (searchQuery.trim().length >= 2) {
            val q = searchQuery.trim().lowercase()
            allRooms.filter { 
                it.title.lowercase().contains(q) || it.about.lowercase().contains(q)
            }.take(8)
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
    ) {
        // Заголовок экрана
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Навигатор кампуса",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Пр. Большевиков, 22 • Векторные схемы этажей",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Поле поиска с автокомплитом
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    showSuggestions = it.trim().length >= 2
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск: 504/1, 210/2, столовая, деканат...") },
                leadingIcon = { 
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            selectedRoom = null
                            showSuggestions = false
                            webViewInstance?.evaluateJavascript("highlight('');", null)
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Выпадающий список подсказок
            if (showSuggestions && matchingRooms.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp)
                        .heightIn(max = 240.dp),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(matchingRooms) { room ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchQuery = room.title
                                        selectedRoom = room
                                        selectedFloor = room.floor
                                        selectedBuildingTab = if (room.building == "2") 2 else 1
                                        showSuggestions = false
                                        webViewInstance?.evaluateJavascript("highlight(${JSONObject.quote(room.title)});", null)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.MeetingRoom,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = room.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Этаж ${room.floor} • Корпус ${room.building} — ${room.about}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Вкладки корпусов: Все / Корпус 1 / Корпус 2
        TabRow(
            selectedTabIndex = selectedBuildingTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            val tabs = listOf("Все корпуса", "Корпус 1", "Корпус 2")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedBuildingTab == index,
                    onClick = { selectedBuildingTab = index },
                    text = { 
                        Text(
                            text = title, 
                            fontWeight = if (selectedBuildingTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        ) 
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Выбор этажа: 1..7
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Layers,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Этаж:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            for (floor in 1..7) {
                FilterChip(
                    selected = selectedFloor == floor,
                    onClick = {
                        selectedFloor = floor
                    },
                    label = { 
                        Text(
                            text = "$floor",
                            fontSize = 13.sp,
                            fontWeight = if (selectedFloor == floor) FontWeight.Bold else FontWeight.Normal
                        ) 
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Интерактивная карта (SVG через WebView)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) Color(0xFF121418) else Color(0xFFF1F5F9))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        addJavascriptInterface(
                            MapBridge { roomId ->
                                val clicked = allRooms.firstOrNull { 
                                    it.id.equals(roomId, ignoreCase = true) ||
                                    it.id.replace("-", "").equals(roomId.replace("-", ""), ignoreCase = true)
                                }
                                if (clicked != null) {
                                    selectedRoom = clicked
                                    searchQuery = clicked.title
                                } else {
                                    selectedRoom = CampusRoom(
                                        id = roomId,
                                        title = roomId.replace("k1-", "").replace("k2-", ""),
                                        floor = selectedFloor,
                                        building = if (roomId.startsWith("k2")) "2" else "1",
                                        about = "Помещение на $selectedFloor этаже",
                                        x = 0f,
                                        y = 0f
                                    )
                                }
                            },
                            "AndroidBridge"
                        )

                        val svg = loadSvg(ctx, selectedFloor)
                        if (svg.isNotEmpty()) {
                            val html = buildMapHtml(svg, isDark, searchQuery)
                            loadDataWithBaseURL("https://lk.sut.ru/", html, "text/html", "UTF-8", null)
                        }

                        webViewInstance = this
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                }
            )

            // Бейдж этажа
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "План: $selectedFloor этаж • Корпус ${if (selectedBuildingTab == 0) "1 и 2" else "$selectedBuildingTab"}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Кнопки зума и сброса (+ / − / ⤢)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { webViewInstance?.zoomIn() },
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Приблизить", modifier = Modifier.size(20.dp))
                }
                FloatingActionButton(
                    onClick = { webViewInstance?.zoomOut() },
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Отдалить", modifier = Modifier.size(20.dp))
                }
                FloatingActionButton(
                    onClick = { 
                        webViewInstance?.evaluateJavascript("window.scrollTo({ left: 350, top: 350, behavior: 'smooth' });", null)
                    },
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Rounded.FitScreen, contentDescription = "По центру", modifier = Modifier.size(20.dp))
                }
            }
        }

        // Карточка выбранной аудитории внизу
        val activeRoom = selectedRoom ?: allRooms.firstOrNull { 
            searchQuery.isNotBlank() && (it.title.equals(searchQuery, ignoreCase = true) || it.id.equals(searchQuery, ignoreCase = true)) 
        }

        if (activeRoom != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (activeRoom.title.isNotBlank()) "Аудитория ${activeRoom.title}" else "Помещение ${activeRoom.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Корпус ${activeRoom.building} • ${activeRoom.floor} этаж",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (activeRoom.about.isNotBlank() && activeRoom.about != activeRoom.title) {
                            Text(
                                text = activeRoom.about,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                maxLines = 2
                            )
                        }
                    }
                    IconButton(onClick = {
                        selectedRoom = null
                        searchQuery = ""
                        webViewInstance?.evaluateJavascript("highlight('');", null)
                    }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
