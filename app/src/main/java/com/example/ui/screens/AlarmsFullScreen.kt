package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AlarmItem
import com.example.model.CHRONOS_RINGTONES
import com.example.model.CHRONOS_VERSION
import com.example.model.ChronosTranslations
import com.example.model.WakeMission
import com.example.model.WeatherData
import com.example.model.calculateSleepCycleTelemetry
import com.example.model.formatAlarmRemaining
import com.example.model.formatAlarmTargetDateTime
import com.example.model.getNextAlarmOccurrenceMillis
import com.example.service.ChronosAlarmScheduler
import com.example.service.ChronosAudioSynthesizer
import com.example.ui.components.ChronosAmberNeon
import com.example.ui.components.ChronosBgDeep
import com.example.ui.components.ChronosCyanNeon
import com.example.ui.components.ChronosGreenNeon
import com.example.ui.components.ChronosRoseNeon
import androidx.compose.foundation.border
import com.example.ui.components.ChronosSurfaceDark
import com.example.ui.components.AlarmBackgroundThemes
import com.example.ui.components.AlarmRealisticBackgroundCanvas
import com.example.ui.components.AlarmThemeSelectorPills
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun AlarmsFullScreen(
    nowMillis: Long,
    alarms: List<AlarmItem>,
    appLanguage: String,
    is24HourFormat: Boolean,
    alarmFadeInDuration: Int,
    defaultWakeMission: WakeMission,
    defaultRingtone: Int = 0,
    defaultVibrate: Boolean = true,
    sleepCycleTarget: Int = 5,
    defaultAlarmBackgroundTheme: String = "COSMIC_DAWN",
    onAddAlarm: (AlarmItem) -> Unit,
    onUpdateAlarm: (AlarmItem) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
    onTestRingAlarm: (AlarmItem) -> Unit
) {
    val isId = appLanguage == "id"
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }
    var notificationSyncStatus by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        ChronosAlarmScheduler.syncAlarmsWithSystem(context, alarms, notificationsEnabled = true, isId = isId)
        notificationSyncStatus = if (granted) {
            if (isId) "✓ Izin Notifikasi Aktif • Notifikasi Latar Belakang Tersinkron" else "✓ Notification Permission Active • Background Notification Synced"
        } else {
            if (isId) "✓ Alarm Latar Belakang Dijadwalkan Presisi" else "✓ Background Exact Alarm Scheduled"
        }
    }

    val daysLabels = ChronosTranslations.daysList(appLanguage)

    val nextAlarmInfo = remember(alarms, nowMillis / 1000L) {
        var bestAlarm: AlarmItem? = null
        var bestMillis: Long? = null
        for (a in alarms) {
            if (!a.isEnabled) continue
            val occ = getNextAlarmOccurrenceMillis(a, nowMillis)
            if (occ != null && (bestMillis == null || occ < bestMillis)) {
                bestMillis = occ
                bestAlarm = a
            }
        }
        if (bestAlarm == null || bestMillis == null) null else {
            val diffSec = max(0L, (bestMillis - nowMillis) / 1000L).toInt()
            val h = diffSec / 3600
            val m = (diffSec % 3600) / 60
            val s = diffSec % 60
            val cycles = "%.1f".format(Locale.US, (diffSec / 60.0) / 90.0)
            Quadruple(bestAlarm, h, m, s to cycles)
        }
    }

    fun createQuickSmartAlarm(offsetMinutes: Int, presetLabel: String, mission: WakeMission) {
        val dt = Calendar.getInstance().apply {
            timeInMillis = nowMillis + offsetMinutes * 60_000L
        }
        val h = dt.get(Calendar.HOUR_OF_DAY)
        val m = dt.get(Calendar.MINUTE)
        val cycles = max(1, (offsetMinutes / 90.0).roundToInt())
        val quote = if (isId) {
            "Disetel otomatis untuk pemulihan optimal (${if (offsetMinutes >= 90) "$cycles siklus REM" else "$offsetMinutes menit Power Nap"})."
        } else {
            "Auto-aligned for restorative recovery (${if (offsetMinutes >= 90) "$cycles REM cycles" else "${offsetMinutes}m Power Nap"})."
        }
        onAddAlarm(
            AlarmItem(
                id = System.currentTimeMillis(),
                hour = h,
                minute = m,
                label = "$presetLabel|||$quote",
                daysOfWeek = 0,
                dateMillis = 0L,
                isVibrate = defaultVibrate,
                soundUri = CHRONOS_RINGTONES.getOrElse(defaultRingtone) { "Vitality Spirit" },
                isEnabled = true,
                isSnooze = true,
                wakeMission = mission,
                fadeInSeconds = alarmFadeInDuration,
                preWakeGentle = true
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("alarms_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            text = ChronosTranslations.t(appLanguage, "alarms").uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp,
                            color = ChronosCyanNeon
                        )
                        Text(
                            text = "SMART CIRCADIAN ENGINE • REAL-TIME TELEMETRY",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ChronosCyanNeon.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${alarms.count { it.isEnabled }} ${ChronosTranslations.t(appLanguage, "active").lowercase()}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = Color(0xFF67E8F9),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Real-Time Next Alarm & Circadian Sleep HUD
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ChronosSurfaceDark.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (nextAlarmInfo != null) {
                            val (alarm, h, m, pair) = nextAlarmInfo
                            val (s, cycles) = pair
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
                                            .background(ChronosGreenNeon)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isId) "ALARM AKTIF BERIKUTNYA (REAL-TIME)" else "NEXT ACTIVE ALARM (REAL-TIME)",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosGreenNeon
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ChronosAmberNeon.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "~$cycles ${if (isId) "Siklus REM" else "REM Cycles"}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = ChronosAmberNeon,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alarm.label.split("|||").firstOrNull().orEmpty(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val soundDisplay = if (ChronosAudioSynthesizer.isCustomDeviceUri(alarm.soundUri)) {
                                        "🎵 ${alarm.customAudioTitle.ifBlank { if (isId) "Musik Perangkat" else "Device Music" }}"
                                    } else {
                                        alarm.soundUri
                                    }
                                    Text(
                                        text = "%02d:%02d • %s".format(alarm.hour, alarm.minute, soundDisplay),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = Color(0xFF9CA3AF),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "%02d:%02d:%02d".format(h, m, s),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Light,
                                        fontSize = 24.sp,
                                        color = Color(0xFF67E8F9)
                                    )
                                    Text(
                                        text = formatAlarmTargetDateTime(alarm, nowMillis, isId),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ChronosGreenNeon
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Live Smart Animated Background Notification Preview Card
                            val totalWaitSec = max(1, h * 3600 + m * 60 + s)
                            val progressFraction = (1f - (totalWaitSec.coerceAtMost(8 * 3600) / (8f * 3600f))).coerceIn(0.08f, 1f)
                            val animPhase = (nowMillis % 6000L) / 6000f * (2f * Math.PI.toFloat())

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF060B16),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.28f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("live_background_alarm_notification_card")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = ChronosCyanNeon,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isId) "NOTIFIKASI ALARM LATAR BELAKANG LIVE" else "LIVE BACKGROUND ALARM NOTIFICATION",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ChronosCyanNeon
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ChronosGreenNeon.copy(alpha = 0.14f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                text = if (isId) "EXACT WAKE • SIAGA PENUH" else "EXACT WAKE • ARMED",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ChronosGreenNeon,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Smart Real-Time Harmonic Wave & Circadian Pulse Animation Canvas
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF0A1122)),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val w = size.width
                                            val ch = size.height
                                            val midY = ch / 2f
                                            val points = 36
                                            val stepX = w / points.toFloat()
                                            for (i in 0 until points) {
                                                val x1 = i * stepX
                                                val x2 = (i + 1) * stepX
                                                val angle1 = animPhase + i * 0.32f
                                                val angle2 = animPhase + (i + 1) * 0.32f
                                                val y1 = midY + sin(angle1) * (ch * 0.28f)
                                                val y2 = midY + sin(angle2) * (ch * 0.28f)
                                                val y1b = midY + cos(angle1 * 0.7f) * (ch * 0.18f)
                                                val y2b = midY + cos(angle2 * 0.7f) * (ch * 0.18f)
                                                drawLine(
                                                    color = ChronosCyanNeon.copy(alpha = 0.55f),
                                                    start = Offset(x1, y1),
                                                    end = Offset(x2, y2),
                                                    strokeWidth = 2.5f
                                                )
                                                drawLine(
                                                    color = ChronosGreenNeon.copy(alpha = 0.35f),
                                                    start = Offset(x1, y1b),
                                                    end = Offset(x2, y2b),
                                                    strokeWidth = 1.8f
                                                )
                                            }
                                            // Progress indicator dot
                                            val dotX = (w * progressFraction).coerceIn(12f, w - 12f)
                                            val dotY = midY + sin(animPhase + (dotX / stepX) * 0.32f) * (ch * 0.28f)
                                            drawCircle(
                                                color = ChronosGreenNeon.copy(alpha = 0.25f),
                                                radius = 12f,
                                                center = Offset(dotX, dotY)
                                            )
                                            drawCircle(
                                                color = ChronosGreenNeon,
                                                radius = 5f,
                                                center = Offset(dotX, dotY)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isId) {
                                                    "🔔 ${alarm.label.split("|||").firstOrNull()?.ifBlank { "Alarm Smart" } ?: "Alarm Smart"} • ${formatAlarmTargetDateTime(alarm, nowMillis, true)}"
                                                } else {
                                                    "🔔 ${alarm.label.split("|||").firstOrNull()?.ifBlank { "Smart Alarm" } ?: "Smart Alarm"} • ${formatAlarmTargetDateTime(alarm, nowMillis, false)}"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.Black.copy(alpha = 0.65f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f))
                                            ) {
                                                Text(
                                                    text = "T-%02d:%02d:%02d".format(h, m, s),
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ChronosCyanNeon,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Smart Multi-Channel & Battery Efficiency Telemetry Strip
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ChronosCyanNeon.copy(alpha = 0.10f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.25f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (isId) "⚡ 0% Idle Drain (Chronometer)" else "⚡ 0% Idle Drain (Chronometer)",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 8.sp,
                                                color = Color(0xFF67E8F9),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (alarm.preWakeGentle) ChronosAmberNeon.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (alarm.preWakeGentle) ChronosAmberNeon.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (alarm.preWakeGentle) {
                                                    if (isId) "🌅 Pra-Bangun REM Aktif" else "🌅 Pre-Wake REM Armed"
                                                } else {
                                                    if (isId) "🌅 Pra-Bangun Nonaktif" else "🌅 Pre-Wake Off"
                                                },
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 8.sp,
                                                color = if (alarm.preWakeGentle) ChronosAmberNeon else Color(0xFF9CA3AF),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notificationSyncStatus ?: if (isId) {
                                                "Aktif di latar belakang dengan AlarmClockInfo & Chronometer Realtime"
                                            } else {
                                                "Active in background with AlarmClockInfo & Realtime Chronometer"
                                            },
                                            fontSize = 10.sp,
                                            color = Color(0xFF9CA3AF),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        if (alarm.daysOfWeek != 0 && alarm.dateMillis == 0L) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = ChronosAmberNeon.copy(alpha = 0.14f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.4f)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val occMs = getNextAlarmOccurrenceMillis(alarm, System.currentTimeMillis())
                                                        if (occMs != null) {
                                                            ChronosAlarmScheduler.markUpcomingOccurrenceSkipped(alarm.id, occMs)
                                                            ChronosAlarmScheduler.syncAlarmsWithSystem(context, alarms, notificationsEnabled = true, isId = isId)
                                                            notificationSyncStatus = if (isId) {
                                                                "✓ Siklus berikutnya (%02d:%02d) dilewati; dijadwalkan ke hari berikutnya".format(alarm.hour, alarm.minute)
                                                            } else {
                                                                "✓ Next occurrence (%02d:%02d) skipped; armed for next cycle".format(alarm.hour, alarm.minute)
                                                            }
                                                        }
                                                    }
                                                    .testTag("skip_next_alarm_occurrence_button")
                                            ) {
                                                Text(
                                                    text = if (isId) "Lewati 1x" else "Skip 1x",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ChronosAmberNeon,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ChronosCyanNeon.copy(alpha = 0.16f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.45f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                                    } else {
                                                        ChronosAlarmScheduler.syncAlarmsWithSystem(context, alarms, notificationsEnabled = true, isId = isId)
                                                        notificationSyncStatus = if (isId) {
                                                            "✓ Notifikasi Alarm Latar Belakang Disegarkan (${"%02d:%02d".format(alarm.hour, alarm.minute)})"
                                                        } else {
                                                            "✓ Background Alarm Notification Refreshed (${"%02d:%02d".format(alarm.hour, alarm.minute)})"
                                                        }
                                                    }
                                                }
                                                .testTag("sync_live_alarm_notification_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = null,
                                                    tint = ChronosCyanNeon,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isId) "Segarkan Notifikasi" else "Sync Notification",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ChronosCyanNeon
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isId) "Belum Ada Alarm Aktif" else "No Active Alarms",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isId) "Pilih preset cepat di bawah atau ketuk +" else "Use a quick smart preset below or tap +",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isId) "PRESET SMART ALARM INSTAN:" else "INSTANT SMART ALARM PRESETS:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF67E8F9)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickAlarmPresetButton(
                                tag = "+20m",
                                subtitle = "Power Nap",
                                tagColor = ChronosCyanNeon,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    createQuickSmartAlarm(20, if (isId) "Power Nap Segar" else "Power Nap", WakeMission.NONE)
                                }
                            )
                            QuickAlarmPresetButton(
                                tag = "+90m",
                                subtitle = if (isId) "1 Siklus REM" else "1 REM Cycle",
                                tagColor = ChronosAmberNeon,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    createQuickSmartAlarm(90, if (isId) "1 Siklus REM Penuh" else "Full REM Cycle", WakeMission.TAP)
                                }
                            )
                            QuickAlarmPresetButton(
                                tag = "+${"%.1f".format(Locale.US, sleepCycleTarget * 1.5)}h",
                                subtitle = if (isId) "$sleepCycleTarget Siklus Ideal" else "$sleepCycleTarget Cycles Ideal",
                                tagColor = ChronosGreenNeon,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    createQuickSmartAlarm(sleepCycleTarget * 90, if (isId) "Restorasi $sleepCycleTarget Siklus" else "$sleepCycleTarget-Cycle Optimal", WakeMission.MATH)
                                }
                            )
                        }
                    }
                }
            }

            // Alarms List
            items(alarms, key = { it.id }) { alarm ->
                val parts = alarm.label.split("|||")
                val labelText = parts.firstOrNull().orEmpty().ifBlank { "Alarm" }
                val motivationText = parts.getOrNull(1).orEmpty()

                val mainTime = if (is24HourFormat) {
                    "%02d:%02d".format(alarm.hour, alarm.minute)
                } else {
                    val h12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
                    "%02d:%02d".format(h12, alarm.minute)
                }
                val suffix = if (is24HourFormat) "" else if (alarm.hour >= 12) "PM" else "AM"

                val missionBadge = when {
                    alarm.wakeMission != WakeMission.NONE -> alarm.wakeMission.name
                    defaultWakeMission != WakeMission.NONE -> defaultWakeMission.name
                    else -> null
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ChronosSurfaceDark.copy(alpha = if (alarm.isEnabled) 0.9f else 0.45f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (alarm.isEnabled) ChronosCyanNeon.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            editingAlarm = alarm
                            showDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = mainTime,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 36.sp,
                                    color = Color.White
                                )
                                if (suffix.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = suffix,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ChronosCyanNeon,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = labelText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE5E7EB)
                                )
                                if (alarm.daysOfWeek > 0) {
                                    val activeDays = daysLabels.filterIndexed { idx, _ ->
                                        (alarm.daysOfWeek and (1 shl idx)) != 0
                                    }.joinToString(", ")
                                    Text(
                                        text = " • $activeDays",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                            }

                            if (motivationText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = ChronosAmberNeon,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = motivationText,
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = ChronosAmberNeon.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (alarm.isEnabled) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ChronosAmberNeon.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "In ${formatAlarmRemaining(alarm, nowMillis)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChronosAmberNeon,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                val isCustomMusic = ChronosAudioSynthesizer.isCustomDeviceUri(alarm.soundUri)
                                val soundBadgeLabel = if (isCustomMusic) {
                                    "🎵 ${alarm.customAudioTitle.ifBlank { if (isId) "Musik Kustom" else "Custom Audio" }}"
                                } else {
                                    alarm.soundUri
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCustomMusic) ChronosGreenNeon.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isCustomMusic) ChronosGreenNeon.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Text(
                                        text = soundBadgeLabel,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = if (isCustomMusic) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCustomMusic) ChronosGreenNeon else Color(0xFFD1D5DB),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (missionBadge != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ChronosCyanNeon.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "Mission: $missionBadge",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChronosCyanNeon,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                val itemThemeSpec = remember(alarm.backgroundTheme) { AlarmBackgroundThemes.getTheme(alarm.backgroundTheme) }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = itemThemeSpec.primaryColor.copy(alpha = 0.14f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, itemThemeSpec.primaryColor.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "${itemThemeSpec.emoji} ${if (isId) itemThemeSpec.nameId else itemThemeSpec.nameEn}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = itemThemeSpec.primaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Custom Toggle Pill
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (alarm.isEnabled) ChronosCyanNeon else Color(0xFF374151))
                                    .clickable { onUpdateAlarm(alarm.copy(isEnabled = !alarm.isEnabled)) }
                                    .padding(2.dp),
                                contentAlignment = if (alarm.isEnabled) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { onTestRingAlarm(alarm) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("test_ring_alarm_${alarm.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Test Alarm Activity Ring",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteAlarm(alarm.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Alarm",
                                        tint = Color(0xFF9CA3AF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                editingAlarm = null
                showDialog = true
            },
            containerColor = ChronosCyanNeon,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 96.dp)
                .size(56.dp)
                .testTag("fab_add_alarm")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(28.dp))
        }
    }

    if (showDialog) {
        ChronosAlarmDialog(
            initialAlarm = editingAlarm,
            appLanguage = appLanguage,
            nowMillis = nowMillis,
            defaultWakeMission = defaultWakeMission,
            defaultFadeInSeconds = alarmFadeInDuration,
            defaultRingtone = defaultRingtone,
            defaultVibrate = defaultVibrate,
            defaultAlarmBackgroundTheme = defaultAlarmBackgroundTheme,
            onSave = { saved ->
                if (editingAlarm != null) {
                    onUpdateAlarm(saved)
                } else {
                    onAddAlarm(saved)
                }
                showDialog = false
                editingAlarm = null
            },
            onDismiss = {
                showDialog = false
                editingAlarm = null
            }
        )
    }
}

@Composable
private fun QuickAlarmPresetButton(
    tag: String,
    subtitle: String,
    tagColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = tag,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = tagColor
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChronosAlarmDialog(
    initialAlarm: AlarmItem?,
    appLanguage: String,
    nowMillis: Long,
    defaultWakeMission: WakeMission,
    defaultFadeInSeconds: Int,
    defaultRingtone: Int = 0,
    defaultVibrate: Boolean = true,
    defaultAlarmBackgroundTheme: String = "COSMIC_DAWN",
    onSave: (AlarmItem) -> Unit,
    onDismiss: () -> Unit
) {
    val isId = appLanguage == "id"
    var hour by remember { mutableIntStateOf(initialAlarm?.hour ?: 6) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: 30) }
    var label by remember {
        mutableStateOf(
            initialAlarm?.label?.split("|||")?.firstOrNull()
                ?: if (isId) "Bangun Pagi" else "Morning Alarm"
        )
    }
    var daysOfWeek by remember { mutableIntStateOf(initialAlarm?.daysOfWeek ?: 0) }
    var smartAlarmEnabled by remember { mutableStateOf(true) }
    var soundUri by remember {
        mutableStateOf(
            initialAlarm?.soundUri ?: CHRONOS_RINGTONES.getOrElse(defaultRingtone) { "Vitality Spirit" }
        )
    }
    var customAudioTitle by remember {
        mutableStateOf(initialAlarm?.customAudioTitle ?: "")
    }
    var customAudioStatusMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val customMusicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {
                // Ignore if provider does not support persistable URI permissions
            }
            val fileTitle = ChronosAudioSynthesizer.queryDisplayName(context, uri)
            soundUri = uri.toString()
            customAudioTitle = fileTitle
            customAudioStatusMsg = if (isId) {
                "✓ Musik Perangkat Terhubung: $fileTitle"
            } else {
                "✓ Device Music Linked: $fileTitle"
            }
            ChronosAudioSynthesizer.playCustomOrPresetAlarm(
                context = context,
                soundUri = uri.toString(),
                durationSeconds = 6,
                fadeInSeconds = 1
            )
        }
    }

    var wakeMission by remember { mutableStateOf(initialAlarm?.wakeMission ?: defaultWakeMission) }
    var fadeInSeconds by remember { mutableIntStateOf(initialAlarm?.fadeInSeconds ?: defaultFadeInSeconds) }
    var isVibrate by remember { mutableStateOf(initialAlarm?.isVibrate ?: defaultVibrate) }
    var preWakeGentle by remember { mutableStateOf(initialAlarm?.preWakeGentle ?: true) }
    var backgroundTheme by remember { mutableStateOf(initialAlarm?.backgroundTheme ?: defaultAlarmBackgroundTheme) }

    val telemetry = remember(hour, minute, nowMillis / 1000L, isId) {
        calculateSleepCycleTelemetry(hour, minute, nowMillis, isId)
    }

    val aiSuggestion = remember(hour, minute, telemetry.cycles, isId) {
        val h = telemetry.h
        val m = telemetry.m
        val c = telemetry.cycles
        when {
            hour in 4..8 -> if (isId) {
                "Durasi dari sekarang ${h}j ${m}m (~$c siklus REM). Bangun pagi menyelaraskan ritme kortisol alami & semangat hidup."
            } else {
                "Sleep window from now: ${h}h ${m}m (~$c REM cycles). Early wake-up aligns natural cortisol & peak vitality."
            }
            hour >= 21 || hour <= 3 -> if (isId) {
                "Jendela waktu ${h}j ${m}m (~$c siklus REM). Redupkan layar 45 menit sebelum tidur untuk optimasi melatonin."
            } else {
                "Window: ${h}h ${m}m (~$c REM cycles). Dim blue light 45m before sleep for optimal melatonin release."
            }
            else -> if (isId) {
                "Target %02d:%02d (${h}j ${m}m • ~$c siklus). Konsistensi waktu bangun menjaga ritme sirkadian.".format(hour, minute)
            } else {
                "Target %02d:%02d (${h}h ${m}m • ~$c cycles). Consistent wake times stabilize circadian rhythm.".format(hour, minute)
            }
        }
    }

    val daysLabels = ChronosTranslations.daysList(appLanguage)

    Dialog(
        onDismissRequest = {
            ChronosAudioSynthesizer.stopChime()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0E1624),
            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialAlarm != null) ChronosTranslations.t(appLanguage, "edit_alarm")
                            else ChronosTranslations.t(appLanguage, "add_new_alarm"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 2.sp,
                            color = ChronosCyanNeon
                        )
                        Text(
                            text = "SMART ALARM ENGINE V$CHRONOS_VERSION • REAL-TIME REM SYNC",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            color = ChronosGreenNeon
                        )
                    }
                    IconButton(onClick = {
                        ChronosAudioSynthesizer.stopChime()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF9CA3AF))
                    }
                }

                // Time Orbit Readout + Sliders
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "%02d : %02d".format(hour, minute),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Light,
                            fontSize = 42.sp,
                            color = ChronosCyanNeon
                        )
                        Slider(
                            value = hour.toFloat(),
                            onValueChange = { hour = it.toInt() },
                            valueRange = 0f..23f
                        )
                        Slider(
                            value = minute.toFloat(),
                            onValueChange = { minute = it.toInt() },
                            valueRange = 0f..59f
                        )
                    }
                }

                // Live Real-Time Countdown & REM Cycle Calculator
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ChronosSurfaceDark.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isId) "Hitung Mundur Real-Time:" else "Real-Time Countdown:",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "%02dh : %02dm : %02ds".format(telemetry.h, telemetry.m, telemetry.s),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF67E8F9)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isId) "Estimasi Siklus Tidur REM:" else "Estimated REM Sleep Cycles:",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "${telemetry.cycles} ${if (isId) "Siklus" else "Cycles"} • ${telemetry.qualityLabel}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(telemetry.qualityColorHex)
                            )
                        }
                        Text(
                            text = if (isId) "Rekomendasi Jam Bangun Siklus 90m (Jika tidur sekarang):"
                            else "Recommended 90m REM Wake Windows (If sleeping now):",
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            telemetry.recommendedWakeTimes.forEach { rec ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ChronosCyanNeon.copy(alpha = 0.1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            hour = rec.hour
                                            minute = rec.minute
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "%02d:%02d".format(rec.hour, rec.minute),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF67E8F9)
                                        )
                                        Text(
                                            text = rec.label,
                                            fontSize = 8.sp,
                                            color = Color(0xFF9CA3AF),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Alarm Name
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(ChronosTranslations.t(appLanguage, "alarm_name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Repeat Days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ChronosTranslations.t(appLanguage, "repeat_days"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosAmberNeon
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            31 to (if (isId) "Sen-Jum" else "Weekdays"),
                            96 to (if (isId) "Akhir Pekan" else "Weekends"),
                            127 to (if (isId) "Setiap Hari" else "Everyday")
                        ).forEach { (mask, text) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { daysOfWeek = mask }
                            ) {
                                Text(
                                    text = text,
                                    fontSize = 9.sp,
                                    color = Color(0xFFD1D5DB),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    daysLabels.forEachIndexed { idx, day ->
                        val selected = (daysOfWeek and (1 shl idx)) != 0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) ChronosAmberNeon else Color.White.copy(alpha = 0.05f),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { daysOfWeek = daysOfWeek xor (1 shl idx) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else Color(0xFF9CA3AF)
                                )
                            }
                        }
                    }
                }

                // Smart Wake-Up Cognitive Mission
                Text(
                    text = if (isId) "MISI BANGUN CERDAS (ANTI-KETIDURAN)" else "SMART WAKE-UP COGNITIVE MISSION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChronosCyanNeon
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        WakeMission.NONE to (if (isId) "Standar" else "Standard"),
                        WakeMission.MATH to (if (isId) "Matematika" else "Math"),
                        WakeMission.MEMORY to (if (isId) "Memori" else "Memory"),
                        WakeMission.TAP to (if (isId) "Ketuk 5x" else "Tap 5x")
                    ).forEach { (m, lbl) ->
                        val selected = wakeMission == m
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) ChronosCyanNeon else Color.White.copy(alpha = 0.04f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { wakeMission = m }
                        ) {
                            Text(
                                text = lbl,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.Black else Color(0xFFD1D5DB),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                // Alarm Sound Selector + Custom Device File Picker + Preview
                val isCustomSelected = ChronosAudioSynthesizer.isCustomDeviceUri(soundUri)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ChronosTranslations.t(appLanguage, "alarm_sound"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosCyanNeon
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ChronosRoseNeon.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    ChronosAudioSynthesizer.stopChime()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = ChronosRoseNeon, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (isId) "Stop" else "Stop", fontSize = 10.sp, color = ChronosRoseNeon)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ChronosCyanNeon.copy(alpha = 0.15f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    ChronosAudioSynthesizer.playCustomOrPresetAlarm(
                                        context = context,
                                        soundUri = soundUri,
                                        durationSeconds = 5,
                                        fadeInSeconds = 0
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = ChronosCyanNeon, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isId) "Uji Suara" else "Preview", fontSize = 10.sp, color = ChronosCyanNeon)
                            }
                        }
                    }
                }

                // Custom Device Music File Access Card (MP3 / WAV / OGG / FLAC / M4A)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCustomSelected) ChronosGreenNeon.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.04f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCustomSelected) ChronosGreenNeon.copy(alpha = 0.55f) else ChronosCyanNeon.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = if (isCustomSelected) ChronosGreenNeon else ChronosCyanNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isId) "MUSIK ALARM KUSTOM DARI FILE PERANGKAT" else "CUSTOM DEVICE ALARM MUSIC FILE",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCustomSelected) ChronosGreenNeon else ChronosCyanNeon
                                    )
                                    Text(
                                        text = if (isCustomSelected) {
                                            "🎵 ${customAudioTitle.ifBlank { if (isId) "File Audio Perangkat Aktif" else "Device Audio File Active" }}"
                                        } else {
                                            if (isId) "Mendukung MP3, WAV, OGG, FLAC, M4A dari penyimpanan perangkat"
                                            else "Supports MP3, WAV, OGG, FLAC, M4A from device storage"
                                        },
                                        fontSize = 11.sp,
                                        color = if (isCustomSelected) Color.White else Color(0xFF9CA3AF),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ChronosGreenNeon.copy(alpha = 0.20f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        customMusicLauncher.launch(arrayOf("audio/*"))
                                    }
                                    .testTag("pick_custom_alarm_music_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = ChronosGreenNeon, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isId) "Pilih File Musik Perangkat" else "Pick Device Music File",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosGreenNeon
                                    )
                                }
                            }

                            if (isCustomSelected) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            ChronosAudioSynthesizer.stopChime()
                                            soundUri = CHRONOS_RINGTONES.getOrElse(defaultRingtone) { "Vitality Spirit" }
                                            customAudioTitle = ""
                                            customAudioStatusMsg = null
                                        }
                                ) {
                                    Text(
                                        text = if (isId) "Reset Bawaan" else "Reset Preset",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD1D5DB),
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp)
                                    )
                                }
                            }
                        }

                        if (!customAudioStatusMsg.isNullOrBlank()) {
                            Text(
                                text = customAudioStatusMsg!!,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = ChronosGreenNeon
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CHRONOS_RINGTONES.chunked(4).forEach { rowSounds ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowSounds.forEach { snd ->
                                val selected = soundUri == snd
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) ChronosAmberNeon else Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            soundUri = snd
                                            customAudioTitle = ""
                                            customAudioStatusMsg = null
                                            ChronosAudioSynthesizer.playCustomOrPresetAlarm(
                                                context = context,
                                                soundUri = snd,
                                                durationSeconds = 3,
                                                fadeInSeconds = 0
                                            )
                                        }
                                ) {
                                    Text(
                                        text = snd.split(" ").first(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.Black else Color(0xFFD1D5DB),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Smart Alarm Options: Vibrate, Gentle Pre-Wake (10m before), and Audio Fade-In
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isId) "OPSI SMART ALARM & NOTIFIKASI LATAR BELAKANG" else "SMART ALARM & BACKGROUND NOTIFICATION OPTIONS",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosCyanNeon
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isVibrate) ChronosCyanNeon.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isVibrate) ChronosCyanNeon.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isVibrate = !isVibrate }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Smartphone,
                                        contentDescription = null,
                                        tint = if (isVibrate) ChronosCyanNeon else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isId) {
                                            if (isVibrate) "Getar: Aktif" else "Getar: Nonaktif"
                                        } else {
                                            if (isVibrate) "Vibrate: ON" else "Vibrate: OFF"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isVibrate) ChronosCyanNeon else Color(0xFF9CA3AF)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (preWakeGentle) ChronosGreenNeon.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (preWakeGentle) ChronosGreenNeon.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { preWakeGentle = !preWakeGentle }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = if (preWakeGentle) ChronosGreenNeon else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isId) {
                                            if (preWakeGentle) "Pra-Bangun -10m" else "Tanpa Pra-Bangun"
                                        } else {
                                            if (preWakeGentle) "Pre-Wake -10m" else "No Pre-Wake"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (preWakeGentle) ChronosGreenNeon else Color(0xFF9CA3AF)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isId) "Fade-In Volume Bertahap:" else "Gradual Audio Fade-In:",
                                fontSize = 10.sp,
                                color = Color(0xFFD1D5DB)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0, 15, 30, 60).forEach { sec ->
                                    val selectedFade = fadeInSeconds == sec
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selectedFade) ChronosAmberNeon else Color.White.copy(alpha = 0.06f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { fadeInSeconds = sec }
                                    ) {
                                        Text(
                                            text = if (sec == 0) (if (isId) "Instan" else "0s") else "${sec}s",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedFade) Color.Black else Color(0xFFD1D5DB),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Realistic Background Visual Theme & Live Canvas Preview
                val activeThemeSpec = remember(backgroundTheme) { AlarmBackgroundThemes.getTheme(backgroundTheme) }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.03f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeThemeSpec.primaryColor.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeThemeSpec.emoji,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isId) "TEMA LATAR BELAKANG NOTIFIKASI REALISTIS" else "REALISTIC NOTIFICATION BACKGROUND THEME",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeThemeSpec.primaryColor
                                )
                            }
                            Text(
                                text = if (isId) activeThemeSpec.nameId else activeThemeSpec.nameEn,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Live Procedural Canvas Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, activeThemeSpec.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        ) {
                            AlarmRealisticBackgroundCanvas(
                                themeId = backgroundTheme,
                                sunriseIntensity = 0.65f,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = if (isId) activeThemeSpec.descriptionId else activeThemeSpec.descriptionEn,
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Theme selector pills
                        AlarmThemeSelectorPills(
                            selectedThemeId = backgroundTheme,
                            onSelectTheme = { backgroundTheme = it },
                            isId = isId
                        )
                    }
                }

                // AI Sleep Suggestion Box & Live Target Preview
                val previewAlarmItem = remember(hour, minute, daysOfWeek, backgroundTheme) {
                    AlarmItem(
                        id = initialAlarm?.id ?: 0L,
                        hour = hour,
                        minute = minute,
                        isEnabled = true,
                        daysOfWeek = daysOfWeek,
                        dateMillis = 0L,
                        backgroundTheme = backgroundTheme
                    )
                }
                val previewTargetLabel = remember(previewAlarmItem, nowMillis / 1000L, isId) {
                    formatAlarmTargetDateTime(previewAlarmItem, nowMillis, isId)
                }
                val previewRemainingLabel = remember(previewAlarmItem, nowMillis / 1000L) {
                    formatAlarmRemaining(previewAlarmItem, nowMillis)
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.03f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔔 $previewTargetLabel",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosGreenNeon
                            )
                            Text(
                                text = previewRemainingLabel,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosCyanNeon
                            )
                        }
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ChronosAmberNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = aiSuggestion,
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFFD1D5DB)
                            )
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            ChronosAudioSynthesizer.stopChime()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(ChronosTranslations.t(appLanguage, "cancel"), color = ChronosCyanNeon)
                    }
                    Button(
                        onClick = {
                            ChronosAudioSynthesizer.stopChime()
                            val cleanLabel = label.trim().ifBlank { if (isId) "Alarm Cerdas" else "Smart Alarm" }
                            val finalLabel = if (smartAlarmEnabled) "$cleanLabel|||$aiSuggestion" else cleanLabel
                            onSave(
                                AlarmItem(
                                    id = initialAlarm?.id ?: System.currentTimeMillis(),
                                    hour = hour,
                                    minute = minute,
                                    isEnabled = true,
                                    label = finalLabel,
                                    daysOfWeek = daysOfWeek,
                                    dateMillis = 0L,
                                    isVibrate = isVibrate,
                                    soundUri = soundUri,
                                    customAudioTitle = customAudioTitle,
                                    isSnooze = true,
                                    wakeMission = wakeMission,
                                    fadeInSeconds = fadeInSeconds,
                                    preWakeGentle = preWakeGentle,
                                    backgroundTheme = backgroundTheme
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChronosCyanNeon),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = ChronosTranslations.t(appLanguage, "save_alarm"),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmActivityModal(
    alarm: AlarmItem,
    weatherData: WeatherData?,
    appLanguage: String,
    snoozeDuration: Int,
    temperatureUnit: Int,
    defaultWakeMission: WakeMission,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onUpdateTheme: ((String) -> Unit)? = null
) {
    val isId = appLanguage == "id"
    var currentTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var fadeProgress by remember { mutableIntStateOf(20) }
    var isMuted by remember { mutableStateOf(false) }

    val activeMission = if (alarm.wakeMission != WakeMission.NONE) alarm.wakeMission else defaultWakeMission
    var showMissionModal by remember { mutableStateOf(false) }

    val mathA = remember { Random.nextInt(12, 26) }
    val mathB = remember { Random.nextInt(7, 21) }
    var mathInput by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }

    val memoryTargets = remember {
        val set = mutableSetOf<Int>()
        while (set.size < 3) set.add(Random.nextInt(0, 9))
        set.toList()
    }
    val memorySelected = remember { mutableStateListOf<Int>() }
    var tapsDone by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val activeSoundDisplay = if (ChronosAudioSynthesizer.isCustomDeviceUri(alarm.soundUri)) {
        "🎵 ${alarm.customAudioTitle.ifBlank { if (isId) "Musik Kustom Perangkat" else "Custom Device Music" }}"
    } else {
        alarm.soundUri
    }

    DisposableEffect(alarm.id) {
        val recentlyStartedByReceiver = ChronosAudioSynthesizer.isPlayingAlarmChime &&
            (System.currentTimeMillis() - ChronosAlarmScheduler.lastReceiverAudioTriggerMs) < 3500L
        val stopper = if (recentlyStartedByReceiver) {
            { ChronosAudioSynthesizer.stopChime() }
        } else {
            ChronosAudioSynthesizer.playCustomOrPresetAlarm(
                context = context,
                soundUri = alarm.soundUri,
                durationSeconds = 60,
                fadeInSeconds = alarm.fadeInSeconds
            )
        }
        onDispose {
            stopper()
        }
    }

    LaunchedEffect(alarm.id) {
        val start = System.currentTimeMillis()
        val fadeSecs = max(1, alarm.fadeInSeconds)
        var tickCount = 0
        while (true) {
            currentTimeMs = System.currentTimeMillis()
            val elapsedSec = (currentTimeMs - start) / 1000f
            fadeProgress = ((elapsedSec / fadeSecs) * 100f).roundToInt().coerceIn(10, 100)
            if (alarm.isVibrate && !isMuted && tickCount % 6 == 0) {
                ChronosAudioSynthesizer.triggerHaptic(context, 3)
            }
            tickCount++
            delay(250L)
        }
    }

    val parts = alarm.label.split("|||")
    val alarmLabel = parts.firstOrNull().orEmpty().ifBlank { if (isId) "Alarm Pagi" else "Morning Alarm" }
    val aiMotivation = parts.getOrNull(1).orEmpty()

    val cal = remember(currentTimeMs / 1000L) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMs }
    }
    val timeStr = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    val secStr = "%02d".format(cal.get(Calendar.SECOND))
    val dateStr = remember(currentTimeMs / 60_000L, isId) {
        SimpleDateFormat("EEEE, d MMMM yyyy", if (isId) Locale.forLanguageTag("id-ID") else Locale.US)
            .format(Date(currentTimeMs))
    }

    var currentThemeId by remember(alarm.backgroundTheme) { mutableStateOf(alarm.backgroundTheme) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030D22))
            .testTag("alarm_activity_modal")
    ) {
        // Smart Procedural Realistic Canvas (Fajar Kosmis, Hujan Ambien, Bintang Sirkadian, Hutan Fajar, Cyber Aurora)
        AlarmRealisticBackgroundCanvas(
            themeId = currentThemeId,
            sunriseIntensity = (fadeProgress / 100f).coerceIn(0.15f, 1f),
            isEcoMode = false,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Audio Bar & Real-Time Theme Controls
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeSpec = remember(currentThemeId) { AlarmBackgroundThemes.getTheme(currentThemeId) }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = activeSpec.primaryColor.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, activeSpec.primaryColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${activeSpec.emoji} ${activeSoundDisplay} • $fadeProgress% VOL",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            if (isMuted) {
                                ChronosAudioSynthesizer.stopChime()
                            } else {
                                ChronosAudioSynthesizer.playCustomOrPresetAlarm(
                                    context = context,
                                    soundUri = alarm.soundUri,
                                    durationSeconds = 45,
                                    fadeInSeconds = 2
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute Chime",
                            tint = if (isMuted) ChronosRoseNeon else ChronosCyanNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Real-Time Realistic Background Theme Selector Strip
                AlarmThemeSelectorPills(
                    selectedThemeId = currentThemeId,
                    onSelectTheme = { newTheme ->
                        currentThemeId = newTheme
                        onUpdateTheme?.invoke(newTheme)
                    },
                    isId = isId
                )
            }

            // Center Readout
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${ChronosTranslations.t(appLanguage, "alarms").uppercase()} • LIVE WAKE TELEMETRY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = ChronosCyanNeon
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeStr,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 68.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ":$secStr",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 24.sp,
                        color = ChronosCyanNeon,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Text(
                    text = alarmLabel,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = dateStr,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(modifier = Modifier.width(240.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isId) "Intensitas Suara & Fajar" else "Sunrise & Audio Ramp",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = ChronosAmberNeon
                        )
                        Text(
                            text = "$fadeProgress%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = ChronosAmberNeon
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { fadeProgress / 100f },
                        color = ChronosAmberNeon,
                        trackColor = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }

                if (aiMotivation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = ChronosAmberNeon.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChronosAmberNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "\"$aiMotivation\"",
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFFFDE68A)
                            )
                        }
                    }
                }

                if (weatherData != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${weatherData.locationName.split(",").first()}: ${ChronosTranslations.t(appLanguage, weatherData.description)} • ${
                                if (temperatureUnit == 1) "${((weatherData.temp * 9.0) / 5.0 + 32.0).roundToInt()}°F"
                                else "${weatherData.temp.roundToInt()}°C"
                            }",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ChronosCyanNeon.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSnooze() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, tint = ChronosCyanNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isId) "Goyang HP untuk Snooze" else "Shake device to Snooze",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF67E8F9)
                        )
                    }
                }
            }

            // Bottom Snooze & Dismiss Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onSnooze() }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ChronosTranslations.t(appLanguage, "snooze").uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$snoozeDuration ${if (isId) "MNT" else "MIN"}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ChronosCyanNeon.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            if (activeMission == WakeMission.NONE) {
                                onDismiss()
                            } else {
                                showMissionModal = true
                            }
                        }
                        .testTag("dismiss_alarm_button")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = ChronosCyanNeon, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ChronosTranslations.t(appLanguage, "dismiss").uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (activeMission != WakeMission.NONE) "Mission: ${activeMission.name}"
                            else if (isId) "Matikan" else "Turn Off",
                            fontSize = 11.sp,
                            color = Color(0xFF67E8F9)
                        )
                    }
                }
            }
        }

        // Cognitive Wake Verification Modal
        if (showMissionModal) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF0E1624),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isId) "VERIFIKASI KESADARAN PENUH" else "COGNITIVE WAKE VERIFICATION",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosCyanNeon
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        when (activeMission) {
                            WakeMission.MATH -> {
                                Text(
                                    text = if (isId) "Selesaikan Soal untuk Mematikan Alarm" else "Solve to Dismiss Alarm",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "$mathA + $mathB = ?",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 30.sp,
                                    color = ChronosCyanNeon
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = mathInput,
                                    onValueChange = {
                                        mathInput = it
                                        mathError = false
                                    },
                                    label = { Text(if (isId) "Jawaban..." else "Answer...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (mathError) {
                                    Text(
                                        text = if (isId) "Jawaban belum tepat, coba lagi!" else "Incorrect answer, try again!",
                                        fontSize = 12.sp,
                                        color = ChronosRoseNeon,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        if (mathInput.trim().toIntOrNull() == mathA + mathB) {
                                            onDismiss()
                                        } else {
                                            mathError = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ChronosCyanNeon),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isId) "Verifikasi & Matikan" else "Verify & Dismiss",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            WakeMission.MEMORY -> {
                                Text(
                                    text = if (isId) "Ketuk 3 Kotak Berkilau Cyan" else "Tap the 3 Highlighted Cyan Nodes",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (row in 0 until 3) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (col in 0 until 3) {
                                                val idx = row * 3 + col
                                                val isTarget = memoryTargets.contains(idx)
                                                val isPicked = memorySelected.contains(idx)
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = when {
                                                        isPicked -> ChronosGreenNeon
                                                        isTarget -> ChronosCyanNeon.copy(alpha = 0.25f)
                                                        else -> Color.White.copy(alpha = 0.05f)
                                                    },
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isTarget) ChronosCyanNeon else Color.White.copy(alpha = 0.1f)
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(56.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .clickable {
                                                            if (isTarget && !isPicked) {
                                                                memorySelected.add(idx)
                                                                if (memorySelected.size >= memoryTargets.size) {
                                                                    onDismiss()
                                                                }
                                                            }
                                                        }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = if (isPicked) "✓" else if (isTarget) "★" else "•",
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isPicked) Color.Black else ChronosCyanNeon
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = if (isId) "Ketuk Tombol Energi 5 Kali" else "Tap the Energy Core 5 Times",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(ChronosCyanNeon, ChronosAmberNeon)
                                            )
                                        )
                                        .clickable {
                                            val next = tapsDone + 1
                                            tapsDone = next
                                            if (next >= 5) {
                                                onDismiss()
                                            }
                                        }
                                ) {
                                    Text(
                                        text = "${5 - tapsDone}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ChronosTranslations.t(appLanguage, "cancel"),
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF),
                                modifier = Modifier.clickable { showMissionModal = false }
                            )
                            Text(
                                text = if (isId) "Matikan Darurat" else "Emergency Dismiss",
                                fontSize = 12.sp,
                                color = ChronosAmberNeon,
                                modifier = Modifier.clickable { onDismiss() }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
