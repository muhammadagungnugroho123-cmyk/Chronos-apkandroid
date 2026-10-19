package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.SystemClock
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChronosTranslations
import com.example.model.StopwatchSessionRecord
import com.example.service.ChronosAudioSynthesizer
import com.example.ui.components.ChronosAmberNeon
import com.example.ui.components.ChronosBgDeep
import com.example.ui.components.ChronosCyanNeon
import com.example.ui.components.ChronosGreenNeon
import com.example.ui.components.ChronosPurpleNeon
import com.example.ui.components.ChronosRoseNeon
import com.example.ui.components.ChronosSurfaceDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Persistent In-Memory Engine for Multi-Track 4-Channel Stopwatch and HIIT Interval Coach
 * so switching tabs (Clock / Weather / Alarms) NEVER resets running tracks or HIIT workouts.
 */
object PersistentStopwatchSubEngines {
    class MultiTrackChannelState(
        val id: Int,
        initialLabel: String,
        val accentColor: Color
    ) {
        var label by mutableStateOf(initialLabel)
        var isRunning by mutableStateOf(false)
        var accumulatedMs by mutableLongStateOf(0L)
        var startRealtimeMs by mutableLongStateOf(0L)
        var displayMs by mutableLongStateOf(0L)
        val laps = mutableStateListOf<Long>()

        fun start() {
            if (!isRunning) {
                startRealtimeMs = SystemClock.elapsedRealtime()
                isRunning = true
            }
        }

        fun pause() {
            if (isRunning) {
                accumulatedMs += (SystemClock.elapsedRealtime() - startRealtimeMs)
                displayMs = accumulatedMs
                isRunning = false
            }
        }

        fun toggle() {
            if (isRunning) pause() else start()
        }

        fun recordLap() {
            val cur = if (isRunning) accumulatedMs + (SystemClock.elapsedRealtime() - startRealtimeMs) else accumulatedMs
            if (cur > 0L) {
                laps.add(0, cur)
            }
        }

        fun reset() {
            isRunning = false
            accumulatedMs = 0L
            startRealtimeMs = 0L
            displayMs = 0L
            laps.clear()
        }

        fun tick(nowRealtime: Long) {
            if (isRunning) {
                displayMs = max(0L, accumulatedMs + (nowRealtime - startRealtimeMs))
            }
        }
    }

    val tracks = listOf(
        MultiTrackChannelState(1, "Lintasan 1 • Atlet Utama", ChronosCyanNeon),
        MultiTrackChannelState(2, "Lintasan 2 • Interval / Rival", ChronosAmberNeon),
        MultiTrackChannelState(3, "Lintasan 3 • Proyek / Tugas", ChronosGreenNeon),
        MultiTrackChannelState(4, "Lintasan 4 • Cadangan / Pit", ChronosPurpleNeon)
    )

    var selectedMode by mutableIntStateOf(0)
    var selectedActivityTag by mutableStateOf("Lari & Atletik")
    var targetLapSeconds by mutableIntStateOf(30)
    var selectedDistanceMeters by mutableIntStateOf(400) // 100m, 400m, 1000m

    // HIIT Coach State
    var hiitWorkSeconds by mutableIntStateOf(30)
    var hiitRestSeconds by mutableIntStateOf(15)
    var hiitTotalRounds by mutableIntStateOf(8)
    var hiitRunning by mutableStateOf(false)
    var hiitCurrentRound by mutableIntStateOf(1)
    var hiitIsWorkPhase by mutableStateOf(true)
    var hiitPhaseRemainingSec by mutableIntStateOf(30)
    var hiitTotalElapsedSec by mutableIntStateOf(0)
    var hiitLastTickRealtime by mutableLongStateOf(0L)

    // v1.66.98 Smart Stopwatch Menu & Lab State
    var smartMenuExpanded by mutableStateOf(true)
    var autoLapIntervalSec by mutableIntStateOf(0) // 0=Off, 15, 30, 60, 120
    var lastAutoLapBucket by mutableLongStateOf(0L)
    var cadenceBpm by mutableIntStateOf(0) // 0=Off, 120, 150, 170, 180
    var bestReactionMs by mutableLongStateOf(0L)
    var lastReactionMs by mutableLongStateOf(0L)
}

@Composable
fun TimersFullScreen(
    totalSeconds: Int,
    remainingSeconds: Int,
    isRunning: Boolean,
    isCompleted: Boolean,
    appLanguage: String,
    onSetDuration: (Int) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDismissCompleted: () -> Unit,
    onAiCommand: (String) -> Unit = {}
) {
    val isId = appLanguage == "id"
    val context = LocalContext.current
    val onStart: (Int?) -> Unit = { dur ->
        if (dur != null) onSetDuration(dur)
        onStartOrResume()
    }
    val onResume: () -> Unit = onStartOrResume
    var customMinInput by remember { mutableStateOf("") }
    var customSecInput by remember { mutableStateOf("") }
    var activeSoundscape by remember { mutableStateOf(ChronosAudioSynthesizer.currentSoundType) }

    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val h = remainingSeconds / 3600
    val m = (remainingSeconds % 3600) / 60
    val s = remainingSeconds % 60
    val formattedTime = if (h > 0) {
        "%02d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }

    val presets = listOf(
        Triple(60, "1m", if (isId) "Peregangan" else "Stretch"),
        Triple(180, "3m", if (isId) "Seduh Teh" else "Brew Tea"),
        Triple(300, "5m", if (isId) "fokus Cepat" else "Quick Focus"),
        Triple(600, "10m", if (isId) "Meditasi" else "Meditation"),
        Triple(900, "15m", if (isId) "Baca Buku" else "Reading"),
        Triple(1200, "20m", "Power Nap"),
        Triple(1500, "25m", "Pomodoro"),
        Triple(2700, "45m", "Deep Work"),
        Triple(3600, "60m", if (isId) "Olahraga" else "Workout")
    )

    val soundscapes = listOf(
        "NONE" to (if (isId) "Hening" else "Off"),
        "FOCUS_BINAURAL" to "Alpha 40Hz",
        "RAIN" to (if (isId) "Hujan" else "Rain"),
        "OCEAN_WAVES" to (if (isId) "Ombak" else "Ocean"),
        "MORNING_BIRDS" to (if (isId) "Alam" else "Forest")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("timers_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = ChronosTranslations.t(appLanguage, "timers").uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp,
                        color = ChronosCyanNeon
                    )
                    Text(
                        text = if (isId) "MESIN HITUNG MUNDUR PRESISI • AUDIO FOKUS"
                        else "PRECISION COUNTDOWN ENGINE • FOCUS AUDIO",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isRunning) ChronosGreenNeon.copy(alpha = 0.14f) else ChronosCyanNeon.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRunning) ChronosGreenNeon.copy(alpha = 0.4f) else ChronosCyanNeon.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (isRunning) (if (isId) "BERJALAN" else "RUNNING")
                        else if (isCompleted) (if (isId) "SELESAI!" else "FINISHED!")
                        else (if (isId) "SIAP" else "READY"),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) ChronosGreenNeon else ChronosCyanNeon,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (isCompleted) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = ChronosAmberNeon.copy(alpha = 0.16f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ChronosAmberNeon),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = ChronosAmberNeon,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isId) "WAKTU TIMER HABIS!" else "TIMER COMPLETED!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isId) "Sesi fokus telah dicatat ke statistik harian."
                                    else "Focus session recorded to your daily stats.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFE5E7EB)
                                )
                            }
                        }
                        Button(
                            onClick = onDismissCompleted,
                            colors = ButtonDefaults.buttonColors(containerColor = ChronosAmberNeon),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isId) "Matikan" else "Dismiss",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Orbital Countdown Dial
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = ChronosSurfaceDark.copy(alpha = 0.90f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW = 10.dp.toPx()
                            val diameter = size.minDimension - strokeW * 2
                            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                            drawArc(
                                color = Color.White.copy(alpha = 0.08f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeW)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(ChronosCyanNeon, ChronosGreenNeon, ChronosCyanNeon)
                                ),
                                startAngle = -90f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formattedTime,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (h > 0) 32.sp else 42.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(progress * 100).roundToInt()}% • ${totalSeconds / 60}m ${totalSeconds % 60}s",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = ChronosCyanNeon
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Time Adjustment Chips (+30s, +1m, +5m)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            30 to "+30s",
                            60 to "+1m",
                            300 to "+5m"
                        ).forEach { (addSec, label) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val nextTotal = remainingSeconds + addSec
                                        if (isRunning) {
                                            onStart(nextTotal)
                                        } else {
                                            onSetDuration(nextTotal)
                                        }
                                    }
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChronosCyanNeon,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Controls: Play/Pause & Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    ChronosAudioSynthesizer.triggerHaptic(context, 1)
                                    onReset()
                                }
                                .testTag("timer_reset_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ChronosTranslations.t(appLanguage, "reset"),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isRunning) ChronosAmberNeon else ChronosCyanNeon,
                            modifier = Modifier
                                .weight(1.4f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    ChronosAudioSynthesizer.triggerHaptic(context, 2)
                                    if (isRunning) {
                                        onPause()
                                    } else if (remainingSeconds < totalSeconds && remainingSeconds > 0) {
                                        onResume()
                                    } else {
                                        onStart(null)
                                    }
                                }
                                .testTag("timer_start_pause_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = ChronosBgDeep,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRunning) ChronosTranslations.t(appLanguage, "pause")
                                    else ChronosTranslations.t(appLanguage, "start"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ChronosBgDeep
                                )
                            }
                        }
                    }
                }
            }
        }

        // Preset Grid
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ChronosSurfaceDark.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isId) "PRESET AKTIVITAS SEHARI-HARI" else "DAILY ACTIVITY PRESETS",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosCyanNeon
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    for (row in 0 until 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = if (row < 2) 8.dp else 0.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 3) {
                                val idx = row * 3 + col
                                val (secs, badge, label) = presets[idx]
                                val isSelected = totalSeconds == secs
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) ChronosCyanNeon.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) ChronosCyanNeon else Color.White.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSetDuration(secs) }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = badge,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) ChronosCyanNeon else Color.White
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            color = Color(0xFF9CA3AF),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Min/Sec Setter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customMinInput,
                            onValueChange = { customMinInput = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text(if (isId) "Menit" else "Min", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customSecInput,
                            onValueChange = { customSecInput = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text(if (isId) "Detik" else "Sec", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val mins = customMinInput.toIntOrNull() ?: 0
                                val secs = customSecInput.toIntOrNull() ?: 0
                                val total = mins * 60 + secs
                                if (total > 0) {
                                    onSetDuration(total)
                                    customMinInput = ""
                                    customSecInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChronosCyanNeon),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text(
                                text = if (isId) "Setel" else "Set",
                                color = ChronosBgDeep,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Ambient Soundscape Selector
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ChronosSurfaceDark.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isId) "GENERATOR GELOMBANG SUARA FOKUS (PCM SYNTH)" else "AMBIENT FOCUS SOUNDSCAPE SYNTHESIZER",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosPurpleNeon
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(soundscapes) { (type, label) ->
                            val active = activeSoundscape == type && (type == "NONE" || ChronosAudioSynthesizer.isPlayingSoundscape)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) ChronosPurpleNeon.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (active) ChronosPurpleNeon else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (type == "NONE") {
                                            ChronosAudioSynthesizer.stopSoundscape()
                                            activeSoundscape = "NONE"
                                        } else {
                                            ChronosAudioSynthesizer.toggleSoundscape(type) {
                                                activeSoundscape = ChronosAudioSynthesizer.currentSoundType
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (type == "NONE") Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = if (active) ChronosPurpleNeon else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else Color(0xFFD1D5DB)
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

private data class LapSplitEntry(
    val lapNumber: Int,
    val splitDurationMs: Long,
    val cumulativeMs: Long,
    val deltaFromTargetMs: Long
)

@Composable
fun StopwatchFullScreen(
    elapsedMs: Long,
    isRunning: Boolean,
    laps: List<Long>,
    showMs: Boolean = true,
    lapVibrate: Boolean = true,
    appLanguage: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onLap: () -> Unit,
    onReset: () -> Unit,
    onAiCommand: (String) -> Unit = {},
    savedSessions: List<StopwatchSessionRecord> = emptyList(),
    onSaveSession: (StopwatchSessionRecord, Boolean) -> Unit = { _, _ -> },
    onClearSessions: () -> Unit = {}
) {
    val showMilliseconds = showMs
    val isId = appLanguage == "id"
    val context = LocalContext.current

    // Mode: 0 = Kronograf Pro, 1 = Multi-Track (4), 2 = Interval & Log, 3 = Smart Lab & Menu Baru
    var selectedMode by remember { mutableIntStateOf(PersistentStopwatchSubEngines.selectedMode) }
    var selectedActivityTag by remember { mutableStateOf(PersistentStopwatchSubEngines.selectedActivityTag) }
    var targetLapSeconds by remember { mutableIntStateOf(PersistentStopwatchSubEngines.targetLapSeconds.coerceAtLeast(5)) }
    var selectedDistanceMeters by remember { mutableIntStateOf(PersistentStopwatchSubEngines.selectedDistanceMeters) }
    var smartMenuExpanded by remember { mutableStateOf(PersistentStopwatchSubEngines.smartMenuExpanded) }
    var autoLapIntervalSec by remember { mutableIntStateOf(PersistentStopwatchSubEngines.autoLapIntervalSec) }
    var cadenceBpm by remember { mutableIntStateOf(PersistentStopwatchSubEngines.cadenceBpm) }
    var statusToast by remember { mutableStateOf<String?>(null) }

    // Auto-Lap Engine (fires only when autoLapBucket advances, avoiding 30Hz coroutine churn)
    if (elapsedMs == 0L && PersistentStopwatchSubEngines.lastAutoLapBucket != 0L) {
        PersistentStopwatchSubEngines.lastAutoLapBucket = 0L
    }
    val currentAutoLapBucket = if (isRunning && autoLapIntervalSec > 0) {
        elapsedMs / (autoLapIntervalSec * 1000L)
    } else 0L
    LaunchedEffect(isRunning, autoLapIntervalSec, currentAutoLapBucket) {
        if (isRunning && autoLapIntervalSec > 0 && currentAutoLapBucket > 0L) {
            if (currentAutoLapBucket > PersistentStopwatchSubEngines.lastAutoLapBucket) {
                PersistentStopwatchSubEngines.lastAutoLapBucket = currentAutoLapBucket
                if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 1)
                ChronosAudioSynthesizer.playTacticalBeep(1046.5, 70)
                onLap()
            }
        }
    }

    // Live Cadence Metronome BPM Ticker (fires while stopwatch is running and cadenceBpm > 0)
    LaunchedEffect(isRunning, cadenceBpm) {
        if (isRunning && cadenceBpm > 0) {
            val delayMs = (60_000L / cadenceBpm.coerceIn(60, 220)).coerceAtLeast(250L)
            while (isActive && isRunning && cadenceBpm > 0) {
                ChronosAudioSynthesizer.playTacticalBeep(784.0, 42)
                delay(delayMs)
            }
        }
    }

    val activityTags = remember(isId) {
        if (isId) {
            listOf("Lari & Atletik", "Gym & HIIT", "Renang / Sepeda", "Fokus Kerja", "Eksperimen Lab", "Memasak / Seduh")
        } else {
            listOf("Running & Track", "Gym & HIIT", "Swim / Cycling", "Work Sprint", "Lab Timing", "Cooking / Brew")
        }
    }

    // Compute accurate split durations from cumulative `laps` (newest first in `laps`)
    val chronologicalCumulative = remember(laps) { laps.reversed() }
    val splitEntries = remember(chronologicalCumulative, targetLapSeconds) {
        val targetMs = targetLapSeconds * 1000L
        chronologicalCumulative.mapIndexed { idx, cumMs ->
            val prevCum = if (idx == 0) 0L else chronologicalCumulative[idx - 1]
            val splitMs = max(1L, cumMs - prevCum)
            LapSplitEntry(
                lapNumber = idx + 1,
                splitDurationMs = splitMs,
                cumulativeMs = cumMs,
                deltaFromTargetMs = splitMs - targetMs
            )
        }.reversed() // Newest first for display
    }

    val currentLapElapsedMs = remember(elapsedMs, chronologicalCumulative) {
        val lastCum = chronologicalCumulative.lastOrNull() ?: 0L
        max(0L, elapsedMs - lastCum)
    }

    val fastestSplitMs = remember(splitEntries) {
        splitEntries.minOfOrNull { it.splitDurationMs } ?: 0L
    }
    val slowestSplitMs = remember(splitEntries) {
        splitEntries.maxOfOrNull { it.splitDurationMs } ?: 0L
    }
    val averageSplitMs = remember(splitEntries) {
        if (splitEntries.isEmpty()) 0L else splitEntries.sumOf { it.splitDurationMs } / splitEntries.size
    }
    val consistencyScore = remember(splitEntries, averageSplitMs) {
        if (splitEntries.size < 2 || averageSplitMs <= 0L) {
            100
        } else {
            val variance = splitEntries.map {
                val diff = (it.splitDurationMs - averageSplitMs).toDouble()
                diff * diff
            }.average()
            val stdDev = sqrt(variance)
            val cv = (stdDev / averageSplitMs.toDouble()) * 100.0
            (100.0 - cv * 1.5).roundToInt().coerceIn(15, 100)
        }
    }

    val predictedNextLapMs = remember(splitEntries, averageSplitMs) {
        if (splitEntries.isEmpty()) 0L
        else {
            val recent = splitEntries.take(3).map { it.splitDurationMs }.average().toLong()
            (recent * 0.65 + averageSplitMs * 0.35).toLong()
        }
    }

    // Persistent 4-Channel Multi-Track Engine Ticker (sleeps completely at 0% CPU when no track is running)
    val multiTracks = PersistentStopwatchSubEngines.tracks
    val anyMultiTrackRunning = multiTracks.any { it.isRunning }
    LaunchedEffect(anyMultiTrackRunning) {
        if (anyMultiTrackRunning) {
            while (isActive && multiTracks.any { it.isRunning }) {
                val now = SystemClock.elapsedRealtime()
                multiTracks.forEach { it.tick(now) }
                delay(33L)
            }
        }
    }

    // Persistent HIIT Interval & Tempo Coach Ticker
    LaunchedEffect(PersistentStopwatchSubEngines.hiitRunning) {
        if (PersistentStopwatchSubEngines.hiitRunning) {
            PersistentStopwatchSubEngines.hiitLastTickRealtime = SystemClock.elapsedRealtime()
            while (isActive && PersistentStopwatchSubEngines.hiitRunning) {
                delay(250L)
                val nowRt = SystemClock.elapsedRealtime()
                if (nowRt - PersistentStopwatchSubEngines.hiitLastTickRealtime >= 1000L) {
                    PersistentStopwatchSubEngines.hiitLastTickRealtime += 1000L
                    PersistentStopwatchSubEngines.hiitTotalElapsedSec += 1
                    if (PersistentStopwatchSubEngines.hiitPhaseRemainingSec > 1) {
                        PersistentStopwatchSubEngines.hiitPhaseRemainingSec -= 1
                        if (PersistentStopwatchSubEngines.hiitPhaseRemainingSec <= 3) {
                            ChronosAudioSynthesizer.playTacticalBeep(660.0, 65)
                        }
                    } else {
                        if (PersistentStopwatchSubEngines.hiitIsWorkPhase) {
                            PersistentStopwatchSubEngines.hiitIsWorkPhase = false
                            PersistentStopwatchSubEngines.hiitPhaseRemainingSec = PersistentStopwatchSubEngines.hiitRestSeconds
                            ChronosAudioSynthesizer.playTacticalBeep(528.0, 160)
                            if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 2)
                        } else {
                            if (PersistentStopwatchSubEngines.hiitCurrentRound < PersistentStopwatchSubEngines.hiitTotalRounds) {
                                PersistentStopwatchSubEngines.hiitCurrentRound += 1
                                PersistentStopwatchSubEngines.hiitIsWorkPhase = true
                                PersistentStopwatchSubEngines.hiitPhaseRemainingSec = PersistentStopwatchSubEngines.hiitWorkSeconds
                                ChronosAudioSynthesizer.playTacticalBeep(880.0, 160)
                                if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 2)
                            } else {
                                PersistentStopwatchSubEngines.hiitRunning = false
                                PersistentStopwatchSubEngines.hiitPhaseRemainingSec = 0
                                ChronosAudioSynthesizer.playAlarmChime(0, 3)
                                if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 3)
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(statusToast) {
        if (statusToast != null) {
            delay(3500L)
            statusToast = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("stopwatch_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isId) "STOPWATCH CERDAS PRO" else "SMART STOPWATCH PRO",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp,
                        color = ChronosCyanNeon
                    )
                    Text(
                        text = if (isId) "KRONOGRAF ANALITIK • MULTI-TRACK • INTERVAL HIIT"
                        else "ANALYTICAL CHRONOGRAPH • MULTI-TRACK • HIIT COACH",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                val runningCount = (if (isRunning) 1 else 0) +
                    multiTracks.count { it.isRunning } +
                    (if (PersistentStopwatchSubEngines.hiitRunning) 1 else 0)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (runningCount > 0) ChronosGreenNeon.copy(alpha = 0.14f) else ChronosCyanNeon.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (runningCount > 0) ChronosGreenNeon.copy(alpha = 0.4f) else ChronosCyanNeon.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (runningCount > 0) {
                            if (isId) "$runningCount AKTIF" else "$runningCount ACTIVE"
                        } else {
                            if (isId) "PRESISI TINGGI" else "READY"
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (runningCount > 0) ChronosGreenNeon else ChronosCyanNeon,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 2. 4-Mode Interactive Command Bar + Smart Quick Menu Inside Stopwatch (v1.66.98)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ChronosSurfaceDark.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        val modes = listOf(
                            Triple(0, if (isId) "Kronograf" else "Chrono", Icons.Default.Timer),
                            Triple(1, "Multi (4)", Icons.Default.Speed),
                            Triple(2, "HIIT & Log", Icons.Default.FitnessCenter),
                            Triple(3, if (isId) "Smart Lab" else "Smart Lab", Icons.Default.Analytics)
                        )
                        modes.forEach { (idx, title, icon) ->
                            val active = selectedMode == idx
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = if (active) ChronosCyanNeon else Color.White.copy(alpha = 0.04f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(13.dp))
                                    .clickable {
                                        selectedMode = idx
                                        PersistentStopwatchSubEngines.selectedMode = idx
                                    }
                                    .testTag("stopwatch_mode_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (active) ChronosBgDeep else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = title,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) ChronosBgDeep else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // New Interactive Quick Smart Menu Bar inside Stopwatch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                smartMenuExpanded = !smartMenuExpanded
                                PersistentStopwatchSubEngines.smartMenuExpanded = smartMenuExpanded
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (autoLapIntervalSec > 0 || cadenceBpm > 0) ChronosGreenNeon else ChronosCyanNeon)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isId) "MENU PINTAR STOPWATCH V1.66.98" else "SMART STOPWATCH MENU V1.66.98",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosCyanNeon
                            )
                        }

                        Text(
                            text = if (smartMenuExpanded) (if (isId) "Sembunyikan ▲" else "Hide ▲")
                            else (if (isId) "Buka Menu ▼" else "Open Menu ▼"),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosAmberNeon,
                            modifier = Modifier.clickable {
                                smartMenuExpanded = !smartMenuExpanded
                                PersistentStopwatchSubEngines.smartMenuExpanded = smartMenuExpanded
                            }
                        )
                    }

                    if (smartMenuExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Quick Chip 1: Auto-Lap Interval
                            val autoOptions = listOf(0, 15, 30, 60, 120)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (autoLapIntervalSec > 0) ChronosGreenNeon.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (autoLapIntervalSec > 0) ChronosGreenNeon.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val nextIdx = (autoOptions.indexOf(autoLapIntervalSec) + 1) % autoOptions.size
                                        autoLapIntervalSec = autoOptions[nextIdx]
                                        PersistentStopwatchSubEngines.autoLapIntervalSec = autoLapIntervalSec
                                        PersistentStopwatchSubEngines.lastAutoLapBucket = if (autoLapIntervalSec > 0) elapsedMs / (autoLapIntervalSec * 1000L) else 0L
                                        statusToast = if (autoLapIntervalSec > 0) {
                                            if (isId) "⚡ Auto-Lap aktif setiap ${autoLapIntervalSec} detik" else "⚡ Auto-Lap enabled every ${autoLapIntervalSec}s"
                                        } else {
                                            if (isId) "Auto-Lap dinonaktifkan" else "Auto-Lap disabled"
                                        }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "AUTO-LAP",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (autoLapIntervalSec > 0) ChronosGreenNeon else Color(0xFF9CA3AF)
                                    )
                                    Text(
                                        text = if (autoLapIntervalSec > 0) "${autoLapIntervalSec}s" else "OFF",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Quick Chip 2: Cadence Metronome BPM
                            val bpmOptions = listOf(0, 120, 150, 170, 180)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (cadenceBpm > 0) ChronosAmberNeon.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (cadenceBpm > 0) ChronosAmberNeon.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val nextIdx = (bpmOptions.indexOf(cadenceBpm) + 1) % bpmOptions.size
                                        cadenceBpm = bpmOptions[nextIdx]
                                        PersistentStopwatchSubEngines.cadenceBpm = cadenceBpm
                                        statusToast = if (cadenceBpm > 0) {
                                            if (isId) "🔊 Metronom Kadensi: $cadenceBpm BPM (Aktif saat jalan)" else "🔊 Cadence Metronome: $cadenceBpm BPM"
                                        } else {
                                            if (isId) "Metronom Kadensi OFF" else "Cadence Metronome OFF"
                                        }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isId) "KADENSI" else "CADENCE",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cadenceBpm > 0) ChronosAmberNeon else Color(0xFF9CA3AF)
                                    )
                                    Text(
                                        text = if (cadenceBpm > 0) "$cadenceBpm BPM" else "OFF",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Quick Chip 3: Track Distance Selector
                            val distOptions = listOf(100, 400, 1000, 5000)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ChronosCyanNeon.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val nextIdx = (distOptions.indexOf(selectedDistanceMeters) + 1) % distOptions.size
                                        selectedDistanceMeters = distOptions[nextIdx]
                                        PersistentStopwatchSubEngines.selectedDistanceMeters = selectedDistanceMeters
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isId) "JARAK LINTAS" else "DISTANCE",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosCyanNeon
                                    )
                                    Text(
                                        text = if (selectedDistanceMeters >= 1000) "${selectedDistanceMeters / 1000} km" else "${selectedDistanceMeters} m",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Quick Chip 4: Instant CSV Export
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ChronosPurpleNeon.copy(alpha = 0.14f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosPurpleNeon.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val csv = buildString {
                                            appendLine("Lap,Split_Ms,Split_Formatted,Cumulative_Formatted,Delta_Target_Sec")
                                            splitEntries.reversed().forEach { s ->
                                                appendLine("${s.lapNumber},${s.splitDurationMs},${formatStopwatchTime(s.splitDurationMs, true)},${formatStopwatchTime(s.cumulativeMs, true)},${"%.2f".format(Locale.US, s.deltaFromTargetMs / 1000.0)}")
                                            }
                                        }
                                        copyToClipboard(context, csv)
                                        statusToast = if (isId) "✓ Data Tabel CSV Putaran disalin ke Clipboard!" else "✓ Lap CSV Table copied to Clipboard!"
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "CSV DATA",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosPurpleNeon
                                    )
                                    Text(
                                        text = if (isId) "Salin CSV" else "Copy CSV",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (statusToast != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ChronosGreenNeon.copy(alpha = 0.16f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = ChronosGreenNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusToast!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        when (selectedMode) {
            0 -> {
                // ============================================================
                // MODE 0: KRONOGRAF PRO, DUAL-RING DIAL, PACE PREDICTOR & ANALYTICS
                // ============================================================
                item {
                    // Activity Tag & Target Pace Bar
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ChronosSurfaceDark.copy(alpha = 0.88f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isId) "KATEGORI AKTIVITAS & TARGET PUTARAN" else "ACTIVITY TAG & TARGET LAP PACE",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChronosCyanNeon
                                )
                                Text(
                                    text = "${if (isId) "Target:" else "Target:"} ${targetLapSeconds}s/lap",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChronosAmberNeon
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(activityTags) { tag ->
                                    val selected = tag == selectedActivityTag
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) ChronosCyanNeon.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.04f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selected) ChronosCyanNeon else Color.White.copy(alpha = 0.08f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedActivityTag = tag
                                                PersistentStopwatchSubEngines.selectedActivityTag = tag
                                            }
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) ChronosCyanNeon else Color(0xFFD1D5DB),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isId) "Target Pace:" else "Target Pace:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                                listOf(15, 30, 45, 60, 90, 120).forEach { sec ->
                                    val active = targetLapSeconds == sec
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (active) ChronosAmberNeon.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.04f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (active) ChronosAmberNeon else Color.White.copy(alpha = 0.08f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                targetLapSeconds = sec
                                                PersistentStopwatchSubEngines.targetLapSeconds = sec
                                            }
                                    ) {
                                        Text(
                                            text = "${sec}s",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) ChronosAmberNeon else Color(0xFFD1D5DB),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Dual-Ring Precision Chronograph Dial & Controls
                item {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = ChronosSurfaceDark.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.28f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(230.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val outerSweep = ((elapsedMs % 60_000L).toFloat() / 60_000f) * 360f
                                val targetMs = (targetLapSeconds * 1000L).coerceAtLeast(1000L)
                                val innerSweep = ((currentLapElapsedMs.toFloat() / targetMs.toFloat()).coerceIn(0f, 1f)) * 360f
                                val isOnPace = currentLapElapsedMs <= targetMs

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val outerR = (size.minDimension / 2f) - 10.dp.toPx()
                                    val innerR = outerR - 16.dp.toPx()

                                    // 60 tick marks
                                    for (t in 0 until 60) {
                                        val rad = Math.toRadians((t * 6.0) - 90.0)
                                        val r1 = if (t % 5 == 0) outerR - 6.dp.toPx() else outerR - 3.dp.toPx()
                                        drawLine(
                                            color = if (t % 5 == 0) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                                            start = Offset(center.x + (cos(rad) * r1).toFloat(), center.y + (sin(rad) * r1).toFloat()),
                                            end = Offset(center.x + (cos(rad) * outerR).toFloat(), center.y + (sin(rad) * outerR).toFloat()),
                                            strokeWidth = if (t % 5 == 0) 2.dp.toPx() else 1.dp.toPx()
                                        )
                                    }

                                    // Outer 60s Sweep Arc
                                    drawArc(
                                        color = ChronosCyanNeon,
                                        startAngle = -90f,
                                        sweepAngle = outerSweep,
                                        useCenter = false,
                                        topLeft = Offset(center.x - outerR, center.y - outerR),
                                        size = Size(outerR * 2f, outerR * 2f),
                                        style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // Inner Target Pace Track & Arc
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.06f),
                                        radius = innerR,
                                        center = center,
                                        style = Stroke(width = 4.dp.toPx())
                                    )
                                    drawArc(
                                        color = if (isOnPace) ChronosGreenNeon else ChronosRoseNeon,
                                        startAngle = -90f,
                                        sweepAngle = innerSweep,
                                        useCenter = false,
                                        topLeft = Offset(center.x - innerR, center.y - innerR),
                                        size = Size(innerR * 2f, innerR * 2f),
                                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = selectedActivityTag.uppercase(),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosCyanNeon
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatStopwatchTime(elapsedMs, showMilliseconds),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (elapsedMs >= 3600_000L) 28.sp else 34.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${if (isId) "Putaran #${splitEntries.size + 1}:" else "Lap #${splitEntries.size + 1}:"} ${formatStopwatchTime(currentLapElapsedMs, true)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = if (currentLapElapsedMs <= targetLapSeconds * 1000L) ChronosGreenNeon else ChronosAmberNeon
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action Buttons: Reset, Lap, Start/Pause, Save Session
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Reset Button
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.06f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 1)
                                            onReset()
                                        }
                                        .testTag("stopwatch_reset_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 13.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = ChronosTranslations.t(appLanguage, "reset"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Lap Button
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ChronosPurpleNeon.copy(alpha = if (elapsedMs > 0L) 0.22f else 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        ChronosPurpleNeon.copy(alpha = if (elapsedMs > 0L) 0.6f else 0.2f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = elapsedMs > 0L) {
                                            if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 1)
                                            ChronosAudioSynthesizer.playTacticalBeep(987.77, 85)
                                            onLap()
                                        }
                                        .testTag("stopwatch_lap_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 13.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Flag, contentDescription = null, tint = ChronosPurpleNeon, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = ChronosTranslations.t(appLanguage, "lap"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Start / Pause Button
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isRunning) ChronosAmberNeon else ChronosCyanNeon,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 2)
                                            ChronosAudioSynthesizer.playTacticalBeep(if (isRunning) 528.0 else 880.0, 90)
                                            if (isRunning) onPause() else onStart()
                                        }
                                        .testTag("stopwatch_start_pause_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 13.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = ChronosBgDeep,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isRunning) ChronosTranslations.t(appLanguage, "pause")
                                            else ChronosTranslations.t(appLanguage, "start"),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ChronosBgDeep
                                        )
                                    }
                                }
                            }

                            // Save Session & Export Clipboard Row when elapsedMs > 0
                            if (elapsedMs > 0L) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isSport = selectedActivityTag.contains("Lari", ignoreCase = true) ||
                                        selectedActivityTag.contains("Run", ignoreCase = true) ||
                                        selectedActivityTag.contains("Gym", ignoreCase = true) ||
                                        selectedActivityTag.contains("Renang", ignoreCase = true) ||
                                        selectedActivityTag.contains("Swim", ignoreCase = true)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ChronosGreenNeon.copy(alpha = 0.16f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
                                                val record = StopwatchSessionRecord(
                                                    id = System.currentTimeMillis().toString(),
                                                    activityTag = selectedActivityTag,
                                                    totalElapsedMs = elapsedMs,
                                                    lapCount = splitEntries.size,
                                                    fastestLapMs = if (fastestSplitMs > 0L) fastestSplitMs else elapsedMs,
                                                    averageLapMs = if (averageSplitMs > 0L) averageSplitMs else elapsedMs,
                                                    consistencyScore = consistencyScore,
                                                    timestampStr = sdf.format(Date()),
                                                    lapSplitsMs = splitEntries.map { it.splitDurationMs }
                                                )
                                                onSaveSession(record, isSport)
                                                statusToast = if (isId) {
                                                    "✓ Sesi disimpan ke Riwayat & disinkronkan ke ${if (isSport) "Olahraga Life Suite" else "Fokus Produktivitas"}!"
                                                } else {
                                                    "✓ Session saved & synced to ${if (isSport) "Life Suite Workout" else "Productivity Focus"}!"
                                                }
                                            }
                                            .testTag("stopwatch_save_session_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, tint = ChronosGreenNeon, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isId) "Simpan & Sinkron Sesi" else "Save & Sync Session",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ChronosGreenNeon
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ChronosCyanNeon.copy(alpha = 0.14f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                val report = buildStopwatchReportText(
                                                    activityTag = selectedActivityTag,
                                                    totalMs = elapsedMs,
                                                    splits = splitEntries,
                                                    fastestMs = fastestSplitMs,
                                                    avgMs = averageSplitMs,
                                                    consistency = consistencyScore,
                                                    isId = isId
                                                )
                                                copyToClipboard(context, report)
                                                statusToast = if (isId) "✓ Laporan telemetri disalin ke Clipboard!" else "✓ Telemetry report copied to Clipboard!"
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ChronosCyanNeon, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isId) "Salin Laporan" else "Copy Report",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ChronosCyanNeon
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Pace & Finish Predictor Card (Speed km/h, Pace min/km, 5K/10K Projections)
                if (elapsedMs >= 1000L) {
                    item {
                        PaceAndFinishPredictorCard(
                            elapsedMs = elapsedMs,
                            lapCount = max(1, splitEntries.size),
                            averageSplitMs = if (averageSplitMs > 0L) averageSplitMs else elapsedMs,
                            selectedDistanceMeters = selectedDistanceMeters,
                            onSelectDistanceMeters = {
                                selectedDistanceMeters = it
                                PersistentStopwatchSubEngines.selectedDistanceMeters = it
                            },
                            isId = isId
                        )
                    }
                }

                // Real-Time Lap Analytics & Visual Bar Chart (when at least 1 lap recorded)
                if (splitEntries.isNotEmpty()) {
                    item {
                        StopwatchAnalyticsCard(
                            splitEntries = splitEntries,
                            fastestSplitMs = fastestSplitMs,
                            slowestSplitMs = slowestSplitMs,
                            averageSplitMs = averageSplitMs,
                            consistencyScore = consistencyScore,
                            predictedNextLapMs = predictedNextLapMs,
                            targetLapMs = targetLapSeconds * 1000L,
                            isId = isId
                        )
                    }

                    // Detailed Lap & Split Table
                    item {
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
                                        text = if (isId) "TABEL SPLIT PUTARAN & SELISIH TARGET" else "LAP SPLITS & TARGET DELTA TABLE",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosCyanNeon
                                    )
                                    Text(
                                        text = "${splitEntries.size} ${if (isId) "Putaran" else "Laps"}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    splitEntries.forEach { entry ->
                                        val isFastest = splitEntries.size > 1 && entry.splitDurationMs == fastestSplitMs
                                        val isSlowest = splitEntries.size > 1 && entry.splitDurationMs == slowestSplitMs
                                        val deltaSec = entry.deltaFromTargetMs / 1000.0
                                        val deltaStr = "%s%.2fs".format(
                                            Locale.US,
                                            if (deltaSec >= 0) "+" else "",
                                            deltaSec
                                        )

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    when {
                                                        isFastest -> ChronosGreenNeon.copy(alpha = 0.10f)
                                                        isSlowest -> ChronosRoseNeon.copy(alpha = 0.10f)
                                                        else -> Color.White.copy(alpha = 0.03f)
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "#%02d".format(entry.lapNumber),
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = when {
                                                        isFastest -> ChronosGreenNeon
                                                        isSlowest -> ChronosRoseNeon
                                                        else -> ChronosCyanNeon
                                                    }
                                                )
                                                if (isFastest || isSlowest) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (isFastest) ChronosGreenNeon.copy(alpha = 0.2f) else ChronosRoseNeon.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = if (isFastest) (if (isId) "TERCEPAT" else "BEST")
                                                            else (if (isId) "TERLAMBAT" else "SLOW"),
                                                            fontFamily = JetBrainsMonoFontFamily,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isFastest) ChronosGreenNeon else ChronosRoseNeon,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "+${formatStopwatchTime(entry.splitDurationMs, true)}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )

                                            Text(
                                                text = deltaStr,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                color = if (entry.deltaFromTargetMs <= 0L) ChronosGreenNeon else ChronosAmberNeon
                                            )

                                            Text(
                                                text = formatStopwatchTime(entry.cumulativeMs, true),
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                color = Color(0xFF9CA3AF)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // ============================================================
                // MODE 1: MULTI-TRACK STOPWATCH (4 PARALLEL CHANNELS)
                // ============================================================
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ChronosSurfaceDark.copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.28f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isId) "4 STOPWATCH PARALEL INDEPENDEN (PERSISTEN)"
                                        else "4 PARALLEL INDEPENDENT STOPWATCHES (PERSISTENT)",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosCyanNeon
                                    )
                                    Text(
                                        text = if (isId) "Tetap berjalan akurat meski berpindah tab layar"
                                        else "Keeps running accurately even when switching screens",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 2)
                                        ChronosAudioSynthesizer.playTacticalBeep(880.0, 95)
                                        multiTracks.forEach { it.start() }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ChronosGreenNeon),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isId) "Mulai Semua" else "Start All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosBgDeep
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 1)
                                        multiTracks.forEach { it.pause() }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ChronosAmberNeon),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isId) "Jeda Semua" else "Pause All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosBgDeep
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 1)
                                        multiTracks.forEach { it.reset() }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isId) "Reset Semua" else "Reset All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                itemsIndexed(multiTracks) { idx, track ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ChronosSurfaceDark.copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (track.isRunning) track.accentColor.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(track.accentColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = track.label,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = if (track.laps.isNotEmpty()) "${track.laps.size} Laps" else "Track #${idx + 1}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = track.accentColor
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatStopwatchTime(track.displayMs, true),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    color = Color.White
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.06f),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 1)
                                                track.reset()
                                            }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Reset Track",
                                            tint = Color(0xFFD1D5DB),
                                            modifier = Modifier.padding(8.dp).size(18.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = track.accentColor.copy(alpha = 0.16f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, track.accentColor.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 1)
                                                ChronosAudioSynthesizer.playTacticalBeep(960.0, 75)
                                                track.recordLap()
                                            }
                                    ) {
                                        Text(
                                            text = "Lap",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = track.accentColor,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (track.isRunning) ChronosAmberNeon else track.accentColor,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 2)
                                                ChronosAudioSynthesizer.playTacticalBeep(784.0, 80)
                                                track.toggle()
                                            }
                                    ) {
                                        Text(
                                            text = if (track.isRunning) (if (isId) "Jeda" else "Pause") else (if (isId) "Mulai" else "Start"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ChronosBgDeep,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            if (track.laps.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    itemsIndexed(track.laps.take(6)) { lIdx, lapMs ->
                                        val lapNum = track.laps.size - lIdx
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White.copy(alpha = 0.04f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                        ) {
                                            Text(
                                                text = "#$lapNum ${formatStopwatchTime(lapMs, true)}",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 10.sp,
                                                color = Color(0xFFD1D5DB),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // ============================================================
                // MODE 2: HIIT INTERVAL COACH & SAVED STOPWATCH SESSIONS LOG
                // ============================================================
                item {
                    val phaseColor = if (PersistentStopwatchSubEngines.hiitIsWorkPhase) ChronosGreenNeon else ChronosCyanNeon
                    val phaseTotal = if (PersistentStopwatchSubEngines.hiitIsWorkPhase) {
                        PersistentStopwatchSubEngines.hiitWorkSeconds
                    } else {
                        PersistentStopwatchSubEngines.hiitRestSeconds
                    }
                    val phaseProgress = if (phaseTotal > 0) {
                        (PersistentStopwatchSubEngines.hiitPhaseRemainingSec.toFloat() / phaseTotal.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = ChronosSurfaceDark.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, phaseColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isId) "PELATIH INTERVAL HIIT & TABATA OTOMATIS"
                                        else "AUTOMATED HIIT & TABATA INTERVAL COACH",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = phaseColor
                                    )
                                    Text(
                                        text = if (isId) "Transisi Kerja/Istirahat dengan sinyal suara & getar"
                                        else "Work/Rest cycle transitions with audio chime & haptics",
                                        fontSize = 11.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = phaseColor.copy(alpha = 0.16f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, phaseColor.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "Ronde ${PersistentStopwatchSubEngines.hiitCurrentRound}/${PersistentStopwatchSubEngines.hiitTotalRounds}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = phaseColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (PersistentStopwatchSubEngines.hiitIsWorkPhase) {
                                    if (isId) "🔥 FASE KERJA / SPRINT" else "🔥 WORK / SPRINT PHASE"
                                } else {
                                    if (isId) "💧 FASE PEMULIHAN / ISTIRAHAT" else "💧 REST / RECOVERY PHASE"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = phaseColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "%02d:%02d".format(
                                    PersistentStopwatchSubEngines.hiitPhaseRemainingSec / 60,
                                    PersistentStopwatchSubEngines.hiitPhaseRemainingSec % 60
                                ),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 42.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { phaseProgress },
                                color = phaseColor,
                                trackColor = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Config selectors for Work / Rest / Rounds
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IntervalConfigBox(
                                    label = if (isId) "KERJA" else "WORK",
                                    options = listOf(20, 30, 45, 60),
                                    selected = PersistentStopwatchSubEngines.hiitWorkSeconds,
                                    unit = "s",
                                    accent = ChronosGreenNeon,
                                    modifier = Modifier.weight(1f),
                                    onSelect = {
                                        PersistentStopwatchSubEngines.hiitWorkSeconds = it
                                        if (!PersistentStopwatchSubEngines.hiitRunning && PersistentStopwatchSubEngines.hiitIsWorkPhase) {
                                            PersistentStopwatchSubEngines.hiitPhaseRemainingSec = it
                                        }
                                    }
                                )
                                IntervalConfigBox(
                                    label = if (isId) "ISTIRAHAT" else "REST",
                                    options = listOf(10, 15, 20, 30),
                                    selected = PersistentStopwatchSubEngines.hiitRestSeconds,
                                    unit = "s",
                                    accent = ChronosCyanNeon,
                                    modifier = Modifier.weight(1f),
                                    onSelect = {
                                        PersistentStopwatchSubEngines.hiitRestSeconds = it
                                        if (!PersistentStopwatchSubEngines.hiitRunning && !PersistentStopwatchSubEngines.hiitIsWorkPhase) {
                                            PersistentStopwatchSubEngines.hiitPhaseRemainingSec = it
                                        }
                                    }
                                )
                                IntervalConfigBox(
                                    label = if (isId) "RONDE" else "ROUNDS",
                                    options = listOf(4, 8, 10, 12),
                                    selected = PersistentStopwatchSubEngines.hiitTotalRounds,
                                    unit = "x",
                                    accent = ChronosAmberNeon,
                                    modifier = Modifier.weight(1f),
                                    onSelect = { PersistentStopwatchSubEngines.hiitTotalRounds = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        PersistentStopwatchSubEngines.hiitRunning = false
                                        PersistentStopwatchSubEngines.hiitCurrentRound = 1
                                        PersistentStopwatchSubEngines.hiitIsWorkPhase = true
                                        PersistentStopwatchSubEngines.hiitPhaseRemainingSec = PersistentStopwatchSubEngines.hiitWorkSeconds
                                        PersistentStopwatchSubEngines.hiitTotalElapsedSec = 0
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = ChronosTranslations.t(appLanguage, "reset"),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = {
                                        if (lapVibrate) ChronosAudioSynthesizer.triggerHaptic(context, 2)
                                        ChronosAudioSynthesizer.playTacticalBeep(880.0, 95)
                                        if (!PersistentStopwatchSubEngines.hiitRunning && PersistentStopwatchSubEngines.hiitPhaseRemainingSec <= 0) {
                                            PersistentStopwatchSubEngines.hiitCurrentRound = 1
                                            PersistentStopwatchSubEngines.hiitIsWorkPhase = true
                                            PersistentStopwatchSubEngines.hiitPhaseRemainingSec = PersistentStopwatchSubEngines.hiitWorkSeconds
                                        }
                                        PersistentStopwatchSubEngines.hiitRunning = !PersistentStopwatchSubEngines.hiitRunning
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (PersistentStopwatchSubEngines.hiitRunning) ChronosAmberNeon else phaseColor
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1.4f)
                                ) {
                                    Text(
                                        text = if (PersistentStopwatchSubEngines.hiitRunning) (if (isId) "Jeda HIIT" else "Pause HIIT")
                                        else (if (isId) "Mulai HIIT" else "Start HIIT"),
                                        color = ChronosBgDeep,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Saved Stopwatch Sessions History Log
                item {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = ChronosCyanNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isId) "RIWAYAT SESI STOPWATCH TERSIMPAN" else "SAVED STOPWATCH SESSION LOG",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChronosCyanNeon
                                    )
                                }
                                if (savedSessions.isNotEmpty()) {
                                    IconButton(
                                        onClick = onClearSessions,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear History",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (savedSessions.isEmpty()) {
                                Text(
                                    text = if (isId) "Belum ada sesi tersimpan. Gunakan tombol 'Simpan & Sinkron Sesi' pada Kronograf Pro untuk menyimpan rekaman waktu Anda."
                                    else "No saved sessions yet. Tap 'Save & Sync Session' in Chrono Pro to log your timing sessions.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF9CA3AF),
                                    lineHeight = 17.sp
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    savedSessions.forEach { rec ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White.copy(alpha = 0.03f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = rec.activityTag,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "${rec.timestampStr} • ${rec.lapCount} Laps • Konsistensi ${rec.consistencyScore}%",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF9CA3AF)
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = formatStopwatchTime(rec.totalElapsedMs, true),
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = ChronosCyanNeon
                                                    )
                                                    Text(
                                                        text = "Best: ${formatStopwatchTime(rec.fastestLapMs, true)}",
                                                        fontFamily = JetBrainsMonoFontFamily,
                                                        fontSize = 10.sp,
                                                        color = ChronosGreenNeon
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
            }

            3 -> {
                // ============================================================
                // MODE 3: SMART LAB BARU V1.66.98 (REACTION START GATE, PACE CALCULATOR & TELEMETRY)
                // ============================================================
                item {
                    SmartStopwatchReactionAndLabSection(
                        elapsedMs = elapsedMs,
                        isRunning = isRunning,
                        lapCount = max(1, splitEntries.size),
                        averageSplitMs = if (averageSplitMs > 0L) averageSplitMs else max(1000L, elapsedMs),
                        selectedDistanceMeters = selectedDistanceMeters,
                        onSelectDistanceMeters = {
                            selectedDistanceMeters = it
                            PersistentStopwatchSubEngines.selectedDistanceMeters = it
                        },
                        onStartMainStopwatch = {
                            if (!isRunning) onStart()
                            selectedMode = 0
                            PersistentStopwatchSubEngines.selectedMode = 0
                        },
                        isId = isId
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartStopwatchReactionAndLabSection(
    elapsedMs: Long,
    isRunning: Boolean,
    lapCount: Int,
    averageSplitMs: Long,
    selectedDistanceMeters: Int,
    onSelectDistanceMeters: (Int) -> Unit,
    onStartMainStopwatch: () -> Unit,
    isId: Boolean
) {
    val context = LocalContext.current
    // Gate states: 0=Idle, 1=Armed (Red lights building), 2=Ready (Green GO!), 3=False Start, 4=Result
    var gateState by remember { mutableIntStateOf(0) }
    var redLightsLit by remember { mutableIntStateOf(0) }
    var goTimestampRealtime by remember { mutableLongStateOf(0L) }
    var reactionMs by remember { mutableLongStateOf(PersistentStopwatchSubEngines.lastReactionMs) }
    var bestReactionMs by remember { mutableLongStateOf(PersistentStopwatchSubEngines.bestReactionMs) }
    var autoLaunchChronoOnGo by remember { mutableStateOf(true) }

    LaunchedEffect(gateState) {
        if (gateState == 1) {
            for (light in 1..5) {
                redLightsLit = light
                ChronosAudioSynthesizer.playTacticalBeep(440.0, 55)
                delay(480L)
            }
            val randomHold = kotlin.random.Random.nextLong(650L, 2100L)
            delay(randomHold)
            if (gateState == 1) {
                redLightsLit = 0
                goTimestampRealtime = SystemClock.elapsedRealtime()
                gateState = 2
                ChronosAudioSynthesizer.playTacticalBeep(1174.66, 130)
                ChronosAudioSynthesizer.triggerHaptic(context, 2)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Pro Start Gate & Reflex Calibrator Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ChronosSurfaceDark.copy(alpha = 0.94f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when (gateState) {
                    2 -> ChronosGreenNeon
                    3 -> ChronosRoseNeon
                    else -> ChronosCyanNeon.copy(alpha = 0.35f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isId) "GERBANG START ATLETIK & KALIBRASI REFLEKS"
                            else "PRO START REACTION GATE & CALIBRATOR",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosCyanNeon
                        )
                        Text(
                            text = if (isId) "Simulasi lampu start 5-titik dengan deteksi False Start presisi tinggi"
                            else "5-light sprint start gate with millisecond false-start detection",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    if (bestReactionMs > 0L) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ChronosGreenNeon.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "PB: ${bestReactionMs} ms",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosGreenNeon,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5 Start Gate Lights Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        val lightColor = when {
                            gateState == 2 -> ChronosGreenNeon
                            gateState == 3 -> ChronosRoseNeon
                            gateState == 1 && i <= redLightsLit -> ChronosRoseNeon
                            else -> Color.White.copy(alpha = 0.12f)
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(lightColor)
                                .border(2.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Reaction Pad
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = when (gateState) {
                        1 -> ChronosRoseNeon.copy(alpha = 0.18f)
                        2 -> ChronosGreenNeon.copy(alpha = 0.25f)
                        3 -> ChronosRoseNeon.copy(alpha = 0.28f)
                        else -> ChronosCyanNeon.copy(alpha = 0.14f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        when (gateState) {
                            1 -> ChronosRoseNeon
                            2 -> ChronosGreenNeon
                            3 -> ChronosRoseNeon
                            else -> ChronosCyanNeon
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            when (gateState) {
                                0, 3, 4 -> {
                                    redLightsLit = 0
                                    gateState = 1
                                }
                                1 -> {
                                    // False Start!
                                    gateState = 3
                                    ChronosAudioSynthesizer.playTacticalBeep(220.0, 240)
                                    ChronosAudioSynthesizer.triggerHaptic(context, 3)
                                }
                                2 -> {
                                    val delta = max(1L, SystemClock.elapsedRealtime() - goTimestampRealtime)
                                    reactionMs = delta
                                    PersistentStopwatchSubEngines.lastReactionMs = delta
                                    if (bestReactionMs == 0L || delta < bestReactionMs) {
                                        bestReactionMs = delta
                                        PersistentStopwatchSubEngines.bestReactionMs = delta
                                    }
                                    gateState = 4
                                    ChronosAudioSynthesizer.triggerHaptic(context, 2)
                                    if (autoLaunchChronoOnGo) {
                                        onStartMainStopwatch()
                                    }
                                }
                            }
                        }
                        .testTag("stopwatch_reaction_gate_pad")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val headline = when (gateState) {
                            1 -> if (isId) "TAHAN... TUNGGU LAMPU PADAM / HIJAU!" else "HOLD... WAIT FOR GREEN!"
                            2 -> if (isId) "KETUK SEKARANG! (GO!)" else "TAP NOW! (GO!)"
                            3 -> if (isId) "⚠️ FALSE START! KETUK UNTUK ULANGI" else "⚠️ FALSE START! TAP TO RETRY"
                            4 -> if (isId) "⚡ WAKTU REAKSI: $reactionMs ms" else "⚡ REACTION TIME: $reactionMs ms"
                            else -> if (isId) "KETUK UNTUK MULAI GERBANG START" else "TAP TO ARM START GATE"
                        }
                        Text(
                            text = headline,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (gateState == 4) {
                                val rating = when {
                                    reactionMs < 180L -> if (isId) "Kelas Olimpiade / Pro (<180ms)" else "Olympic / Pro Reflex (<180ms)"
                                    reactionMs < 250L -> if (isId) "Sangat Cepat & Tajam" else "Sharp Athletic Reflex"
                                    else -> if (isId) "Respons Stabil" else "Steady Response"
                                }
                                "$rating • ${if (isId) "Ketuk lagi untuk uji ulang" else "Tap again to re-test"}"
                            } else {
                                if (isId) "Otomatis menyalakan Kronograf utama saat GO: ${if (autoLaunchChronoOnGo) "AKTIF" else "MATI"}"
                                else "Auto-start main Chronograph on GO: ${if (autoLaunchChronoOnGo) "ON" else "OFF"}"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFFD1D5DB),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isId) "Mulai Kronograf Otomatis saat Start Hijau"
                        else "Auto-Start Chronograph on Green Light",
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (autoLaunchChronoOnGo) ChronosGreenNeon.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { autoLaunchChronoOnGo = !autoLaunchChronoOnGo }
                    ) {
                        Text(
                            text = if (autoLaunchChronoOnGo) (if (isId) "AKTIF" else "ON") else "OFF",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (autoLaunchChronoOnGo) ChronosGreenNeon else Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 2. Always-Accessible Speed, Pace & Marathon Predictor Lab
        PaceAndFinishPredictorCard(
            elapsedMs = max(1000L, elapsedMs),
            lapCount = lapCount,
            averageSplitMs = averageSplitMs,
            selectedDistanceMeters = selectedDistanceMeters,
            onSelectDistanceMeters = onSelectDistanceMeters,
            isId = isId
        )
    }
}

@Composable
private fun PaceAndFinishPredictorCard(
    elapsedMs: Long,
    lapCount: Int,
    averageSplitMs: Long,
    selectedDistanceMeters: Int,
    onSelectDistanceMeters: (Int) -> Unit,
    isId: Boolean
) {
    val referenceMs = max(1000L, averageSplitMs)
    val speedMps = selectedDistanceMeters.toDouble() / (referenceMs / 1000.0)
    val speedKmh = (speedMps * 3.6).coerceIn(0.1, 120.0)
    val secPerKm = (1000.0 / max(0.1, speedMps)).roundToInt()
    val paceMin = secPerKm / 60
    val paceSec = secPerKm % 60

    fun formatProjectedSec(totalSec: Int): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
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
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = ChronosGreenNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isId) "KALKULATOR KECEPATAN & PREDIKSI FINIS"
                        else "LIVE SPEED & FINISH TIME PREDICTOR",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosGreenNeon
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(100 to "100m", 400 to "400m", 1000 to "1km").forEach { (meters, label) ->
                        val active = selectedDistanceMeters == meters
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (active) ChronosGreenNeon else Color.White.copy(alpha = 0.06f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSelectDistanceMeters(meters) }
                        ) {
                            Text(
                                text = label,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) ChronosBgDeep else Color(0xFFD1D5DB),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMetricBox(
                    label = if (isId) "KECEPATAN" else "SPEED",
                    value = "%.1f km/j".format(Locale.US, speedKmh),
                    color = ChronosGreenNeon,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    label = "PACE / KM",
                    value = "%02d'%02d\"/km".format(paceMin, paceSec),
                    color = ChronosCyanNeon,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    label = if (isId) "PROYEKSI 5 KM" else "5K FINISH",
                    value = formatProjectedSec(secPerKm * 5),
                    color = ChronosAmberNeon,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    label = if (isId) "PROYEKSI 10 KM" else "10K FINISH",
                    value = formatProjectedSec(secPerKm * 10),
                    color = ChronosPurpleNeon,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StopwatchAnalyticsCard(
    splitEntries: List<LapSplitEntry>,
    fastestSplitMs: Long,
    slowestSplitMs: Long,
    averageSplitMs: Long,
    consistencyScore: Int,
    predictedNextLapMs: Long,
    targetLapMs: Long,
    isId: Boolean
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ChronosSurfaceDark.copy(alpha = 0.90f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.25f)),
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
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = ChronosCyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isId) "ANALITIK PERFORMA & GRAFIK RITME PUTARAN"
                        else "PERFORMANCE ANALYTICS & LAP RHYTHM CHART",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosCyanNeon
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ChronosGreenNeon.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${if (isId) "Konsistensi" else "Consistency"} $consistencyScore%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosGreenNeon,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsMetricBox(
                    label = if (isId) "TERCEPAT" else "FASTEST",
                    value = formatStopwatchTime(fastestSplitMs, true),
                    color = ChronosGreenNeon,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    label = if (isId) "RATA-RATA" else "AVERAGE",
                    value = formatStopwatchTime(averageSplitMs, true),
                    color = ChronosCyanNeon,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricBox(
                    label = if (isId) "PROYEKSI NEXT" else "PREDICTED",
                    value = formatStopwatchTime(predictedNextLapMs, true),
                    color = ChronosAmberNeon,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Lap Bar Chart (chronological up to last 12 laps)
            val recentChrono = splitEntries.take(12).reversed()
            val maxBarMs = max(targetLapMs, slowestSplitMs).coerceAtLeast(1000L)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF08111E))
                    .padding(10.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = recentChrono.size
                    if (count > 0) {
                        val slotW = size.width / count.toFloat()
                        val barW = min(slotW * 0.62f, 22.dp.toPx())

                        // Target pace dashed reference line
                        val targetRatio = (targetLapMs.toFloat() / maxBarMs.toFloat()).coerceIn(0.1f, 0.95f)
                        val targetY = size.height * (1f - targetRatio)
                        drawLine(
                            color = ChronosAmberNeon.copy(alpha = 0.45f),
                            start = Offset(0f, targetY),
                            end = Offset(size.width, targetY),
                            strokeWidth = 1.2.dp.toPx()
                        )

                        recentChrono.forEachIndexed { i, item ->
                            val ratio = (item.splitDurationMs.toFloat() / maxBarMs.toFloat()).coerceIn(0.12f, 1f)
                            val barH = size.height * ratio
                            val x = i * slotW + (slotW - barW) / 2f
                            val y = size.height - barH
                            val barColor = when {
                                item.splitDurationMs == fastestSplitMs -> ChronosGreenNeon
                                item.splitDurationMs <= targetLapMs -> ChronosCyanNeon
                                else -> ChronosAmberNeon
                            }
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barW, barH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IntervalConfigBox(
    label: String,
    options: List<Int>,
    selected: Int,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { opt ->
                    val isSel = opt == selected
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSel) accent else Color.White.copy(alpha = 0.05f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelect(opt) }
                    ) {
                        Text(
                            text = "$opt$unit",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) ChronosBgDeep else Color(0xFFD1D5DB),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatStopwatchTime(ms: Long, showMs: Boolean): String {
    val safeMs = max(0L, ms)
    val totalSeconds = safeMs / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val centiseconds = (safeMs % 1000L) / 10L
    val base = if (hours > 0L) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
    return if (showMs) "$base.%02d".format(centiseconds) else base
}

private fun buildStopwatchReportText(
    activityTag: String,
    totalMs: Long,
    splits: List<LapSplitEntry>,
    fastestMs: Long,
    avgMs: Long,
    consistency: Int,
    isId: Boolean
): String {
    val sb = StringBuilder()
    sb.appendLine("=== CHRONOS SMART STOPWATCH PRO REPORT ===")
    sb.appendLine("${if (isId) "Aktivitas" else "Activity"}: $activityTag")
    sb.appendLine("${if (isId) "Total Waktu" else "Total Time"}: ${formatStopwatchTime(totalMs, true)}")
    if (splits.isNotEmpty()) {
        sb.appendLine("${if (isId) "Jumlah Putaran" else "Lap Count"}: ${splits.size}")
        sb.appendLine("${if (isId) "Lap Tercepat" else "Fastest Lap"}: ${formatStopwatchTime(fastestMs, true)}")
        sb.appendLine("${if (isId) "Rata-rata Lap" else "Average Lap"}: ${formatStopwatchTime(avgMs, true)}")
        sb.appendLine("${if (isId) "Skor Konsistensi" else "Consistency Score"}: $consistency%")
        sb.appendLine("--- Splits ---")
        splits.reversed().forEach { s ->
            sb.appendLine("Lap #${s.lapNumber}: +${formatStopwatchTime(s.splitDurationMs, true)} (Total ${formatStopwatchTime(s.cumulativeMs, true)})")
        }
    }
    return sb.toString()
}

private fun copyToClipboard(context: Context, text: String) {
    try {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cb?.setPrimaryClip(ClipData.newPlainText("CHRONOS Stopwatch Report", text))
    } catch (_: Throwable) {
    }
}
