package ir.daroyar.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.daroyar.app.R
import ir.daroyar.app.data.Medication
import ir.daroyar.app.drugs.DrugMatcher
import ir.daroyar.app.ui.MainActivity
import ir.daroyar.app.util.fa
import ir.daroyar.app.util.formatMinute
import java.time.LocalDate

object Notifier {

    private const val CHANNEL_ID = "dose_reminders"
    private const val TEST_ID = Int.MAX_VALUE

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "یادآوری مصرف دارو", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "اعلان در زمان هر نوبت دارو"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notificationId(medId: Long, minuteOfDay: Int): Int = (medId * 1440 + minuteOfDay).toInt()

    fun showDose(context: Context, med: Medication, minuteOfDay: Int, date: LocalDate) {
        val text = buildString {
            if (med.dose.isNotBlank()) append(med.dose).append(" · ")
            append("ساعت ").append(formatMinute(minuteOfDay))
            // The user's own note wins; otherwise a short hint from the drug reference ("همراه غذا").
            val hint = med.notes.ifBlank { DrugMatcher.match(med.name).firstNotNullOfOrNull { it.tip }.orEmpty() }
            if (hint.isNotBlank()) append("\n").append(hint)
            if (med.lowStock) append("\n⚠️ موجودی رو به اتمام است: ").append((med.stock ?: 0).fa()).append(" عدد")
        }
        val takenPi = actionIntent(context, ReminderScheduler.ACTION_TAKEN, med.id, minuteOfDay, date)
        val snoozePi = actionIntent(context, ReminderScheduler.ACTION_SNOOZE, med.id, minuteOfDay, date)

        val notification = baseBuilder(context)
            .setContentTitle("وقت مصرف ${med.name}")
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .addAction(0, "مصرف کردم ✓", takenPi)
            .addAction(0, "${ReminderScheduler.SNOOZE_MINUTES.toInt().fa()} دقیقه بعد", snoozePi)
            .build()
        post(context, notificationId(med.id, minuteOfDay), notification)
    }

    fun showTest(context: Context) {
        val notification = baseBuilder(context)
            .setContentTitle("دارویار آماده است")
            .setContentText("اعلان‌های یادآوری دارو به همین شکل نمایش داده می‌شوند.")
            .build()
        post(context, TEST_ID, notification)
    }

    fun cancel(context: Context, medId: Long, minuteOfDay: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId(medId, minuteOfDay))
    }

    private fun baseBuilder(context: Context): NotificationCompat.Builder {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF0F8B7D.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(open)
    }

    private fun actionIntent(context: Context, action: String, medId: Long, minuteOfDay: Int, date: LocalDate): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            ReminderScheduler.doseIntent(context, action, medId, minuteOfDay)
                .putExtra(ReminderScheduler.EXTRA_DAY, date.toEpochDay()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun post(context: Context, id: Int, notification: android.app.Notification) {
        if (!canNotify(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Notification permission withdrawn; nothing to show.
        }
    }
}
