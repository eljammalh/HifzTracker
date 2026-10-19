package com.hifz.tracker.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hifz.tracker.MainActivity
import com.hifz.tracker.R
import com.hifz.tracker.data.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    const val CHANNEL_ID = "hifz_reminders"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Reminders for daily memorization and review"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Schedules (or cancels) a daily reminder. type = "mem" or "rev". */
    fun schedule(context: Context, type: String, enabled: Boolean, hour: Int, minute: Int) {
        val wm = WorkManager.getInstance(context)
        val name = "reminder_$type"
        if (!enabled) {
            wm.cancelUniqueWork(name)
            return
        }
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, minute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMinutes = Duration.between(now, next).toMinutes()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf("type" to type))
            .build()
        wm.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val type = inputData.getString("type") ?: "mem"
        val ar = SettingsRepository(ctx).settings.first().arabic
        val (title, text) = if (type == "mem") {
            if (ar) "وِردُ الحفظ" to "حان وقت حفظ مقطع اليوم."
            else "Time to memorize" to "Today's memorization segment is waiting for you."
        } else {
            if (ar) "وِردُ المراجعة" to "لا تنسَ مراجعة محفوظك اليوم."
            else "Time to review" to "Keep your memorization strong — review today's portion."
        }
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            ctx, if (type == "mem") 1 else 2, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(ctx, Reminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(if (type == "mem") 101 else 102, notification)
        return Result.success()
    }
}
