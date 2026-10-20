package ir.daroyar.app

import ir.daroyar.app.util.JalaliDate
import ir.daroyar.app.util.PERSIAN_WEEK
import ir.daroyar.app.util.fa
import ir.daroyar.app.util.formatDays
import ir.daroyar.app.util.formatMinute
import ir.daroyar.app.util.formatPersianDate
import ir.daroyar.app.util.latinDigits
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PersianTest {

    private fun jalali(y: Int, m: Int, d: Int) = JalaliDate.from(LocalDate.of(y, m, d))

    @Test
    fun nowruzFallsOnTheRightGregorianDay() {
        assertEquals(JalaliDate(1403, 1, 1), jalali(2024, 3, 20))
        assertEquals(JalaliDate(1402, 12, 29), jalali(2024, 3, 19))
        assertEquals(JalaliDate(1404, 1, 1), jalali(2025, 3, 21))
        assertEquals(JalaliDate(1403, 12, 30), jalali(2025, 3, 20)) // 1403 is a leap year
        assertEquals(JalaliDate(1405, 1, 1), jalali(2026, 3, 21))
        assertEquals(JalaliDate(1406, 1, 1), jalali(2027, 3, 21))
    }

    @Test
    fun convertsDatesInsideTheYear() {
        assertEquals(JalaliDate(1405, 7, 28), jalali(2026, 10, 20))
        assertEquals(JalaliDate(1405, 6, 9), jalali(2026, 8, 31))
        assertEquals(JalaliDate(1378, 10, 11), jalali(2000, 1, 1))
    }

    @Test
    fun formatsDateInPersian() {
        assertEquals("سه‌شنبه ۲۸ مهر ۱۴۰۵", formatPersianDate(LocalDate.of(2026, 10, 20)))
    }

    @Test
    fun weekStartsOnSaturdayAndDaysAreListedInThatOrder() {
        assertEquals(DayOfWeek.SATURDAY, PERSIAN_WEEK.first())
        assertEquals(DayOfWeek.values().toSet(), PERSIAN_WEEK.toSet())
        assertEquals("هر روز", formatDays(emptySet()))
        assertEquals("هر روز", formatDays(DayOfWeek.values().toSet()))
        assertEquals("هفته‌ای یک بار: شنبه", formatDays(setOf(DayOfWeek.SATURDAY)))
        assertEquals("شنبه، دوشنبه، جمعه", formatDays(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.SATURDAY)))
    }

    @Test
    fun digitsRoundTrip() {
        assertEquals("۰۸:۳۰", formatMinute(8 * 60 + 30))
        assertEquals("۲۳:۰۵", formatMinute(23 * 60 + 5))
        assertEquals("۱ قرص", "1 قرص".fa())
        assertEquals("120", "۱۲۰".latinDigits())
        assertEquals("45", "٤٥".latinDigits())
    }
}
