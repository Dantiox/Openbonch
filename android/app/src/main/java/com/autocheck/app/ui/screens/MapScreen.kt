package com.autocheck.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BuildingInfo(
    val name: String,
    val shortName: String,
    val address: String,
    val maxFloors: Int,
)

val BONCH_BUILDINGS = listOf(
    BuildingInfo("Корпус 1", "Большевиков 1", "пр. Большевиков, 22, корп. 1", 6),
    BuildingInfo("Корпус 2", "Большевиков 2", "пр. Большевиков, 22, корп. 2", 5),
    BuildingInfo("Мойка", "Мойка 61", "наб. реки Мойки, 61", 4),
    BuildingInfo("ВУЦ / Английский", "Английский 3", "Английский пр., 3", 3),
)

data class RoomItem(
    val number: String,
    val buildingIndex: Int,
    val floor: Int,
    val title: String,
    val description: String,
)

val POPULAR_ROOMS = listOf(
    RoomItem("504/1", 0, 5, "Лекционная аудитория", "Корпус 1, 5 этаж, правое крыло"),
    RoomItem("210/2", 1, 2, "Компьютерный класс", "Корпус 2, 2 этаж, центральный коридор"),
    RoomItem("332/1", 0, 3, "Лаборатория сетей связи", "Корпус 1, 3 этаж, левое крыло"),
    RoomItem("Актовый зал", 0, 1, "Главный актовый зал", "Корпус 1, 1 этаж, прямо от входа"),
    RoomItem("Столовая", 0, 1, "Студенческая столовая", "Корпус 1, 1 этаж, цокольный переход в к.2"),
    RoomItem("Деканат ИКСС", 0, 3, "Деканат факультета ИКСС", "Корпус 1, 3 этаж, каб. 341"),
)

@Composable
fun MapScreen(
    bottomPadding: Dp,
    highlightRoom: String? = null
) {
    var selectedBuildingIndex by remember { mutableIntStateOf(0) }
    var selectedFloor by remember { mutableIntStateOf(1) }
    var searchQuery by remember { mutableStateOf(highlightRoom ?: "") }

    LaunchedEffect(highlightRoom) {
        if (!highlightRoom.isNullOrBlank()) {
            searchQuery = highlightRoom
            if (highlightRoom.contains("/2")) {
                selectedBuildingIndex = 1
            } else if (highlightRoom.contains("/1")) {
                selectedBuildingIndex = 0
            }
            val roomDigits = highlightRoom.filter { it.isDigit() }
            if (roomDigits.length >= 3) {
                val floorDigit = roomDigits.first().digitToIntOrNull()
                if (floorDigit != null && floorDigit in 1..6) {
                    selectedFloor = floorDigit
                }
            }
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val currentBuilding = BONCH_BUILDINGS[selectedBuildingIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
    ) {
        // Заголовок
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Навигатор кампуса",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = currentBuilding.address,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Поиск аудитории
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Поиск: 504/1, столовая, деканат...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Выбор корпуса
        ScrollableTabRow(
            selectedTabIndex = selectedBuildingIndex,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            BONCH_BUILDINGS.forEachIndexed { index, building ->
                Tab(
                    selected = selectedBuildingIndex == index,
                    onClick = {
                        selectedBuildingIndex = index
                        selectedFloor = 1
                    },
                    text = { Text(building.shortName) }
                )
            }
        }

        // Выбор этажа
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Этаж:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            for (floor in 1..currentBuilding.maxFloors) {
                FilterChip(
                    selected = selectedFloor == floor,
                    onClick = { selectedFloor = floor },
                    label = { Text("$floor") }
                )
            }
        }

        // Схема этажа (Интерактивный холст с масштабированием пальцами)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.7f, 3.5f)
                        offset = Offset(offset.x + pan.x, offset.y + pan.y)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2 + offset.x
                val cy = size.height / 2 + offset.y

                // Схематичный контур этажа корпуса Бонча
                val width = 280.dp.toPx() * scale
                val height = 180.dp.toPx() * scale

                // Коридор
                drawRoundRect(
                    color = Color(0xFF1E88E5).copy(alpha = 0.2f),
                    topLeft = Offset(cx - width / 2, cy - height / 2),
                    size = Size(width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = Color(0xFF1E88E5),
                    topLeft = Offset(cx - width / 2, cy - height / 2),
                    size = Size(width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                    style = Stroke(width = 3f * scale)
                )

                // Аудитории по бокам (кабинеты)
                val roomWidth = 50.dp.toPx() * scale
                val roomHeight = 40.dp.toPx() * scale
                val numRooms = 4

                for (i in 0 until numRooms) {
                    val rx = cx - width / 2 + (i + 0.5f) * (width / numRooms) - roomWidth / 2
                    // Верхний ряд
                    drawRect(
                        color = Color(0xFF64B5F6).copy(alpha = 0.6f),
                        topLeft = Offset(rx, cy - height / 2 + 10.dp.toPx() * scale),
                        size = Size(roomWidth, roomHeight)
                    )
                    // Нижний ряд
                    drawRect(
                        color = Color(0xFF81C784).copy(alpha = 0.6f),
                        topLeft = Offset(rx, cy + height / 2 - roomHeight - 10.dp.toPx() * scale),
                        size = Size(roomWidth, roomHeight)
                    )
                }

                // Центральная лестница / переход
                drawCircle(
                    color = Color(0xFFFFA726),
                    radius = 16.dp.toPx() * scale,
                    center = Offset(cx, cy)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "План: Этаж $selectedFloor (${currentBuilding.shortName})",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }

        // Карточка найденной аудитории или подсказки
        val matchedRoom = POPULAR_ROOMS.firstOrNull { 
            searchQuery.isNotBlank() && (it.number.contains(searchQuery, ignoreCase = true) || it.title.contains(searchQuery, ignoreCase = true)) 
        } ?: if (searchQuery.isNotBlank()) {
            val buildingNum = if (searchQuery.contains("/2")) "2" else "1"
            val roomDigits = searchQuery.filter { it.isDigit() }
            val floor = if (roomDigits.length >= 3) "${roomDigits.first()}" else "1"
            RoomItem(
                number = searchQuery,
                buildingIndex = if (buildingNum == "2") 1 else 0,
                floor = floor.toIntOrNull() ?: 1,
                title = "Учебная аудитория",
                description = "Корпус $buildingNum, $floor этаж"
            )
        } else null

        if (matchedRoom != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Аудитория ${matchedRoom.number} — ${matchedRoom.title}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = matchedRoom.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}
