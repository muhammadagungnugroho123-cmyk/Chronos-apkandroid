package com.example.model

import java.util.Calendar
import java.util.Date
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val CHRONOS_VERSION = "1.67.20"

enum class ScreenType(val id: String) {
    CLOCK("clock"),
    ALARMS("alarms"),
    TIMERS("timers"),
    STOPWATCH("stopwatch"),
    WORLD_CLOCK("world_clock"),
    AI_ASSISTANT("ai_assistant"),
    WEATHER("weather"),
    SETTINGS("settings"),
    PRODUCTIVITY("productivity"),
    CALCULATOR("calculator")
}

enum class PeriodFilter(val id: String) {
    TODAY("TODAY"),
    TOMORROW("TOMORROW"),
    DAY_AFTER("DAY_AFTER"),
    THIS_WEEK("THIS_WEEK"),
    NEXT_WEEK("NEXT_WEEK")
}

enum class TaskPriority { HIGH, MEDIUM, LOW }
enum class TaskCategory { WORK, PERSONAL, HEALTH, STUDY, PROJECT, OTHER }
enum class RecurrenceType { NONE, DAILY, WEEKDAYS, WEEKLY }
enum class WakeMission { NONE, MATH, MEMORY, TAP }

data class TaskItem(
    val id: String,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: TaskCategory = TaskCategory.WORK,
    val deadline: String = "18:00",
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val targetPeriod: PeriodFilter = PeriodFilter.TODAY,
    val completedAtMillis: Long = 0L,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class AgendaEvent(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val hasReminder: Boolean = true,
    val colorHex: String = "#00E5FF",
    val targetPeriod: PeriodFilter = PeriodFilter.TODAY,
    val isSystemCalendar: Boolean = false
)

data class HabitItem(
    val id: String,
    val name: String,
    val category: String = "Health",
    val currentCount: Int = 0,
    val targetCount: Int = 8,
    val unit: String = "glasses",
    val streakDays: Int = 1,
    val isCompletedToday: Boolean = false,
    val iconName: String = "Water"
)

data class ReminderItem(
    val id: String,
    val title: String,
    val timeStr: String,
    val recurrence: RecurrenceType = RecurrenceType.DAILY,
    val isEnabled: Boolean = true,
    val category: String = "General"
)

data class NoteItem(
    val id: String,
    val title: String,
    val content: String,
    val category: String = "Work",
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String = "",
    val startTime: Long = System.currentTimeMillis(),
    val color: String = "#00E5FF"
)

data class LifeLog(
    val day: String,
    val water: Int = 0,
    val breakfast: Boolean = false,
    val lunch: Boolean = false,
    val dinner: Boolean = false,
    val sleepHours: Int = 7,
    val workoutMins: Int = 0
)

data class DailyGoalProgress(
    val completedTasks: Int,
    val totalTasks: Int,
    val completedEvents: Int,
    val totalEvents: Int,
    val exerciseMinutes: Int,
    val exerciseTargetMinutes: Int,
    val readingMinutes: Int,
    val readingTargetMinutes: Int,
    val productivityPercentage: Int
)

data class DayProgress(
    val dayLabel: String,
    val percentage: Int,
    val tasksDone: Int,
    val focusMins: Int
)

data class WeeklyProgressSummary(
    val completedTasksCount: Int,
    val remainingTasksCount: Int,
    val focusSessionsCount: Int,
    val habitConsistencyPercentage: Int,
    val productiveDaysCount: Int,
    val overallProductivityPercentage: Int,
    val dailyChart: List<DayProgress>
)

data class AlarmItem(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val label: String = "Bangun Pagi",
    val daysOfWeek: Int = 0, // bitmask: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64
    val dateMillis: Long = 0L,
    val isVibrate: Boolean = true,
    val soundUri: String = "Vitality Spirit",
    val customAudioTitle: String = "",
    val isSnooze: Boolean = true,
    val wakeMission: WakeMission = WakeMission.NONE,
    val fadeInSeconds: Int = 10,
    val preWakeGentle: Boolean = true,
    val backgroundTheme: String = "COSMIC_DAWN"
)

data class WorldCity(
    val name: String,
    val country: String,
    val timeZoneId: String,
    val lat: Double,
    val lon: Double,
    val flag: String
)

data class HourlyForecast(
    val time: String,
    val temp: Double,
    val code: Int,
    val isDay: Boolean,
    val precipProb: Int,
    val windSpeed: Double = 10.0,
    val humidity: Int = 70
)

data class DailyForecast(
    val dayName: String,
    val tempMax: Double,
    val tempMin: Double,
    val code: Int,
    val precipProb: Int,
    val isDay: Boolean
)

data class WeatherData(
    val temp: Double,
    val humidity: Int,
    val windSpeed: Double,
    val description: String,
    val isDay: Boolean,
    val locationName: String,
    val timeOfDayStr: String,
    val code: Int,
    val utcOffsetSeconds: Int = 0,
    val hourly: List<HourlyForecast> = emptyList(),
    val apparentTemp: Double = temp,
    val tempMax: Double = temp + 2,
    val tempMin: Double = temp - 4,
    val uvIndex: Double = 5.0,
    val pressure: Double = 1010.0,
    val visibility: Double = 10.0,
    val dewPoint: Double = 23.0,
    val sunrise: String = "05:46",
    val sunset: String = "17:52",
    val daily: List<DailyForecast> = emptyList(),
    val latitude: Double = -6.2,
    val longitude: Double = 106.8,
    val aqi: Int = 45,
    val source: String = "live", // "live" | "cache" | "offline"
    val windDirectionDeg: Int = 130,
    val windGusts: Double = (windSpeed * 1.38 * 10.0).roundToInt() / 10.0,
    val cloudCover: Int = 42,
    val cloudCoverLow: Int = 24,
    val cloudCoverMid: Int = 18,
    val cloudCoverHigh: Int = 12,
    val precipitationMm: Double = 0.0,
    val seaWindSpeed: Double = (windSpeed * 1.22 * 10.0).roundToInt() / 10.0,
    val waveHeightM: Double = 0.85,
    val swellHeightM: Double = 0.65,
    val wavePeriodSec: Double = 7.8,
    val seaSurfaceTempC: Double = 28.6,
    val pressureDeltaHpa: Double = 0.0,
    val stationElevationM: Double = 14.0,
    val solarRadiationWm2: Double = 420.0,
    val observationTimestampMs: Long = System.currentTimeMillis()
)

data class StopwatchSessionRecord(
    val id: String,
    val activityTag: String,
    val totalElapsedMs: Long,
    val lapCount: Int,
    val fastestLapMs: Long,
    val averageLapMs: Long,
    val consistencyScore: Int,
    val timestampStr: String,
    val lapSplitsMs: List<Long> = emptyList()
)

data class MultiTrackChannel(
    val id: Int,
    val label: String,
    val isRunning: Boolean = false,
    val accumulatedMs: Long = 0L,
    val startRealtimeMs: Long = 0L,
    val laps: List<Long> = emptyList(),
    val accentHex: Long = 0xFF00E5FF
)

data class CalculatorHistoryRecord(
    val id: String,
    val categoryName: String,
    val expression: String,
    val result: String,
    val timestampStr: String
)

val CHRONOS_RINGTONES = listOf(
    "Vitality Spirit",
    "AI Cosmos",
    "AI Pulse",
    "Zen Garden",
    "Cyberpunk",
    "Morning Birds",
    "Ocean Waves",
    "Digital Beep"
)

fun formatWindSpeed(kmh: Double, unitIndex: Int): String = when (unitIndex) {
    1 -> "%.1f m/s".format(java.util.Locale.US, kmh / 3.6)
    2 -> "${(kmh * 0.621371).roundToInt()} mph"
    else -> "${kmh.roundToInt()} km/h"
}

fun formatCompassDirection(deg: Int, isId: Boolean): String {
    val norm = ((deg % 360) + 360) % 360
    return when (norm) {
        in 0..22, in 338..360 -> if (isId) "Utara (U)" else "North (N)"
        in 23..67 -> if (isId) "Timur Laut (TL)" else "Northeast (NE)"
        in 68..112 -> if (isId) "Timur (T)" else "East (E)"
        in 113..157 -> if (isId) "Tenggara (TG)" else "Southeast (SE)"
        in 158..202 -> if (isId) "Selatan (S)" else "South (S)"
        in 203..247 -> if (isId) "Barat Daya (BD)" else "Southwest (SW)"
        in 248..292 -> if (isId) "Barat (B)" else "West (W)"
        else -> if (isId) "Barat Laut (BL)" else "Northwest (NW)"
    }
}

data class AppSettings(
    val themeColorIndex: Int = 0, // 0=Cyan, 1=Blue, 2=Green, 3=Orange, 4=Purple, 5=Rose, 6=Gold
    val clockStyleIndex: Int = 0, // 0=Sci-Fi Ring, 1=Digital Glass, 2=Precision Dial, 3=Cyber HUD, 4=Dual Solar
    val is24HourFormat: Boolean = true,
    val showClockSeconds: Boolean = true,
    val smoothSweepSeconds: Boolean = true,
    val ambientGlowIntensity: Int = 2, // 0=Off, 1=Subtle, 2=Dynamic, 3=Ultra Neon
    val highContrastGlass: Boolean = false,
    val enableSwipeNavigation: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val showHomeUtcBadge: Boolean = true,
    val showSmartDailyBriefing: Boolean = true,
    val showHomeWeatherCard: Boolean = true,
    val showHomeLifeSuite: Boolean = true,
    val showHomeCalendarCard: Boolean = true,
    val defaultVibrate: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val appLanguage: String = "id", // "en" | "id" | "zh"
    val alarmRingtoneIndex: Int = 0, // 0..7 in CHRONOS_RINGTONES
    val showWorldClockGlobe: Boolean = true,
    val worldClockGlobeStyle: Int = 1, // 0=Cyber Grid, 1=Realistic Map, 2=Solar Day/Night, 3=Emerald 3D
    val globeAutoRotate: Boolean = true,
    val globeRotationSpeed: Int = 1, // 0=Slow, 1=Normal, 2=Fast
    val temperatureUnit: Int = 0, // 0=Celsius, 1=Fahrenheit
    val windSpeedUnit: Int = 0, // 0=km/h, 1=m/s, 2=mph
    val weatherAlertsEnabled: Boolean = true,
    val savedCities: List<String> = listOf("Tulungagung", "Jakarta", "Tokyo", "Makkah", "London", "New York"),
    val snoozeDuration: Int = 9,
    val alarmFadeInDuration: Int = 10,
    val preWakeGentleDefault: Boolean = true,
    val bedtimeReminderEnabled: Boolean = true,
    val targetSleepCycles: Int = 5, // 4 (6h), 5 (7.5h), 6 (9h)
    val masterSoundVolume: Float = 0.75f,
    val weatherRefreshInterval: Int = 30, // 15, 30, 60, 0 (Manual)
    val autoCloudBackup: Boolean = true,
    val lastBackupTimestamp: Long = 0L,
    val timerSound: String = "AI Cosmos",
    val timerVibrate: Boolean = true,
    val timerAutoRepeat: Boolean = false,
    val timerAutoSoundscape: String = "NONE", // "NONE", "RAIN", "OCEAN_WAVES", "WIND", "FOCUS_BINAURAL"
    val defaultFocusMinutes: Int = 25, // 15, 25, 45, 60
    val stopwatchShowMs: Boolean = true,
    val stopwatchLapVibrate: Boolean = true,
    val calendarFirstDayOfWeek: String = "Monday",
    val calendarShowHolidays: Boolean = true,
    val dailyWaterGoal: Int = 8, // 6, 8, 10, 12 glasses
    val autoSortTasksByPriority: Boolean = true,
    val aiModelIndex: Int = 0,
    val aiResponseStyle: Int = 1,
    val aiAutoExecuteActions: Boolean = false,
    val defaultWakeMission: WakeMission = WakeMission.NONE,
    val ecoPerformanceMode: Boolean = false,
    val defaultAlarmBackgroundTheme: String = "COSMIC_DAWN"
)

data class ChatMsg(
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

data class ActiveBannerAlert(
    val kind: String, // "reminder" | "timer"
    val title: String,
    val meta: String
)

data class RecommendedWakeTime(
    val cycles: Int,
    val hour: Int,
    val minute: Int,
    val label: String
)

data class SleepCycleTelemetry(
    val h: Int,
    val m: Int,
    val s: Int,
    val totalMins: Int,
    val cycles: Double,
    val fullCycles: Int,
    val qualityLabel: String,
    val qualityColorHex: Long,
    val recommendedWakeTimes: List<RecommendedWakeTime>
)

fun calculateSleepCycleTelemetry(
    hour: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis(),
    isId: Boolean = true
): SleepCycleTelemetry {
    val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val targetCal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= nowMillis) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    val diffMs = max(1000L, targetCal.timeInMillis - nowCal.timeInMillis)
    val totalMins = max(1, (diffMs / 60000L).toInt())
    val totalSecs = max(1, (diffMs / 1000L).toInt())
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    val effectiveSleepMins = max(0, totalMins - 14)
    val cycles = ((effectiveSleepMins / 90.0) * 10.0).roundToInt() / 10.0
    val fullCycles = (effectiveSleepMins / 90.0).roundToInt()

    val (qualityLabel, qualityColorHex) = when {
        totalMins < 90 -> (if (isId) "Power Nap / Istirahat Singkat" else "Power Nap / Short Rest") to 0xFF00E5FF
        totalMins < 360 -> (if (isId) "Tidur Singkat (< 4 Siklus)" else "Short Recovery (< 4 Cycles)") to 0xFFFFD54F
        totalMins > 570 -> (if (isId) "Restorasi Panjang (6+ Siklus)" else "Extended Recovery (6+ Cycles)") to 0xFF2979FF
        else -> (if (isId) "Optimal (Siklus Penuh)" else "Optimal (Full REM Cycles)") to 0xFF00E676
    }

    val recs = listOf(1, 4, 5, 6).map { c ->
        val dt = Calendar.getInstance().apply {
            timeInMillis = nowMillis + (14 + c * 90) * 60_000L
        }
        val hrsStr = if ((c * 1.5) % 1.0 == 0.0) "${(c * 1.5).toInt()}h" else "${c * 1.5}h"
        val lbl = if (c == 1) {
            if (isId) "Power Cycle (1.5j)" else "1 Cycle (1.5h)"
        } else {
            "$c ${if (isId) "Siklus" else "Cycles"} ($hrsStr)"
        }
        RecommendedWakeTime(
            cycles = c,
            hour = dt.get(Calendar.HOUR_OF_DAY),
            minute = dt.get(Calendar.MINUTE),
            label = lbl
        )
    }

    return SleepCycleTelemetry(
        h = h,
        m = m,
        s = s,
        totalMins = totalMins,
        cycles = cycles,
        fullCycles = fullCycles,
        qualityLabel = qualityLabel,
        qualityColorHex = qualityColorHex,
        recommendedWakeTimes = recs
    )
}

fun getDayOfWeekMask(cal: Calendar): Int {
    val day = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2, ..., Sat=7
    val bitIndex = if (day == Calendar.SUNDAY) 6 else day - 2
    return 1 shl bitIndex
}

fun getNextAlarmOccurrenceMillis(alarm: AlarmItem, nowMillis: Long = System.currentTimeMillis()): Long? {
    if (!alarm.isEnabled) return null

    if (alarm.dateMillis > 0L) {
        if (alarm.label.startsWith("Snooze:") && alarm.dateMillis > nowMillis) {
            return alarm.dateMillis
        }
        val target = Calendar.getInstance().apply {
            timeInMillis = alarm.dateMillis
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return if (target.timeInMillis > nowMillis) target.timeInMillis else null
    }

    if (alarm.daysOfWeek > 0) {
        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.timeInMillis <= nowMillis) continue
            val mask = getDayOfWeekMask(candidate)
            if ((alarm.daysOfWeek and mask) != 0) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    val candidate = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, alarm.hour)
        set(Calendar.MINUTE, alarm.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= nowMillis) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    return candidate.timeInMillis
}

fun formatAlarmTargetDateTime(alarm: AlarmItem, nowMillis: Long = System.currentTimeMillis(), isId: Boolean = true): String {
    val targetMs = getNextAlarmOccurrenceMillis(alarm, nowMillis) ?: return ""
    val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val targetCal = Calendar.getInstance().apply { timeInMillis = targetMs }
    val timePart = "%02d:%02d:%02d".format(
        targetCal.get(Calendar.HOUR_OF_DAY),
        targetCal.get(Calendar.MINUTE),
        targetCal.get(Calendar.SECOND)
    )
    val sameDay = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
    val tomorrowCal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, 1)
    }
    val isTomorrow = tomorrowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
        tomorrowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)

    val dayPrefix = when {
        sameDay -> if (isId) "Hari Ini" else "Today"
        isTomorrow -> if (isId) "Besok" else "Tomorrow"
        else -> {
            val dayNamesId = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
            val dayNamesEn = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val dow = (targetCal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)
            if (isId) dayNamesId[dow] else dayNamesEn[dow]
        }
    }
    return "$dayPrefix • $timePart"
}

fun formatAlarmRemaining(alarm: AlarmItem, nowMillis: Long = System.currentTimeMillis()): String {
    val target = getNextAlarmOccurrenceMillis(alarm, nowMillis) ?: return ""
    val diffMs = target - nowMillis
    if (diffMs <= 0L) return "< 1 m"
    val totalSecs = (diffMs / 1000L).toInt()
    val totalMins = totalSecs / 60
    val days = totalMins / (24 * 60)
    val hours = (totalMins % (24 * 60)) / 60
    val mins = totalMins % 60
    val secs = totalSecs % 60
    if (days > 0) return if (hours > 0) "${days}h ${hours}j" else "${days} hari"
    if (hours > 0) return if (mins > 0) "${hours}j ${mins}m" else "${hours}j"
    return if (mins == 0) "${secs}d" else "${mins}m ${secs}d"
}

val ALL_AVAILABLE_CITIES = listOf(
    WorldCity("Jakarta", "Indonesia (WIB)", "Asia/Jakarta", -6.2, 106.8, "🇮🇩"),
    WorldCity("Tulungagung", "Indonesia (WIB)", "Asia/Jakarta", -8.06, 111.9, "🇮🇩"),
    WorldCity("Surabaya", "Indonesia (WIB)", "Asia/Jakarta", -7.25, 112.75, "🇮🇩"),
    WorldCity("Yogyakarta", "Indonesia (WIB)", "Asia/Jakarta", -7.79, 110.36, "🇮🇩"),
    WorldCity("Bandung", "Indonesia (WIB)", "Asia/Jakarta", -6.91, 107.61, "🇮🇩"),
    WorldCity("Bali", "Indonesia (WITA)", "Asia/Makassar", -8.65, 115.21, "🇮🇩"),
    WorldCity("Makassar", "Indonesia (WITA)", "Asia/Makassar", -5.14, 119.43, "🇮🇩"),
    WorldCity("Jayapura", "Indonesia (WIT)", "Asia/Jayapura", -2.53, 140.71, "🇮🇩"),
    WorldCity("Singapore", "Singapore", "Asia/Singapore", 1.3, 103.8, "🇸🇬"),
    WorldCity("Kuala Lumpur", "Malaysia", "Asia/Kuala_Lumpur", 3.13, 101.68, "🇲🇾"),
    WorldCity("Tokyo", "Japan", "Asia/Tokyo", 35.6, 139.6, "🇯🇵"),
    WorldCity("Seoul", "South Korea", "Asia/Seoul", 37.5, 126.9, "🇰🇷"),
    WorldCity("Hong Kong", "China", "Asia/Hong_Kong", 22.3, 114.1, "🇭🇰"),
    WorldCity("Beijing", "China", "Asia/Shanghai", 39.9, 116.4, "🇨🇳"),
    WorldCity("Bangkok", "Thailand", "Asia/Bangkok", 13.7, 100.5, "🇹🇭"),
    WorldCity("Sydney", "Australia", "Australia/Sydney", -33.8, 151.2, "🇦🇺"),
    WorldCity("Auckland", "New Zealand", "Pacific/Auckland", -36.84, 174.76, "🇳🇿"),
    WorldCity("Dubai", "UAE", "Asia/Dubai", 25.2, 55.2, "🇦🇪"),
    WorldCity("Makkah", "Saudi Arabia", "Asia/Riyadh", 21.42, 39.82, "🇸🇦"),
    WorldCity("Doha", "Qatar", "Asia/Qatar", 25.28, 51.53, "🇶🇦"),
    WorldCity("Istanbul", "Turkey", "Europe/Istanbul", 41.0, 28.9, "🇹🇷"),
    WorldCity("Moscow", "Russia", "Europe/Moscow", 55.7, 37.6, "🇷🇺"),
    WorldCity("Mumbai", "India", "Asia/Kolkata", 19.0, 72.8, "🇮🇳"),
    WorldCity("London", "UK", "Europe/London", 51.5, -0.1, "🇬🇧"),
    WorldCity("Paris", "France", "Europe/Paris", 48.8, 2.3, "🇫🇷"),
    WorldCity("Berlin", "Germany", "Europe/Berlin", 52.5, 13.4, "🇩🇪"),
    WorldCity("Zurich", "Switzerland", "Europe/Zurich", 47.37, 8.54, "🇨🇭"),
    WorldCity("Rome", "Italy", "Europe/Rome", 41.9, 12.5, "🇮🇹"),
    WorldCity("Cairo", "Egypt", "Africa/Cairo", 30.0, 31.2, "🇪🇬"),
    WorldCity("Cape Town", "South Africa", "Africa/Johannesburg", -33.9, 18.4, "🇿🇦"),
    WorldCity("New York", "USA", "America/New_York", 40.7, -74.0, "🇺🇸"),
    WorldCity("Chicago", "USA", "America/Chicago", 41.8, -87.6, "🇺🇸"),
    WorldCity("Los Angeles", "USA", "America/Los_Angeles", 34.0, -118.2, "🇺🇸"),
    WorldCity("San Francisco", "USA", "America/Los_Angeles", 37.77, -122.41, "🇺🇸"),
    WorldCity("Toronto", "Canada", "America/Toronto", 43.7, -79.3, "🇨🇦"),
    WorldCity("São Paulo", "Brazil", "America/Sao_Paulo", -23.5, -46.6, "🇧🇷"),
    WorldCity("Mexico City", "Mexico", "America/Mexico_City", 19.4, -99.1, "🇲🇽")
)

val WORLD_MAP_POLYGONS: List<IntArray> = listOf(
    intArrayOf(-125, 50, -120, 50, -110, 49, -95, 49, -82, 42, -75, 45, -65, 45, -70, 42, -75, 35, -80, 25, -82, 28, -90, 30, -97, 26, -105, 23, -110, 30, -118, 34, -124, 40, -125, 50),
    intArrayOf(-165, 65, -155, 71, -140, 70, -130, 55, -145, 60, -165, 60, -165, 65),
    intArrayOf(-75, 10, -60, 8, -50, 0, -35, -5, -40, -22, -50, -30, -58, -35, -65, -55, -72, -52, -75, -45, -70, -30, -80, -5, -77, 2, -75, 10),
    intArrayOf(-10, 36, 0, 42, 3, 47, 8, 54, 15, 55, 25, 70, 30, 70, 35, 65, 40, 55, 30, 45, 25, 40, 20, 37, 15, 38, -5, 36, -10, 36),
    intArrayOf(-5, 50, 1, 52, -2, 58, -5, 58, -5, 50),
    intArrayOf(-17, 15, -15, 25, -5, 35, 10, 37, 25, 32, 32, 31, 40, 15, 51, 12, 42, 0, 40, -12, 32, -28, 28, -34, 18, -34, 12, -18, 10, -5, 3, 5, -8, 5, -17, 15),
    intArrayOf(44, -12, 50, -16, 47, -25, 43, -25, 44, -12),
    intArrayOf(35, 32, 45, 40, 50, 40, 60, 40, 70, 40, 80, 42, 90, 45, 100, 42, 110, 44, 120, 50, 135, 48, 140, 55, 160, 60, 170, 65, 180, 65, 170, 70, 140, 72, 100, 75, 70, 72, 60, 68, 50, 60, 38, 42, 35, 32),
    intArrayOf(68, 24, 72, 20, 77, 8, 80, 13, 85, 20, 90, 22, 92, 25, 88, 27, 78, 35, 70, 30, 68, 24),
    intArrayOf(100, 20, 108, 21, 105, 10, 102, 6, 100, 15, 105, 22, 115, 22, 122, 30, 120, 38, 125, 40, 120, 25, 100, 20),
    intArrayOf(130, 32, 135, 35, 140, 36, 142, 44, 140, 45, 135, 37, 130, 32),
    intArrayOf(95, 5, 105, -5, 106, -6, 114, -8, 115, -8, 108, -6, 98, 3, 95, 5),
    intArrayOf(110, -3, 118, -4, 118, 5, 110, 2, 110, -3),
    intArrayOf(120, -3, 125, 1, 123, -5, 120, -3),
    intArrayOf(132, -2, 140, -3, 141, -8, 135, -4, 132, -2),
    intArrayOf(114, -22, 113, -26, 115, -34, 125, -32, 135, -34, 145, -38, 150, -36, 153, -28, 145, -15, 136, -12, 125, -15, 118, -20, 114, -22),
    intArrayOf(168, -46, 174, -42, 172, -43, 168, -46),
    intArrayOf(174, -36, 178, -38, 175, -41, 174, -36),
    intArrayOf(-45, 60, -25, 70, -20, 80, -35, 83, -55, 80, -55, 70, -45, 60),
    intArrayOf(5, 60, 15, 65, 25, 71, 30, 70, 20, 60, 12, 57, 5, 60)
)

object ChronosTranslations {
    private val en = mapOf(
        "clock" to "Clock",
        "timers" to "Timers",
        "stopwatch" to "Stopwatch",
        "ai_assistant" to "AI Assistant",
        "world_clock" to "World Clock",
        "settings" to "Settings",
        "appearance" to "Appearance",
        "language" to "Language",
        "theme_color" to "Theme Color",
        "time_date" to "Time & Date",
        "24_hour_format" to "24-hour format",
        "use_24_hour_format" to "Use 24-hour time format",
        "alarms_timers" to "Alarms & Timers",
        "default_vibrate" to "Default Vibrate",
        "vibrate_desc" to "Vibrate when alarm or timer goes off",
        "notifications" to "Notifications",
        "notifications_desc" to "Show notifications for alarms and timers",
        "about" to "About",
        "version" to "Version",
        "weather" to "Weather",
        "productivity" to "Productivity",
        "calculator" to "Kalkulator Smart",
        "alarms" to "Alarms",
        "add_alarm" to "Add Alarm",
        "edit_alarm" to "Edit Alarm",
        "save" to "Save",
        "cancel" to "Cancel",
        "label" to "Label (optional)",
        "vibrate" to "Vibrate",
        "snooze" to "Snooze",
        "alarm_sound" to "Alarm Sound",
        "no_alarms" to "No alarms set",
        "custom_timer" to "Custom Timer",
        "set" to "Set",
        "search_city" to "Search city...",
        "dismiss" to "Dismiss",
        "add_city" to "Add City",
        "timer" to "Timer",
        "lap" to "Lap",
        "clock_style" to "Clock Style",
        "ringtone" to "Ringtone",
        "add_new_alarm" to "ADD NEW ALARM",
        "alarm_name" to "ALARM NAME",
        "repeat_days" to "REPEAT DAYS",
        "smart_alarm" to "SMART ALARM FEATURE",
        "active" to "ACTIVE",
        "inactive" to "INACTIVE",
        "save_alarm" to "SAVE ALARM",
        "ai_suggestion" to "AI SUGGESTION",
        "start" to "START",
        "stop" to "STOP",
        "reset" to "RESET",
        "resume" to "RESUME",
        "pause" to "PAUSE",
        "ai_greeting" to "Hello! I am your CHRONOS AI Assistant. How can I assist you with your schedule, alarms, or climate forecasts?",
        "ask_schedule" to "Ask for schedule advice or control timers...",
        "weather_clear" to "Clear",
        "weather_mostly_clear" to "Mostly Clear",
        "weather_cloudy" to "Cloudy",
        "weather_overcast" to "Overcast",
        "weather_fog" to "Foggy",
        "weather_drizzle" to "Drizzle",
        "weather_rain" to "Rain",
        "weather_heavy_rain" to "Heavy Rain",
        "weather_snow" to "Snow",
        "weather_thunderstorm" to "Thunderstorm",
        "weather_thunderstorm_hail" to "Thunderstorm with Hail",
        "time_early_morning" to "Early Morning",
        "time_morning" to "Morning",
        "time_noon" to "Noon",
        "time_afternoon" to "Afternoon",
        "time_evening" to "Evening"
    )

    private val id = mapOf(
        "clock" to "Jam",
        "timers" to "Pengatur Waktu",
        "stopwatch" to "Stopwatch",
        "ai_assistant" to "AI Asisten",
        "world_clock" to "Jam Dunia",
        "settings" to "Pengaturan",
        "appearance" to "Tampilan",
        "language" to "Bahasa",
        "theme_color" to "Warna Tema",
        "time_date" to "Waktu & Tanggal",
        "24_hour_format" to "Format 24 jam",
        "use_24_hour_format" to "Gunakan format waktu 24 jam",
        "alarms_timers" to "Alarm & Pengatur Waktu",
        "default_vibrate" to "Getar Default",
        "vibrate_desc" to "Bergetar saat alarm atau timer berbunyi",
        "notifications" to "Notifikasi",
        "notifications_desc" to "Tampilkan notifikasi untuk alarm dan timer",
        "about" to "Tentang",
        "version" to "Versi",
        "weather" to "Cuaca",
        "productivity" to "Produktivitas",
        "calculator" to "Kalkulator Smart",
        "alarms" to "Alarm",
        "add_alarm" to "Tambah Alarm",
        "edit_alarm" to "Edit Alarm",
        "save" to "Simpan",
        "cancel" to "Batal",
        "label" to "Label (opsional)",
        "vibrate" to "Getar",
        "snooze" to "Tidur Sebentar",
        "alarm_sound" to "Suara Alarm",
        "no_alarms" to "Tidak ada alarm",
        "custom_timer" to "Pengatur Waktu Kustom",
        "set" to "Atur",
        "search_city" to "Cari kota...",
        "dismiss" to "Berhenti",
        "add_city" to "Tambah Kota",
        "timer" to "Pengatur Waktu",
        "lap" to "Putaran",
        "clock_style" to "Gaya Jam",
        "ringtone" to "Nada Dering",
        "add_new_alarm" to "TAMBAH ALARM BARU",
        "alarm_name" to "NAMA ALARM",
        "repeat_days" to "PENGULANGAN HARI",
        "smart_alarm" to "FITUR ALARM CERDAS",
        "active" to "AKTIF",
        "inactive" to "NONAKTIF",
        "save_alarm" to "SIMPAN ALARM",
        "ai_suggestion" to "SARAN AI",
        "start" to "MULAI",
        "stop" to "BERHENTI",
        "reset" to "RESET",
        "resume" to "LANJUTKAN",
        "pause" to "JEDA",
        "ai_greeting" to "Halo! Saya Asisten AI CHRONOS. Ada yang bisa saya bantu terkait jadwal, alarm, atau prakiraan iklim?",
        "ask_schedule" to "Tanyakan saran jadwal atau kendalikan jam...",
        "weather_clear" to "Cerah",
        "weather_mostly_clear" to "Cerah Berawan",
        "weather_cloudy" to "Berawan",
        "weather_overcast" to "Mendung",
        "weather_fog" to "Berkabut",
        "weather_drizzle" to "Gerimis",
        "weather_rain" to "Hujan",
        "weather_heavy_rain" to "Hujan Deras",
        "weather_snow" to "Salju",
        "weather_thunderstorm" to "Badai Petir",
        "weather_thunderstorm_hail" to "Badai Petir & Es",
        "time_early_morning" to "Dini Hari",
        "time_morning" to "Pagi",
        "time_noon" to "Siang",
        "time_afternoon" to "Sore",
        "time_evening" to "Malam"
    )

    private val zh = mapOf(
        "clock" to "时钟",
        "timers" to "计时器",
        "stopwatch" to "秒表",
        "ai_assistant" to "AI 助手",
        "world_clock" to "世界时钟",
        "settings" to "设置",
        "appearance" to "外观",
        "language" to "语言",
        "theme_color" to "主题颜色",
        "time_date" to "时间与日期",
        "24_hour_format" to "24小时制",
        "use_24_hour_format" to "使用24小时制格式",
        "about" to "关于",
        "version" to "版本",
        "weather" to "天气",
        "productivity" to "生产力",
        "alarms" to "闹钟",
        "add_alarm" to "添加闹钟",
        "edit_alarm" to "编辑闹钟",
        "save" to "保存",
        "cancel" to "取消",
        "vibrate" to "振动",
        "snooze" to "小睡",
        "alarm_sound" to "闹钟铃声",
        "no_alarms" to "未设置闹钟",
        "custom_timer" to "自定义计时器",
        "set" to "设定",
        "search_city" to "搜索城市...",
        "dismiss" to "关闭",
        "add_city" to "添加城市",
        "timer" to "计时器",
        "lap" to "记圈",
        "clock_style" to "表盘风格",
        "ringtone" to "铃声",
        "add_new_alarm" to "添加新闹钟",
        "alarm_name" to "闹钟名称",
        "repeat_days" to "重复周期",
        "smart_alarm" to "智能闹钟功能",
        "active" to "启用",
        "inactive" to "未启用",
        "save_alarm" to "保存闹钟",
        "ai_suggestion" to "AI建议",
        "start" to "开始",
        "stop" to "停止",
        "reset" to "重置",
        "resume" to "继续",
        "pause" to "暂停",
        "ai_greeting" to "您好！我是 CHRONOS AI 助手，有什么可以为您服务的吗？",
        "ask_schedule" to "咨询作息建议或控制时钟...",
        "weather_clear" to "晴朗",
        "weather_mostly_clear" to "大部晴朗",
        "weather_cloudy" to "多云",
        "weather_overcast" to "阴天",
        "weather_fog" to "大雾",
        "weather_drizzle" to "毛毛雨",
        "weather_rain" to "降雨",
        "weather_heavy_rain" to "暴雨",
        "weather_snow" to "降雪",
        "weather_thunderstorm" to "雷阵雨",
        "weather_thunderstorm_hail" to "雷暴冰雹",
        "time_early_morning" to "凌晨",
        "time_morning" to "早晨",
        "time_noon" to "中午",
        "time_afternoon" to "下午",
        "time_evening" to "夜晚"
    )

    fun t(lang: String, key: String): String {
        val dict = when (lang) {
            "id" -> id
            "zh" -> zh
            else -> en
        }
        return dict[key] ?: en[key] ?: key
    }

    fun daysList(lang: String): List<String> = when (lang) {
        "id" -> listOf("S", "S", "R", "K", "J", "S", "M")
        "zh" -> listOf("一", "二", "三", "四", "五", "六", "日")
        else -> listOf("M", "T", "W", "T", "F", "S", "S")
    }
}
