package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tsunami
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChronosTranslations
import com.example.model.WeatherData
import com.example.model.formatCompassDirection
import com.example.model.formatWindSpeed
import com.example.service.BeachMarineTelemetry
import com.example.service.IndonesianBeachRegion
import com.example.service.WeatherService
import com.example.ui.components.ChronosAmberNeon
import com.example.ui.components.ChronosBgDeep
import com.example.ui.components.ChronosCyanNeon
import com.example.ui.components.ChronosGreenNeon
import com.example.ui.components.ChronosPurpleNeon
import com.example.ui.components.ChronosRoseNeon
import com.example.ui.components.ChronosSurfaceDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun WeatherFullScreen(
    citiesData: List<WeatherData>,
    savedCityNames: List<String> = emptyList(),
    isLoading: Boolean,
    appLanguage: String,
    temperatureUnit: Int,
    windSpeedUnit: Int = 0,
    isNetworkOnline: Boolean = true,
    networkTransportLabel: String = "ONLINE • AKTIF",
    onBack: () -> Unit,
    onRefreshAll: () -> Unit,
    onDetectGpsLocation: ((Boolean, String) -> Unit) -> Unit = {},
    onAddCity: (String, (Boolean, String?) -> Unit) -> Unit,
    onRemoveCity: (String) -> Unit
) {
    val citiesWeatherData = citiesData
    val onRefresh = onRefreshAll
    val isId = appLanguage == "id"
    var isDetectingGps by remember { mutableStateOf(false) }
    var selectedCityName by remember { mutableStateOf<String?>(null) }
    var customBeachWeather by remember { mutableStateOf<WeatherData?>(null) }
    var isFetchingCustomBeach by remember { mutableStateOf(false) }

    val selectedCityWeather = remember(selectedCityName, citiesWeatherData, customBeachWeather) {
        if (customBeachWeather != null && selectedCityName == customBeachWeather?.locationName) {
            customBeachWeather
        } else {
            val name = selectedCityName ?: return@remember null
            val targetBase = name.split(",").firstOrNull()?.trim().orEmpty()
            citiesWeatherData.find {
                val base = it.locationName.split(",").firstOrNull()?.trim().orEmpty()
                it.locationName.equals(name, ignoreCase = true) || base.equals(targetBase, ignoreCase = true)
            }
        }
    }

    var topTab by remember { mutableIntStateOf(0) } // 0 = Kota & Peringatan Dini, 1 = Pantai, Ombak & Pasang Surut Indonesia
    var detailSubTab by remember { mutableIntStateOf(0) } // 0 = Ikhtisar & Peringatan, 1 = Angin/Suhu/Awan, 2 = Laut & Pasang Surut

    var newCityInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isDetectingGps = true
            errorMsg = null
            onDetectGpsLocation { ok, msg ->
                isDetectingGps = false
                if (ok) {
                    successMsg = msg
                    errorMsg = null
                } else {
                    errorMsg = msg
                }
            }
        } else {
            errorMsg = if (isId) {
                "Izin lokasi GPS perangkat ditolak. Aktifkan izin lokasi untuk akurasi koordinat penuh."
            } else {
                "Device GPS location permission denied. Grant location permission for high-accuracy telemetry."
            }
        }
    }

    val regions = remember { WeatherService.INDONESIAN_COASTAL_REGIONS }
    var selectedIslandFilter by remember { mutableStateOf("ALL") }
    val filteredRegions = remember(regions, selectedIslandFilter) {
        if (selectedIslandFilter == "ALL") regions
        else regions.filter { it.islandGroup.equals(selectedIslandFilter, ignoreCase = true) }
    }

    var selectedBeach by remember { mutableStateOf(regions.first()) }
    var beachTelemetry by remember(selectedBeach, isId) {
        mutableStateOf(
            WeatherService.computeBeachMarineTelemetry(
                region = selectedBeach,
                nowMillis = System.currentTimeMillis(),
                isId = isId
            )
        )
    }
    var isLoadingMarine by remember { mutableStateOf(false) }
    var scrubbedHour by remember(selectedBeach) {
        mutableIntStateOf(beachTelemetry.currentHourFloat.toInt().coerceIn(0, 23))
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedBeach, isId) {
        isLoadingMarine = true
        val fetched = WeatherService.fetchBeachMarineTelemetry(
            region = selectedBeach,
            isId = isId,
            forceRefresh = false
        )
        beachTelemetry = fetched
        scrubbedHour = fetched.currentHourFloat.toInt().coerceIn(0, 23)
        isLoadingMarine = false
    }

    val infiniteTransition = rememberInfiniteTransition(label = "weather_live_anim")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    fun formatTemp(c: Double): String {
        return if (temperatureUnit == 1) {
            "${((c * 9.0) / 5.0 + 32.0).roundToInt()}°F"
        } else {
            "${c.roundToInt()}°C"
        }
    }

    fun formatTempPrecise(c: Double): String {
        return if (temperatureUnit == 1) {
            "%.1f°F".format(Locale.US, (c * 9.0) / 5.0 + 32.0)
        } else {
            "%.1f°C".format(Locale.US, c)
        }
    }

    val primaryWeather = citiesWeatherData.firstOrNull()

    val quickIndonesianCities = remember {
        listOf(
            "Tulungagung",
            "Surabaya",
            "Yogyakarta",
            "Bandung",
            "Semarang",
            "Denpasar",
            "Medan",
            "Makassar",
            "Balikpapan",
            "Manado",
            "Labuan Bajo",
            "Jayapura"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("weather_full_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(
                        onClick = {
                            if (selectedCityWeather != null) {
                                selectedCityName = null
                                customBeachWeather = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFD1D5DB)
                        )
                    }
                    Column {
                        Text(
                            text = selectedCityWeather?.locationName
                                ?: if (isId) "Pusat Cuaca & Maritim Realtime" else "Real-Time Weather & Marine Hub",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if ((selectedCityWeather ?: primaryWeather)?.source == "live") ChronosGreenNeon
                                        else ChronosCyanNeon
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (selectedCityWeather != null) {
                                    val src = when (selectedCityWeather.source) {
                                        "live" -> if (isId) "LIVE WMO + MARITIM API" else "LIVE WMO + MARINE API"
                                        "cache" -> if (isId) "CACHE REALTIME (<10M)" else "REALTIME CACHE (<10M)"
                                        else -> if (isId) "SINOPTIK NUSANTARA" else "SYNOPTIC ENGINE"
                                    }
                                    "$src • %.2f°, %.2f°".format(
                                        Locale.US,
                                        selectedCityWeather.latitude,
                                        selectedCityWeather.longitude
                                    )
                                } else {
                                    if (isId) "PERINGATAN DINI • ANGIN & AWAN • PASANG SURUT"
                                    else "EARLY WARNING • WIND & CLOUDS • TIDES & WAVES"
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = Color(0xFF67E8F9)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        onRefresh()
                        val custom = customBeachWeather
                        if (custom != null && selectedCityName == custom.locationName) {
                            coroutineScope.launch {
                                isFetchingCustomBeach = true
                                customBeachWeather = WeatherService.fetchWeatherByCoordinates(
                                    lat = custom.latitude,
                                    lon = custom.longitude,
                                    locationName = custom.locationName,
                                    forceRefresh = true
                                )
                                isFetchingCustomBeach = false
                            }
                        }
                        coroutineScope.launch {
                            isLoadingMarine = true
                            beachTelemetry = WeatherService.fetchBeachMarineTelemetry(
                                region = selectedBeach,
                                isId = isId,
                                forceRefresh = true
                            )
                            isLoadingMarine = false
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ChronosCyanNeon.copy(alpha = 0.14f))
                        .border(1.dp, ChronosCyanNeon.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .testTag("weather_refresh_button")
                ) {
                    if (isLoading || isLoadingMarine || isFetchingCustomBeach) {
                        CircularProgressIndicator(
                            color = ChronosCyanNeon,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Weather",
                            tint = ChronosCyanNeon
                        )
                    }
                }
            }
        }

        // 2. Mode Switcher when no specific city is opened
        if (selectedCityWeather == null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ChronosSurfaceDark.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf(
                            Triple(0, if (isId) "Cuaca Kota & Peringatan" else "Cities & Early Warning", Icons.Default.WbSunny),
                            Triple(1, if (isId) "Pantai, Ombak & Pasut RI" else "Indonesian Tides & Waves", Icons.Default.Waves)
                        )
                        tabs.forEach { (idx, label, icon) ->
                            val active = topTab == idx
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (active) ChronosCyanNeon else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { topTab = idx }
                                    .testTag("weather_top_tab_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (active) ChronosBgDeep else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) ChronosBgDeep else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (topTab == 0) {
                // Live Realistic Atmospheric Hero Banner + Early Warning for Primary City
                if (primaryWeather != null) {
                    item {
                        RealisticWeatherHeroBanner(
                            weatherData = primaryWeather,
                            isId = isId,
                            animPhase = animPhase,
                            formatTemp = ::formatTemp,
                            formatTempPrecise = ::formatTempPrecise,
                            windSpeedUnit = windSpeedUnit,
                            onClick = {
                                selectedCityName = primaryWeather.locationName
                                detailSubTab = 0
                            }
                        )
                    }
                    item {
                        val alert = remember(primaryWeather, isId) {
                            WeatherService.evaluateEarlyWarning(primaryWeather, isId)
                        }
                        EarlyWarningBannerCard(
                            alert = alert,
                            locationName = primaryWeather.locationName,
                            isId = isId,
                            onInspectCity = {
                                selectedCityName = primaryWeather.locationName
                                detailSubTab = 0
                            }
                        )
                    }
                }

                // Add City / Coordinate Bar + GPS Hardware Calibration + Quick Indonesian Cities Chips
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ChronosSurfaceDark.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Online / Offline & Device GPS Accuracy Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = (if (isNetworkOnline) ChronosGreenNeon else ChronosAmberNeon).copy(alpha = 0.14f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        (if (isNetworkOnline) ChronosGreenNeon else ChronosAmberNeon).copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isNetworkOnline) ChronosGreenNeon else ChronosAmberNeon)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isNetworkOnline) "ONLINE WMO • $networkTransportLabel"
                                            else if (isId) "OFFLINE • MODE CACHE LOKAL" else "OFFLINE • LOCAL CACHE MODE",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNetworkOnline) ChronosGreenNeon else ChronosAmberNeon
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ChronosCyanNeon.copy(alpha = 0.16f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isDetectingGps) {
                                            gpsPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                        .testTag("weather_gps_calibrate_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CompassCalibration,
                                            contentDescription = null,
                                            tint = ChronosCyanNeon,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isDetectingGps) {
                                                if (isId) "Mendeteksi GPS..." else "Detecting GPS..."
                                            } else {
                                                if (isId) "📍 Akurasi GPS Perangkat" else "📍 Device GPS Sync"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChronosCyanNeon
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCityInput,
                                    onValueChange = {
                                        newCityInput = it
                                        errorMsg = null
                                        successMsg = null
                                    },
                                    placeholder = {
                                        Text(
                                            text = if (isId) "Cari kota / pantai / koordinat (-8.06, 111.90)..."
                                            else "Search city / beach / coords (-8.06, 111.90)...",
                                            fontSize = 12.sp
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = ChronosCyanNeon, modifier = Modifier.size(18.dp))
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("weather_city_input")
                                )
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ChronosCyanNeon,
                                    modifier = Modifier
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            if (newCityInput.isNotBlank()) {
                                                val query = newCityInput.trim()
                                                onAddCity(query) { ok, err ->
                                                    if (ok) {
                                                        newCityInput = ""
                                                        errorMsg = null
                                                        successMsg = if (isId) "✓ Kota '$query' berhasil ditambahkan!" else "✓ '$query' added!"
                                                    } else {
                                                        errorMsg = err
                                                    }
                                                }
                                            }
                                        }
                                        .testTag("add_weather_city_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = ChronosBgDeep, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = ChronosTranslations.t(appLanguage, "add"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChronosBgDeep
                                        )
                                    }
                                }
                            }

                            if (errorMsg != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = errorMsg!!, fontSize = 11.sp, color = ChronosRoseNeon)
                            }
                            if (successMsg != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = successMsg!!, fontSize = 11.sp, color = ChronosGreenNeon)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isId) "TAMBAH CEPAT KOTA & PESISIR INDONESIA:" else "QUICK ADD INDONESIAN CITIES & COASTS:",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9CA3AF)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(quickIndonesianCities) { quickCity ->
                                    val alreadySaved = citiesWeatherData.any {
                                        it.locationName.startsWith(quickCity, ignoreCase = true)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (alreadySaved) ChronosGreenNeon.copy(alpha = 0.12f)
                                        else Color.White.copy(alpha = 0.05f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (alreadySaved) ChronosGreenNeon.copy(alpha = 0.4f)
                                            else Color.White.copy(alpha = 0.12f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                if (alreadySaved) {
                                                    selectedCityName = quickCity
                                                    detailSubTab = 0
                                                } else {
                                                    onAddCity(quickCity) { ok, err ->
                                                        if (ok) {
                                                            successMsg = if (isId) "✓ $quickCity ditambahkan ke radar cuaca!" else "✓ $quickCity added!"
                                                            errorMsg = null
                                                        } else {
                                                            errorMsg = err
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = if (alreadySaved) "✓ $quickCity" else "+ $quickCity",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (alreadySaved) ChronosGreenNeon else Color(0xFFE5E7EB),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Saved Cities List with Real-Time Wind, Cloud, Wave & Transparency Badges
                items(citiesWeatherData, key = { it.locationName }) { city ->
                    val cityAlert = remember(city, isId) {
                        WeatherService.evaluateEarlyWarning(city, isId)
                    }
                    val alertColor = Color(cityAlert.colorHex)

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = ChronosSurfaceDark.copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (cityAlert.level >= 2) alertColor.copy(alpha = 0.45f)
                            else ChronosCyanNeon.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable {
                                selectedCityName = city.locationName
                                detailSubTab = 0
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = ChronosCyanNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = city.locationName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = alertColor.copy(alpha = 0.14f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, alertColor.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = cityAlert.badgeText,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = alertColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${ChronosTranslations.t(appLanguage, city.description)} • ${if (isId) "Terasa" else "Feels"} ${formatTemp(city.apparentTemp)}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF9CA3AF),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatTemp(city.temp),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 26.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "H:${formatTemp(city.tempMax)} L:${formatTemp(city.tempMin)}",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }
                                    if (citiesWeatherData.size > 1) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { onRemoveCity(city.locationName) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove City",
                                                tint = Color(0xFF6B7280),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4 Clean Telemetry Pills: Wind, Clouds, Humidity, Waves
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CompactTelemetryPill(
                                    icon = Icons.Default.Air,
                                    label = formatWindSpeed(city.windSpeed, windSpeedUnit),
                                    sub = formatCompassDirection(city.windDirectionDeg, isId).substringBefore(" "),
                                    tint = ChronosCyanNeon,
                                    modifier = Modifier.weight(1f)
                                )
                                CompactTelemetryPill(
                                    icon = Icons.Default.Cloud,
                                    label = "${city.cloudCover}%",
                                    sub = if (isId) "Awan" else "Clouds",
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.weight(1f)
                                )
                                CompactTelemetryPill(
                                    icon = Icons.Default.WaterDrop,
                                    label = "${city.humidity}%",
                                    sub = "DP ${city.dewPoint.roundToInt()}°",
                                    tint = ChronosGreenNeon,
                                    modifier = Modifier.weight(1f)
                                )
                                CompactTelemetryPill(
                                    icon = Icons.Default.Waves,
                                    label = "%.2fm".format(Locale.US, city.waveHeightM),
                                    sub = "${"%.0f".format(Locale.US, city.wavePeriodSec)}s Ombak",
                                    tint = ChronosAmberNeon,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB 1: PUSAT OMBAK, ANGIN LAUT & PASANG SURUT SELURUH PANTAI INDONESIA
                item {
                    IndonesianCoastalTideCenterSection(
                        regions = filteredRegions,
                        selectedIslandFilter = selectedIslandFilter,
                        onSelectIslandFilter = { selectedIslandFilter = it },
                        selectedBeach = selectedBeach,
                        telemetry = beachTelemetry,
                        isLoadingMarine = isLoadingMarine,
                        scrubbedHour = scrubbedHour,
                        onScrubHourChange = { scrubbedHour = it },
                        isId = isId,
                        windSpeedUnit = windSpeedUnit,
                        formatTemp = ::formatTemp,
                        animPhase = animPhase,
                        onSelectBeach = { selectedBeach = it },
                        onInspectFullBeachWeather = { beach ->
                            coroutineScope.launch {
                                isFetchingCustomBeach = true
                                val data = WeatherService.fetchWeatherByCoordinates(
                                    lat = beach.lat,
                                    lon = beach.lon,
                                    locationName = beach.name,
                                    forceRefresh = true
                                )
                                customBeachWeather = data
                                selectedCityName = data.locationName
                                detailSubTab = 0
                                isFetchingCustomBeach = false
                            }
                        }
                    )
                }
            }
        } else {
            // 3. CITY DETAIL VIEW (Organized into 3 Clean Sub-Tabs + Live Atmospheric Hero)
            val w = selectedCityWeather
            val cityAlert = WeatherService.evaluateEarlyWarning(w, isId)

            // Realistic Animated Atmospheric Hero Card
            item {
                RealisticWeatherHeroBanner(
                    weatherData = w,
                    isId = isId,
                    animPhase = animPhase,
                    formatTemp = ::formatTemp,
                    formatTempPrecise = ::formatTempPrecise,
                    windSpeedUnit = windSpeedUnit,
                    onClick = null
                )
            }

            // Sub-tab Selector for Detail Screen
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ChronosSurfaceDark.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val subTabs = listOf(
                            0 to (if (isId) "Ikhtisar & Stasiun" else "Overview & Station"),
                            1 to (if (isId) "Angin, Suhu & Awan" else "Wind, Temp & Clouds"),
                            2 to (if (isId) "Laut & Pasang Surut" else "Marine & Tides")
                        )
                        subTabs.forEach { (idx, title) ->
                            val active = detailSubTab == idx
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) ChronosCyanNeon else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { detailSubTab = idx }
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) ChronosBgDeep else Color(0xFFD1D5DB),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            when (detailSubTab) {
                0 -> {
                    // SUBTAB 0: EARLY WARNING, TRANSPARENCY CARD, 24H HOURLY, 7-DAY & ORBIT
                    item {
                        EarlyWarningBannerCard(
                            alert = cityAlert,
                            locationName = w.locationName,
                            isId = isId,
                            onInspectCity = null
                        )
                    }

                    // Live Synoptic Transparency & Station Accuracy Card
                    item {
                        SynopticTransparencyCard(
                            w = w,
                            isId = isId,
                            formatTempPrecise = ::formatTempPrecise,
                            onForceRefresh = onRefresh
                        )
                    }

                    // 24-Hour Forecast Horizontal Scroll (with Temp, Rain %, Wind Speed & Humidity)
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = ChronosSurfaceDark.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isId) "PRAKIRAAN 24 JAM REALTIME (SUHU, HUJAN & ANGIN)" else "REAL-TIME 24-HOUR FORECAST (TEMP, RAIN & WIND)",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosCyanNeon
                                    )
                                    Text(
                                        text = "UTC${if (w.utcOffsetSeconds >= 0) "+" else ""}${w.utcOffsetSeconds / 3600}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(w.hourly.take(24)) { h ->
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White.copy(alpha = 0.04f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .width(76.dp)
                                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = h.time,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF9CA3AF)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Icon(
                                                    imageVector = getWeatherIcon(h.code, h.isDay),
                                                    contentDescription = null,
                                                    tint = if (h.isDay) ChronosAmberNeon else ChronosCyanNeon,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = formatTemp(h.temp),
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "💧${h.precipProb}%",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (h.precipProb >= 40) ChronosCyanNeon else Color(0xFF9CA3AF)
                                                )
                                                Text(
                                                    text = "🌬️${h.windSpeed.roundToInt()}km/j",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF67E8F9).copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7-Day Forecast
                    item {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = ChronosSurfaceDark.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (isId) "PROYEKSI CUACA 7 HARI" else "7-DAY SYNOPTIC OUTLOOK",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChronosCyanNeon
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    w.daily.forEach { d ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.03f))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (d.dayName == "Today") ChronosTranslations.t(appLanguage, "today") else d.dayName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                modifier = Modifier.width(86.dp)
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = getWeatherIcon(d.code, true),
                                                    contentDescription = null,
                                                    tint = ChronosAmberNeon,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "💧${d.precipProb}%",
                                                    fontSize = 11.sp,
                                                    color = ChronosCyanNeon,
                                                    modifier = Modifier.width(48.dp)
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = formatTemp(d.tempMin),
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF9CA3AF)
                                                )
                                                Text(
                                                    text = formatTemp(d.tempMax),
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sun & Moon Horizon Orbit Tracker (Timezone-aware)
                    item {
                        SunMoonHorizonOrbitCard(
                            sunrise = w.sunrise,
                            sunset = w.sunset,
                            isDay = w.isDay,
                            utcOffsetSeconds = w.utcOffsetSeconds,
                            isId = isId
                        )
                    }
                }

                1 -> {
                    // SUBTAB 1: WIND COMPASS, TEMPERATURE PROFILE & MULTI-LAYER CLOUD ALTITUDE
                    item {
                        WindAndCloudTelemetrySection(
                            w = w,
                            isId = isId,
                            windSpeedUnit = windSpeedUnit,
                            formatTemp = ::formatTemp
                        )
                    }
                }

                2 -> {
                    // SUBTAB 2: MARINE WAVES, SEA BREEZE & INDONESIAN COASTAL TIDES
                    item {
                        CityMarineAndIndonesianTideBridge(
                            w = w,
                            regions = filteredRegions,
                            selectedIslandFilter = selectedIslandFilter,
                            onSelectIslandFilter = { selectedIslandFilter = it },
                            selectedBeach = selectedBeach,
                            beachTelemetry = beachTelemetry,
                            isLoadingMarine = isLoadingMarine,
                            scrubbedHour = scrubbedHour,
                            onScrubHourChange = { scrubbedHour = it },
                            isId = isId,
                            windSpeedUnit = windSpeedUnit,
                            formatTemp = ::formatTemp,
                            animPhase = animPhase,
                            onSelectBeach = { selectedBeach = it },
                            onInspectFullBeachWeather = { beach ->
                                coroutineScope.launch {
                                    isFetchingCustomBeach = true
                                    val data = WeatherService.fetchWeatherByCoordinates(
                                        lat = beach.lat,
                                        lon = beach.lon,
                                        locationName = beach.name,
                                        forceRefresh = true
                                    )
                                    customBeachWeather = data
                                    selectedCityName = data.locationName
                                    detailSubTab = 0
                                    isFetchingCustomBeach = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealisticWeatherHeroBanner(
    weatherData: WeatherData,
    isId: Boolean,
    animPhase: Float,
    formatTemp: (Double) -> String,
    formatTempPrecise: (Double) -> String,
    windSpeedUnit: Int,
    onClick: (() -> Unit)?
) {
    val isStorm = weatherData.code in listOf(95, 96, 99)
    val isRain = weatherData.code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) || weatherData.precipitationMm > 0.2
    val skyColors = when {
        isStorm -> listOf(Color(0xFF1A102B), Color(0xFF0E1B2E), Color(0xFF09131F))
        isRain -> listOf(Color(0xFF0D2238), Color(0xFF0A192B), Color(0xFF081220))
        weatherData.isDay -> listOf(Color(0xFF0C2D48), Color(0xFF0A2239), Color(0xFF081726))
        else -> listOf(Color(0xFF071120), Color(0xFF0B162C), Color(0xFF060C18))
    }

    val obsTimeStr = remember(weatherData.observationTimestampMs) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(weatherData.observationTimestampMs))
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isStorm) ChronosRoseNeon.copy(alpha = 0.5f) else ChronosCyanNeon.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(skyColors))
        ) {
            // Living Atmospheric Canvas (Sun/Stars, Volumetric Clouds, Rain Drops, Wind Lines, Sea Swell)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
            ) {
                val w = size.width
                val h = size.height

                // 1. Celestial Body (Sun Rays or Moon + Stars)
                if (weatherData.isDay && !isStorm) {
                    val sunCenter = Offset(w * 0.84f, h * 0.26f)
                    val pulse = 1f + 0.08f * sin(animPhase)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD54F).copy(alpha = 0.38f),
                                Color(0xFFFFB300).copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = sunCenter,
                            radius = 78.dp.toPx() * pulse
                        ),
                        radius = 78.dp.toPx() * pulse,
                        center = sunCenter
                    )
                    drawCircle(
                        color = Color(0xFFFFE082).copy(alpha = 0.85f),
                        radius = 18.dp.toPx(),
                        center = sunCenter
                    )
                } else {
                    // Twinkling Stars + Moon Glow
                    val starPositions = listOf(
                        0.12f to 0.18f, 0.28f to 0.12f, 0.46f to 0.22f,
                        0.64f to 0.14f, 0.76f to 0.28f, 0.90f to 0.16f
                    )
                    starPositions.forEachIndexed { idx, (sx, sy) ->
                        val twinkle = (0.3f + 0.6f * abs(sin(animPhase + idx * 0.9f))).coerceIn(0.15f, 0.9f)
                        drawCircle(
                            color = Color.White.copy(alpha = twinkle),
                            radius = (1.5f + (idx % 2)).dp.toPx(),
                            center = Offset(w * sx, h * sy)
                        )
                    }
                    val moonCenter = Offset(w * 0.85f, h * 0.25f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ChronosCyanNeon.copy(alpha = 0.25f), Color.Transparent),
                            center = moonCenter,
                            radius = 52.dp.toPx()
                        ),
                        radius = 52.dp.toPx(),
                        center = moonCenter
                    )
                    drawCircle(
                        color = Color(0xFFE0F7FA).copy(alpha = 0.85f),
                        radius = 14.dp.toPx(),
                        center = moonCenter
                    )
                }

                // 2. Drifting Cloud Layers scaled by cloudCover %
                val cloudCount = (2 + (weatherData.cloudCover / 22)).coerceIn(2, 6)
                val cloudAlpha = (0.06f + (weatherData.cloudCover / 100f) * 0.16f).coerceIn(0.06f, 0.24f)
                for (c in 0 until cloudCount) {
                    val drift = ((animPhase / (2f * PI.toFloat())) * w * 0.25f + c * (w / cloudCount)) % (w * 1.15f) - w * 0.08f
                    val cy = h * (0.22f + (c % 3) * 0.13f)
                    val cw = (54 + (c % 3) * 20).dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = cloudAlpha),
                        radius = cw * 0.45f,
                        center = Offset(drift, cy)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = cloudAlpha * 0.85f),
                        radius = cw * 0.35f,
                        center = Offset(drift + cw * 0.32f, cy + 4.dp.toPx())
                    )
                }

                // 3. Rain Droplets & Lightning Flash when Rain / Storm active
                if (isRain || isStorm) {
                    val dropCount = if (isStorm) 24 else 14
                    val progress = animPhase / (2f * PI.toFloat())
                    for (d in 0 until dropCount) {
                        val rx = ((d * 43f + progress * 120f) % 100f) / 100f * w
                        val ry = ((d * 29f + progress * 240f) % 100f) / 100f * h
                        drawLine(
                            color = ChronosCyanNeon.copy(alpha = 0.42f),
                            start = Offset(rx, ry),
                            end = Offset(rx - 5.dp.toPx(), ry + 14.dp.toPx()),
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    if (isStorm && sin(animPhase * 3f) > 0.92f) {
                        drawRect(color = Color.White.copy(alpha = 0.12f))
                    }
                }

                // 4. Bottom Coastal Swell Wave Ribbon
                val wavePath = Path()
                val baseWaveY = h * 0.86f
                val amp = (3f + (weatherData.waveHeightM.toFloat() * 3.5f)).coerceIn(3f, 14f).dp.toPx()
                wavePath.moveTo(0f, h)
                wavePath.lineTo(0f, baseWaveY)
                val steps = 36
                for (s in 0..steps) {
                    val x = (s.toFloat() / steps) * w
                    val y = baseWaveY + sin((s.toFloat() / steps) * 2.5f * PI.toFloat() + animPhase) * amp
                    wavePath.lineTo(x, y)
                }
                wavePath.lineTo(w, h)
                wavePath.close()
                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ChronosCyanNeon.copy(alpha = 0.16f),
                            ChronosCyanNeon.copy(alpha = 0.04f)
                        ),
                        startY = baseWaveY - amp,
                        endY = h
                    )
                )
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (weatherData.source == "live") ChronosGreenNeon.copy(alpha = 0.16f)
                        else ChronosAmberNeon.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (weatherData.source == "live") ChronosGreenNeon.copy(alpha = 0.5f)
                            else ChronosAmberNeon.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = if (weatherData.source == "live") ChronosGreenNeon else ChronosAmberNeon,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when (weatherData.source) {
                                    "live" -> if (isId) "REALTIME TERVERIFIKASI • $obsTimeStr" else "LIVE VERIFIED • $obsTimeStr"
                                    "cache" -> if (isId) "SINKRON CACHE • $obsTimeStr" else "CACHED SYNC • $obsTimeStr"
                                    else -> if (isId) "SINOPTIK NUSANTARA • $obsTimeStr" else "SYNOPTIC MODEL • $obsTimeStr"
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (weatherData.source == "live") ChronosGreenNeon else ChronosAmberNeon
                            )
                        }
                    }

                    Text(
                        text = "Elev ${weatherData.stationElevationM.roundToInt()}m • ${weatherData.pressure.roundToInt()} hPa",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = Color(0xFFD1D5DB)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = weatherData.locationName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ChronosTranslations.t(if (isId) "id" else "en", weatherData.description),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF67E8F9)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isId) {
                                "Terasa ${formatTempPrecise(weatherData.apparentTemp)} • Titik Embun ${formatTempPrecise(weatherData.dewPoint)}"
                            } else {
                                "Feels ${formatTempPrecise(weatherData.apparentTemp)} • Dew Point ${formatTempPrecise(weatherData.dewPoint)}"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFFD1D5DB)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatTemp(weatherData.temp),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 44.sp,
                            color = Color.White
                        )
                        Text(
                            text = "H:${formatTemp(weatherData.tempMax)} • L:${formatTemp(weatherData.tempMin)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Live Strip inside Hero
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🌬️ ${formatWindSpeed(weatherData.windSpeed, windSpeedUnit)} (${formatCompassDirection(weatherData.windDirectionDeg, isId).substringBefore(" ")})",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = Color.White
                    )
                    Text(
                        text = "☁️ ${weatherData.cloudCover}% • 🌊 ${"%.2f".format(Locale.US, weatherData.waveHeightM)}m",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = Color(0xFF67E8F9)
                    )
                    Text(
                        text = "☀️ ${weatherData.solarRadiationWm2.roundToInt()} W/m²",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = ChronosAmberNeon
                    )
                }
            }
        }
    }
}

@Composable
private fun SynopticTransparencyCard(
    w: WeatherData,
    isId: Boolean,
    formatTempPrecise: (Double) -> String,
    onForceRefresh: () -> Unit
) {
    val obsTime = remember(w.observationTimestampMs) {
        SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()).format(Date(w.observationTimestampMs))
    }
    val pressureTrendLabel = when {
        w.pressureDeltaHpa >= 0.8 -> if (isId) "Naik (+${"%.1f".format(Locale.US, w.pressureDeltaHpa)} hPa/3j)" else "Rising (+${"%.1f".format(Locale.US, w.pressureDeltaHpa)} hPa/3h)"
        w.pressureDeltaHpa <= -0.8 -> if (isId) "Turun (${"%.1f".format(Locale.US, w.pressureDeltaHpa)} hPa/3j)" else "Falling (${"%.1f".format(Locale.US, w.pressureDeltaHpa)} hPa/3h)"
        else -> if (isId) "Stabil (${"%.1f".format(Locale.US, w.pressureDeltaHpa)} hPa/3j)" else "Steady (${"%.1f".format(Locale.US, w.pressureDeltaHpa)} hPa/3h)"
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ChronosSurfaceDark.copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = ChronosGreenNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isId) "TRANSPARANSI & AKURASI TELEMETRI STASIUN" else "STATION TELEMETRY & TRANSPARENCY",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosGreenNeon
                        )
                        Text(
                            text = if (w.source == "live") {
                                if (isId) "Sumber: Open-Meteo WMO Synoptic + Marine API" else "Source: Open-Meteo WMO Synoptic + Marine API"
                            } else {
                                if (isId) "Sumber: Cache / Model Harmonik Sinoptik Lokal" else "Source: Local Cache / Harmonic Synoptic Model"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ChronosCyanNeon.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onForceRefresh() }
                ) {
                    Text(
                        text = if (isId) "Sinkron Ulang" else "Resync Live",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosCyanNeon,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStatBox(
                    title = if (isId) "KOORDINAT GPS" else "GPS COORDS",
                    value = "%.2f°, %.2f°".format(Locale.US, w.latitude, w.longitude),
                    sub = "Elev ${w.stationElevationM.roundToInt()}m dpl",
                    color = ChronosCyanNeon,
                    modifier = Modifier.weight(1f)
                )
                MiniStatBox(
                    title = if (isId) "TEKANAN BAROMETRIK" else "BAROMETRIC PRESSURE",
                    value = "${w.pressure.roundToInt()} hPa",
                    sub = pressureTrendLabel,
                    color = ChronosAmberNeon,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStatBox(
                    title = if (isId) "TITIK EMBUN & JARAK PANDANG" else "DEW POINT & VISIBILITY",
                    value = "${formatTempPrecise(w.dewPoint)} • ${"%.1f".format(Locale.US, w.visibility)} km",
                    sub = if (isId) "Kelembaban ${w.humidity}%" else "Humidity ${w.humidity}%",
                    color = ChronosGreenNeon,
                    modifier = Modifier.weight(1f)
                )
                MiniStatBox(
                    title = if (isId) "RADIASI SURYA & IKU" else "SOLAR RAD & AQI",
                    value = "${w.solarRadiationWm2.roundToInt()} W/m²",
                    sub = "US AQI ${w.aqi} • UV ${"%.1f".format(Locale.US, w.uvIndex)}",
                    color = ChronosPurpleNeon,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isId) "Waktu Pembaruan Observasi: $obsTime" else "Last Observation Sync: $obsTime",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
private fun EarlyWarningBannerCard(
    alert: com.example.service.EarlyWarningAlert,
    locationName: String,
    isId: Boolean,
    onInspectCity: (() -> Unit)?
) {
    val accent = Color(alert.colorHex)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ChronosSurfaceDark.copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onInspectCity != null) {
                    Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { onInspectCity() }
                } else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accent.copy(alpha = 0.16f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (alert.level >= 2) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = alert.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = locationName,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = alert.badgeText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = alert.summary,
                fontSize = 12.sp,
                color = Color(0xFFE5E7EB),
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                alert.parameters.forEach { param ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = param,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.10f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 ${alert.recommendation}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun WindAndCloudTelemetrySection(
    w: WeatherData,
    isId: Boolean,
    windSpeedUnit: Int,
    formatTemp: (Double) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Interactive Wind Vector Compass & Sea Wind Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ChronosSurfaceDark.copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isId) "RADAR ANGIN DARAT & ANGIN LAUT REALTIME" else "REAL-TIME LAND & SEA WIND TELEMETRY",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChronosCyanNeon
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Compass Vector Dial
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = r,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = ChronosCyanNeon.copy(alpha = 0.2f),
                                radius = r * 0.68f,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            for (i in 0 until 12) {
                                val a = Math.toRadians((i * 30.0) - 90.0)
                                val inner = if (i % 3 == 0) r * 0.78f else r * 0.86f
                                drawLine(
                                    color = if (i == 0) ChronosRoseNeon else Color.White.copy(alpha = 0.35f),
                                    start = Offset(
                                        center.x + (cos(a) * inner).toFloat(),
                                        center.y + (sin(a) * inner).toFloat()
                                    ),
                                    end = Offset(
                                        center.x + (cos(a) * (r * 0.96f)).toFloat(),
                                        center.y + (sin(a) * (r * 0.96f)).toFloat()
                                    ),
                                    strokeWidth = if (i % 3 == 0) 2.5.dp.toPx() else 1.2.dp.toPx()
                                )
                            }
                            val windRad = Math.toRadians(w.windDirectionDeg.toDouble() - 90.0)
                            val tip = Offset(
                                center.x + (cos(windRad) * (r * 0.72f)).toFloat(),
                                center.y + (sin(windRad) * (r * 0.72f)).toFloat()
                            )
                            val tail = Offset(
                                center.x - (cos(windRad) * (r * 0.45f)).toFloat(),
                                center.y - (sin(windRad) * (r * 0.45f)).toFloat()
                            )
                            drawLine(
                                color = ChronosCyanNeon,
                                start = tail,
                                end = tip,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = ChronosCyanNeon,
                                radius = 5.dp.toPx(),
                                center = tip
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${w.windDirectionDeg}°",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = formatCompassDirection(w.windDirectionDeg, isId).substringBefore(" "),
                                fontSize = 10.sp,
                                color = ChronosCyanNeon
                            )
                        }
                    }

                    // Wind Metrics Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WindDetailRow(
                            label = if (isId) "Kecepatan Angin" else "Sustained Wind",
                            value = formatWindSpeed(w.windSpeed, windSpeedUnit),
                            accent = ChronosCyanNeon
                        )
                        WindDetailRow(
                            label = if (isId) "Hembusan Maks (Gust)" else "Wind Gusts",
                            value = formatWindSpeed(w.windGusts, windSpeedUnit),
                            accent = ChronosAmberNeon
                        )
                        WindDetailRow(
                            label = if (isId) "Angin Laut Pesisir" else "Coastal Sea Wind",
                            value = formatWindSpeed(w.seaWindSpeed, windSpeedUnit),
                            accent = ChronosGreenNeon
                        )
                        WindDetailRow(
                            label = if (isId) "Skala Beaufort" else "Beaufort Scale",
                            value = beaufortLabel(w.windSpeed, isId),
                            accent = Color(0xFFD8B4FE)
                        )
                    }
                }
            }
        }

        // 2. Multi-Layer Cloud Cover & Altitude Profile Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ChronosSurfaceDark.copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "LAPISAN AWAN & STRATIFIKASI ATMOSFER" else "CLOUD LAYERS & ATMOSPHERIC STRATIFICATION",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosCyanNeon
                    )
                    Text(
                        text = "${if (isId) "Total" else "Total"} ${w.cloudCover}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                CloudLayerProgressRow(
                    title = if (isId) "Awan Tinggi (Cirrus • >6 km)" else "High Clouds (Cirrus • >6 km)",
                    percent = w.cloudCoverHigh,
                    color = Color(0xFF93C5FD)
                )
                Spacer(modifier = Modifier.height(8.dp))
                CloudLayerProgressRow(
                    title = if (isId) "Awan Menengah (Altocumulus • 2–6 km)" else "Mid Clouds (Altocumulus • 2–6 km)",
                    percent = w.cloudCoverMid,
                    color = ChronosCyanNeon
                )
                Spacer(modifier = Modifier.height(8.dp))
                CloudLayerProgressRow(
                    title = if (isId) "Awan Rendah (Stratus / Cumulus • <2 km)" else "Low Clouds (Stratus / Cumulus • <2 km)",
                    percent = w.cloudCoverLow,
                    color = ChronosAmberNeon
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isId) "Curah Hujan Jam Ini: ${"%.1f".format(Locale.US, w.precipitationMm)} mm"
                        else "Current Precipitation: ${"%.1f".format(Locale.US, w.precipitationMm)} mm",
                        fontSize = 11.sp,
                        color = Color(0xFFD1D5DB)
                    )
                    Text(
                        text = if (isId) "Jarak Pandang: ${"%.1f".format(Locale.US, w.visibility)} km"
                        else "Visibility: ${"%.1f".format(Locale.US, w.visibility)} km",
                        fontSize = 11.sp,
                        color = ChronosGreenNeon
                    )
                }
            }
        }

        // 3. Thermal Profile & Atmospheric Metrics Grid
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ChronosSurfaceDark.copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isId) "PROFIL TERMAL SUHU & KUALITAS UDARA" else "THERMAL TEMPERATURE & AIR QUALITY PROFILE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChronosCyanNeon
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStatBox(
                        title = if (isId) "SUHU AKTUAL" else "ACTUAL TEMP",
                        value = formatTemp(w.temp),
                        sub = "H:${formatTemp(w.tempMax)} L:${formatTemp(w.tempMin)}",
                        color = ChronosCyanNeon,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        title = if (isId) "TERASA SEPERTI" else "FEELS LIKE",
                        value = formatTemp(w.apparentTemp),
                        sub = if (isId) "Indeks Termal" else "Heat Index",
                        color = ChronosAmberNeon,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        title = if (isId) "TITIK EMBUN" else "DEW POINT",
                        value = formatTemp(w.dewPoint),
                        sub = "RH ${w.humidity}%",
                        color = ChronosGreenNeon,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStatBox(
                        title = if (isId) "SUHU PERMUKAAN LAUT" else "SEA SURFACE TEMP",
                        value = formatTemp(w.seaSurfaceTempC),
                        sub = if (isId) "Perairan Sekitar" else "Coastal Water",
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        title = if (isId) "TEKANAN UDARA" else "PRESSURE",
                        value = "${w.pressure.roundToInt()} hPa",
                        sub = if (w.pressure >= 1012) (if (isId) "Stabil Tinggi" else "High Stable") else (if (isId) "Konvektif" else "Low Active"),
                        color = Color(0xFFC084FC),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        title = if (isId) "INDEKS UV & AQI" else "UV & US AQI",
                        value = "UV ${w.uvIndex.roundToInt()} • ${w.aqi}",
                        sub = if (w.aqi <= 50) (if (isId) "Udara Bersih" else "Good Air") else (if (isId) "Sedang" else "Moderate"),
                        color = if (w.aqi <= 50) ChronosGreenNeon else ChronosAmberNeon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CityMarineAndIndonesianTideBridge(
    w: WeatherData,
    regions: List<IndonesianBeachRegion>,
    selectedIslandFilter: String,
    onSelectIslandFilter: (String) -> Unit,
    selectedBeach: IndonesianBeachRegion,
    beachTelemetry: BeachMarineTelemetry,
    isLoadingMarine: Boolean,
    scrubbedHour: Int,
    onScrubHourChange: (Int) -> Unit,
    isId: Boolean,
    windSpeedUnit: Int,
    formatTemp: (Double) -> String,
    animPhase: Float,
    onSelectBeach: (IndonesianBeachRegion) -> Unit,
    onInspectFullBeachWeather: (IndonesianBeachRegion) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // City Local Marine Summary
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ChronosSurfaceDark.copy(alpha = 0.88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isId) "TELEMETRI LAUT & OMBAK SEKITAR ${w.locationName.uppercase()}"
                    else "MARINE & WAVE TELEMETRY NEAR ${w.locationName.uppercase()}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChronosCyanNeon
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniStatBox(
                        title = if (isId) "TINGGI OMBAK" else "WAVE HEIGHT",
                        value = "%.2f m".format(Locale.US, w.waveHeightM),
                        sub = "Swell %.2f m".format(Locale.US, w.swellHeightM),
                        color = ChronosCyanNeon,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        title = if (isId) "PERIODE OMBAK" else "WAVE PERIOD",
                        value = "%.1f dtk".format(Locale.US, w.wavePeriodSec),
                        sub = if (isId) "Interval Alun" else "Swell Period",
                        color = ChronosAmberNeon,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        title = if (isId) "ANGIN LAUT" else "SEA WIND",
                        value = formatWindSpeed(w.seaWindSpeed, windSpeedUnit),
                        sub = formatTemp(w.seaSurfaceTempC),
                        color = ChronosGreenNeon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Full Indonesian Coastal Tide & Wave Center
        IndonesianCoastalTideCenterSection(
            regions = regions,
            selectedIslandFilter = selectedIslandFilter,
            onSelectIslandFilter = onSelectIslandFilter,
            selectedBeach = selectedBeach,
            telemetry = beachTelemetry,
            isLoadingMarine = isLoadingMarine,
            scrubbedHour = scrubbedHour,
            onScrubHourChange = onScrubHourChange,
            isId = isId,
            windSpeedUnit = windSpeedUnit,
            formatTemp = formatTemp,
            animPhase = animPhase,
            onSelectBeach = onSelectBeach,
            onInspectFullBeachWeather = onInspectFullBeachWeather
        )
    }
}

@Composable
private fun IndonesianCoastalTideCenterSection(
    regions: List<IndonesianBeachRegion>,
    selectedIslandFilter: String,
    onSelectIslandFilter: (String) -> Unit,
    selectedBeach: IndonesianBeachRegion,
    telemetry: BeachMarineTelemetry,
    isLoadingMarine: Boolean,
    scrubbedHour: Int,
    onScrubHourChange: (Int) -> Unit,
    isId: Boolean,
    windSpeedUnit: Int,
    formatTemp: (Double) -> String,
    animPhase: Float,
    onSelectBeach: (IndonesianBeachRegion) -> Unit,
    onInspectFullBeachWeather: (IndonesianBeachRegion) -> Unit
) {
    val safetyColor = when (telemetry.safetyLevel) {
        2 -> ChronosRoseNeon
        1 -> ChronosAmberNeon
        else -> ChronosGreenNeon
    }

    val scrubbedInfo = remember(telemetry, scrubbedHour, isId) {
        WeatherService.inspectBeachHourTelemetry(telemetry, scrubbedHour, isId)
    }

    val islandFilters = listOf(
        "ALL" to (if (isId) "Semua Pantai (16)" else "All Coasts (16)"),
        "Jawa" to "Jawa",
        "Bali & Nusa" to "Bali & Nusa Tenggara",
        "Sumatera" to "Sumatera",
        "Kalimantan" to "Kalimantan",
        "Sulawesi" to "Sulawesi",
        "Maluku & Papua" to "Maluku & Papua"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Coastal Region Selector Card (16 Indonesian Regions + Island Filter)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ChronosSurfaceDark.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isId) "PASANG SURUT, OMBAK & ANGIN LAUT NUSANTARA"
                            else "INDONESIAN ARCHIPELAGO TIDES, WAVES & SEA WIND",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosCyanNeon
                        )
                        Text(
                            text = if (isId) "16 Wilayah Pantai Utama Indonesia (WIB • WITA • WIT)"
                            else "16 Major Indonesian Coastal Regions (WIB • WITA • WIT)",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ChronosCyanNeon.copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "UTC+${selectedBeach.utcOffsetHours}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosCyanNeon,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Island Group Filter Bar
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(islandFilters) { (filterId, filterTitle) ->
                        val isFilterActive = selectedIslandFilter == filterId
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isFilterActive) ChronosAmberNeon else Color.White.copy(alpha = 0.05f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onSelectIslandFilter(filterId) }
                        ) {
                            Text(
                                text = filterTitle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFilterActive) ChronosBgDeep else Color(0xFFD1D5DB),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal scrollable chips for filtered Indonesian beaches
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(regions, key = { it.id }) { region ->
                        val selected = region.id == selectedBeach.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) ChronosCyanNeon else Color.White.copy(alpha = 0.04f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) ChronosCyanNeon else Color.White.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectBeach(region) }
                                .testTag("beach_chip_${region.id}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = region.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) ChronosBgDeep else Color.White
                                )
                                Text(
                                    text = "${region.islandGroup} • ${region.province}",
                                    fontSize = 9.sp,
                                    color = if (selected) ChronosBgDeep.copy(alpha = 0.8f) else Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Real-Time 24-Hour Tidal Curve Canvas + Interactive Hour Scrubber
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ChronosSurfaceDark.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedBeach.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${selectedBeach.province} • Tipe ${selectedBeach.tidalType}",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = telemetry.dataSourceLabel,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            color = ChronosGreenNeon
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "%.2f m".format(Locale.US, telemetry.currentTideHeightM),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = ChronosCyanNeon
                        )
                        Text(
                            text = telemetry.tideStatusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChronosGreenNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 24-Hour Harmonic Tide Curve Canvas with Both Live Marker & Scrubbed Hour Marker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF07101C))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    val curve = telemetry.hourlyTideCurve24h
                    val curHour = telemetry.currentHourFloat
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (curve.size >= 2) {
                            val minH = (curve.minOrNull() ?: 0.0) - 0.15
                            val maxH = (curve.maxOrNull() ?: 2.5) + 0.25
                            val span = max(0.5, maxH - minH)

                            // Horizontal reference grid lines
                            for (g in 1..3) {
                                val gy = size.height * (g / 4f)
                                drawLine(
                                    color = Color.White.copy(alpha = 0.06f),
                                    start = Offset(0f, gy),
                                    end = Offset(size.width, gy),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            val strokePath = Path()
                            val fillPath = Path()
                            curve.forEachIndexed { idx, hVal ->
                                val x = (idx.toFloat() / (curve.size - 1).toFloat()) * size.width
                                val normY = ((hVal - minH) / span).toFloat().coerceIn(0f, 1f)
                                val waveRipple = sin(animPhase + idx * 0.45f) * 1.8f
                                val y = size.height - (normY * (size.height * 0.82f)) - (size.height * 0.08f) + waveRipple
                                if (idx == 0) {
                                    strokePath.moveTo(x, y)
                                    fillPath.moveTo(x, size.height)
                                    fillPath.lineTo(x, y)
                                } else {
                                    strokePath.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                            }
                            fillPath.lineTo(size.width, size.height)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        ChronosCyanNeon.copy(alpha = 0.30f),
                                        ChronosCyanNeon.copy(alpha = 0.02f)
                                    )
                                )
                            )
                            drawPath(
                                path = strokePath,
                                color = ChronosCyanNeon,
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Scrubbed Hour Vertical Line & Amber Marker
                            val scrubX = (scrubbedHour.coerceIn(0, 24).toFloat() / 24f) * size.width
                            val scrubH = curve.getOrElse(scrubbedHour.coerceIn(0, 24)) { telemetry.currentTideHeightM }
                            val scrubNormY = ((scrubH - minH) / span).toFloat().coerceIn(0f, 1f)
                            val scrubY = size.height - (scrubNormY * (size.height * 0.82f)) - (size.height * 0.08f)

                            drawLine(
                                color = ChronosAmberNeon.copy(alpha = 0.55f),
                                start = Offset(scrubX, 0f),
                                end = Offset(scrubX, size.height),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawCircle(
                                color = ChronosAmberNeon.copy(alpha = 0.30f),
                                radius = 9.dp.toPx(),
                                center = Offset(scrubX, scrubY)
                            )
                            drawCircle(
                                color = ChronosAmberNeon,
                                radius = 4.5.dp.toPx(),
                                center = Offset(scrubX, scrubY)
                            )

                            // Current Local Hour Live Marker (Green Neon)
                            val curX = (curHour.coerceIn(0f, 24f) / 24f) * size.width
                            val curNormY = ((telemetry.currentTideHeightM - minH) / span).toFloat().coerceIn(0f, 1f)
                            val curY = size.height - (curNormY * (size.height * 0.82f)) - (size.height * 0.08f)

                            drawLine(
                                color = ChronosGreenNeon.copy(alpha = 0.45f),
                                start = Offset(curX, 0f),
                                end = Offset(curX, size.height),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawCircle(
                                color = ChronosGreenNeon.copy(alpha = 0.28f),
                                radius = 10.dp.toPx(),
                                center = Offset(curX, curY)
                            )
                            drawCircle(
                                color = ChronosGreenNeon,
                                radius = 4.5.dp.toPx(),
                                center = Offset(curX, curY)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { lbl ->
                            Text(
                                text = lbl,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Hour-by-Hour Scrubber Slider & Inspected Hour Telemetry Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.03f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isId) "INSPEKSI JAM PASANG SURUT: ${scrubbedInfo.timeLabel}"
                                else "INSPECT TIDE HOUR: ${scrubbedInfo.timeLabel}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosAmberNeon
                            )
                            Text(
                                text = "%.2f m • %s".format(Locale.US, scrubbedInfo.heightM, scrubbedInfo.phaseLabel),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Slider(
                            value = scrubbedHour.toFloat(),
                            onValueChange = { onScrubHourChange(it.roundToInt().coerceIn(0, 23)) },
                            valueRange = 0f..23f,
                            steps = 22,
                            colors = SliderDefaults.colors(
                                thumbColor = ChronosAmberNeon,
                                activeTrackColor = ChronosAmberNeon,
                                inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "🧭 ${scrubbedInfo.activityHint}",
                            fontSize = 11.sp,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High & Low Tide Schedule Table
                Text(
                    text = if (isId) "JADWAL PUNCAK PASANG & SURUT HARI INI:" else "TODAY'S HIGH & LOW TIDE WINDOWS:",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    telemetry.highLowEvents.forEach { ev ->
                        val isHigh = ev.type == "HIGH"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isHigh) ChronosCyanNeon.copy(alpha = 0.10f) else ChronosAmberNeon.copy(alpha = 0.10f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isHigh) ChronosCyanNeon.copy(alpha = 0.3f) else ChronosAmberNeon.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isHigh) (if (isId) "▲ PASANG" else "▲ HIGH") else (if (isId) "▼ SURUT" else "▼ LOW"),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHigh) ChronosCyanNeon else ChronosAmberNeon
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ev.timeStr,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "%.2fm".format(Locale.US, ev.heightM),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = Color(0xFFD1D5DB)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Button to inspect full synoptic weather for this beach
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ChronosCyanNeon.copy(alpha = 0.16f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onInspectFullBeachWeather(selectedBeach) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = ChronosCyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isId) "Buka Radar Cuaca Penuh ${selectedBeach.name}"
                            else "Open Full Synoptic Weather for ${selectedBeach.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosCyanNeon
                        )
                    }
                }
            }
        }

        // 3. Sea Waves, Swell, Sea Wind & Coastal Safety Advisory Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ChronosSurfaceDark.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, safetyColor.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "TELEMETRI OMBAK LAUT, ANGIN LAUT & KESELAMATAN"
                        else "OCEAN WAVES, SEA WIND & SAFETY TELEMETRY",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = safetyColor
                    )
                    if (isLoadingMarine) {
                        CircularProgressIndicator(
                            color = ChronosCyanNeon,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = telemetry.seaStateCategory,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = safetyColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniStatBox(
                        title = if (isId) "OMBAK SIGNIFIKAN" else "SIG. WAVE HEIGHT",
                        value = "%.2f m".format(Locale.US, telemetry.waveHeightM),
                        sub = "Maks %.2f m".format(Locale.US, telemetry.maxWaveHeightM),
                        color = ChronosCyanNeon,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        title = if (isId) "ALUN (SWELL)" else "OCEAN SWELL",
                        value = "%.2f m".format(Locale.US, telemetry.swellHeightM),
                        sub = "${"%.1f".format(Locale.US, telemetry.wavePeriodSec)}s periode",
                        color = ChronosAmberNeon,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        title = if (isId) "ANGIN LAUT" else "SEA BREEZE",
                        value = formatWindSpeed(telemetry.seaWindSpeedKmh, windSpeedUnit),
                        sub = formatCompassDirection(telemetry.seaWindDirDeg, isId).substringBefore(" "),
                        color = ChronosGreenNeon,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniStatBox(
                        title = if (isId) "SUHU AIR LAUT" else "SEA SURFACE TEMP",
                        value = formatTemp(telemetry.seaSurfaceTempC),
                        sub = if (isId) "Perairan Pantai" else "Coastal Water",
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        title = if (isId) "KECEPATAN ARUS" else "TIDAL CURRENT",
                        value = "${"%.1f".format(Locale.US, telemetry.currentSpeedKnots)} kt",
                        sub = if (isId) "Arus Pasut (${telemetry.tidePercentage}%)" else "Tide Flow (${telemetry.tidePercentage}%)",
                        color = Color(0xFFC084FC),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatBox(
                        title = if (isId) "RENTANG PASUT" else "TIDAL RANGE",
                        value = "${"%.2f".format(Locale.US, selectedBeach.meanRangeM)} m",
                        sub = "Maks ${"%.2f".format(Locale.US, selectedBeach.maxRangeM)} m",
                        color = ChronosAmberNeon,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = safetyColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, safetyColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = telemetry.safetyRecommendation,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactTelemetryPill(
    icon: ImageVector,
    label: String,
    sub: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = sub,
                fontSize = 9.sp,
                color = Color(0xFF9CA3AF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniStatBox(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.22f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WindDetailRow(
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF9CA3AF))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = accent
        )
    }
}

@Composable
private fun CloudLayerProgressRow(
    title: String,
    percent: Int,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, fontSize = 11.sp, color = Color(0xFFD1D5DB))
            Text(
                text = "${percent.coerceIn(0, 100)}%",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (percent.coerceIn(0, 100) / 100f) },
            color = color,
            trackColor = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

private fun beaufortLabel(kmh: Double, isId: Boolean): String = when {
    kmh < 6.0 -> if (isId) "Bft 1 • Sepoi" else "Bft 1 • Light Air"
    kmh < 19.0 -> if (isId) "Bft 2-3 • Lembut" else "Bft 2-3 • Gentle"
    kmh < 29.0 -> if (isId) "Bft 4 • Sedang" else "Bft 4 • Moderate"
    kmh < 39.0 -> if (isId) "Bft 5 • Segar" else "Bft 5 • Fresh"
    else -> if (isId) "Bft 6+ • Kuat" else "Bft 6+ • Strong"
}

@Composable
private fun SunMoonHorizonOrbitCard(
    sunrise: String,
    sunset: String,
    isDay: Boolean,
    utcOffsetSeconds: Int,
    isId: Boolean
) {
    fun parseMins(hhmm: String, fallback: Int): Int {
        val p = hhmm.split(":")
        val h = p.getOrNull(0)?.toIntOrNull() ?: return fallback
        val m = p.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    val riseMins = parseMins(sunrise, 345)
    val setMins = parseMins(sunset, 1072)
    val cityCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = System.currentTimeMillis() + (utcOffsetSeconds * 1000L)
    }
    val nowMins = cityCal.get(Calendar.HOUR_OF_DAY) * 60 + cityCal.get(Calendar.MINUTE)

    val progress = if (isDay) {
        ((nowMins - riseMins).toFloat() / max(1, setMins - riseMins).toFloat()).coerceIn(0.05f, 0.95f)
    } else {
        val nightElapsed = if (nowMins >= setMins) nowMins - setMins else (1440 - setMins) + nowMins
        val nightTotal = max(1, (1440 - setMins) + riseMins)
        (nightElapsed.toFloat() / nightTotal.toFloat()).coerceIn(0.05f, 0.95f)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ChronosSurfaceDark.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isId) "ORBIT HORIZON SURYA & BULAN" else "SOLAR & LUNAR HORIZON ORBIT",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDay) ChronosAmberNeon else ChronosCyanNeon
                )
                Text(
                    text = if (isDay) (if (isId) "Fase Siang" else "Daylight Phase") else (if (isId) "Fase Malam" else "Lunar Phase"),
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                val w = size.width
                val h = size.height
                val path = Path()
                path.moveTo(0f, h * 0.85f)
                path.quadraticTo(w * 0.5f, -h * 0.25f, w, h * 0.85f)
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.18f),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(0f, h * 0.85f),
                    end = Offset(w, h * 0.85f),
                    strokeWidth = 1.dp.toPx()
                )
                val t = progress
                val oneMinusT = 1f - t
                val bx = (oneMinusT * oneMinusT * 0f) + (2f * oneMinusT * t * (w * 0.5f)) + (t * t * w)
                val by = (oneMinusT * oneMinusT * (h * 0.85f)) + (2f * oneMinusT * t * (-h * 0.25f)) + (t * t * (h * 0.85f))
                val orbColor = if (isDay) ChronosAmberNeon else ChronosCyanNeon
                drawCircle(
                    color = orbColor.copy(alpha = 0.28f),
                    radius = 14.dp.toPx(),
                    center = Offset(bx, by)
                )
                drawCircle(
                    color = orbColor,
                    radius = 6.dp.toPx(),
                    center = Offset(bx, by)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = ChronosAmberNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${if (isId) "Terbit" else "Sunrise"} $sunrise",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFD1D5DB)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WbTwilight, contentDescription = null, tint = ChronosRoseNeon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${if (isId) "Terbenam" else "Sunset"} $sunset",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFD1D5DB)
                    )
                }
            }
        }
    }
}

private fun getWeatherIcon(code: Int, isDay: Boolean): ImageVector = when (code) {
    0, 1 -> if (isDay) Icons.Default.WbSunny else Icons.Default.NightsStay
    2, 3, 45, 48 -> Icons.Default.Cloud
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Default.WaterDrop
    71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit
    95, 96, 99 -> Icons.Default.Bolt
    else -> Icons.Default.WbSunny
}
