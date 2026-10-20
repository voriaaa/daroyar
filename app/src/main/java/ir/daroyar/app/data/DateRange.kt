package ir.daroyar.app.data

import ir.daroyar.app.util.JalaliDate
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Dates a medication is limited to, both ends included, e.g. 21 Mehr to 2 Aban.
 *
 * With [monthly], the same span comes back every (Jalali) month from [start] on: the 21st for
 * twelve days, every month. That fits medicines taken around the menstrual period.
 */
data class DateRange(val start: LocalDate, val end: LocalDate, val monthly: Boolean = false) {

    init {
        require(!end.isBefore(start)) { "range ends before it starts" }
    }

    /** Days in one span, both ends counted. */
    val length: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1

    operator fun contains(date: LocalDate): Boolean {
        if (date.isBefore(start)) return false
        if (!monthly) return !date.isAfter(end)
        // The span that starts in date's own month, or one that started in an earlier month and runs into it.
        val first = JalaliDate.from(start)
        val target = JalaliDate.from(date)
        val monthsSinceStart = (target.year - first.year) * 12 + (target.month - first.month)
        val spanMonths = length / 29 + 1
        return (maxOf(0, monthsSinceStart - spanMonths)..monthsSinceStart).any { k ->
            val spanStart = first.plusMonths(k).toLocalDate()
            !date.isBefore(spanStart) && date.isBefore(spanStart.plusDays(length.toLong()))
        }
    }

    /** Over for good: a one-off range whose last day is before [date]. */
    fun endedBefore(date: LocalDate): Boolean = !monthly && end.isBefore(date)

    fun toJson(): JSONObject = JSONObject()
        .put("start", start.toEpochDay())
        .put("end", end.toEpochDay())
        .put("monthly", monthly)

    companion object {
        fun fromJson(o: JSONObject?): DateRange? {
            if (o == null || !o.has("start") || !o.has("end")) return null
            val start = LocalDate.ofEpochDay(o.getLong("start"))
            val end = LocalDate.ofEpochDay(o.getLong("end"))
            if (end.isBefore(start)) return null
            return DateRange(start, end, o.optBoolean("monthly", false))
        }
    }
}
