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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
        context.assets.open("maps/${floor}k.svg").bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        ""
    }
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
    var isWebViewReady by remember { mutableStateOf(false) }

    // Обработка перехода из расписания или внешнего запроса
    LaunchedEffect(highlightRoom) {
        if (!highlightRoom.isNullOrBlank()) {
            searchQuery = highlightRoom
            // Ищем комнату по номеру
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
                // Пытаемся определить этаж по первой цифре номера
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

    // Обновление этажа в WebView
    LaunchedEffect(selectedFloor, isWebViewReady, webViewInstance) {
        val wv = webViewInstance
        if (isWebViewReady && wv != null) {
            val svgContent = loadSvg(context, selectedFloor)
            if (svgContent.isNotEmpty()) {
                val quoted = JSONObject.quote(svgContent)
                wv.evaluateJavascript("window.loadSvg($quoted);", null)

                // Если есть активный поиск - подсвечиваем на новом этаже
                if (searchQuery.isNotBlank()) {
                    val q = JSONObject.quote(searchQuery)
                    wv.evaluateJavascript("window.highlightRoom($q);", null)
                }
            }
        }
    }

    // Обновление темы в WebView
    LaunchedEffect(isDark, isWebViewReady, webViewInstance) {
        val wv = webViewInstance
        if (isWebViewReady && wv != null) {
            wv.evaluateJavascript("window.setTheme($isDark);", null)
        }
    }

    // Обновление фокуса корпуса
    LaunchedEffect(selectedBuildingTab, isWebViewReady, webViewInstance) {
        val wv = webViewInstance
        if (isWebViewReady && wv != null) {
            wv.evaluateJavascript("window.focusBuilding($selectedBuildingTab);", null)
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
                text = "Пр. Большевиков, 22 • Официальные планы этажей",
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
                            webViewInstance?.evaluateJavascript("window.highlightRoom('');", null)
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
                                        webViewInstance?.evaluateJavascript("window.highlightRoom(${JSONObject.quote(room.title)});", null)
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

        // Основная интерактивная карта (Векторный SVG план здания через WebView)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) Color(0xFF1E2026) else Color(0xFFF1F3F6))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.allowFileAccess = true
                        settings.domStorageEnabled = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        addJavascriptInterface(
                            MapBridge { roomId ->
                                // Ищем комнату по ID
                                val clicked = allRooms.firstOrNull { 
                                    it.id.equals(roomId, ignoreCase = true) ||
                                    it.id.replace("-", "").equals(roomId.replace("-", ""), ignoreCase = true)
                                }
                                if (clicked != null) {
                                    selectedRoom = clicked
                                    searchQuery = clicked.title
                                } else {
                                    // Резервный поиск
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

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isWebViewReady = true
                                val svg = loadSvg(ctx, selectedFloor)
                                if (svg.isNotEmpty()) {
                                    evaluateJavascript("window.loadSvg(${JSONObject.quote(svg)});", null)
                                    evaluateJavascript("window.setTheme($isDark);", null)
                                    if (searchQuery.isNotBlank()) {
                                        evaluateJavascript("window.highlightRoom(${JSONObject.quote(searchQuery)});", null)
                                    }
                                }
                            }
                        }

                        loadUrl("file:///android_asset/maps/viewer.html")
                        webViewInstance = this
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                }
            )

            // Бейдж текущего этажа в углу
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
                        webViewInstance?.evaluateJavascript("window.highlightRoom('');", null)
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
