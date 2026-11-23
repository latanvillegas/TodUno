package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.LunarAstronomer
import com.example.domain.MoonPhaseType
import com.example.domain.SimpleDate
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object SeleniaAlertManager {
    const val CHANNEL_TIMER_ID = "selenia_timer_alerts"
    const val CHANNEL_LUNAR_ID = "selenia_lunar_reminders"

    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_IS_TIMER = "extra_is_timer"

    private const val TIMER_NOTIFICATION_ID = 4001
    private const val LUNAR_BASE_ID = 5000
    private const val NOTE_BASE_ID = 8000

    private val isChimePlaying = AtomicBoolean(false)

    // Single lightweight daemon executor that times out to 0 threads when idle:
    // avoids allocating raw OS Threads per chime and guarantees native ToneGenerator release.
    private val audioReleaseExecutor by lazy {
        ScheduledThreadPoolExecutor(1) { runnable ->
            Executors.defaultThreadFactory().newThread(runnable).apply {
                name = "selenia-audio-release"
                isDaemon = true
            }
        }.apply {
            setKeepAliveTime(5L, TimeUnit.SECONDS)
            allowCoreThreadTimeOut(true)
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_ID,
                "Temporizador TodUno",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas sonoras y vibración al finalizar el temporizador"
                enableVibration(true)
            }
            val lunarChannel = NotificationChannel(
                CHANNEL_LUNAR_ID,
                "Fases Lunares y Notas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios de Luna Llena, Luna Nueva y notas del calendario"
            }
            nm.createNotificationChannel(timerChannel)
            nm.createNotificationChannel(lunarChannel)
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

    fun triggerTimerFinishedFeedback(context: Context) {
        val appCtx = context.applicationContext
        vibrateAlertPattern(appCtx, longAlert = true)
        playChimeSound()
        cancelBackgroundTimerAlert(appCtx)
        postNotification(
            context = appCtx,
            notificationId = TIMER_NOTIFICATION_ID,
            title = "¡Temporizador completado!",
            body = "Tu cuenta regresiva en TodUno ha llegado a 00:00.",
            isTimer = true
        )
    }

    fun vibrateAlertPattern(context: Context, longAlert: Boolean = true) {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = if (longAlert) {
                        longArrayOf(0L, 260L, 140L, 260L, 140L, 420L)
                    } else {
                        longArrayOf(0L, 120L, 80L, 160L)
                    }
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(if (longAlert) 700L else 200L)
                }
            }
        }
    }

    fun playChimeSound() {
        if (!isChimePlaying.compareAndSet(false, true)) return
        var toneGen: ToneGenerator? = null
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 85)
            val started = toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 550)
            if (started) {
                val captured = toneGen
                audioReleaseExecutor.schedule({
                    try {
                        captured.release()
                    } catch (_: Throwable) {
                    } finally {
                        isChimePlaying.set(false)
                    }
                }, 620L, TimeUnit.MILLISECONDS)
            } else {
                toneGen.release()
                isChimePlaying.set(false)
            }
        } catch (_: Throwable) {
            runCatching { toneGen?.release() }
            isChimePlaying.set(false)
        }
    }

    /**
     * Schedules a battery-friendly window alarm anchored on monotonic [SystemClock.elapsedRealtime]
     * so wall-clock/NTP adjustments cannot prematurely fire or delay an active countdown.
     */
    fun scheduleBackgroundTimerAlert(context: Context, remainingDurationMs: Long, totalSeconds: Int) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val hrs = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        val label = when {
            hrs > 0 -> "${hrs}h ${mins}m ${secs}s"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
        val intent = Intent(appCtx, LunarNotificationReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, TIMER_NOTIFICATION_ID)
            putExtra(EXTRA_TITLE, "¡Temporizador completado! ($label)")
            putExtra(EXTRA_BODY, "El tiempo programado en TodUno ha finalizado.")
            putExtra(EXTRA_IS_TIMER, true)
        }
        val pi = PendingIntent.getBroadcast(
            appCtx,
            TIMER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerElapsedMs = SystemClock.elapsedRealtime() + remainingDurationMs.coerceAtLeast(1_000L)
        alarmManager.setWindow(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerElapsedMs,
            2_500L,
            pi
        )
    }

    fun cancelBackgroundTimerAlert(context: Context) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(appCtx, LunarNotificationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            appCtx,
            TIMER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun scheduleNoteReminder(
        context: Context,
        noteId: Long,
        date: SimpleDate,
        title: String,
        category: String,
        reminderHour: Int = 9,
        reminderMinute: Int = 0
    ) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val safeHour = reminderHour.coerceIn(0, 23)
        val safeMinute = reminderMinute.coerceIn(0, 59)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.month - 1)
            set(Calendar.DAY_OF_MONTH, date.day)
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        val triggerMs = if (cal.timeInMillis <= now) {
            if (date == LunarAstronomer.today()) now + 8_000L else return
        } else {
            cal.timeInMillis
        }

        val safeTitle = title.take(160)
        val safeCategory = category.take(36)
        val formattedTime = "%02d:%02d".format(Locale.US, safeHour, safeMinute)
        val reqCode = NOTE_BASE_ID + ((noteId and 0x7FFF_FFFFL) % 10000L).toInt()
        val intent = Intent(appCtx, LunarNotificationReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, reqCode)
            putExtra(EXTRA_TITLE, "Recordatorio TodUno • $safeCategory ($formattedTime)")
            putExtra(EXTRA_BODY, "$safeTitle (${date.formatSpanishShort()})")
            putExtra(EXTRA_IS_TIMER, false)
        }
        val pi = PendingIntent.getBroadcast(
            appCtx,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerMs, 5 * 60 * 1000L, pi)
    }

    fun cancelNoteReminder(context: Context, noteId: Long) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val reqCode = NOTE_BASE_ID + ((noteId and 0x7FFF_FFFFL) % 10000L).toInt()
        val intent = Intent(appCtx, LunarNotificationReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            appCtx,
            reqCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun syncUpcomingLunarPhaseAlerts(context: Context, enabled: Boolean) {
        val appCtx = context.applicationContext
        val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (i in 0..5) {
            val cancelIntent = Intent(appCtx, LunarNotificationReceiver::class.java)
            val cancelPi = PendingIntent.getBroadcast(
                appCtx,
                LUNAR_BASE_ID + i,
                cancelIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (cancelPi != null) {
                alarmManager.cancel(cancelPi)
                cancelPi.cancel()
            }
        }
        if (!enabled) return

        val today = LunarAstronomer.today()
        val milestones = LunarAstronomer.getUpcomingMajorPhases(today, count = 6)
            .filter { it.phaseType == MoonPhaseType.FULL_MOON || it.phaseType == MoonPhaseType.NEW_MOON }

        val now = System.currentTimeMillis()
        milestones.take(4).forEachIndexed { idx, milestone ->
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, milestone.date.year)
                set(Calendar.MONTH, milestone.date.month - 1)
                set(Calendar.DAY_OF_MONTH, milestone.date.day)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis > now) {
                val reqCode = LUNAR_BASE_ID + idx
                val intent = Intent(appCtx, LunarNotificationReceiver::class.java).apply {
                    putExtra(EXTRA_NOTIFICATION_ID, reqCode)
                    putExtra(EXTRA_TITLE, "Hoy es ${milestone.phaseType.spanishName}")
                    putExtra(
                        EXTRA_BODY,
                        "Iluminación ${milestone.illuminationPercent}% • Abre el Observatorio Lunar para ver efemérides."
                    )
                    putExtra(EXTRA_IS_TIMER, false)
                }
                val pi = PendingIntent.getBroadcast(
                    appCtx,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, cal.timeInMillis, 45 * 60 * 1000L, pi)
            }
        }
    }

    fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        isTimer: Boolean
    ) {
        val appCtx = context.applicationContext
        ensureChannels(appCtx)
        if (!hasNotificationPermission(appCtx)) return

        val openAppIntent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            appCtx,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isTimer) CHANNEL_TIMER_ID else CHANNEL_LUNAR_ID
        val notification = NotificationCompat.Builder(appCtx, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (isTimer) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        // Android 13+ permission may be revoked between composing the notification and posting it.
        // Keep the permission check local to the call so Android Lint can verify the guarded path.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appCtx,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        try {
            NotificationManagerCompat.from(appCtx).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission can change concurrently; skip the notification without crashing.
        }
    }
}

class LunarNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appCtx = context.applicationContext
        val id = intent.getIntExtra(SeleniaAlertManager.EXTRA_NOTIFICATION_ID, 5999)
        val title = intent.getStringExtra(SeleniaAlertManager.EXTRA_TITLE) ?: "TodUno"
        val body = intent.getStringExtra(SeleniaAlertManager.EXTRA_BODY) ?: "Evento astronómico programado"
        val isTimer = intent.getBooleanExtra(SeleniaAlertManager.EXTRA_IS_TIMER, false)

        if (isTimer) {
            SeleniaAlertManager.vibrateAlertPattern(appCtx, longAlert = true)
            SeleniaAlertManager.playChimeSound()
        } else {
            SeleniaAlertManager.vibrateAlertPattern(appCtx, longAlert = false)
        }

        SeleniaAlertManager.postNotification(
            context = appCtx,
            notificationId = id,
            title = title,
            body = body,
            isTimer = isTimer
        )
    }
}

class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_TIME_CHANGED
        ) {
            return
        }
        val appCtx = context.applicationContext
        val pendingResult = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val repository = com.example.data.SeleniaRepository.getInstance(appCtx)
                val settings = repository.getPersistedSettingsSnapshot()
                if (settings.lunarAlertsEnabled) {
                    SeleniaAlertManager.syncUpcomingLunarPhaseAlerts(appCtx, true)
                }
                val reminderNotes = repository.getReminderNotes()
                reminderNotes.forEach { note ->
                    SeleniaAlertManager.scheduleNoteReminder(
                        context = appCtx,
                        noteId = note.id,
                        date = note.date,
                        title = note.title,
                        category = note.category,
                        reminderHour = note.reminderHour,
                        reminderMinute = note.reminderMinute
                    )
                }
                if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    val nowWall = System.currentTimeMillis()
                    if (settings.timerRunning && settings.timerEndWallEpochMs > nowWall) {
                        SeleniaAlertManager.scheduleBackgroundTimerAlert(
                            context = appCtx,
                            remainingDurationMs = settings.timerEndWallEpochMs - nowWall,
                            totalSeconds = settings.timerTotalSeconds
                        )
                    }
                }
                com.example.widget.SeleniaMoonWidgetProvider.refreshAllWidgets(
                    context = appCtx,
                    isNorthernHemisphereOverride = settings.isNorthernHemisphere
                )
            } catch (_: Throwable) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
