package ir.daroyar.app

import android.app.Application
import ir.daroyar.app.reminder.Notifier
import ir.daroyar.app.reminder.ReminderScheduler

class DaroYarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        // Re-arming on every process start is cheap and repairs alarms a vendor battery saver dropped.
        ReminderScheduler.sync(this)
    }
}
