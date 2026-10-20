package ir.daroyar.app

import ir.daroyar.app.data.DateRange
import ir.daroyar.app.data.Medication
import ir.daroyar.app.reminder.DoseStatus
import ir.daroyar.app.reminder.Schedule
import ir.daroyar.app.util.JalaliDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    private val week = (0L until 7L).map { LocalDate.of(2026, 10, 24).plusDays(it) } // Saturday to Friday

    private fun slotDays(meds: List<Medication>, minute: Int) =
        week.filter { Schedule.slotDueOn(meds, minute, it) }.map { it.dayOfWeek }.toSet()

    @Test
    fun slotRingsOnTheUnionOfItsMedicationsDays() {
        val sat = Medication(id = 1, name = "a", times = listOf(eight), days = setOf(SATURDAY))
        val mon = Medication(id = 2, name = "b", times = listOf(eight, 20 * 60), days = setOf(MONDAY))
        val daily = Medication(id = 3, name = "c", times = listOf(eight))
        val otherTime = Medication(id = 4, name = "d", times = listOf(9 * 60))

        assertEquals(setOf(SATURDAY, MONDAY), slotDays(listOf(sat, mon, otherTime), eight))
        assertEquals(setOf(MONDAY), slotDays(listOf(sat, mon), 20 * 60))
        // One daily medication at that time makes the slot daily.
        assertEquals(DayOfWeek.values().toSet(), slotDays(listOf(sat, mon, daily), eight))
        // Paused medications don't count.
        assertEquals(setOf(SATURDAY), slotDays(listOf(sat, daily.copy(active = false)), eight))
    }

    // 1 Aban 1405 is 2026-10-23; 21 Mehr 1405 is 2026-10-13.

    @Test
    fun oneOffRangeIsDueOnlyBetweenItsDays() {
        val range = DateRange(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 24))
        val med = Medication(id = 1, name = "x", times = listOf(eight), range = range)
        assertFalse(med.isDueOn(LocalDate.of(2026, 10, 12)))
        assertTrue(med.isDueOn(LocalDate.of(2026, 10, 13)))
        assertTrue(med.isDueOn(LocalDate.of(2026, 10, 24)))
        assertFalse(med.isDueOn(LocalDate.of(2026, 10, 25)))
        assertTrue(range.endedBefore(LocalDate.of(2026, 10, 25)))
        assertEquals(12, range.length)
    }

    @Test
    fun monthlyRangeRepeatsOnTheSameJalaliDays() {
        // 21 Mehr to 2 Aban, then 21 Aban to 2 Azar and so on.
        val start = JalaliDate(1405, 7, 21).toLocalDate()
        val end = JalaliDate(1405, 8, 2).toLocalDate()
        val range = DateRange(start, end, monthly = true)
        assertEquals(12, range.length)
        assertTrue(JalaliDate(1405, 8, 2).toLocalDate() in range)
        assertFalse(JalaliDate(1405, 8, 3).toLocalDate() in range)
        assertFalse(JalaliDate(1405, 8, 20).toLocalDate() in range)
        assertTrue(JalaliDate(1405, 8, 21).toLocalDate() in range)
        assertTrue(JalaliDate(1405, 9, 1).toLocalDate() in range)
        assertFalse(JalaliDate(1405, 9, 3).toLocalDate() in range)
        assertTrue(JalaliDate(1406, 1, 25).toLocalDate() in range)
        // Not before it was first set.
        assertFalse(JalaliDate(1405, 6, 25).toLocalDate() in range)
        assertFalse(range.endedBefore(LocalDate.of(2030, 1, 1)))
    }

    @Test
    fun monthlyRangeOnThe31stUsesTheLastDayOfShorterMonths() {
        val start = JalaliDate(1405, 6, 31).toLocalDate()
        val range = DateRange(start, start.plusDays(2), monthly = true)
        // Mehr has 30 days, so its span starts on the 30th.
        assertFalse(JalaliDate(1405, 7, 29).toLocalDate() in range)
        assertTrue(JalaliDate(1405, 7, 30).toLocalDate() in range)
        assertTrue(JalaliDate(1405, 8, 2).toLocalDate() in range)
        assertFalse(JalaliDate(1405, 8, 3).toLocalDate() in range)
    }

    @Test
    fun slotFollowsTheDateRangeAndStopsAfterIt() {
        val range = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3))
        val med = Medication(id = 1, name = "x", times = listOf(eight), range = range)
        val now = LocalDateTime.of(2026, 10, 20, 9, 0)
        assertEquals(LocalDateTime.of(2026, 11, 1, 8, 0), Schedule.nextSlot(listOf(med), eight, now))
        val fired = LocalDate.of(2026, 11, 3)
        assertNull(Schedule.followingSlot(listOf(med), eight, fired, fired.atTime(8, 0, 1)))
        assertNull(Schedule.nextSlot(listOf(med), eight, LocalDateTime.of(2026, 11, 4, 0, 0)))
    }

    @Test
    fun monthlySlotJumpsToNextMonthsSpan() {
        val start = JalaliDate(1405, 7, 21).toLocalDate()
        val range = DateRange(start, start.plusDays(1), monthly = true) // 21st and 22nd
        val med = Medication(id = 1, name = "x", times = listOf(eight), range = range)
        val fired = JalaliDate(1405, 7, 22).toLocalDate()
        assertEquals(
            JalaliDate(1405, 8, 21).toLocalDate().atTime(8, 0),
            Schedule.followingSlot(listOf(med), eight, fired, fired.atTime(8, 0, 1)),
        )
    }

    @Test
    fun rangeIsSavedAndRestored() {
        val range = DateRange(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 24), monthly = true)
        val med = Medication(id = 1, name = "x", times = listOf(eight), range = range)
        assertEquals(med, Medication.fromJson(med.toJson()))
        assertNull(Medication.fromJson(med.copy(range = null).toJson()).range)
    }

    @Test
    fun jalaliDatesRoundTrip() {
        var d = LocalDate.of(2024, 1, 1)
        while (d.year < 2031) {
            assertEquals(d, JalaliDate.from(d).toLocalDate())
            d = d.plusDays(1)
        }
        assertEquals(LocalDate.of(2026, 10, 23), JalaliDate(1405, 8, 1).toLocalDate())
        assertEquals(30, JalaliDate.monthLength(1403, 12)) // 1403 is a leap year
        assertEquals(29, JalaliDate.monthLength(1404, 12))
        assertEquals(JalaliDate(1406, 1, 31), JalaliDate(1405, 12, 31).plusMonths(1))
        assertEquals(JalaliDate(1405, 12, 29), JalaliDate(1405, 11, 30).plusMonths(1))
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
