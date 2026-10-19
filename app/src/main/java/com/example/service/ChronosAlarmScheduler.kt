package com.example.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.AlarmItem
import com.example.model.WakeMission
import com.example.model.formatAlarmRemaining
import com.example.model.formatAlarmTargetDateTime
import com.example.model.getNextAlarmOccurrenceMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

object ChronosAlarmScheduler {

    const val ACTION_TRIGGER_ALARM = "com.example.chronos.ACTION_TRIGGER_ALARM"
    const val ACTION_PRE_WAKE_ALARM = "com.example.chronos.ACTION_PRE_WAKE_ALARM"
    const val ACTION_SNOOZE_ALARM = "com.example.chronos.ACTION_SNOOZE_ALARM"
    const val ACTION_DISMISS_ALARM = "com.example.chronos.ACTION_DISMISS_ALARM"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
    const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
    const val EXTRA_ALARM_LABEL = "extra_alarm_label"
    const val EXTRA_ALARM_SOUND = "extra_alarm_sound"
    const val EXTRA_ALARM_CUSTOM_TITLE = "extra_alarm_custom_title"
    const val EXTRA_ALARM_VIBRATE = "extra_alarm_vibrate"
    const val EXTRA_ALARM_FADE = "extra_alarm_fade"
    const val EXTRA_ALARM_MISSION = "extra_alarm_mission"
    const val EXTRA_ALARM_THEME = "extra_alarm_theme"
    const val EXTRA_OPEN_ALARMS = "extra_open_alarms"
    const val EXTRA_TARGET_SCREEN = "extra_target_screen"

    private const val CHANNEL_RING_ID = "chronos_alarm_ring_channel_v2"
    private const val CHANNEL_STANDBY_ID = "chronos_alarm_standby_channel_v2"
    private const val CHANNEL_ALERT_ID = "chronos_smart_alert_channel_v2"

    private const val NOTIF_ID_STANDBY = 7101
    private const val NOTIF_ID_RINGING = 7102
    private const val NOTIF_ID_PRE_WAKE = 7103
    private const val NOTIF_ID_TIMER_REMINDER = 7104
    private const val NOTIF_ID_TIMER_LIVE = 7105
    private const val NOTIF_ID_FOCUS_LIVE = 7106

    private val _backgroundRingingAlarm = MutableStateFlow<AlarmItem?>(null)
    val backgroundRingingAlarm: StateFlow<AlarmItem?> = _backgroundRingingAlarm.asStateFlow()

    private val _backgroundActionEvent = MutableStateFlow<Pair<String, Long>?>(null)
    val backgroundActionEvent: StateFlow<Pair<String, Long>?> = _backgroundActionEvent.asStateFlow()

    private val _nextScheduledTargetMs = MutableStateFlow<Long?>(null)
    val nextScheduledTargetMs: StateFlow<Long?> = _nextScheduledTargetMs.asStateFlow()

    private val _scheduledCount = MutableStateFlow(0)
    val scheduledCount: StateFlow<Int> = _scheduledCount.asStateFlow()

    private val skippedOccurrenceTargetMap = java.util.concurrent.ConcurrentHashMap<Long, Long>()

    @Volatile
    var lastReceiverAudioTriggerMs: Long = 0L
        private set

    private var activeWakeLock: PowerManager.WakeLock? = null

    fun markUpcomingOccurrenceSkipped(alarmId: Long, occurrenceMillis: Long) {
        skippedOccurrenceTargetMap[alarmId] = occurrenceMillis
    }

    fun clearSkippedOccurrence(alarmId: Long) {
        skippedOccurrenceTargetMap.remove(alarmId)
    }

    fun consumeBackgroundActionEvent() {
        _backgroundActionEvent.value = null
    }

    fun clearBackgroundRingingState(context: Context?) {
        _backgroundRingingAlarm.value = null
        releaseWakeLock()
        if (context != null) {
            try {
                NotificationManagerCompat.from(context).cancel(NOTIF_ID_RINGING)
            } catch (_: Throwable) {
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannels(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val ringChannel = NotificationChannel(
                CHANNEL_RING_ID,
                "CHRONOS Alarm Dering Aktif (Prioritas Utama)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi dering alarm penuh waktu nyata di latar belakang & layar kunci"
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val standbyChannel = NotificationChannel(
                CHANNEL_STANDBY_ID,
                "CHRONOS Status Alarm Latar Belakang Realtime",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Indikator hitung mundur realtime untuk alarm yang dijadwalkan di latar belakang"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "CHRONOS Pra-Bangun, Timer & Pengingat Pintar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pra-bangun lembut, timer selesai, dan pengingat agenda"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            nm.createNotificationChannel(ringChannel)
            nm.createNotificationChannel(standbyChannel)
            nm.createNotificationChannel(alertChannel)
        } catch (_: Throwable) {
        }
    }

    /**
     * Synchronizes all alarms with Android's system AlarmManager (zero-delay setAlarmClock)
     * and updates the live real-time countdown notification in the notification shade.
     */
    fun syncAlarmsWithSystem(
        context: Context,
        alarms: List<AlarmItem>,
        notificationsEnabled: Boolean = true,
        isId: Boolean = true
    ) {
        try {
            ensureNotificationChannels(context)
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val nowMs = System.currentTimeMillis()

            // Cancel old pending intents for IDs and schedule active ones
            var earliestAlarm: AlarmItem? = null
            var earliestTriggerMs: Long? = null
            var activeCount = 0

            for (alarm in alarms) {
                cancelSystemAlarmIntent(context, am, alarm.id)
                if (!alarm.isEnabled) {
                    skippedOccurrenceTargetMap.remove(alarm.id)
                    continue
                }

                var nextMs = getNextAlarmOccurrenceMillis(alarm, nowMs) ?: continue
                val skippedMs = skippedOccurrenceTargetMap[alarm.id]
                if (skippedMs != null) {
                    if (nowMs >= skippedMs + 60_000L) {
                        skippedOccurrenceTargetMap.remove(alarm.id)
                    } else if (kotlin.math.abs(nextMs - skippedMs) < 60_000L) {
                        // Advance past the skipped occurrence to the next scheduled cycle
                        nextMs = getNextAlarmOccurrenceMillis(alarm, skippedMs + 60_000L) ?: continue
                    }
                }

                activeCount++
                scheduleSingleExactAlarm(context, am, alarm, nextMs)

                // Schedule gentle pre-wake notification 10 minutes prior (or 5 mins if 6..11 mins away)
                if (alarm.preWakeGentle) {
                    val diffMs = nextMs - nowMs
                    val preWakeLeadMs = if (diffMs > 11 * 60_000L) 10 * 60_000L else if (diffMs > 6 * 60_000L) 5 * 60_000L else 0L
                    if (preWakeLeadMs > 0L) {
                        schedulePreWakeGentleIntent(context, am, alarm, nextMs - preWakeLeadMs)
                    }
                }

                if (earliestTriggerMs == null || nextMs < earliestTriggerMs) {
                    earliestTriggerMs = nextMs
                    earliestAlarm = alarm
                }
            }

            _scheduledCount.value = activeCount
            _nextScheduledTargetMs.value = earliestTriggerMs

            if (notificationsEnabled && earliestAlarm != null && earliestTriggerMs != null) {
                postOrUpdateStandbyCountdownNotification(
                    context = context,
                    nextAlarm = earliestAlarm,
                    targetMillis = earliestTriggerMs,
                    totalActiveCount = activeCount,
                    isId = isId
                )
            } else {
                try {
                    NotificationManagerCompat.from(context).cancel(NOTIF_ID_STANDBY)
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun buildTriggerIntent(context: Context, alarm: AlarmItem, action: String): Intent {
        return Intent(context, ChronosAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_HOUR, alarm.hour)
            putExtra(EXTRA_ALARM_MINUTE, alarm.minute)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_ALARM_SOUND, alarm.soundUri)
            putExtra(EXTRA_ALARM_CUSTOM_TITLE, alarm.customAudioTitle)
            putExtra(EXTRA_ALARM_VIBRATE, alarm.isVibrate)
            putExtra(EXTRA_ALARM_FADE, alarm.fadeInSeconds)
            putExtra(EXTRA_ALARM_MISSION, alarm.wakeMission.name)
            putExtra(EXTRA_ALARM_THEME, alarm.backgroundTheme)
        }
    }

    private fun requestCodeFor(alarmId: Long, offset: Int = 0): Int {
        return ((alarmId xor (alarmId ushr 32)).toInt() and 0x0FFFFFFF) + offset
    }

    private fun cancelSystemAlarmIntent(context: Context, am: AlarmManager?, alarmId: Long) {
        if (am == null) return
        try {
            val triggerIntent = Intent(context, ChronosAlarmReceiver::class.java).apply {
                action = ACTION_TRIGGER_ALARM
            }
            val pi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(alarmId, 0),
                triggerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)

            val preIntent = Intent(context, ChronosAlarmReceiver::class.java).apply {
                action = ACTION_PRE_WAKE_ALARM
            }
            val prePi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(alarmId, 1000),
                preIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(prePi)
        } catch (_: Throwable) {
        }
    }

    private fun scheduleSingleExactAlarm(
        context: Context,
        am: AlarmManager?,
        alarm: AlarmItem,
        triggerAtMillis: Long
    ) {
        if (am == null) return
        val intent = buildTriggerIntent(context, alarm, ACTION_TRIGGER_ALARM)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeFor(alarm.id, 0),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCodeFor(alarm.id, 2000),
            showActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Primary zero-delay alarm clock scheduling (exempt from Doze restrictions)
            val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent)
            am.setAlarmClock(clockInfo, pendingIntent)
        } catch (_: SecurityException) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (_: Throwable) {
                try {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun schedulePreWakeGentleIntent(
        context: Context,
        am: AlarmManager?,
        alarm: AlarmItem,
        preWakeMillis: Long
    ) {
        if (am == null) return
        try {
            val intent = buildTriggerIntent(context, alarm, ACTION_PRE_WAKE_ALARM)
            val pi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(alarm.id, 1000),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preWakeMillis, pi)
        } catch (_: Throwable) {
        }
    }

    /**
     * Posts a live, real-time ticking countdown notification in the Android notification shade
     * using native Chronometer countdown (`setUsesChronometer(true)` + `setChronometerCountDown(true)`).
     */
    fun postOrUpdateStandbyCountdownNotification(
        context: Context,
        nextAlarm: AlarmItem,
        targetMillis: Long,
        totalActiveCount: Int,
        isId: Boolean = true
    ) {
        if (!hasNotificationPermission(context)) return
        try {
            ensureNotificationChannels(context)
            val nowMs = System.currentTimeMillis()
            val parts = nextAlarm.label.split("|||")
            val cleanTitle = parts.firstOrNull().orEmpty().ifBlank { if (isId) "Alarm Pintar" else "Smart Alarm" }
            val quote = parts.getOrNull(1).orEmpty()
            val soundName = if (ChronosAudioSynthesizer.isCustomDeviceUri(nextAlarm.soundUri)) {
                "🎵 ${nextAlarm.customAudioTitle.ifBlank { if (isId) "Musik Kustom" else "Custom Music" }}"
            } else {
                nextAlarm.soundUri
            }
            val targetStr = formatAlarmTargetDateTime(nextAlarm, nowMs, isId)
            val remStr = formatAlarmRemaining(nextAlarm, nowMs)
            val diffMins = ((targetMillis - nowMs) / 60_000.0).coerceAtLeast(1.0)
            val remCycles = String.format(Locale.US, "%.1f", diffMins / 90.0)
            val themeSpec = com.example.ui.components.AlarmBackgroundThemes.getTheme(nextAlarm.backgroundTheme)
            val themeName = if (isId) themeSpec.nameId else themeSpec.nameEn

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_ALARMS, true)
                putExtra(EXTRA_ALARM_ID, nextAlarm.id)
                putExtra(EXTRA_ALARM_THEME, nextAlarm.backgroundTheme)
            }
            val openPi = PendingIntent.getActivity(
                context,
                9100,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dismissNextIntent = Intent(context, ChronosAlarmReceiver::class.java).apply {
                action = ACTION_DISMISS_ALARM
                putExtra(EXTRA_ALARM_ID, nextAlarm.id)
            }
            val dismissNextPi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(nextAlarm.id, 3000),
                dismissNextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val titleText = if (isId) {
                "⏰ %02d:%02d %s • %s (%s)".format(nextAlarm.hour, nextAlarm.minute, themeSpec.emoji, cleanTitle, remStr)
            } else {
                "⏰ %02d:%02d %s • %s (in %s)".format(nextAlarm.hour, nextAlarm.minute, themeSpec.emoji, cleanTitle, remStr)
            }

            val subLine = if (isId) {
                "Target: $targetStr • Tema: $themeName • Nada: $soundName • ~$remCycles Siklus REM"
            } else {
                "Target: $targetStr • Theme: $themeName • Sound: $soundName • ~$remCycles REM Cycles"
            }

            val bigBody = buildString {
                append(subLine)
                if (quote.isNotBlank()) {
                    append("\n\"")
                    append(quote)
                    append("\"")
                }
                append(
                    if (isId) "\n• Visual Realistis: ${themeSpec.descriptionId}"
                    else "\n• Realistic Visual: ${themeSpec.descriptionEn}"
                )
                if (totalActiveCount > 1) {
                    append(
                        if (isId) "\n• Total $totalActiveCount alarm aktif terjaga di latar belakang tanpa delay."
                        else "\n• $totalActiveCount active alarms armed in background with zero delay."
                    )
                }
            }

            val notifColor = when (nextAlarm.backgroundTheme) {
                "COSMIC_DAWN" -> 0xFFFF9100.toInt()
                "RAINY_HORIZON" -> 0xFF00E5FF.toInt()
                "CIRCADIAN_STARS" -> 0xFFD500F9.toInt()
                "FOREST_MIST" -> 0xFF00E676.toInt()
                else -> 0xFF00E5FF.toInt()
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_STANDBY_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(titleText)
                .setContentText(subLine)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigBody))
                .setSubText(if (isId) "${themeSpec.emoji} $themeName" else "${themeSpec.emoji} $themeName")
                .setWhen(targetMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(notifColor)
                .setContentIntent(openPi)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    if (isId) "Matikan Berikutnya" else "Dismiss Next",
                    dismissNextPi
                )

            NotificationManagerCompat.from(context).notify(NOTIF_ID_STANDBY, builder.build())
        } catch (_: Throwable) {
        }
    }

    /**
     * Posts a high-priority full-screen ringing alarm notification with real-time elapsed chronometer
     * and interactive Snooze & Dismiss buttons.
     */
    fun postRingingAlarmNotification(
        context: Context,
        alarm: AlarmItem,
        isId: Boolean = true
    ) {
        if (!hasNotificationPermission(context)) return
        try {
            ensureNotificationChannels(context)
            val parts = alarm.label.split("|||")
            val cleanTitle = parts.firstOrNull().orEmpty().ifBlank { if (isId) "Alarm Berbunyi!" else "Alarm Ringing!" }
            val motivation = parts.getOrNull(1).orEmpty()
            val timeFormatted = "%02d:%02d".format(alarm.hour, alarm.minute)

            val themeSpec = com.example.ui.components.AlarmBackgroundThemes.getTheme(alarm.backgroundTheme)
            val themeName = if (isId) themeSpec.nameId else themeSpec.nameEn

            val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(EXTRA_ALARM_THEME, alarm.backgroundTheme)
            }
            val fullScreenPi = PendingIntent.getActivity(
                context,
                requestCodeFor(alarm.id, 4000),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, ChronosAlarmReceiver::class.java).apply {
                action = ACTION_SNOOZE_ALARM
                putExtra(EXTRA_ALARM_ID, alarm.id)
            }
            val snoozePi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(alarm.id, 5000),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dismissIntent = Intent(context, ChronosAlarmReceiver::class.java).apply {
                action = ACTION_DISMISS_ALARM
                putExtra(EXTRA_ALARM_ID, alarm.id)
            }
            val dismissPi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(alarm.id, 6000),
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val bodyText = if (motivation.isNotBlank()) {
                "$cleanTitle • \"$motivation\""
            } else {
                if (isId) "$cleanTitle • ${themeSpec.emoji} $themeName — Ketuk untuk membuka layar bangun realistis atau matikan."
                else "$cleanTitle • ${themeSpec.emoji} $themeName — Tap to open realistic wake screen or dismiss."
            }

            val ringColor = when (alarm.backgroundTheme) {
                "COSMIC_DAWN" -> 0xFFFF9100.toInt()
                "RAINY_HORIZON" -> 0xFF00E5FF.toInt()
                "CIRCADIAN_STARS" -> 0xFFD500F9.toInt()
                "FOREST_MIST" -> 0xFF00E676.toInt()
                else -> 0xFF00E5FF.toInt()
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_RING_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("${themeSpec.emoji} ALARM $timeFormatted • ${cleanTitle.uppercase()}")
                .setContentText(bodyText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                .setSubText(if (isId) "DERING REALISTIS AKTIF" else "REALISTIC RINGING LIVE")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                .setUsesChronometer(true)
                .setOngoing(true)
                .setAutoCancel(false)
                .setColor(ringColor)
                .setFullScreenIntent(fullScreenPi, true)
                .setContentIntent(fullScreenPi)
                .addAction(
                    android.R.drawable.ic_popup_sync,
                    if (isId) "Tunda (Snooze)" else "Snooze",
                    snoozePi
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    if (isId) "Matikan Alarm" else "Dismiss",
                    dismissPi
                )
                .build()

            NotificationManagerCompat.from(context).notify(NOTIF_ID_RINGING, notification)
        } catch (_: Throwable) {
        }
    }

    fun postPreWakeNotification(
        context: Context,
        alarm: AlarmItem,
        isId: Boolean = true,
        minutesRemaining: Int = 10
    ) {
        if (!hasNotificationPermission(context)) return
        try {
            ensureNotificationChannels(context)
            val cleanTitle = alarm.label.split("|||").firstOrNull().orEmpty().ifBlank { "Alarm" }
            val timeStr = "%02d:%02d".format(alarm.hour, alarm.minute)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_OPEN_ALARMS, true)
                putExtra(EXTRA_ALARM_ID, alarm.id)
            }
            val openPi = PendingIntent.getActivity(
                context,
                9200,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val skipIntent = Intent(context, ChronosAlarmReceiver::class.java).apply {
                action = ACTION_DISMISS_ALARM
                putExtra(EXTRA_ALARM_ID, alarm.id)
            }
            val skipPi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(alarm.id, 7000),
                skipIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ALERT_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(
                    if (isId) "🌅 Pra-Bangun Cerdas • $cleanTitle ($timeStr)"
                    else "🌅 Smart Pre-Wake • $cleanTitle ($timeStr)"
                )
                .setContentText(
                    if (isId) "Alarm utama berbunyi dalam $minutesRemaining menit. Siklus REM sedang diselaraskan."
                    else "Main alarm rings in $minutesRemaining minutes. Aligning REM wake phase."
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setColor(0xFFFFD54F.toInt())
                .setContentIntent(openPi)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    if (isId) "Sudah Bangun (Lewati)" else "Awake (Skip)",
                    skipPi
                )
                .build()

            NotificationManagerCompat.from(context).notify(NOTIF_ID_PRE_WAKE, notification)
        } catch (_: Throwable) {
        }
    }

    /**
     * Posts or cancels a zero-battery-drain live countdown notification for active Timers or Focus sessions.
     * Uses Android's native system UI chronometer (`setUsesChronometer(true)` + `setChronometerCountDown(true)`)
     * so the notification ticks every second in the shade WITHOUT waking up app coroutines!
     */
    fun postOrCancelLiveTimerNotification(
        context: Context,
        isRunning: Boolean,
        endTimeMs: Long?,
        totalSeconds: Int,
        isFocusSession: Boolean = false,
        isId: Boolean = true
    ) {
        val notifId = if (isFocusSession) NOTIF_ID_FOCUS_LIVE else NOTIF_ID_TIMER_LIVE
        if (!isRunning || endTimeMs == null || !hasNotificationPermission(context)) {
            try {
                NotificationManagerCompat.from(context).cancel(notifId)
            } catch (_: Throwable) {
            }
            return
        }
        try {
            ensureNotificationChannels(context)
            val targetScreen = if (isFocusSession) "productivity" else "timers"
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_TARGET_SCREEN, targetScreen)
            }
            val openPi = PendingIntent.getActivity(
                context,
                if (isFocusSession) 9401 else 9400,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val totalMins = (totalSeconds / 60).coerceAtLeast(1)
            val title = if (isFocusSession) {
                if (isId) "🎯 Sesi Fokus Mendalam Aktif ($totalMins mnt)" else "🎯 Deep Focus Session Active ($totalMins min)"
            } else {
                if (isId) "⏳ Timer Pintar Berjalan ($totalMins mnt)" else "⏳ Smart Timer Running ($totalMins min)"
            }
            val body = if (isFocusSession) {
                if (isId) "Hitung mundur otomatis hemat baterai di latar belakang." else "Zero-drain live countdown running in background."
            } else {
                if (isId) "Hitung mundur presisi berjalan stabil di latar belakang." else "Precision countdown running steadily in background."
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_STANDBY_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setSubText(if (isId) "CHRONOS SMART LIVE" else "CHRONOS SMART LIVE")
                .setWhen(endTimeMs)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(0xFF00E5FF.toInt())
                .setContentIntent(openPi)
                .build()

            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: Throwable) {
        }
    }

    fun postSmartAlertNotification(
        context: Context,
        title: String,
        body: String,
        subText: String = "CHRONOS SMART ALERT",
        targetScreen: String? = null
    ) {
        if (!hasNotificationPermission(context)) return
        try {
            ensureNotificationChannels(context)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (!targetScreen.isNullOrBlank()) {
                    putExtra(EXTRA_TARGET_SCREEN, targetScreen)
                }
            }
            val openPi = PendingIntent.getActivity(
                context,
                9300,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ALERT_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setSubText(subText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setColor(0xFF00E5FF.toInt())
                .setContentIntent(openPi)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIF_ID_TIMER_REMINDER, notification)
        } catch (_: Throwable) {
        }
    }

    internal fun acquireWakeLock(context: Context) {
        try {
            releaseWakeLock()
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            val wl = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "chronos:background_alarm_wakelock"
            )
            wl.acquire(60_000L)
            activeWakeLock = wl
        } catch (_: Throwable) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (activeWakeLock?.isHeld == true) {
                activeWakeLock?.release()
            }
        } catch (_: Throwable) {
        } finally {
            activeWakeLock = null
        }
    }

    internal fun triggerFromReceiver(context: Context, alarm: AlarmItem) {
        acquireWakeLock(context)
        lastReceiverAudioTriggerMs = System.currentTimeMillis()
        _backgroundRingingAlarm.value = alarm
        postRingingAlarmNotification(context, alarm, isId = true)
        ChronosAudioSynthesizer.playCustomOrPresetAlarm(
            context = context,
            soundUri = alarm.soundUri,
            durationSeconds = 60,
            fadeInSeconds = alarm.fadeInSeconds
        )
        if (alarm.isVibrate) {
            ChronosAudioSynthesizer.triggerHaptic(context, 3)
        }
    }

    internal fun emitBackgroundAction(context: Context, action: String, alarmId: Long) {
        ChronosAudioSynthesizer.stopChime()
        clearBackgroundRingingState(context)
        try {
            NotificationManagerCompat.from(context).cancel(NOTIF_ID_PRE_WAKE)
        } catch (_: Throwable) {
        }
        _backgroundActionEvent.value = action to alarmId
        updatePersistedAlarmsFromBackground(context, action, alarmId)
    }

    /**
     * Ensures that even if the app process was cold when the user tapped Snooze/Dismiss
     * in the system notification shade, SharedPreferences and AlarmManager are updated immediately.
     */
    internal fun updatePersistedAlarmsFromBackground(context: Context, action: String, alarmId: Long) {
        try {
            val prefs = context.getSharedPreferences("chronos_v1_66_88_prefs", Context.MODE_PRIVATE)
            val raw = prefs.getString("chronos_state_json", null) ?: return
            val root = JSONObject(raw)
            val sObj = root.optJSONObject("settings")
            val snoozeMins = sObj?.optInt("snoozeDuration", 9) ?: 9
            val notificationsEnabled = sObj?.optBoolean("notificationsEnabled", true) ?: true
            val isId = (sObj?.optString("appLanguage", "id") ?: "id") != "en"

            val aArr = root.optJSONArray("alarms") ?: return
            val list = mutableListOf<AlarmItem>()
            for (i in 0 until aArr.length()) {
                val item = aArr.getJSONObject(i)
                list.add(
                    AlarmItem(
                        id = item.optLong("id", 0L),
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
                        wakeMission = runCatching { WakeMission.valueOf(item.optString("wakeMission", "NONE")) }
                            .getOrDefault(WakeMission.NONE),
                        fadeInSeconds = item.optInt("fadeInSeconds", 10),
                        preWakeGentle = item.optBoolean("preWakeGentle", true),
                        backgroundTheme = item.optString("backgroundTheme", "COSMIC_DAWN")
                    )
                )
            }

            if (action == "BOOT_SYNC") {
                syncAlarmsWithSystem(context, list, notificationsEnabled, isId)
                return
            }

            val nowMs = System.currentTimeMillis()
            val target = list.find { it.id == alarmId } ?: return
            val updatedList = mutableListOf<AlarmItem>()
            for (a in list) {
                if (a.id == alarmId) {
                    if (a.label.startsWith("Snooze:")) {
                        // Remove one-shot snooze alarm
                        continue
                    } else if (a.daysOfWeek == 0 || a.dateMillis > 0L) {
                        updatedList.add(a.copy(isEnabled = false))
                    } else {
                        // Repeating alarm: if dismissed before or during its occurrence, mark upcoming occurrence skipped
                        val upcomingMs = getNextAlarmOccurrenceMillis(a, nowMs)
                        if (upcomingMs != null) {
                            markUpcomingOccurrenceSkipped(a.id, upcomingMs)
                        }
                        updatedList.add(a)
                    }
                } else {
                    updatedList.add(a)
                }
            }

            if (action == ACTION_SNOOZE_ALARM) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMs + (snoozeMins * 60_000L)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val snoozeTimeMs = cal.timeInMillis
                val baseTitle = target.label.split("|||").firstOrNull().orEmpty().removePrefix("Snooze: ")
                updatedList.add(
                    AlarmItem(
                        id = nowMs,
                        hour = cal.get(Calendar.HOUR_OF_DAY),
                        minute = cal.get(Calendar.MINUTE),
                        isEnabled = true,
                        label = "Snooze: $baseTitle",
                        daysOfWeek = 0,
                        dateMillis = snoozeTimeMs,
                        isVibrate = target.isVibrate,
                        soundUri = target.soundUri,
                        customAudioTitle = target.customAudioTitle,
                        isSnooze = true,
                        wakeMission = target.wakeMission,
                        fadeInSeconds = target.fadeInSeconds,
                        preWakeGentle = false,
                        backgroundTheme = target.backgroundTheme
                    )
                )
            }

            val newArr = org.json.JSONArray()
            updatedList.forEach { a ->
                newArr.put(
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
            root.put("alarms", newArr)
            prefs.edit().putString("chronos_state_json", root.toString()).apply()
            syncAlarmsWithSystem(context, updatedList, notificationsEnabled, isId)
        } catch (_: Throwable) {
        }
    }
}

class ChronosAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            ChronosAlarmScheduler.ACTION_TRIGGER_ALARM -> {
                val id = intent.getLongExtra(ChronosAlarmScheduler.EXTRA_ALARM_ID, System.currentTimeMillis())
                val hour = intent.getIntExtra(ChronosAlarmScheduler.EXTRA_ALARM_HOUR, 6)
                val minute = intent.getIntExtra(ChronosAlarmScheduler.EXTRA_ALARM_MINUTE, 30)
                val label = intent.getStringExtra(ChronosAlarmScheduler.EXTRA_ALARM_LABEL) ?: "Bangun Pagi"
                val sound = intent.getStringExtra(ChronosAlarmScheduler.EXTRA_ALARM_SOUND) ?: "Vitality Spirit"
                val customTitle = intent.getStringExtra(ChronosAlarmScheduler.EXTRA_ALARM_CUSTOM_TITLE) ?: ""
                val vibrate = intent.getBooleanExtra(ChronosAlarmScheduler.EXTRA_ALARM_VIBRATE, true)
                val fade = intent.getIntExtra(ChronosAlarmScheduler.EXTRA_ALARM_FADE, 10)
                val missionStr = intent.getStringExtra(ChronosAlarmScheduler.EXTRA_ALARM_MISSION) ?: "NONE"
                val mission = runCatching { WakeMission.valueOf(missionStr) }.getOrDefault(WakeMission.NONE)

                val alarm = AlarmItem(
                    id = id,
                    hour = hour,
                    minute = minute,
                    isEnabled = true,
                    label = label,
                    soundUri = sound,
                    customAudioTitle = customTitle,
                    isVibrate = vibrate,
                    fadeInSeconds = fade,
                    wakeMission = mission
                )
                ChronosAlarmScheduler.triggerFromReceiver(context, alarm)
            }

            ChronosAlarmScheduler.ACTION_PRE_WAKE_ALARM -> {
                val id = intent.getLongExtra(ChronosAlarmScheduler.EXTRA_ALARM_ID, 0L)
                val hour = intent.getIntExtra(ChronosAlarmScheduler.EXTRA_ALARM_HOUR, 6)
                val minute = intent.getIntExtra(ChronosAlarmScheduler.EXTRA_ALARM_MINUTE, 30)
                val label = intent.getStringExtra(ChronosAlarmScheduler.EXTRA_ALARM_LABEL) ?: "Alarm"
                val alarm = AlarmItem(id = id, hour = hour, minute = minute, label = label)
                ChronosAlarmScheduler.postPreWakeNotification(context, alarm, isId = true)
                ChronosAudioSynthesizer.playAlarmChime(ringtoneIndex = 3, durationSeconds = 3, fadeInSeconds = 2)
            }

            ChronosAlarmScheduler.ACTION_SNOOZE_ALARM,
            ChronosAlarmScheduler.ACTION_DISMISS_ALARM -> {
                val id = intent.getLongExtra(ChronosAlarmScheduler.EXTRA_ALARM_ID, -1L)
                if (id != -1L) {
                    ChronosAlarmScheduler.emitBackgroundAction(context, action, id)
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                ChronosAlarmScheduler.updatePersistedAlarmsFromBackground(context, "BOOT_SYNC", -1L)
            }
        }
    }
}
