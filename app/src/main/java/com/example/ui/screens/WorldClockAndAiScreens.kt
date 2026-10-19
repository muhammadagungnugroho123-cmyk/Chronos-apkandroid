package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ALL_AVAILABLE_CITIES
import com.example.ui.components.ChronosRoseNeon
import com.example.model.CHRONOS_VERSION
import com.example.model.ChatMsg
import com.example.model.ChronosTranslations
import com.example.model.WORLD_MAP_POLYGONS
import com.example.model.WorldCity
import com.example.ui.PendingAiAction
import com.example.ui.components.ChronosAmberNeon
import com.example.ui.components.ChronosCyanNeon
import com.example.ui.components.ChronosGreenNeon
import com.example.ui.components.ChronosSurfaceDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun WorldClockFullScreen(
    nowMillis: Long,
    savedCities: List<String>,
    showWorldClockGlobe: Boolean,
    worldClockGlobeStyle: Int,
    appLanguage: String,
    is24HourFormat: Boolean,
    onUpdateSavedCities: (List<String>) -> Unit,
    onChangeGlobeStyle: (Int) -> Unit
) {
    val isId = appLanguage == "id"
    var showAddModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var manualRotation by remember { mutableFloatStateOf(0f) }
    var manualTilt by remember { mutableFloatStateOf(12f) }
    var autoRotate by remember { mutableStateOf(true) }
    var focusedCityName by remember { mutableStateOf<String?>(null) }
    var scrubberOffsetHours by remember { mutableIntStateOf(0) }

    val activeCities = remember(savedCities) {
        savedCities.mapNotNull { name ->
            ALL_AVAILABLE_CITIES.find { it.name.equals(name, ignoreCase = true) }
        }.distinctBy { it.name.lowercase(Locale.ROOT) }
    }

    fun focusCityOnGlobe(city: WorldCity) {
        focusedCityName = city.name
        autoRotate = false
        manualRotation = -city.lon.toFloat()
        manualTilt = (city.lat * 0.55).toFloat().coerceIn(-45f, 45f)
    }

    val effectiveNowMs = nowMillis + scrubberOffsetHours * 3600_000L
    val quickCuratedCities = listOf("Tulungagung", "Jakarta", "Surabaya", "Bali", "Singapore", "Tokyo", "Makkah", "London", "New York")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("world_clock_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = ChronosTranslations.t(appLanguage, "world_clock").uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp,
                        color = ChronosCyanNeon
                    )
                    Text(
                        text = "3D ORBITAL GLOBE V$CHRONOS_VERSION • ${activeCities.size} ${if (isId) "KOTA AKTIF" else "ACTIVE CITIES"}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ChronosCyanNeon.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showAddModal = true }
                        .testTag("world_clock_add_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Add City", tint = ChronosCyanNeon, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 3D Globe Visualization + Style Switcher
        if (showWorldClockGlobe) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            0 to (if (isId) "Holo Biru" else "Cyber Grid"),
                            1 to (if (isId) "Peta Benua" else "Realistic Map"),
                            2 to (if (isId) "Siang/Malam" else "Solar Day/Night"),
                            3 to (if (isId) "Emerald 3D" else "Emerald Matrix")
                        ).forEach { (id, label) ->
                            val selected = worldClockGlobeStyle == id
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) ChronosCyanNeon else Color.White.copy(alpha = 0.05f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selected) ChronosCyanNeon else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onChangeGlobeStyle(id) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else Color(0xFFD1D5DB),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (autoRotate) ChronosGreenNeon.copy(alpha = 0.2f) else ChronosAmberNeon.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (autoRotate) ChronosGreenNeon.copy(alpha = 0.4f) else ChronosAmberNeon.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    autoRotate = !autoRotate
                                    if (autoRotate) focusedCityName = null
                                }
                        ) {
                            Text(
                                text = if (autoRotate) "3D SPIN: ON" else "LOCKED",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (autoRotate) ChronosGreenNeon else ChronosAmberNeon,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OrbitalGlobe3DCanvas(
                        styleIdx = worldClockGlobeStyle,
                        autoRotate = autoRotate,
                        manualRotation = manualRotation,
                        manualTilt = manualTilt,
                        activeCities = activeCities,
                        focusedCityName = focusedCityName,
                        onDragGlobe = { dx, dy ->
                            manualRotation += dx * 0.5f
                            manualTilt = (manualTilt - dy * 0.5f).coerceIn(-60f, 60f)
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isId) "Geser bola dunia 3D atau ketuk kartu kota untuk memfokuskan lokasi"
                        else "Drag 3D globe to rotate or tap any city card below to lock focus",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }

        // Quick Curated City Selection Bar
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ChronosSurfaceDark.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isId) "PILIHAN CEPAT KOTA DUNIA & NUSANTARA:" else "QUICK CURATED WORLD CITIES:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF67E8F9)
                        )
                        Text(
                            text = "1-Tap Toggle",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickCuratedCities.forEach { cName ->
                            val isSaved = savedCities.any { it.equals(cName, ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSaved) ChronosCyanNeon.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSaved) ChronosCyanNeon.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (isSaved) {
                                            ALL_AVAILABLE_CITIES.find { it.name.equals(cName, ignoreCase = true) }
                                                ?.let { focusCityOnGlobe(it) }
                                        } else {
                                            onUpdateSavedCities(savedCities + cName)
                                        }
                                    }
                            ) {
                                Text(
                                    text = if (isSaved) "✓ $cName" else "+ $cName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSaved) Color(0xFF67E8F9) else Color(0xFF9CA3AF),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // World Time Zone Scrubber
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ChronosSurfaceDark.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isId) "Simulasi Lintas Zona Waktu:" else "World Time Zone Scrubber:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE5E7EB)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (scrubberOffsetHours == 0) "LIVE NOW"
                                else "${if (scrubberOffsetHours > 0) "+" else ""}${scrubberOffsetHours}h",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF67E8F9)
                            )
                            if (scrubberOffsetHours != 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ChronosCyanNeon,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { scrubberOffsetHours = 0 }
                                ) {
                                    Text(
                                        text = "Reset",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Slider(
                        value = scrubberOffsetHours.toFloat(),
                        onValueChange = { scrubberOffsetHours = it.roundToInt() },
                        valueRange = -12f..12f
                    )
                }
            }
        }

        // Active Cities Cards
        items(activeCities, key = { it.name }) { city ->
            val cityTz = remember(city.timeZoneId) { TimeZone.getTimeZone(city.timeZoneId) }
            val localTz = TimeZone.getDefault()
            val cityCal = remember(effectiveNowMs / 1000L, city.timeZoneId) {
                Calendar.getInstance(cityTz).apply { timeInMillis = effectiveNowMs }
            }
            val cityHour = cityCal.get(Calendar.HOUR_OF_DAY)
            val cityMin = cityCal.get(Calendar.MINUTE)
            val citySec = cityCal.get(Calendar.SECOND)

            val timeString = if (is24HourFormat) {
                "%02d:%02d".format(cityHour, cityMin)
            } else {
                val h12 = if (cityHour % 12 == 0) 12 else cityHour % 12
                val suffix = if (cityHour >= 12) "PM" else "AM"
                "%02d:%02d %s".format(h12, cityMin, suffix)
            }

            val diffMinutes = (cityTz.getOffset(effectiveNowMs) - localTz.getOffset(effectiveNowMs)) / 60_000
            val diffH = abs(diffMinutes) / 60
            val diffM = abs(diffMinutes) % 60
            val sign = if (diffMinutes >= 0) "+" else "-"
            val diffText = when {
                diffMinutes == 0 -> if (isId) "Waktu lokal" else "Local time"
                diffM == 0 -> "$sign$diffH ${if (isId) "jam" else "hrs"}"
                else -> "$sign${diffH}h ${diffM}m"
            }

            val isDaytime = cityHour in 6..17
            val isWorkHours = cityHour in 9..16
            val isFocused = focusedCityName?.equals(city.name, ignoreCase = true) == true

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isFocused) ChronosCyanNeon.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isFocused) ChronosCyanNeon else Color.White.copy(alpha = 0.1f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { focusCityOnGlobe(city) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = city.flag, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = city.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isDaytime) "☀️" else "🌙", fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = city.country, fontSize = 11.sp, color = Color(0xFF9CA3AF))
                            Text(text = "•", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                            Text(
                                text = diffText,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF67E8F9)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isWorkHours) ChronosGreenNeon.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f)
                            ) {
                                Text(
                                    text = if (isWorkHours) (if (isId) "Jam Kerja" else "Work Hrs")
                                    else if (isDaytime) (if (isId) "Siang" else "Day")
                                    else (if (isId) "Malam" else "Night"),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWorkHours) ChronosGreenNeon else Color(0xFF9CA3AF),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = timeString,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 26.sp,
                                color = ChronosCyanNeon
                            )
                            Text(
                                text = ":%02d • %.1f°, %.1f°".format(citySec, city.lat, city.lon),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                onUpdateSavedCities(savedCities.filterNot { it.equals(city.name, ignoreCase = true) })
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove City",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddModal) {
        Dialog(onDismissRequest = { showAddModal = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosSurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ChronosTranslations.t(appLanguage, "add_city"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showAddModal = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF9CA3AF))
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text(ChronosTranslations.t(appLanguage, "search_city")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filtered = ALL_AVAILABLE_CITIES.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                            it.country.contains(searchQuery, ignoreCase = true)
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.name }) { city ->
                            val alreadyAdded = savedCities.any { it.equals(city.name, ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.03f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (!alreadyAdded) {
                                            onUpdateSavedCities(savedCities + city.name)
                                        }
                                        showAddModal = false
                                        searchQuery = ""
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = city.flag, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = city.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(text = " • ${city.country}", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                                    }
                                    Text(
                                        text = if (alreadyAdded) "✓" else "+",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = ChronosCyanNeon
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

@Composable
private fun OrbitalGlobe3DCanvas(
    styleIdx: Int,
    autoRotate: Boolean,
    manualRotation: Float,
    manualTilt: Float,
    activeCities: List<WorldCity>,
    focusedCityName: String?,
    onDragGlobe: (Float, Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "globe_spin")
    val spinDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "globe_spin_deg"
    )

    val totalRotation = (if (autoRotate) spinDeg else 0f) + manualRotation
    val tiltRad = (manualTilt * PI / 180.0).toFloat()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(260.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragGlobe(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * 0.38f

            // Outer Orbital Telemetry Ring
            rotate(degrees = -16f, pivot = Offset(cx, cy)) {
                drawOval(
                    color = if (styleIdx == 3) ChronosGreenNeon.copy(alpha = 0.25f) else ChronosCyanNeon.copy(alpha = 0.25f),
                    topLeft = Offset(cx - radius * 1.24f, cy - radius * 0.36f),
                    size = androidx.compose.ui.geometry.Size(radius * 2.48f, radius * 0.72f),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )
            }

            // Atmosphere Halo Glow
            val haloColor = when (styleIdx) {
                3 -> ChronosGreenNeon.copy(alpha = 0.25f)
                2 -> ChronosAmberNeon.copy(alpha = 0.22f)
                else -> ChronosCyanNeon.copy(alpha = 0.3f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(haloColor, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius * 1.28f
                ),
                radius = radius * 1.28f,
                center = Offset(cx, cy)
            )

            // Ocean Sphere Base
            val oceanColors = when (styleIdx) {
                1 -> listOf(Color(0xFF152944), Color(0xFF0D1B2E), Color(0xFF060D17))
                2 -> listOf(Color(0xFF1E3A5F), Color(0xFF0F2038), Color(0xFF050B14))
                3 -> listOf(Color(0xFF0A2E26), Color(0xFF061C17), Color(0xFF030E0B))
                else -> listOf(Color(0xFF29B6F6), Color(0xFF0277BD), Color(0xFF013A63))
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = oceanColors,
                    center = Offset(cx - radius * 0.3f, cy - radius * 0.3f),
                    radius = radius * 1.35f
                ),
                radius = radius,
                center = Offset(cx, cy)
            )

            fun project(lat: Double, lon: Double): Triple<Float, Float, Float> {
                val latRad = lat * PI / 180.0
                val lonRad = (lon + totalRotation) * PI / 180.0
                val x1 = (cos(latRad) * sin(lonRad)).toFloat()
                val y1 = (-sin(latRad)).toFloat()
                val z1 = (cos(latRad) * cos(lonRad)).toFloat()

                val x = x1
                val y = y1 * cos(tiltRad) - z1 * sin(tiltRad)
                val z = y1 * sin(tiltRad) + z1 * cos(tiltRad)
                return Triple(cx + x * radius, cy + y * radius, z)
            }

            // Continents Polygon Rendering
            val strokeColor = when (styleIdx) {
                1 -> Color(0xFF4DD0E1)
                2 -> ChronosAmberNeon
                3 -> ChronosGreenNeon
                else -> Color.White.copy(alpha = 0.6f)
            }
            val fillColor = strokeColor.copy(alpha = 0.18f)

            for (poly in WORLD_MAP_POLYGONS) {
                val path = Path()
                var isFirst = true
                var visibleCount = 0
                var i = 0
                while (i < poly.size - 1) {
                    val lon = poly[i].toDouble()
                    val lat = poly[i + 1].toDouble()
                    val (px, py, pz) = project(lat, lon)
                    if (pz > 0.02f) {
                        visibleCount++
                        if (isFirst) {
                            path.moveTo(px, py)
                            isFirst = false
                        } else {
                            path.lineTo(px, py)
                        }
                    } else {
                        isFirst = true
                    }
                    i += 2
                }
                if (visibleCount > 2) {
                    drawPath(path = path, color = fillColor, style = Fill)
                    drawPath(path = path, color = strokeColor, style = Stroke(width = 1.3.dp.toPx()))
                }
            }

            // City Beacons
            for (city in activeCities) {
                val (px, py, pz) = project(city.lat, city.lon)
                if (pz > 0.05f) {
                    val isFocused = focusedCityName != null && focusedCityName.equals(city.name, ignoreCase = true)
                    val dotColor = if (isFocused) ChronosAmberNeon else ChronosCyanNeon
                    drawCircle(
                        color = dotColor.copy(alpha = 0.45f),
                        radius = if (isFocused) 8.dp.toPx() else 6.dp.toPx(),
                        center = Offset(px, py),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = dotColor,
                        radius = if (isFocused) 4.5.dp.toPx() else 3.5.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}

@Composable
fun AiAssistantFullScreen(
    messages: List<ChatMsg>,
    pendingAction: PendingAiAction?,
    appLanguage: String,
    isNetworkOnline: Boolean = true,
    networkTransportLabel: String = "ONLINE • AKTIF",
    onRefreshNetwork: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    onConfirmPendingAction: () -> Unit,
    onCancelPendingAction: () -> Unit
) {
    val isId = appLanguage == "id"
    var inputText by remember { mutableStateOf("") }

    val quickPrompts = remember(isId) {
        if (isId) {
            listOf(
                "Cuaca & Ombak Realtime",
                "Analisis Tidur REM",
                "Pasang alarm 06:00",
                "Timer fokus 25 menit"
            )
        } else {
            listOf(
                "Live Weather & Waves",
                "REM Sleep Analysis",
                "Set alarm 06:00",
                "Timer 25 minutes"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp)
            .testTag("ai_assistant_screen")
    ) {
        // Header with Live Online/Offline Telemetry Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ChronosCyanNeon,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "CHRONOS AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp,
                        color = ChronosCyanNeon
                    )
                    Text(
                        text = if (isId) "CLOUD GEMINI & TELEMETRI REALTIME" else "CLOUD GEMINI & LIVE TELEMETRY",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isNetworkOnline) ChronosGreenNeon.copy(alpha = 0.14f) else ChronosRoseNeon.copy(alpha = 0.16f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isNetworkOnline) ChronosGreenNeon.copy(alpha = 0.45f) else ChronosRoseNeon.copy(alpha = 0.55f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onRefreshNetwork() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isNetworkOnline) ChronosGreenNeon else ChronosRoseNeon)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = networkTransportLabel,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNetworkOnline) ChronosGreenNeon else ChronosRoseNeon
                    )
                }
            }
        }

        // Online vs Offline Architecture Banner when offline
        if (!isNetworkOnline) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ChronosRoseNeon.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosRoseNeon.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isId) "⚠️ Perangkat Offline: AI Cloud & Cuaca Satelit memerlukan internet. Fitur dasar (Alarm, Timer, Stopwatch) tetap aktif offline."
                        else "⚠️ Device Offline: Cloud AI & Satellite Weather require internet. Core features (Alarm, Timer, Stopwatch) remain active offline.",
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ChronosCyanNeon.copy(alpha = 0.2f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRefreshNetwork() }
                    ) {
                        Text(
                            text = if (isId) "Cek" else "Retry",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosCyanNeon,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Quick Prompt Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickPrompts.forEach { chip ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSendMessage(chip) }
                ) {
                    Text(
                        text = chip,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD1D5DB),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Messages Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.isUser) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    if (!m.isUser) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (m.isUser) ChronosCyanNeon.copy(alpha = 0.2f) else ChronosSurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (m.isUser) ChronosCyanNeon.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth(0.82f)
                    ) {
                        Text(
                            text = m.text,
                            fontSize = 12.sp,
                            color = Color.White,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    if (m.isUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = ChronosCyanNeon.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = ChronosCyanNeon, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }

        // Pending Action Confirmation Card
        if (pendingAction != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ChronosSurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = pendingAction.summary,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onConfirmPendingAction,
                            colors = ButtonDefaults.buttonColors(containerColor = ChronosCyanNeon),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isId) "Konfirmasi" else "Confirm",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        TextButton(onClick = onCancelPendingAction) {
                            Text(
                                text = if (isId) "Batal" else "Cancel",
                                fontSize = 12.sp,
                                color = Color(0xFFD1D5DB)
                            )
                        }
                    }
                }
            }
        }

        // Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = ChronosTranslations.t(appLanguage, "ask_schedule"),
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input")
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ChronosCyanNeon,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }
                    .testTag("ai_send_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
