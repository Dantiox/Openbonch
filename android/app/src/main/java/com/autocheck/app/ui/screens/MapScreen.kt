package com.autocheck.app.ui.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.PathParser
import org.json.JSONObject

// ─────────────────────────────────────────────────────────────
// Данные схемы этажа и аудиторий
// ─────────────────────────────────────────────────────────────

data class RoomShape(
    val id: String,
    val name: String,
    val shape: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val colorType: String,
    val building: String,
    val points: List<Offset> = emptyList()
)

data class FloorMapData(
    val floor: Int,
    val walls: List<Path>,
    val courtyards: List<Path>,
    val rooms: List<RoomShape>
)

private fun loadFloors(context: Context): Map<Int, FloorMapData> {
    val result = mutableMapOf<Int, FloorMapData>()
    try {
        val jsonStr = context.assets.open("maps/campus_floors.json").bufferedReader().use { it.readText() }
        val root = JSONObject(jsonStr)

        for (floor in 1..7) {
            val key = floor.toString()
            if (!root.has(key)) continue
            val obj = root.getJSONObject(key)

            // Стены
            val wallsList = mutableListOf<Path>()
            val wallsArr = obj.optJSONArray("walls")
            if (wallsArr != null) {
                for (i in 0 until wallsArr.length()) {
                    val d = wallsArr.getString(i)
                    try {
                        wallsList.add(PathParser.createPathFromPathData(d).asComposePath())
                    } catch (_: Exception) {}
                }
            }

            // Дворы
            val courtyardsList = mutableListOf<Path>()
            val courtsArr = obj.optJSONArray("courtyards")
            if (courtsArr != null) {
                for (i in 0 until courtsArr.length()) {
                    val d = courtsArr.getString(i)
                    try {
                        courtyardsList.add(PathParser.createPathFromPathData(d).asComposePath())
                    } catch (_: Exception) {}
                }
            }

            // Аудитории
            val roomsList = mutableListOf<RoomShape>()
            val roomsArr = obj.optJSONArray("rooms")
            if (roomsArr != null) {
                for (i in 0 until roomsArr.length()) {
                    val rObj = roomsArr.getJSONObject(i)
                    val pts = mutableListOf<Offset>()
                    val ptsArr = rObj.optJSONArray("points")
                    if (ptsArr != null) {
                        for (p in 0 until ptsArr.length()) {
                            val pt = ptsArr.getJSONArray(p)
                            pts.add(Offset(pt.getDouble(0).toFloat(), pt.getDouble(1).toFloat()))
                        }
                    }

                    roomsList.add(
                        RoomShape(
                            id = rObj.optString("id"),
                            name = rObj.optString("name"),
                            shape = rObj.optString("shape", "rect"),
                            x = rObj.optDouble("x", 0.0).toFloat(),
                            y = rObj.optDouble("y", 0.0).toFloat(),
                            w = rObj.optDouble("w", 0.0).toFloat(),
                            h = rObj.optDouble("h", 0.0).toFloat(),
                            colorType = rObj.optString("color", "slate"),
                            building = rObj.optString("building", "1"),
                            points = pts
                        )
                    )
                }
            }

            result[floor] = FloorMapData(floor, wallsList, courtyardsList, roomsList)
        }
    } catch (_: Exception) {}
    return result
}

// ─────────────────────────────────────────────────────────────
// Главный экран Навигатора кампуса (Стиль Bonch Go 1:1)
// ─────────────────────────────────────────────────────────────

@Composable
fun MapScreen(
    bottomPadding: Dp,
    highlightRoom: String? = null
) {
    val context = LocalContext.current
    val allFloors = remember { loadFloors(context) }

    var selectedCampus by remember { mutableIntStateOf(0) } // 0: СПбГУТ, 1: СПбКТ
    var selectedFloor by remember { mutableIntStateOf(1) }
    var searchQuery by remember { mutableStateOf(highlightRoom ?: "") }
    var selectedRoom by remember { mutableStateOf<RoomShape?>(null) }
    var originRoom by remember { mutableStateOf<RoomShape?>(null) }
    var destRoom by remember { mutableStateOf<RoomShape?>(null) }
    var isSelectingOrigin by remember { mutableStateOf(false) }

    // Масштаб и панорамирование
    var scale by remember { mutableFloatStateOf(0.72f) }
    var offsetX by remember { mutableFloatStateOf(-300f) }
    var offsetY by remember { mutableFloatStateOf(-120f) }

    val currentFloorData = allFloors[selectedFloor]

    // Настройка шрифта для отрисовки номеров комнат
    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
        }
    }

    // Сброс масштаба в центр здания
    fun resetView() {
        scale = 0.72f
        offsetX = -300f
        offsetY = -120f
    }

    // Центрирование на аудитории
    fun centerOnRoom(room: RoomShape) {
        scale = 1.6f
        offsetX = -room.x * scale + 500f
        offsetY = -room.y * scale + 800f
    }

    // Обработка внешнего перехода из расписания
    LaunchedEffect(highlightRoom) {
        if (!highlightRoom.isNullOrBlank()) {
            searchQuery = highlightRoom
            // Ищем комнату по всем этажам
            for (f in 1..7) {
                val match = allFloors[f]?.rooms?.firstOrNull { 
                    it.name.equals(highlightRoom, ignoreCase = true) ||
                    it.id.equals(highlightRoom, ignoreCase = true)
                }
                if (match != null) {
                    selectedFloor = f
                    selectedRoom = match
                    destRoom = match
                    centerOnRoom(match)
                    break
                }
            }
        }
    }

    // Подсказки поиска
    val searchSuggestions = remember(searchQuery, allFloors) {
        if (searchQuery.trim().length >= 2) {
            val q = searchQuery.trim().lowercase()
            val list = mutableListOf<Pair<Int, RoomShape>>()
            for (f in 1..7) {
                allFloors[f]?.rooms?.forEach { r ->
                    if (r.name.lowercase().contains(q) || r.id.lowercase().contains(q)) {
                        list.add(f to r)
                    }
                }
            }
            list.take(6)
        } else {
            emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1015)) // Глубокий темный фон
    ) {
        // ─────────────────────────────────────────────────────────────
        // Слой 1: Точечная координатная сетка (Blueprint Grid)
        // ─────────────────────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 32.dp.toPx()
            var x = 0f
            while (x < size.width) {
                var y = 0f
                while (y < size.height) {
                    drawCircle(
                        color = Color(0xFF1E232E),
                        radius = 1.4f,
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 2: Интерактивная векторная карта (GPU Canvas с жестами)
        // ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.35f, 4.5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val localX = (tapOffset.x - offsetX) / scale
                        val localY = (tapOffset.y - offsetY) / scale

                        val tapped = currentFloorData?.rooms?.firstOrNull { r ->
                            localX >= r.x && localX <= r.x + r.w &&
                            localY >= r.y && localY <= r.y + r.h
                        }

                        if (tapped != null) {
                            if (isSelectingOrigin) {
                                originRoom = tapped
                                isSelectingOrigin = false
                            } else {
                                selectedRoom = tapped
                                destRoom = tapped
                                searchQuery = tapped.name
                            }
                        }
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
            ) {
                if (currentFloorData != null) {
                    // 1. Внешние контуры стен и переходов
                    for (wall in currentFloorData.walls) {
                        drawPath(wall, color = Color(0xFF161B24))
                        drawPath(wall, color = Color(0xFF2A3241), style = Stroke(width = 2.5f))
                    }

                    // 2. Внутренние световые дворы (темные вырезы)
                    for (court in currentFloorData.courtyards) {
                        drawPath(court, color = Color(0xFF0B0E14))
                        drawPath(court, color = Color(0xFF1E2430), style = Stroke(width = 1.5f))
                    }

                    // 3. Стрелочка входа (как на скриншоте)
                    if (selectedFloor == 1) {
                        val arrowX = 1080f
                        val arrowY = 150f
                        val arrowPath = Path().apply {
                            moveTo(arrowX, arrowY)
                            lineTo(arrowX - 16f, arrowY - 24f)
                            lineTo(arrowX + 16f, arrowY - 24f)
                            close()
                        }
                        drawPath(arrowPath, color = Color(0xFF8B949E))
                    }

                    // 4. Отрисовка аудиторий с семантическими цветами
                    for (room in currentFloorData.rooms) {
                        val isSelected = selectedRoom?.id == room.id || 
                                        (searchQuery.isNotBlank() && room.name.equals(searchQuery, ignoreCase = true))

                        val fillColor = when {
                            isSelected -> Color(0xFFFF6D00)
                            room.colorType == "green" -> Color(0xFF2E7D32)
                            room.colorType == "orange" -> Color(0xFFE65100)
                            room.colorType == "yellow" -> Color(0xFFF57F17)
                            room.colorType == "pink" -> Color(0xFFC2185B)
                            room.colorType == "cyan" -> Color(0xFF00838F)
                            else -> Color(0xFF1E2430) // Обычные аудитории
                        }

                        val strokeColor = when {
                            isSelected -> Color(0xFFFFD54F)
                            room.colorType == "green" -> Color(0xFF81C784)
                            room.colorType == "orange" -> Color(0xFFFFB74D)
                            room.colorType == "yellow" -> Color(0xFFFFF176)
                            room.colorType == "pink" -> Color(0xFFF06292)
                            room.colorType == "cyan" -> Color(0xFF4DD0E1)
                            else -> Color(0xFF2D3545)
                        }

                        if (room.shape == "rect") {
                            drawRoundRect(
                                color = fillColor,
                                topLeft = Offset(room.x, room.y),
                                size = Size(room.w, room.h),
                                cornerRadius = CornerRadius(5f)
                            )
                            drawRoundRect(
                                color = strokeColor,
                                topLeft = Offset(room.x, room.y),
                                size = Size(room.w, room.h),
                                cornerRadius = CornerRadius(5f),
                                style = Stroke(width = if (isSelected) 3.5f else 1.2f)
                            )
                        } else if (room.points.isNotEmpty()) {
                            val polyPath = Path().apply {
                                moveTo(room.points[0].x, room.points[0].y)
                                for (p in 1 until room.points.size) {
                                    lineTo(room.points[p].x, room.points[p].y)
                                }
                                close()
                            }
                            drawPath(polyPath, color = fillColor)
                            drawPath(polyPath, color = strokeColor, style = Stroke(width = if (isSelected) 3.5f else 1.2f))
                        }

                        // Номер аудитории внутри плашки
                        if (scale >= 0.65f && room.name.isNotEmpty()) {
                            val cx = room.x + room.w / 2f
                            val cy = room.y + room.h / 2f + 4.5f
                            drawContext.canvas.nativeCanvas.drawText(room.name, cx, cy, textPaint)
                        }
                    }

                    // 5. Отрисовка маршрута (если выбраны Откуда и Куда)
                    if (originRoom != null && destRoom != null && selectedFloor == originRoom?.building?.toIntOrNull()) {
                        val oX = originRoom!!.x + originRoom!!.w / 2f
                        val oY = originRoom!!.y + originRoom!!.h / 2f
                        val dX = destRoom!!.x + destRoom!!.w / 2f
                        val dY = destRoom!!.y + destRoom!!.h / 2f

                        drawLine(
                            color = Color(0xFFFF6D00),
                            start = Offset(oX, oY),
                            end = Offset(dX, dY),
                            strokeWidth = 5f
                        )
                        drawCircle(color = Color(0xFF00E676), radius = 8f, center = Offset(oX, oY))
                        drawCircle(color = Color(0xFFFF6D00), radius = 8f, center = Offset(dX, dY))
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 3: Верхний левый переключатель кампусов (СПбГУТ / СПбКТ)
        // ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF161A22).copy(alpha = 0.95f))
                .border(1.dp, Color(0xFF262C38), RoundedCornerShape(18.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selectedCampus == 0) Color(0xFFFF6D00) else Color.Transparent)
                    .clickable { selectedCampus = 0 }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = "СПбГУТ",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selectedCampus == 1) Color(0xFFFF6D00) else Color.Transparent)
                    .clickable { selectedCampus = 1 }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = "СПбКТ",
                    color = if (selectedCampus == 1) Color.White else Color(0xFF8B949E),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 4: Правая вертикальная панель этажей и зума (Скриншот 1:1)
        // ─────────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF161A22).copy(alpha = 0.94f)
            ),
            border = BorderStroke(1.dp, Color(0xFF262C38))
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (floor in 7 downTo 1) {
                    val isCurrent = selectedFloor == floor
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (isCurrent) Color(0xFFFF6D00) else Color.Transparent)
                            .clickable { selectedFloor = floor },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$floor",
                            color = if (isCurrent) Color.White else Color(0xFF8B949E),
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier
                        .width(22.dp)
                        .padding(vertical = 4.dp),
                    color = Color(0xFF262C38)
                )

                // Сброс масштаба в центр
                IconButton(
                    onClick = { resetView() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Rounded.CropFree,
                        contentDescription = "Центр",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Приблизить (+)
                IconButton(
                    onClick = { scale = (scale * 1.35f).coerceIn(0.35f, 4.5f) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Приблизить",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Отдалить (-)
                IconButton(
                    onClick = { scale = (scale / 1.35f).coerceIn(0.35f, 4.5f) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Rounded.Remove,
                        contentDescription = "Отдалить",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 5: Бейдж «🟠 БЕТА» слева внизу
        // ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = bottomPadding + 220.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161A22).copy(alpha = 0.9f))
                .border(1.dp, Color(0xFF262C38), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF6D00))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "БЕТА",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 6: Выпадающий список подсказок поиска (Autocomplete)
        // ─────────────────────────────────────────────────────────────
        if (searchSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = bottomPadding + 210.dp)
                    .heightIn(max = 240.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1F2B)),
                border = BorderStroke(1.dp, Color(0xFF2D3545))
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(searchSuggestions) { (floor, room) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFloor = floor
                                    selectedRoom = room
                                    destRoom = room
                                    searchQuery = room.name
                                    centerOnRoom(room)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.MeetingRoom,
                                contentDescription = null,
                                tint = Color(0xFFFF6D00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Аудитория ${room.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Корпус ${room.building} • $floor этаж",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFF262C38))
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 7: Нижняя карточка навигации (Bottom Sheet точно по фото)
        // ─────────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = bottomPadding),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
            border = BorderStroke(1.dp, Color(0xFF262C38))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF374151))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Статус расписания
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Пары на сегодня закончились",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Поисковая строка с колокольчиком
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "Аудитория, кафедра, буфет...",
                                fontSize = 14.sp,
                                color = Color(0xFF64748B)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    selectedRoom = null
                                    destRoom = null
                                }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Очистить",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1F2430),
                            unfocusedContainerColor = Color(0xFF1F2430),
                            focusedBorderColor = Color(0xFFFF6D00),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // Кнопка колокольчика
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1F2430))
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = "Уведомления",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Кнопки маршрутизатора: "Откуда" / "Куда"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Откуда
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1F2430))
                            .border(
                                1.dp,
                                if (isSelectingOrigin) Color(0xFF00E676) else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { isSelectingOrigin = true }
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = originRoom?.name ?: "Откуда",
                                color = if (originRoom != null) Color.White else Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Куда
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1F2430))
                            .clickable { }
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF6D00))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = destRoom?.name ?: "Куда",
                                color = if (destRoom != null) Color.White else Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Всплывающая карточка деталей выбранной аудитории
                AnimatedVisibility(
                    visible = selectedRoom != null,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    val room = selectedRoom
                    if (room != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2430)),
                            border = BorderStroke(1.dp, Color(0xFFFF6D00).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF6D00)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Navigation,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Аудитория ${room.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Корпус ${room.building} • $selectedFloor этаж",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                IconButton(onClick = { selectedRoom = null }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Закрыть",
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
