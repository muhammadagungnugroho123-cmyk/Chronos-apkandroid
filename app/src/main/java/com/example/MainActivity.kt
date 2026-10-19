package com.example

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.ScreenType
import com.example.service.ChronosAlarmScheduler
import com.example.service.ChronosAudioSynthesizer
import com.example.ui.ChronosStateHolder
import com.example.ui.components.AnimatedGradientBackground
import com.example.ui.components.ChronosAmberNeon
import com.example.ui.components.ChronosBgDeep
import com.example.ui.components.ChronosBottomNavigation
import com.example.ui.components.ChronosCyanNeon
import com.example.ui.components.ChronosSurfaceDark
import com.example.ui.components.ChronosTopBar
import com.example.ui.components.FeatureHubDrawer
import com.example.ui.screens.AiAssistantFullScreen
import com.example.ui.screens.AlarmActivityModal
import com.example.ui.screens.AlarmsFullScreen
import com.example.ui.screens.CalculatorFullScreen
import com.example.ui.screens.ClockHomeScreen
import com.example.ui.screens.ProductivityFullScreen
import com.example.ui.screens.SettingsFullScreen
import com.example.ui.screens.StopwatchFullScreen
import com.example.ui.screens.TimersFullScreen
import com.example.ui.screens.WeatherFullScreen
import com.example.ui.screens.WorldClockFullScreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private val latestIntentFlow = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        latestIntentFlow.value = intent
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                val stateHolder: ChronosStateHolder = viewModel()
                val currentIntent by latestIntentFlow.collectAsStateWithLifecycle()
                LaunchedEffect(currentIntent) {
                    val incoming = currentIntent ?: return@LaunchedEffect
                    val alarmId = incoming.getLongExtra(ChronosAlarmScheduler.EXTRA_ALARM_ID, -1L)
                    val openAlarms = incoming.getBooleanExtra(ChronosAlarmScheduler.EXTRA_OPEN_ALARMS, false)
                    val targetScreen = incoming.getStringExtra(ChronosAlarmScheduler.EXTRA_TARGET_SCREEN)
                    if (alarmId != -1L) {
                        val match = stateHolder.alarms.value.firstOrNull { it.id == alarmId }
                        if (match != null && stateHolder.ringingAlarm.value == null && openAlarms) {
                            stateHolder.navigateTo(ScreenType.ALARMS)
                        }
                    } else if (openAlarms) {
                        stateHolder.navigateTo(ScreenType.ALARMS)
                    } else if (!targetScreen.isNullOrBlank()) {
                        when (targetScreen.lowercase()) {
                            "timers" -> stateHolder.navigateTo(ScreenType.TIMERS)
                            "productivity" -> stateHolder.navigateTo(ScreenType.PRODUCTIVITY)
                            "weather" -> stateHolder.navigateTo(ScreenType.WEATHER)
                            "alarms" -> stateHolder.navigateTo(ScreenType.ALARMS)
                            "calculator" -> stateHolder.navigateTo(ScreenType.CALCULATOR)
                        }
                    }
                }
                ChronosRootApp(stateHolder = stateHolder)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntentFlow.value = intent
    }

    override fun onResume() {
        super.onResume()
        // Sync any background alarm state when returning to foreground
    }

    override fun onDestroy() {
        ChronosAudioSynthesizer.releaseAll()
        super.onDestroy()
    }
}

@Composable
fun ChronosRootApp(stateHolder: ChronosStateHolder) {
    val showSplash by stateHolder.showSplash.collectAsStateWithLifecycle()
    val currentScreen by stateHolder.currentScreen.collectAsStateWithLifecycle()
    val isFeatureHubOpen by stateHolder.isFeatureHubOpen.collectAsStateWithLifecycle()
    val selectedLifePillar by stateHolder.selectedLifePillar.collectAsStateWithLifecycle()
    val nowMillis by stateHolder.nowMillis.collectAsStateWithLifecycle()
    val settings by stateHolder.settings.collectAsStateWithLifecycle()

    val alarms by stateHolder.alarms.collectAsStateWithLifecycle()
    val ringingAlarm by stateHolder.ringingAlarm.collectAsStateWithLifecycle()
    val activeBanner by stateHolder.activeBanner.collectAsStateWithLifecycle()

    val timerTotalSeconds by stateHolder.timerTotalSeconds.collectAsStateWithLifecycle()
    val timerRemainingSeconds by stateHolder.timerRemainingSeconds.collectAsStateWithLifecycle()
    val timerIsRunning by stateHolder.timerIsRunning.collectAsStateWithLifecycle()
    val timerIsCompleted by stateHolder.timerIsCompleted.collectAsStateWithLifecycle()

    val stopwatchIsRunning by stateHolder.stopwatchIsRunning.collectAsStateWithLifecycle()
    val stopwatchElapsedMs by stateHolder.stopwatchElapsedMs.collectAsStateWithLifecycle()
    val stopwatchLaps by stateHolder.stopwatchLaps.collectAsStateWithLifecycle()
    val savedStopwatchSessions by stateHolder.savedStopwatchSessions.collectAsStateWithLifecycle()
    val calculatorHistory by stateHolder.calculatorHistory.collectAsStateWithLifecycle()

    val currentWeatherData by stateHolder.currentWeatherData.collectAsStateWithLifecycle()
    val isLoadingWeather by stateHolder.isLoadingWeather.collectAsStateWithLifecycle()
    val savedWeatherCities by stateHolder.savedWeatherCities.collectAsStateWithLifecycle()
    val citiesWeatherData by stateHolder.citiesWeatherData.collectAsStateWithLifecycle()

    val lifeLog by stateHolder.lifeLog.collectAsStateWithLifecycle()
    val isLifeNotified by stateHolder.isLifeNotified.collectAsStateWithLifecycle()

    val selectedPeriod by stateHolder.selectedPeriod.collectAsStateWithLifecycle()
    val tasks by stateHolder.tasks.collectAsStateWithLifecycle()
    val events by stateHolder.events.collectAsStateWithLifecycle()
    val habits by stateHolder.habits.collectAsStateWithLifecycle()
    val reminders by stateHolder.reminders.collectAsStateWithLifecycle()
    val notes by stateHolder.notes.collectAsStateWithLifecycle()
    val calendarEvents by stateHolder.calendarEvents.collectAsStateWithLifecycle()

    val focusTotalSeconds by stateHolder.focusTotalSeconds.collectAsStateWithLifecycle()
    val focusRemainingSeconds by stateHolder.focusRemainingSeconds.collectAsStateWithLifecycle()
    val isFocusRunning by stateHolder.isFocusRunning.collectAsStateWithLifecycle()
    val completedFocusSessions by stateHolder.completedFocusSessions.collectAsStateWithLifecycle()
    val totalFocusMinutes by stateHolder.totalFocusMinutes.collectAsStateWithLifecycle()
    val aiInsights by stateHolder.aiInsights.collectAsStateWithLifecycle()
    val isAiAnalyzing by stateHolder.isAiAnalyzing.collectAsStateWithLifecycle()

    val chatMessages by stateHolder.chatMessages.collectAsStateWithLifecycle()
    val pendingAiAction by stateHolder.pendingAiAction.collectAsStateWithLifecycle()
    val isNetworkOnline by stateHolder.isNetworkOnline.collectAsStateWithLifecycle()
    val networkTransportLabel by stateHolder.networkTransportLabel.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(settings.hapticFeedbackEnabled, settings.masterSoundVolume) {
        ChronosAudioSynthesizer.hapticsEnabled = settings.hapticFeedbackEnabled
        ChronosAudioSynthesizer.volume = settings.masterSoundVolume
    }

    DisposableEffect(currentScreen, timerIsRunning, stopwatchIsRunning, ringingAlarm, settings.ecoPerformanceMode) {
        val window = (context as? Activity)?.window
        val shouldKeepAwake = (!settings.ecoPerformanceMode && currentScreen == ScreenType.CLOCK) ||
            timerIsRunning || stopwatchIsRunning || ringingAlarm != null
        if (shouldKeepAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler(enabled = isFeatureHubOpen || currentScreen != ScreenType.CLOCK) {
        stateHolder.goBack()
    }

    // 1. Splash Screen View matching chronos.tsx
    if (showSplash) {
        val infiniteSpin = rememberInfiniteTransition(label = "splash_spin")
        val angle by infiniteSpin.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "splash_angle"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChronosBgDeep),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .rotate(angle)
                        .border(2.dp, ChronosCyanNeon, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = ChronosCyanNeon,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "CHRONOS",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Light,
                    fontSize = 30.sp,
                    letterSpacing = 10.sp,
                    color = ChronosCyanNeon
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "TIME • WEATHER • INTELLIGENCE",
                    fontSize = 11.sp,
                    letterSpacing = 2.5.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
        return
    }

    val hasActiveAlarm = remember(alarms) {
        alarms.any { it.isEnabled && !it.label.startsWith("Snooze:") }
    }
    val isDetailFullScreen = currentScreen == ScreenType.WEATHER ||
        currentScreen == ScreenType.SETTINGS ||
        currentScreen == ScreenType.PRODUCTIVITY ||
        currentScreen == ScreenType.CALCULATOR

    var accumulatedDragX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChronosBgDeep)
            .pointerInput(currentScreen, settings.enableSwipeNavigation) {
                if (settings.enableSwipeNavigation) {
                    detectHorizontalDragGestures(
                        onDragStart = { accumulatedDragX = 0f },
                        onDragEnd = {
                            if (abs(accumulatedDragX) > 110f) {
                                if (accumulatedDragX < 0f) {
                                    stateHolder.onSwipeLeft()
                                } else {
                                    stateHolder.onSwipeRight()
                                }
                            }
                            accumulatedDragX = 0f
                        }
                    ) { _, dragAmount ->
                        accumulatedDragX += dragAmount
                    }
                }
            }
    ) {
        // Dynamic Animated Gradient Background
        AnimatedGradientBackground(
            themeColorIndex = settings.themeColorIndex,
            ecoMode = settings.ecoPerformanceMode,
            ambientGlowIntensity = settings.ambientGlowIntensity
        )

        // Main App Layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 540.dp)
            ) {
                if (!isDetailFullScreen) {
                    ChronosTopBar(
                        hasActiveAlarm = hasActiveAlarm,
                        onOpenFeatureHub = { stateHolder.setFeatureHubOpen(true) },
                        onNavigateToWeather = { stateHolder.navigateTo(ScreenType.WEATHER) },
                        onNavigateToSettings = { stateHolder.navigateTo(ScreenType.SETTINGS) },
                        onNavigateToProductivity = { stateHolder.navigateTo(ScreenType.PRODUCTIVITY) }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        ScreenType.CLOCK -> {
                            ClockHomeScreen(
                                nowMillis = nowMillis,
                                settings = settings,
                                alarms = alarms,
                                clockStyleIndex = settings.clockStyleIndex,
                                is24HourFormat = settings.is24HourFormat,
                                themeColorIndex = settings.themeColorIndex,
                                appLanguage = settings.appLanguage,
                                temperatureUnit = settings.temperatureUnit,
                                weatherData = currentWeatherData,
                                isLoadingWeather = isLoadingWeather,
                                selectedLifePillar = selectedLifePillar,
                                lifeLog = lifeLog,
                                isLifeNotified = isLifeNotified,
                                tasks = tasks,
                                agendaEvents = events,
                                calendarEvents = calendarEvents,
                                onCycleClockStyle = {
                                    ChronosAudioSynthesizer.triggerHaptic(context, 1)
                                    stateHolder.updateSettings {
                                        it.copy(clockStyleIndex = (it.clockStyleIndex + 1) % 5)
                                    }
                                },
                                onRefreshWeather = { stateHolder.refreshPrimaryWeather(true) },
                                onNavigateToWeather = { stateHolder.navigateTo(ScreenType.WEATHER) },
                                onNavigateToProductivity = { stateHolder.navigateTo(ScreenType.PRODUCTIVITY) },
                                onNavigateToAlarms = { stateHolder.navigateTo(ScreenType.ALARMS) },
                                onNavigateToTimers = { stateHolder.navigateTo(ScreenType.TIMERS) },
                                onSelectLifePillar = stateHolder::selectLifePillar,
                                onUpdateLifeLog = stateHolder::updateLifeLog,
                                onToggleLifeNotify = stateHolder::toggleLifeNotify,
                                onAddHydrationReminder = stateHolder::addReminder,
                                onStart20MinTimer = { stateHolder.startTimer(20 * 60) },
                                onAddCalendarEvent = stateHolder::addCalendarEvent,
                                onRemoveCalendarEvent = stateHolder::removeCalendarEvent
                            )
                        }
                        ScreenType.ALARMS -> {
                            AlarmsFullScreen(
                                nowMillis = nowMillis,
                                alarms = alarms,
                                appLanguage = settings.appLanguage,
                                is24HourFormat = settings.is24HourFormat,
                                alarmFadeInDuration = settings.alarmFadeInDuration,
                                defaultWakeMission = settings.defaultWakeMission,
                                defaultRingtone = settings.alarmRingtoneIndex,
                                defaultVibrate = settings.defaultVibrate,
                                sleepCycleTarget = settings.targetSleepCycles,
                                defaultAlarmBackgroundTheme = settings.defaultAlarmBackgroundTheme,
                                onAddAlarm = stateHolder::addAlarm,
                                onUpdateAlarm = stateHolder::updateAlarm,
                                onDeleteAlarm = stateHolder::deleteAlarm,
                                onTestRingAlarm = stateHolder::triggerTestAlarmRing
                            )
                        }
                        ScreenType.TIMERS -> {
                            TimersFullScreen(
                                totalSeconds = timerTotalSeconds,
                                remainingSeconds = timerRemainingSeconds,
                                isRunning = timerIsRunning,
                                isCompleted = timerIsCompleted,
                                appLanguage = settings.appLanguage,
                                onSetDuration = stateHolder::setTimerDuration,
                                onStartOrResume = stateHolder::resumeTimer,
                                onPause = stateHolder::pauseTimer,
                                onReset = stateHolder::resetTimer,
                                onDismissCompleted = stateHolder::dismissCompletedTimer,
                                onAiCommand = { cmd -> stateHolder.sendAiChatMessage("timer $cmd", executeImmediately = true) }
                            )
                        }
                        ScreenType.STOPWATCH -> {
                            StopwatchFullScreen(
                                elapsedMs = stopwatchElapsedMs,
                                isRunning = stopwatchIsRunning,
                                laps = stopwatchLaps,
                                showMs = settings.stopwatchShowMs,
                                lapVibrate = settings.stopwatchLapVibrate,
                                appLanguage = settings.appLanguage,
                                onStart = stateHolder::startStopwatch,
                                onPause = stateHolder::pauseStopwatch,
                                onLap = stateHolder::lapStopwatch,
                                onReset = stateHolder::resetStopwatch,
                                onAiCommand = { cmd -> stateHolder.sendAiChatMessage(cmd, executeImmediately = true) },
                                savedSessions = savedStopwatchSessions,
                                onSaveSession = stateHolder::saveStopwatchSession,
                                onClearSessions = stateHolder::clearStopwatchSessions
                            )
                        }
                        ScreenType.WORLD_CLOCK -> {
                            WorldClockFullScreen(
                                nowMillis = nowMillis,
                                savedCities = settings.savedCities,
                                showWorldClockGlobe = settings.showWorldClockGlobe,
                                worldClockGlobeStyle = settings.worldClockGlobeStyle,
                                appLanguage = settings.appLanguage,
                                is24HourFormat = settings.is24HourFormat,
                                onUpdateSavedCities = { list ->
                                    stateHolder.updateSettings { it.copy(savedCities = list) }
                                },
                                onChangeGlobeStyle = { st ->
                                    stateHolder.updateSettings { it.copy(worldClockGlobeStyle = st) }
                                }
                            )
                        }
                        ScreenType.AI_ASSISTANT -> {
                            AiAssistantFullScreen(
                                messages = chatMessages,
                                pendingAction = pendingAiAction,
                                appLanguage = settings.appLanguage,
                                isNetworkOnline = isNetworkOnline,
                                networkTransportLabel = networkTransportLabel,
                                onRefreshNetwork = stateHolder::checkLiveNetworkStatus,
                                onSendMessage = { msg -> stateHolder.sendAiChatMessage(msg, executeImmediately = false) },
                                onConfirmPendingAction = stateHolder::confirmPendingAiAction,
                                onCancelPendingAction = stateHolder::cancelPendingAiAction
                            )
                        }
                        ScreenType.WEATHER -> {
                            WeatherFullScreen(
                                citiesData = citiesWeatherData,
                                savedCityNames = savedWeatherCities,
                                isLoading = isLoadingWeather,
                                appLanguage = settings.appLanguage,
                                temperatureUnit = settings.temperatureUnit,
                                windSpeedUnit = settings.windSpeedUnit,
                                isNetworkOnline = isNetworkOnline,
                                networkTransportLabel = networkTransportLabel,
                                onBack = { stateHolder.goBack() },
                                onRefreshAll = stateHolder::loadAllSavedCitiesWeather,
                                onDetectGpsLocation = stateHolder::detectDeviceGpsWeather,
                                onAddCity = stateHolder::addWeatherCity,
                                onRemoveCity = stateHolder::removeWeatherCity
                            )
                        }
                        ScreenType.PRODUCTIVITY -> {
                            ProductivityFullScreen(
                                selectedPeriod = selectedPeriod,
                                tasks = tasks,
                                events = events,
                                habits = habits,
                                reminders = reminders,
                                notes = notes,
                                focusRemainingSeconds = focusRemainingSeconds,
                                focusTotalSeconds = focusTotalSeconds,
                                isFocusRunning = isFocusRunning,
                                completedFocusSessions = completedFocusSessions,
                                totalFocusMinutes = totalFocusMinutes,
                                aiInsights = aiInsights,
                                isAiAnalyzing = isAiAnalyzing,
                                appLanguage = settings.appLanguage,
                                onBack = { stateHolder.goBack() },
                                onNavigateToSettings = { stateHolder.navigateTo(ScreenType.SETTINGS) },
                                onSelectPeriod = stateHolder::selectPeriod,
                                onAnalyzeDeep = stateHolder::runDeepProductivityAnalysis,
                                onAddTask = stateHolder::addTask,
                                onToggleTask = stateHolder::toggleTask,
                                onDeleteTask = stateHolder::deleteTask,
                                onAddEvent = stateHolder::addAgendaEvent,
                                onToggleEvent = stateHolder::toggleAgendaEvent,
                                onDeleteEvent = stateHolder::deleteAgendaEvent,
                                onAddHabit = stateHolder::addHabit,
                                onIncrementHabit = stateHolder::incrementHabit,
                                onDecrementHabit = stateHolder::decrementHabit,
                                onDeleteHabit = stateHolder::deleteHabit,
                                onAddReminder = stateHolder::addReminder,
                                onToggleReminder = stateHolder::toggleReminder,
                                onDeleteReminder = stateHolder::deleteReminder,
                                onAddNote = stateHolder::addNote,
                                onDeleteNote = stateHolder::deleteNote,
                                onToggleFocus = stateHolder::toggleProductivityFocus,
                                onResetFocus = stateHolder::resetProductivityFocus,
                                onSelectFocusDuration = stateHolder::selectProductivityFocusDuration
                            )
                        }
                        ScreenType.SETTINGS -> {
                            SettingsFullScreen(
                                settings = settings,
                                alarms = alarms,
                                onBack = { stateHolder.goBack() },
                                onUpdateSettings = stateHolder::updateSettings,
                                onResetSettings = stateHolder::resetSettingsToDefault,
                                onExportJson = stateHolder::exportBackupJson,
                                onImportJson = stateHolder::importBackupJson
                            )
                        }
                        ScreenType.CALCULATOR -> {
                            CalculatorFullScreen(
                                appLanguage = settings.appLanguage,
                                historyRecords = calculatorHistory,
                                onBack = { stateHolder.goBack() },
                                onOpenFeatureHub = { stateHolder.setFeatureHubOpen(true) },
                                onAddHistory = stateHolder::addCalculatorHistoryRecord,
                                onDeleteHistoryItem = stateHolder::deleteCalculatorHistoryRecord,
                                onClearHistory = stateHolder::clearCalculatorHistory
                            )
                        }
                    }
                }
            }

            // Bottom Floating Navigation Dock
            if (!isDetailFullScreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .widthIn(max = 500.dp)
                ) {
                    ChronosBottomNavigation(
                        currentScreen = currentScreen,
                        onSelectScreen = stateHolder::navigateTo,
                        hasActiveAlarm = hasActiveAlarm,
                        appLanguage = settings.appLanguage
                    )
                }
            }

            // Active Scheduled Reminder / Timer Toast Banner
            val banner = activeBanner
            if (banner != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ChronosSurfaceDark.copy(alpha = 0.96f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.85f)),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .widthIn(max = 460.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ChronosAmberNeon.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.4f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = ChronosAmberNeon)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ChronosAmberNeon
                                    ) {
                                        Text(
                                            text = when (banner.kind) {
                                                "timer" -> "TIMER"
                                                "alarm" -> "ALARM SMART"
                                                else -> if (settings.appLanguage == "id") "PENGINGAT" else "REMINDER"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = banner.meta,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = ChronosAmberNeon
                                    )
                                }
                                Text(
                                    text = banner.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = stateHolder::dismissBannerAlert) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF))
                        }
                    }
                }
            }
        }

        // Feature Discovery Hub Slide-Out Drawer
        FeatureHubDrawer(
            isOpen = isFeatureHubOpen,
            onClose = { stateHolder.setFeatureHubOpen(false) },
            onSelectScreen = stateHolder::navigateTo,
            onOpenLifePillar = stateHolder::openLifePillarFromHub,
            appLanguage = settings.appLanguage,
            hasActiveAlarm = hasActiveAlarm
        )

        // Full-Screen Ringing Alarm Activity Simulation Modal
        val activeRing = ringingAlarm
        if (activeRing != null) {
            AlarmActivityModal(
                alarm = activeRing,
                weatherData = currentWeatherData,
                appLanguage = settings.appLanguage,
                snoozeDuration = settings.snoozeDuration,
                temperatureUnit = settings.temperatureUnit,
                defaultWakeMission = settings.defaultWakeMission,
                onDismiss = stateHolder::dismissRingingAlarm,
                onSnooze = stateHolder::snoozeRingingAlarm,
                onUpdateTheme = { themeId -> stateHolder.updateAlarmTheme(activeRing.id, themeId) }
            )
        }
    }
}
