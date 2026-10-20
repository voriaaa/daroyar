package ir.daroyar.app.reminder

import ir.daroyar.app.data.Medication
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class DoseStatus { TAKEN, LATE, UPCOMING }

/**
 * Pure date/time rules for doses; kept free of Android types so they can be unit-tested.
 *
 * A set of weekdays limits a dose to those days; an empty set means every day.
 */
object Schedule {

    fun timeOf(minuteOfDay: Int): LocalTime = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

    fun isDue(days: Set<DayOfWeek>, date: LocalDate): Boolean = days.isEmpty() || date.dayOfWeek in days

    /** All seven days is the same as every day; store it as the empty set. */
    fun normalizeDays(days: Collection<DayOfWeek>): Set<DayOfWeek> =
        days.toSet().let { if (it.size == 7) emptySet() else it }

    /** [date] itself if [days] includes it, otherwise the next day that does. */
    private fun firstDueOnOrAfter(date: LocalDate, days: Set<DayOfWeek>): LocalDate {
        var d = date
        while (!isDue(days, d)) d = d.plusDays(1) // at most six steps: days is empty or has a match
        return d
    }

    /** First occurrence of [minuteOfDay] on one of [days] strictly after [now]. */
    fun nextAfter(now: LocalDateTime, minuteOfDay: Int, days: Set<DayOfWeek> = emptySet()): LocalDateTime {
        val today = now.toLocalDate()
        val start = if (today.atTime(timeOf(minuteOfDay)).isAfter(now)) today else today.plusDays(1)
        return firstDueOnOrAfter(start, days).atTime(timeOf(minuteOfDay))
    }

    /**
     * The occurrence after the one scheduled on [firedDate]. If the device was off and that
     * day has also passed, jumps to the next occurrence after [now] instead of firing a backlog.
     */
    fun following(
        firedDate: LocalDate,
        minuteOfDay: Int,
        now: LocalDateTime,
        days: Set<DayOfWeek> = emptySet(),
    ): LocalDateTime {
        val candidate = firstDueOnOrAfter(firedDate.plusDays(1), days).atTime(timeOf(minuteOfDay))
        return if (candidate.isAfter(now)) candidate else nextAfter(now, minuteOfDay, days)
    }

    /**
     * Weekdays on which the alarm slot at [minuteOfDay] has to ring: the union of the days of the
     * active medications using that time, or every day (empty) if any of them is daily.
     */
    fun slotDays(meds: List<Medication>, minuteOfDay: Int): Set<DayOfWeek> {
        val using = meds.filter { it.active && minuteOfDay in it.times }
        return if (using.any { it.everyDay }) emptySet() else normalizeDays(using.flatMap { it.days })
    }

    fun status(minuteOfDay: Int, now: LocalTime, taken: Boolean): DoseStatus = when {
        taken -> DoseStatus.TAKEN
        now >= timeOf(minuteOfDay) -> DoseStatus.LATE
        else -> DoseStatus.UPCOMING
    }

    /** Quick presets in prescription terms ("هر ۸ ساعت"), as minutes since midnight. */
    val presets: List<Pair<String, List<Int>>> = listOf(
        "روزی یک بار" to listOf(9 * 60),
        "هر ۱۲ ساعت" to listOf(9 * 60, 21 * 60),
        "هر ۸ ساعت" to listOf(6 * 60, 14 * 60, 22 * 60),
        "هر ۶ ساعت" to listOf(0, 6 * 60, 12 * 60, 18 * 60),
    )
}
