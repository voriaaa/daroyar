package ir.daroyar.app.reminder

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class DoseStatus { TAKEN, LATE, UPCOMING }

/** Pure date/time rules for daily doses; kept free of Android types so they can be unit-tested. */
object Schedule {

    fun timeOf(minuteOfDay: Int): LocalTime = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

    /** First occurrence of [minuteOfDay] strictly after [now]. */
    fun nextAfter(now: LocalDateTime, minuteOfDay: Int): LocalDateTime {
        val today = now.toLocalDate().atTime(timeOf(minuteOfDay))
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /**
     * The occurrence after the one scheduled on [firedDate]. If the device was off and that
     * day has also passed, jumps to the next occurrence after [now] instead of firing a backlog.
     */
    fun following(firedDate: LocalDate, minuteOfDay: Int, now: LocalDateTime): LocalDateTime {
        val candidate = firedDate.plusDays(1).atTime(timeOf(minuteOfDay))
        return if (candidate.isAfter(now)) candidate else nextAfter(now, minuteOfDay)
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
