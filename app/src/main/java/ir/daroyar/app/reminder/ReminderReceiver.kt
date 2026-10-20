package ir.daroyar.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.daroyar.app.data.MedStore
import java.time.LocalDate
import java.time.LocalDateTime

/** Handles dose alarms and the notification's "taken" / "snooze" buttons. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val minute = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTE, -1)
        val medId = intent.getLongExtra(ReminderScheduler.EXTRA_MED, -1)
        val date = LocalDate.ofEpochDay(intent.getLongExtra(ReminderScheduler.EXTRA_DAY, LocalDate.now().toEpochDay()))
        if (minute < 0) return

        val store = MedStore.get(context)
        when (intent.action) {
            ReminderScheduler.ACTION_FIRE -> {
                // Medications may have been deleted, paused or given new times since the alarm was set.
                val due = store.meds.value.filter { it.active && minute in it.times }
                due.filterNot { store.isTaken(it.id, date, minute) }
                    .forEach { Notifier.showDose(context, it, minute, date) }
                if (due.isNotEmpty()) {
                    ReminderScheduler.scheduleSlot(context, minute, Schedule.following(date, minute, LocalDateTime.now()))
                }
            }
            ReminderScheduler.ACTION_SNOOZE_FIRE -> {
                val med = store.medication(medId) ?: return
                if (med.active && minute in med.times && !store.isTaken(medId, date, minute)) {
                    Notifier.showDose(context, med, minute, date)
                }
            }
            ReminderScheduler.ACTION_TAKEN -> {
                store.setTaken(medId, date, minute, true)
                Notifier.cancel(context, medId, minute)
            }
            ReminderScheduler.ACTION_SNOOZE -> {
                Notifier.cancel(context, medId, minute)
                ReminderScheduler.snooze(context, medId, minute, date)
            }
        }
    }
}
