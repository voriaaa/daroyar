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
 * A set of weekdays limits a dose to those days; an empty set means every day. A medication's
 * date range limits it further ([Medication.isDueOn]).
 */
object Schedule {

    fun timeOf(minuteOfDay: Int): LocalTime = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

    fun isDue(days: Set<DayOfWeek>, date: LocalDate): Boolean = days.isEmpty() || date.dayOfWeek in days

    /** All seven days is the same as every day; store it as the empty set. */
    fun normalizeDays(days: Collection<DayOfWeek>): Set<DayOfWeek> =
        days.toSet().let { if (it.size == 7) emptySet() else it }

    /** How far ahead to look for a due day; a date range may start months from now. */
    private const val HORIZON_DAYS = 800

    /** [date] itself if it is due, otherwise the next day that is; null if none is within [HORIZON_DAYS]. */
    private fun firstDueOnOrAfter(date: LocalDate, isDue: (LocalDate) -> Boolean): LocalDate? {
        var d = date
        repeat(HORIZON_DAYS) {
            if (isDue(d)) return d
            d = d.plusDays(1)
        }
        return null
    }

    /** First occurrence of [minuteOfDay] on one of [days] strictly after [now]. */
    fun nextAfter(now: LocalDateTime, minuteOfDay: Int, days: Set<DayOfWeek> = emptySet()): LocalDateTime =
        nextDue(now, minuteOfDay) { isDue(days, it) }!! // days is empty or has a match within a week

    /** First occurrence of [minuteOfDay] on a day that [isDue] strictly after [now], or null if there is none. */
    fun nextDue(now: LocalDateTime, minuteOfDay: Int, isDue: (LocalDate) -> Boolean): LocalDateTime? {
        val today = now.toLocalDate()
        val start = if (today.atTime(timeOf(minuteOfDay)).isAfter(now)) today else today.plusDays(1)
        return firstDueOnOrAfter(start, isDue)?.atTime(timeOf(minuteOfDay))
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
    ): LocalDateTime = followingDue(firedDate, minuteOfDay, now) { isDue(days, it) }!!

    /** [following] for any rule of due days; null once no day is due any more. */
    fun followingDue(
        firedDate: LocalDate,
        minuteOfDay: Int,
        now: LocalDateTime,
        isDue: (LocalDate) -> Boolean,
    ): LocalDateTime? {
        val candidate = firstDueOnOrAfter(firedDate.plusDays(1), isDue)?.atTime(timeOf(minuteOfDay))
        return if (candidate == null || candidate.isAfter(now)) candidate else nextDue(now, minuteOfDay, isDue)
    }

    /**
     * Whether the alarm slot at [minuteOfDay] has to ring on [date]: some active medication uses that
     * time and is due that day (by its weekdays and date range).
     */
    fun slotDueOn(meds: List<Medication>, minuteOfDay: Int, date: LocalDate): Boolean =
        meds.any { it.active && minuteOfDay in it.times && it.isDueOn(date) }

    /** Next time the slot at [minuteOfDay] rings after [now]; null when no medication needs it again. */
    fun nextSlot(meds: List<Medication>, minuteOfDay: Int, now: LocalDateTime): LocalDateTime? =
        nextDue(now, minuteOfDay) { slotDueOn(meds, minuteOfDay, it) }

    /** The slot's ring after the one on [firedDate]; see [following]. */
    fun followingSlot(meds: List<Medication>, minuteOfDay: Int, firedDate: LocalDate, now: LocalDateTime): LocalDateTime? =
        followingDue(firedDate, minuteOfDay, now) { slotDueOn(meds, minuteOfDay, it) }

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
