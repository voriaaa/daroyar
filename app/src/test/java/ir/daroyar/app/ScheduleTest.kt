package ir.daroyar.app

import ir.daroyar.app.data.Medication
import ir.daroyar.app.reminder.DoseStatus
import ir.daroyar.app.reminder.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.WEDNESDAY
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

    // 2026-10-20 is a Tuesday, so 2026-10-24 is a Saturday.

    @Test
    fun nextAfterSkipsToTheChosenWeekday() {
        val tuesday = LocalDateTime.of(2026, 10, 20, 7, 0)
        assertEquals(LocalDateTime.of(2026, 10, 24, 8, 0), Schedule.nextAfter(tuesday, eight, setOf(SATURDAY)))
    }

    @Test
    fun nextAfterOnTheChosenDayWrapsToNextWeekOnceThatTimeHasPassed() {
        val saturday = LocalDateTime.of(2026, 10, 24, 7, 59)
        assertEquals(LocalDateTime.of(2026, 10, 24, 8, 0), Schedule.nextAfter(saturday, eight, setOf(SATURDAY)))

        val atTime = LocalDateTime.of(2026, 10, 24, 8, 0)
        assertEquals(LocalDateTime.of(2026, 10, 31, 8, 0), Schedule.nextAfter(atTime, eight, setOf(SATURDAY)))
    }

    @Test
    fun nextAfterWrapsPastTheEndOfTheWeek() {
        val fridayNight = LocalDateTime.of(2026, 10, 23, 21, 0)
        val monWed = setOf(MONDAY, WEDNESDAY)
        assertEquals(LocalDateTime.of(2026, 10, 26, 8, 0), Schedule.nextAfter(fridayNight, eight, monWed))
    }

    @Test
    fun nextAfterWeeklyCrossesTheYearEnd() {
        val newYearsEve = LocalDateTime.of(2026, 12, 31, 23, 30) // Thursday
        assertEquals(LocalDateTime.of(2027, 1, 2, 0, 0), Schedule.nextAfter(newYearsEve, 0, setOf(SATURDAY)))
    }

    @Test
    fun followingWeeklyIsSevenDaysLater() {
        val fired = LocalDate.of(2026, 10, 24)
        val now = LocalDateTime.of(2026, 10, 24, 8, 0, 1)
        assertEquals(LocalDateTime.of(2026, 10, 31, 8, 0), Schedule.following(fired, eight, now, setOf(SATURDAY)))
    }

    @Test
    fun followingGoesToTheNextChosenDayAndWrapsTheWeek() {
        val days = setOf(MONDAY, WEDNESDAY, FRIDAY)
        val monday = LocalDate.of(2026, 10, 26)
        assertEquals(
            LocalDateTime.of(2026, 10, 28, 8, 0),
            Schedule.following(monday, eight, monday.atTime(8, 0, 1), days),
        )
        val friday = LocalDate.of(2026, 10, 30)
        assertEquals(
            LocalDateTime.of(2026, 11, 2, 8, 0),
            Schedule.following(friday, eight, friday.atTime(8, 0, 1), days),
        )
    }

    @Test
    fun followingWeeklySkipsWeeksMissedWhileThePhoneWasOff() {
        val fired = LocalDate.of(2026, 10, 24)
        val now = LocalDateTime.of(2026, 11, 2, 12, 0) // the alarm of 31 Oct was missed too
        assertEquals(LocalDateTime.of(2026, 11, 7, 8, 0), Schedule.following(fired, eight, now, setOf(SATURDAY)))
    }

    @Test
    fun emptyOrAllSevenDaysMeansEveryDay() {
        assertEquals(emptySet<DayOfWeek>(), Schedule.normalizeDays(DayOfWeek.values().toList()))
        assertEquals(setOf(SATURDAY), Schedule.normalizeDays(listOf(SATURDAY, SATURDAY)))
        val week = (0L until 7L).map { LocalDate.of(2026, 10, 20).plusDays(it) }
        assertTrue(week.all { Schedule.isDue(emptySet(), it) })
        assertEquals(listOf(LocalDate.of(2026, 10, 24)), week.filter { Schedule.isDue(setOf(SATURDAY), it) })
        // Without days, nextAfter behaves exactly as before.
        val t = LocalDateTime.of(2026, 10, 20, 9, 0)
        assertEquals(Schedule.nextAfter(t, eight), Schedule.nextAfter(t, eight, emptySet()))
    }

    @Test
    fun medicationIsDueOnlyOnItsDays() {
        val weekly = Medication(id = 1, name = "x", times = listOf(eight), days = setOf(SATURDAY))
        assertTrue(weekly.isDueOn(LocalDate.of(2026, 10, 24)))
        assertFalse(weekly.isDueOn(LocalDate.of(2026, 10, 25)))
        assertTrue(weekly.copy(days = emptySet()).isDueOn(LocalDate.of(2026, 10, 25)))
    }

    @Test
    fun slotRingsOnTheUnionOfItsMedicationsDays() {
        val sat = Medication(id = 1, name = "a", times = listOf(eight), days = setOf(SATURDAY))
        val mon = Medication(id = 2, name = "b", times = listOf(eight, 20 * 60), days = setOf(MONDAY))
        val daily = Medication(id = 3, name = "c", times = listOf(eight))
        val otherTime = Medication(id = 4, name = "d", times = listOf(9 * 60))

        assertEquals(setOf(SATURDAY, MONDAY), Schedule.slotDays(listOf(sat, mon, otherTime), eight))
        assertEquals(setOf(MONDAY), Schedule.slotDays(listOf(sat, mon), 20 * 60))
        // One daily medication at that time makes the slot daily.
        assertEquals(emptySet<DayOfWeek>(), Schedule.slotDays(listOf(sat, mon, daily), eight))
        // Paused medications don't count.
        assertEquals(setOf(SATURDAY), Schedule.slotDays(listOf(sat, daily.copy(active = false)), eight))
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
