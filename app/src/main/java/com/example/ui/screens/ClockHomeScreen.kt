package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AgendaEvent
import com.example.model.AlarmItem
import com.example.model.AppSettings
import com.example.model.CalendarEvent
import com.example.model.ChronosTranslations
import com.example.model.LifeLog
import com.example.model.RecurrenceType
import com.example.model.ReminderItem
import com.example.model.TaskItem
import com.example.model.WeatherData
import com.example.model.formatWindSpeed
import com.example.ui.components.ChronosAmberNeon
import com.example.ui.components.ChronosBgDeep
import com.example.ui.components.ChronosCyanNeon
import com.example.ui.components.ChronosGreenNeon
import com.example.ui.components.ChronosPurpleNeon
import com.example.ui.components.ChronosRoseNeon
import com.example.ui.components.ClockFace
import com.example.ui.components.getThemeAccentColor
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ClockHomeScreen(
    nowMillis: Long,
    settings: AppSettings = AppSettings(),
    alarms: List<AlarmItem> = emptyList(),
    clockStyleIndex: Int = settings.clockStyleIndex,
    is24HourFormat: Boolean = settings.is24HourFormat,
    themeColorIndex: Int = settings.themeColorIndex,
    appLanguage: String = settings.appLanguage,
    temperatureUnit: Int = settings.temperatureUnit,
    weatherData: WeatherData?,
    isLoadingWeather: Boolean,
    selectedLifePillar: String,
    lifeLog: LifeLog,
    isLifeNotified: Boolean,
    tasks: List<TaskItem>,
    agendaEvents: List<AgendaEvent>,
    calendarEvents: List<CalendarEvent>,
    onCycleClockStyle: () -> Unit,
    onRefreshWeather: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToProductivity: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToTimers: () -> Unit,
    onSelectLifePillar: (String) -> Unit,
    onUpdateLifeLog: ((LifeLog) -> LifeLog) -> Unit,
    onToggleLifeNotify: () -> Unit,
    onAddHydrationReminder: (ReminderItem) -> Unit,
    onStart20MinTimer: () -> Unit,
    onAddCalendarEvent: (String, String, String, String) -> Unit,
    onRemoveCalendarEvent: (String) -> Unit
) {
    val accentColor = getThemeAccentColor(themeColorIndex)

    val dateString = remember(nowMillis / 60_000L, appLanguage) {
        val locale = when (appLanguage) {
            "id" -> Locale.forLanguageTag("id-ID")
            "zh" -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.US
        }
        SimpleDateFormat("EEE, dd MMM", locale).format(Date(nowMillis)).uppercase(locale)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("clock_home_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Main Clock Face (5 Interactive Styles + Smooth Sweep / Seconds Toggle)
        item {
            Box(
                modifier = Modifier.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                ClockFace(
                    styleIndex = clockStyleIndex,
                    nowMillis = nowMillis,
                    is24Hour = is24HourFormat,
                    accentColor = accentColor,
                    showSeconds = settings.showClockSeconds,
                    smoothSweep = settings.smoothSweepSeconds && !settings.ecoPerformanceMode,
                    onCycleStyle = onCycleClockStyle
                )
            }
        }

        // 2. Frosted Glass Container Card holding Date, Briefing, Holiday, Weather, Life Suite & Calendar
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.02f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Ticking Date
                    Text(
                        text = dateString,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 3.5.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    // National & International Holiday Banner
                    if (settings.calendarShowHolidays) {
                        NationalHolidayBanner(
                            nowMillis = nowMillis,
                            appLanguage = appLanguage,
                            accentColor = accentColor
                        )
                    }

                    // Smart Daily Briefing Banner
                    if (settings.showSmartDailyBriefing) {
                        SmartDailyBriefingCard(
                            nowMillis = nowMillis,
                            appLanguage = appLanguage,
                            weatherData = weatherData,
                            temperatureUnit = temperatureUnit,
                            activeAlarmsCount = alarms.count { it.isEnabled },
                            pendingTasksCount = tasks.count { !it.isCompleted },
                            waterGlasses = lifeLog.water,
                            hydrationGoal = settings.dailyWaterGoal,
                            accentColor = accentColor,
                            onOpenProductivity = onNavigateToProductivity,
                            onOpenAlarms = onNavigateToAlarms
                        )
                    }

                    // Live Weather Card
                    if (settings.showHomeWeatherCard) {
                        LiveWeatherCard(
                            weatherData = weatherData,
                            isLoading = isLoadingWeather,
                            onRefresh = onRefreshWeather,
                            onNavigateToWeather = onNavigateToWeather,
                            temperatureUnit = temperatureUnit,
                            windSpeedUnit = settings.windSpeedUnit,
                            weatherAlertNotification = settings.weatherAlertsEnabled,
                            appLanguage = appLanguage
                        )
                    }

                    // CHRONOS Life Suite Card (Health, Food, Activity, Productivity)
                    if (settings.showHomeLifeSuite) {
                        HealthFoodActivityCard(
                            appLanguage = appLanguage,
                            temperatureC = weatherData?.temp,
                            hydrationDailyGoal = settings.dailyWaterGoal,
                            selectedPillarId = selectedLifePillar,
                            lifeLog = lifeLog,
                            openTasksCount = tasks.count { !it.isCompleted },
                            isNotified = isLifeNotified,
                            onSelectPillar = onSelectLifePillar,
                            onUpdateLife = onUpdateLifeLog,
                            onToggleNotify = onToggleLifeNotify,
                            onAddHydrationReminder = onAddHydrationReminder,
                            onStart20MinTimer = {
                                onStart20MinTimer()
                                onNavigateToTimers()
                            },
                            onOpenProductivity = onNavigateToProductivity,
                            onOpenAlarms = onNavigateToAlarms
                        )
                    }

                    // Calendar Sync Card
                    if (settings.showHomeCalendarCard) {
                        CalendarEventsCard(
                            appLanguage = appLanguage,
                            calendarEvents = calendarEvents,
                            agendaEvents = agendaEvents,
                            onAddEvent = onAddCalendarEvent,
                            onRemoveEvent = onRemoveCalendarEvent,
                            onOpenProductivity = onNavigateToProductivity
                        )
                    }

                    // Weather Notification Advisory
                    if (settings.weatherAlertsEnabled) {
                        WeatherNotificationBanner(
                            weatherData = weatherData,
                            appLanguage = appLanguage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NationalHolidayBanner(
    nowMillis: Long,
    appLanguage: String,
    accentColor: Color
) {
    val isId = appLanguage == "id"
    val cal = remember(nowMillis / 3_600_000L) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
    val curMonth = cal.get(Calendar.MONTH) + 1
    val curDay = cal.get(Calendar.DAY_OF_MONTH)

    data class HolidayEntry(val month: Int, val day: Int, val nameId: String, val nameEn: String)
    val holidays = remember {
        listOf(
            HolidayEntry(1, 1, "Tahun Baru Masehi", "New Year's Day"),
            HolidayEntry(2, 17, "Tahun Baru Imlek", "Lunar New Year"),
            HolidayEntry(3, 20, "Hari Raya Idul Fitri", "Eid al-Fitr"),
            HolidayEntry(4, 18, "Wafat Isa Almasih", "Good Friday"),
            HolidayEntry(5, 1, "Hari Buruh Internasional", "International Labor Day"),
            HolidayEntry(5, 14, "Kenaikan Isa Almasih", "Ascension Day"),
            HolidayEntry(6, 1, "Hari Lahir Pancasila", "Pancasila Day"),
            HolidayEntry(8, 17, "Hari Kemerdekaan RI", "Indonesia Independence Day"),
            HolidayEntry(10, 28, "Hari Sumpah Pemuda", "Youth Pledge Day"),
            HolidayEntry(11, 10, "Hari Pahlawan Nasional", "National Heroes Day"),
            HolidayEntry(12, 25, "Hari Raya Natal", "Christmas Day")
        )
    }

    val upcoming = remember(curMonth, curDay) {
        holidays.firstOrNull { it.month > curMonth || (it.month == curMonth && it.day >= curDay) }
            ?: holidays.first()
    }
    val isToday = upcoming.month == curMonth && upcoming.day == curDay

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isToday) {
                        if (isId) "HARI BESAR HARI INI: ${upcoming.nameId}" else "HOLIDAY TODAY: ${upcoming.nameEn}"
                    } else {
                        if (isId) "Hari Besar Berikutnya: ${upcoming.nameId}" else "Next Holiday: ${upcoming.nameEn}"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "%02d/%02d".format(upcoming.day, upcoming.month),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun SmartDailyBriefingCard(
    nowMillis: Long,
    appLanguage: String,
    weatherData: WeatherData?,
    temperatureUnit: Int,
    activeAlarmsCount: Int,
    pendingTasksCount: Int,
    waterGlasses: Int,
    hydrationGoal: Int,
    accentColor: Color,
    onOpenProductivity: () -> Unit,
    onOpenAlarms: () -> Unit
) {
    val isId = appLanguage == "id"
    val hour = remember(nowMillis / 60_000L) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    }

    val (greeting, circadianPhase, readinessScore) = remember(hour, isId) {
        when (hour) {
            in 5..10 -> Triple(
                if (isId) "Ringkasan Pagi Cerdas" else "Smart Morning Briefing",
                if (isId) "Puncak Fokus Kognitif (Cortisol Awakening)" else "Peak Cognitive Focus Window",
                94
            )
            in 11..14 -> Triple(
                if (isId) "Ringkasan Siang Cerdas" else "Smart Midday Briefing",
                if (isId) "Fase Eksekusi & Kolaborasi" else "High Execution & Collaboration Phase",
                86
            )
            in 15..18 -> Triple(
                if (isId) "Ringkasan Sore Cerdas" else "Smart Afternoon Briefing",
                if (isId) "Puncak Koordinasi Fisik & Olahraga" else "Peak Physical Coordination & Cardio",
                89
            )
            else -> Triple(
                if (isId) "Ringkasan Malam & Pemulihan" else "Evening Recovery Briefing",
                if (isId) "Transisi Melatonin & Restorasi REM" else "Melatonin Wind-Down & REM Prep",
                91
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0E1729).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ChronosAmberNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = greeting.uppercase(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosAmberNeon
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ChronosGreenNeon.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "BIO-SCORE $readinessScore%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosGreenNeon,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = circadianPhase,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenAlarms() }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$activeAlarmsCount",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosCyanNeon
                        )
                        Text(
                            text = if (isId) "Alarm Aktif" else "Alarms",
                            fontSize = 9.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenProductivity() }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$pendingTasksCount",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosAmberNeon
                        )
                        Text(
                            text = if (isId) "Tugas Aktif" else "Open Tasks",
                            fontSize = 9.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$waterGlasses/$hydrationGoal",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosGreenNeon
                        )
                        Text(
                            text = if (isId) "Hidrasi" else "Hydration",
                            fontSize = 9.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveWeatherCard(
    weatherData: WeatherData?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onNavigateToWeather: () -> Unit,
    temperatureUnit: Int,
    windSpeedUnit: Int = 0,
    weatherAlertNotification: Boolean = true,
    appLanguage: String
) {
    val isId = appLanguage == "id"
    fun formatTemp(c: Double): String {
        return if (temperatureUnit == 1) "${((c * 9.0) / 5.0 + 32.0).roundToInt()}°F" else "${c.roundToInt()}°C"
    }
    fun formatTempNum(c: Double): String {
        return if (temperatureUnit == 1) "${((c * 9.0) / 5.0 + 32.0).roundToInt()}" else "${c.roundToInt()}"
    }

    val isSevere = weatherAlertNotification && weatherData != null && weatherData.code in listOf(95, 96, 99, 61, 63, 65, 71, 73, 75, 77, 85, 86)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onNavigateToWeather() }
            .testTag("live_weather_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (weatherData?.source == "live") ChronosGreenNeon else ChronosCyanNeon)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (weatherData?.source == "live") {
                            if (isId) "CUACA REALTIME • LIVE API" else "REAL-TIME WEATHER • LIVE API"
                        } else {
                            "LIVE WEATHER"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (weatherData?.source == "live") ChronosGreenNeon else ChronosCyanNeon
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (weatherData != null) {
                        val obsHhMm = remember(weatherData.observationTimestampMs) {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(weatherData.observationTimestampMs))
                        }
                        Text(
                            text = "${weatherData.locationName.split(",").firstOrNull().orEmpty().uppercase()} • $obsHhMm",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Weather",
                            tint = if (isLoading) ChronosCyanNeon else Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = ChronosCyanNeon.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (weatherData != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Weather Visual Orb
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(60.dp)
                    ) {
                        if (weatherData.code == 0 || weatherData.code == 1) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (weatherData.isDay) Color(0xFFFBBF24) else Color(0xFFCBD5E1)
                                    )
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = ChronosCyanNeon,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatTempNum(weatherData.temp),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Light,
                                fontSize = 36.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (temperatureUnit == 1) "°F" else "°C",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ChronosCyanNeon,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = "${ChronosTranslations.t(appLanguage, weatherData.description)} • ${ChronosTranslations.t(appLanguage, weatherData.timeOfDayStr)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD1D5DB)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = ChronosCyanNeon,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${weatherData.humidity}%",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Air,
                                    contentDescription = null,
                                    tint = ChronosCyanNeon,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = formatWindSpeed(weatherData.windSpeed, windSpeedUnit),
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            }
                            Text(
                                text = "☁️ ${weatherData.cloudCover}%",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "🌊 ${"%.2f".format(weatherData.waveHeightM)}m",
                                fontSize = 11.sp,
                                color = ChronosCyanNeon
                            )
                        }
                    }
                }

                if (isSevere) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ChronosAmberNeon.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ChronosAmberNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "Peringatan Dini: Potensi Cuaca Ekstrem" else "Early Warning: Severe Weather Risk",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosAmberNeon
                            )
                        }
                    }
                }

                if (weatherData.hourly.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isId) "PRAKIRAAN MENDATANG" else "UPCOMING HOURS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        weatherData.hourly.take(4).forEach { h ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.02f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = h.time, fontSize = 11.sp, color = Color(0xFF9CA3AF))
                                    Text(
                                        text = formatTemp(h.temp),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    Text(
                                        text = if (h.precipProb > 0) "💧${h.precipProb}%" else "—",
                                        fontSize = 10.sp,
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

private data class PillarSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val highlights: List<String>
)

@Composable
private fun HealthFoodActivityCard(
    appLanguage: String,
    temperatureC: Double?,
    hydrationDailyGoal: Int = 8,
    selectedPillarId: String,
    lifeLog: LifeLog,
    openTasksCount: Int,
    isNotified: Boolean,
    onSelectPillar: (String) -> Unit,
    onUpdateLife: ((LifeLog) -> LifeLog) -> Unit,
    onToggleNotify: () -> Unit,
    onAddHydrationReminder: (ReminderItem) -> Unit,
    onStart20MinTimer: () -> Unit,
    onOpenProductivity: () -> Unit,
    onOpenAlarms: () -> Unit
) {
    val isId = appLanguage == "id"
    var showDetailDialog by remember { mutableStateOf(false) }
    var hydrationFeedback by remember { mutableStateOf<String?>(null) }

    val weatherWaterBonus = when {
        temperatureC == null || temperatureC < 28.0 -> 0
        temperatureC < 32.0 -> 1
        else -> 2
    }
    val waterTarget = max(4, hydrationDailyGoal + weatherWaterBonus)

    val pillars = remember(isId) {
        mapOf(
            "PRODUCTIVITY" to PillarSpec(
                id = "PRODUCTIVITY",
                title = if (isId) "Produktivitas" else "Productivity",
                subtitle = if (isId) "Fokus & Ritme Produktif" else "Deep Focus & Peak Flow",
                description = if (isId) "Sinkronisasi sesi fokus Pomodoro terpadu, pelacakan waktu puncak kerja harian, dan pengingat jeda istirahat cerdas ergonomis."
                else "Sync Pomodoro deep work sessions, track daily peak productive hours, and receive intelligent ergonomic break prompts.",
                icon = Icons.Default.Psychology,
                accentColor = ChronosAmberNeon,
                highlights = if (isId) listOf("Sesi Pomodoro Terpadu", "Waktu Fokus Puncak", "Jeda Ergonomis Cerdas")
                else listOf("Deep Work Flow", "Peak Hours Tracker", "Smart Break Prompts")
            ),
            "HEALTH" to PillarSpec(
                id = "HEALTH",
                title = if (isId) "Kesehatan" else "Health",
                subtitle = if (isId) "Ritme Sirkadian & Pemulihan" else "Circadian Rhythm & Recovery",
                description = if (isId) "Sinkronisasi ritme biologis tubuh dengan waktu istirahat, detak nadi istirahat, dan skor pemulihan harian."
                else "Sync your biological clock with smart rest cycles, resting heart-rate trends, and daily recovery scores.",
                icon = Icons.Default.Favorite,
                accentColor = ChronosRoseNeon,
                highlights = if (isId) listOf("Ritme Sirkadian", "Kualitas Tidur Cerdas", "Pemulihan Biometrik")
                else listOf("Circadian Sync", "Smart Sleep Cycles", "Biometric Recovery")
            ),
            "FOOD" to PillarSpec(
                id = "FOOD",
                title = if (isId) "Nutrisi" else "Food",
                subtitle = if (isId) "Pola Makan & Hidrasi Dinamis" else "Smart Meals & Hydration",
                description = if (isId) "Pengingat jendela makan berkala (intermittent fasting) dan rekomendasi asupan air minum adaptif sesuai suhu cuaca."
                else "Timed meal window reminders (intermittent fasting) and temperature-adapted hydration targets.",
                icon = Icons.Default.Restaurant,
                accentColor = Color(0xFF69F0AE),
                highlights = if (isId) listOf("Jendela Waktu Makan", "Target Hidrasi Cuaca", "Pola Makan Seimbang")
                else listOf("Smart Meal Windows", "Weather Hydration Target", "Balanced Nutrition")
            ),
            "ACTIVITY" to PillarSpec(
                id = "ACTIVITY",
                title = if (isId) "Aktivitas" else "Activity",
                subtitle = if (isId) "Kebugaran & Gerak Adaptif" else "Adaptive Fitness & Movement",
                description = if (isId) "Rekomendasi jam terbaik berolahraga luar ruangan berdasarkan indeks UV & suhu, serta pengatur waktu interval terintegrasi."
                else "Weather-optimized workout windows based on UV and temperature, plus built-in interval training timers.",
                icon = Icons.Default.FitnessCenter,
                accentColor = ChronosCyanNeon,
                highlights = if (isId) listOf("Jendela Waktu Olahraga", "Integrasi Timer Interval", "Target Gerak Harian")
                else listOf("Golden Workout Window", "Interval Timers Sync", "Adaptive Step Goals")
            )
        )
    }

    val currentPillar = pillars[selectedPillarId] ?: pillars.getValue("PRODUCTIVITY")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFBBF24), Color(0xFFFACC15), Color(0xFFF59E0B))
                )
            )
            .padding(1.5.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(19.dp),
            color = Color(0xFF101724).copy(alpha = 0.96f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ChronosAmberNeon.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.6f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ChronosAmberNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CHRONOS Life Suite",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isId) "Produktivitas • Kesehatan • Nutrisi • Gerak"
                                else "Productivity • Health • Food • Activity",
                                fontSize = 11.sp,
                                color = ChronosAmberNeon.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ChronosGreenNeon.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.8f))
                    ) {
                        Text(
                            text = "LIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = ChronosGreenNeon,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4 Pillars Nav
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pillars.values.forEach { p ->
                        val isSelected = p.id == selectedPillarId
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = if (isSelected) 0.1f else 0.03f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) p.accentColor else Color.White.copy(alpha = 0.06f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectPillar(p.id) }
                                .testTag("life_pillar_${p.id.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = p.accentColor.copy(alpha = 0.16f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = p.icon,
                                            contentDescription = p.title,
                                            tint = p.accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = p.title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pillar Details Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showDetailDialog = true }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentPillar.subtitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentPillar.accentColor
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isId) "Pratinjau" else "Preview",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentPillar.description,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            currentPillar.highlights.forEach { h ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = currentPillar.accentColor.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, currentPillar.accentColor.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = h,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = currentPillar.accentColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Controls per Pillar
                when (selectedPillarId) {
                    "PRODUCTIVITY" -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isId) "Tugas terbuka hari ini" else "Open tasks today",
                                    fontSize = 12.sp,
                                    color = Color(0xFFD1D5DB)
                                )
                                Text(
                                    text = openTasksCount.toString(),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ChronosAmberNeon
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onOpenProductivity,
                            colors = ButtonDefaults.buttonColors(containerColor = ChronosCyanNeon),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = ChronosBgDeep,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isId) "Buka Dashboard Produktivitas" else "Open Productivity Dashboard",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosBgDeep
                            )
                        }
                    }
                    "HEALTH" -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isId) "Tidur semalam" else "Last night's sleep",
                                        fontSize = 12.sp,
                                        color = Color(0xFFD1D5DB)
                                    )
                                    Text(
                                        text = "${lifeLog.sleepHours}h",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ChronosRoseNeon
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.06f),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onUpdateLife { it.copy(sleepHours = (it.sleepHours - 1).coerceAtLeast(0)) }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { (lifeLog.sleepHours / 9f).coerceIn(0f, 1f) },
                                        color = ChronosRoseNeon,
                                        trackColor = Color.White.copy(alpha = 0.1f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.06f),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onUpdateLife { it.copy(sleepHours = (it.sleepHours + 1).coerceAtMost(12)) }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onOpenAlarms,
                            colors = ButtonDefaults.buttonColors(containerColor = ChronosRoseNeon),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isId) "Atur Alarm Ritme Sirkadian" else "Schedule Circadian Sleep Alarms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    "FOOD" -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isId) "Hidrasi" else "Hydration"} · ${lifeLog.water}/$waterTarget",
                                        fontSize = 12.sp,
                                        color = Color(0xFFD1D5DB)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ChronosGreenNeon,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onUpdateLife { it.copy(water = (it.water + 1).coerceAtMost(16)) }
                                            }
                                    ) {
                                        Text(
                                            text = "+1",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChronosBgDeep,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (lifeLog.water.toFloat() / waterTarget.toFloat()).coerceIn(0f, 1f) },
                                    color = ChronosGreenNeon,
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        Triple("breakfast", if (isId) "Pagi" else "AM", lifeLog.breakfast),
                                        Triple("lunch", if (isId) "Siang" else "Noon", lifeLog.lunch),
                                        Triple("dinner", if (isId) "Malam" else "PM", lifeLog.dinner)
                                    ).forEach { (key, label, active) ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (active) ChronosGreenNeon.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (active) ChronosGreenNeon.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onUpdateLife { cur ->
                                                        when (key) {
                                                            "breakfast" -> cur.copy(breakfast = !cur.breakfast)
                                                            "lunch" -> cur.copy(lunch = !cur.lunch)
                                                            else -> cur.copy(dinner = !cur.dinner)
                                                        }
                                                    }
                                                }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (active) ChronosGreenNeon else Color(0xFF9CA3AF),
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                onAddHydrationReminder(
                                    ReminderItem(
                                        id = "hydro_${System.currentTimeMillis()}",
                                        title = if (isId) "Target Hidrasi Air & Segarkan Tubuh" else "Hydration Target & Refresh",
                                        timeStr = "10:30",
                                        recurrence = RecurrenceType.DAILY,
                                        isEnabled = true,
                                        category = "Health"
                                    )
                                )
                                hydrationFeedback = if (isId) "✓ Pengingat hidrasi disetel pukul 10:30!" else "✓ Hydration reminder set for 10:30!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChronosGreenNeon),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = ChronosBgDeep, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = hydrationFeedback ?: if (isId) "Jadwalkan Pengingat Hidrasi 10:30" else "Schedule 10:30 Hydration Reminder",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosBgDeep
                            )
                        }
                    }
                    "ACTIVITY" -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isId) "Gerak hari ini" else "Movement today",
                                        fontSize = 12.sp,
                                        color = Color(0xFFD1D5DB)
                                    )
                                    Text(
                                        text = "${lifeLog.workoutMins}m",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ChronosCyanNeon
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = ChronosCyanNeon.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                onUpdateLife { it.copy(workoutMins = it.workoutMins + 15) }
                                            }
                                    ) {
                                        Text(
                                            text = "+15m",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChronosCyanNeon,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = ChronosCyanNeon,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onStart20MinTimer() }
                                    ) {
                                        Text(
                                            text = if (isId) "Timer 20 mnt" else "20 min timer",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChronosBgDeep,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "Tersimpan di perangkat ini" else "Saved on this device",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isNotified) ChronosGreenNeon.copy(alpha = 0.2f) else ChronosAmberNeon.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isNotified) ChronosGreenNeon.copy(alpha = 0.4f) else ChronosAmberNeon.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleNotify() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isNotified) Icons.Default.Check else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (isNotified) ChronosGreenNeon else ChronosAmberNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isNotified) {
                                    if (isId) "Telah Didaftarkan" else "Subscribed"
                                } else {
                                    if (isId) "Ingatkan Saya" else "Notify Me"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isNotified) ChronosGreenNeon else ChronosAmberNeon
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDetailDialog) {
        AlertDialog(
            onDismissRequest = { showDetailDialog = false },
            containerColor = Color(0xFF0E1624),
            title = {
                Column {
                    Text(
                        text = if (isId) "Pratinjau: ${currentPillar.title}" else "Preview: ${currentPillar.title}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = currentPillar.subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = currentPillar.accentColor
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = currentPillar.description,
                        fontSize = 13.sp,
                        color = Color(0xFFD1D5DB)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isId) "FITUR UNGGULAN:" else "KEY INNOVATIONS:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    currentPillar.highlights.forEach { h ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(currentPillar.accentColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = h, fontSize = 12.sp, color = Color(0xFFE5E7EB))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetailDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ChronosAmberNeon)
                ) {
                    Text(
                        text = if (isId) "Tutup" else "Close",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}

@Composable
private fun CalendarEventsCard(
    appLanguage: String,
    calendarEvents: List<CalendarEvent>,
    agendaEvents: List<AgendaEvent>,
    onAddEvent: (String, String, String, String) -> Unit,
    onRemoveEvent: (String) -> Unit,
    onOpenProductivity: () -> Unit
) {
    val isId = appLanguage == "id"
    var showAddModal by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newTime by remember { mutableStateOf("14:00") }
    var newDesc by remember { mutableStateOf("") }
    var newColor by remember { mutableStateOf("#00E5FF") }

    val unifiedEvents = remember(calendarEvents, agendaEvents) {
        val list = mutableListOf<CalendarEvent>()
        list.addAll(calendarEvents)
        agendaEvents.forEach { pe ->
            list.add(
                CalendarEvent(
                    id = "prod_${pe.id}",
                    title = pe.title,
                    description = if (pe.location.isNotBlank()) "📍 ${pe.location}" else pe.description,
                    startTime = System.currentTimeMillis(),
                    color = pe.colorHex
                )
            )
        }
        list
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.35f)),
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
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFFC084FC),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CALENDAR SYNC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD8B4FE),
                        letterSpacing = 1.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFA855F7).copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showAddModal = true }
                        .testTag("add_calendar_event_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Event",
                            tint = Color(0xFFD8B4FE),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (unifiedEvents.isEmpty()) {
                Text(
                    text = if (isId) "Tidak ada jadwal mendatang" else "No upcoming events",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unifiedEvents.forEach { ev ->
                        val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.02f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(ChronosCyanNeon)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = ev.title,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = Color(0xFF9CA3AF),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = timeFormat.format(Date(ev.startTime)),
                                                fontSize = 11.sp,
                                                color = Color(0xFF9CA3AF)
                                            )
                                            if (ev.description.isNotBlank()) {
                                                Text(
                                                    text = " • ${ev.description}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF9CA3AF),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { onRemoveEvent(ev.id) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Event",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFA855F7).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenProductivity() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "Kelola Seluruh Agenda di Produktivitas" else "Manage Full Agenda in Productivity",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD8B4FE)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFFD8B4FE),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    if (showAddModal) {
        AlertDialog(
            onDismissRequest = { showAddModal = false },
            containerColor = Color(0xFF131D26),
            title = {
                Text(
                    text = if (isId) "Tambah Jadwal Baru" else "Add Calendar Event",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text(if (isId) "Judul Kegiatan" else "Event Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTime,
                        onValueChange = { newTime = it },
                        label = { Text(if (isId) "Waktu (HH:mm)" else "Time (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text(if (isId) "Catatan / Deskripsi" else "Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onAddEvent(newTitle.trim(), newTime.trim(), newDesc.trim(), newColor)
                            newTitle = ""
                            newDesc = ""
                            showAddModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                ) {
                    Text(if (isId) "Simpan Jadwal" else "Save Event", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddModal = false }) {
                    Text(if (isId) "Batal" else "Cancel", color = Color(0xFF9CA3AF))
                }
            }
        )
    }
}

@Composable
private fun WeatherNotificationBanner(
    weatherData: WeatherData?,
    appLanguage: String
) {
    val isId = appLanguage == "id"
    if (weatherData == null) return

    val code = weatherData.code
    val temp = weatherData.temp
    val isSevere = code in listOf(80, 81, 82, 95, 96, 99)
    val isRain = code in listOf(51, 53, 55, 61, 63, 65)
    val isSnow = code in listOf(71, 73, 75, 77, 85, 86)

    val (title, tag, desc, color, icon) = when {
        isSevere -> Quintuple(
            if (isId) "PERINGATAN CUACA BURUK" else "SEVERE STORM WARNING",
            if (isId) "SEKARANG" else "NOW",
            if (isId) "Badai petir atau hujan lebat terdeteksi. Harap batasi aktivitas fisik di luar ruangan."
            else "Thunderstorms or heavy storms detected. Please limit outdoor physical activity.",
            ChronosRoseNeon,
            Icons.Default.Warning
        )
        isRain -> Quintuple(
            if (isId) "ANTISIPASI HUJAN" else "RAIN ADVISORY",
            if (isId) "ADVISORI" else "ADVISORY",
            if (isId) "Hujan ringan hingga sedang terdeteksi. Sedia payung sebelum bepergian!"
            else "Light to moderate rainfall expected. Grab an umbrella before you head out!",
            ChronosCyanNeon,
            Icons.Default.Cloud
        )
        isSnow -> Quintuple(
            if (isId) "ADVISORI SALJU" else "SNOW ADVISORY",
            if (isId) "PERINGATAN" else "WARNING",
            if (isId) "Hujan salju terdeteksi. Waspada jalanan licin dan suhu dingin ekstrem."
            else "Snowfall detected. Be cautious of slippery roads and low temperatures.",
            Color(0xFFE0F7FA),
            Icons.Default.AcUnit
        )
        temp > 32.0 -> Quintuple(
            if (isId) "PERINGATAN SUHU TINGGI" else "HIGH TEMP ALERT",
            if (isId) "TIPS KESEHATAN" else "HEALTH TIP",
            if (isId) "Suhu sangat terik (${"%.1f".format(temp)}°C). Jaga hidrasi tubuh dan gunakan tabir surya SPF 30+."
            else "Very hot outside (${"%.1f".format(temp)}°C). Keep well hydrated and use SPF 30+ sunscreen.",
            Color(0xFFFFB74D),
            Icons.Default.WbSunny
        )
        else -> Quintuple(
            if (isId) "KONDISI CUACA SEMPURNA" else "EXCELLENT CONDITIONS",
            "INFO",
            if (isId) "Cuaca luar biasa (${"%.1f".format(temp)}°C, IKU ${weatherData.aqi}). Sangat ideal untuk beraktivitas luar ruangan."
            else "Excellent conditions (${"%.1f".format(temp)}°C, AQI ${weatherData.aqi}). Perfect for outdoor walks and workouts.",
            Color(0xFF69F0AE),
            Icons.Default.CheckCircle
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = color.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = Color(0xFFD1D5DB),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
