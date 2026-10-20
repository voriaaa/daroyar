package ir.daroyar.app

import android.app.Application
import android.util.Log
import ir.daroyar.app.drugs.DrugIndex
import ir.daroyar.app.reminder.Notifier
import ir.daroyar.app.reminder.ReminderScheduler

class DaroYarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        // Re-arming on every process start is cheap and repairs alarms a vendor battery saver dropped.
        ReminderScheduler.sync(this)
        // Thousands of names; loaded off the main thread. Until it finishes only detailed entries match.
        Thread({
            try {
                DrugIndex.load(this)
            } catch (e: Exception) {
                Log.w("DaroYar", "drug index not loaded", e)
            }
        }, "drug-index").start()
    }
}
