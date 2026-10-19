package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AgendaEvent
import com.example.model.DailyGoalProgress
import com.example.model.DayProgress
import com.example.model.HabitItem
import com.example.model.NoteItem
import com.example.model.PeriodFilter
import com.example.model.RecurrenceType
import com.example.model.ReminderItem
import com.example.model.TaskCategory
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.model.WeeklyProgressSummary
import com.example.ui.components.ChronosAmberNeon
import com.example.ui.components.ChronosBlueNeon
import com.example.ui.components.ChronosCardNavy
import com.example.ui.components.ChronosCyanNeon
import com.example.ui.components.ChronosGreenNeon
import com.example.ui.components.ChronosRoseNeon
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ProductivityFullScreen(
    selectedPeriod: PeriodFilter,
    tasks: List<TaskItem>,
    events: List<AgendaEvent>,
    habits: List<HabitItem>,
    reminders: List<ReminderItem>,
    notes: List<NoteItem>,
    focusRemainingSeconds: Int,
    focusTotalSeconds: Int,
    isFocusRunning: Boolean,
    completedFocusSessions: Int,
    totalFocusMinutes: Int,
    aiInsights: List<String>,
    isAiAnalyzing: Boolean,
    appLanguage: String,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSelectPeriod: (PeriodFilter) -> Unit,
    onAnalyzeDeep: () -> Unit,
    onAddTask: (TaskItem) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddEvent: (AgendaEvent) -> Unit,
    onToggleEvent: (String) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onAddHabit: (HabitItem) -> Unit,
    onIncrementHabit: (String) -> Unit,
    onDecrementHabit: (String) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onAddReminder: (ReminderItem) -> Unit,
    onToggleReminder: (String) -> Unit,
    onDeleteReminder: (String) -> Unit,
    onAddNote: (NoteItem) -> Unit,
    onDeleteNote: (String) -> Unit,
    onToggleFocus: () -> Unit,
    onResetFocus: () -> Unit,
    onSelectFocusDuration: (Int) -> Unit
) {
    val isId = appLanguage == "id"
    var notesQuery by remember { mutableStateOf("") }
    var selectedNoteCategory by remember { mutableStateOf<String?>(null) }

    var showAddTask by remember { mutableStateOf(false) }
    var showAddEvent by remember { mutableStateOf(false) }
    var showAddReminder by remember { mutableStateOf(false) }
    var showAddHabit by remember { mutableStateOf(false) }
    var showAddNote by remember { mutableStateOf(false) }

    val completedT = tasks.count { it.isCompleted }
    val completedE = events.count { it.isCompleted }
    val completedH = habits.count { it.isCompletedToday }
    val totalItems = tasks.size + events.size + habits.size
    val completedItems = completedT + completedE + completedH
    val productivityPct = if (totalItems > 0) ((completedItems.toFloat() / totalItems.toFloat()) * 100f).roundToInt() else 0

    val exerciseHabit = habits.find { it.category.contains("fitness", true) || it.name.contains("olahraga", true) || it.name.contains("workout", true) }
    val readingHabit = habits.find { it.category.contains("mind", true) || it.name.contains("baca", true) || it.name.contains("read", true) }
    val exerciseMinutes = if (exerciseHabit != null) min(exerciseHabit.targetCount * 10, exerciseHabit.currentCount * 10) else 0
    val exerciseTargetMinutes = if (exerciseHabit != null) max(10, exerciseHabit.targetCount * 10) else 30
    val readingMinutes = if (readingHabit != null) min(readingHabit.targetCount * 5, readingHabit.currentCount * 5) else 0
    val readingTargetMinutes = if (readingHabit != null) max(15, readingHabit.targetCount * 5) else 30

    val goalProgress = DailyGoalProgress(
        completedTasks = completedT,
        totalTasks = tasks.size,
        completedEvents = completedE,
        totalEvents = events.size,
        exerciseMinutes = exerciseMinutes,
        exerciseTargetMinutes = exerciseTargetMinutes,
        readingMinutes = readingMinutes,
        readingTargetMinutes = readingTargetMinutes,
        productivityPercentage = productivityPct
    )

    val habitConsistency = if (habits.isNotEmpty()) ((completedH.toFloat() / habits.size.toFloat()) * 100f).roundToInt() else 0
    val remainingTasks = max(0, tasks.size - completedT)

    val daysLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val currentDayIndex = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7
    val dailyChart = daysLabels.mapIndexed { idx, lbl ->
        if (idx == currentDayIndex) {
            DayProgress(lbl, productivityPct, completedT, totalFocusMinutes)
        } else {
            DayProgress(lbl, 0, 0, 0)
        }
    }
    val weeklySummary = WeeklyProgressSummary(
        completedTasksCount = completedT,
        remainingTasksCount = remainingTasks,
        focusSessionsCount = completedFocusSessions,
        habitConsistencyPercentage = habitConsistency,
        productiveDaysCount = dailyChart.count { it.percentage >= 40 },
        overallProductivityPercentage = productivityPct,
        dailyChart = dailyChart
    )

    val filteredTasks = tasks.filter { it.targetPeriod == selectedPeriod || selectedPeriod == PeriodFilter.THIS_WEEK }
    val filteredEvents = events.filter { it.targetPeriod == selectedPeriod || selectedPeriod == PeriodFilter.THIS_WEEK }
    val filteredNotes = notes.filter { n ->
        val mQuery = notesQuery.isBlank() || n.title.contains(notesQuery, true) || n.content.contains(notesQuery, true)
        val mCat = selectedNoteCategory == null || n.category.equals(selectedNoteCategory, true)
        mQuery && mCat
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("productivity_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ChronosCyanNeon)
                    }
                    Column {
                        Text(
                            text = "PRODUCTIVITY",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 3.sp,
                            color = ChronosCyanNeon
                        )
                        Text(
                            text = "Plan your day. Get things done.",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
                Row {
                    IconButton(onClick = onAnalyzeDeep) {
                        Icon(Icons.Default.Refresh, contentDescription = "Analyze", tint = ChronosCyanNeon)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF9CA3AF))
                    }
                }
            }
        }

        // 1. PeriodSelectorBar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    PeriodFilter.TODAY to (if (isId) "Hari Ini" else "Today"),
                    PeriodFilter.TOMORROW to (if (isId) "Besok" else "Tomorrow"),
                    PeriodFilter.DAY_AFTER to (if (isId) "Lusa" else "Day After"),
                    PeriodFilter.THIS_WEEK to (if (isId) "Minggu Ini" else "This Week"),
                    PeriodFilter.NEXT_WEEK to (if (isId) "Minggu Depan" else "Next Week")
                ).forEach { (period, label) ->
                    val selected = period == selectedPeriod
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) ChronosCyanNeon else ChronosCardNavy,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectPeriod(period) }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (selected) Color.Black else Color(0xFF9CA3AF),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 2. Today's Summary Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isId) "Ringkasan Hari Ini" else "Today's Summary",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isId) "Sinkronisasi tinggi dengan inti CHRONOS" else "High synchronization with CHRONOS core",
                                fontSize = 11.sp,
                                color = ChronosCyanNeon.copy(alpha = 0.8f)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ChronosCyanNeon.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "$productivityPct% ${if (isId) "Target Harian" else "Daily Goal"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF67E8F9),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { productivityPct / 100f },
                        color = ChronosCyanNeon,
                        trackColor = Color(0xFF1E283D),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryStatBox(
                            value = "$completedT/${tasks.size}",
                            label = if (isId) "Tugas" else "Tasks",
                            color = ChronosCyanNeon,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatBox(
                            value = "${events.size}",
                            label = if (isId) "Agenda" else "Events",
                            color = ChronosBlueNeon,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatBox(
                            value = "${reminders.count { it.isEnabled }}",
                            label = if (isId) "Pengingat" else "Reminders",
                            color = ChronosAmberNeon,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStatBox(
                            value = "$completedH/${habits.size}",
                            label = if (isId) "Kebiasaan" else "Habits",
                            color = ChronosGreenNeon,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3. Quick Actions Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "QUICK ACTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9CA3AF)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionPill("+ Task", ChronosCyanNeon, Modifier.weight(1f)) { showAddTask = true }
                    QuickActionPill("+ Event", ChronosBlueNeon, Modifier.weight(1f)) { showAddEvent = true }
                    QuickActionPill("+ Reminder", ChronosAmberNeon, Modifier.weight(1f)) { showAddReminder = true }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickActionPill("+ Note", Color(0xFFA855F7), Modifier.weight(1f)) { showAddNote = true }
                    QuickActionPill("+ Habit", ChronosGreenNeon, Modifier.weight(1f)) { showAddHabit = true }
                    QuickActionPill("Focus", ChronosRoseNeon, Modifier.weight(1f)) { onToggleFocus() }
                }
            }
        }

        // 4. Smart Daily Life Routine Presets (1-Tap Apply)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChronosGreenNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isId) "PRESET RUTINITAS KEHIDUPAN CERDAS" else "SMART DAILY LIFE ROUTINE PRESETS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "1-TAP APPLY",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = ChronosGreenNeon
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoutineTemplateCard(
                            title = if (isId) "🌅 Pagi Segar" else "🌅 Morning Rise",
                            desc = if (isId) "+Tugas Fokus & Hidrasi" else "+Focus Task & Hydration",
                            accent = ChronosGreenNeon,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val now = System.currentTimeMillis()
                                onAddTask(
                                    TaskItem(
                                        id = "tpl_t_$now",
                                        title = if (isId) "Prioritas Utama Pagi (Deep Work)" else "Morning Top Priority Sprint",
                                        description = if (isId) "Selesaikan fokus utama sebelum siang" else "Complete primary deliverable before noon",
                                        priority = TaskPriority.HIGH,
                                        category = TaskCategory.WORK,
                                        deadline = "11:00",
                                        targetPeriod = selectedPeriod
                                    )
                                )
                                onAddHabit(
                                    HabitItem(
                                        id = "tpl_h_$now",
                                        name = if (isId) "Hidrasi Pagi & Elektrolit" else "Morning Hydration (8 Glasses)",
                                        category = "Health",
                                        currentCount = 1,
                                        targetCount = 8,
                                        unit = "glasses"
                                    )
                                )
                            }
                        )
                        RoutineTemplateCard(
                            title = if (isId) "⚡ Fokus Penuh" else "⚡ Deep Sprint",
                            desc = if (isId) "+Agenda & Siapkan 25m" else "+Agenda & 25m Timer",
                            accent = ChronosCyanNeon,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onAddEvent(
                                    AgendaEvent(
                                        id = "tpl_e_${System.currentTimeMillis()}",
                                        title = if (isId) "Blok Kerja Fokus Tanpa Gangguan" else "Deep Focus Sprint Block",
                                        startTime = "14:00",
                                        endTime = "15:30",
                                        location = "Workspace",
                                        description = "Pomodoro + Alpha 40Hz",
                                        targetPeriod = selectedPeriod
                                    )
                                )
                                onSelectFocusDuration(25)
                            }
                        )
                        RoutineTemplateCard(
                            title = if (isId) "🌿 Hidup Sehat" else "🌿 Vitality Care",
                            desc = if (isId) "+Olahraga & Pengingat" else "+Workout & Sleep Prep",
                            accent = ChronosAmberNeon,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val now = System.currentTimeMillis()
                                onAddHabit(
                                    HabitItem(
                                        id = "tpl_fit_$now",
                                        name = if (isId) "Olahraga & Peregangan Sore" else "Active Workout & Stretch",
                                        category = "Fitness",
                                        currentCount = 1,
                                        targetCount = 3,
                                        unit = "sets"
                                    )
                                )
                                onAddReminder(
                                    ReminderItem(
                                        id = "tpl_rem_$now",
                                        title = if (isId) "Detoks Layar & Persiapan Tidur REM" else "Screen Detox & REM Sleep Prep",
                                        timeStr = "21:30",
                                        recurrence = RecurrenceType.DAILY
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // 5. AI Productivity Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ChronosCyanNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI PRODUCTIVITY",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChronosCyanNeon
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ChronosCyanNeon.copy(alpha = 0.2f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAnalyzeDeep() }
                        ) {
                            Text(
                                text = if (isAiAnalyzing) (if (isId) "Menganalisis..." else "Analyzing...")
                                else (if (isId) "Analisis AI" else "Analyze"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF67E8F9),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    aiInsights.forEach { ins ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", fontWeight = FontWeight.Bold, color = ChronosCyanNeon, fontSize = 12.sp)
                            Text(text = ins, fontSize = 12.sp, color = Color(0xFFE5E7EB), lineHeight = 17.sp)
                        }
                    }
                }
            }
        }

        // 6. Agenda Timeline Section
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosBlueNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ChronosBlueNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "AGENDA HARI INI" else "TODAY'S AGENDA",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { showAddEvent = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = ChronosBlueNeon)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (filteredEvents.isEmpty()) {
                        Text(
                            text = if (isId) "Tidak ada agenda kegiatan." else "No agenda events scheduled for this period.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredEvents.forEach { event ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.02f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF1E283D)
                                            ) {
                                                Text(
                                                    text = event.startTime,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = ChronosCyanNeon,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { onToggleEvent(event.id) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (event.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                    contentDescription = null,
                                                    tint = if (event.isCompleted) ChronosCyanNeon else Color(0xFF9CA3AF)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = event.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (event.isCompleted) Color(0xFF6B7280) else Color.White,
                                                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                                if (event.location.isNotBlank()) {
                                                    Text(text = "📍 ${event.location}", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                                                }
                                            }
                                        }
                                        IconButton(onClick = { onDeleteEvent(event.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Tasks Section
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckBox, contentDescription = null, tint = ChronosCyanNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "DAFTAR TUGAS" else "TODAY'S TASKS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { showAddTask = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = ChronosCyanNeon)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (filteredTasks.isEmpty()) {
                        Text(
                            text = if (isId) "Belum ada tugas dibuat. Ketuk '+' untuk menambah." else "No tasks created yet.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredTasks.forEach { task ->
                                val priColor = when (task.priority) {
                                    TaskPriority.HIGH -> ChronosRoseNeon
                                    TaskPriority.MEDIUM -> ChronosAmberNeon
                                    TaskPriority.LOW -> ChronosCyanNeon
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.02f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            IconButton(
                                                onClick = { onToggleTask(task.id) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                    contentDescription = null,
                                                    tint = if (task.isCompleted) ChronosCyanNeon else Color(0xFF9CA3AF)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = task.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (task.isCompleted) Color(0xFF6B7280) else Color.White,
                                                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = priColor.copy(alpha = 0.15f),
                                                        border = androidx.compose.foundation.BorderStroke(1.dp, priColor.copy(alpha = 0.3f))
                                                    ) {
                                                        Text(
                                                            text = task.priority.name,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = priColor,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                    Text(text = task.category.name, fontSize = 10.sp, color = Color(0xFF9CA3AF))
                                                    if (task.deadline.isNotBlank()) {
                                                        Text(text = "• Due ${task.deadline}", fontSize = 10.sp, color = Color(0xFF6B7280))
                                                    }
                                                }
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (!task.isCompleted) {
                                                IconButton(
                                                    onClick = {
                                                        onSelectFocusDuration(25)
                                                        if (!isFocusRunning) onToggleFocus()
                                                    },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Timer, contentDescription = null, tint = ChronosRoseNeon, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            IconButton(onClick = { onDeleteTask(task.id) }, modifier = Modifier.size(26.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
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

        // 8. Habits Section
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosGreenNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = ChronosGreenNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "KEBIASAAN" else "HABITS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { showAddHabit = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = ChronosGreenNeon)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (habits.isEmpty()) {
                        Text(
                            text = if (isId) "Belum ada kebiasaan dibuat. Ketuk '+' untuk menambah target baru."
                            else "No habits created yet. Tap '+' to add a new habit.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            habits.forEach { habit ->
                                val progress = (habit.currentCount.toFloat() / max(1, habit.targetCount).toFloat()).coerceIn(0f, 1f)
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.02f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = habit.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (habit.isCompletedToday) ChronosGreenNeon else Color.White
                                                )
                                                if (habit.streakDays > 0) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = ChronosAmberNeon.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "🔥 ${habit.streakDays}d streak",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ChronosAmberNeon,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "${habit.currentCount}/${habit.targetCount}",
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFD1D5DB)
                                                )
                                                IconButton(onClick = { onDecrementHabit(habit.id) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Remove, contentDescription = null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(14.dp))
                                                }
                                                IconButton(onClick = { onIncrementHabit(habit.id) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Add, contentDescription = null, tint = ChronosGreenNeon, modifier = Modifier.size(14.dp))
                                                }
                                                IconButton(onClick = { onDeleteHabit(habit.id) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            color = ChronosGreenNeon,
                                            trackColor = Color(0xFF1E283D),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(50))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 9. Reminders Section
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosAmberNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = ChronosAmberNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "PENGINGAT" else "REMINDERS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { showAddReminder = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = ChronosAmberNeon)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (reminders.isEmpty()) {
                        Text(
                            text = if (isId) "Belum ada pengingat terjadwal. Ketuk '+' untuk menambah."
                            else "No reminders scheduled yet. Tap '+' to add one.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            reminders.forEach { r ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.02f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = r.timeStr,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (r.isEnabled) ChronosAmberNeon else Color(0xFF6B7280)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = r.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (r.isEnabled) Color.White else Color(0xFF6B7280)
                                                )
                                                Text(
                                                    text = r.recurrence.name,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF6B7280)
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .width(36.dp)
                                                    .height(20.dp)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (r.isEnabled) ChronosAmberNeon else Color(0xFF374151))
                                                    .clickable { onToggleReminder(r.id) }
                                                    .padding(2.dp),
                                                contentAlignment = if (r.isEnabled) Alignment.CenterEnd else Alignment.CenterStart
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black)
                                                )
                                            }
                                            IconButton(onClick = { onDeleteReminder(r.id) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
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

        // 10. Focus Mode Section
        item {
            val mins = focusRemainingSeconds / 60
            val secs = focusRemainingSeconds % 60
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosRoseNeon.copy(alpha = 0.35f)),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = ChronosRoseNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "SESI FOKUS (POMODORO)" else "FOCUS SESSION",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(25, 50).forEach { m ->
                                val selected = focusTotalSeconds == m * 60
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) ChronosRoseNeon else Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onSelectFocusDuration(m) }
                                ) {
                                    Text(
                                        text = "${m}m",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else Color(0xFF9CA3AF),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "%02d:%02d".format(mins, secs),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Light,
                        fontSize = 44.sp,
                        color = ChronosRoseNeon
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onToggleFocus,
                            colors = ButtonDefaults.buttonColors(containerColor = ChronosRoseNeon),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = if (isFocusRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFocusRunning) "Pause" else if (isId) "Mulai Fokus" else "Start Focus",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Button(
                            onClick = onResetFocus,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Reset", fontSize = 12.sp, color = Color(0xFFD1D5DB))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$completedFocusSessions",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = ChronosRoseNeon
                            )
                            Text(text = if (isId) "Sesi Selesai" else "Sessions Done", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${totalFocusMinutes}m",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = ChronosAmberNeon
                            )
                            Text(text = if (isId) "Total Waktu" else "Total Time", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "100m",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = ChronosCyanNeon
                            )
                            Text(text = if (isId) "Target Harian" else "Daily Target", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                        }
                    }
                }
            }
        }

        // 11. Notes Section
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFC084FC), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isId) "CATATAN" else "NOTES",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = { showAddNote = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFFC084FC))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesQuery,
                        onValueChange = { notesQuery = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        placeholder = { Text(if (isId) "Cari catatan..." else "Search notes...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("All", "Personal", "Work", "Ideas", "Shopping", "Projects").forEach { cat ->
                            val isAll = cat == "All"
                            val active = if (isAll) selectedNoteCategory == null else selectedNoteCategory == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (active) Color(0xFFA855F7) else Color.White.copy(alpha = 0.05f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedNoteCategory = if (isAll) null else cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) Color.White else Color(0xFF9CA3AF),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    if (filteredNotes.isEmpty()) {
                        Text(
                            text = if (isId) "Tidak ada catatan ditemukan." else "No notes found.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredNotes.forEach { note ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.02f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = note.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFA855F7).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = note.category,
                                                        fontSize = 9.sp,
                                                        color = Color(0xFFD8B4FE),
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = note.content, fontSize = 11.sp, color = Color(0xFFD1D5DB))
                                        }
                                        IconButton(onClick = { onDeleteNote(note.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF6B7280), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 12. Daily Goals Section
        item {
            val rows = listOf(
                Triple(if (isId) "Tugas Selesai" else "Tasks Done", goalProgress.completedTasks to goalProgress.totalTasks, ""),
                Triple(if (isId) "Agenda Terlaksana" else "Events Done", goalProgress.completedEvents to goalProgress.totalEvents, ""),
                Triple(if (isId) "Olahraga" else "Exercise", goalProgress.exerciseMinutes to goalProgress.exerciseTargetMinutes, " min"),
                Triple(if (isId) "Membaca" else "Reading", goalProgress.readingMinutes to goalProgress.readingTargetMinutes, " min")
            )
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosCyanNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isId) "TARGET HARIAN" else "DAILY GOALS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    rows.forEach { (label, pair, unit) ->
                        val (cur, target) = pair
                        val pct = min(100, ((cur.toFloat() / max(1, target).toFloat()) * 100f).roundToInt())
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = label, fontSize = 12.sp, color = Color(0xFFD1D5DB))
                                Text(
                                    text = "$cur/$target$unit ($pct%)",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = ChronosCyanNeon
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                color = ChronosCyanNeon,
                                trackColor = Color(0xFF1E283D),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }
        }

        // 13. Weekly Progress Section
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ChronosCardNavy.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ChronosBlueNeon.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isId) "PROGRES MINGGUAN" else "WEEKLY PROGRESS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklySummary.dailyChart.forEach { d ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${d.percentage}%",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = Color(0xFF6B7280)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(max(4f, d.percentage * 0.55f).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(ChronosCyanNeon, ChronosBlueNeon)
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = d.dayLabel, fontSize = 10.sp, color = Color(0xFF9CA3AF))
                            }
                        }
                    }
                }
            }
        }
    }

    // Creation Modals
    if (showAddTask) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var deadline by remember { mutableStateOf("18:00") }
        AlertDialog(
            onDismissRequest = { showAddTask = false },
            containerColor = ChronosCardNavy,
            title = { Text(if (isId) "Tambah Tugas Baru" else "Add New Task", color = ChronosCyanNeon, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (isId) "Judul Tugas *" else "Task Title *") }, singleLine = true)
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(if (isId) "Deskripsi" else "Description") }, singleLine = true)
                    OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text(if (isId) "Batas Waktu" else "Due Time") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddTask(
                                TaskItem(
                                    id = System.currentTimeMillis().toString(),
                                    title = title.trim(),
                                    description = desc.trim(),
                                    deadline = deadline.trim(),
                                    targetPeriod = selectedPeriod
                                )
                            )
                            showAddTask = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChronosCyanNeon)
                ) {
                    Text(if (isId) "Simpan Tugas" else "Create Task", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTask = false }) { Text(if (isId) "Batal" else "Cancel", color = Color(0xFF9CA3AF)) }
            }
        )
    }

    if (showAddEvent) {
        var title by remember { mutableStateOf("") }
        var start by remember { mutableStateOf("09:00") }
        var end by remember { mutableStateOf("10:00") }
        var loc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddEvent = false },
            containerColor = ChronosCardNavy,
            title = { Text(if (isId) "Tambah Agenda Kegiatan" else "Add Agenda Event", color = ChronosBlueNeon, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (isId) "Judul Agenda *" else "Event Title *") }, singleLine = true)
                    OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text(if (isId) "Waktu Mulai" else "Start Time") }, singleLine = true)
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text(if (isId) "Lokasi" else "Location") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddEvent(
                                AgendaEvent(
                                    id = System.currentTimeMillis().toString(),
                                    title = title.trim(),
                                    startTime = start.trim(),
                                    endTime = end.trim(),
                                    location = loc.trim(),
                                    targetPeriod = selectedPeriod
                                )
                            )
                            showAddEvent = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChronosBlueNeon)
                ) {
                    Text(if (isId) "Simpan Agenda" else "Add Event", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEvent = false }) { Text(if (isId) "Batal" else "Cancel", color = Color(0xFF9CA3AF)) }
            }
        )
    }

    if (showAddReminder) {
        var title by remember { mutableStateOf("") }
        var timeStr by remember { mutableStateOf("09:00") }
        AlertDialog(
            onDismissRequest = { showAddReminder = false },
            containerColor = ChronosCardNavy,
            title = { Text(if (isId) "Tambah Pengingat" else "Create Reminder", color = ChronosAmberNeon, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (isId) "Label Pengingat *" else "Reminder Label *") }, singleLine = true)
                    OutlinedTextField(value = timeStr, onValueChange = { timeStr = it }, label = { Text("Time (HH:mm)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddReminder(
                                ReminderItem(
                                    id = System.currentTimeMillis().toString(),
                                    title = title.trim(),
                                    timeStr = timeStr.trim()
                                )
                            )
                            showAddReminder = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChronosAmberNeon)
                ) {
                    Text(if (isId) "Setel Pengingat" else "Set Reminder", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddReminder = false }) { Text(if (isId) "Batal" else "Cancel", color = Color(0xFF9CA3AF)) }
            }
        )
    }

    if (showAddHabit) {
        var name by remember { mutableStateOf("") }
        var target by remember { mutableIntStateOf(8) }
        var unit by remember { mutableStateOf("glasses") }
        AlertDialog(
            onDismissRequest = { showAddHabit = false },
            containerColor = ChronosCardNavy,
            title = { Text(if (isId) "Bangun Kebiasaan Baru" else "Build New Habit", color = ChronosGreenNeon, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(if (isId) "Nama Kebiasaan *" else "Habit Name *") }, singleLine = true)
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text(if (isId) "Satuan" else "Unit") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddHabit(
                                HabitItem(
                                    id = System.currentTimeMillis().toString(),
                                    name = name.trim(),
                                    targetCount = target,
                                    unit = unit.trim()
                                )
                            )
                            showAddHabit = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChronosGreenNeon)
                ) {
                    Text(if (isId) "Mulai Kebiasaan" else "Start Habit", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabit = false }) { Text(if (isId) "Batal" else "Cancel", color = Color(0xFF9CA3AF)) }
            }
        )
    }

    if (showAddNote) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNote = false },
            containerColor = ChronosCardNavy,
            title = { Text(if (isId) "Catatan Cepat" else "Quick Note", color = Color(0xFFC084FC), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (isId) "Judul Catatan" else "Note Title") }, singleLine = true)
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text(if (isId) "Isi Catatan" else "Note Content") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() || content.isNotBlank()) {
                            onAddNote(
                                NoteItem(
                                    id = System.currentTimeMillis().toString(),
                                    title = title.trim().ifBlank { if (isId) "Catatan Baru" else "New Note" },
                                    content = content.trim()
                                )
                            )
                            showAddNote = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7))
                ) {
                    Text(if (isId) "Simpan Catatan" else "Save Note", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNote = false }) { Text(if (isId) "Batal" else "Cancel", color = Color(0xFF9CA3AF)) }
            }
        )
    }
}

@Composable
private fun SummaryStatBox(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.02f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = color
            )
            Text(text = label, fontSize = 10.sp, color = Color(0xFF9CA3AF))
        }
    }
}

@Composable
private fun QuickActionPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun RoutineTemplateCard(
    title: String,
    desc: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 9.sp,
                color = Color(0xFF9CA3AF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
