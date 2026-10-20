package ir.daroyar.app.util

import ir.daroyar.app.data.DateRange
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"

/** Replaces Latin digits with Persian ones for display. */
fun String.fa(): String = buildString(length) {
    for (c in this@fa) append(if (c in '0'..'9') PERSIAN_DIGITS[c - '0'] else c)
}

fun Int.fa(): String = toString().fa()

/** Replaces Persian and Arabic-Indic digits with Latin ones so user input can be parsed. */
fun String.latinDigits(): String = buildString(length) {
    for (c in this@latinDigits) {
        val p = PERSIAN_DIGITS.indexOf(c)
        val a = ARABIC_DIGITS.indexOf(c)
        append(
            when {
                p >= 0 -> '0' + p
                a >= 0 -> '0' + a
                else -> c
            }
        )
    }
}

/** "08:30" in Persian digits, from minutes since midnight. */
fun formatMinute(minuteOfDay: Int): String =
    String.format(Locale.US, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60).fa()

data class JalaliDate(val year: Int, val month: Int, val day: Int) : Comparable<JalaliDate> {
    val monthName: String get() = MONTHS[month - 1]

    override fun compareTo(other: JalaliDate): Int = compareValuesBy(this, other, { it.year }, { it.month }, { it.day })

    /** The Gregorian date of this day; a day past the end of its month gives the month's last day. */
    fun toLocalDate(): LocalDate {
        // 1 Farvardin falls on 20 or 21 March, so the estimate is off by a day or two at most.
        val dayOfYear = if (month <= 6) (month - 1) * 31 else 186 + (month - 7) * 30
        var d = LocalDate.of(year + 621, 3, 21).plusDays((dayOfYear + day - 1).toLong())
        while (from(d) < this) d = d.plusDays(1)
        while (from(d) > this) d = d.minusDays(1)
        return d
    }

    /** The same day [months] months later (or earlier), on the last day of a shorter month. */
    fun plusMonths(months: Int): JalaliDate {
        val index = year * 12 + (month - 1) + months
        val y = Math.floorDiv(index, 12)
        val m = Math.floorMod(index, 12) + 1
        return JalaliDate(y, m, minOf(day, monthLength(y, m)))
    }

    companion object {
        fun monthLength(year: Int, month: Int): Int = when {
            month <= 6 -> 31
            month <= 11 -> 30
            // Esfand has 30 days in a leap year.
            else -> if (from(JalaliDate(year, 12, 30).toLocalDate()).day == 30) 30 else 29
        }

        private val MONTHS = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
        )

        /** Arithmetic Gregorian → Solar Hijri conversion (valid for the 33-year cycle used in Iran). */
        fun from(date: LocalDate): JalaliDate {
            val gy = date.year
            val gm = date.monthValue
            val gd = date.dayOfMonth
            val gDaysBeforeMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
            val gy2 = if (gm > 2) gy + 1 else gy
            var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 +
                gd + gDaysBeforeMonth[gm - 1]
            var jy = -1595 + 33 * (days / 12053)
            days %= 12053
            jy += 4 * (days / 1461)
            days %= 1461
            if (days > 365) {
                jy += (days - 1) / 365
                days = (days - 1) % 365
            }
            val jm: Int
            val jd: Int
            if (days < 186) {
                jm = 1 + days / 31
                jd = 1 + days % 31
            } else {
                jm = 7 + (days - 186) / 30
                jd = 1 + (days - 186) % 30
            }
            return JalaliDate(jy, jm, jd)
        }
    }
}

fun DayOfWeek.persianName(): String = when (this) {
    DayOfWeek.SATURDAY -> "شنبه"
    DayOfWeek.SUNDAY -> "یکشنبه"
    DayOfWeek.MONDAY -> "دوشنبه"
    DayOfWeek.TUESDAY -> "سه‌شنبه"
    DayOfWeek.WEDNESDAY -> "چهارشنبه"
    DayOfWeek.THURSDAY -> "پنجشنبه"
    DayOfWeek.FRIDAY -> "جمعه"
}

/** The Iranian week, Saturday first. */
val PERSIAN_WEEK: List<DayOfWeek> = listOf(
    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
)

/** "هر روز", "هفته‌ای یک بار: شنبه" or "شنبه، دوشنبه، چهارشنبه"; empty or all seven means every day. */
fun formatDays(days: Set<DayOfWeek>): String = when (days.size) {
    0, 7 -> "هر روز"
    1 -> "هفته‌ای یک بار: ${days.single().persianName()}"
    else -> PERSIAN_WEEK.filter { it in days }.joinToString("، ") { it.persianName() }
}

/** e.g. "۲۱ مهر", with the year only when it isn't the current one. */
fun formatShortDate(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val j = JalaliDate.from(date)
    val year = if (j.year != JalaliDate.from(today).year) " ${j.year.fa()}" else ""
    return "${j.day.fa()} ${j.monthName}$year"
}

/** "از ۲۱ مهر تا ۲ آبان", or for a monthly range "هر ماه از روز ۲۱ به مدت ۱۲ روز". */
fun formatRange(range: DateRange, today: LocalDate = LocalDate.now()): String = when {
    range.monthly -> "هر ماه از روز ${JalaliDate.from(range.start).day.fa()} به مدت ${range.length.fa()} روز"
    range.start == range.end -> "فقط ${formatShortDate(range.start, today)}"
    else -> "از ${formatShortDate(range.start, today)} تا ${formatShortDate(range.end, today)}"
}

/** e.g. "سه‌شنبه ۲۸ مهر ۱۴۰۵" */
fun formatPersianDate(date: LocalDate): String {
    val j = JalaliDate.from(date)
    return "${date.dayOfWeek.persianName()} ${j.day.fa()} ${j.monthName} ${j.year.fa()}"
}
