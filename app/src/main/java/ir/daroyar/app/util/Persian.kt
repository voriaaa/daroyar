package ir.daroyar.app.util

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

data class JalaliDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = MONTHS[month - 1]

    companion object {
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

/** e.g. "سه‌شنبه ۲۸ مهر ۱۴۰۵" */
fun formatPersianDate(date: LocalDate): String {
    val j = JalaliDate.from(date)
    return "${date.dayOfWeek.persianName()} ${j.day.fa()} ${j.monthName} ${j.year.fa()}"
}
