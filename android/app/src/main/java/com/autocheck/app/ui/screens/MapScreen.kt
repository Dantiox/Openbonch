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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.Elevator
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Wc
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
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autocheck.app.data.AutoClickState
import org.json.JSONObject
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────
// Модели данных схемы этажа, аудиторий и объектов инфраструктуры
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

data class Amenity(
    val id: String,
    val type: String, // "stairs", "elevator", "wc_woman", "wc_man", "wc_accessible", "wc"
    val name: String,
    val about: String,
    val x: Float,
    val y: Float
)

data class FloorMapData(
    val floor: Int,
    val walls: List<Path>,
    val courtyards: List<Path>,
    val rooms: List<RoomShape>,
    val amenities: List<Amenity> = emptyList()
)

enum class AmenityFilter(val title: String, val icon: String) {
    ALL("Все", "🏷️"),
    RESTROOMS("Туалеты", "🚻"),
    STAIRS("Лестницы", "🪜"),
    ELEVATORS("Лифты", "🛗")
}

enum class MapTheme(
    val title: String,
    val icon: String,
    val bg: Color,
    val gridDot: Color,
    val wallFill: Color,
    val wallStroke: Color,
    val courtyardFill: Color,
    val courtyardStroke: Color,
    val defaultRoomFill: Color,
    val defaultRoomStroke: Color,
    val surfaceColor: Color,
    val strokeColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isLight: Boolean = false
) {
    TACTICAL(
        title = "Тактическая",
        icon = "🌙",
        bg = Color(0xFF0E1015),
        gridDot = Color(0xFF1E232E),
        wallFill = Color(0xFF161B24),
        wallStroke = Color(0xFF2A3241),
        courtyardFill = Color(0xFF0B0E14),
        courtyardStroke = Color(0xFF1E2430),
        defaultRoomFill = Color(0xFF1E2430),
        defaultRoomStroke = Color(0xFF2D3545),
        surfaceColor = Color(0xFF161A22),
        strokeColor = Color(0xFF262C38),
        textPrimary = Color.White,
        textSecondary = Color(0xFF8B949E)
    ),
    LIGHT(
        title = "Светлая",
        icon = "☀️",
        bg = Color(0xFFF1F5F9),
        gridDot = Color(0xFFCBD5E1),
        wallFill = Color(0xFFFFFFFF),
        wallStroke = Color(0xFF94A3B8),
        courtyardFill = Color(0xFFE2E8F0),
        courtyardStroke = Color(0xFFCBD5E1),
        defaultRoomFill = Color(0xFFE2E8F0),
        defaultRoomStroke = Color(0xFFCBD5E1),
        surfaceColor = Color(0xFFFFFFFF),
        strokeColor = Color(0xFFCBD5E1),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF64748B),
        isLight = true
    ),
    CYBERPUNK(
        title = "Киберпанк",
        icon = "⚡",
        bg = Color(0xFF060B14),
        gridDot = Color(0xFF0D3256),
        wallFill = Color(0xFF0A192F),
        wallStroke = Color(0xFF00E5FF),
        courtyardFill = Color(0xFF03070E),
        courtyardStroke = Color(0xFF005577),
        defaultRoomFill = Color(0xFF0F2642),
        defaultRoomStroke = Color(0xFF00B4D8),
        surfaceColor = Color(0xFF0A192F),
        strokeColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
        textPrimary = Color(0xFF00E5FF),
        textSecondary = Color(0xFF7DD3FC)
    ),
    OLED(
        title = "OLED",
        icon = "🖤",
        bg = Color(0xFF000000),
        gridDot = Color(0xFF1A1A1A),
        wallFill = Color(0xFF080808),
        wallStroke = Color(0xFF333333),
        courtyardFill = Color(0xFF000000),
        courtyardStroke = Color(0xFF222222),
        defaultRoomFill = Color(0xFF121212),
        defaultRoomStroke = Color(0xFF262626),
        surfaceColor = Color(0xFF0A0A0A),
        strokeColor = Color(0xFF222222),
        textPrimary = Color.White,
        textSecondary = Color(0xFF888888)
    )
}

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

            // Объекты инфраструктуры (Лестницы, лифты, туалеты)
            val amenitiesList = mutableListOf<Amenity>()
            val amenitiesArr = obj.optJSONArray("amenities")
            if (amenitiesArr != null) {
                for (i in 0 until amenitiesArr.length()) {
                    val aObj = amenitiesArr.getJSONObject(i)
                    amenitiesList.add(
                        Amenity(
                            id = aObj.optString("id"),
                            type = aObj.optString("type"),
                            name = aObj.optString("name"),
                            about = aObj.optString("about"),
                            x = aObj.optDouble("x", 0.0).toFloat(),
                            y = aObj.optDouble("y", 0.0).toFloat()
                        )
                    )
                }
            }

            result[floor] = FloorMapData(floor, wallsList, courtyardsList, roomsList, amenitiesList)
        }
    } catch (_: Exception) {}
    return result
}

// Координаты главного входа университета на 1 этаже
private val MAIN_ENTRANCE_POINT = Offset(1080f, 180f)
private const val MAIN_ENTRANCE_NAME = "Главный вход"

// ─────────────────────────────────────────────────────────────
// Главный экран Навигатора кампуса (Полноценная навигация)
// ─────────────────────────────────────────────────────────────

@Composable
fun MapScreen(
    bottomPadding: Dp,
    highlightRoom: String? = null
) {
    val context = LocalContext.current
    val allFloors = remember { loadFloors(context) }
    val scheduleData by AutoClickState.schedule.collectAsStateWithLifecycle()

    var currentTheme by remember { mutableStateOf(MapTheme.TACTICAL) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var selectedAmenityFilter by remember { mutableStateOf(AmenityFilter.ALL) }

    var selectedCampus by remember { mutableIntStateOf(0) } // 0: СПбГУТ, 1: СПбКТ
    var selectedFloor by remember { mutableIntStateOf(1) }
    var searchQuery by remember { mutableStateOf(highlightRoom ?: "") }
    
    var selectedRoom by remember { mutableStateOf<RoomShape?>(null) }
    var selectedAmenity by remember { mutableStateOf<Amenity?>(null) }

    // Точки маршрута: Откуда и Куда (По умолчанию: Откуда = Главный вход 1 этажа)
    var originFloor by remember { mutableIntStateOf(1) }
    var originRoom by remember { mutableStateOf<RoomShape?>(null) }
    var originAmenity by remember { mutableStateOf<Amenity?>(null) }
    var isOriginDefaultEntrance by remember { mutableStateOf(true) }

    var destFloor by remember { mutableIntStateOf(1) }
    var destRoom by remember { mutableStateOf<RoomShape?>(null) }
    var destAmenity by remember { mutableStateOf<Amenity?>(null) }

    var isSelectingOrigin by remember { mutableStateOf(false) }

    // Масштаб, панорамирование и угол вращения карты
    var scale by remember { mutableFloatStateOf(0.72f) }
    var offsetX by remember { mutableFloatStateOf(-300f) }
    var offsetY by remember { mutableFloatStateOf(-120f) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    val currentFloorData = allFloors[selectedFloor]

    // Настройка шрифта для номеров аудиторий (адаптируется под тему)
    val textPaint = remember(currentTheme) {
        Paint().apply {
            isAntiAlias = true
            color = if (currentTheme.isLight) android.graphics.Color.rgb(15, 23, 42) else android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
        }
    }

    // Настройка шрифта для значков туалетов (М/Ж/WC)
    val amenityTextPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
        }
    }

    // Сброс масштаба, положения и вращения
    fun resetView() {
        scale = 0.72f
        offsetX = -300f
        offsetY = -120f
        rotationAngle = 0f
    }

    // Центрирование на аудитории
    fun centerOnRoom(room: RoomShape) {
        scale = 1.6f
        offsetX = -room.x * scale + 500f
        offsetY = -room.y * scale + 800f
    }

    // Центрирование на объекте инфраструктуры
    fun centerOnAmenity(amenity: Amenity) {
        scale = 1.6f
        offsetX = -amenity.x * scale + 500f
        offsetY = -amenity.y * scale + 800f
    }

    // Инвертирование экранных координат тача в локальные координаты карты с учетом вращения
    fun screenToLocal(tapX: Float, tapY: Float, viewW: Float, viewH: Float): Offset {
        val pivotX = viewW / 2f
        val pivotY = viewH / 2f
        val p1X = tapX - offsetX - pivotX
        val p1Y = tapY - offsetY - pivotY
        val rad = Math.toRadians(-rotationAngle.toDouble())
        val cos = kotlin.math.cos(rad).toFloat()
        val sin = kotlin.math.sin(rad).toFloat()
        val p2X = p1X * cos - p1Y * sin
        val p2Y = p1X * sin + p1Y * cos
        val localX = p2X / scale + pivotX
        val localY = p2Y / scale + pivotY
        return Offset(localX, localY)
    }

    // Установка маршрута между парами из расписания ("из прошлой аудитории в следующую")
    fun routeBetweenClasses() {
        val days = scheduleData?.days ?: emptyList()
        val allLessons = days.flatMap { it.lessons }.filter { it.auditorium.isNotBlank() }

        val (firstAud, secondAud) = if (allLessons.size >= 2) {
            allLessons[0].auditorium to allLessons[1].auditorium
        } else if (allLessons.size == 1) {
            "Главный вход" to allLessons[0].auditorium
        } else {
            // Демо-пары из скриншота расписания (521/2 и 501/1)
            "521/2" to "501/1"
        }

        // Поиск аудитории отправления
        if (firstAud == "Главный вход") {
            isOriginDefaultEntrance = true
            originRoom = null
            originAmenity = null
            originFloor = 1
        } else {
            for (f in 1..7) {
                val match = allFloors[f]?.rooms?.firstOrNull { 
                    it.name.contains(firstAud, ignoreCase = true) || it.id.contains(firstAud, ignoreCase = true) 
                }
                if (match != null) {
                    originRoom = match
                    originAmenity = null
                    originFloor = f
                    isOriginDefaultEntrance = false
                    break
                }
            }
        }

        // Поиск аудитории назначения
        for (f in 1..7) {
            val match = allFloors[f]?.rooms?.firstOrNull { 
                it.name.contains(secondAud, ignoreCase = true) || it.id.contains(secondAud, ignoreCase = true) 
            }
            if (match != null) {
                destRoom = match
                destAmenity = null
                destFloor = f
                selectedRoom = match
                selectedFloor = originFloor
                break
            }
        }
    }

    // Поиск ближайшей лестницы/лифта на этаже для межэтажного маршрута
    fun findClosestStair(floor: Int, targetPoint: Offset): Amenity? {
        val stairs = allFloors[floor]?.amenities?.filter { it.type == "stairs" || it.type == "elevator" }
        return stairs?.minByOrNull {
            val dx = it.x - targetPoint.x
            val dy = it.y - targetPoint.y
            dx * dx + dy * dy
        }
    }

    // Внешний переход из расписания
    LaunchedEffect(highlightRoom) {
        if (!highlightRoom.isNullOrBlank()) {
            searchQuery = highlightRoom
            for (f in 1..7) {
                val match = allFloors[f]?.rooms?.firstOrNull { 
                    it.name.equals(highlightRoom, ignoreCase = true) ||
                    it.id.equals(highlightRoom, ignoreCase = true)
                }
                if (match != null) {
                    selectedFloor = f
                    destFloor = f
                    destRoom = match
                    destAmenity = null
                    selectedRoom = match
                    selectedAmenity = null
                    centerOnRoom(match)
                    break
                }
            }
        }
    }

    // Подсказки поиска (аудитории + инфраструктура)
    val searchSuggestions = remember(searchQuery, allFloors) {
        if (searchQuery.trim().length >= 2) {
            val q = searchQuery.trim().lowercase()
            val list = mutableListOf<Triple<Int, RoomShape?, Amenity?>>()
            for (f in 1..7) {
                allFloors[f]?.rooms?.forEach { r ->
                    if (r.name.lowercase().contains(q) || r.id.lowercase().contains(q)) {
                        list.add(Triple(f, r, null))
                    }
                }
                allFloors[f]?.amenities?.forEach { a ->
                    if (a.name.lowercase().contains(q) || a.about.lowercase().contains(q)) {
                        list.add(Triple(f, null, a))
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
            .background(currentTheme.bg)
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
                        color = currentTheme.gridDot,
                        radius = 1.4f,
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 2: Интерактивная векторная карта (GPU Canvas + Вращение)
        // ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        scale = (scale * zoom).coerceIn(0.35f, 4.5f)
                        rotationAngle = (rotationAngle + rotation) % 360f
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
                .pointerInput(rotationAngle, scale, offsetX, offsetY, selectedFloor) {
                    detectTapGestures { tapOffset ->
                        val local = screenToLocal(tapOffset.x, tapOffset.y, size.width.toFloat(), size.height.toFloat())
                        val localX = local.x
                        val localY = local.y

                        // 1. Проверяем клик по объектам инфраструктуры (туалеты, лифты, лестницы)
                        val tappedAmenity = currentFloorData?.amenities?.firstOrNull { a ->
                            val dx = localX - a.x
                            val dy = localY - a.y
                            (dx * dx + dy * dy) <= 28f * 28f
                        }

                        if (tappedAmenity != null) {
                            selectedAmenity = tappedAmenity
                            selectedRoom = null
                            if (isSelectingOrigin) {
                                originAmenity = tappedAmenity
                                originRoom = null
                                originFloor = selectedFloor
                                isOriginDefaultEntrance = false
                                isSelectingOrigin = false
                            } else {
                                destAmenity = tappedAmenity
                                destRoom = null
                                destFloor = selectedFloor
                            }
                            return@detectTapGestures
                        }

                        // 2. Проверяем клик по аудитории
                        val tappedRoom = currentFloorData?.rooms?.firstOrNull { r ->
                            localX >= r.x && localX <= r.x + r.w &&
                            localY >= r.y && localY <= r.y + r.h
                        }

                        if (tappedRoom != null) {
                            selectedAmenity = null
                            if (isSelectingOrigin) {
                                originRoom = tappedRoom
                                originAmenity = null
                                originFloor = selectedFloor
                                isOriginDefaultEntrance = false
                                isSelectingOrigin = false
                            } else {
                                selectedRoom = tappedRoom
                                destRoom = tappedRoom
                                destAmenity = null
                                destFloor = selectedFloor
                                searchQuery = tappedRoom.name
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
                        rotationZ = rotationAngle
                    }
            ) {
                if (currentFloorData != null) {
                    // 1. Внешние контуры стен и переходов
                    for (wall in currentFloorData.walls) {
                        drawPath(wall, color = currentTheme.wallFill)
                        drawPath(wall, color = currentTheme.wallStroke, style = Stroke(width = 2.5f))
                    }

                    // 2. Внутренние световые дворы (вырезы)
                    for (court in currentFloorData.courtyards) {
                        drawPath(court, color = currentTheme.courtyardFill)
                        drawPath(court, color = currentTheme.courtyardStroke, style = Stroke(width = 1.5f))
                    }

                    // 3. Стрелочка главного входа (с компенсацией поворота)
                    if (selectedFloor == 1) {
                        val arrowX = MAIN_ENTRANCE_POINT.x
                        val arrowY = MAIN_ENTRANCE_POINT.y
                        drawContext.canvas.nativeCanvas.save()
                        drawContext.canvas.nativeCanvas.rotate(-rotationAngle, arrowX, arrowY)
                        val arrowPath = Path().apply {
                            moveTo(arrowX, arrowY + 10f)
                            lineTo(arrowX - 16f, arrowY - 20f)
                            lineTo(arrowX + 16f, arrowY - 20f)
                            close()
                        }
                        drawPath(arrowPath, color = Color(0xFFFF6D00))
                        drawContext.canvas.nativeCanvas.drawText("ВХОД", arrowX, arrowY - 26f, textPaint)
                        drawContext.canvas.nativeCanvas.restore()
                    }

                    // 4. Отрисовка аудиторий с семантическими цветами
                    for (room in currentFloorData.rooms) {
                        val isSelected = selectedRoom?.id == room.id || 
                                        destRoom?.id == room.id ||
                                        (searchQuery.isNotBlank() && room.name.equals(searchQuery, ignoreCase = true))

                        val fillColor = when {
                            isSelected -> Color(0xFFFF6D00)
                            room.colorType == "green" -> Color(0xFF2E7D32)
                            room.colorType == "orange" -> Color(0xFFE65100)
                            room.colorType == "yellow" -> Color(0xFFF57F17)
                            room.colorType == "pink" -> Color(0xFFC2185B)
                            room.colorType == "cyan" -> Color(0xFF00838F)
                            else -> currentTheme.defaultRoomFill
                        }

                        val strokeColor = when {
                            isSelected -> Color(0xFFFFD54F)
                            room.colorType == "green" -> Color(0xFF81C784)
                            room.colorType == "orange" -> Color(0xFFFFB74D)
                            room.colorType == "yellow" -> Color(0xFFFFF176)
                            room.colorType == "pink" -> Color(0xFFF06292)
                            room.colorType == "cyan" -> Color(0xFF4DD0E1)
                            else -> currentTheme.defaultRoomStroke
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

                        // Номер аудитории с КОМПЕНСАЦИЕЙ ПОВОРОТА (всегда горизонтален для глаз)
                        if (scale >= 0.65f && room.name.isNotEmpty()) {
                            val cx = room.x + room.w / 2f
                            val cy = room.y + room.h / 2f
                            drawContext.canvas.nativeCanvas.save()
                            drawContext.canvas.nativeCanvas.rotate(-rotationAngle, cx, cy)
                            drawContext.canvas.nativeCanvas.drawText(room.name, cx, cy + 4.5f, textPaint)
                            drawContext.canvas.nativeCanvas.restore()
                        }
                    }

                    // 5. Отрисовка объектов инфраструктуры с КОМПЕНСАЦИЕЙ ПОВОРОТА
                    val filteredAmenities = currentFloorData.amenities.filter { a ->
                        when (selectedAmenityFilter) {
                            AmenityFilter.ALL -> true
                            AmenityFilter.RESTROOMS -> a.type.startsWith("wc")
                            AmenityFilter.STAIRS -> a.type == "stairs"
                            AmenityFilter.ELEVATORS -> a.type == "elevator"
                        }
                    }

                    for (amenity in filteredAmenities) {
                        val isAmenitySelected = selectedAmenity?.id == amenity.id || destAmenity?.id == amenity.id
                        val badgeRadius = if (isAmenitySelected) 18f else 14f

                        // Компенсируем вращение холста: иконка всегда ориентирована прямо
                        drawContext.canvas.nativeCanvas.save()
                        drawContext.canvas.nativeCanvas.rotate(-rotationAngle, amenity.x, amenity.y)

                        when (amenity.type) {
                            "elevator" -> {
                                drawCircle(
                                    color = Color(0xFF673AB7),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y)
                                )
                                drawCircle(
                                    color = if (isAmenitySelected) Color(0xFFFFD54F) else Color(0xFFD1C4E9),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y),
                                    style = Stroke(width = if (isAmenitySelected) 3.5f else 1.8f)
                                )
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(amenity.x - 6f, amenity.y - 7f),
                                    size = Size(12f, 14f),
                                    cornerRadius = CornerRadius(2f),
                                    style = Stroke(width = 1.6f)
                                )
                                drawCircle(color = Color.White, radius = 1.8f, center = Offset(amenity.x, amenity.y - 2.5f))
                                drawCircle(color = Color.White, radius = 1.8f, center = Offset(amenity.x, amenity.y + 2.5f))
                            }
                            "stairs" -> {
                                drawCircle(
                                    color = Color(0xFF00897B),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y)
                                )
                                drawCircle(
                                    color = if (isAmenitySelected) Color(0xFFFFD54F) else Color(0xFF80CBC4),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y),
                                    style = Stroke(width = if (isAmenitySelected) 3.5f else 1.8f)
                                )
                                val stairPath = Path().apply {
                                    moveTo(amenity.x - 6f, amenity.y + 6f)
                                    lineTo(amenity.x - 6f, amenity.y + 1f)
                                    lineTo(amenity.x - 1f, amenity.y + 1f)
                                    lineTo(amenity.x - 1f, amenity.y - 4f)
                                    lineTo(amenity.x + 6f, amenity.y - 4f)
                                    lineTo(amenity.x + 6f, amenity.y - 6f)
                                }
                                drawPath(stairPath, color = Color.White, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
                            }
                            "wc_woman" -> {
                                drawCircle(
                                    color = Color(0xFFD81B60),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y)
                                )
                                drawCircle(
                                    color = if (isAmenitySelected) Color(0xFFFFD54F) else Color(0xFFF48FB1),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y),
                                    style = Stroke(width = if (isAmenitySelected) 3.5f else 1.8f)
                                )
                                drawContext.canvas.nativeCanvas.drawText("Ж", amenity.x, amenity.y + 4f, amenityTextPaint)
                            }
                            "wc_man" -> {
                                drawCircle(
                                    color = Color(0xFF1976D2),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y)
                                )
                                drawCircle(
                                    color = if (isAmenitySelected) Color(0xFFFFD54F) else Color(0xFF90CAF9),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y),
                                    style = Stroke(width = if (isAmenitySelected) 3.5f else 1.8f)
                                )
                                drawContext.canvas.nativeCanvas.drawText("М", amenity.x, amenity.y + 4f, amenityTextPaint)
                            }
                            "wc_accessible" -> {
                                drawCircle(
                                    color = Color(0xFF00ACC1),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y)
                                )
                                drawCircle(
                                    color = if (isAmenitySelected) Color(0xFFFFD54F) else Color(0xFF80DEEA),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y),
                                    style = Stroke(width = if (isAmenitySelected) 3.5f else 1.8f)
                                )
                                drawContext.canvas.nativeCanvas.drawText("♿", amenity.x, amenity.y + 4f, amenityTextPaint)
                            }
                            else -> {
                                drawCircle(
                                    color = Color(0xFF5E35B1),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y)
                                )
                                drawCircle(
                                    color = if (isAmenitySelected) Color(0xFFFFD54F) else Color(0xFFB39DDB),
                                    radius = badgeRadius,
                                    center = Offset(amenity.x, amenity.y),
                                    style = Stroke(width = if (isAmenitySelected) 3.5f else 1.8f)
                                )
                                drawContext.canvas.nativeCanvas.drawText("WC", amenity.x, amenity.y + 4f, amenityTextPaint)
                            }
                        }
                        drawContext.canvas.nativeCanvas.restore()
                    }

                    // 6. Отрисовка маршрута навигатора (Одноэтажный и многоэтажный)
                    val originPoint = if (isOriginDefaultEntrance) {
                        MAIN_ENTRANCE_POINT
                    } else {
                        originRoom?.let { Offset(it.x + it.w / 2f, it.y + it.h / 2f) } 
                            ?: originAmenity?.let { Offset(it.x, it.y) }
                    }

                    val destPoint = destRoom?.let { Offset(it.x + it.w / 2f, it.y + it.h / 2f) }
                        ?: destAmenity?.let { Offset(it.x, it.y) }

                    if (originPoint != null && destPoint != null) {
                        if (originFloor == destFloor) {
                            // Одноэтажный маршрут
                            if (selectedFloor == originFloor) {
                                drawLine(
                                    color = Color(0xFFFF6D00),
                                    start = originPoint,
                                    end = destPoint,
                                    strokeWidth = 6f,
                                    cap = StrokeCap.Round
                                )
                                drawCircle(color = Color(0xFF00E676), radius = 9f, center = originPoint)
                                drawCircle(color = Color(0xFFFF6D00), radius = 9f, center = destPoint)
                            }
                        } else {
                            // Многоэтажный маршрут через лестницу/лифт
                            val stairOrigin = findClosestStair(originFloor, originPoint)
                            val stairDest = stairOrigin?.let { so ->
                                allFloors[destFloor]?.amenities?.minByOrNull { 
                                    val dx = it.x - so.x
                                    val dy = it.y - so.y
                                    dx * dx + dy * dy
                                }
                            }

                            if (selectedFloor == originFloor && stairOrigin != null) {
                                // Отрезок маршрута на этаже отправления: от старта до лестницы
                                val stairPoint = Offset(stairOrigin.x, stairOrigin.y)
                                drawLine(
                                    color = Color(0xFFFF6D00),
                                    start = originPoint,
                                    end = stairPoint,
                                    strokeWidth = 6f,
                                    cap = StrokeCap.Round
                                )
                                drawCircle(color = Color(0xFF00E676), radius = 9f, center = originPoint)
                                drawCircle(color = Color(0xFFFF9800), radius = 10f, center = stairPoint)
                            } else if (selectedFloor == destFloor && stairDest != null) {
                                // Отрезок маршрута на этаже назначения: от лестницы до цели
                                val stairPoint = Offset(stairDest.x, stairDest.y)
                                drawLine(
                                    color = Color(0xFFFF6D00),
                                    start = stairPoint,
                                    end = destPoint,
                                    strokeWidth = 6f,
                                    cap = StrokeCap.Round
                                )
                                drawCircle(color = Color(0xFFFF9800), radius = 10f, center = stairPoint)
                                drawCircle(color = Color(0xFFFF6D00), radius = 9f, center = destPoint)
                            }
                        }
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
                .background(currentTheme.surfaceColor.copy(alpha = 0.95f))
                .border(1.dp, currentTheme.strokeColor, RoundedCornerShape(18.dp))
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
                    color = if (selectedCampus == 1) Color.White else currentTheme.textSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 4: Правая вертикальная панель (Этажи, Компас, Зум, Темы)
        // ─────────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = currentTheme.surfaceColor.copy(alpha = 0.94f)
            ),
            border = BorderStroke(1.dp, currentTheme.strokeColor)
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
                            color = if (isCurrent) Color.White else currentTheme.textSecondary,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier
                        .width(22.dp)
                        .padding(vertical = 4.dp),
                    color = currentTheme.strokeColor
                )

                // Компас (Вращение карты и сброс ориентации на Север)
                IconButton(
                    onClick = { rotationAngle = 0f },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Rounded.Explore,
                        contentDescription = "Ориентация на Север",
                        tint = if (kotlin.math.abs(rotationAngle) > 2f) Color(0xFFFF6D00) else currentTheme.textSecondary,
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { rotationZ = -rotationAngle }
                    )
                }

                // Сброс масштаба в центр
                IconButton(
                    onClick = { resetView() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Rounded.CropFree,
                        contentDescription = "Центр",
                        tint = currentTheme.textPrimary,
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
                        tint = currentTheme.textPrimary,
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
                        tint = currentTheme.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Переключатель темы (Палитра)
                IconButton(
                    onClick = { showThemeMenu = !showThemeMenu },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Rounded.Palette,
                        contentDescription = "Выбор темы",
                        tint = currentTheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Всплывающее меню выбора тем оформления
        if (showThemeMenu) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 68.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceColor),
                border = BorderStroke(1.dp, currentTheme.strokeColor)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    MapTheme.entries.forEach { theme ->
                        val isSelected = currentTheme == theme
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFFF6D00).copy(alpha = 0.2f) else Color.Transparent)
                                .clickable {
                                    currentTheme = theme
                                    showThemeMenu = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = theme.icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = theme.title,
                                color = if (isSelected) Color(0xFFFF6D00) else currentTheme.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 5: Бейдж «Угол поворота» при вращении
        // ─────────────────────────────────────────────────────────────
        if (kotlin.math.abs(rotationAngle) > 2f) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(currentTheme.surfaceColor.copy(alpha = 0.9f))
                    .border(1.dp, currentTheme.strokeColor, RoundedCornerShape(14.dp))
                    .clickable { rotationAngle = 0f }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Explore,
                    contentDescription = null,
                    tint = Color(0xFFFF6D00),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${((rotationAngle % 360 + 360) % 360).toInt()}° (нажмите для сброса)",
                    color = currentTheme.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 6: Выпадающий список подсказок поиска (Autocomplete)
        // ─────────────────────────────────────────────────────────────
        if (searchSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = bottomPadding + 280.dp)
                    .heightIn(max = 240.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceColor),
                border = BorderStroke(1.dp, currentTheme.strokeColor)
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(searchSuggestions) { (floor, room, amenity) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFloor = floor
                                    if (room != null) {
                                        selectedRoom = room
                                        selectedAmenity = null
                                        destRoom = room
                                        destAmenity = null
                                        destFloor = floor
                                        searchQuery = room.name
                                        centerOnRoom(room)
                                    } else if (amenity != null) {
                                        selectedAmenity = amenity
                                        selectedRoom = null
                                        destAmenity = amenity
                                        destRoom = null
                                        destFloor = floor
                                        searchQuery = amenity.name
                                        centerOnAmenity(amenity)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (room != null) Icons.Rounded.MeetingRoom else Icons.Rounded.Navigation,
                                contentDescription = null,
                                tint = Color(0xFFFF6D00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = room?.let { "Аудитория ${it.name}" } ?: amenity?.name ?: "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = currentTheme.textPrimary
                                )
                                Text(
                                    text = if (room != null) "Корпус ${room.building} • $floor этаж" else "$floor этаж • ${amenity?.about ?: ""}",
                                    fontSize = 12.sp,
                                    color = currentTheme.textSecondary
                                )
                            }
                        }
                        HorizontalDivider(color = currentTheme.strokeColor)
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // Слой 7: Нижняя карточка навигации (Фильтры, Поиск, Маршрут)
        // ─────────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = bottomPadding),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = currentTheme.surfaceColor),
            border = BorderStroke(1.dp, currentTheme.strokeColor)
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
                        .background(currentTheme.textSecondary.copy(alpha = 0.4f))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Кнопка быстрого маршрута: "⚡ Из прошлой аудитории в следующую"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFF6D00).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFFF6D00).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { routeBetweenClasses() }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = Color(0xFFFF6D00),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Из прошлой аудитории в следующую",
                        color = Color(0xFFFF9800),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Фильтры инфраструктуры (Все, Туалеты, Лестницы, Лифты)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AmenityFilter.entries.forEach { filter ->
                        val isFilterActive = selectedAmenityFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isFilterActive) Color(0xFFFF6D00)
                                    else currentTheme.defaultRoomFill
                                )
                                .border(
                                    1.dp,
                                    if (isFilterActive) Color(0xFFFF6D00) else currentTheme.strokeColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedAmenityFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = filter.icon, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = filter.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isFilterActive) Color.White else currentTheme.textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                text = "Аудитория, туалет, лифт...",
                                fontSize = 14.sp,
                                color = currentTheme.textSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = currentTheme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    selectedRoom = null
                                    selectedAmenity = null
                                    destRoom = null
                                    destAmenity = null
                                }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Очистить",
                                        tint = currentTheme.textSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = currentTheme.defaultRoomFill,
                            unfocusedContainerColor = currentTheme.defaultRoomFill,
                            focusedBorderColor = Color(0xFFFF6D00),
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = currentTheme.textPrimary,
                            unfocusedTextColor = currentTheme.textPrimary
                        )
                    )

                    // Кнопка колокольчика
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(currentTheme.defaultRoomFill)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription = "Уведомления",
                            tint = currentTheme.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Кнопки маршрутизатора: "Откуда" / [⇆] / "Куда"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Откуда
                    val originLabel = when {
                        isOriginDefaultEntrance -> "Вход (1 эт.)"
                        originRoom != null -> "Ауд. ${originRoom?.name}"
                        originAmenity != null -> originAmenity?.name ?: "Откуда"
                        else -> "Вход (1 эт.)"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(currentTheme.defaultRoomFill)
                            .border(
                                1.dp,
                                if (isSelectingOrigin) Color(0xFF00E676) else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { isSelectingOrigin = true }
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = originLabel,
                                color = currentTheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }

                    // Кнопка быстрой смены направления местами (Swap)
                    IconButton(
                        onClick = {
                            val tempRoom = originRoom
                            val tempAmenity = originAmenity
                            val tempFloor = originFloor
                            val tempEntrance = isOriginDefaultEntrance

                            originRoom = destRoom
                            originAmenity = destAmenity
                            originFloor = destFloor
                            isOriginDefaultEntrance = false

                            destRoom = tempRoom
                            destAmenity = tempAmenity
                            destFloor = tempFloor
                            if (tempEntrance) {
                                destFloor = 1
                            }
                            selectedFloor = originFloor
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(currentTheme.defaultRoomFill)
                    ) {
                        Icon(
                            Icons.Rounded.SwapVert,
                            contentDescription = "Поменять местами",
                            tint = currentTheme.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Куда
                    val destLabel = when {
                        destRoom != null -> "Ауд. ${destRoom?.name}"
                        destAmenity != null -> destAmenity?.name ?: "Куда"
                        else -> "Куда"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(currentTheme.defaultRoomFill)
                            .clickable { }
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF6D00))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = destLabel,
                                color = if (destRoom != null || destAmenity != null) currentTheme.textPrimary else currentTheme.textSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Многоэтажный пошаговый навигатор (если начало и конец на разных этажах)
                if (destRoom != null || destAmenity != null) {
                    if (originFloor != destFloor) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Шаг 1: Этаж отправления
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedFloor == originFloor) Color(0xFFFF6D00) else currentTheme.defaultRoomFill)
                                    .clickable { selectedFloor = originFloor }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "1. Этаж $originFloor: До лестницы",
                                    color = if (selectedFloor == originFloor) Color.White else currentTheme.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Шаг 2: Переход между этажами
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(currentTheme.defaultRoomFill)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (originFloor < destFloor) "▲ Поднимитесь на $destFloor эт." else "▼ Спуститесь на $destFloor эт.",
                                    color = Color(0xFFFF9800),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Шаг 3: Этаж назначения
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedFloor == destFloor) Color(0xFFFF6D00) else currentTheme.defaultRoomFill)
                                    .clickable { selectedFloor = destFloor }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "2. Этаж $destFloor: До цели",
                                    color = if (selectedFloor == destFloor) Color.White else currentTheme.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Карточка деталей выбранной аудитории
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
                            colors = CardDefaults.cardColors(containerColor = currentTheme.defaultRoomFill),
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
                                        color = currentTheme.textPrimary
                                    )
                                    Text(
                                        text = "Корпус ${room.building} • $selectedFloor этаж",
                                        fontSize = 12.sp,
                                        color = currentTheme.textSecondary
                                    )
                                }
                                IconButton(onClick = { selectedRoom = null }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Закрыть",
                                        tint = currentTheme.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Карточка деталей выбранного объекта инфраструктуры
                AnimatedVisibility(
                    visible = selectedAmenity != null,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically { it / 2 } + fadeOut()
                ) {
                    val amenity = selectedAmenity
                    if (amenity != null) {
                        val badgeColor = when (amenity.type) {
                            "elevator" -> Color(0xFF673AB7)
                            "stairs" -> Color(0xFF00897B)
                            "wc_woman" -> Color(0xFFD81B60)
                            "wc_man" -> Color(0xFF1976D2)
                            "wc_accessible" -> Color(0xFF00ACC1)
                            else -> Color(0xFF5E35B1)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = currentTheme.defaultRoomFill),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.7f))
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
                                        .background(badgeColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val icon = when {
                                        amenity.type == "elevator" -> Icons.Rounded.Elevator
                                        amenity.type == "stairs" -> Icons.Rounded.Stairs
                                        else -> Icons.Rounded.Wc
                                    }
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = amenity.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = currentTheme.textPrimary
                                    )
                                    Text(
                                        text = if (amenity.about.isNotBlank()) amenity.about else "$selectedFloor этаж",
                                        fontSize = 12.sp,
                                        color = currentTheme.textSecondary
                                    )
                                }
                                IconButton(onClick = { selectedAmenity = null }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Закрыть",
                                        tint = currentTheme.textSecondary
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
