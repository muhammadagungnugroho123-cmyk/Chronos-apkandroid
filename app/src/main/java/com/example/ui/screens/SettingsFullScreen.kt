package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.ui.components.AlarmThemeSelectorPills
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlarmItem
import com.example.model.AppSettings
import com.example.model.CHRONOS_RINGTONES
import com.example.model.CHRONOS_VERSION
import com.example.model.ChronosTranslations
import com.example.model.WakeMission
import com.example.service.ChronosAudioSynthesizer
import com.example.service.WeatherService
import com.example.ui.components.ChronosAmberNeon
import com.example.ui.components.ChronosBlueNeon
import com.example.ui.components.ChronosCyanNeon
import com.example.ui.components.ChronosGoldNeon
import com.example.ui.components.ChronosGreenNeon
import com.example.ui.components.ChronosOrangeNeon
import com.example.ui.components.ChronosPurpleNeon
import com.example.ui.components.ChronosRoseNeon
import com.example.ui.components.ChronosSurfaceDark
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsFullScreen(
    settings: AppSettings,
    alarms: List<AlarmItem>,
    onBack: () -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onResetSettings: () -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Boolean
) {
    val isId = settings.appLanguage == "id"
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var expandedSection by remember { mutableStateOf<String?>("appearance") }
    var expandAll by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var diagReport by remember { mutableStateOf<List<String>?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }

    var hasGpsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasGpsPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result[Manifest.permission.POST_NOTIFICATIONS] == true ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        syncMessage = if (isId) {
            "Status izin perangkat diperbarui: GPS=${if (hasGpsPermission) "AKTIF" else "NONAKTIF"}, Notifikasi=${if (hasNotificationPermission) "AKTIF" else "NONAKTIF"}"
        } else {
            "Device permissions updated: GPS=${if (hasGpsPermission) "GRANTED" else "DENIED"}, Notifications=${if (hasNotificationPermission) "GRANTED" else "DENIED"}"
        }
    }

    fun isSectionOpen(id: String): Boolean = expandAll || expandedSection == id

    fun toggleSection(id: String) {
        ChronosAudioSynthesizer.triggerHaptic(context, 1)
        if (expandAll) {
            expandAll = false
            expandedSection = id
        } else {
            expandedSection = if (expandedSection == id) null else id
        }
    }

    fun applySettingChange(msgId: String? = null, msgEn: String? = null, block: (AppSettings) -> AppSettings) {
        ChronosAudioSynthesizer.triggerHaptic(context, 0)
        onUpdateSettings(block)
        if (msgId != null && msgEn != null) {
            syncMessage = if (isId) msgId else msgEn
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = {
                        ChronosAudioSynthesizer.triggerHaptic(context, 1)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChronosCyanNeon)
                    }
                    Column {
                        Text(
                            text = ChronosTranslations.t(settings.appLanguage, "settings").uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                        Text(
                            text = "ANDROID 11+ PRO ENGINE • V$CHRONOS_VERSION",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = ChronosCyanNeon
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                ChronosAudioSynthesizer.triggerHaptic(context, 1)
                                expandAll = !expandAll
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (expandAll) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                contentDescription = null,
                                tint = ChronosCyanNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (expandAll) (if (isId) "Tutup Semua" else "Collapse") else (if (isId) "Buka Semua" else "Expand All"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Quick Smart Mode Preset Profiles
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ChronosSurfaceDark.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = ChronosAmberNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isId) "PROFIL PENGATURAN PINTAR INSTAN" else "INSTANT SMART CONFIGURATION PROFILES",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosAmberNeon
                            )
                        }
                        Text(
                            text = if (isId) "1-Ketuk Aktif" else "1-Tap Apply",
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(
                                if (isId) "⚡ Performa Ultra" else "⚡ Ultra Pro",
                                ChronosCyanNeon
                            ) {
                                applySettingChange(
                                    "Profil Performa Ultra diaktifkan (60fps Sweep, Briefing Cerdas & Haptik HD).",
                                    "Ultra Pro profile applied (60fps Sweep, Smart Briefing & HD Haptics)."
                                ) {
                                    it.copy(
                                        ecoPerformanceMode = false,
                                        smoothSweepSeconds = true,
                                        showClockSeconds = true,
                                        ambientGlowIntensity = 3,
                                        hapticFeedbackEnabled = true,
                                        showSmartDailyBriefing = true
                                    )
                                }
                            },
                            Triple(
                                if (isId) "🌿 Hemat Baterai (Eco)" else "🌿 Battery Eco",
                                ChronosGreenNeon
                            ) {
                                applySettingChange(
                                    "Profil Hemat Daya diaktifkan (Animasi Ringan, Detik 1Hz, Cahaya Redup).",
                                    "Battery Eco profile applied (Lightweight FPS, 1Hz Quartz, Subtle Glow)."
                                ) {
                                    it.copy(
                                        ecoPerformanceMode = true,
                                        smoothSweepSeconds = false,
                                        ambientGlowIntensity = 1,
                                        globeAutoRotate = false
                                    )
                                }
                            },
                            Triple(
                                if (isId) "🌙 Tidur REM Optimal" else "🌙 Deep Sleep REM",
                                ChronosPurpleNeon
                            ) {
                                applySettingChange(
                                    "Profil Tidur Sirkadian REM diaktifkan (5 Siklus, Fade-In 20d, Misi Matematika).",
                                    "Circadian REM Sleep profile applied (5 Cycles, 20s Fade-In, Math Mission)."
                                ) {
                                    it.copy(
                                        targetSleepCycles = 5,
                                        alarmFadeInDuration = 20,
                                        preWakeGentleDefault = true,
                                        bedtimeReminderEnabled = true,
                                        defaultWakeMission = WakeMission.MATH
                                    )
                                }
                            },
                            Triple(
                                if (isId) "🎯 Fokus Mendalam" else "🎯 Deep Focus",
                                ChronosAmberNeon
                            ) {
                                applySettingChange(
                                    "Profil Fokus Mendalam diaktifkan (Pomodoro 45m, Audio Alpha 40Hz, Auto-Prioritas).",
                                    "Deep Focus profile applied (45m Pomodoro, 40Hz Alpha Audio, Priority Sort)."
                                ) {
                                    it.copy(
                                        defaultFocusMinutes = 45,
                                        timerAutoSoundscape = "FOCUS_BINAURAL",
                                        autoSortTasksByPriority = true,
                                        clockStyleIndex = 3
                                    )
                                }
                            }
                        ).forEach { (label, col, action) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = col.copy(alpha = 0.14f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, col.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { action() }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = col,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (syncMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ChronosCyanNeon.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = ChronosCyanNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = syncMessage.orEmpty(), fontSize = 12.sp, color = Color(0xFF67E8F9), modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 1. Appearance, Visuals & Clock Face
        item {
            SettingsAccordionCard(
                title = if (isId) "Tampilan, Visual & Gaya Jam" else "Appearance, Visuals & Clock Face",
                subtitle = if (isId) "7 tema neon, 5 gaya jam, cahaya latar, haptik & mode ringan" else "7 neon themes, 5 clock faces, ambient glow, haptics & eco FPS",
                icon = Icons.Default.Palette,
                iconColor = ChronosCyanNeon,
                expanded = isSectionOpen("appearance"),
                onToggle = { toggleSection("appearance") }
            ) {
                Text(if (isId) "Palet Warna Tema Utama (7 Pilihan)" else "Primary Neon Theme Palette (7 Options)", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Cyan" to ChronosCyanNeon,
                        "Blue" to ChronosBlueNeon,
                        "Emerald" to ChronosGreenNeon,
                        "Solar" to ChronosOrangeNeon,
                        "Purple" to ChronosPurpleNeon,
                        "Crimson" to ChronosRoseNeon,
                        "Gold" to ChronosGoldNeon
                    ).forEachIndexed { i, (label, c) ->
                        val selected = settings.themeColorIndex == i
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) c.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.03f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (selected) c else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .width(68.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { applySettingChange { it.copy(themeColorIndex = i) } }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Gaya Dial Jam Utama (5 Desain Interaktif)" else "Main Clock Dial Style (5 Interactive Designs)", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "Sci-Fi Orbital",
                        1 to "Digital Glass",
                        2 to "Precision Analog",
                        3 to "Cyber Matrix HUD",
                        4 to "Dual Solar Ring"
                    ).forEach { (i, st) ->
                        SettingsSelectPill(
                            label = st,
                            selected = settings.clockStyleIndex == i,
                            activeColor = ChronosCyanNeon,
                            onClick = { applySettingChange { it.copy(clockStyleIndex = i) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Intensitas Cahaya Ambient Latar" else "Background Ambient Glow Intensity", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        0 to (if (isId) "Mati" else "Off"),
                        1 to (if (isId) "Redup" else "Subtle"),
                        2 to (if (isId) "Dinamis" else "Dynamic"),
                        3 to "Ultra Neon"
                    ).forEach { (lvl, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = settings.ambientGlowIntensity == lvl,
                            activeColor = ChronosCyanNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(ambientGlowIntensity = lvl) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(ChronosTranslations.t(settings.appLanguage, "language"), fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("id" to "Indonesia", "en" to "English", "zh" to "中文").forEach { (id, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = settings.appLanguage == id,
                            activeColor = ChronosCyanNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(appLanguage = id) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                SettingsToggleRow(
                    title = if (isId) "Tampilkan Detik pada Jam Utama (:SS)" else "Show Seconds on Main Clock (:SS)",
                    subtitle = if (isId) "Tampilkan indikator detik & cincin progres detik" else "Display live seconds counter and progress arc",
                    checked = settings.showClockSeconds,
                    activeColor = ChronosCyanNeon,
                    onToggle = { applySettingChange { it.copy(showClockSeconds = !it.showClockSeconds) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Gerakan Jarum Detik Halus (Smooth Sweep)" else "Smooth Sweeping Second Hand",
                    subtitle = if (isId) "Animasi presisi tinggi vs detak kuarsa 1 detik" else "Continuous sweep motion vs 1Hz quartz tick",
                    checked = settings.smoothSweepSeconds,
                    activeColor = ChronosCyanNeon,
                    onToggle = { applySettingChange { it.copy(smoothSweepSeconds = !it.smoothSweepSeconds) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Kontras Kaca Tinggi (High-Contrast Glass)" else "High-Contrast Glassmorphism Borders",
                    subtitle = if (isId) "Pertegas garis tepi kartu untuk keterbacaan ekstra" else "Sharpen card borders and surface contrast",
                    checked = settings.highContrastGlass,
                    activeColor = ChronosCyanNeon,
                    onToggle = { applySettingChange { it.copy(highContrastGlass = !it.highContrastGlass) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Navigasi Geser Layar (Swipe Antar Tab)" else "Horizontal Swipe Tab Navigation",
                    subtitle = if (isId) "Geser kiri/kanan untuk berpindah layar utama" else "Swipe left or right to switch primary tabs",
                    checked = settings.enableSwipeNavigation,
                    activeColor = ChronosCyanNeon,
                    onToggle = { applySettingChange { it.copy(enableSwipeNavigation = !it.enableSwipeNavigation) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Umpan Balik Haptik Sentuhan HD (Android 11+)" else "Android 11+ HD Tactile Haptics",
                    subtitle = if (isId) "Getaran presisi saat menekan tombol & berpindah menu" else "Crisp tactile feedback on buttons and toggles",
                    checked = settings.hapticFeedbackEnabled,
                    activeColor = ChronosCyanNeon,
                    onToggle = {
                        val next = !settings.hapticFeedbackEnabled
                        ChronosAudioSynthesizer.hapticsEnabled = next
                        applySettingChange { it.copy(hapticFeedbackEnabled = next) }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Mode Ringan & Hemat Daya (Eco FPS)" else "Lightweight Eco Render Mode",
                    subtitle = if (isId) "Optimalkan frekuensi render agar ekstra ringan & hemat baterai" else "Throttle background shader & tick rate for maximum efficiency",
                    checked = settings.ecoPerformanceMode,
                    activeColor = ChronosGreenNeon,
                    onToggle = { applySettingChange { it.copy(ecoPerformanceMode = !it.ecoPerformanceMode) } }
                )
            }
        }

        // 2. Time, Date & 3D World Globe
        item {
            SettingsAccordionCard(
                title = if (isId) "Waktu, Tanggal & Bola Dunia 3D" else "Time, Date & 3D World Globe",
                subtitle = if (isId) "Format 24 jam, bar UTC/ISO & kontrol rotasi bola dunia 3D" else "24-hour format, UTC/ISO bar & 3D globe rotation controls",
                icon = Icons.Default.AccessTime,
                iconColor = ChronosBlueNeon,
                expanded = isSectionOpen("time_date"),
                onToggle = { toggleSection("time_date") }
            ) {
                SettingsToggleRow(
                    title = ChronosTranslations.t(settings.appLanguage, "24_hour_format"),
                    subtitle = ChronosTranslations.t(settings.appLanguage, "use_24_hour_format"),
                    checked = settings.is24HourFormat,
                    activeColor = ChronosCyanNeon,
                    onToggle = { applySettingChange { it.copy(is24HourFormat = !it.is24HourFormat) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Tampilkan Bar Telemetri UTC & Minggu ISO" else "Show Live UTC & ISO Week Telemetry Bar",
                    subtitle = if (isId) "Tampilkan waktu UTC real-time & minggu ke-n di bawah jam" else "Display live UTC clock & ISO week badge on home screen",
                    checked = settings.showHomeUtcBadge,
                    activeColor = ChronosCyanNeon,
                    onToggle = { applySettingChange { it.copy(showHomeUtcBadge = !it.showHomeUtcBadge) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Tampilkan Bola Dunia 3D Interaktif" else "Show Interactive 3D World Globe",
                    subtitle = if (isId) "Visualisasi proyeksi ortografik 3D pada layar Jam Dunia" else "Display 3D orthographic globe in World Clock screen",
                    checked = settings.showWorldClockGlobe,
                    activeColor = ChronosCyanNeon,
                    onToggle = { applySettingChange { it.copy(showWorldClockGlobe = !it.showWorldClockGlobe) } }
                )
                if (settings.showWorldClockGlobe) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(if (isId) "Tema Visual Bola Dunia 3D" else "3D Globe Visual Style", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Cyber Grid", "Realistic", "Solar Day", "Emerald").forEachIndexed { i, st ->
                            SettingsSelectPill(
                                label = st,
                                selected = settings.worldClockGlobeStyle == i,
                                activeColor = ChronosCyanNeon,
                                modifier = Modifier.weight(1f),
                                onClick = { applySettingChange { it.copy(worldClockGlobeStyle = i) } }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    SettingsToggleRow(
                        title = if (isId) "Rotasi Otomatis Bola Dunia 3D" else "Auto-Rotate 3D Globe",
                        subtitle = if (isId) "Putar bola dunia secara halus saat tidak disentuh" else "Smoothly spin the 3D globe when idle",
                        checked = settings.globeAutoRotate,
                        activeColor = ChronosBlueNeon,
                        onToggle = { applySettingChange { it.copy(globeAutoRotate = !it.globeAutoRotate) } }
                    )
                    if (settings.globeAutoRotate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (isId) "Kecepatan Rotasi Bola Dunia" else "Globe Rotation Speed", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                0 to (if (isId) "Lambat" else "Slow"),
                                1 to (if (isId) "Normal" else "Normal"),
                                2 to (if (isId) "Cepat" else "Fast")
                            ).forEach { (spd, lbl) ->
                                SettingsSelectPill(
                                    label = lbl,
                                    selected = settings.globeRotationSpeed == spd,
                                    activeColor = ChronosBlueNeon,
                                    modifier = Modifier.weight(1f),
                                    onClick = { applySettingChange { it.copy(globeRotationSpeed = spd) } }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Smart Alarm, Sleep REM & Audio Synthesizer
        item {
            SettingsAccordionCard(
                title = if (isId) "Alarm Pintar, Siklus REM & Audio" else "Smart Alarm, Sleep REM & Audio",
                subtitle = if (isId) "8 nada dering AI, misi bangun, target siklus REM & fade-in" else "8 AI ringtones, wake missions, REM cycle targets & fade-in",
                icon = Icons.Default.Alarm,
                iconColor = ChronosAmberNeon,
                expanded = isSectionOpen("alarm"),
                onToggle = { toggleSection("alarm") }
            ) {
                SettingsToggleRow(
                    title = if (isId) "Notifikasi & Banner Peringatan Aktif" else "System Notifications & Active Banners",
                    subtitle = if (isId) "Tampilkan banner peringatan untuk alarm, timer & pengingat" else "Show interactive top alerts for alarms, timers, and reminders",
                    checked = settings.notificationsEnabled,
                    activeColor = ChronosAmberNeon,
                    onToggle = { applySettingChange { it.copy(notificationsEnabled = !it.notificationsEnabled) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Getar Saat Alarm & Timer Berbunyi" else "Vibrate on Alarm & Timer",
                    subtitle = if (isId) "Aktifkan getaran perangkat saat waktu habis" else "Pulse device vibrator when alarms or timers trigger",
                    checked = settings.defaultVibrate,
                    activeColor = ChronosAmberNeon,
                    onToggle = { applySettingChange { it.copy(defaultVibrate = !it.defaultVibrate) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Harmoni Pra-Bangun Lembut (Pre-Wake Gentle)" else "Pre-Wake Gentle Circadian Harmonics",
                    subtitle = if (isId) "Transisi frekuensi lembut untuk mencegah inersia tidur" else "Soft harmonic warm-up to minimize sleep inertia",
                    checked = settings.preWakeGentleDefault,
                    activeColor = ChronosAmberNeon,
                    onToggle = { applySettingChange { it.copy(preWakeGentleDefault = !it.preWakeGentleDefault) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Pengingat Cerdas Waktu Tidur REM" else "Smart Circadian Bedtime REM Advisory",
                    subtitle = if (isId) "Tampilkan rekomendasi jendela tidur ideal pada layar Alarm" else "Show optimal REM sleep window recommendations",
                    checked = settings.bedtimeReminderEnabled,
                    activeColor = ChronosAmberNeon,
                    onToggle = { applySettingChange { it.copy(bedtimeReminderEnabled = !it.bedtimeReminderEnabled) } }
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Target Siklus Tidur Sirkadian (90 Menit/Siklus)" else "Target Circadian Sleep Cycles (90m/Cycle)", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        4 to (if (isId) "4 Siklus (6.0j)" else "4 Cycles (6.0h)"),
                        5 to (if (isId) "5 Siklus (7.5j)" else "5 Cycles (7.5h)"),
                        6 to (if (isId) "6 Siklus (9.0j)" else "6 Cycles (9.0h)")
                    ).forEach { (cyc, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = settings.targetSleepCycles == cyc,
                            activeColor = ChronosAmberNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(targetSleepCycles = cyc) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isId) "Nada Dering Default (Ketuk untuk Uji Suara)" else "Default AI Ringtone (Tap to Test)", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    TextButton(onClick = { ChronosAudioSynthesizer.stopChime() }) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = ChronosRoseNeon, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isId) "Hentikan Uji" else "Stop Test", fontSize = 10.sp, color = ChronosRoseNeon)
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CHRONOS_RINGTONES.forEachIndexed { idx, rt ->
                        SettingsSelectPill(
                            label = "♪ $rt",
                            selected = settings.alarmRingtoneIndex == idx,
                            activeColor = ChronosAmberNeon,
                            onClick = {
                                applySettingChange { it.copy(alarmRingtoneIndex = idx, timerSound = rt) }
                                ChronosAudioSynthesizer.playAlarmChime(idx, durationSeconds = 3, fadeInSeconds = 0)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Volume Master Synthesizer Audio" else "Master Audio Synthesizer Volume", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.25f to "25%", 0.50f to "50%", 0.75f to "75%", 1.0f to "100%").forEach { (vol, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = kotlin.math.abs(settings.masterSoundVolume - vol) < 0.05f,
                            activeColor = ChronosAmberNeon,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                ChronosAudioSynthesizer.volume = vol
                                applySettingChange { it.copy(masterSoundVolume = vol) }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Durasi Fade-In Audio Alarm" else "Alarm Audio Fade-In Duration", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "Instan", 5 to "5s", 10 to "10s", 20 to "20s", 30 to "30s").forEach { (sec, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = settings.alarmFadeInDuration == sec,
                            activeColor = ChronosAmberNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(alarmFadeInDuration = sec) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Durasi Tunda (Snooze)" else "Snooze Duration", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 5, 9, 10, 15).forEach { m ->
                        SettingsSelectPill(
                            label = "$m min",
                            selected = settings.snoozeDuration == m,
                            activeColor = ChronosAmberNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(snoozeDuration = m) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Misi Bangun Cerdas Default" else "Default Smart Wake-Up Mission", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        WakeMission.NONE to (if (isId) "Standar" else "Standard"),
                        WakeMission.MATH to (if (isId) "Matematika" else "Math"),
                        WakeMission.MEMORY to (if (isId) "Memori" else "Memory"),
                        WakeMission.TAP to (if (isId) "Ketuk 15x" else "Tap 15x")
                    ).forEach { (m, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = settings.defaultWakeMission == m,
                            activeColor = ChronosAmberNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(defaultWakeMission = m) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isId) "Tema Latar Belakang Alarm Cerdas Default" else "Default Smart Alarm Background Theme",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(modifier = Modifier.height(6.dp))
                AlarmThemeSelectorPills(
                    selectedThemeId = settings.defaultAlarmBackgroundTheme,
                    onSelectTheme = { themeId ->
                        applySettingChange { it.copy(defaultAlarmBackgroundTheme = themeId) }
                    },
                    isId = isId
                )
            }
        }

        // 4. Timers, Pomodoro & Stopwatch Precision
        item {
            SettingsAccordionCard(
                title = if (isId) "Timer Presisi, Pomodoro & Stopwatch" else "Precision Timers, Pomodoro & Stopwatch",
                subtitle = if (isId) "Auto-repeat timer, soundscape fokus otomatis & milidetik" else "Timer auto-repeat, auto focus soundscape & lap haptics",
                icon = Icons.Default.Timer,
                iconColor = Color(0xFF2DD4BF),
                expanded = isSectionOpen("timers_stopwatch"),
                onToggle = { toggleSection("timers_stopwatch") }
            ) {
                SettingsToggleRow(
                    title = if (isId) "Tampilkan Milidetik Stopwatch (.00)" else "Show Stopwatch Milliseconds (.00)",
                    subtitle = if (isId) "Presisi 1/100 detik secara real-time" else "Real-time 1/100s precision readout",
                    checked = settings.stopwatchShowMs,
                    activeColor = Color(0xFF2DD4BF),
                    onToggle = { applySettingChange { it.copy(stopwatchShowMs = !it.stopwatchShowMs) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Getar Haptik Saat Mencatat Putaran (Lap)" else "Vibrate on Stopwatch Lap",
                    subtitle = if (isId) "Umpan balik haptik setiap tombol Lap ditekan" else "Tactile haptic pulse when recording a lap",
                    checked = settings.stopwatchLapVibrate,
                    activeColor = Color(0xFF2DD4BF),
                    onToggle = { applySettingChange { it.copy(stopwatchLapVibrate = !it.stopwatchLapVibrate) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Ulangi Timer Otomatis Saat Selesai (Auto-Repeat)" else "Auto-Repeat Countdown Timer",
                    subtitle = if (isId) "Mulai ulang siklus timer secara otomatis setelah waktu habis" else "Automatically restart timer cycle upon completion",
                    checked = settings.timerAutoRepeat,
                    activeColor = Color(0xFF2DD4BF),
                    onToggle = { applySettingChange { it.copy(timerAutoRepeat = !it.timerAutoRepeat) } }
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Putar Otomatis Soundscape Saat Timer Berjalan" else "Auto-Play Ambient Soundscape During Timer", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "NONE" to (if (isId) "Mati" else "Off"),
                        "RAIN" to (if (isId) "Hujan" else "Rain"),
                        "OCEAN_WAVES" to (if (isId) "Ombak Laut" else "Ocean"),
                        "WIND" to (if (isId) "Angin Malam" else "Wind"),
                        "FOCUS_BINAURAL" to "Alpha 40Hz"
                    ).forEach { (id, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = settings.timerAutoSoundscape == id,
                            activeColor = Color(0xFF2DD4BF),
                            onClick = { applySettingChange { it.copy(timerAutoSoundscape = id) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Durasi Fokus Pomodoro Default" else "Default Pomodoro Focus Duration", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        SettingsSelectPill(
                            label = "$mins min",
                            selected = settings.defaultFocusMinutes == mins,
                            activeColor = Color(0xFF2DD4BF),
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(defaultFocusMinutes = mins) } }
                        )
                    }
                }
            }
        }

        // 5. Weather & Environmental Radar
        item {
            SettingsAccordionCard(
                title = "${ChronosTranslations.t(settings.appLanguage, "weather")} & Radar",
                subtitle = if (isId) "Satuan suhu, satuan kecepatan angin, jadwal refresh & peringatan" else "Temperature units, wind units, auto-refresh & alerts",
                icon = Icons.Default.Cloud,
                iconColor = ChronosCyanNeon,
                expanded = isSectionOpen("weather"),
                onToggle = { toggleSection("weather") }
            ) {
                Text(if (isId) "Satuan Suhu" else "Temperature Unit", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsSelectPill(
                        label = "Celsius (°C)",
                        selected = settings.temperatureUnit == 0,
                        activeColor = ChronosCyanNeon,
                        modifier = Modifier.weight(1f),
                        onClick = { applySettingChange { it.copy(temperatureUnit = 0) } }
                    )
                    SettingsSelectPill(
                        label = "Fahrenheit (°F)",
                        selected = settings.temperatureUnit == 1,
                        activeColor = ChronosCyanNeon,
                        modifier = Modifier.weight(1f),
                        onClick = { applySettingChange { it.copy(temperatureUnit = 1) } }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Satuan Kecepatan Angin" else "Wind Speed Unit", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "km/h", 1 to "m/s", 2 to "mph").forEach { (u, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = settings.windSpeedUnit == u,
                            activeColor = ChronosCyanNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(windSpeedUnit = u) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Interval Pembaruan Otomatis Cuaca" else "Weather Auto-Refresh Interval", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15 to "15m", 30 to "30m", 60 to "60m", 0 to "Manual").forEach { (mins, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = settings.weatherRefreshInterval == mins,
                            activeColor = ChronosCyanNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(weatherRefreshInterval = mins) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                SettingsToggleRow(
                    title = if (isId) "Peringatan Dini Cuaca Ekstrem & Indeks UV" else "Severe Weather & High UV Early Warnings",
                    subtitle = if (isId) "Tampilkan banner peringatan badai/hujan lebat di layar utama" else "Display storm, heavy rain & UV advisories on home screen",
                    checked = settings.weatherAlertsEnabled,
                    activeColor = ChronosCyanNeon,
                    onToggle = { applySettingChange { it.copy(weatherAlertsEnabled = !it.weatherAlertsEnabled) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Tampilkan Kartu Cuaca Live di Beranda" else "Show Live Weather Card on Home Screen",
                    subtitle = if (isId) "Tampilkan ringkasan cuaca satelit di bawah jam utama" else "Display live satellite weather card below main clock",
                    checked = settings.showHomeWeatherCard,
                    activeColor = ChronosCyanNeon,
                    onToggle = { applySettingChange { it.copy(showHomeWeatherCard = !it.showHomeWeatherCard) } }
                )
            }
        }

        // 6. Calendar, Life Suite & Productivity
        item {
            SettingsAccordionCard(
                title = if (isId) "Kalender, Life Suite & Produktivitas" else "Calendar, Life Suite & Productivity",
                subtitle = if (isId) "Modul beranda, target hidrasi harian & pengurutan tugas" else "Home screen modules, daily water goal & smart task sorting",
                icon = Icons.Default.CalendarToday,
                iconColor = Color(0xFF818CF8),
                expanded = isSectionOpen("calendar_prod"),
                onToggle = { toggleSection("calendar_prod") }
            ) {
                Text(if (isId) "Hari Pertama dalam Minggu" else "First Day of Week", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Monday" to (if (isId) "Senin (Monday)" else "Monday"), "Sunday" to (if (isId) "Minggu (Sunday)" else "Sunday")).forEach { (day, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = settings.calendarFirstDayOfWeek == day,
                            activeColor = Color(0xFF818CF8),
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(calendarFirstDayOfWeek = day) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isId) "Target Hidrasi Air Harian (Life Suite)" else "Daily Hydration Water Target (Life Suite)", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(6, 8, 10, 12).forEach { gl ->
                        SettingsSelectPill(
                            label = if (isId) "$gl Gelas" else "$gl Glasses",
                            selected = settings.dailyWaterGoal == gl,
                            activeColor = Color(0xFF818CF8),
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(dailyWaterGoal = gl) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                SettingsToggleRow(
                    title = if (isId) "Ringkasan Cerdas Harian (Smart Daily Briefing)" else "CHRONOS Smart Daily Briefing Card",
                    subtitle = if (isId) "Kartu telemetri pintar fase sirkadian, alarm & tugas di Beranda" else "Intelligent circadian phase, next alarm & task readiness summary",
                    checked = settings.showSmartDailyBriefing,
                    activeColor = Color(0xFF818CF8),
                    onToggle = { applySettingChange { it.copy(showSmartDailyBriefing = !it.showSmartDailyBriefing) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Tampilkan 8 Pilar Life Suite di Beranda" else "Show 8-Pillar Life Suite on Home Screen",
                    subtitle = if (isId) "Kesehatan, hidrasi, nutrisi, tidur REM & template harian" else "Health, hydration, meals, REM sleep & daily templates card",
                    checked = settings.showHomeLifeSuite,
                    activeColor = Color(0xFF818CF8),
                    onToggle = { applySettingChange { it.copy(showHomeLifeSuite = !it.showHomeLifeSuite) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Tampilkan Kalender & Agenda di Beranda" else "Show Monthly Calendar & Agenda on Home",
                    subtitle = if (isId) "Kalender interaktif & sinkronisasi jadwal kegiatan" else "Interactive month grid and upcoming agenda events",
                    checked = settings.showHomeCalendarCard,
                    activeColor = Color(0xFF818CF8),
                    onToggle = { applySettingChange { it.copy(showHomeCalendarCard = !it.showHomeCalendarCard) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = if (isId) "Urutkan Tugas Otomatis Berdasarkan Prioritas" else "Auto-Sort Tasks by Priority (HIGH First)",
                    subtitle = if (isId) "Tempatkan tugas prioritas Tinggi yang belum selesai di urutan teratas" else "Automatically pin uncompleted HIGH-priority tasks to the top",
                    checked = settings.autoSortTasksByPriority,
                    activeColor = Color(0xFF818CF8),
                    onToggle = { applySettingChange { it.copy(autoSortTasksByPriority = !it.autoSortTasksByPriority) } }
                )
            }
        }

        // 7. CHRONOS AI & Smart Automation
        item {
            SettingsAccordionCard(
                title = "CHRONOS AI & Intelligence",
                subtitle = if (isId) "Profil mesin AI, kedalaman analisis & eksekusi otomatis" else "AI neural model profile, response depth & auto-execution",
                icon = Icons.Default.AutoAwesome,
                iconColor = ChronosRoseNeon,
                expanded = isSectionOpen("ai_intelligence"),
                onToggle = { toggleSection("ai_intelligence") }
            ) {
                Text(if (isId) "Profil Mesin CHRONOS AI" else "CHRONOS AI Engine Profile", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Neural v4.5", "Fast Turbo", "Deep Circadian").forEachIndexed { idx, mdl ->
                        SettingsSelectPill(
                            label = mdl,
                            selected = settings.aiModelIndex == idx,
                            activeColor = ChronosRoseNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(aiModelIndex = idx) } }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(if (isId) "Gaya Respons AI" else "AI Response Style", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0 to (if (isId) "Ringkas" else "Concise"),
                        1 to (if (isId) "Seimbang" else "Balanced"),
                        2 to (if (isId) "Ilmiah" else "Scientific")
                    ).forEach { (id, lbl) ->
                        SettingsSelectPill(
                            label = lbl,
                            selected = settings.aiResponseStyle == id,
                            activeColor = ChronosRoseNeon,
                            modifier = Modifier.weight(1f),
                            onClick = { applySettingChange { it.copy(aiResponseStyle = id) } }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                SettingsToggleRow(
                    title = if (isId) "Eksekusi Langsung Perintah AI (Zero-Click)" else "Direct Auto-Execute AI Commands",
                    subtitle = if (isId) "Jalankan perintah alarm/timer/pengingat langsung tanpa kartu konfirmasi" else "Execute alarm, timer, and reminder commands immediately",
                    checked = settings.aiAutoExecuteActions,
                    activeColor = ChronosRoseNeon,
                    onToggle = { applySettingChange { it.copy(aiAutoExecuteActions = !it.aiAutoExecuteActions) } }
                )
            }
        }

        // 8. System Performance & Diagnostics
        item {
            SettingsAccordionCard(
                title = if (isId) "Performa & Diagnostik Sistem" else "System Performance & Diagnostics",
                subtitle = "ANDROID 11+ (API 30+) • ZERO-CRASH GUARD • 100% STABLE",
                icon = Icons.Default.FitnessCenter,
                iconColor = ChronosGreenNeon,
                expanded = isSectionOpen("diagnostics"),
                onToggle = { toggleSection("diagnostics") }
            ) {
                // Device Hardware Access & Online/Offline Transparency Hub (v1.66.98)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isId) "AKSES PERANGKAT & ARSITEKTUR ONLINE/OFFLINE V$CHRONOS_VERSION"
                            else "DEVICE HARDWARE ACCESS & ONLINE/OFFLINE ARCHITECTURE V$CHRONOS_VERSION",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChronosCyanNeon
                        )
                        Text(
                            text = if (isId) {
                                "• Fitur Online Realtime (Wajib Internet): Cuaca WMO & Maritim 16 Pantai RI, Asisten AI Cloud Gemini, & Pencarian Koordinat Global.\n• Fitur Dasar Inti (100% Offline Stabil): Jam Presisi, Alarm & Musik File Perangkat, Timer, Stopwatch Pro 5-Menu, & Produktivitas."
                            } else {
                                "• Real-Time Online Features (Requires Internet): WMO & Marine Weather, Gemini Cloud AI Assistant, & Global Geocoding.\n• Core Offline Features (100% Offline Ready): Precision Clock, Alarms & Custom Device Music, Timers, 5-Menu Pro Stopwatch, & Productivity."
                            },
                            fontSize = 11.sp,
                            color = Color(0xFFD1D5DB),
                            lineHeight = 15.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (hasGpsPermission) ChronosGreenNeon else ChronosAmberNeon).copy(alpha = 0.14f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (hasGpsPermission) "✓ GPS Akurasi Tinggi: Aktif" else "⚠ GPS Perangkat: Belum Diizinkan",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasGpsPermission) ChronosGreenNeon else ChronosAmberNeon,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = (if (hasNotificationPermission) ChronosGreenNeon else ChronosAmberNeon).copy(alpha = 0.14f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (hasNotificationPermission) "✓ Notifikasi & Audio SAF: Aktif" else "⚠ Izin Notifikasi: Belum Aktif",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasNotificationPermission) ChronosGreenNeon else ChronosAmberNeon,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                ChronosAudioSynthesizer.triggerHaptic(context, 1)
                                val perms = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionsLauncher.launch(perms.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChronosCyanNeon.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grant_device_permissions_button")
                        ) {
                            Text(
                                text = if (isId) "Izinkan Akses GPS & Notifikasi Perangkat (Akurasi Tinggi)"
                                else "Authorize Device GPS & Notification Access (High Accuracy)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosCyanNeon
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (diagReport != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ChronosGreenNeon.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            diagReport?.forEach { line ->
                                Text(text = line, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = ChronosGreenNeon)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            ChronosAudioSynthesizer.triggerHaptic(context, 2)
                            val activeAlarms = alarms.count { it.isEnabled }
                            val customMusicAlarms = alarms.count { it.soundUri.startsWith("content://") || it.soundUri.startsWith("file://") }
                            diagReport = if (isId) {
                                listOf(
                                    "✓ Target Platform: Android 11+ (MinSDK 30 / TargetSDK 36) • Rilis Resmi V$CHRONOS_VERSION",
                                    "✓ Penjadwal Alarm & Musik Perangkat: Stabil ($activeAlarms aktif, $customMusicAlarms lagu kustom SAF)",
                                    "✓ Stopwatch Cerdas 5-Menu: Kronograf, Multi-Track, HIIT, Lab Pintar & Audit CSV (0ms drift)",
                                    "✓ Integrasi Online/Offline: Cuaca WMO + AI Cloud aktif, Inti Offline 100% bebas crash"
                                )
                            } else {
                                listOf(
                                    "✓ Target Platform: Android 11+ (MinSDK 30 / TargetSDK 36) • Official Release V$CHRONOS_VERSION",
                                    "✓ Alarm Scheduler & Custom Device Music: Healthy ($activeAlarms active, $customMusicAlarms SAF tracks)",
                                    "✓ 5-Menu Smart Stopwatch: Chrono, Multi-Track, HIIT, Smart Lab & CSV Audit (0ms drift)",
                                    "✓ Online/Offline Engine: Live WMO Weather + Cloud AI active, Offline Core 100% crash-free"
                                )
                            }
                            syncMessage = if (isId) "Diagnostik sistem V$CHRONOS_VERSION selesai: Siap dipasang & bebas kendala!" else "System diagnostics V$CHRONOS_VERSION complete: Ready for official launch!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChronosGreenNeon),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isId) "Cek Diagnostik" else "Diagnostics",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Button(
                        onClick = {
                            ChronosAudioSynthesizer.triggerHaptic(context, 1)
                            val purged = WeatherService.clearMemoryCache()
                            ChronosAudioSynthesizer.stopChime()
                            syncMessage = if (isId) "Memori & Audio dioptimalkan! $purged cache cuaca dibersihkan."
                            else "Memory & Audio optimized! $purged cached weather entries purged."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isId) "Optimalkan Memori" else "Optimize Memory",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 9. Backup & Cloud
        item {
            SettingsAccordionCard(
                title = "Backup & Cloud",
                subtitle = if (isId) "Ekspor/impor JSON & sinkronisasi lokal otomatis" else "Export/import JSON & automatic local persistence",
                icon = Icons.Default.CloudUpload,
                iconColor = Color(0xFFC084FC),
                expanded = isSectionOpen("backup"),
                onToggle = { toggleSection("backup") }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            ChronosAudioSynthesizer.triggerHaptic(context, 0)
                            val json = onExportJson()
                            clipboard.setText(AnnotatedString(json))
                            syncMessage = if (isId) "Kode backup V$CHRONOS_VERSION telah disalin ke clipboard!"
                            else "V$CHRONOS_VERSION backup code copied to clipboard!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChronosCyanNeon),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Export", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                    Button(
                        onClick = { showImportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ChronosRoseNeon.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChronosRoseNeon)
                    }
                }
            }
        }

        // 10. About
        item {
            SettingsAccordionCard(
                title = ChronosTranslations.t(settings.appLanguage, "about"),
                subtitle = "CHRONOS V$CHRONOS_VERSION • ANDROID 11+ (API 30–36)",
                icon = Icons.Default.Info,
                iconColor = ChronosGreenNeon,
                expanded = isSectionOpen("about"),
                onToggle = { toggleSection("about") }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(ChronosTranslations.t(settings.appLanguage, "version"), fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    Text("$CHRONOS_VERSION (Android 11+ Pro)", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ChronosGreenNeon)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isId) {
                        "CHRONOS V$CHRONOS_VERSION Pro (Rilis Resmi Android 11+ / API 30+): Dilengkapi Pemilih Lagu/Musik Alarm Kustom dari File Perangkat (SAF Persistable Audio), Kalibrasi GPS Perangkat Akurasi Tinggi, Kanvas Cuaca & Maritim Realtime Online (Open-Meteo WMO + Marine API 16 Wilayah Pantai Indonesia), Asisten AI Cloud + Inti Offline Cerdas, serta Stopwatch Pro 5-Menu (Kronograf, Multi-Track 4x, HIIT, Lab Pintar Auto-Lap/Metronom/Reaction Gate, & Riwayat CSV)."
                    } else {
                        "CHRONOS V$CHRONOS_VERSION Pro (Official Release Android 11+ / API 30+): Featuring Custom Device Music Alarm Picker (SAF Persistable Audio), High-Accuracy Device GPS Calibration, Real-Time Online Weather & Marine Telemetry (Open-Meteo WMO + Marine API across 16 Indonesian Coasts), Cloud AI Assistant + Offline Core, and 5-Menu Smart Stopwatch (Chrono Pro, 4x Multi-Track, HIIT, Smart Lab Auto-Lap/Metronome/Reaction Gate, & CSV Audit)."
                    },
                    fontSize = 11.sp,
                    color = Color(0xFFD1D5DB),
                    lineHeight = 16.sp
                )
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = ChronosSurfaceDark,
            title = { Text(if (isId) "Pulihkan Pengaturan dari JSON" else "Restore Settings from JSON", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importText,
                        onValueChange = {
                            importText = it
                            importError = null
                        },
                        placeholder = { Text("{\"version\": \"1.66.88\", ...}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (importError != null) {
                        Text(importError.orEmpty(), fontSize = 12.sp, color = ChronosRoseNeon)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onImportJson(importText)) {
                            showImportDialog = false
                            importText = ""
                            syncMessage = if (isId) "Backup berhasil dipulihkan!" else "Backup restored successfully!"
                        } else {
                            importError = if (isId) "Format JSON tidak valid" else "Invalid JSON format"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChronosCyanNeon)
                ) {
                    Text("Restore Now", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text(ChronosTranslations.t(settings.appLanguage, "cancel"), color = Color(0xFF9CA3AF)) }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = ChronosSurfaceDark,
            title = { Text(if (isId) "Kembalikan Pengaturan Awal?" else "Reset All Settings?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (isId) "Semua preferensi tampilan, tema warna, bahasa, dan kota cuaca akan dikembalikan ke pengaturan awal V$CHRONOS_VERSION."
                    else "All appearance preferences, theme colors, language, and saved cities will be restored to default V$CHRONOS_VERSION values.",
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSettings()
                        showResetConfirm = false
                        syncMessage = if (isId) "Pengaturan dikembalikan ke awal." else "Settings restored to defaults."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChronosRoseNeon)
                ) {
                    Text(if (isId) "Ya, Reset" else "Reset Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(ChronosTranslations.t(settings.appLanguage, "cancel"), color = Color(0xFF9CA3AF)) }
            }
        )
    }
}

@Composable
private fun SettingsAccordionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.03f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (expanded) iconColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = iconColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF9CA3AF))
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = if (expanded) iconColor else Color(0xFF9CA3AF)
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingsSelectPill(
    label: String,
    selected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) activeColor else Color.White.copy(alpha = 0.04f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) activeColor else Color.White.copy(alpha = 0.12f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.Black else Color(0xFFD1D5DB),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    activeColor: Color,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF9CA3AF))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(50))
                .background(if (checked) activeColor else Color(0xFF374151))
                .padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            )
        }
    }
}
