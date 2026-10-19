package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.model.ALL_AVAILABLE_CITIES
import com.example.model.ActiveBannerAlert
import com.example.model.AgendaEvent
import com.example.model.AlarmItem
import com.example.model.AppSettings
import com.example.model.CHRONOS_VERSION
import com.example.model.CalculatorHistoryRecord
import com.example.model.CalendarEvent
import com.example.model.ChatMsg
import com.example.model.ChronosTranslations
import com.example.model.DailyGoalProgress
import com.example.model.DayProgress
import com.example.model.HabitItem
import com.example.model.LifeLog
import com.example.model.NoteItem
import com.example.model.PeriodFilter
import com.example.model.RecurrenceType
import com.example.model.ReminderItem
import com.example.model.ScreenType
import com.example.model.StopwatchSessionRecord
import com.example.model.TaskCategory
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.model.WakeMission
import com.example.model.WeatherData
import com.example.model.WeeklyProgressSummary
import com.example.model.formatAlarmRemaining
import com.example.model.formatAlarmTargetDateTime
import com.example.model.getDayOfWeekMask
import com.example.model.getNextAlarmOccurrenceMillis
import com.example.service.ChronosAlarmScheduler
import com.example.service.ChronosAudioSynthesizer
import com.example.service.WeatherService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PendingAiAction(
    val kind: String,
    val summary: String,
    val execute: () -> Unit
)

class ChronosStateHolder(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("chronos_v1_66_88_prefs", Context.MODE_PRIVATE)
    private var isRestoring = false
    @Volatile
    private var stateDirty = false

    // Splash & Navigation
    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    private val _currentScreen = MutableStateFlow(ScreenType.CLOCK)
    val currentScreen: StateFlow<ScreenType> = _currentScreen.asStateFlow()

    private val screenHistory = mutableListOf(ScreenType.CLOCK)

    private val _isFeatureHubOpen = MutableStateFlow(false)
    val isFeatureHubOpen: StateFlow<Boolean> = _isFeatureHubOpen.asStateFlow()

    private val _selectedLifePillar = MutableStateFlow("PRODUCTIVITY")
    val selectedLifePillar: StateFlow<String> = _selectedLifePillar.asStateFlow()

    // Live 1-second Clock Tick
    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    // App Settings
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // Live Online / Offline Network Telemetry State
    private val _isNetworkOnline = MutableStateFlow(true)
    val isNetworkOnline: StateFlow<Boolean> = _isNetworkOnline.asStateFlow()

    private val _networkTransportLabel = MutableStateFlow("ONLINE • CLOUD SYNC")
    val networkTransportLabel: StateFlow<String> = _networkTransportLabel.asStateFlow()

    // Alarms
    private val _alarms = MutableStateFlow(
        listOf(
            AlarmItem(
                id = 1L,
                hour = 6,
                minute = 30,
                isEnabled = true,
                label = "Bangun Pagi & Meditasi|||Awali hari dengan kesadaran penuh dan energi positif.",
                daysOfWeek = 31, // Mon-Fri
                dateMillis = 0L,
                isVibrate = true,
                soundUri = "AI Cosmos",
                isSnooze = true
            ),
            AlarmItem(
                id = 2L,
                hour = 7,
                minute = 15,
                isEnabled = false,
                label = "Olahraga Pagi",
                daysOfWeek = 0,
                dateMillis = 0L,
                isVibrate = true,
                soundUri = "AI Pulse",
                isSnooze = true
            )
        )
    )
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _ringingAlarm = MutableStateFlow<AlarmItem?>(null)
    val ringingAlarm: StateFlow<AlarmItem?> = _ringingAlarm.asStateFlow()

    private val firedAlarmKeys = mutableMapOf<Long, String>()
    private val firedPreWakeKeys = mutableSetOf<String>()
    private val firedReminderKeys = mutableSetOf<String>()

    // Active Toast / Reminder Banner
    private val _activeBanner = MutableStateFlow<ActiveBannerAlert?>(null)
    val activeBanner: StateFlow<ActiveBannerAlert?> = _activeBanner.asStateFlow()

    // Central Timer State
    private val _timerTotalSeconds = MutableStateFlow(25 * 60)
    val timerTotalSeconds: StateFlow<Int> = _timerTotalSeconds.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(25 * 60)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val _timerIsRunning = MutableStateFlow(false)
    val timerIsRunning: StateFlow<Boolean> = _timerIsRunning.asStateFlow()

    private val _timerIsCompleted = MutableStateFlow(false)
    val timerIsCompleted: StateFlow<Boolean> = _timerIsCompleted.asStateFlow()

    private var timerEndTimeMs: Long? = null

    // Central Stopwatch State
    private val _stopwatchIsRunning = MutableStateFlow(false)
    val stopwatchIsRunning: StateFlow<Boolean> = _stopwatchIsRunning.asStateFlow()

    private val _stopwatchElapsedMs = MutableStateFlow(0L)
    val stopwatchElapsedMs: StateFlow<Long> = _stopwatchElapsedMs.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow<List<Long>>(emptyList())
    val stopwatchLaps: StateFlow<List<Long>> = _stopwatchLaps.asStateFlow()

    private val _savedStopwatchSessions = MutableStateFlow<List<StopwatchSessionRecord>>(emptyList())
    val savedStopwatchSessions: StateFlow<List<StopwatchSessionRecord>> = _savedStopwatchSessions.asStateFlow()

    // Calculator Smart History State
    private val _calculatorHistory = MutableStateFlow<List<CalculatorHistoryRecord>>(emptyList())
    val calculatorHistory: StateFlow<List<CalculatorHistoryRecord>> = _calculatorHistory.asStateFlow()

    private var stopwatchStartRealtime: Long = 0L
    private var stopwatchAccumulatedMs: Long = 0L
    private var stopwatchJob: Job? = null

    // Weather State
    private val _currentWeatherData = MutableStateFlow<WeatherData?>(
        WeatherService.offlineWeather("Jakarta", -6.2, 106.8)
    )
    val currentWeatherData: StateFlow<WeatherData?> = _currentWeatherData.asStateFlow()

    private val _isLoadingWeather = MutableStateFlow(false)
    val isLoadingWeather: StateFlow<Boolean> = _isLoadingWeather.asStateFlow()

    private val _savedWeatherCities = MutableStateFlow(
        listOf("Jakarta", "Tokyo", "London", "New York")
    )
    val savedWeatherCities: StateFlow<List<String>> = _savedWeatherCities.asStateFlow()

    private val _citiesWeatherData = MutableStateFlow<List<WeatherData>>(emptyList())
    val citiesWeatherData: StateFlow<List<WeatherData>> = _citiesWeatherData.asStateFlow()

    // Life Suite Log
    private val _lifeLog = MutableStateFlow(LifeLog(day = todayKey()))
    val lifeLog: StateFlow<LifeLog> = _lifeLog.asStateFlow()

    private val _isLifeNotified = MutableStateFlow(false)
    val isLifeNotified: StateFlow<Boolean> = _isLifeNotified.asStateFlow()

    // Productivity Suite State
    private val _selectedPeriod = MutableStateFlow(PeriodFilter.TODAY)
    val selectedPeriod: StateFlow<PeriodFilter> = _selectedPeriod.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private val _events = MutableStateFlow<List<AgendaEvent>>(emptyList())
    val events: StateFlow<List<AgendaEvent>> = _events.asStateFlow()

    private val _habits = MutableStateFlow<List<HabitItem>>(emptyList())
    val habits: StateFlow<List<HabitItem>> = _habits.asStateFlow()

    private val _reminders = MutableStateFlow<List<ReminderItem>>(emptyList())
    val reminders: StateFlow<List<ReminderItem>> = _reminders.asStateFlow()

    private val _notes = MutableStateFlow<List<NoteItem>>(emptyList())
    val notes: StateFlow<List<NoteItem>> = _notes.asStateFlow()

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    // Productivity Focus Session State
    private val _focusTotalSeconds = MutableStateFlow(25 * 60)
    val focusTotalSeconds: StateFlow<Int> = _focusTotalSeconds.asStateFlow()

    private val _focusRemainingSeconds = MutableStateFlow(25 * 60)
    val focusRemainingSeconds: StateFlow<Int> = _focusRemainingSeconds.asStateFlow()

    private val _isFocusRunning = MutableStateFlow(false)
    val isFocusRunning: StateFlow<Boolean> = _isFocusRunning.asStateFlow()

    private var focusEndTimeMs: Long? = null

    private val _completedFocusSessions = MutableStateFlow(0)
    val completedFocusSessions: StateFlow<Int> = _completedFocusSessions.asStateFlow()

    private val _totalFocusMinutes = MutableStateFlow(0)
    val totalFocusMinutes: StateFlow<Int> = _totalFocusMinutes.asStateFlow()

    private val _aiInsights = MutableStateFlow<List<String>>(emptyList())
    val aiInsights: StateFlow<List<String>> = _aiInsights.asStateFlow()

    private val _isAiAnalyzing = MutableStateFlow(false)
    val isAiAnalyzing: StateFlow<Boolean> = _isAiAnalyzing.asStateFlow()

    // AI Assistant Chat State
    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMsg(
                text = ChronosTranslations.t("id", "ai_greeting"),
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMsg>> = _chatMessages.asStateFlow()

    private val _pendingAiAction = MutableStateFlow<PendingAiAction?>(null)
    val pendingAiAction: StateFlow<PendingAiAction?> = _pendingAiAction.asStateFlow()

    private val mainSwipeOrder = listOf(
        ScreenType.CLOCK,
        ScreenType.ALARMS,
        ScreenType.TIMERS,
        ScreenType.STOPWATCH,
        ScreenType.WORLD_CLOCK,
        ScreenType.AI_ASSISTANT,
        ScreenType.PRODUCTIVITY
    )

    init {
        loadPersistedState()
        syncAlarmsToBackgroundEngine()

        // Observe background BroadcastReceiver alarm rings & notification actions
        viewModelScope.launch {
            ChronosAlarmScheduler.backgroundRingingAlarm.collect { bgAlarm ->
                if (bgAlarm != null && _ringingAlarm.value == null) {
                    val matched = _alarms.value.find { it.id == bgAlarm.id } ?: bgAlarm
                    val cal = Calendar.getInstance()
                    markAlarmFiredForCal(matched.id, cal, matched.hour, matched.minute)
                    _ringingAlarm.value = matched
                }
            }
        }

        viewModelScope.launch {
            ChronosAlarmScheduler.backgroundActionEvent.collect { evt ->
                if (evt != null) {
                    val (action, alarmId) = evt
                    ChronosAlarmScheduler.consumeBackgroundActionEvent()
                    if (action == ChronosAlarmScheduler.ACTION_SNOOZE_ALARM) {
                        if (_ringingAlarm.value?.id == alarmId) {
                            snoozeRingingAlarm()
                        } else {
                            loadPersistedState()
                            syncAlarmsToBackgroundEngine()
                        }
                    } else if (action == ChronosAlarmScheduler.ACTION_DISMISS_ALARM) {
                        if (_ringingAlarm.value?.id == alarmId) {
                            dismissRingingAlarm()
                        } else {
                            val target = _alarms.value.find { it.id == alarmId }
                            if (target != null) {
                                val cal = Calendar.getInstance()
                                markAlarmFiredForCal(target.id, cal, target.hour, target.minute)
                                if (target.label.startsWith("Snooze:")) {
                                    deleteAlarm(target.id)
                                } else if (target.daysOfWeek == 0 || target.dateMillis > 0L) {
                                    updateAlarm(target.copy(isEnabled = false))
                                } else {
                                    // Mark upcoming occurrence as skipped & re-sync next day's cycle
                                    val now = System.currentTimeMillis()
                                    val upcomingMs = getNextAlarmOccurrenceMillis(target, now)
                                    if (upcomingMs != null) {
                                        ChronosAlarmScheduler.markUpcomingOccurrenceSkipped(target.id, upcomingMs)
                                    }
                                    syncAlarmsToBackgroundEngine()
                                }
                            }
                        }
                    }
                }
            }
        }

        // Splash screen 1.2s timeout matching chronos.tsx
        viewModelScope.launch {
            delay(1200L)
            _showSplash.value = false
        }

        // Initial Weather fetch
        refreshPrimaryWeather(force = false)
        loadAllSavedCitiesWeather()
        recomputeAiInsights()

        // Auto-save listener for reactive state flows (only writes when dirty)
        viewModelScope.launch {
            while (isActive) {
                delay(2000L)
                if (stateDirty) {
                    savePersistedState()
                }
            }
        }

        // Auto-refresh weather based on settings.weatherRefreshInterval
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                val intervalMins = _settings.value.weatherRefreshInterval
                if (intervalMins > 0) {
                    val cal = Calendar.getInstance()
                    if (cal.get(Calendar.MINUTE) % intervalMins == 0 && cal.get(Calendar.SECOND) < 5) {
                        refreshPrimaryWeather(force = true)
                    }
                }
            }
        }

        // Zero-Drain Adaptive Clock, Alarm, Timer, Focus, & Reminder Scheduler Loop (v1.67.10)
        viewModelScope.launch {
            var lastCheckedSecond = -1L
            var lastNetworkCheckMs = 0L
            checkLiveNetworkStatus()
            while (isActive) {
                val now = System.currentTimeMillis()
                _nowMillis.value = now
                val nowSec = now / 1000L

                if (now - lastNetworkCheckMs >= 15_000L) {
                    lastNetworkCheckMs = now
                    checkLiveNetworkStatus()
                }

                if (nowSec != lastCheckedSecond) {
                    lastCheckedSecond = nowSec
                    checkAlarms(now)
                    checkReminders(now)
                    if (_timerIsRunning.value) {
                        checkTimerStatus(now)
                    }
                    if (_isFocusRunning.value) {
                        checkFocusStatus(now)
                    }
                }

                val s = _settings.value
                val delayMs = when {
                    s.ecoPerformanceMode -> 1000L
                    s.smoothSweepSeconds && _currentScreen.value == ScreenType.CLOCK -> 125L
                    else -> (1000L - (now % 1000L)).coerceIn(250L, 1000L)
                }
                delay(delayMs)
            }
        }
    }

    private fun todayKey(): String {
        val c = Calendar.getInstance()
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH) + 1}-${c.get(Calendar.DAY_OF_MONTH)}"
    }

    // Navigation & Swipe
    fun navigateTo(screen: ScreenType) {
        if (screen != _currentScreen.value) {
            screenHistory.add(screen)
            _currentScreen.value = screen
        }
        _isFeatureHubOpen.value = false
    }

    fun goBack(): Boolean {
        if (_isFeatureHubOpen.value) {
            _isFeatureHubOpen.value = false
            return true
        }
        if (screenHistory.size > 1) {
            screenHistory.removeAt(screenHistory.lastIndex)
            _currentScreen.value = screenHistory.last()
            return true
        }
        if (_currentScreen.value != ScreenType.CLOCK) {
            _currentScreen.value = ScreenType.CLOCK
            return true
        }
        return false
    }

    fun onSwipeLeft() {
        val cur = _currentScreen.value
        if (cur == ScreenType.WEATHER || cur == ScreenType.SETTINGS || cur == ScreenType.CALCULATOR) return
        val idx = mainSwipeOrder.indexOf(cur)
        if (idx != -1 && idx < mainSwipeOrder.lastIndex) {
            navigateTo(mainSwipeOrder[idx + 1])
        }
    }

    fun onSwipeRight() {
        val cur = _currentScreen.value
        if (cur == ScreenType.WEATHER || cur == ScreenType.SETTINGS || cur == ScreenType.PRODUCTIVITY || cur == ScreenType.CALCULATOR) {
            goBack()
            return
        }
        val idx = mainSwipeOrder.indexOf(cur)
        if (idx > 0) {
            navigateTo(mainSwipeOrder[idx - 1])
        } else if (screenHistory.size > 1) {
            goBack()
        }
    }

    fun setFeatureHubOpen(open: Boolean) {
        _isFeatureHubOpen.value = open
    }

    fun selectLifePillar(pillarId: String) {
        _selectedLifePillar.value = pillarId
    }

    fun openLifePillarFromHub(pillarId: String) {
        _selectedLifePillar.value = pillarId
        navigateTo(ScreenType.CLOCK)
    }

    // Settings
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val prevLang = _settings.value.appLanguage
        val prevFocusDef = _settings.value.defaultFocusMinutes
        val next = transform(_settings.value)
        _settings.value = next
        stateDirty = true
        if (next.defaultFocusMinutes != prevFocusDef && !_isFocusRunning.value) {
            val secs = next.defaultFocusMinutes * 60
            _focusTotalSeconds.value = secs
            _focusRemainingSeconds.value = secs
        }
        if (next.appLanguage != prevLang && _chatMessages.value.size == 1 && !_chatMessages.value[0].isUser) {
            _chatMessages.value = listOf(
                ChatMsg(
                    text = ChronosTranslations.t(next.appLanguage, "ai_greeting"),
                    isUser = false
                )
            )
        }
        recomputeAiInsights()
        savePersistedState()
    }

    fun resetSettingsToDefault() {
        _settings.value = AppSettings()
        stateDirty = true
        val secs = _settings.value.defaultFocusMinutes * 60
        if (!_isFocusRunning.value) {
            _focusTotalSeconds.value = secs
            _focusRemainingSeconds.value = secs
        }
        recomputeAiInsights()
        savePersistedState()
    }

    // Live Network Connectivity Telemetry
    fun checkLiveNetworkStatus(): Boolean {
        return try {
            val app = getApplication<Application>()
            val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val net = cm?.activeNetwork
            val caps = if (net != null) cm.getNetworkCapabilities(net) else null
            val hasInternet = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            _isNetworkOnline.value = hasInternet
            _networkTransportLabel.value = when {
                !hasInternet -> "OFFLINE • MODE LOKAL"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "ONLINE • WIFI"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "ONLINE • SELULER"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ONLINE • ETHERNET"
                else -> "ONLINE • AKTIF"
            }
            hasInternet
        } catch (_: Throwable) {
            _isNetworkOnline.value = true
            _networkTransportLabel.value = "ONLINE • AKTIF"
            true
        }
    }

    // Device GPS Coordinate Weather & Coastal Telemetry Detection
    fun detectDeviceGpsWeather(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        val app = getApplication<Application>()
        val isId = _settings.value.appLanguage == "id"
        val hasFine = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            onResult(
                false,
                if (isId) "Izin Lokasi GPS belum diaktifkan. Silakan izinkan akses lokasi perangkat."
                else "Device location permission not granted. Please allow location access."
            )
            return
        }
        if (!checkLiveNetworkStatus()) {
            onResult(
                false,
                if (isId) "Fitur Cuaca GPS membutuhkan koneksi internet aktif untuk mengambil data satelit realtime."
                else "GPS Weather requires an active internet connection for real-time satellite data."
            )
            return
        }

        viewModelScope.launch {
            _isLoadingWeather.value = true
            try {
                val lm = app.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                var bestLoc: Location? = null
                if (lm != null) {
                    val providers = listOf(
                        LocationManager.GPS_PROVIDER,
                        LocationManager.NETWORK_PROVIDER,
                        LocationManager.PASSIVE_PROVIDER
                    )
                    for (p in providers) {
                        try {
                            val loc = lm.getLastKnownLocation(p)
                            if (loc != null) {
                                if (bestLoc == null || loc.time > bestLoc.time) {
                                    bestLoc = loc
                                }
                            }
                        } catch (_: SecurityException) {
                        } catch (_: Throwable) {
                        }
                    }
                }

                val lat = bestLoc?.latitude ?: -6.2088
                val lon = bestLoc?.longitude ?: 106.8456
                val nearestCity = ALL_AVAILABLE_CITIES.minByOrNull { c ->
                    val dLat = c.lat - lat
                    val dLon = c.lon - lon
                    dLat * dLat + dLon * dLon
                }
                val distDeg = if (nearestCity != null) {
                    kotlin.math.hypot(nearestCity.lat - lat, nearestCity.lon - lon)
                } else 999.0

                val labelName = if (bestLoc == null) {
                    "GPS Jakarta, Indonesia"
                } else if (nearestCity != null && distDeg < 0.85) {
                    "GPS ${nearestCity.name} (${"%.2f".format(Locale.US, lat)}, ${"%.2f".format(Locale.US, lon)})"
                } else {
                    "GPS Koordinat (${"%.2f".format(Locale.US, lat)}, ${"%.2f".format(Locale.US, lon)})"
                }

                val data = WeatherService.fetchWeatherByCoordinates(
                    lat = lat,
                    lon = lon,
                    locationName = labelName,
                    forceRefresh = true
                )
                _currentWeatherData.value = data
                val existingIdx = _citiesWeatherData.value.indexOfFirst {
                    it.locationName.startsWith("GPS ", ignoreCase = true) ||
                        it.locationName.equals(labelName, ignoreCase = true)
                }
                if (existingIdx >= 0) {
                    val mutable = _citiesWeatherData.value.toMutableList()
                    mutable[existingIdx] = data
                    _citiesWeatherData.value = mutable
                } else {
                    _citiesWeatherData.value = listOf(data) + _citiesWeatherData.value
                }
                if (_savedWeatherCities.value.none { it.equals(labelName, ignoreCase = true) }) {
                    _savedWeatherCities.value = (listOf(labelName) + _savedWeatherCities.value).take(10)
                }
                stateDirty = true
                onResult(
                    true,
                    if (isId) "✓ GPS Perangkat Terkunci: $labelName (${data.temp.roundToInt()}°C • ${data.source.uppercase()})"
                    else "✓ Device GPS Locked: $labelName (${data.temp.roundToInt()}°C • ${data.source.uppercase()})"
                )
            } catch (e: Throwable) {
                onResult(
                    false,
                    if (isId) "Gagal membaca sensor GPS: ${e.localizedMessage ?: "Coba lagi"}"
                    else "Failed to read GPS sensor: ${e.localizedMessage ?: "Try again"}"
                )
            } finally {
                _isLoadingWeather.value = false
            }
        }
    }

    // Weather
    fun refreshPrimaryWeather(force: Boolean = true) {
        viewModelScope.launch {
            _isLoadingWeather.value = true
            val primaryCity = _savedWeatherCities.value.firstOrNull()
                ?: _settings.value.savedCities.firstOrNull()
                ?: "Jakarta"
            val data = WeatherService.fetchWeatherByCity(primaryCity, forceRefresh = force)
            _currentWeatherData.value = data
            _isLoadingWeather.value = false
        }
    }

    fun loadAllSavedCitiesWeather() {
        viewModelScope.launch {
            _isLoadingWeather.value = true
            val list = mutableListOf<WeatherData>()
            for (city in _savedWeatherCities.value) {
                list.add(WeatherService.fetchWeatherByCity(city, forceRefresh = true))
            }
            _citiesWeatherData.value = list
            if (list.isNotEmpty()) {
                _currentWeatherData.value = list.first()
            }
            _isLoadingWeather.value = false
        }
    }

    fun addWeatherCity(cityQuery: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        val clean = cityQuery.trim()
        if (clean.isEmpty()) return
        val isId = _settings.value.appLanguage == "id"
        if (_savedWeatherCities.value.size >= 10) {
            onResult(false, if (isId) "Maksimal 10 kota" else "Max 10 cities")
            return
        }
        viewModelScope.launch {
            val data = WeatherService.fetchWeatherByCity(clean, forceRefresh = true)
            val baseName = data.locationName.split(",").firstOrNull()?.trim().orEmpty()
            val exists = _savedWeatherCities.value.any {
                val savedBase = it.split(",").firstOrNull()?.trim().orEmpty()
                it.equals(data.locationName, ignoreCase = true) ||
                    it.equals(clean, ignoreCase = true) ||
                    savedBase.equals(baseName, ignoreCase = true)
            }
            if (exists) {
                onResult(false, if (isId) "Kota sudah ada" else "City already added")
                return@launch
            }
            _savedWeatherCities.value = _savedWeatherCities.value + data.locationName
            _citiesWeatherData.value = _citiesWeatherData.value + data
            stateDirty = true
            onResult(true, null)
        }
    }

    fun removeWeatherCity(locationName: String) {
        val targetBase = locationName.split(",").firstOrNull()?.trim().orEmpty()
        _savedWeatherCities.value = _savedWeatherCities.value.filterNot {
            val savedBase = it.split(",").firstOrNull()?.trim().orEmpty()
            it.equals(locationName, ignoreCase = true) || savedBase.equals(targetBase, ignoreCase = true)
        }
        _citiesWeatherData.value = _citiesWeatherData.value.filterNot {
            val cityBase = it.locationName.split(",").firstOrNull()?.trim().orEmpty()
            it.locationName.equals(locationName, ignoreCase = true) || cityBase.equals(targetBase, ignoreCase = true)
        }
        if (_citiesWeatherData.value.isNotEmpty()) {
            _currentWeatherData.value = _citiesWeatherData.value.first()
        }
        stateDirty = true
    }

    // Alarms & Background Notification Engine
    fun syncAlarmsToBackgroundEngine() {
        val app = getApplication<Application>()
        val s = _settings.value
        ChronosAlarmScheduler.syncAlarmsWithSystem(
            context = app,
            alarms = _alarms.value,
            notificationsEnabled = s.notificationsEnabled,
            isId = s.appLanguage != "en"
        )
    }

    fun triggerBackgroundAlarmNotificationPreview() {
        val app = getApplication<Application>()
        val isId = _settings.value.appLanguage != "en"
        syncAlarmsToBackgroundEngine()
        val now = System.currentTimeMillis()
        val nextAlarm = _alarms.value
            .filter { it.isEnabled }
            .minByOrNull { getNextAlarmOccurrenceMillis(it, now) ?: Long.MAX_VALUE }

        if (nextAlarm != null) {
            val targetMs = getNextAlarmOccurrenceMillis(nextAlarm, now) ?: (now + 3600_000L)
            ChronosAlarmScheduler.postOrUpdateStandbyCountdownNotification(
                context = app,
                nextAlarm = nextAlarm,
                targetMillis = targetMs,
                totalActiveCount = _alarms.value.count { it.isEnabled },
                isId = isId
            )
            val cleanTitle = nextAlarm.label.split("|||").firstOrNull().orEmpty().ifBlank { "Alarm" }
            val rem = formatAlarmRemaining(nextAlarm, now)
            val targetStr = formatAlarmTargetDateTime(nextAlarm, now, isId)
            showBannerAlert(
                ActiveBannerAlert(
                    kind = "alarm",
                    title = if (isId) "Notifikasi Latar Belakang Aktif • $cleanTitle" else "Background Notification Armed • $cleanTitle",
                    meta = "$targetStr ($rem)"
                )
            )
        } else {
            showBannerAlert(
                ActiveBannerAlert(
                    kind = "alarm",
                    title = if (isId) "Tidak ada alarm aktif. Aktifkan minimal 1 alarm." else "No active alarms. Enable at least 1 alarm.",
                    meta = "STANDBY"
                )
            )
        }
    }

    private fun buildFireKey(alarmId: Long, y: Int, mo: Int, d: Int, h: Int, m: Int): String {
        return "${alarmId}_${y}-${mo}-${d}_${h}:${m}"
    }

    private fun markAlarmFiredForCal(alarmId: Long, cal: Calendar, h: Int, m: Int) {
        val y = cal.get(Calendar.YEAR)
        val mo = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        firedAlarmKeys[alarmId] = buildFireKey(alarmId, y, mo, d, h, m)
    }

    private fun checkAlarms(nowMillis: Long) {
        if (_ringingAlarm.value != null) return
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)
        if (h == 0 && m == 0 && s <= 1) {
            firedAlarmKeys.clear()
            firedPreWakeKeys.clear()
        }
        val y = cal.get(Calendar.YEAR)
        val mo = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val app = getApplication<Application>()
        val isId = _settings.value.appLanguage != "en"

        for (alarm in _alarms.value) {
            if (!alarm.isEnabled) continue

            // Check 10-minute / 5-minute gentle Pre-Wake window using exact next occurrence (handles midnight wrap-around)
            if (alarm.preWakeGentle) {
                val nextOccMs = getNextAlarmOccurrenceMillis(alarm, nowMillis)
                if (nextOccMs != null) {
                    val diffSec = (nextOccMs - nowMillis) / 1000L
                    val remMins = when {
                        diffSec in (9 * 60 + 45)..(10 * 60 + 10) -> 10
                        diffSec in (4 * 60 + 45)..(5 * 60 + 10) -> 5
                        else -> 0
                    }
                    if (remMins > 0) {
                        val preKey = "pre_${alarm.id}_${y}-${mo}-${d}_${alarm.hour}:${alarm.minute}_${remMins}m"
                        if (!firedPreWakeKeys.contains(preKey)) {
                            firedPreWakeKeys.add(preKey)
                            val cleanTitle = alarm.label.split("|||").firstOrNull().orEmpty().ifBlank { "Alarm" }
                            if (_settings.value.notificationsEnabled) {
                                ChronosAlarmScheduler.postPreWakeNotification(app, alarm, isId, remMins)
                            }
                            ChronosAudioSynthesizer.playAlarmChime(ringtoneIndex = 3, durationSeconds = 3, fadeInSeconds = 2)
                            showBannerAlert(
                                ActiveBannerAlert(
                                    kind = "pre_wake",
                                    title = if (isId) "Pra-Bangun Cerdas: $cleanTitle dalam $remMins menit" else "Smart Pre-Wake: $cleanTitle in $remMins mins",
                                    meta = "%02d:%02d".format(alarm.hour, alarm.minute)
                                )
                            )
                        }
                    }
                }
            }

            val matchesDateOrDay = when {
                alarm.dateMillis > 0L -> {
                    if (nowMillis + 1000L < alarm.dateMillis) false
                    else {
                        val target = Calendar.getInstance().apply { timeInMillis = alarm.dateMillis }
                        target.get(Calendar.YEAR) == y &&
                            target.get(Calendar.MONTH) + 1 == mo &&
                            target.get(Calendar.DAY_OF_MONTH) == d
                    }
                }
                alarm.daysOfWeek > 0 -> {
                    val mask = getDayOfWeekMask(cal)
                    (alarm.daysOfWeek and mask) != 0
                }
                else -> true
            }
            if (!matchesDateOrDay) continue

            if (alarm.hour != h || alarm.minute != m) continue
            val fireKey = buildFireKey(alarm.id, y, mo, d, h, m)
            if (firedAlarmKeys[alarm.id] == fireKey) continue

            firedAlarmKeys[alarm.id] = fireKey
            _ringingAlarm.value = alarm
            if (_settings.value.notificationsEnabled) {
                ChronosAlarmScheduler.postRingingAlarmNotification(app, alarm, isId)
            }
            break
        }
    }

    fun addAlarm(alarm: AlarmItem) {
        val now = System.currentTimeMillis()
        val withId = if (alarm.id <= 0L) alarm.copy(id = now) else alarm
        ChronosAlarmScheduler.clearSkippedOccurrence(withId.id)
        // If added for the current minute while already inside that minute, prevent instant false-trigger until next cycle
        if (withId.isEnabled && withId.dateMillis == 0L) {
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            if (cal.get(Calendar.HOUR_OF_DAY) == withId.hour && cal.get(Calendar.MINUTE) == withId.minute) {
                markAlarmFiredForCal(withId.id, cal, withId.hour, withId.minute)
            }
        }
        _alarms.value = _alarms.value + withId
        stateDirty = true
        savePersistedState()
        syncAlarmsToBackgroundEngine()

        if (withId.isEnabled) {
            val isId = _settings.value.appLanguage != "en"
            val cleanTitle = withId.label.split("|||").firstOrNull().orEmpty().ifBlank { "Alarm" }
            val rem = formatAlarmRemaining(withId, now)
            showBannerAlert(
                ActiveBannerAlert(
                    kind = "alarm",
                    title = if (isId) "Alarm Disetel (%02d:%02d) • %s".format(withId.hour, withId.minute, cleanTitle)
                    else "Alarm Armed (%02d:%02d) • %s".format(withId.hour, withId.minute, cleanTitle),
                    meta = if (isId) "Aktif Latar Belakang • $rem" else "Background Armed • $rem"
                )
            )
        }
    }

    fun updateAlarm(updated: AlarmItem) {
        val now = System.currentTimeMillis()
        ChronosAlarmScheduler.clearSkippedOccurrence(updated.id)
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        if (updated.isEnabled && updated.dateMillis == 0L &&
            cal.get(Calendar.HOUR_OF_DAY) == updated.hour && cal.get(Calendar.MINUTE) == updated.minute
        ) {
            // Keep current minute marked so enabling/editing an alarm during its own minute schedules for tomorrow (+24h)
            markAlarmFiredForCal(updated.id, cal, updated.hour, updated.minute)
        } else {
            firedAlarmKeys.remove(updated.id)
        }
        _alarms.value = _alarms.value.map { if (it.id == updated.id) updated else it }
        stateDirty = true
        savePersistedState()
        syncAlarmsToBackgroundEngine()

        if (updated.isEnabled) {
            val isId = _settings.value.appLanguage != "en"
            val cleanTitle = updated.label.split("|||").firstOrNull().orEmpty().ifBlank { "Alarm" }
            val rem = formatAlarmRemaining(updated, now)
            showBannerAlert(
                ActiveBannerAlert(
                    kind = "alarm",
                    title = if (isId) "Alarm Aktif (%02d:%02d) • %s".format(updated.hour, updated.minute, cleanTitle)
                    else "Alarm Active (%02d:%02d) • %s".format(updated.hour, updated.minute, cleanTitle),
                    meta = if (isId) "Siaga Latar Belakang • $rem" else "Background Ready • $rem"
                )
            )
        }
    }

    fun updateAlarmTheme(id: Long, themeId: String) {
        val target = _alarms.value.find { it.id == id } ?: return
        val updated = target.copy(backgroundTheme = themeId)
        _alarms.value = _alarms.value.map { if (it.id == id) updated else it }
        stateDirty = true
        savePersistedState()
        syncAlarmsToBackgroundEngine()
    }

    fun deleteAlarm(id: Long) {
        firedAlarmKeys.remove(id)
        ChronosAlarmScheduler.clearSkippedOccurrence(id)
        _alarms.value = _alarms.value.filterNot { it.id == id }
        stateDirty = true
        savePersistedState()
        syncAlarmsToBackgroundEngine()
    }

    fun restoreAlarms(newAlarms: List<AlarmItem>) {
        _alarms.value = newAlarms
        stateDirty = true
        savePersistedState()
        syncAlarmsToBackgroundEngine()
    }

    fun triggerTestAlarmRing(alarm: AlarmItem) {
        val cal = Calendar.getInstance()
        markAlarmFiredForCal(alarm.id, cal, alarm.hour, alarm.minute)
        _ringingAlarm.value = alarm
        if (_settings.value.notificationsEnabled) {
            ChronosAlarmScheduler.postRingingAlarmNotification(
                context = getApplication(),
                alarm = alarm,
                isId = _settings.value.appLanguage != "en"
            )
        }
    }

    fun dismissRingingAlarm() {
        val current = _ringingAlarm.value ?: return
        val app = getApplication<Application>()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        markAlarmFiredForCal(current.id, cal, current.hour, current.minute)
        markAlarmFiredForCal(current.id, cal, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

        ChronosAudioSynthesizer.stopChime()
        ChronosAlarmScheduler.clearBackgroundRingingState(app)
        if (current.label.startsWith("Snooze:")) {
            _alarms.value = _alarms.value.filterNot { it.id == current.id }
        } else if (current.daysOfWeek == 0 || current.dateMillis > 0L) {
            _alarms.value = _alarms.value.map { if (it.id == current.id) current.copy(isEnabled = false) else it }
        }
        _ringingAlarm.value = null
        stateDirty = true
        savePersistedState()
        syncAlarmsToBackgroundEngine()
    }

    fun snoozeRingingAlarm() {
        val current = _ringingAlarm.value ?: return
        val app = getApplication<Application>()
        val now = System.currentTimeMillis()
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        markAlarmFiredForCal(current.id, nowCal, current.hour, current.minute)
        markAlarmFiredForCal(current.id, nowCal, nowCal.get(Calendar.HOUR_OF_DAY), nowCal.get(Calendar.MINUTE))

        ChronosAudioSynthesizer.stopChime()
        ChronosAlarmScheduler.clearBackgroundRingingState(app)
        val snoozeMins = _settings.value.snoozeDuration.coerceAtLeast(1)
        val cal = Calendar.getInstance().apply {
            timeInMillis = now + (snoozeMins * 60_000L)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val snoozeTimeMs = cal.timeInMillis
        val baseTitle = current.label.split("|||").firstOrNull().orEmpty().removePrefix("Snooze: ")
        val snoozeAlarm = AlarmItem(
            id = now,
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
            label = "Snooze: $baseTitle",
            daysOfWeek = 0,
            dateMillis = snoozeTimeMs,
            isVibrate = current.isVibrate,
            soundUri = current.soundUri,
            customAudioTitle = current.customAudioTitle,
            isEnabled = true,
            isSnooze = true,
            wakeMission = current.wakeMission,
            fadeInSeconds = current.fadeInSeconds,
            preWakeGentle = false
        )
        val filtered = if (current.label.startsWith("Snooze:")) {
            _alarms.value.filterNot { it.id == current.id }
        } else if (current.daysOfWeek == 0 || current.dateMillis > 0L) {
            _alarms.value.map { if (it.id == current.id) current.copy(isEnabled = false) else it }
        } else {
            _alarms.value
        }
        _alarms.value = filtered + snoozeAlarm
        _ringingAlarm.value = null
        stateDirty = true
        savePersistedState()
        syncAlarmsToBackgroundEngine()

        val isId = _settings.value.appLanguage != "en"
        showBannerAlert(
            ActiveBannerAlert(
                kind = "alarm",
                title = if (isId) "Snooze Aktif: $baseTitle (%02d:%02d)".format(snoozeAlarm.hour, snoozeAlarm.minute)
                else "Snoozed: $baseTitle (%02d:%02d)".format(snoozeAlarm.hour, snoozeAlarm.minute),
                meta = if (isId) "+$snoozeMins menit di latar belakang" else "+$snoozeMins mins in background"
            )
        )
    }

    private fun triggerAutoSoundscapeIfConfigured() {
        val sound = _settings.value.timerAutoSoundscape
        if (sound != "NONE" && sound.isNotBlank()) {
            ChronosAudioSynthesizer.playSoundscape(sound)
        }
    }

    private fun stopAutoSoundscapeIfConfigured() {
        if (_settings.value.timerAutoSoundscape != "NONE" && ChronosAudioSynthesizer.isPlayingSoundscape) {
            ChronosAudioSynthesizer.stopSoundscape()
        }
    }

    // Central Timer Engine
    private fun syncLiveTimerNotification() {
        val s = _settings.value
        ChronosAlarmScheduler.postOrCancelLiveTimerNotification(
            context = getApplication(),
            isRunning = _timerIsRunning.value && s.notificationsEnabled,
            endTimeMs = timerEndTimeMs,
            totalSeconds = _timerTotalSeconds.value,
            isFocusSession = false,
            isId = s.appLanguage != "en"
        )
    }

    private fun syncLiveFocusNotification() {
        val s = _settings.value
        ChronosAlarmScheduler.postOrCancelLiveTimerNotification(
            context = getApplication(),
            isRunning = _isFocusRunning.value && s.notificationsEnabled,
            endTimeMs = focusEndTimeMs,
            totalSeconds = _focusTotalSeconds.value,
            isFocusSession = true,
            isId = s.appLanguage != "en"
        )
    }

    fun startTimer(customSeconds: Int? = null) {
        val total = customSeconds ?: _timerRemainingSeconds.value
        if (total <= 0) return
        if (customSeconds != null) {
            _timerTotalSeconds.value = customSeconds
        }
        _timerRemainingSeconds.value = total
        _timerIsRunning.value = true
        _timerIsCompleted.value = false
        timerEndTimeMs = System.currentTimeMillis() + (total * 1000L)
        triggerAutoSoundscapeIfConfigured()
        syncLiveTimerNotification()
    }

    fun pauseTimer() {
        if (!_timerIsRunning.value) return
        val end = timerEndTimeMs
        if (end != null) {
            val rem = max(0, ceil((end - System.currentTimeMillis()) / 1000.0).toInt())
            _timerRemainingSeconds.value = rem
        }
        _timerIsRunning.value = false
        timerEndTimeMs = null
        stopAutoSoundscapeIfConfigured()
        syncLiveTimerNotification()
    }

    fun resumeTimer() {
        if (_timerIsRunning.value || _timerRemainingSeconds.value <= 0) return
        _timerIsRunning.value = true
        _timerIsCompleted.value = false
        timerEndTimeMs = System.currentTimeMillis() + (_timerRemainingSeconds.value * 1000L)
        triggerAutoSoundscapeIfConfigured()
        syncLiveTimerNotification()
    }

    fun resetTimer() {
        ChronosAudioSynthesizer.stopChime()
        stopAutoSoundscapeIfConfigured()
        _timerIsRunning.value = false
        timerEndTimeMs = null
        _timerRemainingSeconds.value = _timerTotalSeconds.value
        _timerIsCompleted.value = false
        syncLiveTimerNotification()
    }

    fun setTimerDuration(seconds: Int) {
        if (seconds <= 0) return
        _timerIsRunning.value = false
        timerEndTimeMs = null
        _timerTotalSeconds.value = seconds
        _timerRemainingSeconds.value = seconds
        _timerIsCompleted.value = false
        syncLiveTimerNotification()
    }

    fun dismissCompletedTimer() {
        ChronosAudioSynthesizer.stopChime()
        stopAutoSoundscapeIfConfigured()
        _timerIsCompleted.value = false
        _timerRemainingSeconds.value = _timerTotalSeconds.value
        syncLiveTimerNotification()
    }

    private fun checkTimerStatus(nowMillis: Long) {
        val end = timerEndTimeMs ?: return
        if (!_timerIsRunning.value) return
        val remaining = max(0, ceil((end - nowMillis) / 1000.0).toInt())
        if (remaining <= 0) {
            val addedMins = max(1, (_timerTotalSeconds.value / 60.0).roundToInt())
            _completedFocusSessions.value += 1
            _totalFocusMinutes.value += addedMins
            stateDirty = true
            recomputeAiInsights()

            val ringIdx = ChronosAudioSynthesizer.ringtoneIndexFromName(_settings.value.timerSound)
            ChronosAudioSynthesizer.playAlarmChime(ringtoneIndex = ringIdx, durationSeconds = 4)
            val isId = _settings.value.appLanguage == "id"

            if (_settings.value.timerAutoRepeat && _timerTotalSeconds.value > 0) {
                val nextTotal = _timerTotalSeconds.value
                _timerRemainingSeconds.value = nextTotal
                _timerIsRunning.value = true
                _timerIsCompleted.value = false
                timerEndTimeMs = nowMillis + (nextTotal * 1000L)
                syncLiveTimerNotification()
                showBannerAlert(
                    ActiveBannerAlert(
                        kind = "timer",
                        title = if (isId) "Putaran Timer Selesai • Mengulang Otomatis" else "Timer Cycle Done • Auto-Repeating",
                        meta = "%02d:%02d".format(
                            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                            Calendar.getInstance().get(Calendar.MINUTE)
                        )
                    )
                )
            } else {
                _timerRemainingSeconds.value = 0
                _timerIsRunning.value = false
                timerEndTimeMs = null
                _timerIsCompleted.value = true
                syncLiveTimerNotification()
                showBannerAlert(
                    ActiveBannerAlert(
                        kind = "timer",
                        title = if (isId) "Timer selesai" else "Timer finished",
                        meta = "%02d:%02d".format(
                            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                            Calendar.getInstance().get(Calendar.MINUTE)
                        )
                    )
                )
            }
        } else {
            _timerRemainingSeconds.value = remaining
        }
    }

    // Central Stopwatch Engine (Battery-Aware Adaptive Refresh)
    fun startStopwatch() {
        if (_stopwatchIsRunning.value) return
        stopwatchStartRealtime = SystemClock.elapsedRealtime()
        _stopwatchIsRunning.value = true
        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (isActive && _stopwatchIsRunning.value) {
                val cur = stopwatchAccumulatedMs + (SystemClock.elapsedRealtime() - stopwatchStartRealtime)
                _stopwatchElapsedMs.value = max(0L, cur)
                val s = _settings.value
                val stepDelay = when {
                    _currentScreen.value != ScreenType.STOPWATCH -> 500L
                    s.ecoPerformanceMode || !s.stopwatchShowMs -> 100L
                    else -> 33L
                }
                delay(stepDelay)
            }
        }
    }

    fun pauseStopwatch() {
        if (!_stopwatchIsRunning.value) return
        stopwatchAccumulatedMs += (SystemClock.elapsedRealtime() - stopwatchStartRealtime)
        _stopwatchElapsedMs.value = stopwatchAccumulatedMs
        _stopwatchIsRunning.value = false
        stopwatchJob?.cancel()
        stopwatchJob = null
    }

    fun lapStopwatch() {
        val cur = _stopwatchElapsedMs.value
        if (cur <= 0L) return
        _stopwatchLaps.value = listOf(cur) + _stopwatchLaps.value
    }

    fun resetStopwatch() {
        _stopwatchIsRunning.value = false
        stopwatchJob?.cancel()
        stopwatchJob = null
        stopwatchAccumulatedMs = 0L
        stopwatchStartRealtime = 0L
        _stopwatchElapsedMs.value = 0L
        _stopwatchLaps.value = emptyList()
    }

    fun saveStopwatchSession(record: StopwatchSessionRecord, syncToWorkout: Boolean) {
        _savedStopwatchSessions.value = (listOf(record) + _savedStopwatchSessions.value).take(25)
        val addedMins = max(1, (record.totalElapsedMs / 60_000.0).roundToInt())
        if (syncToWorkout) {
            updateLifeLog { it.copy(workoutMins = it.workoutMins + addedMins) }
        } else {
            _completedFocusSessions.value += 1
            _totalFocusMinutes.value += addedMins
        }
        recomputeAiInsights()
        stateDirty = true
    }

    fun clearStopwatchSessions() {
        _savedStopwatchSessions.value = emptyList()
        stateDirty = true
    }

    // Life Suite Actions
    fun updateLifeLog(transform: (LifeLog) -> LifeLog) {
        val cur = if (_lifeLog.value.day == todayKey()) _lifeLog.value else LifeLog(day = todayKey())
        _lifeLog.value = transform(cur)
        stateDirty = true
    }

    fun toggleLifeNotify() {
        val next = !_isLifeNotified.value
        _isLifeNotified.value = next
        val isId = _settings.value.appLanguage == "id"
        if (next) {
            if (_reminders.value.none { it.id == "life_suite_hydration" }) {
                addReminder(
                    ReminderItem(
                        id = "life_suite_hydration",
                        title = if (isId) "Hidrasi & Peregangan Life Suite" else "Life Suite Hydration & Stretch",
                        timeStr = "14:00",
                        recurrence = RecurrenceType.DAILY,
                        isEnabled = true,
                        category = "Health"
                    )
                )
            }
            showBannerAlert(
                ActiveBannerAlert(
                    kind = "reminder",
                    title = if (isId) "Notifikasi Cerdas Life Suite Aktif" else "Life Suite Smart Reminders Enabled",
                    meta = "14:00"
                )
            )
        }
        stateDirty = true
    }

    // Productivity Actions
    fun selectPeriod(period: PeriodFilter) {
        _selectedPeriod.value = period
    }

    fun addTask(task: TaskItem) {
        val combined = listOf(task) + _tasks.value
        _tasks.value = if (_settings.value.autoSortTasksByPriority) {
            combined.sortedWith(compareBy<TaskItem> { it.isCompleted }.thenBy { it.priority.ordinal })
        } else {
            combined
        }
        stateDirty = true
        recomputeAiInsights()
    }

    fun toggleTask(id: String) {
        val updated = _tasks.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        _tasks.value = if (_settings.value.autoSortTasksByPriority) {
            updated.sortedWith(compareBy<TaskItem> { it.isCompleted }.thenBy { it.priority.ordinal })
        } else {
            updated
        }
        stateDirty = true
        recomputeAiInsights()
    }

    fun deleteTask(id: String) {
        _tasks.value = _tasks.value.filterNot { it.id == id }
        stateDirty = true
        recomputeAiInsights()
    }

    fun addAgendaEvent(event: AgendaEvent) {
        _events.value = (_events.value + event).sortedBy { it.startTime }
        stateDirty = true
        recomputeAiInsights()
    }

    fun toggleAgendaEvent(id: String) {
        _events.value = _events.value.map {
            if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it
        }
        stateDirty = true
        recomputeAiInsights()
    }

    fun deleteAgendaEvent(id: String) {
        _events.value = _events.value.filterNot { it.id == id }
        stateDirty = true
        recomputeAiInsights()
    }

    fun addCalendarEvent(title: String, timeStr: String, desc: String, colorHex: String) {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
        }
        val newCalEv = CalendarEvent(
            id = System.currentTimeMillis().toString(),
            title = title,
            description = desc,
            startTime = cal.timeInMillis,
            color = colorHex
        )
        _calendarEvents.value = (_calendarEvents.value + newCalEv).sortedBy { it.startTime }
        stateDirty = true

        val endH = (h + 1) % 24
        addAgendaEvent(
            AgendaEvent(
                id = "pe_${System.currentTimeMillis()}",
                title = title,
                startTime = "%02d:%02d".format(h, m),
                endTime = "%02d:%02d".format(endH, m),
                location = "",
                description = desc,
                isCompleted = false,
                hasReminder = true,
                colorHex = colorHex,
                targetPeriod = PeriodFilter.TODAY,
                isSystemCalendar = true
            )
        )
    }

    fun removeCalendarEvent(id: String) {
        if (id.startsWith("prod_")) {
            val realId = id.removePrefix("prod_")
            deleteAgendaEvent(realId)
        } else {
            _calendarEvents.value = _calendarEvents.value.filterNot { it.id == id }
            stateDirty = true
        }
    }

    fun addHabit(habit: HabitItem) {
        _habits.value = _habits.value + habit
        stateDirty = true
        recomputeAiInsights()
    }

    fun incrementHabit(id: String) {
        _habits.value = _habits.value.map { h ->
            if (h.id != id) h else {
                val nextCount = h.currentCount + 1
                val isCompleted = nextCount >= h.targetCount
                val newStreak = if (isCompleted && !h.isCompletedToday) h.streakDays + 1 else h.streakDays
                h.copy(currentCount = nextCount, isCompletedToday = isCompleted, streakDays = newStreak)
            }
        }
        stateDirty = true
        recomputeAiInsights()
    }

    fun decrementHabit(id: String) {
        _habits.value = _habits.value.map { h ->
            if (h.id != id) h else {
                val nextCount = max(0, h.currentCount - 1)
                h.copy(currentCount = nextCount, isCompletedToday = nextCount >= h.targetCount)
            }
        }
        stateDirty = true
        recomputeAiInsights()
    }

    fun deleteHabit(id: String) {
        _habits.value = _habits.value.filterNot { it.id == id }
        stateDirty = true
        recomputeAiInsights()
    }

    fun addReminder(reminder: ReminderItem) {
        _reminders.value = _reminders.value + reminder
        stateDirty = true
    }

    fun toggleReminder(id: String) {
        _reminders.value = _reminders.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
        stateDirty = true
    }

    fun deleteReminder(id: String) {
        _reminders.value = _reminders.value.filterNot { it.id == id }
        stateDirty = true
    }

    fun addNote(note: NoteItem) {
        _notes.value = listOf(note) + _notes.value
        stateDirty = true
    }

    fun deleteNote(id: String) {
        _notes.value = _notes.value.filterNot { it.id == id }
        stateDirty = true
    }

    fun clearCompletedProductivityItems() {
        _tasks.value = _tasks.value.filterNot { it.isCompleted }
        _events.value = _events.value.filterNot { it.isCompleted }
        stateDirty = true
        recomputeAiInsights()
    }

    // Productivity Focus Timer
    fun toggleProductivityFocus() {
        if (_isFocusRunning.value) {
            val end = focusEndTimeMs
            if (end != null) {
                _focusRemainingSeconds.value = max(0, ceil((end - System.currentTimeMillis()) / 1000.0).toInt())
            }
            focusEndTimeMs = null
            _isFocusRunning.value = false
            stopAutoSoundscapeIfConfigured()
            syncLiveFocusNotification()
        } else {
            val secs = if (_focusRemainingSeconds.value > 0) _focusRemainingSeconds.value else _focusTotalSeconds.value
            focusEndTimeMs = System.currentTimeMillis() + secs * 1000L
            _focusRemainingSeconds.value = secs
            _isFocusRunning.value = true
            triggerAutoSoundscapeIfConfigured()
            syncLiveFocusNotification()
        }
    }

    fun resetProductivityFocus() {
        _isFocusRunning.value = false
        focusEndTimeMs = null
        _focusRemainingSeconds.value = _focusTotalSeconds.value
        stopAutoSoundscapeIfConfigured()
        syncLiveFocusNotification()
    }

    fun selectProductivityFocusDuration(mins: Int) {
        _isFocusRunning.value = false
        focusEndTimeMs = null
        val secs = mins * 60
        _focusTotalSeconds.value = secs
        _focusRemainingSeconds.value = secs
        syncLiveFocusNotification()
    }

    private fun checkFocusStatus(nowMillis: Long) {
        val end = focusEndTimeMs ?: return
        if (!_isFocusRunning.value) return
        val remaining = max(0, ceil((end - nowMillis) / 1000.0).toInt())
        if (remaining <= 0) {
            val minsDone = max(1, (_focusTotalSeconds.value / 60.0).roundToInt())
            _isFocusRunning.value = false
            focusEndTimeMs = null
            _completedFocusSessions.value += 1
            _totalFocusMinutes.value += minsDone
            _focusRemainingSeconds.value = _focusTotalSeconds.value
            stateDirty = true
            syncLiveFocusNotification()
            ChronosAudioSynthesizer.playAlarmChime(4, 3)
            recomputeAiInsights()
            val isId = _settings.value.appLanguage != "en"
            showBannerAlert(
                ActiveBannerAlert(
                    kind = "focus",
                    title = if (isId) "Sesi Fokus Mendalam Selesai (+$minsDone mnt)" else "Deep Focus Session Complete (+$minsDone min)",
                    meta = "%02d:%02d".format(
                        Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                        Calendar.getInstance().get(Calendar.MINUTE)
                    )
                )
            )
        } else {
            _focusRemainingSeconds.value = remaining
        }
    }

    private fun checkReminders(nowMillis: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val y = cal.get(Calendar.YEAR)
        val mo = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)

        for (r in _reminders.value) {
            if (!r.isEnabled) continue
            val parts = r.timeStr.split(":")
            val th = parts.getOrNull(0)?.toIntOrNull() ?: continue
            val tm = parts.getOrNull(1)?.toIntOrNull() ?: continue
            if (h != th || m != tm) continue
            val key = "${r.id}_${y}-${mo}-${d}_${h}:${m}"
            if (firedReminderKeys.contains(key)) continue
            firedReminderKeys.add(key)
            ChronosAudioSynthesizer.playAlarmChime(1, 2)
            showBannerAlert(
                ActiveBannerAlert(
                    kind = "reminder",
                    title = r.title,
                    meta = r.timeStr
                )
            )
        }
    }

    fun showBannerAlert(alert: ActiveBannerAlert) {
        _activeBanner.value = alert
        if (_settings.value.notificationsEnabled && (alert.kind == "timer" || alert.kind == "reminder" || alert.kind == "focus" || alert.kind == "weather")) {
            val targetRoute = when (alert.kind) {
                "timer" -> "timers"
                "focus", "reminder" -> "productivity"
                "weather" -> "weather"
                else -> "alarms"
            }
            ChronosAlarmScheduler.postSmartAlertNotification(
                context = getApplication(),
                title = alert.title,
                body = "${alert.kind.uppercase()} • ${alert.meta}",
                targetScreen = targetRoute
            )
        }
        viewModelScope.launch {
            delay(7000L)
            if (_activeBanner.value == alert) {
                _activeBanner.value = null
            }
        }
    }

    fun dismissBannerAlert() {
        _activeBanner.value = null
    }

    // Productivity Insights
    fun recomputeAiInsights() {
        val isId = _settings.value.appLanguage == "id"
        val currentTasks = _tasks.value
        val currentEvents = _events.value
        val currentHabits = _habits.value
        val focusMins = _totalFocusMinutes.value

        val insights = mutableListOf<String>()
        val pendingTasks = currentTasks.filter { !it.isCompleted }
        val highPri = pendingTasks.filter { it.priority == TaskPriority.HIGH }

        if (pendingTasks.isEmpty()) {
            insights.add(
                if (isId) "FAKTA: Tidak ada tugas tertunda saat ini. Anda dapat membuat target baru kapan saja."
                else "FACT: No pending tasks for today. You can create new goals whenever ready."
            )
        } else {
            insights.add(
                if (isId) "FAKTA: ${pendingTasks.size} tugas tertunda hari ini (${highPri.size} prioritas tinggi)."
                else "FACT: ${pendingTasks.size} pending tasks remaining today (${highPri.size} high priority)."
            )
        }

        if (currentEvents.isEmpty()) {
            insights.add(
                if (isId) "FAKTA: Tidak ada agenda kegiatan yang dijadwalkan untuk hari ini."
                else "FACT: No agenda events scheduled for today."
            )
        } else {
            insights.add(
                if (isId) "FAKTA: Terjadwal ${currentEvents.size} agenda kegiatan pada kalender hari ini."
                else "FACT: ${currentEvents.size} agenda events scheduled on today's calendar."
            )
        }

        if (focusMins == 0) {
            insights.add(
                if (isId) "SARAN: Belum ada waktu fokus tercatat. Ketuk Mulai Fokus atau ikon timer pada tugas untuk memulai."
                else "SUGGESTION: No focus sessions recorded yet today. Tap Start Focus or the timer icon on a task."
            )
        } else {
            insights.add(
                if (isId) "FAKTA: Anda telah mencatatkan $focusMins menit fokus mendalam hari ini."
                else "FACT: You have achieved $focusMins minutes of deep focus today."
            )
        }

        val topHabit = currentHabits.filter { it.streakDays > 0 }.maxByOrNull { it.streakDays }
        if (topHabit != null) {
            insights.add(
                if (isId) "FAKTA: Kebiasaan '${topHabit.name}' mencatat rekor konsistensi ${topHabit.streakDays} hari berturut-turut."
                else "FACT: Habit '${topHabit.name}' has an active streak of ${topHabit.streakDays} consecutive days."
            )
        }

        _aiInsights.value = insights
    }

    fun runDeepProductivityAnalysis() {
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            delay(250L)
            val isId = _settings.value.appLanguage == "id"
            val aiModelName = when (_settings.value.aiModelIndex) {
                1 -> "CHRONOS Fast Turbo"
                2 -> "CHRONOS Deep Circadian"
                else -> "CHRONOS Neural v4.5"
            }
            val tasksLine = if (_tasks.value.isNotEmpty()) {
                _tasks.value.joinToString(", ") { "${it.title} (${it.priority})" }
            } else "No tasks"
            val eventsLine = if (_events.value.isNotEmpty()) {
                _events.value.joinToString(", ") { "${it.startTime} ${it.title}" }
            } else "No events"
            val focusLine = _totalFocusMinutes.value.toString()

            _aiInsights.value = if (isId) {
                listOf(
                    "FAKTA [$aiModelName]: Antrean tugas aktif ($tasksLine) & agenda ($eventsLine).",
                    "FAKTA: Waktu fokus terverifikasi hari ini $focusLine menit di perangkat lokal.",
                    "SARAN: Eksekusi tugas prioritas HIGH pada jendela fokus sirkadian pagi menggunakan Pomodoro 25 menit dengan audio Alpha 40Hz.",
                    "SARAN: Gunakan Template Kehidupan Harian & Smart Alarm Vitality Spirit untuk menjaga ritme produktivitas."
                )
            } else {
                listOf(
                    "FACT [$aiModelName]: Active task queue ($tasksLine) & agenda ($eventsLine).",
                    "FACT: Verified focus duration today is $focusLine minutes on local storage.",
                    "SUGGESTION: Tackle HIGH-priority items during your peak circadian alertness window with a 25m Pomodoro block.",
                    "SUGGESTION: Use Smart Daily Life Templates & Vitality Spirit Smart Alarm for consistent daily momentum."
                )
            }
            _isAiAnalyzing.value = false
        }
    }

    // AI Assistant Chat & Natural Language Command Execution (chronosAiChat port)
    fun sendAiChatMessage(userText: String, executeImmediately: Boolean = false) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        if (!executeImmediately) {
            _chatMessages.value = _chatMessages.value + ChatMsg(text = trimmed, isUser = true)
        }

        viewModelScope.launch {
            delay(160L)
            val isId = _settings.value.appLanguage == "id"
            val aiStyle = _settings.value.aiResponseStyle
            val aiModelName = when (_settings.value.aiModelIndex) {
                1 -> "CHRONOS Fast Turbo"
                2 -> "CHRONOS Deep Circadian"
                else -> "CHRONOS Neural v4.5"
            }
            val lower = trimmed.lowercase(Locale.ROOT)
            val weather = _currentWeatherData.value
            val weatherNote = if (weather != null) " [${weather.locationName}: ${weather.temp.roundToInt()}°C]" else ""

            var reply = if (isId) {
                "Halo! Saya $aiModelName (V$CHRONOS_VERSION). Anda dapat meminta saya menyetel Smart Alarm, memulai timer, mengontrol stopwatch, menambah tugas/pengingat, atau menganalisis cuaca & aktivitas harian."
            } else {
                "Hello! I am $aiModelName (V$CHRONOS_VERSION). Ask me to set a Smart Alarm, start a timer, control the stopwatch, add tasks/reminders, or analyze weather & daily life readiness."
            }

            if (lower.contains("alarm") || lower.contains("bangun")) {
                val timeRegex = Regex("""(\d{1,2})[:.](\d{2})""")
                val hourRegex = Regex("""jam\s*(\d{1,2})""")
                val m1 = timeRegex.find(lower)
                val m2 = hourRegex.find(lower)
                val hour = when {
                    m1 != null -> m1.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: 6
                    m2 != null -> m2.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: 6
                    else -> 6
                }
                val minute = if (m1 != null) m1.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 30 else 30

                reply = if (isId) {
                    "Siap! Smart Alarm telah disiapkan untuk pukul %02d:%02d lengkap dengan harmoni Vitality Spirit & pemantau siklus REM.".format(hour, minute)
                } else {
                    "Smart Alarm prepared for %02d:%02d with Vitality Spirit harmonics & REM cycle alignment.".format(hour, minute)
                }

                val summary = if (isId) {
                    "Pasang alarm %02d:%02d · CHRONOS Smart Alarm".format(hour, minute)
                } else {
                    "Set alarm %02d:%02d · CHRONOS Smart Alarm".format(hour, minute)
                }
                val runAction = {
                    addAlarm(
                        AlarmItem(
                            id = System.currentTimeMillis(),
                            hour = hour,
                            minute = minute,
                            label = "CHRONOS Smart Alarm",
                            daysOfWeek = 0,
                            dateMillis = 0L,
                            isVibrate = true,
                            soundUri = "AI Cosmos",
                            isEnabled = true,
                            isSnooze = true
                        )
                    )
                }
                if (executeImmediately) runAction() else _pendingAiAction.value = PendingAiAction("alarm", summary, runAction)
            } else if (lower.contains("timer") || executeImmediately) {
                if (lower.contains("stop") || lower.contains("pause") || lower.contains("berhenti") || lower.contains("jeda")) {
                    pauseTimer()
                    reply = if (isId) "Timer telah dijeda dengan presisi." else "Timer paused cleanly."
                } else if (lower.contains("reset") || lower.contains("ulang")) {
                    resetTimer()
                    reply = if (isId) "Timer telah direset ke durasi awal." else "Timer reset to initial duration."
                } else {
                    val durRegex = Regex("""(\d+)\s*(menit|min|minute|detik|sec|second|jam|hour)""")
                    val m = durRegex.find(lower)
                    val secs = if (m != null) {
                        val v = m.groupValues[1].toIntOrNull() ?: 5
                        val unit = m.groupValues[2]
                        when {
                            unit.startsWith("detik") || unit.startsWith("sec") -> v
                            unit.startsWith("jam") || unit.startsWith("hour") -> v * 3600
                            else -> v * 60
                        }
                    } else 300

                    reply = if (isId) {
                        "Timer presisi untuk ${if (secs >= 60) "${secs / 60} menit" else "$secs detik"} telah dimulai!"
                    } else {
                        "Precision timer for ${if (secs >= 60) "${secs / 60} minutes" else "$secs seconds"} has started!"
                    }
                    val runTimer = {
                        setTimerDuration(secs)
                        startTimer(secs)
                    }
                    if (executeImmediately) {
                        runTimer()
                    } else {
                        _pendingAiAction.value = PendingAiAction(
                            kind = "timer",
                            summary = if (isId) "Mulai timer $secs detik" else "Start a ${secs}s timer",
                            execute = runTimer
                        )
                    }
                }
            } else if (lower.contains("stopwatch")) {
                when {
                    lower.contains("mulai") || lower.contains("start") -> {
                        startStopwatch()
                        reply = if (isId) "Stopwatch presisi tinggi dimulai!" else "High-precision stopwatch started!"
                    }
                    lower.contains("berhenti") || lower.contains("stop") || lower.contains("pause") || lower.contains("jeda") -> {
                        pauseStopwatch()
                        reply = if (isId) "Stopwatch dihentikan." else "Stopwatch paused."
                    }
                    lower.contains("lap") || lower.contains("putaran") -> {
                        lapStopwatch()
                        reply = if (isId) "Putaran (Lap) baru berhasil dicatat!" else "New stopwatch lap recorded!"
                    }
                    lower.contains("reset") || lower.contains("ulang") -> {
                        resetStopwatch()
                        reply = if (isId) "Stopwatch telah direset ke 00:00.00." else "Stopwatch reset to 00:00.00."
                    }
                }
            } else if (lower.contains("tugas") || lower.contains("task") || lower.contains("todo")) {
                val cleanTitle = trimmed.replace(Regex("""^(tambahkan|buat|add|create|bikin)\s*(tugas|task|todo)?[:\s]*""", RegexOption.IGNORE_CASE), "")
                    .trim()
                    .ifEmpty { if (isId) "Tugas Prioritas" else "Priority Task" }
                addTask(
                    TaskItem(
                        id = System.currentTimeMillis().toString(),
                        title = cleanTitle,
                        description = "Dibuat otomatis oleh Asisten AI CHRONOS",
                        priority = TaskPriority.HIGH
                    )
                )
                reply = if (isId) {
                    "Tugas \"$cleanTitle\" siap ditambahkan ke Dasbor Produktivitas Anda!"
                } else {
                    "Task \"$cleanTitle\" has been added to your Productivity Dashboard!"
                }
            } else if (lower.contains("ingatkan") || lower.contains("remind")) {
                val timeRegex = Regex("""(\d{1,2})[:.](\d{2})""")
                val m = timeRegex.find(lower)
                val timeStr = if (m != null) {
                    "%02d:%02d".format(m.groupValues[1].toIntOrNull() ?: 12, m.groupValues[2].toIntOrNull() ?: 0)
                } else "12:00"
                val cleanTitle = trimmed.replace(Regex("""^(ingatkan|remind\s*me\s*to)?[:\s]*""", RegexOption.IGNORE_CASE), "")
                    .trim()
                    .ifEmpty { if (isId) "Pengingat CHRONOS" else "CHRONOS Reminder" }

                reply = if (isId) {
                    "Pengingat \"$cleanTitle\" pada pukul $timeStr telah dijadwalkan!"
                } else {
                    "Reminder \"$cleanTitle\" scheduled for $timeStr!"
                }
                _pendingAiAction.value = PendingAiAction(
                    kind = "reminder",
                    summary = if (isId) "Simpan pengingat $timeStr · $cleanTitle" else "Save reminder $timeStr · $cleanTitle",
                    execute = {
                        addReminder(
                            ReminderItem(
                                id = System.currentTimeMillis().toString(),
                                title = cleanTitle,
                                timeStr = timeStr
                            )
                        )
                    }
                )
            } else {
                // Online Intelligence Query (Weather, Sleep, Productivity Analysis, or General Question)
                val onlineNow = checkLiveNetworkStatus()
                if (!onlineNow) {
                    reply = if (isId) {
                        "⚠️ Mode Offline Terdeteksi: Fitur Asisten AI Cloud & Telemetri Cuaca membutuhkan koneksi internet aktif agar memberikan data akurat terkini (bukan data fiksi).\n\nNamun Anda tetap dapat menggunakan perintah Offline Dasar sekarang:\n• \"Pasang alarm jam 06:00\"\n• \"Mulai timer 15 menit\"\n• \"Mulai stopwatch\"\n• \"Tambah tugas Evaluasi Proyek\""
                    } else {
                        "⚠️ Offline Mode Detected: Cloud AI Assistant & Live Weather Telemetry require an active internet connection for real-time factual accuracy.\n\nYou can still use Offline Core hardware commands right now:\n• \"Set alarm for 06:00\"\n• \"Start timer 15 minutes\"\n• \"Start stopwatch\"\n• \"Add task Project Review\""
                    }
                } else {
                    val liveWeather = WeatherService.fetchWeatherByCity(
                        _savedWeatherCities.value.firstOrNull() ?: "Jakarta",
                        forceRefresh = false
                    )
                    _currentWeatherData.value = liveWeather
                    val geminiReply = callOnlineGeminiIfConfigured(
                        userPrompt = trimmed,
                        weather = liveWeather,
                        isId = isId,
                        aiModelName = aiModelName
                    )
                    if (!geminiReply.isNullOrBlank()) {
                        reply = geminiReply
                    } else if (lower.contains("cuaca") || lower.contains("weather") || lower.contains("hujan") || lower.contains("ombak") || lower.contains("angin") || lower.contains("laut")) {
                        reply = if (isId) {
                            "📡 **Telemetri Satelit Online Terkini (${liveWeather.locationName})**:\n" +
                                "• Kondisi: **${liveWeather.description}** (${liveWeather.temp.roundToInt()}°C, Terasa ${liveWeather.apparentTemp.roundToInt()}°C)\n" +
                                "• Kelembapan & Tekanan: **${liveWeather.humidity}%** • **${liveWeather.pressure.roundToInt()} hPa**\n" +
                                "• Angin Darat & Laut: **${liveWeather.windSpeed.roundToInt()} km/j** (Hembusan ${liveWeather.windGusts.roundToInt()} km/j, Laut ${liveWeather.seaWindSpeed.roundToInt()} km/j)\n" +
                                "• Tinggi Gelombang Pesisir: **${"%.2f".format(Locale.US, liveWeather.waveHeightM)} m** (Suhu Permukaan Laut ${"%.1f".format(Locale.US, liveWeather.seaSurfaceTempC)}°C)\n" +
                                "• Indeks UV & Kualitas Udara: **UV ${liveWeather.uvIndex}** • **AQI ${liveWeather.aqi}**\n" +
                                "• Sumber Data: **${liveWeather.source.uppercase()} (Open-Meteo Satelit & Maritim)**."
                        } else {
                            "📡 **Live Online Satellite Telemetry (${liveWeather.locationName})**:\n" +
                                "• Condition: **${liveWeather.description}** (${liveWeather.temp.roundToInt()}°C, Feels like ${liveWeather.apparentTemp.roundToInt()}°C)\n" +
                                "• Humidity & Pressure: **${liveWeather.humidity}%** • **${liveWeather.pressure.roundToInt()} hPa**\n" +
                                "• Wind & Marine: **${liveWeather.windSpeed.roundToInt()} km/h** (Gusts ${liveWeather.windGusts.roundToInt()} km/h, Sea ${liveWeather.seaWindSpeed.roundToInt()} km/h)\n" +
                                "• Coastal Wave Height: **${"%.2f".format(Locale.US, liveWeather.waveHeightM)} m** (SST ${"%.1f".format(Locale.US, liveWeather.seaSurfaceTempC)}°C)\n" +
                                "• UV & Air Quality: **UV ${liveWeather.uvIndex}** • **AQI ${liveWeather.aqi}**\n" +
                                "• Source: **${liveWeather.source.uppercase()} (Open-Meteo Satellite & Marine)**."
                        }
                    } else if (lower.contains("tidur") || lower.contains("sleep") || lower.contains("siklus") || lower.contains("rem")) {
                        reply = when (aiStyle) {
                            0 -> if (isId) "Suhu kamar ideal tidur REM: 19°C–22°C (5 siklus = 7.5 jam). Cuaca ${liveWeather.locationName} saat ini ${liveWeather.temp.roundToInt()}°C (${liveWeather.humidity}% RH)."
                            else "Ideal REM sleep room temp: 19°C–22°C (5 cycles = 7.5 hrs). Current ${liveWeather.locationName} temp: ${liveWeather.temp.roundToInt()}°C (${liveWeather.humidity}% RH)."
                            2 -> if (isId) "Analisis Sirkadian & Termal Online ($aiModelName) [${liveWeather.locationName}: ${liveWeather.temp.roundToInt()}°C]: Setiap siklus tidur manusia berlangsung ~90 menit (NREM Tahap 1-3 diikuti fase REM). Suhu kamar 19°C–22°C memfasilitasi penurunan suhu inti tubuh (core thermoregulation) yang memicu pelepasan melatonin dan meminimalkan inersia tidur saat Smart Alarm berbunyi."
                            else "Online Circadian & Thermal Telemetry ($aiModelName) [${liveWeather.locationName}: ${liveWeather.temp.roundToInt()}°C]: Complete human sleep architecture cycles every ~90 minutes. Maintaining an ambient bedroom climate of 19°C–22°C accelerates core thermoregulation and minimizes sleep inertia."
                            else -> if (isId) "Berdasarkan telemetri iklim online CHRONOS [${liveWeather.locationName}: ${liveWeather.temp.roundToInt()}°C, ${liveWeather.description}]: Suhu optimal tidur nyenyak berkisar 19°C - 22°C. Targetkan 5 siklus tidur (7 jam 30 menit) agar bangun tepat di akhir fase REM."
                            else "Based on CHRONOS live climate telemetry [${liveWeather.locationName}: ${liveWeather.temp.roundToInt()}°C, ${liveWeather.description}]: Optimal room temperature for deep REM sleep is 19°C - 22°C. Aim for 5 full 90-minute cycles (7h 30m)."
                        }
                    } else {
                        val activeAlarmsCount = _alarms.value.count { it.isEnabled }
                        val pendingTasksCount = _tasks.value.count { !it.isCompleted }
                        reply = if (isId) {
                            "🌐 **Status Online CHRONOS v$CHRONOS_VERSION ($aiModelName)**:\n" +
                                "• Cuaca Realtime (${liveWeather.locationName}): **${liveWeather.temp.roundToInt()}°C (${liveWeather.description})**, Angin ${liveWeather.windSpeed.roundToInt()} km/j, Ombak ${"%.2f".format(Locale.US, liveWeather.waveHeightM)}m\n" +
                                "• Perangkat Anda: **$activeAlarmsCount Alarm Aktif**, **$pendingTasksCount Tugas Belum Selesai**, **${_totalFocusMinutes.value} Menit Fokus**\n\n" +
                                "Ketik perintah langsung seperti *\"Pasang alarm 05:30\"*, *\"Timer 20 menit\"*, *\"Cuaca terkini\"*, atau *\"Analisis tidur REM\"*."
                        } else {
                            "🌐 **CHRONOS v$CHRONOS_VERSION Online Telemetry ($aiModelName)**:\n" +
                                "• Live Weather (${liveWeather.locationName}): **${liveWeather.temp.roundToInt()}°C (${liveWeather.description})**, Wind ${liveWeather.windSpeed.roundToInt()} km/h, Wave ${"%.2f".format(Locale.US, liveWeather.waveHeightM)}m\n" +
                                "• Device State: **$activeAlarmsCount Active Alarms**, **$pendingTasksCount Pending Tasks**, **${_totalFocusMinutes.value} Focus Mins**\n\n" +
                                "Try commands like *\"Set alarm 05:30\"*, *\"Timer 20 minutes\"*, *\"Live weather\"*, or *\"REM sleep analysis\"*."
                        }
                    }
                }
            }

            if (!executeImmediately) {
                _chatMessages.value = _chatMessages.value + ChatMsg(text = reply, isUser = false)
            }
        }
    }

    private suspend fun callOnlineGeminiIfConfigured(
        userPrompt: String,
        weather: WeatherData,
        isId: Boolean,
        aiModelName: String
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = try {
            val field = BuildConfig::class.java.getDeclaredField("GEMINI_API_KEY")
            field.isAccessible = true
            (field.get(null) as? String).orEmpty()
        } catch (_: Throwable) {
            ""
        }
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY" || apiKey.startsWith("TODO")) {
            return@withContext null
        }
        var conn: HttpURLConnection? = null
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 7000
                readTimeout = 9000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val activeAlarms = _alarms.value.filter { it.isEnabled }.joinToString(", ") { "%02d:%02d".format(it.hour, it.minute) }.ifEmpty { "None" }
            val openTasks = _tasks.value.filter { !it.isCompleted }.joinToString(", ") { it.title }.ifEmpty { "None" }
            val sysContext = if (isId) {
                "Anda adalah $aiModelName di aplikasi CHRONOS v$CHRONOS_VERSION. Jawab singkat, akurat, faktual, dan langsung membantu pengguna dalam Bahasa Indonesia. Data nyata perangkat saat ini: Lokasi=${weather.locationName}, Suhu=${weather.temp.roundToInt()}°C (${weather.description}), Kelembapan=${weather.humidity}%, Angin=${weather.windSpeed.roundToInt()} km/j, Tinggi Gelombang Laut=${weather.waveHeightM}m, Tekanan=${weather.pressure.roundToInt()} hPa, Alarm Aktif=[$activeAlarms], Tugas Aktif=[$openTasks], Fokus Hari Ini=${_totalFocusMinutes.value} menit."
            } else {
                "You are $aiModelName in CHRONOS v$CHRONOS_VERSION. Respond concisely, factually, and helpfully in English. Real device telemetry: Location=${weather.locationName}, Temp=${weather.temp.roundToInt()}°C (${weather.description}), Humidity=${weather.humidity}%, Wind=${weather.windSpeed.roundToInt()} km/h, Wave Height=${weather.waveHeightM}m, Pressure=${weather.pressure.roundToInt()} hPa, Active Alarms=[$activeAlarms], Open Tasks=[$openTasks], Focus Today=${_totalFocusMinutes.value} mins."
            }
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", "$sysContext\n\nPertanyaan Pengguna: $userPrompt"))
                        )
                    )
                )
            }
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            if (conn.responseCode in 200..299) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(respStr)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim()
                if (!text.isNullOrBlank()) return@withContext text
            }
            null
        } catch (_: Throwable) {
            null
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Throwable) {}
        }
    }

    fun confirmPendingAiAction() {
        val pending = _pendingAiAction.value ?: return
        pending.execute()
        val isId = _settings.value.appLanguage == "id"
        _chatMessages.value = _chatMessages.value + ChatMsg(
            text = if (isId) "Dikonfirmasi dan dijalankan." else "Confirmed and applied.",
            isUser = false
        )
        _pendingAiAction.value = null
    }

    fun cancelPendingAiAction() {
        val isId = _settings.value.appLanguage == "id"
        _pendingAiAction.value = null
        _chatMessages.value = _chatMessages.value + ChatMsg(
            text = if (isId) "Aksi dibatalkan." else "Action cancelled.",
            isUser = false
        )
    }

    // Export / Import JSON for Settings Backup, Cloud & Local Persistence
    private fun savePersistedState() {
        if (isRestoring) return
        try {
            stateDirty = false
            prefs.edit().putString("chronos_state_json", exportBackupJson()).apply()
        } catch (_: Exception) {
        }
    }

    private fun loadPersistedState() {
        val raw = prefs.getString("chronos_state_json", null) ?: return
        isRestoring = true
        try {
            importBackupJson(raw)
        } finally {
            isRestoring = false
        }
    }

    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", CHRONOS_VERSION)
        root.put("timestamp", System.currentTimeMillis())
        val s = _settings.value
        val sObj = JSONObject().apply {
            put("themeColorIndex", s.themeColorIndex)
            put("clockStyleIndex", s.clockStyleIndex)
            put("is24HourFormat", s.is24HourFormat)
            put("showClockSeconds", s.showClockSeconds)
            put("smoothSweepSeconds", s.smoothSweepSeconds)
            put("ambientGlowIntensity", s.ambientGlowIntensity)
            put("highContrastGlass", s.highContrastGlass)
            put("enableSwipeNavigation", s.enableSwipeNavigation)
            put("hapticFeedbackEnabled", s.hapticFeedbackEnabled)
            put("showHomeUtcBadge", s.showHomeUtcBadge)
            put("showSmartDailyBriefing", s.showSmartDailyBriefing)
            put("showHomeWeatherCard", s.showHomeWeatherCard)
            put("showHomeLifeSuite", s.showHomeLifeSuite)
            put("showHomeCalendarCard", s.showHomeCalendarCard)
            put("defaultVibrate", s.defaultVibrate)
            put("notificationsEnabled", s.notificationsEnabled)
            put("appLanguage", s.appLanguage)
            put("alarmRingtoneIndex", s.alarmRingtoneIndex)
            put("showWorldClockGlobe", s.showWorldClockGlobe)
            put("worldClockGlobeStyle", s.worldClockGlobeStyle)
            put("globeAutoRotate", s.globeAutoRotate)
            put("globeRotationSpeed", s.globeRotationSpeed)
            put("temperatureUnit", s.temperatureUnit)
            put("windSpeedUnit", s.windSpeedUnit)
            put("weatherAlertsEnabled", s.weatherAlertsEnabled)
            put("snoozeDuration", s.snoozeDuration)
            put("alarmFadeInDuration", s.alarmFadeInDuration)
            put("preWakeGentleDefault", s.preWakeGentleDefault)
            put("bedtimeReminderEnabled", s.bedtimeReminderEnabled)
            put("targetSleepCycles", s.targetSleepCycles)
            put("masterSoundVolume", s.masterSoundVolume.toDouble())
            put("weatherRefreshInterval", s.weatherRefreshInterval)
            put("autoCloudBackup", s.autoCloudBackup)
            put("lastBackupTimestamp", s.lastBackupTimestamp)
            put("timerSound", s.timerSound)
            put("timerVibrate", s.timerVibrate)
            put("timerAutoRepeat", s.timerAutoRepeat)
            put("timerAutoSoundscape", s.timerAutoSoundscape)
            put("defaultFocusMinutes", s.defaultFocusMinutes)
            put("stopwatchShowMs", s.stopwatchShowMs)
            put("stopwatchLapVibrate", s.stopwatchLapVibrate)
            put("calendarFirstDayOfWeek", s.calendarFirstDayOfWeek)
            put("calendarShowHolidays", s.calendarShowHolidays)
            put("dailyWaterGoal", s.dailyWaterGoal)
            put("autoSortTasksByPriority", s.autoSortTasksByPriority)
            put("aiModelIndex", s.aiModelIndex)
            put("aiResponseStyle", s.aiResponseStyle)
            put("aiAutoExecuteActions", s.aiAutoExecuteActions)
            put("defaultWakeMission", s.defaultWakeMission.name)
            put("ecoPerformanceMode", s.ecoPerformanceMode)
            put("defaultAlarmBackgroundTheme", s.defaultAlarmBackgroundTheme)
            put("savedCities", JSONArray(s.savedCities))
        }
        root.put("settings", sObj)
        root.put("savedWeatherCities", JSONArray(_savedWeatherCities.value))
        root.put("completedFocusSessions", _completedFocusSessions.value)
        root.put("totalFocusMinutes", _totalFocusMinutes.value)

        val aArr = JSONArray()
        _alarms.value.forEach { a ->
            aArr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("hour", a.hour)
                    put("minute", a.minute)
                    put("isEnabled", a.isEnabled)
                    put("label", a.label)
                    put("daysOfWeek", a.daysOfWeek)
                    put("dateMillis", a.dateMillis)
                    put("isVibrate", a.isVibrate)
                    put("soundUri", a.soundUri)
                    put("customAudioTitle", a.customAudioTitle)
                    put("isSnooze", a.isSnooze)
                    put("wakeMission", a.wakeMission.name)
                    put("fadeInSeconds", a.fadeInSeconds)
                    put("preWakeGentle", a.preWakeGentle)
                    put("backgroundTheme", a.backgroundTheme)
                }
            )
        }
        root.put("alarms", aArr)

        val tArr = JSONArray()
        _tasks.value.forEach { t ->
            tArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("description", t.description)
                    put("deadline", t.deadline)
                    put("priority", t.priority.name)
                    put("category", t.category.name)
                    put("isCompleted", t.isCompleted)
                }
            )
        }
        root.put("tasks", tArr)

        val eArr = JSONArray()
        _events.value.forEach { e ->
            eArr.put(
                JSONObject().apply {
                    put("id", e.id)
                    put("title", e.title)
                    put("startTime", e.startTime)
                    put("endTime", e.endTime)
                    put("location", e.location)
                    put("description", e.description)
                    put("isCompleted", e.isCompleted)
                    put("colorHex", e.colorHex)
                }
            )
        }
        root.put("events", eArr)

        val hArr = JSONArray()
        _habits.value.forEach { h ->
            hArr.put(
                JSONObject().apply {
                    put("id", h.id)
                    put("name", h.name)
                    put("category", h.category)
                    put("targetCount", h.targetCount)
                    put("currentCount", h.currentCount)
                    put("unit", h.unit)
                    put("streakDays", h.streakDays)
                    put("isCompletedToday", h.isCompletedToday)
                    put("iconName", h.iconName)
                }
            )
        }
        root.put("habits", hArr)

        val rArr = JSONArray()
        _reminders.value.forEach { r ->
            rArr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("title", r.title)
                    put("timeStr", r.timeStr)
                    put("recurrence", r.recurrence.name)
                    put("isEnabled", r.isEnabled)
                }
            )
        }
        root.put("reminders", rArr)

        val nArr = JSONArray()
        _notes.value.forEach { n ->
            nArr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("content", n.content)
                    put("category", n.category)
                    put("updatedAtMillis", n.updatedAtMillis)
                }
            )
        }
        root.put("notes", nArr)

        val lObj = _lifeLog.value
        root.put(
            "lifeLog",
            JSONObject().apply {
                put("day", lObj.day)
                put("water", lObj.water)
                put("breakfast", lObj.breakfast)
                put("lunch", lObj.lunch)
                put("dinner", lObj.dinner)
                put("sleepHours", lObj.sleepHours)
                put("workoutMins", lObj.workoutMins)
            }
        )
        root.put("isLifeNotified", _isLifeNotified.value)

        val swArr = JSONArray()
        _savedStopwatchSessions.value.forEach { rec ->
            swArr.put(
                JSONObject().apply {
                    put("id", rec.id)
                    put("activityTag", rec.activityTag)
                    put("totalElapsedMs", rec.totalElapsedMs)
                    put("lapCount", rec.lapCount)
                    put("fastestLapMs", rec.fastestLapMs)
                    put("averageLapMs", rec.averageLapMs)
                    put("consistencyScore", rec.consistencyScore)
                    put("timestampStr", rec.timestampStr)
                }
            )
        }
        root.put("savedStopwatchSessions", swArr)

        val calcArr = JSONArray()
        _calculatorHistory.value.forEach { ch ->
            calcArr.put(
                JSONObject().apply {
                    put("id", ch.id)
                    put("categoryName", ch.categoryName)
                    put("expression", ch.expression)
                    put("result", ch.result)
                    put("timestampStr", ch.timestampStr)
                }
            )
        }
        root.put("calculatorHistory", calcArr)

        val calArr = JSONArray()
        _calendarEvents.value.forEach { ce ->
            calArr.put(
                JSONObject().apply {
                    put("id", ce.id)
                    put("title", ce.title)
                    put("description", ce.description)
                    put("startTime", ce.startTime)
                    put("color", ce.color)
                }
            )
        }
        root.put("calendarEvents", calArr)

        return root.toString(2)
    }

    fun importBackupJson(rawJson: String): Boolean {
        return try {
            val obj = JSONObject(rawJson)
            val sObj = obj.optJSONObject("settings")
            if (sObj != null) {
                val savedCitiesArr = sObj.optJSONArray("savedCities")
                val citiesList = if (savedCitiesArr != null && savedCitiesArr.length() > 0) {
                    List(savedCitiesArr.length()) { savedCitiesArr.getString(it) }
                } else null

                updateSettings { cur ->
                    cur.copy(
                        themeColorIndex = sObj.optInt("themeColorIndex", cur.themeColorIndex),
                        clockStyleIndex = sObj.optInt("clockStyleIndex", cur.clockStyleIndex),
                        is24HourFormat = sObj.optBoolean("is24HourFormat", cur.is24HourFormat),
                        showClockSeconds = sObj.optBoolean("showClockSeconds", cur.showClockSeconds),
                        smoothSweepSeconds = sObj.optBoolean("smoothSweepSeconds", cur.smoothSweepSeconds),
                        ambientGlowIntensity = sObj.optInt("ambientGlowIntensity", cur.ambientGlowIntensity),
                        highContrastGlass = sObj.optBoolean("highContrastGlass", cur.highContrastGlass),
                        enableSwipeNavigation = sObj.optBoolean("enableSwipeNavigation", cur.enableSwipeNavigation),
                        hapticFeedbackEnabled = sObj.optBoolean("hapticFeedbackEnabled", cur.hapticFeedbackEnabled),
                        showHomeUtcBadge = sObj.optBoolean("showHomeUtcBadge", cur.showHomeUtcBadge),
                        showSmartDailyBriefing = sObj.optBoolean("showSmartDailyBriefing", cur.showSmartDailyBriefing),
                        showHomeWeatherCard = sObj.optBoolean("showHomeWeatherCard", cur.showHomeWeatherCard),
                        showHomeLifeSuite = sObj.optBoolean("showHomeLifeSuite", cur.showHomeLifeSuite),
                        showHomeCalendarCard = sObj.optBoolean("showHomeCalendarCard", cur.showHomeCalendarCard),
                        defaultVibrate = sObj.optBoolean("defaultVibrate", cur.defaultVibrate),
                        notificationsEnabled = sObj.optBoolean("notificationsEnabled", cur.notificationsEnabled),
                        appLanguage = sObj.optString("appLanguage", cur.appLanguage),
                        alarmRingtoneIndex = sObj.optInt("alarmRingtoneIndex", cur.alarmRingtoneIndex),
                        showWorldClockGlobe = sObj.optBoolean("showWorldClockGlobe", cur.showWorldClockGlobe),
                        worldClockGlobeStyle = sObj.optInt("worldClockGlobeStyle", cur.worldClockGlobeStyle),
                        globeAutoRotate = sObj.optBoolean("globeAutoRotate", cur.globeAutoRotate),
                        globeRotationSpeed = sObj.optInt("globeRotationSpeed", cur.globeRotationSpeed),
                        temperatureUnit = sObj.optInt("temperatureUnit", cur.temperatureUnit),
                        windSpeedUnit = sObj.optInt("windSpeedUnit", cur.windSpeedUnit),
                        weatherAlertsEnabled = sObj.optBoolean("weatherAlertsEnabled", cur.weatherAlertsEnabled),
                        snoozeDuration = sObj.optInt("snoozeDuration", cur.snoozeDuration),
                        alarmFadeInDuration = sObj.optInt("alarmFadeInDuration", cur.alarmFadeInDuration),
                        preWakeGentleDefault = sObj.optBoolean("preWakeGentleDefault", cur.preWakeGentleDefault),
                        bedtimeReminderEnabled = sObj.optBoolean("bedtimeReminderEnabled", cur.bedtimeReminderEnabled),
                        targetSleepCycles = sObj.optInt("targetSleepCycles", cur.targetSleepCycles),
                        masterSoundVolume = sObj.optDouble("masterSoundVolume", cur.masterSoundVolume.toDouble()).toFloat(),
                        weatherRefreshInterval = sObj.optInt("weatherRefreshInterval", cur.weatherRefreshInterval),
                        autoCloudBackup = sObj.optBoolean("autoCloudBackup", cur.autoCloudBackup),
                        lastBackupTimestamp = sObj.optLong("lastBackupTimestamp", cur.lastBackupTimestamp),
                        timerSound = sObj.optString("timerSound", cur.timerSound),
                        timerVibrate = sObj.optBoolean("timerVibrate", cur.timerVibrate),
                        timerAutoRepeat = sObj.optBoolean("timerAutoRepeat", cur.timerAutoRepeat),
                        timerAutoSoundscape = sObj.optString("timerAutoSoundscape", cur.timerAutoSoundscape),
                        defaultFocusMinutes = sObj.optInt("defaultFocusMinutes", cur.defaultFocusMinutes),
                        stopwatchShowMs = sObj.optBoolean("stopwatchShowMs", cur.stopwatchShowMs),
                        stopwatchLapVibrate = sObj.optBoolean("stopwatchLapVibrate", cur.stopwatchLapVibrate),
                        calendarFirstDayOfWeek = sObj.optString("calendarFirstDayOfWeek", cur.calendarFirstDayOfWeek),
                        calendarShowHolidays = sObj.optBoolean("calendarShowHolidays", cur.calendarShowHolidays),
                        dailyWaterGoal = sObj.optInt("dailyWaterGoal", cur.dailyWaterGoal),
                        autoSortTasksByPriority = sObj.optBoolean("autoSortTasksByPriority", cur.autoSortTasksByPriority),
                        aiModelIndex = sObj.optInt("aiModelIndex", cur.aiModelIndex),
                        aiResponseStyle = sObj.optInt("aiResponseStyle", cur.aiResponseStyle),
                        aiAutoExecuteActions = sObj.optBoolean("aiAutoExecuteActions", cur.aiAutoExecuteActions),
                        defaultWakeMission = runCatching { WakeMission.valueOf(sObj.optString("defaultWakeMission", cur.defaultWakeMission.name)) }.getOrDefault(cur.defaultWakeMission),
                        ecoPerformanceMode = sObj.optBoolean("ecoPerformanceMode", cur.ecoPerformanceMode),
                        defaultAlarmBackgroundTheme = sObj.optString("defaultAlarmBackgroundTheme", cur.defaultAlarmBackgroundTheme),
                        savedCities = citiesList ?: cur.savedCities
                    )
                }
            }

            val wcArr = obj.optJSONArray("savedWeatherCities")
            if (wcArr != null && wcArr.length() > 0) {
                _savedWeatherCities.value = List(wcArr.length()) { wcArr.getString(it) }
            }
            _completedFocusSessions.value = obj.optInt("completedFocusSessions", _completedFocusSessions.value)
            _totalFocusMinutes.value = obj.optInt("totalFocusMinutes", _totalFocusMinutes.value)

            val aArr = obj.optJSONArray("alarms")
            if (aArr != null) {
                val restored = mutableListOf<AlarmItem>()
                for (i in 0 until aArr.length()) {
                    val item = aArr.getJSONObject(i)
                    restored.add(
                        AlarmItem(
                            id = item.optLong("id", System.currentTimeMillis() + i),
                            hour = item.optInt("hour", 6),
                            minute = item.optInt("minute", 30),
                            isEnabled = item.optBoolean("isEnabled", true),
                            label = item.optString("label", "Alarm"),
                            daysOfWeek = item.optInt("daysOfWeek", 0),
                            dateMillis = item.optLong("dateMillis", 0L),
                            isVibrate = item.optBoolean("isVibrate", true),
                            soundUri = item.optString("soundUri", "AI Cosmos"),
                            customAudioTitle = item.optString("customAudioTitle", ""),
                            isSnooze = item.optBoolean("isSnooze", true),
                            wakeMission = runCatching { WakeMission.valueOf(item.optString("wakeMission", "NONE")) }.getOrDefault(WakeMission.NONE),
                            fadeInSeconds = item.optInt("fadeInSeconds", 20),
                            preWakeGentle = item.optBoolean("preWakeGentle", true),
                            backgroundTheme = item.optString("backgroundTheme", "COSMIC_DAWN")
                        )
                    )
                }
                _alarms.value = restored
            }

            val tArr = obj.optJSONArray("tasks")
            if (tArr != null) {
                val list = mutableListOf<TaskItem>()
                for (i in 0 until tArr.length()) {
                    val item = tArr.getJSONObject(i)
                    list.add(
                        TaskItem(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            description = item.optString("description"),
                            deadline = item.optString("deadline", "18:00"),
                            priority = runCatching { TaskPriority.valueOf(item.optString("priority")) }.getOrDefault(TaskPriority.MEDIUM),
                            category = runCatching { TaskCategory.valueOf(item.optString("category")) }.getOrDefault(TaskCategory.WORK),
                            isCompleted = item.optBoolean("isCompleted", false)
                        )
                    )
                }
                _tasks.value = list
            }

            val eArr = obj.optJSONArray("events")
            if (eArr != null) {
                val list = mutableListOf<AgendaEvent>()
                for (i in 0 until eArr.length()) {
                    val item = eArr.getJSONObject(i)
                    list.add(
                        AgendaEvent(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            startTime = item.optString("startTime", "09:00"),
                            endTime = item.optString("endTime", "10:00"),
                            location = item.optString("location"),
                            description = item.optString("description"),
                            isCompleted = item.optBoolean("isCompleted", false),
                            colorHex = item.optString("colorHex", "#00E5FF")
                        )
                    )
                }
                _events.value = list
            }

            val hArr = obj.optJSONArray("habits")
            if (hArr != null) {
                val list = mutableListOf<HabitItem>()
                for (i in 0 until hArr.length()) {
                    val item = hArr.getJSONObject(i)
                    list.add(
                        HabitItem(
                            id = item.optString("id"),
                            name = item.optString("name"),
                            category = item.optString("category", "Health"),
                            targetCount = item.optInt("targetCount", 1),
                            currentCount = item.optInt("currentCount", 0),
                            unit = item.optString("unit", "kali"),
                            streakDays = item.optInt("streakDays", 0),
                            isCompletedToday = item.optBoolean("isCompletedToday", false),
                            iconName = item.optString("iconName", "Water")
                        )
                    )
                }
                _habits.value = list
            }

            val rArr = obj.optJSONArray("reminders")
            if (rArr != null) {
                val list = mutableListOf<ReminderItem>()
                for (i in 0 until rArr.length()) {
                    val item = rArr.getJSONObject(i)
                    list.add(
                        ReminderItem(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            timeStr = item.optString("timeStr", "09:00"),
                            recurrence = runCatching { RecurrenceType.valueOf(item.optString("recurrence")) }.getOrDefault(RecurrenceType.DAILY),
                            isEnabled = item.optBoolean("isEnabled", true)
                        )
                    )
                }
                _reminders.value = list
            }

            val nArr = obj.optJSONArray("notes")
            if (nArr != null) {
                val list = mutableListOf<NoteItem>()
                for (i in 0 until nArr.length()) {
                    val item = nArr.getJSONObject(i)
                    list.add(
                        NoteItem(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            content = item.optString("content"),
                            category = item.optString("category", "Work"),
                            updatedAtMillis = item.optLong("updatedAtMillis", System.currentTimeMillis())
                        )
                    )
                }
                _notes.value = list
            }

            val lObj = obj.optJSONObject("lifeLog")
            if (lObj != null) {
                val savedDay = lObj.optString("day", todayKey())
                if (savedDay == todayKey()) {
                    _lifeLog.value = LifeLog(
                        day = savedDay,
                        water = lObj.optInt("water", 0),
                        breakfast = lObj.optBoolean("breakfast", false),
                        lunch = lObj.optBoolean("lunch", false),
                        dinner = lObj.optBoolean("dinner", false),
                        sleepHours = lObj.optInt("sleepHours", 7),
                        workoutMins = lObj.optInt("workoutMins", 0)
                    )
                }
            }
            _isLifeNotified.value = obj.optBoolean("isLifeNotified", _isLifeNotified.value)

            val swArr = obj.optJSONArray("savedStopwatchSessions")
            if (swArr != null) {
                val list = mutableListOf<StopwatchSessionRecord>()
                for (i in 0 until swArr.length()) {
                    val item = swArr.getJSONObject(i)
                    list.add(
                        StopwatchSessionRecord(
                            id = item.optString("id"),
                            activityTag = item.optString("activityTag", "Stopwatch"),
                            totalElapsedMs = item.optLong("totalElapsedMs", 0L),
                            lapCount = item.optInt("lapCount", 0),
                            fastestLapMs = item.optLong("fastestLapMs", 0L),
                            averageLapMs = item.optLong("averageLapMs", 0L),
                            consistencyScore = item.optInt("consistencyScore", 100),
                            timestampStr = item.optString("timestampStr", "")
                        )
                    )
                }
                _savedStopwatchSessions.value = list
            }

            val calArr = obj.optJSONArray("calendarEvents")
            if (calArr != null) {
                val list = mutableListOf<CalendarEvent>()
                for (i in 0 until calArr.length()) {
                    val item = calArr.getJSONObject(i)
                    list.add(
                        CalendarEvent(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            description = item.optString("description"),
                            startTime = item.optLong("startTime", System.currentTimeMillis()),
                            color = item.optString("color", "#00E5FF")
                        )
                    )
                }
                _calendarEvents.value = list
            }

            val calcArr = obj.optJSONArray("calculatorHistory")
            if (calcArr != null) {
                val list = mutableListOf<CalculatorHistoryRecord>()
                for (i in 0 until calcArr.length()) {
                    val item = calcArr.getJSONObject(i)
                    list.add(
                        CalculatorHistoryRecord(
                            id = item.optString("id", "calc_${System.currentTimeMillis()}_$i"),
                            categoryName = item.optString("categoryName", "Kalkulator Dasar"),
                            expression = item.optString("expression", ""),
                            result = item.optString("result", "0"),
                            timestampStr = item.optString("timestampStr", "")
                        )
                    )
                }
                _calculatorHistory.value = list
            }

            savePersistedState()
            recomputeAiInsights()
            true
        } catch (_: Exception) {
            false
        }
    }

    // Calculator Smart History Management
    fun addCalculatorHistoryRecord(categoryName: String, expression: String, result: String) {
        if (expression.isBlank() || result.isBlank()) return
        val cal = Calendar.getInstance()
        val ts = "%02d:%02d:%02d • %02d/%02d/%04d".format(
            Locale.US,
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND),
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
        val record = CalculatorHistoryRecord(
            id = "calc_${System.currentTimeMillis()}",
            categoryName = categoryName,
            expression = expression.trim(),
            result = result.trim(),
            timestampStr = ts
        )
        _calculatorHistory.value = (listOf(record) + _calculatorHistory.value).take(100)
        stateDirty = true
        savePersistedState()
    }

    fun deleteCalculatorHistoryRecord(id: String) {
        _calculatorHistory.value = _calculatorHistory.value.filterNot { it.id == id }
        stateDirty = true
        savePersistedState()
    }

    fun clearCalculatorHistory() {
        _calculatorHistory.value = emptyList()
        stateDirty = true
        savePersistedState()
    }
}
