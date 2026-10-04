package com.ascend.app.reminder

import android.Manifest
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
import com.ascend.app.AscendApplication
import com.ascend.app.MainActivity
import com.ascend.app.R
import com.ascend.app.di.ReminderScheduler
import com.ascend.app.domain.PlanStatus
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/** Ежедневное напоминание через WorkManager — переживает перезагрузку устройства. */
class WorkReminderScheduler(private val context: Context) : ReminderScheduler {

    override fun schedule(hour: Int, minute: Int) {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private companion object {
        const val WORK_NAME = "ascend-daily-reminder"
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as AscendApplication).container
        val hero = container.repository.heroState.first() ?: return Result.success()
        if (!hero.reminderEnabled) return Result.success()
        val plan = container.repository.dayPlan(container.time.today()).first()
        val pending = plan.items.count { it.status == PlanStatus.PENDING }
        val message = when {
            plan.total == 0 -> "${hero.name}, герой ждёт приключений" to
                "Добавь хотя бы маленький квест — любой шаг засчитается ✨"
            pending == 0 -> return Result.success()
            plan.done == 0 -> "${hero.name}, новый день — новый опыт" to
                "Квестов на сегодня: $pending. Начни с самого лёгкого"
            else -> "Отличный темп, ${hero.name}!" to
                "Осталось квестов: $pending. Даже один шаг принесёт опыт 🔥"
        }
        ReminderNotifications.show(applicationContext, message.first, message.second)
        return Result.success()
    }
}

object ReminderNotifications {
    private const val CHANNEL_ID = "ascend-reminders"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Напоминания", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Ежедневный мягкий зов к квестам"
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun show(context: Context, title: String, text: String) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!allowed) return
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF8B5CF6.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Разрешение отозвано между проверкой и показом — просто молчим.
        }
    }
}
