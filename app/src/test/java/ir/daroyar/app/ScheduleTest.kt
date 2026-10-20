package ir.daroyar.app

import ir.daroyar.app.data.Medication
import ir.daroyar.app.reminder.DoseStatus
import ir.daroyar.app.reminder.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleTest {

    private val eight = 8 * 60

    @Test
    fun nextAfterIsLaterTodayOrTomorrow() {
        val morning = LocalDateTime.of(2026, 10, 20, 7, 59)
        assertEquals(LocalDateTime.of(2026, 10, 20, 8, 0), Schedule.nextAfter(morning, eight))

        // Exactly at the dose time (the alarm is being delivered) the next one is tomorrow.
        val atTime = LocalDateTime.of(2026, 10, 20, 8, 0)
        assertEquals(LocalDateTime.of(2026, 10, 21, 8, 0), Schedule.nextAfter(atTime, eight))
    }

    @Test
    fun nextAfterCrossesMonthAndYearEnds() {
        val newYearsEve = LocalDateTime.of(2026, 12, 31, 23, 30)
        assertEquals(LocalDateTime.of(2027, 1, 1, 0, 0), Schedule.nextAfter(newYearsEve, 0))
    }

    @Test
    fun followingIsTheNextDay() {
        val fired = LocalDate.of(2026, 10, 20)
        val now = LocalDateTime.of(2026, 10, 20, 8, 0, 1)
        assertEquals(LocalDateTime.of(2026, 10, 21, 8, 0), Schedule.following(fired, eight, now))
    }

    @Test
    fun followingSkipsDaysMissedWhileThePhoneWasOff() {
        val fired = LocalDate.of(2026, 10, 20)
        val now = LocalDateTime.of(2026, 10, 23, 12, 0) // late delivery, three days on
        assertEquals(LocalDateTime.of(2026, 10, 24, 8, 0), Schedule.following(fired, eight, now))
    }

    @Test
    fun doseStatus() {
        assertEquals(DoseStatus.UPCOMING, Schedule.status(eight, LocalTime.of(7, 0), taken = false))
        assertEquals(DoseStatus.LATE, Schedule.status(eight, LocalTime.of(8, 0), taken = false))
        assertEquals(DoseStatus.TAKEN, Schedule.status(eight, LocalTime.of(9, 0), taken = true))
        assertEquals(DoseStatus.TAKEN, Schedule.status(eight, LocalTime.of(7, 0), taken = true))
    }

    @Test
    fun presetsAreSortedValidTimes() {
        Schedule.presets.forEach { (_, times) ->
            assertEquals(times.sorted(), times)
            assertTrue(times.all { it in 0 until 24 * 60 })
        }
    }

    @Test
    fun lowStockMeansUnderThreeDaysLeft() {
        val med = Medication(id = 1, name = "x", times = listOf(eight, 20 * 60), perDose = 1)
        assertFalse(med.copy(stock = null).lowStock)
        assertFalse(med.copy(stock = 6).lowStock)
        assertTrue(med.copy(stock = 5).lowStock)
        assertTrue(med.copy(stock = 11, perDose = 2).lowStock)
    }
}
