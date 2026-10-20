package ir.daroyar.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import ir.daroyar.app.data.MedStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * One alarm per time of day that any active medication uses (a "slot"), always set for its next
 * occurrence only. When it fires, [ReminderReceiver] notifies every medication due at that time
 * on that day and sets the slot's next alarm. Sharing one alarm per slot matters in Doze, where
 * Android delays an app's back-to-back exact alarms by several minutes.
 *
 * A slot rings only on the weekdays some medication needs it ([Schedule.slotDays]), so a time used
 * only by a weekly drug wakes the phone once a week.
 */
object ReminderScheduler {

    const val ACTION_FIRE = "ir.daroyar.app.FIRE"
    const val ACTION_SNOOZE_FIRE = "ir.daroyar.app.SNOOZE_FIRE"
    const val ACTION_TAKEN = "ir.daroyar.app.TAKEN"
    const val ACTION_SNOOZE = "ir.daroyar.app.SNOOZE"

    const val EXTRA_MED = "med"
    const val EXTRA_MINUTE = "minute"
    const val EXTRA_DAY = "epochDay"

    const val SNOOZE_MINUTES = 10L

    private const val PREFS = "daroyar_alarms"
    private const val KEY_SLOTS = "slots"

    /** Brings the alarms in line with the current medications; call after any change to them. */
    @Synchronized
    fun sync(context: Context) {
        val meds = MedStore.get(context).meds.value
        val wanted = meds
            .filter { it.active }
            .flatMap { it.times }
            .toSet()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_SLOTS, "").orEmpty()
            .split(',').mapNotNull { it.toIntOrNull() }.toSet()

        val am = context.getSystemService(AlarmManager::class.java)
        (previous - wanted).forEach { minute ->
            PendingIntent.getBroadcast(
                context, 0, slotIntent(context, minute),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )?.let {
                am.cancel(it)
                it.cancel()
            }
        }
        val now = LocalDateTime.now()
        wanted.forEach { minute ->
            scheduleSlot(context, minute, Schedule.nextAfter(now, minute, Schedule.slotDays(meds, minute)))
        }
        prefs.edit().putString(KEY_SLOTS, wanted.sorted().joinToString(",")).apply()
    }

    fun scheduleSlot(context: Context, minuteOfDay: Int, at: LocalDateTime) {
        setAlarm(context, slotIntent(context, minuteOfDay).putExtra(EXTRA_DAY, at.toLocalDate().toEpochDay()), at)
    }

    /** Shows one medication's reminder again in [SNOOZE_MINUTES], for the same dose (same date). */
    fun snooze(context: Context, medId: Long, minuteOfDay: Int, date: LocalDate) {
        val intent = doseIntent(context, ACTION_SNOOZE_FIRE, medId, minuteOfDay).putExtra(EXTRA_DAY, date.toEpochDay())
        setAlarm(context, intent, LocalDateTime.now().plusMinutes(SNOOZE_MINUTES))
    }

    /** Drops a pending snooze, e.g. when the dose is marked taken in the app. */
    fun cancelSnooze(context: Context, medId: Long, minuteOfDay: Int) {
        PendingIntent.getBroadcast(
            context, 0, doseIntent(context, ACTION_SNOOZE_FIRE, medId, minuteOfDay),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it)
            it.cancel()
        }
    }

    // Each PendingIntent is told apart by its data URI and action (extras don't count).
    private fun slotIntent(context: Context, minuteOfDay: Int): Intent =
        Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .setData(Uri.parse("daroyar://slot/$minuteOfDay"))
            .putExtra(EXTRA_MINUTE, minuteOfDay)

    fun doseIntent(context: Context, action: String, medId: Long, minuteOfDay: Int): Intent =
        Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("daroyar://dose/$medId/$minuteOfDay"))
            .putExtra(EXTRA_MED, medId)
            .putExtra(EXTRA_MINUTE, minuteOfDay)

    private fun setAlarm(context: Context, intent: Intent, at: LocalDateTime) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        } catch (e: SecurityException) {
            // Exact-alarm permission revoked between the check and the call.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }
}
